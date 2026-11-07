import Foundation

let CUP_CAPACITY = 420
let IMAGE_DIRECTORY = "bean_images"

enum Roast: String, Codable, CaseIterable, Identifiable, Hashable {
    case light = "LIGHT"
    case mediumLight = "MEDIUM_LIGHT"
    case medium = "MEDIUM"
    case mediumDark = "MEDIUM_DARK"
    case dark = "DARK"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .light: return "浅烘"
        case .mediumLight: return "中浅烘"
        case .medium: return "中烘"
        case .mediumDark: return "中深烘"
        case .dark: return "深烘"
        }
    }

    var short: String {
        switch self {
        case .light: return "浅"
        case .mediumLight: return "中浅"
        case .medium: return "中"
        case .mediumDark: return "中深"
        case .dark: return "深"
        }
    }

    var toneHex: String {
        switch self {
        case .light: return "DCC99A"
        case .mediumLight: return "C9A875"
        case .medium: return "AE8253"
        case .mediumDark: return "805535"
        case .dark: return "50301C"
        }
    }
}

enum Storage: String, Codable, CaseIterable, Identifiable, Hashable {
    case room = "ROOM"
    case fridge = "FRIDGE"
    case freezer = "FREEZER"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .room: return "常温"
        case .fridge: return "冷藏"
        case .freezer: return "冷冻"
        }
    }
}

enum IngredientKind: String, Codable, CaseIterable, Identifiable, Hashable {
    case liquid = "LIQUID"
    case foam = "FOAM"
    case syrup = "SYRUP"
    case puree = "PUREE"
    case chunk = "CHUNK"
    case ice = "ICE"
    case sparkling = "SPARKLING"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .liquid: return "液体"
        case .foam: return "奶泡"
        case .syrup: return "糖浆"
        case .puree: return "果泥"
        case .chunk: return "果肉"
        case .ice: return "冰块"
        case .sparkling: return "气泡"
        }
    }

    var defaultRate: Int {
        switch self {
        case .liquid: return 34
        case .foam: return 20
        case .syrup: return 12
        case .puree, .chunk: return 24
        case .ice: return 80
        case .sparkling: return 45
        }
    }
}

enum BeanStatus: Equatable {
    case resting, peak, stable, fading, frozen

    var label: String {
        switch self {
        case .resting: return "养豆中"
        case .peak: return "最佳赏味期"
        case .stable: return "风味稳定"
        case .fading: return "风味衰退期"
        case .frozen: return "冷冻中"
        }
    }

    var bgHex: String {
        switch self {
        case .resting: return "F3E8BE"
        case .peak: return "E7F0C7"
        case .stable: return "DDE9EE"
        case .fading: return "F0DED7"
        case .frozen: return "DCE7F2"
        }
    }

    var inkHex: String {
        switch self {
        case .resting: return "6E5B22"
        case .peak: return "536A1A"
        case .stable: return "3E5E6A"
        case .fading: return "7A4A37"
        case .frozen: return "3D5873"
        }
    }
}

struct Bean: Identifiable, Codable, Hashable {
    var id: String
    var name: String
    var roaster: String
    var origin: String
    var region: String
    var process: String
    var roast: Roast
    var flavors: [String]
    var totalGrams: Double
    var remainingGrams: Double
    var roastDate: Int64
    var openedAt: Int64
    var storage: Storage
    var imagePath: String?
    var note: String
    var createdAt: Int64
    var lastUsedAt: Int64

    static func empty() -> Bean {
        let now = nowMillis()
        return Bean(
            id: UUID().uuidString,
            name: "",
            roaster: "",
            origin: "",
            region: "",
            process: "水洗",
            roast: .medium,
            flavors: [],
            totalGrams: 150,
            remainingGrams: 150,
            roastDate: now,
            openedAt: now,
            storage: .room,
            imagePath: nil,
            note: "",
            createdAt: now,
            lastUsedAt: 0
        )
    }

