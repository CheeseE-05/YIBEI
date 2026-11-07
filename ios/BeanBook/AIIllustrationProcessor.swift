import Foundation
import UIKit
import CoreGraphics

/// AI 插画透明化方式（用于 UI 提示与验证）。
enum AIIllustrationProcessingMethod: String {
    case alphaPreserved
    case colorKey
    case checkerboard
    case fallback

    var label: String {
        switch self {
        case .alphaPreserved: return "模型直出透明 PNG（alpha 保留）"
        case .colorKey: return "纯色背景颜色抠图"
        case .checkerboard: return "棋盘格透明底转换"
        case .fallback: return "回退原图"
        }
    }
}

/// AI 插画专用处理结果：透明化 + 去水印 + 透明 PNG。
struct AIIllustrationProcessingResult {
    let image: UIImage
    let didCutout: Bool
    let backgroundUniform: Bool
    let removedWatermarkBlocks: Int
    let fallbackReason: String?
    /// 处理版（裁剪后）透明/不透明像素占比，用于验证与 UI 展示。
    let transparentFraction: Double
    let opaqueFraction: Double
    let sourceSize: CGSize
    let outputSize: CGSize
    let method: AIIllustrationProcessingMethod

    static func fallback(_ image: UIImage, reason: String) -> AIIllustrationProcessingResult {
        AIIllustrationProcessingResult(
            image: image,
            didCutout: false,
            backgroundUniform: false,
            removedWatermarkBlocks: 0,
            fallbackReason: reason,
            transparentFraction: 0,
            opaqueFraction: 1,
            sourceSize: image.size,
            outputSize: image.size,
            method: .fallback
        )
    }
}

/// AI 插画处理流水线（不依赖 Vision / 人像分割）：
/// 1. 采样四边 2% 环带的颜色中位数作为纯色背景色，边缘颜色不均则回退原图；
/// 2. 逐像素 RGB 距离颜色键控：近背景 alpha=0，中间软过渡，并对半透明像素做反混合 despill；
/// 3. 外圈（底部/右侧 12–25% 带）删除「小面积、低饱和、灰色、与主体不相连」的连通块（AI 水印 / logo）；
/// 4. 裁剪透明外边距并留 1.5% padding，输出透明 PNG。
enum AIIllustrationProcessor {
    /// AI 出图一般是 1024–1536；超过则先缩小，保证处理速度。
    private static let maxDimension: CGFloat = 1536

    static func process(_ image: UIImage) async -> AIIllustrationProcessingResult {
        await Task.detached(priority: .userInitiated) {
            processSync(image)
        }.value
    }

    // MARK: - 同步处理

