import UIKit
import Vision
import CoreImage
import ImageIO
import CoreVideo

enum ImageCutout {
    /// 轻量回退：把接近「边缘主色」的均匀背景变成透明（适合干净浅色背景的照片）。
    ///
    /// 说明：`VNGenerateForegroundInstanceMaskRequest` 在部分环境（例如模拟器/无 ML 推理上下文）
    /// 会返回 nil；这时用本地颜色分离作为降级方案，卡片仍能得到透明背景 PNG。
    /// 透明比例过低（没抠掉背景）或过高（整张图都没了）时返回 nil，由界面提示换图。
    static func removeBackground(from image: UIImage) async -> UIImage? {
        let normalized = image.normalizedForVision()
        guard let cgImage = normalized.cgImage else { return nil }

        return await withCheckedContinuation { (continuation: CheckedContinuation<UIImage?, Never>) in
            DispatchQueue.global(qos: .userInitiated).async {
                do {
                    let request = VNGenerateForegroundInstanceMaskRequest()
                    let handler = VNImageRequestHandler(cgImage: cgImage, orientation: .up, options: [:])
                    try handler.perform([request])
                    guard let observation = request.results?.first else {
                        continuation.resume(returning: nil)
                        return
                    }
                    let mask = try observation.generateMaskedImage(
                        ofInstances: observation.allInstances,
                        from: handler,
                        croppedToInstancesExtent: false
                    )
                    let ciImage = CIImage(cvPixelBuffer: mask)
                    let context = CIContext(options: [.useSoftwareRenderer: false])
                    guard let resultCG = context.createCGImage(ciImage, from: ciImage.extent) else {
                        continuation.resume(returning: nil)
                        return
                    }
                    continuation.resume(returning: UIImage(cgImage: resultCG))
                } catch {
                    continuation.resume(returning: nil)
                }
            }
        }
    }
}

extension UIImage {
    /// 重新绘制一次，确保透明 PNG 的 alpha 通道在 SwiftUI 预览/ImageRenderer 中正常合成。
    /// （直接用 CGContext 产出的 CGImage 在个别渲染路径上会丢失透明背景。）
    func bbNormalizedAlphaImage() -> UIImage {
        guard size.width > 0, size.height > 0 else { return self }
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        format.opaque = false
        return UIGraphicsImageRenderer(size: size, format: format).image { _ in
            draw(in: CGRect(origin: .zero, size: size))
        }
    }

    /// 检测图片是否有显著透明像素（alpha < 250），用于相册导入时直接保留透明 PNG。
    /// 1% 以上像素带透明即视为透明图；否则按不透明照片走自动抠图。
    func bbHasSignificantTransparency() -> Bool {
        bbTransparencyFraction() >= 0.01
    }

    /// 计算 alpha < 250 的像素占比（0–1），供透明导入判断与调试用。
    func bbTransparencyFraction() -> Double {
        guard size.width > 0, size.height > 0 else { return 0 }
        let longest = max(size.width, size.height)
        let ratio = longest > 800 ? 800 / longest : 1
        let width = max(1, Int(size.width * ratio))
        let height = max(1, Int(size.height * ratio))
        guard let cgImage = cgImage else { return 0 }
        // 透明背景必须先清零，否则 CGContext 用不透明底板合成会让 alpha 恒为 255。
        var pixels = [UInt8](repeating: 0, count: width * height * 4)
        guard let context = CGContext(
            data: &pixels,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: width * 4,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        ) else { return 0 }
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        format.opaque = false
        let normalized = UIGraphicsImageRenderer(size: CGSize(width: width, height: height), format: format).image { _ in
            UIImage(cgImage: cgImage).draw(in: CGRect(x: 0, y: 0, width: width, height: height))
        }
        guard let normalizedCG = normalized.cgImage else { return 0 }
        context.draw(normalizedCG, in: CGRect(x: 0, y: 0, width: width, height: height))
        var transparentCount = 0
        let total = width * height
        for index in 0..<total {
            if pixels[index * 4 + 3] < 250 { transparentCount += 1 }
        }
        return Double(transparentCount) / Double(total)
    }