    var displayOrigin: String { origin.isEmpty ? "未记录产地" : origin }
    var remainingRatio: Double {
        guard totalGrams > 0 else { return 0 }
        return min(max(remainingGrams / totalGrams, 0), 1)
    }

    var ageDays: Int {
        let calendar = Calendar.current
        let from = calendar.startOfDay(for: Date(timeIntervalSince1970: Double(roastDate) / 1000))
        let to = calendar.startOfDay(for: Date())
        return max(calendar.dateComponents([.day], from: from, to: to).day ?? 0, 0)
    }

    var status: BeanStatus {
        if storage == .freezer { return .frozen }
        let days = ageDays
        if days < 4 { return .resting }
        if days <= 30 { return .peak }
        if days <= 45 { return .stable }
        return .fading
    }

    var peakHint: String {
        switch status {
        case .resting: return "再养 \(max(4 - ageDays, 1)) 天"
        case .peak: return "第 \(ageDays) 天 · 正好喝"
        case .stable: return "第 \(ageDays) 天 · 尽快喝完"
        case .fading: return "已开 \(ageDays) 天"
        case .frozen: return "冷冻保存"
        }
    }
}

struct RecipeLayer: Codable, Hashable {
    var ingredientId: String
    var name: String
    var colorHex: String
    var kind: IngredientKind
    var grams: Int
    var alpha: Double
}

struct Recipe: Identifiable, Codable, Hashable {
    var id: String
    var name: String
    var beanId: String?
    var beanName: String
    var layers: [RecipeLayer]
    var rating: Int
    var notes: String
    var mouthfeel: String
    var sweetness: String
    var beanGrams: Int
    var stirred: Bool
    var createdAt: Int64
    /// 菜单价格（元）；nil / 0 表示未设置，旧数据没有这个字段时解码为 nil。
    var price: Int? = nil
    /// 1.2 视觉功能：每杯配方可保存一张横版宣传卡片的配置（可选，旧数据没有这个字段，解码为 nil）。
    var promoCard: PromoCardConfig? = nil

    var totalGrams: Int { layers.reduce(0) { $0 + $1.grams } }
}

struct CustomIngredient: Identifiable, Codable, Hashable {
    var id: String
    var name: String
    var colorHex: String
    var kind: IngredientKind
    var createdAt: Int64

    var spec: IngredientSpec {
        IngredientSpec(
            id: id,
            name: name,
            colorHex: colorHex,
            rate: kind.defaultRate,
            kind: kind,
            alpha: 1,
            isCustom: true
        )
    }
}

struct IngredientSpec: Identifiable, Hashable {
    var id: String
    var name: String
    var colorHex: String
    var rate: Int
    var kind: IngredientKind
    var alpha: Double = 1
    var isCustom: Bool = false
}

struct AppData: Codable {
    var schemaVersion: Int = 1
    var beans: [Bean] = []
    var recipes: [Recipe] = []
    var customIngredients: [CustomIngredient] = []

    var totalRemaining: Double { beans.reduce(0) { $0 + $1.remainingGrams } }

    func bean(_ id: String?) -> Bean? {
        guard let id else { return nil }
        return beans.first { $0.id == id }
    }

    func recipe(_ id: String) -> Recipe? {
        recipes.first { $0.id == id }
    }
}

struct MakerDraft {
    var layers: [RecipeLayer] = []
    var beanId: String?
    var stirred: Bool = false

    var totalGrams: Int { layers.reduce(0) { $0 + $1.grams } }
    var remaining: Int { max(CUP_CAPACITY - totalGrams, 0) }
}

let palette: [String] = [
    "2A1708", "6B4A33", "C08A3E", "E8C93E",
    "7E9C4F", "A3CD39", "E9A21B", "D8524A",
    "C97B84", "8C6A9E", "6F8FA6", "DDE6E3",
    "F3EDE0", "F7EED6", "F0DED7", "A85C17"
]

