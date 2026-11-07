import Foundation
import UIKit

// MARK: - AI 设置
//
// 使用 OpenAI 兼容的 chat/completions 接口：用户只填 Base URL / API Key / 模型名，
// 所以 OpenAI、DeepSeek、Moonshot、Ollama、LM Studio 等兼容服务都能直接用。
// 所有设置存在 UserDefaults（@AppStorage），随 App 本机保存，不会上传到别处。

/// 插画 Provider 预设（OpenAI 兼容 images/generations）。
///
/// 默认智谱 BigModel 的 GLM-Image（约 0.1 元/次，国内可直连，官方文档推荐用 URL 返回图片）。
/// 响应是 {"data":[{"url":"..."}]}（url 需要下载成 Data 再存 App 私有目录）；
/// 第三方网关也可能返回 data[0].b64_json，两种都兼容。
enum AIImageProviderPreset: String, CaseIterable, Identifiable, Hashable {
    case bigmodel, siliconflow, custom

    var id: String { rawValue }

    var label: String {
        switch self {
        case .bigmodel: return "智谱 BigModel · GLM-Image"
        case .siliconflow: return "硅基流动"
        case .custom: return "自定义"
        }
    }

    var caption: String {
        switch self {
        case .bigmodel: return "GLM-Image · 约 0.1 元/次 · 推荐"
        case .siliconflow: return "Kwai-Kolors/Kolors · 价格便宜"
        case .custom: return "任意 OpenAI 兼容网关"
        }
    }

    var baseURL: String {
        switch self {
        case .bigmodel: return "https://open.bigmodel.cn/api/paas/v4"
        case .siliconflow: return "https://api.siliconflow.cn/v1"
        case .custom: return ""
        }
    }

    var model: String {
        switch self {
        case .bigmodel: return "glm-image"
        case .siliconflow: return "Kwai-Kolors/Kolors"
        case .custom: return ""
        }
    }
}

/// 文案 Provider 预设（OpenAI 兼容 chat/completions）。
enum AITextProviderPreset: String, CaseIterable, Identifiable, Hashable {
    case openai, deepseek, custom

    var id: String { rawValue }

    var label: String {
        switch self {
        case .openai: return "OpenAI"
        case .deepseek: return "DeepSeek"
        case .custom: return "自定义"
        }
    }

    var baseURL: String {
        switch self {
        case .openai: return "https://api.openai.com/v1"
        case .deepseek: return "https://api.deepseek.com/v1"
        case .custom: return ""
        }
    }

    var model: String {
        switch self {
        case .openai: return "gpt-4o-mini"
        case .deepseek: return "deepseek-chat"
        case .custom: return ""
        }
    }
}

struct AISettings {
    // 文案 API（chat/completions，可选：卡片也可以纯手动设计）
    var baseURL: String
    var apiKey: String
    var model: String

    // 插画 API（images/generations）
    var imageBaseURL: String
    var imageAPIKey: String
    var imageModel: String
    var imageSize: String

    // 图片理解 API（chat/completions + image_url，用于「从实物图生成插画」）
    var visionBaseURL: String
    var visionAPIKey: String
    var visionModel: String

    static let baseURLKey = "aiBaseURL"
    static let apiKeyKey = "aiAPIKey"
    static let modelKey = "aiModel"
    static let imageBaseURLKey = "aiImageBaseURL"
    static let imageAPIKeyKey = "aiImageAPIKey"
    static let imageModelKey = "aiImageModel"
    static let imageSizeKey = "aiImageSize"
    static let visionBaseURLKey = "aiVisionBaseURL"
    static let visionAPIKeyKey = "aiVisionAPIKey"
    static let visionModelKey = "aiVisionModel"

    static let defaultBaseURL = "https://api.openai.com/v1"
    static let defaultModel = "gpt-4o-mini"
    static let defaultImageModel = "glm-image"
    static let defaultImageBaseURL = "https://open.bigmodel.cn/api/paas/v4"
    /// 默认 auto：按卡片比例自动选 1024×1440（竖）/ 1440×1024（横）；也可填自定义尺寸。
    static let defaultImageSize = "auto"
    static let defaultVisionBaseURL = "https://open.bigmodel.cn/api/paas/v4"
    static let defaultVisionModel = "glm-4v-flash"

    var trimmedBaseURL: String { baseURL.trimmingCharacters(in: .whitespacesAndNewlines) }
    var trimmedAPIKey: String { apiKey.trimmingCharacters(in: .whitespacesAndNewlines) }
    var trimmedModel: String { model.trimmingCharacters(in: .whitespacesAndNewlines) }

    var trimmedImageBaseURL: String { imageBaseURL.trimmingCharacters(in: .whitespacesAndNewlines) }
    var trimmedImageAPIKey: String { imageAPIKey.trimmingCharacters(in: .whitespacesAndNewlines) }
    var trimmedImageModel: String { imageModel.trimmingCharacters(in: .whitespacesAndNewlines) }
    var trimmedImageSize: String {
        let size = imageSize.trimmingCharacters(in: .whitespacesAndNewlines)
        return size.isEmpty ? Self.defaultImageSize : size
    }