    /// 裁掉透明外边距并留 1.5% padding；用于透明 PNG 导入，不改变原有 alpha 形状。
    func bbTrimmingTransparentMargins(paddingFraction: CGFloat = 0.015) -> UIImage {
        guard size.width > 0, size.height > 0 else { return self }
        let width = Int(size.width)
        let height = Int(size.height)
        guard let cgImage = cgImage else { return self }
        var pixels = [UInt8](repeating: 0, count: width * height * 4)
        guard let context = CGContext(
            data: &pixels,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: width * 4,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        ) else { return self }
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        format.opaque = false
        let normalized = UIGraphicsImageRenderer(size: CGSize(width: width, height: height), format: format).image { _ in
            UIImage(cgImage: cgImage).draw(in: CGRect(x: 0, y: 0, width: width, height: height))
        }
        guard let normalizedCG = normalized.cgImage else { return self }
        context.draw(normalizedCG, in: CGRect(x: 0, y: 0, width: width, height: height))
        var minX = width, maxX = -1, minY = height, maxY = -1
        for y in 0..<height {
            for x in 0..<width {
                if pixels[(y * width + x) * 4 + 3] > 8 {
                    minX = min(minX, x); maxX = max(maxX, x)
                    minY = min(minY, y); maxY = max(maxY, y)
                }
            }
        }
        guard maxX >= minX, maxY >= minY else { return self }
        let padding = Int(Double(max(width, height)) * paddingFraction)
        minX = max(0, minX - padding); minY = max(0, minY - padding)
        maxX = min(width - 1, maxX + padding); maxY = min(height - 1, maxY + padding)
        let cropWidth = maxX - minX + 1
        let cropHeight = maxY - minY + 1
        guard cropWidth >= 8, cropHeight >= 8,
              cropWidth < width || cropHeight < height else { return self }
        var cropped = [UInt8](repeating: 0, count: cropWidth * cropHeight * 4)
        for y in 0..<cropHeight {
            for x in 0..<cropWidth {
                let src = ((minY + y) * width + (minX + x)) * 4
                let dst = (y * cropWidth + x) * 4
                cropped[dst] = pixels[src]
                cropped[dst + 1] = pixels[src + 1]
                cropped[dst + 2] = pixels[src + 2]
                cropped[dst + 3] = pixels[src + 3]
            }
        }
        guard let provider = CGDataProvider(data: Data(cropped) as CFData),
              let output = CGImage(
                width: cropWidth,
                height: cropHeight,
                bitsPerComponent: 8,
                bitsPerPixel: 32,
                bytesPerRow: cropWidth * 4,
                space: CGColorSpaceCreateDeviceRGB(),
                bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.premultipliedLast.rawValue),
                provider: provider,
                decode: nil,
                shouldInterpolate: true,
                intent: .defaultIntent
              ) else { return self }
        return UIImage(cgImage: output, scale: 1, orientation: .up).bbNormalizedAlphaImage()
    }

    func bbDownscaled(maxDimension: CGFloat) -> UIImage {
        let longest = max(size.width, size.height)
        guard longest > maxDimension, longest > 0 else { return self }
        let ratio = maxDimension / longest
        let target = CGSize(width: size.width * ratio, height: size.height * ratio)
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        format.opaque = false
        return UIGraphicsImageRenderer(size: target, format: format).image { _ in
            draw(in: CGRect(origin: .zero, size: target))
        }
    }
}

private extension UIImage {
    func normalizedForVision() -> UIImage {
        guard imageOrientation != .up else { return self }
        let format = UIGraphicsImageRendererFormat()
        format.scale = scale
        format.opaque = false
        return UIGraphicsImageRenderer(size: size, format: format).image { _ in
            draw(in: CGRect(origin: .zero, size: size))
        }
    }
}

// MARK: - 本地回退：均匀浅色背景 → 透明

extension ImageCutout {
    static func removeUniformBackground(from image: UIImage, tolerance: Int = 30) async -> UIImage? {
        let normalized = image.normalizedForVision()
        guard let cgImage = normalized.cgImage else { return nil }

        return await withCheckedContinuation { (continuation: CheckedContinuation<UIImage?, Never>) in
            DispatchQueue.global(qos: .userInitiated).async {
                let width = cgImage.width
                let height = cgImage.height
                guard width > 8, height > 8 else {
                    continuation.resume(returning: nil)
                    return
                }
                let bytesPerRow = width * 4
                var pixels = [UInt8](repeating: 0, count: height * bytesPerRow)
                let colorSpace = CGColorSpaceCreateDeviceRGB()

                let output: CGImage? = pixels.withUnsafeMutableBytes { buffer -> CGImage? in
                    guard let context = CGContext(
                        data: buffer.baseAddress,
                        width: width,
                        height: height,
                        bitsPerComponent: 8,
                        bytesPerRow: bytesPerRow,
                        space: colorSpace,
                        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
                    ) else { return nil }
                    context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))

                    guard let base = buffer.bindMemory(to: UInt8.self).baseAddress else { return nil }
                    func pixel(_ x: Int, _ y: Int) -> UnsafeMutablePointer<UInt8> {
                        base + y * bytesPerRow + x * 4
                    }

                    // 用四条边的像素估计背景主色（避开主体所在的中部）。
                    var sumR = 0.0, sumG = 0.0, sumB = 0.0
                    var samples = 0.0
                    func sample(_ x: Int, _ y: Int) {
                        let p = pixel(x, y)
                        sumR += Double(p[0]); sumG += Double(p[1]); sumB += Double(p[2]); samples += 1
                    }
                    let stepX = max(1, width / 96)
                    let stepY = max(1, height / 96)
                    for x in stride(from: 0, to: width, by: stepX) {
                        sample(x, 0)
                        sample(x, min(height - 1, 1))
                        sample(x, height - 1)
                        sample(x, max(0, height - 2))
                    }
                    for y in stride(from: 0, to: height, by: stepY) {
                        sample(0, y)
                        sample(min(width - 1, 1), y)
                        sample(width - 1, y)
                        sample(max(0, width - 2), y)
                    }
                    guard samples > 0 else { return nil }
                    let bgR = sumR / samples
                    let bgG = sumG / samples
                    let bgB = sumB / samples

                    var removed = 0
                    for y in 0..<height {
                        for x in 0..<width {
                            let p = pixel(x, y)
                            let dr = Double(p[0]) - bgR
                            let dg = Double(p[1]) - bgG
                            let db = Double(p[2]) - bgB
                            let distance = (dr * dr + dg * dg + db * db).squareRoot()
                            if distance < Double(tolerance) {
                                p[3] = 0
                                removed += 1
                            }
                            // 不做整片半透明（会在奶油纸上留下淡淡的方块），只保留硬边；
                            // 透明 PNG 叠加在纸纹上，边缘细节交给图片本身的水彩质感。
                        }
                    }
                    let ratio = Double(removed) / Double(width * height)
                    // 没抠掉背景（<2%）或整张图都被抠没了（>97%）都视为失败。
                    guard ratio > 0.02, ratio < 0.97 else { return nil }
                    return context.makeImage()
                }

                guard let output else {
                    continuation.resume(returning: nil)
                    return
                }
                continuation.resume(returning: UIImage(cgImage: output))
            }
        }
    }
}
