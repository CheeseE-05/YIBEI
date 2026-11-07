import Foundation
import SwiftUI
import UIKit

extension Color {
    init(hex: String) {
        let clean = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var value: UInt64 = 0
        Scanner(string: clean).scanHexInt64(&value)
        let r, g, b, a: Double
        switch clean.count {
        case 8:
            r = Double((value >> 24) & 0xFF) / 255
            g = Double((value >> 16) & 0xFF) / 255
            b = Double((value >> 8) & 0xFF) / 255
            a = Double(value & 0xFF) / 255
        default:
            r = Double((value >> 16) & 0xFF) / 255
            g = Double((value >> 8) & 0xFF) / 255
            b = Double(value & 0xFF) / 255
            a = 1
        }
        self.init(.sRGB, red: r, green: g, blue: b, opacity: a)
    }

    static func blend(_ colors: [(Color, Double)]) -> Color {
        var r: CGFloat = 0
        var g: CGFloat = 0
        var b: CGFloat = 0
        var total: Double = 0
        for (color, weight) in colors {
            // 注意：色板是动态颜色（浅色/暗色两套），必须先按当前 trait 解析，否则 getRed 会失败。
            let ui = UIColor(color).resolvedColor(with: UITraitCollection.current)
            var rr: CGFloat = 0
            var gg: CGFloat = 0
            var bb: CGFloat = 0
            var aa: CGFloat = 0
            if ui.getRed(&rr, green: &gg, blue: &bb, alpha: &aa) {
                r += rr * weight
                g += gg * weight
                b += bb * weight
                total += weight
            }
        }
        guard total > 0 else { return C.line }
        return Color(.sRGB, red: r / total, green: g / total, blue: b / total, opacity: 1)
    }
}

/// 应用外观偏好（跟随系统 / 浅色 / 深色），持久化在 UserDefaults 的 `appearanceMode` 键。
enum AppAppearance: String, CaseIterable, Identifiable {
    case system, light, dark

    var id: String { rawValue }

    var label: String {
        switch self {
        case .system: return "跟随系统"
        case .light: return "浅色"
        case .dark: return "深色"
        }
    }

    var symbol: String {
        switch self {
        case .system: return "circle.lefthalf.filled"
        case .light: return "sun.max"
        case .dark: return "moon"
        }
    }

    /// 传给 `.preferredColorScheme`；nil 表示跟随系统。
    var colorScheme: ColorScheme? {
        switch self {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }

    static let storageKey = "appearanceMode"

    static var current: AppAppearance {
        AppAppearance(rawValue: UserDefaults.standard.string(forKey: storageKey) ?? "") ?? .system
    }
}

/// 品牌色板：全部是动态颜色（浅色 / 暗色两套），因此现有页面无需逐个改颜色即可适配黑暗模式。
/// `contrast` / `onContrast` 是「深色对比卡片」的语义色：浅色模式下就是原来的墨色卡片，
/// 暗色模式下卡片保持深色（比背景略亮一档）以维持原有「黑卡 + 白字」的设计语言。
enum C {
    /// 动态颜色：浅色模式用 lightHex，暗色模式用 darkHex。
    static func adaptive(_ lightHex: String, _ darkHex: String) -> Color {
        Color(uiColor: UIColor { trait in
            let hex = trait.userInterfaceStyle == .dark ? darkHex : lightHex
            return UIColor(Color(hex: hex))
        })
    }

    // 背景与表面
    static let paper = adaptive("EDECE7", "12110F")
    static let paperSoft = adaptive("F3F2EE", "1A1815")
    static let card = adaptive("FBFAF7", "24211D")
    static let pure = Color.white

    // 文字
    static let ink = adaptive("26231F", "F3F0E8")
    static let inkSoft = adaptive("48433C", "D2CCBF")
    static let muted = adaptive("8E897E", "A49D8F")
    static let mutedLight = adaptive("B4AFA4", "777168")

    // 分隔线
    static let line = adaptive("DFDCD3", "3B3833")
    static let lineSoft = adaptive("E9E6DF", "322F2A")

