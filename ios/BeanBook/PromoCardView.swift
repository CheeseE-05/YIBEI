import Foundation
import SwiftUI

// MARK: - 社交「手绘特调咖啡随杯卡」（卡片 v4：可编辑元素 + 手动拖拽 + 自动流式排版）
//
// 视觉设计保持不变：奶油纹理纸、细线分隔、大号衬线英文名 + 中文名、配料/风味/关键词、
// AI 插画或相册抠图作为主视觉。v4 只增加：
//   - 所有元素（分类/烘焙标签/价格/英文名/中文名/配料/风味参考/关键词/插画）可单独显示隐藏、
//     可手动拖动、可缩放（文字还可对齐）；
//   - 默认仍然是测量真实尺寸后的流式排版（不重叠、不出界）；
//   - 手动位置按横/竖版分别归一化保存，A6 与社交尺寸共用。

/// 自动排版时，各元素在卡面坐标系里的真实 frame，用于编辑器显示选框 / 初始化手动布局。
struct PromoCardElementFramesKey: PreferenceKey {
    static var defaultValue: [String: CGRect] = [:]
    static func reduce(value: inout [String: CGRect], nextValue: () -> [String: CGRect]) {
        value.merge(nextValue()) { _, new in new }
    }
}

struct PromoCardView: View {
    let recipe: Recipe
    let bean: Bean?
    let config: PromoCardConfig
    var price: Int? = nil
    var photoImage: UIImage? = nil
    var beanImage: UIImage? = nil
    var illustrationImage: UIImage? = nil
    /// 相册导入 + Vision 自动抠图后的透明 PNG。
    var importedImage: UIImage? = nil
    /// 自动排版时上报元素 frame（编辑器选框用）。
    var onElementFrames: (([String: CGRect]) -> Void)? = nil

    private var accent: Color { Color(hex: config.themeHex) }
    private var isPortrait: Bool { config.ratio.isPortrait }

    // MARK: 内容（全部支持手动覆盖，nil 时按配方自动生成）

