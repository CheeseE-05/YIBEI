import Foundation
import SwiftUI

// MARK: - 宣传卡片数据模型
//
// 每个配方可以保存一张横版宣传卡片（社交随杯卡）的设计配置。配置存在 Recipe.promoCard
// 可选字段里：旧版本的 data.json 没有这个字段时会解码成 nil，再用默认值生成一张卡片，
// 因此完全兼容 1.2 及更早的本地数据 / 导入数据。

extension PromoCardRatio {
    /// 宽 / 高（社交尺寸的版式比例）。
    var value: CGFloat {
        switch self {
        case .wide: return 16.0 / 9.0
        case .classic: return 3.0 / 2.0
        case .portrait: return 3.0 / 4.0
        }
    }

    /// 编辑器的比例选择：横版 16:9 + 竖版 3:4（classic 仅兼容旧保存数据）。
    static var pickerCases: [PromoCardRatio] { [.wide, .portrait] }

    /// 导出 PNG 的像素尺寸：社交尺寸 / A6 300dpi 打印尺寸。
    func pixelSize(for output: PromoCardExportSize) -> CGSize {
        switch (self, output) {
        case (.portrait, .social):
            return CGSize(width: 1200, height: 1600)
        case (.portrait, .printA6):
            return CGSize(width: 1240, height: 1748)
        case (.classic, .social):
            return CGSize(width: 1200, height: 800)
        case (.classic, .printA6):
            return CGSize(width: 1748, height: 1240)
        case (.wide, .social):
            return CGSize(width: 1200, height: 675)
        case (.wide, .printA6):
            return CGSize(width: 1748, height: 1240)
        }
    }

    /// 预览用的宽高比（跟随导出尺寸：A6 打印尺寸比社交尺寸略方）。
    func aspectRatio(for output: PromoCardExportSize) -> CGFloat {
        let size = pixelSize(for: output)
        return size.width / size.height
    }
}

/// 卡片可选的品牌主题色（与 App 色板同源，保证「设计语言统一」）。
struct PromoCardThemePreset: Identifiable, Hashable {
    let name: String
    let hex: String
    var id: String { hex }
}

enum PromoCardThemes {
    static let presets: [PromoCardThemePreset] = [
        PromoCardThemePreset(name: "青柠", hex: "A3CD39"),
        PromoCardThemePreset(name: "咖啡", hex: "6B4A33"),
        PromoCardThemePreset(name: "墨色", hex: "26231F"),
        PromoCardThemePreset(name: "奶油", hex: "D7BC62"),
        PromoCardThemePreset(name: "玫瑰", hex: "B77A6A"),
        PromoCardThemePreset(name: "天空", hex: "6E8F9D"),
        PromoCardThemePreset(name: "抹茶", hex: "7E9C4F"),
        PromoCardThemePreset(name: "南瓜", hex: "C08A3E")
    ]
}

// MARK: - 卡片专用固定色板
//
// 宣传卡片要发到社交平台，导出后的颜色必须稳定、不随 App 的浅色/深色模式变化，
// 所以卡片内部使用一组固定的奶油纸色板，而不是 App 的动态 C 色板。

enum PromoCardPalette {
    static let paper = Color(hex: "F5F1E6")
    static let paperSoft = Color(hex: "FBF8F0")
    static let ink = Color(hex: "2A2723")
    static let inkSoft = Color(hex: "5C564C")
    static let muted = Color(hex: "8E8779")
    static let cream = Color(hex: "F0E9D9")
    static let line = Color(hex: "D8CEB8")
    static let night = Color(hex: "1F1D1A")
}

// MARK: - Color <-> 十六进制（设计工具的自定义主题色用）

extension Color {
    var hexString: String {
        let ui = UIColor(self).resolvedColor(with: UITraitCollection.current)
        var r: CGFloat = 0
        var g: CGFloat = 0
        var b: CGFloat = 0
        var a: CGFloat = 0
        guard ui.getRed(&r, green: &g, blue: &b, alpha: &a) else { return "A3CD39" }
        return String(
            format: "%02X%02X%02X",
            Int(round(r * 255)),
            Int(round(g * 255)),
            Int(round(b * 255))
        )
    }
}

/// 标签输入框用：把「荔枝, 白葡萄，清爽」这类文本拆成标签数组。
func parsePromoCardTags(_ text: String) -> [String] {
    text
        .split(whereSeparator: { $0 == "," || $0 == "，" || $0 == "、" || $0 == " " || $0 == "\n" })
        .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
        .filter { !$0.isEmpty }
}

// MARK: - 随杯卡内容推导（菜系 / 冷热 / 配料中英文 / 风味关键词 / 英文名）