    var trimmedVisionBaseURL: String { visionBaseURL.trimmingCharacters(in: .whitespacesAndNewlines) }
    var trimmedVisionAPIKey: String { visionAPIKey.trimmingCharacters(in: .whitespacesAndNewlines) }
    var trimmedVisionModel: String { visionModel.trimmingCharacters(in: .whitespacesAndNewlines) }

    /// GLM-Image 尺寸校验：512–2048、32 的倍数（如 1024×1440 / 1440×1024 / 1280×1280）；
    /// "auto"/空/非法值返回 nil。
    static func normalizedImageSize(_ raw: String) -> String? {
        let text = raw
            .lowercased()
            .replacingOccurrences(of: "×", with: "x")
            .replacingOccurrences(of: " ", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty, text != "auto" else { return nil }
        let parts = text.split(separator: "x")
        guard parts.count == 2,
              let width = Int(parts[0]),
              let height = Int(parts[1]),
              (512...2048).contains(width),
              (512...2048).contains(height),
              width % 32 == 0,
              height % 32 == 0 else { return nil }
        return "\(width)x\(height)"
    }

    /// 文案 API 是否可用（可留空不用）。
    var isConfigured: Bool {
        !trimmedBaseURL.isEmpty && !trimmedAPIKey.isEmpty && !trimmedModel.isEmpty
    }

    /// 插画 API 是否可用（Key 为空时视为未配置，插画功能禁用并引导去设置）。
    var isImageConfigured: Bool {
        !trimmedImageBaseURL.isEmpty && !trimmedImageAPIKey.isEmpty && !trimmedImageModel.isEmpty
    }

    /// 插画 Base URL：单独配置为空时回退到文案 Base URL（方便只填一套配置的网关）。
    var effectiveImageBaseURL: String {
        trimmedImageBaseURL.isEmpty ? trimmedBaseURL : trimmedImageBaseURL
    }

    var effectiveImageAPIKey: String {
        trimmedImageAPIKey.isEmpty ? trimmedAPIKey : trimmedImageAPIKey
    }

    var effectiveImageModel: String {
        trimmedImageModel.isEmpty ? Self.defaultImageModel : trimmedImageModel
    }

    var effectiveImageSize: String {
        trimmedImageSize.isEmpty ? Self.defaultImageSize : trimmedImageSize
    }

    /// 图片理解 API：Base/模型默认智谱 GLM-4V-Flash；Key 留空时复用「文案 API」的 Key（也可独立填写）。
    var effectiveVisionBaseURL: String {
        trimmedVisionBaseURL.isEmpty ? Self.defaultVisionBaseURL : trimmedVisionBaseURL
    }

    var effectiveVisionAPIKey: String {
        trimmedVisionAPIKey.isEmpty ? trimmedAPIKey : trimmedVisionAPIKey
    }

    var effectiveVisionModel: String {
        trimmedVisionModel.isEmpty ? Self.defaultVisionModel : trimmedVisionModel
    }

    var isVisionConfigured: Bool {
        !effectiveVisionBaseURL.isEmpty && !effectiveVisionAPIKey.isEmpty
    }

    static func current(from defaults: UserDefaults = .standard) -> AISettings {
        AISettings(
            baseURL: defaults.string(forKey: baseURLKey) ?? defaultBaseURL,
            apiKey: defaults.string(forKey: apiKeyKey) ?? "",
            model: defaults.string(forKey: modelKey) ?? defaultModel,
            imageBaseURL: defaults.string(forKey: imageBaseURLKey) ?? defaultImageBaseURL,
            imageAPIKey: defaults.string(forKey: imageAPIKeyKey) ?? "",
            imageModel: defaults.string(forKey: imageModelKey) ?? defaultImageModel,
            imageSize: defaults.string(forKey: imageSizeKey) ?? defaultImageSize,
            visionBaseURL: defaults.string(forKey: visionBaseURLKey) ?? defaultVisionBaseURL,
            visionAPIKey: defaults.string(forKey: visionAPIKeyKey) ?? "",
            visionModel: defaults.string(forKey: visionModelKey) ?? defaultVisionModel
        )
    }

    static func save(_ settings: AISettings) {
        let defaults = UserDefaults.standard
        defaults.set(settings.trimmedBaseURL.isEmpty ? defaultBaseURL : settings.trimmedBaseURL, forKey: baseURLKey)
        defaults.set(settings.trimmedAPIKey, forKey: apiKeyKey)
        defaults.set(settings.trimmedModel.isEmpty ? defaultModel : settings.trimmedModel, forKey: modelKey)
        defaults.set(
            settings.trimmedImageBaseURL.isEmpty ? defaultImageBaseURL : settings.trimmedImageBaseURL,
            forKey: imageBaseURLKey
        )
        defaults.set(settings.trimmedImageAPIKey, forKey: imageAPIKeyKey)
        defaults.set(settings.trimmedImageModel.isEmpty ? defaultImageModel : settings.trimmedImageModel, forKey: imageModelKey)
        defaults.set(settings.trimmedImageSize, forKey: imageSizeKey)
        defaults.set(
            settings.trimmedVisionBaseURL.isEmpty ? defaultVisionBaseURL : settings.trimmedVisionBaseURL,
            forKey: visionBaseURLKey
        )
        defaults.set(settings.trimmedVisionAPIKey, forKey: visionAPIKeyKey)
        defaults.set(
            settings.trimmedVisionModel.isEmpty ? defaultVisionModel : settings.trimmedVisionModel,
            forKey: visionModelKey
        )
    }

    static func resetText() {
        let defaults = UserDefaults.standard
        defaults.set(defaultBaseURL, forKey: baseURLKey)
        defaults.set("", forKey: apiKeyKey)
        defaults.set(defaultModel, forKey: modelKey)
    }

    static func resetImage() {
        let defaults = UserDefaults.standard
        defaults.set(defaultImageBaseURL, forKey: imageBaseURLKey)
        defaults.set("", forKey: imageAPIKeyKey)
        defaults.set(defaultImageModel, forKey: imageModelKey)
        defaults.set(defaultImageSize, forKey: imageSizeKey)
    }

    static func resetVision() {
        let defaults = UserDefaults.standard
        defaults.set(defaultVisionBaseURL, forKey: visionBaseURLKey)
        defaults.set("", forKey: visionAPIKeyKey)
        defaults.set(defaultVisionModel, forKey: visionModelKey)
    }

    static func reset() {
        resetText()
        resetImage()
        resetVision()
    }
}

// MARK: - AI 返回的卡片文案

struct AIGeneratedCopy: Codable, Hashable {
    var title: String
    var subtitle: String
    var tags: [String]
    var theme: String?
    var template: String?