    private var categoryText: String {
        let custom = config.categoryOverride?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return custom.isEmpty ? promoDrinkCategory(for: recipe) : custom
    }
    private var roastTags: [String] {
        if let custom = config.roastTagsOverride, !custom.isEmpty { return custom }
        return promoBeanTags(bean: bean)
    }
    private var englishName: String {
        let custom = config.englishName?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return custom.isEmpty ? promoDefaultEnglishName(recipe: recipe, bean: bean) : custom
    }
    private var chineseName: String { config.title }
    private var ingredientsChinese: String {
        let custom = config.ingredientsChineseOverride?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return custom.isEmpty ? promoIngredientsChinese(for: recipe) : custom
    }
    private var ingredientsEnglish: String {
        let custom = config.ingredientsEnglishOverride?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !custom.isEmpty { return custom }
        let beanName = bean?.name ?? recipe.beanName
        let english = promoIngredientsEnglish(for: recipe)
        if beanName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return english }
        return "\(english) ｜ \(beanName)"
    }
    private var flavorKeywords: [String] {
        if let custom = config.keywordsOverride, !custom.isEmpty { return custom }
        return promoFlavorKeywords(recipe: recipe, bean: bean)
    }
    private var flavorReference: String {
        let custom = config.flavorReferenceOverride?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return custom.isEmpty ? flavorKeywords.joined(separator: "、") : custom
    }
    private var flavorEnglishText: String {
        let custom = config.flavorEnglishOverride?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return custom.isEmpty ? promoFlavorEnglish(recipe: recipe, bean: bean) : custom
    }
    private var hiddenElements: Set<PromoCardElement> {
        Set(config.hiddenElements ?? [])
    }
    private var manualLayouts: [String: PromoCardElementLayout] {
        config.manualLayout?.layouts(for: config.ratio) ?? [:]
    }
    private var usesManualLayout: Bool { !manualLayouts.isEmpty }

    var body: some View {
        GeometryReader { geo in
            let size = geo.size
            let u = size.width / 640
            ZStack {
                PromoPaperTexture()
                if usesManualLayout {
                    manualLayoutView(size: size, u: u)
                } else {
                    autoLayoutView(size: size, u: u)
                }
            }
            .frame(width: size.width, height: size.height)
        }
        .coordinateSpace(name: "promoCard")
        .onPreferenceChange(PromoCardElementFramesKey.self) { frames in
            onElementFrames?(frames)
        }
        .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
    }

    // MARK: - 自动流式排版（默认，视觉与 v2 一致）

    private var illustrationScaleFactor: CGFloat {
        switch config.template {
        case .magazine: return 1.0
        case .poster: return 1.45
        case .minimal: return 0.66
        }
    }

    @ViewBuilder
    private func autoLayoutView(size: CGSize, u: CGFloat) -> some View {
        let scale = illustrationScaleFactor
        Group {
            if isPortrait {
                portraitAutoLayout(size: size, u: u, illustrationScale: scale)
            } else {
                landscapeAutoLayout(size: size, u: u, illustrationScale: scale)
            }
        }
    }

    private func portraitAutoLayout(size: CGSize, u: CGFloat, illustrationScale: CGFloat) -> some View {
        let h = size.height
        let padding = 36 * u
        let illustrationSide = min(size.width - padding * 2, h * 0.34 * illustrationScale)
        let illustrationFirst = config.template == .poster
        return VStack(alignment: .leading, spacing: 0) {
            headerRow(u: u, h: h)
            hairline(u: u)
                .padding(.top, 10 * u)
            if illustrationFirst {
                Spacer(minLength: 12 * u)
                illustrationElement(side: illustrationSide, u: u)
                    .frame(maxWidth: .infinity, alignment: .center)
                Spacer(minLength: 10 * u)
                namesBlock(u: u, h: h)
                Spacer(minLength: 10 * u)
                ingredientsElement(u: u, h: h)
                Spacer(minLength: 8 * u)
                flavorElement(u: u, h: h)
                Spacer(minLength: 10 * u)
                keywordsElement(u: u, h: h)
            } else {
                Spacer(minLength: 16 * u)
                namesBlock(u: u, h: h)
                Spacer(minLength: 12 * u)
                ingredientsElement(u: u, h: h)
                Spacer(minLength: 10 * u)
                illustrationElement(side: illustrationSide, u: u)
                    .frame(maxWidth: .infinity, alignment: .center)
                Spacer(minLength: 8 * u)
                keywordsElement(u: u, h: h)
                Spacer(minLength: 12 * u)
                flavorElement(u: u, h: h)
                Spacer(minLength: 8 * u)
            }
        }
        .padding(.horizontal, padding)
        .padding(.top, 30 * u)
        .padding(.bottom, 26 * u)
        .frame(width: size.width, height: size.height, alignment: .topLeading)
    }

    private func landscapeAutoLayout(size: CGSize, u: CGFloat, illustrationScale: CGFloat) -> some View {
        let h = size.height
        let padding = 38 * u
        let illustrationWidth = size.width * (0.48 + min(0.06, (illustrationScale - 1) * 0.05))
        return HStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 0) {
                headerRow(u: u, h: h)
                hairline(u: u)
                    .padding(.top, 9 * u)
                Spacer(minLength: 12 * u)
                namesBlock(u: u, h: h)
                Spacer(minLength: 10 * u)
                ingredientsElement(u: u, h: h)
                Spacer(minLength: 10 * u)
                flavorElement(u: u, h: h)
                Spacer(minLength: 10 * u)
                keywordsElement(u: u, h: h)
            }
            .padding(.leading, padding)
            .padding(.vertical, 28 * u)
            .frame(width: size.width - illustrationWidth, alignment: .leading)

            illustrationElement(side: min(illustrationWidth - 16 * u, h - 30 * u), u: u)
                .frame(width: illustrationWidth, height: size.height)
                .padding(.trailing, 22 * u)
        }
        .frame(width: size.width, height: size.height)
    }

    // MARK: - 手动布局（拖拽后）

    private func manualLayoutView(size: CGSize, u: CGFloat) -> some View {
        ZStack(alignment: .topLeading) {
            // 手动布局时保留顶部分隔细线（非可编辑元素），保持原有视觉设计。
            hairline(u: u)
                .padding(.horizontal, isPortrait ? 36 * u : 38 * u)
                .frame(width: size.width, alignment: .leading)
                .position(
                    x: size.width / 2,
                    y: size.height * (isPortrait ? 0.088 : 0.128)
                )
            ForEach(PromoCardElement.allCases.filter { !hiddenElements.contains($0) }) { element in
                if let layout = manualLayouts[element.rawValue] {
                    manualElement(element, size: size, u: u, layout: layout)
                }
            }
        }
    }

    @ViewBuilder
    private func manualElement(
        _ element: PromoCardElement,
        size: CGSize,
        u: CGFloat,
        layout: PromoCardElementLayout
    ) -> some View {
        let h = size.height
        let width: CGFloat? = layout.width.map { CGFloat($0) * size.width }
        let height: CGFloat? = layout.height.map { CGFloat($0) * size.height }
        let fallbackWidth = size.width * 0.6
        let alignment = textAlignment(layout.alignment)
        Group {
            switch element {
            case .category:
                categoryView(u: u, h: h, alignment: alignment)
            case .roastTags:
                roastTagsView(u: u, h: h, alignment: alignment)
            case .price:
                priceView(u: u, h: h)
            case .englishName:
                englishNameView(u: u, h: h, alignment: alignment)
            case .chineseName:
                chineseNameView(u: u, h: h, alignment: alignment)
            case .ingredients:
                ingredientsView(u: u, h: h, alignment: alignment)
            case .flavorReference:
                flavorView(u: u, h: h, alignment: alignment)
            case .keywords:
                keywordsView(u: u, h: h)
            case .illustration:
                cover(size: CGSize(width: width ?? fallbackWidth, height: height ?? fallbackWidth))
            }
        }
        .frame(width: element.isText ? (width ?? fallbackWidth) : (width ?? fallbackWidth), alignment: frameAlignment(alignment))
        .scaleEffect(layout.scale)
        .position(x: layout.centerX * size.width, y: layout.centerY * size.height)
    }

    // MARK: - 顶部信息（元素版）

    private func headerRow(u: CGFloat, h: CGFloat) -> some View {
        HStack(alignment: .top, spacing: 12 * u) {
            if !hiddenElements.contains(.category) {
                categoryView(u: u, h: h)
                    .modifier(ElementFrameReporter(element: .category))
            }
            Spacer(minLength: 0)
            VStack(alignment: .trailing, spacing: 3 * u) {
                if !hiddenElements.contains(.roastTags) {
                    roastTagsView(u: u, h: h)
                        .modifier(ElementFrameReporter(element: .roastTags))
                }
                if !hiddenElements.contains(.price), let price, price > 0 {
                    priceView(u: u, h: h)
                        .modifier(ElementFrameReporter(element: .price))
                }
            }
        }
    }

    private func categoryView(u: CGFloat, h: CGFloat, alignment: TextAlignment = .leading) -> some View {
        let baseSize = fontSize(.category, u: u)
        return VStack(alignment: alignment == .center ? .center : (alignment == .trailing ? .trailing : .leading), spacing: 2 * u) {
            Text(categoryText)
                .font(.system(size: baseSize, weight: .medium))
                .tracking(1.4 * u)
                .foregroundStyle(PromoCardPalette.inkSoft)
            if let origin = promoOriginLine(bean: bean) {
                Text(origin)
                    .font(.system(size: baseSize * 0.862))
                    .tracking(0.4 * u)
                    .foregroundStyle(PromoCardPalette.muted)
            }
        }
        .multilineTextAlignment(alignment)
    }

    private func roastTagsView(u: CGFloat, h: CGFloat, alignment: TextAlignment = .trailing) -> some View {
        let baseSize = fontSize(.roastTags, u: u)
        return VStack(alignment: alignment == .center ? .center : (alignment == .leading ? .leading : .trailing), spacing: 2 * u) {
            ForEach(roastTags, id: \.self) { tag in
                Text(tag)
                    .font(.system(size: baseSize, weight: .regular))
                    .tracking(0.4 * u)
                    .foregroundStyle(PromoCardPalette.muted)
                    .lineLimit(1)
            }
        }
        .multilineTextAlignment(alignment)
    }

    private func priceView(u: CGFloat, h: CGFloat) -> some View {
        HStack(alignment: .bottom, spacing: 3 * u) {
            Text("¥\(price ?? 0)")
                .font(.system(size: fontSize(.price, u: u), weight: .medium, design: .serif))
                .foregroundStyle(PromoCardPalette.ink)
            Circle()
                .fill(accent)
                .frame(width: 4.2 * u, height: 4.2 * u)
                .padding(.bottom, h * 0.010)
        }
    }

    // MARK: - 名称 / 配料 / 风味 / 关键词（元素版）

    private func namesBlock(u: CGFloat, h: CGFloat) -> some View {
        VStack(alignment: .leading, spacing: 5 * u) {
            if !hiddenElements.contains(.englishName) {
                englishNameView(u: u, h: h)
                    .modifier(ElementFrameReporter(element: .englishName))
            }
            if !hiddenElements.contains(.chineseName) {
                chineseNameView(u: u, h: h)
                    .modifier(ElementFrameReporter(element: .chineseName))
            }
        }
    }

    private func englishNameView(u: CGFloat, h: CGFloat, alignment: TextAlignment = .leading) -> some View {
        Text(englishName)
            .font(.system(size: fontSize(.englishName, u: u), weight: .regular, design: .serif))
            .foregroundStyle(PromoCardPalette.ink)
            .lineLimit(2)
            .minimumScaleFactor(0.62)
            .tracking(-0.15 * u)
            .lineSpacing(-1 * u)
            .multilineTextAlignment(alignment)
    }

    private func chineseNameView(u: CGFloat, h: CGFloat, alignment: TextAlignment = .leading) -> some View {
        Text(chineseName)
            .font(.system(size: fontSize(.chineseName, u: u), weight: .medium, design: .serif))
            .foregroundStyle(PromoCardPalette.ink)
            .lineLimit(2)
            .minimumScaleFactor(0.75)
            .multilineTextAlignment(alignment)
    }

    private func ingredientsElement(u: CGFloat, h: CGFloat) -> some View {
        ingredientsView(u: u, h: h)
            .modifier(ElementFrameReporter(element: .ingredients))
    }

    private func ingredientsView(u: CGFloat, h: CGFloat, alignment: TextAlignment = .leading) -> some View {
        let baseSize = fontSize(.ingredients, u: u)
        return VStack(alignment: alignment == .center ? .center : (alignment == .trailing ? .trailing : .leading), spacing: 2.5 * u) {
            Text(ingredientsChinese)
                .font(.system(size: baseSize, weight: .medium))
                .foregroundStyle(PromoCardPalette.inkSoft)
                .lineLimit(2)
            Text(ingredientsEnglish)
                .font(.system(size: baseSize * 0.912))
                .foregroundStyle(PromoCardPalette.muted)
                .lineLimit(2)
        }
        .multilineTextAlignment(alignment)
    }

    private func flavorElement(u: CGFloat, h: CGFloat) -> some View {
        flavorView(u: u, h: h)
            .modifier(ElementFrameReporter(element: .flavorReference))
    }

    private func flavorView(u: CGFloat, h: CGFloat, alignment: TextAlignment = .leading) -> some View {
        let baseSize = fontSize(.flavorReference, u: u)
        return VStack(alignment: alignment == .center ? .center : (alignment == .trailing ? .trailing : .leading), spacing: 3 * u) {
            Text("风味参考：")
                .font(.system(size: baseSize, weight: .medium))
                .tracking(1.0 * u)
                .foregroundStyle(PromoCardPalette.inkSoft)
            Text(flavorReference)
                .font(.system(size: baseSize * 1.07))
                .foregroundStyle(PromoCardPalette.inkSoft)
                .lineLimit(1)
            if !flavorEnglishText.isEmpty {
                Text(flavorEnglishText)
                    .font(.system(size: baseSize * 0.957))
                    .foregroundStyle(PromoCardPalette.muted)
                    .lineLimit(1)
            }
        }
        .multilineTextAlignment(alignment)
    }

    private func keywordsElement(u: CGFloat, h: CGFloat) -> some View {
        keywordsView(u: u, h: h)
            .modifier(ElementFrameReporter(element: .keywords))
    }

    private func keywordsView(u: CGFloat, h: CGFloat) -> some View {
        Text(flavorKeywords.prefix(3).joined(separator: "　"))
            .font(.system(size: fontSize(.keywords, u: u), weight: .medium))
            .tracking(2.6 * u)
            .foregroundStyle(PromoCardPalette.inkSoft.opacity(0.88))
            .lineLimit(1)
            .minimumScaleFactor(0.7)
            .frame(maxWidth: .infinity, alignment: .center)
    }

    private func illustrationElement(side: CGFloat, u: CGFloat) -> some View {
        cover(size: CGSize(width: side, height: side))
            .modifier(ElementFrameReporter(element: .illustration))
    }

    private func hairline(u: CGFloat) -> some View {
        Rectangle()
            .fill(PromoCardPalette.ink.opacity(0.20))
            .frame(height: max(0.7, 0.8 * u))
    }

    /// 元素实际字号：自定义（8–72 设计点）优先，否则沿用原有比例；u = 卡宽/640。
    private func fontSize(_ element: PromoCardElement, u: CGFloat) -> CGFloat {
        promoFontSize(for: element, config: config) * u
    }

    private func textAlignment(_ value: String?) -> TextAlignment {
        switch value {
        case "center": return .center
        case "right": return .trailing
        default: return .leading
        }
    }

    private func frameAlignment(_ value: TextAlignment) -> Alignment {
        switch value {
        case .center: return .center
        case .trailing: return .trailing
        default: return .leading
        }
    }

    // MARK: - 主视觉：AI 插画 / 相册导入 / 配方杯图 / 豆子插画 / 相册图

    @ViewBuilder
    private func cover(size: CGSize) -> some View {
        switch config.cover {
        case .cup:
            MiniCupView(layers: recipe.layers, mixed: recipe.stirred)
                .frame(width: size.width, height: size.height)
        case .bean:
            if let beanImage {
                Image(uiImage: beanImage)
                    .resizable()
                    .scaledToFit()
                    .frame(width: size.width, height: size.height)
            } else if let bean {
                PromoBeanCover(bean: bean)
                    .frame(width: size.width, height: size.height)
            } else {
                MiniCupView(layers: recipe.layers, mixed: recipe.stirred)
                    .frame(width: size.width, height: size.height)
            }
        case .photo:
            if let photoImage {
                Image(uiImage: photoImage)
                    .resizable()
                    .scaledToFit()
                    .frame(width: size.width, height: size.height)
            } else {
                MiniCupView(layers: recipe.layers, mixed: recipe.stirred)
                    .frame(width: size.width, height: size.height)
            }
        case .illustration:
            if let illustrationImage {
                cutoutImage(Image(uiImage: illustrationImage), size: size)
            } else {
                illustrationPlaceholder(size: size)
            }
        case .imported:
            if let importedImage {
                cutoutImage(Image(uiImage: importedImage), size: size)
            } else {
                importedPlaceholder(size: size)
            }
        }
    }

    @ViewBuilder
    private func cutoutImage(_ image: Image, size: CGSize) -> some View {
        let base = image
            .resizable()
            .scaledToFit()
            .frame(width: size.width, height: size.height)
        if config.illustrationShadow == true {
            base.shadow(
                color: PromoCardPalette.ink.opacity(0.16),
                radius: size.width * 0.025,
                x: 0,
                y: size.width * 0.018
            )
        } else {
            base
        }
    }

    private func illustrationPlaceholder(size: CGSize) -> some View {
        VStack(spacing: size.width * 0.03) {
            Image(systemName: "sparkles")
                .font(.system(size: size.width * 0.09, weight: .light))
                .foregroundStyle(accent.opacity(0.7))
            Text("AI 插画待生成")
                .font(.system(size: size.width * 0.038, weight: .medium))
                .foregroundStyle(PromoCardPalette.muted)
            Text("在卡片编辑器点「AI 插画」")
                .font(.system(size: size.width * 0.03))
                .foregroundStyle(PromoCardPalette.muted.opacity(0.8))
        }
        .frame(width: size.width, height: size.height)
    }

    private func importedPlaceholder(size: CGSize) -> some View {
        VStack(spacing: size.width * 0.03) {
            Image(systemName: "photo.badge.arrow.down")
                .font(.system(size: size.width * 0.085, weight: .light))
                .foregroundStyle(PromoCardPalette.muted)
            Text("导入图片可自动抠图")
                .font(.system(size: size.width * 0.036, weight: .medium))
                .foregroundStyle(PromoCardPalette.muted)
            Text("透明背景直接叠在纸上")
                .font(.system(size: size.width * 0.028))
                .foregroundStyle(PromoCardPalette.muted.opacity(0.8))
        }
        .frame(width: size.width, height: size.height)
    }
}