    private static func processSync(_ image: UIImage) -> AIIllustrationProcessingResult {
        guard let source = rgbaBuffer(from: image) else {
            return .fallback(image, reason: "无法读取插画像素，已保留原图")
        }
        let width = source.width
        let height = source.height
        var pixels = source.pixels

        // ---- 0. 模型返回的图本身带 alpha（真透明 PNG）→ 直接保留，不再颜色键控 ----
        if image.bbHasSignificantTransparency() {
            let trimmed = image.bbTrimmingTransparentMargins(paddingFraction: 0.015).bbNormalizedAlphaImage()
            let stats = alphaStatistics(of: trimmed)
            return AIIllustrationProcessingResult(
                image: trimmed,
                didCutout: true,
                backgroundUniform: true,
                removedWatermarkBlocks: 0,
                fallbackReason: nil,
                transparentFraction: stats.transparent,
                opaqueFraction: stats.opaque,
                sourceSize: image.size,
                outputSize: trimmed.size,
                method: .alphaPreserved
            )
        }

        // ---- 1. 四边 2% 环带 → 背景色（中位数）+ 均匀度 ----
        let band = max(2, Int(Double(min(width, height)) * 0.02))
        var ringIndices: [Int] = []
        ringIndices.reserveCapacity((width + height) * band * 2)
        for y in 0..<height {
            if y < band || y >= height - band {
                for x in 0..<width { ringIndices.append(y * width + x) }
            } else {
                for x in 0..<band { ringIndices.append(y * width + x) }
                for x in (width - band)..<width { ringIndices.append(y * width + x) }
            }
        }
        var histR = [Int](repeating: 0, count: 256)
        var histG = [Int](repeating: 0, count: 256)
        var histB = [Int](repeating: 0, count: 256)
        for index in ringIndices {
            let p = index * 4
            histR[Int(pixels[p])] += 1
            histG[Int(pixels[p + 1])] += 1
            histB[Int(pixels[p + 2])] += 1
        }
        let bgR = Double(medianValue(histR))
        let bgG = Double(medianValue(histG))
        let bgB = Double(medianValue(histB))
        var uniformCount = 0
        for index in ringIndices {
            let p = index * 4
            let dr = Double(pixels[p]) - bgR
            let dg = Double(pixels[p + 1]) - bgG
            let db = Double(pixels[p + 2]) - bgB
            if (dr * dr + dg * dg + db * db).squareRoot() < 30 { uniformCount += 1 }
        }
        let uniformFraction = ringIndices.isEmpty ? 0 : Double(uniformCount) / Double(ringIndices.count)
        if uniformFraction < 0.72 {
            // 部分模型会把「透明底」画成灰白棋盘格；检测双色棋盘并移除。
            if let checkerMask = checkerboardBackgroundMask(pixels: pixels, width: width, height: height, band: band) {
                var alpha = [UInt8](repeating: 255, count: width * height)
                var transparentCount = 0
                for index in 0..<alpha.count where checkerMask[index] {
                    alpha[index] = 0
                    transparentCount += 1
                    let p = index * 4
                    pixels[p] = 0; pixels[p + 1] = 0; pixels[p + 2] = 0
                }
                let transparentFraction = Double(transparentCount) / Double(width * height)
                guard transparentFraction >= 0.01, transparentFraction <= 0.985 else {
                    return .fallback(image, reason: "检测到棋盘格背景但背景占比异常，已保留原图未抠图")
                }
                return finishProcessing(
                    pixels: pixels,
                    alpha: alpha,
                    width: width,
                    height: height,
                    sourceImage: image,
                    backgroundUniform: false,
                    method: .checkerboard,
                    initialRemovedBlocks: 0
                )
            }
            return .fallback(image, reason: "背景颜色不纯（非纯色 / 非棋盘格透明底），已保留原图未抠图")
        }

        // ---- 2. 颜色键控 + 软过渡 + despill ----
        let inner = 16.0
        let outer = 48.0
        var alpha = [UInt8](repeating: 0, count: width * height)
        for index in 0..<(width * height) {
            let p = index * 4
            let r = Double(pixels[p])
            let g = Double(pixels[p + 1])
            let b = Double(pixels[p + 2])
            let dr = r - bgR
            let dg = g - bgG
            let db = b - bgB
            let distance = (dr * dr + dg * dg + db * db).squareRoot()
            var t = (distance - inner) / (outer - inner)
            t = min(max(t, 0), 1)
            t = t * t * (3 - 2 * t)   // smoothstep，保留抗锯齿边缘
            let alphaByte = UInt8((t * 255).rounded())
            alpha[index] = alphaByte
            if alphaByte == 0 {
                pixels[p] = 0; pixels[p + 1] = 0; pixels[p + 2] = 0
            } else if alphaByte < 255 {
                // 反混合：C = F*a + Bg*(1-a) → F = (C - Bg*(1-a)) / a
                let a = t
                let inv = 1 - a
                let fr = min(max((r - bgR * inv) / a, 0), 255)
                let fg = min(max((g - bgG * inv) / a, 0), 255)
                let fb = min(max((b - bgB * inv) / a, 0), 255)
                // 输出为 premultipliedLast：通道乘以 alpha
                pixels[p] = UInt8((fr * a).rounded())
                pixels[p + 1] = UInt8((fg * a).rounded())
                pixels[p + 2] = UInt8((fb * a).rounded())
            }
            pixels[p + 3] = alphaByte
        }

        let total = width * height
        let transparentBeforeCrop = alpha.reduce(0) { $0 + ($1 <= 8 ? 1 : 0) }
        let transparentFractionBeforeCrop = Double(transparentBeforeCrop) / Double(total)
        guard transparentFractionBeforeCrop >= 0.01, transparentFractionBeforeCrop <= 0.985 else {
            return .fallback(image, reason: "没有识别到可抠出的主体（背景占比异常），已保留原图未抠图")
        }

        return finishProcessing(
            pixels: pixels,
            alpha: alpha,
            width: width,
            height: height,
            sourceImage: image,
            backgroundUniform: true,
            method: .colorKey,
            initialRemovedBlocks: 0
        )
    }