enum Ingredients {
    static let builtIn: [IngredientSpec] = [
        IngredientSpec(id: "espresso", name: "浓缩", colorHex: "2A1708", rate: 20, kind: .liquid),
        IngredientSpec(id: "coldbrew", name: "冷萃", colorHex: "3B2414", rate: 32, kind: .liquid),
        IngredientSpec(id: "water", name: "热水", colorHex: "D8C9AE", rate: 60, kind: .liquid, alpha: 0.55),
        IngredientSpec(id: "matcha", name: "抹茶", colorHex: "7E9C4F", rate: 28, kind: .liquid),
        IngredientSpec(id: "blacktea", name: "红茶", colorHex: "9C4A1C", rate: 55, kind: .liquid, alpha: 0.82),
        IngredientSpec(id: "oolong", name: "乌龙茶", colorHex: "C08A3E", rate: 55, kind: .liquid, alpha: 0.80),
        IngredientSpec(id: "milk", name: "牛奶", colorHex: "F1EADB", rate: 55, kind: .liquid),
        IngredientSpec(id: "oat", name: "燕麦奶", colorHex: "E8D9BC", rate: 48, kind: .liquid),
        IngredientSpec(id: "coconut", name: "椰奶", colorHex: "F3EDE0", rate: 48, kind: .liquid),
        IngredientSpec(id: "soy", name: "豆奶", colorHex: "EFE4CB", rate: 50, kind: .liquid),
        IngredientSpec(id: "foam", name: "奶泡", colorHex: "FAF5E9", rate: 22, kind: .foam),
        IngredientSpec(id: "cheesefoam", name: "芝士奶盖", colorHex: "F7EED6", rate: 18, kind: .foam),
        IngredientSpec(id: "cream", name: "奶油顶", colorHex: "FFF8E8", rate: 16, kind: .foam),
        IngredientSpec(id: "caramel", name: "焦糖糖浆", colorHex: "A85C17", rate: 12, kind: .syrup),
        IngredientSpec(id: "vanilla", name: "香草糖浆", colorHex: "E2BE7B", rate: 14, kind: .syrup),
        IngredientSpec(id: "choc", name: "巧克力酱", colorHex: "46280F", rate: 10, kind: .syrup),
        IngredientSpec(id: "osmanthus", name: "桂花糖浆", colorHex: "D9A441", rate: 13, kind: .syrup),
        IngredientSpec(id: "condensed", name: "炼乳", colorHex: "F0E2BE", rate: 11, kind: .syrup),
        IngredientSpec(id: "brownsugar", name: "黑糖糖浆", colorHex: "6B3A17", rate: 12, kind: .syrup),
        IngredientSpec(id: "hazelnut", name: "榛果糖浆", colorHex: "9A6B35", rate: 13, kind: .syrup),
        IngredientSpec(id: "honey", name: "蜂蜜", colorHex: "DFA231", rate: 10, kind: .syrup),
        IngredientSpec(id: "passion", name: "百香果", colorHex: "E9A21B", rate: 18, kind: .puree),
        IngredientSpec(id: "lemon", name: "柠檬", colorHex: "E8C93E", rate: 16, kind: .puree),
        IngredientSpec(id: "mango", name: "芒果", colorHex: "F2AC3C", rate: 30, kind: .chunk),
        IngredientSpec(id: "strawberry", name: "草莓", colorHex: "D8524A", rate: 26, kind: .chunk),
        IngredientSpec(id: "ice", name: "冰块", colorHex: "DDE6E3", rate: 90, kind: .ice, alpha: 0.55),
        IngredientSpec(id: "soda", name: "气泡水", colorHex: "E3E9E5", rate: 48, kind: .sparkling, alpha: 0.55)
    ]
}

func nowMillis() -> Int64 { Int64(Date().timeIntervalSince1970 * 1000) }

func formatGrams(_ value: Double) -> String {
    if value.rounded() == value { return "\(Int(value))g" }
    return String(format: "%.1fg", value)
}