/// 给自动排版里的元素上报真实 frame（不参与布局，只用于编辑器选框与手动布局初始化）。
private struct ElementFrameReporter: ViewModifier {
    let element: PromoCardElement

    func body(content: Content) -> some View {
        content.background {
            GeometryReader { proxy in
                Color.clear.preference(
                    key: PromoCardElementFramesKey.self,
                    value: [element.rawValue: proxy.frame(in: .named("promoCard"))]
                )
            }
        }
    }
}

// MARK: - 奶油纸纹理背景

struct PromoPaperTexture: View {
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(hex: "F7F3E8"),
                    Color(hex: "F3EDDF"),
                    Color(hex: "F6F2E7")
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            Canvas { context, size in
                var random = PromoSeededRandom(seed: 0xB00C_2026)
                let dots = Int(size.width * size.height / 2600)
                for _ in 0..<dots {
                    let x = random.next() * size.width
                    let y = random.next() * size.height
                    let length = 0.6 + random.next() * 1.8
                    let opacity = 0.012 + random.next() * 0.026
                    context.fill(
                        Path(CGRect(x: x, y: y, width: length, height: 0.7)),
                        with: .color(PromoCardPalette.ink.opacity(opacity))
                    )
                }
                for _ in 0..<Max(12, Int(size.width / 60)) {
                    let x = random.next() * size.width
                    let y = random.next() * size.height
                    let width = 6 + random.next() * 22
                    context.fill(
                        Path(CGRect(x: x, y: y, width: width, height: 0.5)),
                        with: .color(PromoCardPalette.ink.opacity(0.018 + random.next() * 0.022))
                    )
                }
            }
            .allowsHitTesting(false)
        }
    }
}