    // 深色对比卡片（黑卡 + 白字）语义色
    static let contrast = adaptive("1F1D1A", "2B2823")
    static let contrastSoft = adaptive("2A2722", "35312B")
    static let onContrast = Color(hex: "F6F3EA")
    /// 品牌亮色（lime）按钮上的文字色：浅/暗两种模式下都保持深墨色，保证对比度。
    static let onAccent = Color(hex: "26231F")

    // 品牌与点缀
    static let lime = Color(hex: "A3CD39")
    static let limeDeep = adaptive("7FA51D", "B7DE4B")
    static let limeSoft = adaptive("EAF2CE", "2F3A1B")
    static let butter = adaptive("F3E8BE", "3B321D")
    static let butterDeep = adaptive("D7BC62", "D7BC62")
    static let rose = adaptive("F0DEDA", "3B2925")
    static let roseDeep = adaptive("B77A6A", "D69A88")
    static let sky = adaptive("DDE9EF", "22303A")
    static let skyDeep = adaptive("6E8F9D", "93B7C6")
    static let peach = adaptive("F1E3C9", "3A3121")
    static let forest = adaptive("4E5B31", "C3D8A0")
    static let brown = adaptive("6B4A33", "C9A07C")
    static let dark = adaptive("1F1D1A", "0B0A09")
}

extension Font {
    static func display(_ size: CGFloat) -> Font { .system(size: size, weight: .semibold) }
    static func mono(_ size: CGFloat, weight: Font.Weight = .medium) -> Font {
        .system(size: size, weight: weight, design: .monospaced)
    }
}

struct PaperBackground: View {
    var body: some View {
        LinearGradient(
            colors: [C.paperSoft, C.paper],
            startPoint: .top,
            endPoint: .bottom
        )
        .ignoresSafeArea()
    }
}

struct BrewCard<Content: View>: View {
    var color: Color = C.card
    var corner: CGFloat = 26
    var border: Color? = C.lineSoft
    @ViewBuilder var content: Content

    var body: some View {
        content
            .background(color)
            .clipShape(RoundedRectangle(cornerRadius: corner, style: .continuous))
            .overlay {
                if let border {
                    RoundedRectangle(cornerRadius: corner, style: .continuous)
                        .stroke(border, lineWidth: 1)
                }
            }
    }
}

struct HorizontalRule: View {
    var color: Color = C.lineSoft
    var body: some View {
        Rectangle().fill(color).frame(height: 1)
    }
}

struct AdaptivePage<Content: View>: View {
    var maxWidth: CGFloat = 980
    /// 可选地把 ScrollViewProxy 交给页面（仅 Debug 截图时用于自动滚到底部）。
    var onScrollProxy: ((ScrollViewProxy) -> Void)? = nil
    let content: Content

    init(
        maxWidth: CGFloat = 980,
        onScrollProxy: ((ScrollViewProxy) -> Void)? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.maxWidth = maxWidth
        self.onScrollProxy = onScrollProxy
        self.content = content()
    }

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView(showsIndicators: false) {
                content
                    .frame(maxWidth: maxWidth, alignment: .leading)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.horizontal, 20)
                    .padding(.bottom, 24)
            }
            .background(PaperBackground())
            .onAppear { onScrollProxy?(proxy) }
        }
    }
}

func haptic(_ style: UIImpactFeedbackGenerator.FeedbackStyle = .light) {
    UIImpactFeedbackGenerator(style: style).impactOccurred()
}

// MARK: - Liquid Glass（iOS 26+，旧系统自动回退）
//
// 忆杯手记的部署目标是 iOS 17，但底部组件希望用上 iOS 26 的 Liquid Glass：
// 一种会折射后方内容、随触摸产生液态反馈的系统材质。这里统一封装成一层：
//   - iOS 26 及更新系统：系统 .glassEffect(...)
//   - iOS 17–25：回退到原本的 .ultraThinMaterial 毛玻璃质感，功能和布局完全不变。
//
// 玻璃只用在「控件层」（悬浮底栏、中央主按钮、底部操作条），不用作大面积内容背景，
// 避免实时模糊面积过大（iPad 侧栏因此保持 material）。