    /// 转成卡片配置；theme / template 非法时保留原来的值。
    func applying(to base: PromoCardConfig) -> PromoCardConfig {
        var config = base
        let cleanTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanSubtitle = subtitle.trimmingCharacters(in: .whitespacesAndNewlines)
        if !cleanTitle.isEmpty { config.title = String(cleanTitle.prefix(40)) }
        if !cleanSubtitle.isEmpty { config.subtitle = String(cleanSubtitle.prefix(80)) }
        let cleanTags = tags
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        if !cleanTags.isEmpty { config.tags = Array(cleanTags.prefix(4)) }
        if let theme = Self.normalizedThemeHex(theme) {
            config.themeHex = theme
        }
        if let template = template?.trimmingCharacters(in: .whitespacesAndNewlines),
           let parsed = PromoCardTemplate(rawValue: template.lowercased()) {
            config.template = parsed
        }
        config.updatedAt = nowMillis()
        return config
    }

    /// 兼容两种常见返回：6 位十六进制（可带 #）或颜色名（如 "lime" / "coffee"）。
    static func normalizedThemeHex(_ raw: String?) -> String? {
        guard var value = raw?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
              !value.isEmpty else { return nil }
        if value.hasPrefix("#") { value.removeFirst() }
        if value.count == 6, value.range(of: "^[0-9a-f]{6}$", options: .regularExpression) != nil {
            return value.uppercased()
        }
        let named: [String: String] = [
            "lime": "A3CD39", "green": "7E9C4F", "matcha": "7E9C4F",
            "coffee": "6B4A33", "brown": "6B4A33",
            "ink": "26231F", "black": "26231F", "dark": "26231F",
            "cream": "D7BC62", "butter": "D7BC62", "yellow": "D7BC62",
            "rose": "B77A6A", "red": "B77A6A", "pink": "B77A6A",
            "sky": "6E8F9D", "blue": "6E8F9D",
            "orange": "C08A3E", "pumpkin": "C08A3E"
        ]
        return named[value]
    }
}

// MARK: - 错误

enum AIClientError: LocalizedError {
    case notConfigured
    case badURL
    case network(String)
    case http(Int, String)
    case emptyResponse
    case invalidJSON(String)
    case imageDecodeFailed