func originFlag(_ origin: String) -> String {
    if origin.contains("埃塞") { return "🇪🇹" }
    if origin.contains("肯尼亚") { return "🇰🇪" }
    if origin.contains("卢旺达") { return "🇷🇼" }
    if origin.contains("哥伦比亚") { return "🇨🇴" }
    if origin.contains("巴西") { return "🇧🇷" }
    if origin.contains("危地马拉") { return "🇬🇹" }
    if origin.contains("哥斯达黎加") { return "🇨🇷" }
    if origin.contains("巴拿马") { return "🇵🇦" }
    if origin.contains("洪都拉斯") { return "🇭🇳" }
    if origin.contains("秘鲁") { return "🇵🇪" }
    if origin.contains("牙买加") { return "🇯🇲" }
    if origin.contains("云南") || origin.contains("中国") { return "🇨🇳" }
    if origin.contains("印尼") { return "🇮🇩" }
    if origin.contains("越南") { return "🇻🇳" }
    return "◌"
}

extension Int64 {
    var date: Date { Date(timeIntervalSince1970: Double(self) / 1000) }
}

extension Date {
    var millis: Int64 { Int64(timeIntervalSince1970 * 1000) }
    var dayLabel: String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "zh_CN")
        f.dateFormat = "M月d日"
        return f.string(from: self)
    }
    var shortTime: String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "zh_CN")
        f.dateFormat = "HH:mm"
        return f.string(from: self)
    }
}

extension Recipe {
    var localDate: Date { createdAt.date }
    var totalText: String { "\(totalGrams)g" }
    var dateTitle: String {
        let cal = Calendar.current
        if cal.isDateInToday(localDate) { return "今天 " + localDate.shortTime }
        if cal.isDateInYesterday(localDate) { return "昨天 " + localDate.shortTime }
        return localDate.dayLabel
    }
}

// MARK: - 宣传卡片数据模型（Foundation-only，供 macOS 数据层测试编译）

enum PromoCardTemplate: String, Codable, CaseIterable, Identifiable, Hashable {
    case magazine, poster, minimal

    var id: String { rawValue }

    var label: String {
        switch self {
        case .magazine: return "杂志"
        case .poster: return "海报"
        case .minimal: return "留白"
        }
    }

    var caption: String {
        switch self {
        case .magazine: return "左图右文，像一本咖啡杂志"
        case .poster: return "满版封面 + 大字标题"
        case .minimal: return "奶油纸底，安静留白"
        }
    }
}

enum PromoCardCoverKind: String, Codable, CaseIterable, Identifiable, Hashable {
    case cup, bean, photo, illustration, imported

    var id: String { rawValue }

    var label: String {
        switch self {
        case .cup: return "杯图"
        case .bean: return "豆插画"
        case .photo: return "相册图片"
        case .illustration: return "AI 插画"
        case .imported: return "相册导入"
        }
    }

    var symbol: String {
        switch self {
        case .cup: return "cup.and.saucer"
        case .bean: return "leaf"
        case .photo: return "photo"
        case .illustration: return "sparkles"
        case .imported: return "photo.badge.arrow.down"
        }
    }

    /// 卡片编辑器只展示这 4 种来源；`.photo` 仅用于兼容旧版保存的卡片配置。
    static var pickerCases: [PromoCardCoverKind] { [.illustration, .imported, .cup, .bean] }
}

/// AI 插画的来源（持久化字段，便于以后扩展本地上传等来源）。
enum PromoCardIllustrationSource: String, Codable, Hashable {
    case ai
    /// 相册导入 + Vision 自动抠图（透明背景 PNG）。
    case imported
}

enum PromoCardRatio: String, Codable, CaseIterable, Identifiable, Hashable {
    case wide, classic, portrait

    var id: String { rawValue }

    var label: String {
        switch self {
        case .wide: return "A6 横版"
        case .classic: return "3:2 横版"
        case .portrait: return "A6 竖版"
        }
    }