private struct PromoSeededRandom {
    var state: UInt64

    init(seed: UInt64) {
        state = seed == 0 ? 0x1234_5678 : seed
    }

    mutating func next() -> Double {
        state = state &* 6_364_136_223_846_793_005 &+ 1_442_695_040_888_963_407
        return Double((state >> 33) & 0xFFFF_FFFF) / Double(0xFFFF_FFFF)
    }
}

private func Max(_ a: Int, _ b: Int) -> Int { a > b ? a : b }

// MARK: - 卡片里用的豆子插画（固定奶油色系，不依赖 AppStore）

struct PromoBeanCover: View {
    let bean: Bean

    private var tone: Color { Color(hex: bean.roast.toneHex) }
    private var bg: Color { Color(hex: bean.status.bgHex) }

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height
            ZStack {
                bg
                Circle()
                    .fill(Color.white.opacity(0.35))
                    .frame(width: max(w, h) * 0.72)
                Ellipse()
                    .fill(tone)
                    .frame(width: w * 0.44, height: h * 0.58)
                    .rotationEffect(.degrees(-24))
                    .shadow(color: PromoCardPalette.ink.opacity(0.22), radius: max(w, h) * 0.02, x: w * 0.01, y: h * 0.02)
                Path { path in
                    path.move(to: CGPoint(x: w * 0.46, y: h * 0.30))
                    path.addCurve(
                        to: CGPoint(x: w * 0.52, y: h * 0.72),
                        control1: CGPoint(x: w * 0.62, y: h * 0.42),
                        control2: CGPoint(x: w * 0.36, y: h * 0.58)
                    )
                }
                .stroke(PromoCardPalette.ink.opacity(0.45), style: StrokeStyle(lineWidth: max(w, h) * 0.006, lineCap: .round))
                Circle()
                    .fill(Color.white.opacity(0.55))
                    .frame(width: max(w, h) * 0.016)
                    .offset(x: -w * 0.06, y: -h * 0.12)
            }
        }
        .clipped()
    }
}