    var errorDescription: String? {
        switch self {
        case .notConfigured:
            return "还没有配置 AI 服务。请在「统计 → 设置 → AI API」里填写对应的 Base URL、API Key 和模型名。"
        case .badURL:
            return "Base URL 格式不对。示例：https://open.bigmodel.cn/api/paas/v4（本机 mock 可用 http://127.0.0.1:端口/v1）。"
        case .network(let message):
            return "网络请求失败：\(message)"
        case .http(let code, let message):
            return "AI 服务返回错误（HTTP \(code)）：\(message)"
        case .emptyResponse:
            return "AI 服务没有返回内容，请稍后重试。"
        case .invalidJSON(let snippet):
            return "AI 返回的内容不是有效 JSON，可以重试一次。\n\(snippet)"
        case .imageDecodeFailed:
            return "插画接口返回的图片无法解析，请重试或换一个插画模型。"
        }
    }
}

// MARK: - OpenAI 兼容客户端

enum AIClient {
    /// 生成宣传卡片文案：只返回文本 JSON，由本地渲染，不做图像生成。
    static func generateCardCopy(
        for recipe: Recipe,
        bean: Bean?,
        description: String,
        settings: AISettings
    ) async throws -> AIGeneratedCopy {
        guard settings.isConfigured else { throw AIClientError.notConfigured }

        let system = """
        你是「忆杯手记」App 的社交卡片文案助手。根据用户给出的配方信息与描述，\
        只返回一个 JSON 对象，不要 Markdown 代码块，不要任何多余解释。JSON 字段要求：
        - title：卡片大标题，中文，12–20 字，有画面感、适合社交封面，不要引号
        - subtitle：副标题，中文，20–40 字，写风味/口感/场景
        - tags：3–4 个中文短标签，每个 2–6 字
        - theme：从 ["A3CD39","6B4A33","26231F","D7BC62","B77A6A","6E8F9D","7E9C4F","C08A3E"] 中最合适的一个主题色
        - template：从 ["magazine","poster","minimal"] 中最合适的一个模板
        示例：{"title":"荔枝气泡冷萃","subtitle":"荔枝糖的甜香被气泡托起来，适合午后慢慢喝。","tags":["荔枝","气泡","清爽","微甜"],"theme":"A3CD39","template":"magazine"}
        """

        let layerText = recipe.layers
            .map { "\($0.name)\($0.grams)g" }
            .joined(separator: "、")
        var recipeLines: [String] = [
            "配方名：\(recipe.name)",
            "豆子：\(bean?.name ?? recipe.beanName)",
            "口感：\(recipe.mouthfeel)",
            "甜度：\(recipe.sweetness)",
            "评分：\(recipe.rating)/5",
            "层次：\(layerText)"
        ]
        if let bean {
            recipeLines.append("产地：\(bean.origin) \(bean.process)")
            recipeLines.append("豆子风味：\(bean.flavors.joined(separator: "、"))")
        }
        if !recipe.notes.isEmpty {
            recipeLines.append("品鉴笔记：\(recipe.notes)")
        }
        let cleanDescription = description.trimmingCharacters(in: .whitespacesAndNewlines)
        let user = """
        \(recipeLines.joined(separator: "\n"))

        我的描述：\(cleanDescription.isEmpty ? "做一张有氛围感的社交卡片。" : cleanDescription)
        """

        let content = try await chat(
            settings: settings,
            messages: [
                ["role": "system", "content": system],
                ["role": "user", "content": user]
            ],
            maxTokens: 700,
            temperature: 0.85
        )
        guard let json = extractJSONObject(from: content) else {
            throw AIClientError.invalidJSON(String(content.prefix(120)))
        }
        do {
            return try JSONDecoder().decode(AIGeneratedCopy.self, from: Data(json.utf8))
        } catch {
            throw AIClientError.invalidJSON(String(json.prefix(160)))
        }
    }

    /// 测试连接：发一条最小的 chat/completions 请求。
    static func testConnection(settings: AISettings) async throws -> String {
        guard settings.isConfigured else { throw AIClientError.notConfigured }
        let reply = try await chat(
            settings: settings,
            messages: [["role": "user", "content": "ping，请只回复 pong"]],
            maxTokens: 16,
            temperature: 0
        )
        let clean = reply.trimmingCharacters(in: .whitespacesAndNewlines)
        if clean.isEmpty { return "连接成功（模型返回为空，但接口可用）。" }
        if clean.hasPrefix("{") || clean.hasPrefix("[") {
            return "连接成功 · 模型 \(settings.trimmedModel) 已响应（返回 \(clean.count) 字符）。"
        }
        return "连接成功 · \(settings.trimmedModel)：\(String(clean.prefix(48)))"
    }

    // MARK: - 图片理解 API（实物照 → 插画 prompt）
    /// 配方上下文（中文，用于照片描述补全）。
    static func illustrationRecipeContext(recipe: Recipe, bean: Bean?) -> String {
        let category = promoDrinkCategory(for: recipe)
        let layers = recipe.layers.prefix(6).map { "\($0.name) \($0.grams)g" }.joined(separator: "、")
        let beanText: String
        if let bean {
            beanText = "豆子：\(bean.name)（\(bean.origin) \(bean.process)）"
        } else if !recipe.beanName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            beanText = "豆子：\(recipe.beanName)"
        } else {
            beanText = ""
        }
        var parts: [String] = ["配方：\(recipe.name)（\(category)）"]
        if !beanText.isEmpty { parts.append(beanText) }
        if !layers.isEmpty { parts.append("层次：\(layers)") }
        return parts.joined(separator: " · ")
    }