    var isPortrait: Bool { self == .portrait }

}

/// 导出尺寸：社交平台 / A6 300dpi 打印（咖啡店随杯卡）。
enum PromoCardExportSize: String, Codable, CaseIterable, Identifiable, Hashable {
    case social, printA6

    var id: String { rawValue }

    var label: String {
        switch self {
        case .social: return "社交尺寸"
        case .printA6: return "A6 打印 300dpi"
        }
    }

    var caption: String {
        switch self {
        case .social: return "导出分享图"
        case .printA6: return "咖啡店随杯卡可打印"
        }
    }
}

/// 随杯卡可编辑的元素（全部可显示/隐藏、可拖动、文字可缩放/对齐）。
enum PromoCardElement: String, Codable, CaseIterable, Identifiable, Hashable {
    case category, roastTags, price, englishName, chineseName
    case ingredients, flavorReference, keywords, illustration

    var id: String { rawValue }

    var label: String {
        switch self {
        case .category: return "分类/产地"
        case .roastTags: return "烘焙度/处理法"
        case .price: return "价格"
        case .englishName: return "英文名"
        case .chineseName: return "中文名"
        case .ingredients: return "配料行"
        case .flavorReference: return "风味参考"
        case .keywords: return "底部关键词"
        case .illustration: return "插画/图片"
        }
    }

    var isText: Bool { self != .illustration }
}

/// 单个元素在卡面上的归一化布局（0–1，适配 A6/社交任意导出尺寸）。
struct PromoCardElementLayout: Codable, Hashable {
    /// 元素中心点（相对卡宽/卡高）。
    var centerX: Double
    var centerY: Double
    /// 相对自动布局尺寸的缩放。
    var scale: Double = 1
    /// 自动布局时的宽度（归一化），用于保持换行；nil 时按内容自然宽度。
    var width: Double? = nil
    /// 自动布局时的高度（归一化）。
    var height: Double? = nil
    /// 文字对齐：left / center / right。
    var alignment: String? = nil
}

/// 各元素自定义字号（设计点，基于 640pt 宽卡面；8–72pt，横竖版分别保存）。
struct PromoCardFontSizes: Codable, Hashable {
    var portrait: [String: Double] = [:]
    var landscape: [String: Double] = [:]

    func sizes(for ratio: PromoCardRatio) -> [String: Double] {
        ratio.isPortrait ? portrait : landscape
    }

    mutating func setSizes(_ sizes: [String: Double], for ratio: PromoCardRatio) {
        if ratio.isPortrait { portrait = sizes } else { landscape = sizes }
    }

    var isEmpty: Bool { portrait.isEmpty && landscape.isEmpty }
}

/// 横竖版分别保存的手动排版；旧 JSON 没有这个字段时为 nil。
struct PromoCardManualLayout: Codable, Hashable {
    var portrait: [String: PromoCardElementLayout] = [:]
    var landscape: [String: PromoCardElementLayout] = [:]

    func layouts(for ratio: PromoCardRatio) -> [String: PromoCardElementLayout] {
        ratio.isPortrait ? portrait : landscape
    }

    mutating func setLayouts(_ layouts: [String: PromoCardElementLayout], for ratio: PromoCardRatio) {
        if ratio.isPortrait { portrait = layouts } else { landscape = layouts }
    }

    var isEmpty: Bool { portrait.isEmpty && landscape.isEmpty }
}