/// 生成 Liquid Glass 配置；只能在 iOS 26+ 的 `#available` 分支里调用。
/// 用 Xcode 16 及更早版本（Swift < 6.2，没有 iOS 26 SDK）编译时整段被剔除，
/// 工程会直接走毛玻璃回退，不会因为缺少 `Glass` 类型而编译失败。
#if compiler(>=6.2)
@available(iOS 26.0, *)
func liquidGlassStyle(tint: Color? = nil, interactive: Bool = false) -> Glass {
    var glass: Glass = .regular
    if let tint {
        glass = glass.tint(tint)
    }
    if interactive {
        glass = glass.interactive()
    }
    return glass
}
#endif

/// 把 Liquid Glass 应用到任意形状的底板上；旧系统回退到毛玻璃 + 描边 + 阴影。
struct LiquidGlassSurface<S: Shape>: ViewModifier {
    let shape: S
    var tint: Color?
    var interactive: Bool
    var fallbackMaterial: Material
    var fallbackBorder: Color?
    var fallbackShadowColor: Color?
    var fallbackShadowRadius: CGFloat
    var fallbackShadowY: CGFloat

    init(
        shape: S,
        tint: Color? = nil,
        interactive: Bool = false,
        fallbackMaterial: Material = .ultraThinMaterial,
        fallbackBorder: Color? = nil,
        fallbackShadowColor: Color? = nil,
        fallbackShadowRadius: CGFloat = 16,
        fallbackShadowY: CGFloat = 7
    ) {
        self.shape = shape
        self.tint = tint
        self.interactive = interactive
        self.fallbackMaterial = fallbackMaterial
        self.fallbackBorder = fallbackBorder
        self.fallbackShadowColor = fallbackShadowColor
        self.fallbackShadowRadius = fallbackShadowRadius
        self.fallbackShadowY = fallbackShadowY
    }

    @ViewBuilder
    func body(content: Content) -> some View {
        #if compiler(>=6.2)
        if #available(iOS 26.0, *) {
            content.glassEffect(liquidGlassStyle(tint: tint, interactive: interactive), in: shape)
        } else {
            legacySurface(content)
        }
        #else
        legacySurface(content)
        #endif
    }

    @ViewBuilder
    private func legacySurface(_ content: Content) -> some View {
        let base = content
            .background { shape.fill(fallbackMaterial) }
            .clipShape(shape)
            .overlay {
                if let fallbackBorder {
                    shape.stroke(fallbackBorder, lineWidth: 1)
                }
            }
        if let fallbackShadowColor {
            base.shadow(color: fallbackShadowColor, radius: fallbackShadowRadius, x: 0, y: fallbackShadowY)
        } else {
            base
        }
    }
}

extension View {
    /// 给底部导航条、底部操作条套上 Liquid Glass；旧系统回退毛玻璃。
    func liquidGlassSurface<S: Shape>(
        in shape: S,
        tint: Color? = nil,
        interactive: Bool = false,
        fallbackMaterial: Material = .ultraThinMaterial,
        fallbackBorder: Color? = nil,
        fallbackShadowColor: Color? = nil,
        fallbackShadowRadius: CGFloat = 16,
        fallbackShadowY: CGFloat = 7
    ) -> some View {
        modifier(
            LiquidGlassSurface(
                shape: shape,
                tint: tint,
                interactive: interactive,
                fallbackMaterial: fallbackMaterial,
                fallbackBorder: fallbackBorder,
                fallbackShadowColor: fallbackShadowColor,
                fallbackShadowRadius: fallbackShadowRadius,
                fallbackShadowY: fallbackShadowY
            )
        )
    }
}

/// 详情页 / 编辑页的底部主操作条。
///
/// - iOS 26+：悬浮圆角 Liquid Glass 条（左右留边、悬浮在内容之上），与系统玻璃工具条一致；
/// - iOS 17–25：保持原来的全宽 `.ultraThinMaterial` 贴边条，布局与以前完全一致。
struct BottomActionBar<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        if #available(iOS 26.0, *) {
            content
                .padding(.horizontal, 16)
                .padding(.top, 10)
                .padding(.bottom, 8)
                .liquidGlassSurface(in: RoundedRectangle(cornerRadius: 30, style: .continuous))
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
        } else {
            content
                .padding(.horizontal, 20)
                .padding(.top, 12)
                .padding(.bottom, 10)
                .background(.ultraThinMaterial)
        }
    }
}