    /// 把照片描述/用户草稿补全为完整插画 prompt：主体描述 + 配方上下文 + 透明底 PNG 风格后缀。
    /// 总长 ≤1000；超过时只裁剪描述，风格后缀与配方上下文必须保留。
    static func buildIllustrationPrompt(description: String, recipe: Recipe, bean: Bean?) -> String {
        let suffix = illustrationTransparentSuffix
        let context = illustrationRecipeContext(recipe: recipe, bean: bean)
        let separator = "\n"
        let fixedLength = suffix.count + context.count + separator.count * 2
        let budget = max(0, 1000 - fixedLength)

        var body = description
            .trimmingCharacters(in: .whitespacesAndNewlines)
        // 去掉用户/视觉返回中已经带上的透明后缀，避免重复拼接。
        if let range = body.range(of: suffix, options: [.caseInsensitive, .diacriticInsensitive]) {
            body = String(body[..<range.lowerBound])
        }
        // 若已含「配方：」上下文，防止重复补全。
        if let range = body.range(of: "配方：") {
            let prefixPart = String(body[..<range.lowerBound]).trimmingCharacters(in: .whitespacesAndNewlines)
            body = prefixPart
        }
        body = body.trimmingCharacters(in: .whitespacesAndNewlines)
        if body.count > budget {
            body = String(body.prefix(budget))
                .trimmingCharacters(in: .whitespacesAndNewlines)
        }
        if body.isEmpty {
            body = "手绘水彩咖啡插画，主体为这杯饮品，暖色调，构图居中。"
        }
        let result = [body, context, suffix]
            .filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
            .joined(separator: separator)
        // 兜底：极端情况下再裁一次（保留后缀）。
        if result.count > 1000 {
            let allowed = max(0, 1000 - suffix.count - context.count - separator.count * 2)
            let trimmedBody = String(body.prefix(allowed))
            return [trimmedBody, context, suffix].joined(separator: separator)
        }
        return result
    }



    /// 透明背景 PNG 的统一后缀：所有默认/视觉/本地降级 prompt 都追加，保证出图后能抠成透明。
    static let illustrationTransparentSuffix = "isolated illustration on transparent background, PNG with alpha, no background scene, no paper texture, no shadow, no text, no letters, no words, no watermark, no logo, no UI."

    /// 优先用视觉模型（OpenAI 兼容 chat + image_url）理解实物照，生成 ≤200 字的插画描述。
    static func generatePromptFromPhoto(
        _ image: UIImage,
        recipe: Recipe,
        bean: Bean?,
        settings: AISettings
    ) async throws -> String {
        guard settings.isVisionConfigured else { throw AIClientError.notConfigured }
        let prepared = image.bbDownscaled(maxDimension: 768)
        guard let jpeg = prepared.jpegData(compressionQuality: 0.82) else {
            throw AIClientError.imageDecodeFailed
        }
        let dataURL = "data:image/jpeg;base64," + jpeg.base64EncodedString()
        let layerText = recipe.layers.prefix(6).map { "\($0.name) \($0.grams)g" }.joined(separator: "、")
        let beanText = bean.map { "豆子：\($0.name)（\($0.origin) \($0.process)）" } ?? "豆子：\(recipe.beanName)"
        let instruction = """
        请观察这张咖啡实物照片，为它生成一段用于「手绘水彩咖啡插画」的中文提示词，要求：
        - 描述照片中的主体/杯型/饮品层次/配料或装饰/主色与光影/构图，尽量控制在 300 字以内；
        - 只输出提示词正文，不要解释、不要加引号、不要 markdown；
        - 不要出现任何文字、字母、logo、水印、UI 的描述；
        - 配方参考：\(recipe.name)（\(promoDrinkCategory(for: recipe))），\(beanText)，层次：\(layerText)
        """
        let messages: [[String: Any]] = [
            [
                "role": "system",
                "content": "你是咖啡插画提示词助手。你只输出一段可直接用于图像生成模型的中文画面描述，绝不输出多余解释、引号或 markdown。"
            ],
            [
                "role": "user",
                "content": [
                    ["type": "text", "text": instruction],
                    ["type": "image_url", "image_url": ["url": dataURL]]
                ]
            ]
        ]
        let content = try await chatVision(
            settings: settings,
            messages: messages,
            maxTokens: 420,
            temperature: 0.4
        )
        var clean = content.trimmingCharacters(in: .whitespacesAndNewlines)
        clean = clean.replacingOccurrences(of: "```", with: "")
        while let first = clean.first, ["\"", "“", "”", "「", "」"].contains(String(first)) {
            clean.removeFirst()
        }
        while let last = clean.last, ["\"", "“", "”", "「", "」"].contains(String(last)) {
            clean.removeLast()
        }
        clean = clean.trimmingCharacters(in: .whitespacesAndNewlines)
        if clean.hasPrefix("提示词：") {
            clean = String(clean.dropFirst("提示词：".count))
        }
        // 完整描述交给 buildIllustrationPrompt 补全：配方上下文 + 透明底风格后缀，总长 ≤1000；
        // 只裁剪描述，风格后缀始终保留。
        return buildIllustrationPrompt(description: clean, recipe: recipe, bean: bean)
    }