    /// 通用收尾：主体连通域识别 → 外圈水印清理 → 噪点清理 → 裁剪透明外边距 + 1.5% padding。
    private static func finishProcessing(
        pixels: [UInt8],
        alpha: [UInt8],
        width: Int,
        height: Int,
        sourceImage: UIImage,
        backgroundUniform: Bool,
        method: AIIllustrationProcessingMethod,
        initialRemovedBlocks: Int
    ) -> AIIllustrationProcessingResult {
        var pixels = pixels
        var alpha = alpha
        var removedBlocks = initialRemovedBlocks
        let total = width * height

        // ---- 3. alpha 连通域：找主体（最大块） ----
        var labels = [Int32](repeating: 0, count: total)
        var labelSizes: [Int32: Int] = [:]
        var currentLabel: Int32 = 0
        var stack: [Int] = []
        stack.reserveCapacity(4096)
        for start in 0..<total where alpha[start] > 10 && labels[start] == 0 {
            currentLabel += 1
            labels[start] = currentLabel
            stack.append(start)
            var size = 0
            while let index = stack.popLast() {
                size += 1
                let x = index % width
                let y = index / width
                if x > 0 {
                    let n = index - 1
                    if alpha[n] > 10 && labels[n] == 0 { labels[n] = currentLabel; stack.append(n) }
                }
                if x + 1 < width {
                    let n = index + 1
                    if alpha[n] > 10 && labels[n] == 0 { labels[n] = currentLabel; stack.append(n) }
                }
                if y > 0 {
                    let n = index - width
                    if alpha[n] > 10 && labels[n] == 0 { labels[n] = currentLabel; stack.append(n) }
                }
                if y + 1 < height {
                    let n = index + width
                    if alpha[n] > 10 && labels[n] == 0 { labels[n] = currentLabel; stack.append(n) }
                }
            }
            labelSizes[currentLabel] = size
        }
        guard let subjectLabel = labelSizes.max(by: { $0.value < $1.value })?.key else {
            return .fallback(sourceImage, reason: "没有识别到可抠出的主体，已保留原图未抠图")
        }

        // ---- 4. 外圈水印 / logo 低饱和块清理 ----
        var candidateVisited = [Bool](repeating: false, count: total)
        var candidateStack: [Int] = []
        candidateStack.reserveCapacity(512)
        let maxBlockArea = Int(Double(total) * 0.008)
        for start in 0..<total {
            guard !candidateVisited[start] else { continue }
            let p = start * 4
            guard alpha[start] > 200 else { candidateVisited[start] = true; continue }
            let x = start % width
            let y = start / width
            // 外圈：底部 22% 或右侧 25%（水印一般在底部居中 / 右下角）
            guard y > Int(Double(height) * 0.78) || x > Int(Double(width) * 0.75) else {
                candidateVisited[start] = true
                continue
            }
            let r = Int(pixels[p])
            let g = Int(pixels[p + 1])
            let b = Int(pixels[p + 2])
            let saturation = max(r, max(g, b)) - min(r, min(g, b))
            guard saturation <= 45 else { candidateVisited[start] = true; continue }

            // BFS 收集候选连通块
            var block: [Int] = []
            candidateVisited[start] = true
            candidateStack.append(start)
            while let index = candidateStack.popLast() {
                block.append(index)
                let cx = index % width
                let cy = index / width
                func consider(_ n: Int) {
                    guard !candidateVisited[n] else { return }
                    let np = n * 4
                    guard alpha[n] > 200 else { candidateVisited[n] = true; return }
                    let nx = n % width
                    let ny = n / width
                    guard ny > Int(Double(height) * 0.78) || nx > Int(Double(width) * 0.75) else {
                        candidateVisited[n] = true
                        return
                    }
                    let nr = Int(pixels[np]), ng = Int(pixels[np + 1]), nb = Int(pixels[np + 2])
                    let nsat = max(nr, max(ng, nb)) - min(nr, min(ng, nb))
                    guard nsat <= 45 else { candidateVisited[n] = true; return }
                    candidateVisited[n] = true
                    candidateStack.append(n)
                }
                if cx > 0 { consider(index - 1) }
                if cx + 1 < width { consider(index + 1) }
                if cy > 0 { consider(index - width) }
                if cy + 1 < height { consider(index + width) }
            }

            let area = block.count
            guard area >= 6, area <= maxBlockArea else { continue }
            var minX = width, maxX = 0, minY = height, maxY = 0
            for index in block {
                let bx = index % width
                let by = index / width
                minX = min(minX, bx); maxX = max(maxX, bx)
                minY = min(minY, by); maxY = max(maxY, by)
            }
            let blockWidth = maxX - minX + 1
            let blockHeight = maxY - minY + 1
            guard blockWidth <= Int(Double(width) * 0.4), blockHeight <= Int(Double(height) * 0.25) else { continue }

            // 与主体相连的块保护起来（避免抠掉杯子把手/杯口等主体部件）
            var touchesSubject = false
            outer: for index in block {
                let bx = index % width
                let by = index / width
                for dy in -1...1 {
                    for dx in -1...1 {
                        let nx = bx + dx
                        let ny = by + dy
                        guard nx >= 0, nx < width, ny >= 0, ny < height else { continue }
                        if labels[ny * width + nx] == subjectLabel {
                            touchesSubject = true
                            break outer
                        }
                    }
                }
            }
            if touchesSubject { continue }

            // 删除该连通块 + 向外扩 3px 的 AA 边缘（不碰主体）
            var removal = Set(block)
            for _ in 0..<3 {
                var expanded = removal
                for index in removal {
                    let bx = index % width
                    let by = index / width
                    for dy in -1...1 {
                        for dx in -1...1 {
                            let nx = bx + dx
                            let ny = by + dy
                            guard nx >= 0, nx < width, ny >= 0, ny < height else { continue }
                            expanded.insert(ny * width + nx)
                        }
                    }
                }
                removal = expanded
            }
            for index in removal {
                guard labels[index] != subjectLabel else { continue }
                alpha[index] = 0
                let p = index * 4
                pixels[p] = 0; pixels[p + 1] = 0; pixels[p + 2] = 0; pixels[p + 3] = 0
            }
            removedBlocks += 1
        }

        // ---- 4.5 清理残留的 1–2px 噪点（水印 AA 边缘），避免撑大裁剪框 ----
        var visited = [Bool](repeating: false, count: total)
        for start in 0..<total where alpha[start] > 8 && !visited[start] {
            var component: [Int] = []
            var queue = [start]
            visited[start] = true
            while let index = queue.popLast() {
                component.append(index)
                let cx = index % width
                let cy = index / width
                if cx > 0 {
                    let n = index - 1
                    if alpha[n] > 8 && !visited[n] { visited[n] = true; queue.append(n) }
                }
                if cx + 1 < width {
                    let n = index + 1
                    if alpha[n] > 8 && !visited[n] { visited[n] = true; queue.append(n) }
                }
                if cy > 0 {
                    let n = index - width
                    if alpha[n] > 8 && !visited[n] { visited[n] = true; queue.append(n) }
                }
                if cy + 1 < height {
                    let n = index + width
                    if alpha[n] > 8 && !visited[n] { visited[n] = true; queue.append(n) }
                }
            }
            if component.count <= 4 {
                for index in component {
                    alpha[index] = 0
                    let p = index * 4
                    pixels[p] = 0; pixels[p + 1] = 0; pixels[p + 2] = 0; pixels[p + 3] = 0
                }
            }
        }

        // ---- 5. 裁掉透明外边距 + 1.5% padding ----
        var minX = width, maxX = -1, minY = height, maxY = -1
        for index in 0..<total where alpha[index] > 8 {
            let x = index % width
            let y = index / width
            minX = min(minX, x); maxX = max(maxX, x)
            minY = min(minY, y); maxY = max(maxY, y)
        }
        guard maxX >= minX, maxY >= minY else {
            return .fallback(sourceImage, reason: "没有识别到可抠出的主体，已保留原图未抠图")
        }
        let padding = Int(Double(max(width, height)) * 0.015)
        minX = max(0, minX - padding); minY = max(0, minY - padding)
        maxX = min(width - 1, maxX + padding); maxY = min(height - 1, maxY + padding)
        let cropWidth = maxX - minX + 1
        let cropHeight = maxY - minY + 1
        guard cropWidth >= 8, cropHeight >= 8 else {
            return .fallback(sourceImage, reason: "抠图区域异常，已保留原图未抠图")
        }

        var croppedPixels = [UInt8](repeating: 0, count: cropWidth * cropHeight * 4)
        var opaqueCount = 0
        var transparentCount = 0
        for y in 0..<cropHeight {
            for x in 0..<cropWidth {
                let sourceIndex = (minY + y) * width + (minX + x)
                let targetIndex = y * cropWidth + x
                let sp = sourceIndex * 4
                let tp = targetIndex * 4
                let a = alpha[sourceIndex]
                croppedPixels[tp] = pixels[sp]
                croppedPixels[tp + 1] = pixels[sp + 1]
                croppedPixels[tp + 2] = pixels[sp + 2]
                croppedPixels[tp + 3] = a
                if a <= 8 { transparentCount += 1 } else if a >= 247 { opaqueCount += 1 }
            }
        }

        guard let outputCG = makeCGImage(pixels: croppedPixels, width: cropWidth, height: cropHeight) else {
            return .fallback(sourceImage, reason: "输出透明 PNG 失败，已保留原图")
        }
        let output = UIImage(cgImage: outputCG, scale: 1, orientation: .up).bbNormalizedAlphaImage()
        let pixelCount = cropWidth * cropHeight
        return AIIllustrationProcessingResult(
            image: output,
            didCutout: true,
            backgroundUniform: backgroundUniform,
            removedWatermarkBlocks: removedBlocks,
            fallbackReason: nil,
            transparentFraction: Double(transparentCount) / Double(pixelCount),
            opaqueFraction: Double(opaqueCount) / Double(pixelCount),
            sourceSize: CGSize(width: width, height: height),
            outputSize: CGSize(width: cropWidth, height: cropHeight),
            method: method
        )
    }