/// 配方 → 小分类（拿铁/冷萃/特调…），用于卡片顶部左侧。
func promoDrinkCategory(for recipe: Recipe) -> String {
    let ids = Set(recipe.layers.map(\.ingredientId))
    if ids.contains("matcha") { return "抹茶拿铁" }
    if ids.contains("coldbrew") { return "冷萃咖啡" }
    if ids.contains("soda") || ids.contains("sparkling") { return "气泡特调" }
    if ids.contains("espresso"), ids.contains("milk") || ids.contains("oat") || ids.contains("coconut") || ids.contains("soy") {
        return ids.contains("foam") || ids.contains("cheesefoam") || ids.contains("cream") ? "奶咖特调" : "意式拿铁"
    }
    if ids.contains("espresso") { return "意式浓缩" }
    if ids.contains("water") || ids.contains("blacktea") || ids.contains("oolong") { return "手冲咖啡" }
    return "特调咖啡"
}

func promoDrinkCategoryEnglish(for recipe: Recipe) -> String {
    let category = promoDrinkCategory(for: recipe)
    switch category {
    case "抹茶拿铁": return "Matcha Latte"
    case "冷萃咖啡": return "Cold Brew"
    case "气泡特调": return "Sparkling Special"
    case "意式拿铁": return "Latte"
    case "奶咖特调": return "Signature Latte"
    case "意式浓缩": return "Espresso"
    case "手冲咖啡": return "Pour Over"
    default: return "Signature Drink"
    }
}

/// 只含大量冰/冷萃/气泡的配方视为冷饮，其余为热饮（用于插画 prompt 与杯型描述）。
func promoIsIced(_ recipe: Recipe) -> Bool {
    let ids = Set(recipe.layers.map(\.ingredientId))
    return ids.contains("ice") || ids.contains("coldbrew") || ids.contains("soda") || ids.contains("sparkling")
}

/// 卡片顶部右侧的烘焙度 / 处理法（处理法带英文缩写，贴近参考图）。
func promoProcessEnglish(_ process: String) -> String {
    let clean = process.trimmingCharacters(in: .whitespacesAndNewlines)
    if clean.contains("水洗") { return "WASHED" }
    if clean.contains("日晒") || clean.contains("自然") { return "NATURAL" }
    if clean.contains("蜜") { return "HONEY" }
    if clean.contains("厌氧") { return "ANAEROBIC" }
    if clean.contains("湿刨") { return "WET HULLED" }
    if clean.contains("酒桶") || clean.contains("桶") { return "BARREL" }
    return clean.isEmpty ? "PROCESS" : clean.uppercased()
}

/// 配料中文行，如「冷萃 · 牛奶 · 冰块」。
func promoIngredientsChinese(for recipe: Recipe) -> String {
    let names = recipe.layers.map(\.name).filter { !$0.isEmpty }
    return names.isEmpty ? "咖啡" : names.joined(separator: " + ")
}

private let promoIngredientEnglishMap: [String: String] = [
    "espresso": "Espresso", "coldbrew": "Cold Brew", "water": "Hot Water", "matcha": "Matcha",
    "blacktea": "Black Tea", "oolong": "Oolong Tea", "milk": "Milk", "oat": "Oat Milk",
    "coconut": "Coconut Milk", "soy": "Soy Milk", "foam": "Milk Foam", "cheesefoam": "Cheese Foam",
    "cream": "Cream", "caramel": "Caramel Syrup", "vanilla": "Vanilla Syrup", "choc": "Chocolate",
    "osmanthus": "Osmanthus Syrup", "condensed": "Condensed Milk", "brownsugar": "Brown Sugar",
    "hazelnut": "Hazelnut Syrup", "honey": "Honey", "passion": "Passion Fruit", "lemon": "Lemon",
    "mango": "Mango", "strawberry": "Strawberry", "ice": "Ice", "soda": "Sparkling Water",
    "sparkling": "Sparkling Water"
]

/// 配料英文行，如「Cold Brew · Milk · Ice」。
func promoIngredientsEnglish(for recipe: Recipe) -> String {
    let names = recipe.layers.compactMap { layer -> String? in
        if let english = promoIngredientEnglishMap[layer.ingredientId], !english.isEmpty { return english }
        return layer.name.isEmpty ? nil : layer.name
    }
    return names.isEmpty ? "Coffee" : names.joined(separator: " + ")
}

/// 风味关键词：豆子风味前 2 个 + 口感 + 甜度（去重，最多 4 个）。
func promoFlavorKeywords(recipe: Recipe, bean: Bean?) -> [String] {
    var keywords: [String] = []
    if let bean { keywords.append(contentsOf: bean.flavors.prefix(2)) }
    keywords.append(recipe.mouthfeel)
    keywords.append(recipe.sweetness)
    var seen = Set<String>()
    return keywords
        .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
        .filter { !$0.isEmpty && seen.insert($0).inserted }
        .prefix(4)
        .map { $0 }
}

