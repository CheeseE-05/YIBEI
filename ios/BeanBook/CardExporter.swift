import Foundation
import SwiftUI
import UIKit
import Photos

// MARK: - 宣传卡片导出
//
// 用 SwiftUI ImageRenderer 把 PromoCardView 渲染成 PNG：
//   - 社交尺寸：16:9 → 1200×675，3:4 竖版 → 1200×1600；
//   - A6 300dpi 打印尺寸：横 1748×1240，竖 1240×1748；
// 再用 Photos framework 以「仅添加」权限写入系统相册（Info.plist 已加
// NSPhotoLibraryAddUsageDescription），或生成临时 PNG 交给系统分享面板（含打印）。

enum CardExportError: LocalizedError {
    case renderFailed
    case permissionDenied
    case saveFailed(String)

    var errorDescription: String? {
        switch self {
        case .renderFailed:
            return "卡片渲染失败，请稍后重试。"
        case .permissionDenied:
            return "没有相册「仅添加」权限，无法保存图片。可以在系统设置里允许「忆杯手记」添加照片。"
        case .saveFailed(let message):
            return "保存到相册失败：\(message)"
        }
    }
}

@MainActor
enum PromoCardExporter {
    /// 当前配置对应的导出像素尺寸（社交 / A6 300dpi 打印）。
    static func exportPixelSize(for config: PromoCardConfig) -> CGSize {
        config.ratio.pixelSize(for: config.effectiveExportSize)
    }

    /// 渲染 PNG（按导出尺寸 1:1 渲染：A6 打印即 300dpi 实际像素）。
    static func renderPNG(
        recipe: Recipe,
        bean: Bean?,
        config: PromoCardConfig,
        price: Int?,
        photoImage: UIImage?,
        beanImage: UIImage?,
        illustrationImage: UIImage?,
        importedImage: UIImage?
    ) -> Data? {
        let size = exportPixelSize(for: config)
        let card = PromoCardView(
            recipe: recipe,
            bean: bean,
            config: config,
            price: price,
            photoImage: photoImage,
            beanImage: beanImage,
            illustrationImage: illustrationImage,
            importedImage: importedImage
        )
        .frame(width: size.width, height: size.height)
        let renderer = ImageRenderer(content: card)
        renderer.scale = 1
        renderer.isOpaque = false
        guard let image = renderer.uiImage else { return nil }
        return image.pngData()
    }

    /// 生成临时 PNG 文件，用于系统分享面板（AirDrop、微信、打印等）。
    static func makeShareFile(
        recipe: Recipe,
        bean: Bean?,
        config: PromoCardConfig,
        price: Int?,
        photoImage: UIImage?,
        beanImage: UIImage?,
        illustrationImage: UIImage?,
        importedImage: UIImage?
    ) -> URL? {
        guard let data = renderPNG(
            recipe: recipe,
            bean: bean,
            config: config,
            price: price,
            photoImage: photoImage,
            beanImage: beanImage,
            illustrationImage: illustrationImage,
            importedImage: importedImage
        ) else { return nil }
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent("BeanBookCards", isDirectory: true)
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let safeName = config.title
            .replacingOccurrences(of: "[\\\\/:*?\"<>|\\s]+", with: "_", options: .regularExpression)
            .prefix(24)
        let suffix = config.effectiveExportSize == .printA6 ? "A6-300dpi" : "social"
        let filename = "\(safeName.isEmpty ? "card" : String(safeName))-\(suffix)-\(Int(Date().timeIntervalSince1970)).png"
        let url = directory.appendingPathComponent(filename)
        do {
            try data.write(to: url, options: .atomic)
            return url
        } catch {
            return nil
        }
    }

    static func requestAddOnlyAuthorization() async -> PHAuthorizationStatus {
        await withCheckedContinuation { continuation in
            PHPhotoLibrary.requestAuthorization(for: .addOnly) { status in
                continuation.resume(returning: status)
            }
        }
    }

    static func saveToPhotoLibrary(_ data: Data) async throws {
        let status = await requestAddOnlyAuthorization()
        guard status == .authorized || status == .limited else {
            throw CardExportError.permissionDenied
        }
        do {
            try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
                PHPhotoLibrary.shared().performChanges {
                    let request = PHAssetCreationRequest.forAsset()
                    let options = PHAssetResourceCreationOptions()
                    options.uniformTypeIdentifier = "public.png"
                    request.addResource(with: .photo, data: data, options: options)
                } completionHandler: { success, error in
                    if success {
                        continuation.resume()
                    } else {
                        continuation.resume(throwing: CardExportError.saveFailed(error?.localizedDescription ?? "未知错误"))
                    }
                }
            }
        } catch let error as CardExportError {
            throw error
        } catch {
            throw CardExportError.saveFailed(error.localizedDescription)
        }
    }
}