    /// 统计透明/不透明像素占比（alpha ≤8 / ≥247）。
    private static func alphaStatistics(of image: UIImage) -> (transparent: Double, opaque: Double) {
        guard let buffer = rgbaBuffer(from: image) else { return (0, 1) }
        let total = buffer.width * buffer.height
        guard total > 0 else { return (0, 1) }
        var transparent = 0
        var opaque = 0
        for index in 0..<total {
            let alpha = buffer.pixels[index * 4 + 3]
            if alpha <= 8 { transparent += 1 } else if alpha >= 247 { opaque += 1 }
        }
        return (Double(transparent) / Double(total), Double(opaque) / Double(total))
    }

    /// 检测「模型画出来的灰白棋盘格透明底」，返回需要变透明的背景像素 mask。
    /// 判据：四边环带颜色可用 2 个低饱和浅色聚类解释、两色比例均衡、边缘扫描线有明显交替方块；
    /// 然后从图像四边泛洪，只移除与边缘相连的棋盘格颜色像素（白色杯身等主体内部不受影响）。
    private static func checkerboardBackgroundMask(
        pixels: [UInt8],
        width: Int,
        height: Int,
        band: Int
    ) -> [Bool]? {
        let total = width * height
        guard total > 0 else { return nil }

        // 采样四边环带（每 2px 一个样本）
        var samples: [(r: Double, g: Double, b: Double)] = []
        samples.reserveCapacity((width + height) * 2)
        for y in stride(from: 0, to: height, by: 2) {
            let isEdgeRow = y < band || y >= height - band
            for x in stride(from: 0, to: width, by: 2) {
                if !isEdgeRow && x >= band && x < width - band { continue }
                let p = (y * width + x) * 4
                samples.append((Double(pixels[p]), Double(pixels[p + 1]), Double(pixels[p + 2])))
            }
        }
        guard samples.count >= 200 else { return nil }

        // 2-means 聚类
        var center1 = samples[0]
        var farDistance = -1.0
        var center2 = samples[0]
        for sample in samples {
            let d = distanceSquared(sample, center1)
            if d > farDistance { farDistance = d; center2 = sample }
        }
        for _ in 0..<8 {
            var sum1 = (0.0, 0.0, 0.0); var count1 = 0
            var sum2 = (0.0, 0.0, 0.0); var count2 = 0
            for sample in samples {
                if distanceSquared(sample, center1) <= distanceSquared(sample, center2) {
                    sum1.0 += sample.r; sum1.1 += sample.g; sum1.2 += sample.b; count1 += 1
                } else {
                    sum2.0 += sample.r; sum2.1 += sample.g; sum2.2 += sample.b; count2 += 1
                }
            }
            guard count1 > 0, count2 > 0 else { return nil }
            center1 = (sum1.0 / Double(count1), sum1.1 / Double(count1), sum1.2 / Double(count1))
            center2 = (sum2.0 / Double(count2), sum2.1 / Double(count2), sum2.2 / Double(count2))
        }
        let share1 = Double(samples.filter { distanceSquared($0, center1) <= distanceSquared($0, center2) }.count) / Double(samples.count)
        guard share1 >= 0.15, share1 <= 0.85 else { return nil }
        guard colorSaturation(center1) <= 20, colorSaturation(center2) <= 20 else { return nil }
        let brightness1 = colorBrightness(center1)
        let brightness2 = colorBrightness(center2)
        guard brightness1 >= 135, brightness2 >= 135 else { return nil }
        let centerDistance = distanceSquared(center1, center2).squareRoot()
        guard centerDistance >= 8 else { return nil }

        // 边缘扫描线交替性：至少 2 条边有 10 次以上切换，且方块边长中位数在 3...80
        func scanLine(points: [(r: Double, g: Double, b: Double)]) -> (transitions: Int, medianRun: Double)? {
            guard points.count > 20 else { return nil }
            var labels: [Int] = []
            for point in points {
                labels.append(distanceSquared(point, center1) <= distanceSquared(point, center2) ? 0 : 1)
            }
            var transitions = 0
            var runs: [Int] = []
            var runLength = 1
            for index in 1..<labels.count {
                if labels[index] == labels[index - 1] {
                    runLength += 1
                } else {
                    transitions += 1
                    runs.append(runLength)
                    runLength = 1
                }
            }
            runs.append(runLength)
            guard !runs.isEmpty else { return nil }
            let sorted = runs.sorted()
            return (transitions, Double(sorted[sorted.count / 2]))
        }
        var validEdges = 0
        let scanLines: [[(r: Double, g: Double, b: Double)]] = [
            (0..<width).map { x in
                let p = ((band / 2) * width + x) * 4
                return (Double(pixels[p]), Double(pixels[p + 1]), Double(pixels[p + 2]))
            },
            (0..<width).map { x in
                let y = height - 1 - band / 2
                let p = (y * width + x) * 4
                return (Double(pixels[p]), Double(pixels[p + 1]), Double(pixels[p + 2]))
            },
            (0..<height).map { y in
                let p = (y * width + band / 2) * 4
                return (Double(pixels[p]), Double(pixels[p + 1]), Double(pixels[p + 2]))
            },
            (0..<height).map { y in
                let x = width - 1 - band / 2
                let p = (y * width + x) * 4
                return (Double(pixels[p]), Double(pixels[p + 1]), Double(pixels[p + 2]))
            }
        ]
        for line in scanLines {
            guard let result = scanLine(points: line) else { continue }
            if result.transitions >= 10, (3...80).contains(result.medianRun) { validEdges += 1 }
        }
        guard validEdges >= 2 else { return nil }

        // 候选背景像素：颜色接近两种棋盘格颜色；从四边泛洪
        var candidate = [Bool](repeating: false, count: total)
        for index in 0..<total {
            let p = index * 4
            let pixel = (r: Double(pixels[p]), g: Double(pixels[p + 1]), b: Double(pixels[p + 2]))
            let nearest = min(distanceSquared(pixel, center1), distanceSquared(pixel, center2))
            if nearest <= 36 * 36 {
                candidate[index] = true
            }
        }
        var mask = [Bool](repeating: false, count: total)
        var stack: [Int] = []
        stack.reserveCapacity(2048)
        func seed(_ index: Int) {
            if candidate[index] && !mask[index] {
                mask[index] = true
                stack.append(index)
            }
        }
        for x in 0..<width { seed(x); seed((height - 1) * width + x) }
        for y in 0..<height { seed(y * width); seed(y * width + width - 1) }
        while let index = stack.popLast() {
            let x = index % width
            let y = index / width
            if x > 0 { seed(index - 1) }
            if x + 1 < width { seed(index + 1) }
            if y > 0 { seed(index - width) }
            if y + 1 < height { seed(index + width) }
        }
        let backgroundCount = mask.reduce(0) { $0 + ($1 ? 1 : 0) }
        let fraction = Double(backgroundCount) / Double(total)
        guard fraction >= 0.15, fraction <= 0.90 else { return nil }
        return mask
    }