private let promoFlavorEnglishMap: [String: String] = [
    "茉莉": "Jasmine", "柠檬": "Lemon", "红茶感": "Black Tea", "荔枝": "Lychee",
    "白葡萄": "White Grape", "焦糖": "Caramel", "巧克力": "Chocolate", "坚果": "Nutty",
    "玫瑰": "Rose", "桂花": "Osmanthus", "蜂蜜": "Honey", "杏子": "Apricot",
    "西柚": "Grapefruit", "柑橘": "Citrus", "莓果": "Berries", "香草": "Vanilla",
    "抹茶": "Matcha", "奶油": "Cream", "黑巧": "Dark Chocolate", "可可": "Cocoa",
    "莓": "Berry", "桃": "Peach", "葡萄": "Grape", "苹果": "Apple", "乌龙": "Oolong",
    "清爽": "Refreshing", "均衡": "Balanced", "浓郁": "Rich",
    "微甜": "Lightly Sweet", "甜感适中": "Medium Sweet", "明显偏甜": "Sweet"
]

/// 风味关键词英文行（找不到映射的中文词会被跳过，保证英文行可读）。
func promoFlavorEnglish(recipe: Recipe, bean: Bean?) -> String {
    let english = promoFlavorKeywords(recipe: recipe, bean: bean).compactMap { keyword -> String? in
        if let mapped = promoFlavorEnglishMap[keyword] { return mapped }
        for (chinese, value) in promoFlavorEnglishMap where keyword.contains(chinese) {
            return value
        }
        return nil
    }
    return english.joined(separator: ", ")
}

/// 英文饮品名（可被卡片配置里的 englishName 覆盖）：如「Lychee Cold Brew」。
func promoDefaultEnglishName(recipe: Recipe, bean: Bean?) -> String {
    let flavorEnglish = promoFlavorKeywords(recipe: recipe, bean: bean)
        .compactMap { keyword -> String? in
            if let mapped = promoFlavorEnglishMap[keyword], !["Refreshing", "Balanced", "Rich"].contains(mapped),
               !["Lightly Sweet", "Medium Sweet", "Sweet"].contains(mapped) {
                return mapped
            }
            return nil
        }
        .prefix(2)
    let prefix = flavorEnglish.joined(separator: " ")
    let category = promoDrinkCategoryEnglish(for: recipe)
    return prefix.isEmpty ? category : "\(prefix) \(category)"
}

/// 顶部右侧的小标签行：烘焙度 + 处理法。
func promoBeanTags(bean: Bean?) -> [String] {
    guard let bean else { return [] }
    var tags = [bean.roast.label]
    if !bean.process.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
        tags.append("△\(bean.process) \(promoProcessEnglish(bean.process))")
    }
    return tags
}

/// 顶部左侧的产地小行：如「埃塞俄比亚 · Guji」。
func promoOriginLine(bean: Bean?) -> String? {
    guard let bean else { return nil }
    let parts = [bean.origin, bean.region].filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    return parts.isEmpty ? nil : parts.joined(separator: " · ")
}


// MARK: - 卡片字号（设计点：640pt 宽卡面为 1x；8–72pt）

/// 当前卡片在 640pt 设计宽度下的设计高度（用于把 h 比例换算成设计字号）。
func promoCardDesignHeight(for config: PromoCardConfig) -> CGFloat {
    let aspect = config.ratio.aspectRatio(for: config.effectiveExportSize)
    return aspect > 0 ? 640 / aspect : 902
}

/// 各元素默认字号（设计点）。与卡片原有字号比例一一对应，默认视觉不变。
func promoDefaultFontSize(for element: PromoCardElement, config: PromoCardConfig) -> CGFloat {
    let h = promoCardDesignHeight(for: config)
    switch element {
    case .category: return h * 0.0145
    case .roastTags: return h * 0.012
    case .price: return h * 0.038
    case .englishName: return min(h * 0.076, 64)
    case .chineseName: return h * 0.030
    case .ingredients: return h * 0.0148
    case .flavorReference: return h * 0.0138
    case .keywords: return h * 0.0132
    case .illustration: return 0
    }
}

/// 当前元素实际生效字号（自定义优先，否则默认）；插画返回 0。
func promoFontSize(for element: PromoCardElement, config: PromoCardConfig) -> CGFloat {
    if let custom = config.fontSizes?.sizes(for: config.ratio)[element.rawValue] {
        return CGFloat(min(max(custom, 8), 72))
    }
    return promoDefaultFontSize(for: element, config: config)
}