    /// 本地降级：未配置视觉 API 或调用失败时，用照片主色/亮度 + 配方生成 prompt。
    static func localPromptFromPhoto(_ image: UIImage, recipe: Recipe, bean: Bean?) -> String {
        let prepared = image.bbDownscaled(maxDimension: 48)
        var red = 0.0, green = 0.0, blue = 0.0, count = 0.0
        if let cgImage = prepared.cgImage {
            let width = max(1, Int(prepared.size.width))
            let height = max(1, Int(prepared.size.height))
            var pixels = [UInt8](repeating: 0, count: width * height * 4)
            if let context = CGContext(
                data: &pixels,
                width: width,
                height: height,
                bitsPerComponent: 8,
                bytesPerRow: width * 4,
                space: CGColorSpaceCreateDeviceRGB(),
                bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
            ) {
                context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))
                for index in 0..<(width * height) {
                    let p = index * 4
                    let a = Double(pixels[p + 3]) / 255.0
                    guard a > 0.1 else { continue }
                    red += Double(pixels[p]) / a
                    green += Double(pixels[p + 1]) / a
                    blue += Double(pixels[p + 2]) / a
                    count += 1
                }
            }
        }
        if count > 0 { red /= count; green /= count; blue /= count }
        let r = Int(red.rounded()), g = Int(green.rounded()), b = Int(blue.rounded())
        let brightness = (red + green + blue) / 3
        let tone = brightness > 168 ? "明亮柔和" : (brightness > 96 ? "温暖均衡" : "深邃浓郁")
        let colorName: String
        if green > red && green > blue {
            colorName = "草本绿调"
        } else if red > green + 20 && red > blue + 20 {
            colorName = "焦糖棕红"
        } else if blue > red && blue > green {
            colorName = "冷调蓝灰"
        } else if abs(red - green) < 18 && abs(green - blue) < 18 {
            colorName = brightness > 168 ? "奶油米白" : "深灰棕"
        } else {
            colorName = "暖棕色调"
        }
        let layers = recipe.layers.prefix(4).map { $0.name }.joined(separator: "、")
        let category = promoDrinkCategory(for: recipe)
        let chinese = "手绘水彩咖啡插画：主体是一杯“\(recipe.name)”（\(category)），杯型与构图参考实物照片，层次包含\(layers.isEmpty ? "饮品与配料" : layers)，主色为\(colorName)（#\(String(format: "%02X%02X%02X", r, g, b))），整体\(tone)调，保留杯身反光与配料装饰，无文字。"
        return buildIllustrationPrompt(description: chinese, recipe: recipe, bean: bean)
    }

    /// 测试图片理解接口（会真实调用一次视觉模型，消耗额度）。
    static func testVisionConnection(settings: AISettings) async throws -> String {
        guard settings.isVisionConfigured else { throw AIClientError.notConfigured }
        let size = CGSize(width: 64, height: 64)
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        format.opaque = true
        let image = UIGraphicsImageRenderer(size: size, format: format).image { context in
            UIColor(red: 0.96, green: 0.94, blue: 0.90, alpha: 1).setFill()
            context.fill(CGRect(origin: .zero, size: size))
            UIColor(red: 0.36, green: 0.24, blue: 0.14, alpha: 1).setFill()
            UIBezierPath(ovalIn: CGRect(x: 18, y: 18, width: 28, height: 28)).fill()
        }
        guard let jpeg = image.jpegData(compressionQuality: 0.8) else {
            throw AIClientError.imageDecodeFailed
        }
        let dataURL = "data:image/jpeg;base64," + jpeg.base64EncodedString()
        let messages: [[String: Any]] = [[
            "role": "user",
            "content": [
                ["type": "text", "text": "这张图里有什么颜色？请只回复一句中文描述。"],
                ["type": "image_url", "image_url": ["url": dataURL]]
            ]
        ]]
        let reply = try await chatVision(
            settings: settings,
            messages: messages,
            maxTokens: 60,
            temperature: 0
        )
        let clean = reply.trimmingCharacters(in: .whitespacesAndNewlines)
        let model = settings.effectiveVisionModel
        if clean.isEmpty { return "图片理解连接成功 · \(model) 已响应（返回为空）。" }
        return "图片理解连接成功 · \(model)：\(String(clean.prefix(80)))"
    }

    private static func chatVision(
        settings: AISettings,
        messages: [[String: Any]],
        maxTokens: Int,
        temperature: Double
    ) async throws -> String {
        guard let url = endpoint(from: settings.effectiveVisionBaseURL) else { throw AIClientError.badURL }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.timeoutInterval = 60
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(settings.effectiveVisionAPIKey)", forHTTPHeaderField: "Authorization")
        let body: [String: Any] = [
            "model": settings.effectiveVisionModel,
            "messages": messages,
            "temperature": temperature,
            "max_tokens": maxTokens
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch {
            throw AIClientError.network(error.localizedDescription)
        }
        guard let http = response as? HTTPURLResponse else {
            throw AIClientError.network("没有收到有效的 HTTP 响应。")
        }
        guard (200..<300).contains(http.statusCode) else {
            let message = apiErrorMessage(from: data) ?? String(data: data.prefix(200), encoding: .utf8) ?? ""
            throw AIClientError.http(http.statusCode, message)
        }
        guard
            let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
            let choices = object["choices"] as? [[String: Any]],
            let message = choices.first?["message"] as? [String: Any],
            let content = message["content"] as? String
        else {
            throw AIClientError.emptyResponse
        }
        return content
    }

    // MARK: - 底层请求

    private static func chat(
        settings: AISettings,
        messages: [[String: String]],
        maxTokens: Int,
        temperature: Double
    ) async throws -> String {
        guard let url = endpoint(from: settings.trimmedBaseURL) else { throw AIClientError.badURL }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.timeoutInterval = 40
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(settings.trimmedAPIKey)", forHTTPHeaderField: "Authorization")

        let body: [String: Any] = [
            "model": settings.trimmedModel,
            "messages": messages,
            "temperature": temperature,
            "max_tokens": maxTokens
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch {
            throw AIClientError.network(error.localizedDescription)
        }

        guard let http = response as? HTTPURLResponse else {
            throw AIClientError.network("没有收到有效的 HTTP 响应。")
        }
        guard (200..<300).contains(http.statusCode) else {
            let message = apiErrorMessage(from: data) ?? String(data: data.prefix(200), encoding: .utf8) ?? ""
            throw AIClientError.http(http.statusCode, message)
        }

        guard
            let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
            let choices = object["choices"] as? [[String: Any]],
            let message = choices.first?["message"] as? [String: Any],
            let content = message["content"] as? String
        else {
            throw AIClientError.emptyResponse
        }
        return content
    }

    /// 兼容用户填写…/v1、…/v1/、…/v1/chat/completions 三种写法。
    private static func endpoint(from baseURL: String) -> URL? {
        var base = baseURL.trimmingCharacters(in: .whitespacesAndNewlines)
        while base.hasSuffix("/") { base.removeLast() }
        guard !base.isEmpty else { return nil }
        let full = base.contains("/chat/completions") ? base : base + "/chat/completions"
        guard let url = URL(string: full), let scheme = url.scheme?.lowercased(),
              scheme == "http" || scheme == "https", url.host != nil else { return nil }
        return url
    }

    private static func apiErrorMessage(from data: Data) -> String? {
        guard
            let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
            let error = object["error"] as? [String: Any],
            let message = error["message"] as? String
        else { return nil }
        return message
    }

    /// 模型有时会包一层 Markdown 代码块，这里取出第一个 { 到最后一个 } 之间的 JSON。
    private static func extractJSONObject(from content: String) -> String? {
        guard let start = content.firstIndex(of: "{"),
              let end = content.lastIndex(of: "}"),
              start < end else { return nil }
        return String(content[start...end])
    }

    // MARK: - AI 插画（OpenAI 兼容 images/generations）

    /// 根据当前配方生成「手绘水彩咖啡插画」prompt：英文风格词 + 中文配方细节。
    static func illustrationPrompt(for recipe: Recipe, bean: Bean?) -> String {
        let category = promoDrinkCategory(for: recipe)
        let iced = promoIsIced(recipe)
        let cup = iced
            ? "iced drink in a clear glass with ice cubes"
            : "hot drink in a warm ceramic cup"
        let layerNames = recipe.layers.prefix(5).map { "\($0.name) \($0.grams)g" }.joined(separator: ", ")
        let colors = recipe.layers.prefix(4).map { $0.colorHex }.joined(separator: ", ")
        let flavors = promoFlavorKeywords(recipe: recipe, bean: bean).joined(separator: ", ")
        let beanName = bean?.name ?? recipe.beanName
        return """
        Hand-painted watercolor illustration for a specialty coffee shop's drink card: \
        \(recipe.name) (\(category)), \(cup). Ingredients: \(layerNames); coffee bean: \(beanName); \
        flavor notes: \(flavors); color palette: \(colors). \
        Add ingredient garnishes that match the flavors (fruit slices, herbs and coffee beans), \
        hand-drawn watercolor texture, warm tone, soft brush strokes, centered composition, square 1024. \
        Isolated illustration on a transparent background, PNG with alpha, no background scene, \
        no paper texture, no shadow, no text, no letters, no words, no watermark, no logo, no UI.
        """
    }

    /// 请求 OpenAI 兼容的 images/generations 生成正方形咖啡插画，并优先请求关闭服务端水印。
    /// 智谱 GLM-Image 支持 `watermark_enabled: false`；若账号未签免责声明导致失败，会自动回退 true 重试一次。
    /// 返回：图片 / 实际 prompt / 是否成功请求了「去水印」。
    static func generateIllustration(
        recipe: Recipe,
        bean: Bean?,
        prompt: String?,
        preferredSize: String? = nil,
        settings: AISettings
    ) async throws -> (image: UIImage, prompt: String, watermarkDisabled: Bool) {
        do {
            return try await performIllustrationRequest(
                recipe: recipe,
                bean: bean,
                prompt: prompt,
                preferredSize: preferredSize,
                settings: settings,
                watermarkEnabled: false
            )
        } catch {
            if shouldRetryWithWatermarkEnabled(after: error) {
                return try await performIllustrationRequest(
                    recipe: recipe,
                    bean: bean,
                    prompt: prompt,
                    preferredSize: preferredSize,
                    settings: settings,
                    watermarkEnabled: true
                )
            }
            throw error
        }
    }

    /// 账号不支持去水印（4xx / 水印/免责声明相关错误）时回退 true 重试一次。
    private static func shouldRetryWithWatermarkEnabled(after error: Error) -> Bool {
        guard case let AIClientError.http(status, message) = error else { return false }
        if (400...403).contains(status) { return true }
        let lower = message.lowercased()
        return lower.contains("watermark") || message.contains("水印") || message.contains("免责")
    }

    private static func performIllustrationRequest(
        recipe: Recipe,
        bean: Bean?,
        prompt: String?,
        preferredSize: String? = nil,
        settings: AISettings,
        watermarkEnabled: Bool
    ) async throws -> (image: UIImage, prompt: String, watermarkDisabled: Bool) {
        guard settings.isImageConfigured else { throw AIClientError.notConfigured }
        guard let url = imagesEndpoint(from: settings.effectiveImageBaseURL) else { throw AIClientError.badURL }

        let cleanPrompt = prompt?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let rawPrompt = cleanPrompt.isEmpty ? illustrationPrompt(for: recipe, bean: bean) : cleanPrompt
        // GLM-Image 要求 prompt ≤ 1000 字符；卡片文字全部由 App 本地排版，prompt 只描述画面。
        let finalPrompt = String(rawPrompt.prefix(1000))
        let model = settings.effectiveImageModel
        let size = AISettings.normalizedImageSize(settings.trimmedImageSize)
            ?? AISettings.normalizedImageSize(preferredSize ?? "")
            ?? "1280x1280"

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.timeoutInterval = 120
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(settings.effectiveImageAPIKey)", forHTTPHeaderField: "Authorization")
        request.httpBody = try JSONSerialization.data(withJSONObject: [
            "model": model,
            "prompt": finalPrompt,
            "size": size,
            "watermark_enabled": watermarkEnabled
        ])

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch {
            throw AIClientError.network(error.localizedDescription)
        }
        guard let http = response as? HTTPURLResponse else {
            throw AIClientError.network("没有收到有效的 HTTP 响应。")
        }
        guard (200..<300).contains(http.statusCode) else {
            let message = apiErrorMessage(from: data) ?? String(data: data.prefix(200), encoding: .utf8) ?? ""
            throw AIClientError.http(http.statusCode, message)
        }

        guard
            let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
            let items = object["data"] as? [[String: Any]],
            let first = items.first
        else {
            throw AIClientError.emptyResponse
        }

        var imageData: Data?
        if let b64 = first["b64_json"] as? String, let raw = Data(base64Encoded: b64) {
            imageData = raw
        } else if let urlString = first["url"] as? String, let imageURL = URL(string: urlString) {
            do {
                let (downloaded, _) = try await URLSession.shared.data(from: imageURL)
                imageData = downloaded
            } catch {
                throw AIClientError.network("插画下载失败：\(error.localizedDescription)")
            }
        }
        guard let imageData, let image = UIImage(data: imageData) else {
            throw AIClientError.imageDecodeFailed
        }
        return (image, finalPrompt, !watermarkEnabled)
    }

    /// 测试插画接口（会真实消耗一次出图额度）：生成一张很小的咖啡插画。
    static func testImageConnection(settings: AISettings) async throws -> String {
        guard settings.isImageConfigured else { throw AIClientError.notConfigured }
        let result = try await generateIllustration(
            recipe: Recipe(
                id: "connection-test",
                name: "手冲咖啡",
                beanId: nil,
                beanName: "",
                layers: [],
                rating: 4,
                notes: "",
                mouthfeel: "均衡",
                sweetness: "微甜",
                beanGrams: 15,
                stirred: false,
                createdAt: nowMillis()
            ),
            bean: nil,
            prompt: "A small hand-painted watercolor coffee cup, isolated illustration on transparent background, PNG with alpha, warm tone, ingredient garnish, centered, no paper texture, no shadow, no text no letters, no watermark",
            preferredSize: "1280x1280",
            settings: settings
        )
        let bytes = result.image.pngData()?.count ?? 0
        let sizeText = "\(Int(result.image.size.width))×\(Int(result.image.size.height))"
        return "出图成功 · 模型 \(settings.effectiveImageModel) · \(sizeText) · \(bytes / 1024)KB。真实生成会消耗一次额度。"
    }

    /// 兼容用户填写 …/v1、…/v1/、…/v1/images/generations 三种写法。
    private static func imagesEndpoint(from baseURL: String) -> URL? {
        var base = baseURL.trimmingCharacters(in: .whitespacesAndNewlines)
        while base.hasSuffix("/") { base.removeLast() }
        guard !base.isEmpty else { return nil }
        let full = base.contains("/images/generations") ? base : base + "/images/generations"
        guard let url = URL(string: full), let scheme = url.scheme?.lowercased(),
              scheme == "http" || scheme == "https", url.host != nil else { return nil }
        return url
    }
}