    private static func distanceSquared(
        _ a: (r: Double, g: Double, b: Double),
        _ b: (r: Double, g: Double, b: Double)
    ) -> Double {
        let dr = a.r - b.r
        let dg = a.g - b.g
        let db = a.b - b.b
        return dr * dr + dg * dg + db * db
    }

    private static func colorSaturation(_ c: (r: Double, g: Double, b: Double)) -> Double {
        max(c.r, max(c.g, c.b)) - min(c.r, min(c.g, c.b))
    }

    private static func colorBrightness(_ c: (r: Double, g: Double, b: Double)) -> Double {
        (c.r + c.g + c.b) / 3
    }

    // MARK: - 工具

    private struct RGBABuffer {
        let width: Int
        let height: Int
        var pixels: [UInt8]
    }

    private static func rgbaBuffer(from image: UIImage) -> RGBABuffer? {
        guard image.size.width > 0, image.size.height > 0 else { return nil }
        let maxSide = max(image.size.width, image.size.height)
        let ratio = maxSide > maxDimension ? maxDimension / maxSide : 1
        let width = max(1, Int((image.size.width * ratio).rounded()))
        let height = max(1, Int((image.size.height * ratio).rounded()))
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        format.opaque = false
        let normalized = UIGraphicsImageRenderer(size: CGSize(width: width, height: height), format: format).image { _ in
            image.draw(in: CGRect(x: 0, y: 0, width: width, height: height))
        }
        guard let cgImage = normalized.cgImage else { return nil }
        var pixels = [UInt8](repeating: 0, count: width * height * 4)
        guard let context = CGContext(
            data: &pixels,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: width * 4,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        ) else { return nil }
        context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))
        return RGBABuffer(width: width, height: height, pixels: pixels)
    }

    private static func makeCGImage(pixels: [UInt8], width: Int, height: Int) -> CGImage? {
        guard let provider = CGDataProvider(data: Data(pixels) as CFData) else { return nil }
        return CGImage(
            width: width,
            height: height,
            bitsPerComponent: 8,
            bitsPerPixel: 32,
            bytesPerRow: width * 4,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.premultipliedLast.rawValue),
            provider: provider,
            decode: nil,
            shouldInterpolate: true,
            intent: .defaultIntent
        )
    }

    private static func medianValue(_ histogram: [Int]) -> Int {
        let total = histogram.reduce(0, +)
        guard total > 0 else { return 0 }
        var accumulated = 0
        for (value, count) in histogram.enumerated() {
            accumulated += count
            if accumulated * 2 >= total { return value }
        }
        return 0
    }
}