struct PromoCardConfig: Codable, Hashable {
    var template: PromoCardTemplate = .magazine
    var ratio: PromoCardRatio = .wide
    var cover: PromoCardCoverKind = .cup
    /// 相册封面保存在 App 私有图片目录里的相对路径（bean_images/xxx.png）。
    var coverImagePath: String?
    /// AI 生成的咖啡插画：私有图片目录相对路径 / 来源 / 生成时的 prompt。
    /// 全部是可选字段，旧版本保存的卡片 JSON（没有这几个键）仍可正常解码。
    var illustrationImagePath: String?
    /// AI 插画原图（未处理）路径：保留用于重新抠图 / 重新去水印。
    var illustrationRawImagePath: String?
    /// AI 插画处理说明（抠图结果 / 去水印数量 / 回退原因）。
    var illustrationProcessNote: String?
    /// 本地清理掉的 AI 水印 / logo 连通块数量。
    var illustrationRemovedWatermarkBlocks: Int?
    /// 从实物图生成插画时保存的实物照片路径与视觉/本地生成的 prompt（可选，旧 JSON 兼容）。
    var illustrationPhotoImagePath: String?
    var illustrationPhotoPrompt: String?
    /// prompt 来源标记：vision（图片理解 API）/ local（照片主色+配方降级）/ recipe（配方自动）。
    var illustrationPromptSource: String?
    var illustrationSource: PromoCardIllustrationSource?
    var illustrationPrompt: String?
    /// 相册导入（自动抠图）：透明背景 PNG 路径 / 原图路径 / 是否抠图成功。
    var importedImagePath: String?
    var importedOriginalImagePath: String?
    var importedCutout: Bool?
    /// 主视觉是否加柔和阴影（默认关，参考手绘卡直接叠在纸上）。
    var illustrationShadow: Bool?
    // ——— 卡片编辑器 v3：所有元素可手动编辑（nil 时按配方自动生成）———
    var categoryOverride: String?
    var roastTagsOverride: [String]?
    var ingredientsChineseOverride: String?
    var ingredientsEnglishOverride: String?
    var flavorReferenceOverride: String?
    var flavorEnglishOverride: String?
    var keywordsOverride: [String]?
    /// 隐藏的元素（旧 JSON 没有 = 全部显示）。
    var hiddenElements: [PromoCardElement]?
    /// 横/竖版手动位置与缩放；nil = 完全自动排版。
    var manualLayout: PromoCardManualLayout?
    /// 各元素自定义字号（8–72pt，横竖版分别保存；nil = 使用默认字号）。
    var fontSizes: PromoCardFontSizes?
    /// 导出尺寸（社交 / A6 打印）；旧 JSON 缺省为社交尺寸。
    var exportSize: PromoCardExportSize?
    /// 随杯卡上的大号英文名；nil 时按配方自动推导（如 Lychee Cold Brew）。
    var englishName: String?
    var title: String
    var subtitle: String
    var tags: [String]
    var themeHex: String = "A3CD39"
    var updatedAt: Int64 = 0

    var effectiveExportSize: PromoCardExportSize { exportSize ?? .social }

    /// 根据配方生成一张默认卡片：标题用配方名，副标题用豆子/口感，标签来自豆子风味与口感甜度。
    static func makeDefault(for recipe: Recipe, bean: Bean?) -> PromoCardConfig {
        var tags: [String] = []
        if let bean {
            tags.append(contentsOf: bean.flavors.prefix(2))
        }
        tags.append(recipe.mouthfeel)
        tags.append(recipe.sweetness)
        tags = tags.filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
        if tags.isEmpty { tags = ["手冲", "今日特调"] }

        var subtitleParts: [String] = []
        if let bean, !bean.name.isEmpty {
            subtitleParts.append(bean.name)
        } else if !recipe.beanName.isEmpty {
            subtitleParts.append(recipe.beanName)
        }
        subtitleParts.append(recipe.mouthfeel)
        subtitleParts.append(recipe.sweetness)
        let subtitle = subtitleParts.joined(separator: " · ")

        return PromoCardConfig(
            template: .magazine,
            ratio: .portrait,
            cover: .cup,
            coverImagePath: nil,
            illustrationImagePath: nil,
            illustrationSource: nil,
            illustrationPrompt: nil,
            exportSize: .printA6,
            title: recipe.name,
            subtitle: subtitle,
            tags: Array(tags.prefix(4)),
            themeHex: "A3CD39",
            updatedAt: nowMillis()
        )
    }
}


