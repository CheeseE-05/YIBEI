import Foundation
import SwiftUI
import UniformTypeIdentifiers

// MARK: - 设置页
//
// 1.3 功能：统计页底部进入。包含：
//   - 外观：跟随系统 / 浅色 / 深色（AppAppearance 持久化在 UserDefaults）
//   - AI API：OpenAI 兼容 chat/completions 的 Base URL / API Key / 模型名，可保存、测试连接、恢复默认
//   - 数据：全库导出 / 合并导入 / 覆盖恢复 / 导入离线配方（从原「数据与分享」页迁入）
//   - 关于：版本号、隐私说明

private enum SettingsImportAction: Equatable {
    case merge, replace, recipe
}

struct SettingsView: View {
    @EnvironmentObject private var store: AppStore
    @Environment(\.dismiss) private var dismiss

    @AppStorage(AppAppearance.storageKey) private var appearanceRaw = AppAppearance.system.rawValue
    @AppStorage(AISettings.baseURLKey) private var aiBaseURL = AISettings.defaultBaseURL
    @AppStorage(AISettings.apiKeyKey) private var aiAPIKey = ""
    @AppStorage(AISettings.modelKey) private var aiModel = AISettings.defaultModel
    @AppStorage(AISettings.imageBaseURLKey) private var aiImageBaseURL = AISettings.defaultImageBaseURL
    @AppStorage(AISettings.imageAPIKeyKey) private var aiImageAPIKey = ""
    @AppStorage(AISettings.imageModelKey) private var aiImageModel = AISettings.defaultImageModel
    @AppStorage(AISettings.imageSizeKey) private var aiImageSize = AISettings.defaultImageSize
    @AppStorage(AISettings.visionBaseURLKey) private var aiVisionBaseURL = AISettings.defaultVisionBaseURL
    @AppStorage(AISettings.visionAPIKeyKey) private var aiVisionAPIKey = ""
    @AppStorage(AISettings.visionModelKey) private var aiVisionModel = AISettings.defaultVisionModel

    @State private var testing = false
    @State private var aiStatus: String?
    @State private var aiStatusIsError = false
    @State private var testingImage = false
    @State private var imageStatus: String?
    @State private var imageStatusIsError = false
    @State private var testingVision = false
    @State private var visionStatus: String?
    @State private var visionStatusIsError = false

    @State private var exportDocument = JSONDocument(data: Data())
    @State private var exportName = "YIBEI Backup"
    @State private var showExporter = false
    @State private var showImporter = false
    @State private var pendingAction: SettingsImportAction = .merge
    @State private var showReplaceAlert = false
    @State private var busy = false
    @State private var message: String?
    @State private var messageIsError = false

    var body: some View {
        VStack(spacing: 0) {
            topBar
            ScrollViewReader { proxy in
                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 18) {
                        header
                        appearanceSection
                        aiSection
                        dataSection.id("settings_data")
                        aboutSection.id("settings_about")
                        if let message {
                            messageView(message, isError: messageIsError)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 40)
                    .frame(maxWidth: 760)
                    .frame(maxWidth: .infinity)
                }
                .onAppear { runDebugScrollIfNeeded(proxy: proxy) }
            }
        }
        .background(PaperBackground())
        .fileExporter(
            isPresented: $showExporter,
            document: exportDocument,
            contentType: .json,
            defaultFilename: exportName
        ) { result in
            switch result {
            case .success:
                showMessage("数据库已导出，可以保存到文件或发到另一台设备。")
            case .failure(let error):
                showMessage("导出失败：\(error.localizedDescription)", error: true)
            }
        }
        .fileImporter(
            isPresented: $showImporter,
            allowedContentTypes: [.json],
            allowsMultipleSelection: false
        ) { result in
            switch result {
            case .success(let urls):
                guard let url = urls.first else { return }
                importFile(url)
            case .failure(let error):
                showMessage("读取文件失败：\(error.localizedDescription)", error: true)
            }
        }
        .onAppear { runDebugAITestIfNeeded() }
        .alert("覆盖当前数据？", isPresented: $showReplaceAlert) {
            Button("继续覆盖", role: .destructive) {
                pendingAction = .replace
                showImporter = true
            }
            Button("取消", role: .cancel) {}
        } message: {
            Text("文件里的豆子、配方、材料和照片会替换这台设备现有的全部数据，且不能撤销。")
        }
        .toolbar(.hidden, for: .navigationBar)
    }

    // MARK: - 顶部 / 标题

    private var topBar: some View {
        HStack(spacing: 10) {
            RoundIconButton(systemName: "chevron.left", accessibilityLabel: "返回统计") {
                dismiss()
            }
            VStack(alignment: .leading, spacing: 2) {
                Overline(text: "SETTINGS")
                Text("设置")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(C.ink)
            }
            Spacer()
            Text("1.2 · \(buildNumber)")
                .font(.mono(9))
                .foregroundStyle(C.muted)
        }
        .padding(.horizontal, 20)
        .padding(.top, 8)
        .padding(.bottom, 10)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("把豆仓调成你喜欢的样子")
                .font(.display(28))
                .foregroundStyle(C.ink)
            Text("外观、AI 文案、数据备份都在这里；所有数据仍然只保存在本机。")
                .font(.system(size: 12))
                .foregroundStyle(C.muted)
                .lineSpacing(3)
        }
    }

    // MARK: - 外观

    private var appearanceSection: some View {
        settingsCard(title: "外观", overline: "APPEARANCE", symbol: "circle.lefthalf.filled") {
            VStack(spacing: 8) {
                ForEach(AppAppearance.allCases) { appearance in
                    Button {
                        appearanceRaw = appearance.rawValue
                        haptic()
                    } label: {
                        HStack(spacing: 10) {
                            Image(systemName: appearance.symbol)
                                .font(.system(size: 14, weight: .medium))
                                .foregroundStyle(C.forest)
                                .frame(width: 22)
                            Text(appearance.label)
                                .font(.system(size: 13.5, weight: .medium))
                                .foregroundStyle(C.ink)
                            Spacer()
                            if appearanceRaw == appearance.rawValue {
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.system(size: 15))
                                    .foregroundStyle(C.lime)
                            }
                        }
                        .padding(.horizontal, 13)
                        .frame(height: 46)
                        .background(appearanceRaw == appearance.rawValue ? C.limeSoft : C.paperSoft)
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    // MARK: - AI API

    private var aiSection: some View {
        VStack(spacing: 12) {
            textAPISection.id("settings_text_api")
            imageAPISection.id("settings_image_api")
            visionAPISection.id("settings_vision_api")
            Text("三个 API 的 Key 都只保存在本机设置里，不会写进导出的数据库备份；请使用你自己的密钥。")
                .font(.system(size: 9.5))
                .foregroundStyle(C.mutedLight)
                .padding(.horizontal, 4)
        }
    }

    // MARK: 文案 API（可选，卡片可以纯手动设计）

    private var textAPISection: some View {
        settingsCard(title: "文案 API", overline: "CHAT · 可选", symbol: "text.bubble") {
            VStack(alignment: .leading, spacing: 11) {
                Text("用于「AI 文案」生成随杯卡的英文名/标题/标签，走 OpenAI 兼容 chat/completions。不配置也可以手动设计卡片。")
                    .font(.system(size: 11))
                    .foregroundStyle(C.muted)
                    .lineSpacing(3)

                presetRow(
                    titles: AITextProviderPreset.allCases.map { ($0.label, "") },
                    selectedIndex: AITextProviderPreset.allCases.firstIndex(of: selectedTextPreset) ?? 2
                ) { index in
                    let preset = AITextProviderPreset.allCases[index]
                    if preset != .custom {
                        aiBaseURL = preset.baseURL
                        aiModel = preset.model
                    }
                }

                settingsField(label: "BASE URL", placeholder: "https://api.deepseek.com/v1", text: $aiBaseURL)
                secureKeyField(label: "API KEY", text: $aiAPIKey)
                settingsField(label: "MODEL", placeholder: "deepseek-chat / gpt-4o-mini", text: $aiModel)
                aiButtons(
                    testing: testing,
                    testTitle: "测试连接",
                    onSave: saveTextAPI,
                    onTest: testAIConnection,
                    onReset: restoreTextDefaults
                )
                statusView(text: aiStatus, isError: aiStatusIsError)
            }
        }
    }

    // MARK: 插画 API（默认智谱 BigModel · GLM-Image）

    private var imageAPISection: some View {
        settingsCard(title: "插画 API", overline: "IMAGES · GLM-IMAGE / KOLORS", symbol: "photo.artframe") {
            VStack(alignment: .leading, spacing: 11) {
                Text("根据这杯配方生成手绘水彩咖啡插画，用在随杯卡主视觉。OpenAI 兼容 images/generations（GLM-Image 返回 url，需要下载后存本机；第三方网关的 b64_json 也支持）；未填 Key 时插画功能禁用。")
                    .font(.system(size: 11))
                    .foregroundStyle(C.muted)
                    .lineSpacing(3)

                presetRow(
                    titles: AIImageProviderPreset.allCases.map { ($0.label, $0.caption) },
                    selectedIndex: AIImageProviderPreset.allCases.firstIndex(of: selectedImagePreset) ?? 2
                ) { index in
                    let preset = AIImageProviderPreset.allCases[index]
                    if preset != .custom {
                        aiImageBaseURL = preset.baseURL
                        aiImageModel = preset.model
                    }
                }

                settingsField(label: "BASE URL", placeholder: "https://open.bigmodel.cn/api/paas/v4", text: $aiImageBaseURL)
                secureKeyField(label: "API KEY（智谱 / 硅基流动）", text: $aiImageAPIKey)
                settingsField(label: "MODEL", placeholder: "glm-image / Kwai-Kolors/Kolors", text: $aiImageModel)
                settingsField(label: "SIZE", placeholder: "auto / 1024x1440 / 1440x1024", text: $aiImageSize)
                aiButtons(
                    testing: testingImage,
                    testTitle: "测试出图",
                    onSave: saveImageAPI,
                    onTest: testImageConnection,
                    onReset: restoreImageDefaults
                )
                statusView(text: imageStatus, isError: imageStatusIsError)
                Text("GLM-Image 约 0.1 元/次，「测试出图」会真实调用一次接口并消耗额度。SIZE 填 auto 时按卡片比例自动选竖版 1024×1440 / 横版 1440×1024（自定义需 512–2048 且为 32 的倍数）。")
                    .font(.system(size: 9.5))
                    .foregroundStyle(C.mutedLight)
                    .lineSpacing(2)
            }
        }
    }

    // MARK: 图片理解 API（实物照 → 插画 prompt；默认智谱 GLM-4V-Flash）

    private var visionAPISection: some View {
        settingsCard(title: "图片理解 API", overline: "VISION · 实物图生成插画", symbol: "viewfinder") {
            VStack(alignment: .leading, spacing: 11) {
                Text("用于「AI 插画 → 从实物图生成」：把实物照片发给视觉模型，生成插画 prompt（OpenAI 兼容 chat/completions + image_url）。默认智谱 GLM-4V-Flash；Key 留空时自动复用「文案 API」的 Key。")
                    .font(.system(size: 11))
                    .foregroundStyle(C.muted)
                    .lineSpacing(3)

                settingsField(label: "BASE URL", placeholder: AISettings.defaultVisionBaseURL, text: $aiVisionBaseURL)
                secureKeyField(label: "API KEY（图片理解 / 留空复用文案 Key）", text: $aiVisionAPIKey)
                settingsField(label: "MODEL", placeholder: "glm-4v-flash", text: $aiVisionModel)
                aiButtons(
                    testing: testingVision,
                    testTitle: "测试图片理解",
                    onSave: saveVisionAPI,
                    onTest: testVisionConnection,
                    onReset: restoreVisionDefaults
                )
                statusView(text: visionStatus, isError: visionStatusIsError)
                Text("「测试图片理解」会真实调用一次视觉模型并消耗额度；Key 只存本机。未配置或调用失败时，卡片编辑器会自动改用「照片主色/亮度 + 配方」本地生成 prompt 并提示。")
                    .font(.system(size: 9.5))
                    .foregroundStyle(C.mutedLight)
                    .lineSpacing(2)
            }
        }
    }

    private func presetRow(
        titles: [(String, String)],
        selectedIndex: Int,
        onSelect: @escaping (Int) -> Void
    ) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(Array(titles.enumerated()), id: \.offset) { index, item in
                    Button {
                        onSelect(index)
                        haptic()
                    } label: {
                        VStack(alignment: .leading, spacing: 1) {
                            Text(item.0)
                                .font(.system(size: 12, weight: index == selectedIndex ? .semibold : .regular))
                                .foregroundStyle(index == selectedIndex ? C.forest : C.inkSoft)
                            if !item.1.isEmpty {
                                Text(item.1)
                                    .font(.system(size: 8.5))
                                    .foregroundStyle(C.muted)
                            }
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(index == selectedIndex ? C.limeSoft : C.paperSoft)
                        .clipShape(RoundedRectangle(cornerRadius: 13, style: .continuous))
                        .overlay {
                            if index == selectedIndex {
                                RoundedRectangle(cornerRadius: 13, style: .continuous)
                                    .stroke(C.lime.opacity(0.45), lineWidth: 1)
                            }
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func secureKeyField(label: String, text: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.mono(9, weight: .regular))
                .tracking(0.7)
                .foregroundStyle(C.muted)
            SecureField("sk-…", text: text)
                .font(.system(size: 14))
                .foregroundStyle(C.ink)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
        }
        .padding(.horizontal, 15)
        .padding(.vertical, 11)
        .background(C.paperSoft)
        .clipShape(RoundedRectangle(cornerRadius: 17, style: .continuous))
    }

    private func aiButtons(
        testing: Bool,
        testTitle: String,
        onSave: @escaping () -> Void,
        onTest: @escaping () -> Void,
        onReset: @escaping () -> Void
    ) -> some View {
        HStack(spacing: 8) {
            Button(action: onSave) {
                Text("保存配置")
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(C.onAccent)
                    .frame(maxWidth: .infinity)
                    .frame(height: 42)
                    .background(C.lime)
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)

            Button(action: onTest) {
                HStack(spacing: 6) {
                    if testing { ProgressView().controlSize(.small) }
                    Text(testing ? "测试中…" : testTitle)
                        .font(.system(size: 12.5, weight: .medium))
                }
                .foregroundStyle(C.ink)
                .frame(maxWidth: .infinity)
                .frame(height: 42)
                .background(C.card)
                .clipShape(Capsule())
                .overlay(Capsule().stroke(C.line, lineWidth: 1))
            }
            .buttonStyle(.plain)
            .disabled(testing)

            Button(action: onReset) {
                Text("恢复默认")
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(C.muted)
                    .frame(height: 42)
                    .padding(.horizontal, 12)
            }
            .buttonStyle(.plain)
        }
    }

    @ViewBuilder
    private func statusView(text: String?, isError: Bool) -> some View {
        if let text {
            Text(text)
                .font(.system(size: 11))
                .foregroundStyle(isError ? C.roseDeep : C.forest)
                .lineSpacing(2)
                .padding(.horizontal, 12)
                .padding(.vertical, 9)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(isError ? C.rose.opacity(0.7) : C.limeSoft)
                .clipShape(RoundedRectangle(cornerRadius: 13, style: .continuous))
        }
    }

    private var selectedTextPreset: AITextProviderPreset {
        AITextProviderPreset.allCases.first { $0.baseURL == aiBaseURL && $0.model == aiModel } ?? .custom
    }

    private var selectedImagePreset: AIImageProviderPreset {
        AIImageProviderPreset.allCases.first { $0.baseURL == aiImageBaseURL && $0.model == aiImageModel } ?? .custom
    }

    private func settingsField(label: String, placeholder: String, text: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.mono(9, weight: .regular))
                .tracking(0.7)
                .foregroundStyle(C.muted)
            TextField(placeholder, text: text)
                .font(.system(size: 14))
                .foregroundStyle(C.ink)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .keyboardType(.URL)
        }
        .padding(.horizontal, 15)
        .padding(.vertical, 11)
        .background(C.paperSoft)
        .clipShape(RoundedRectangle(cornerRadius: 17, style: .continuous))
    }

    // MARK: - 数据（从原「数据与分享」页迁入）

    private var dataSection: some View {
        settingsCard(title: "数据备份与迁移", overline: "DATA & TRANSFER", symbol: "externaldrive") {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 0) {
                    settingsStat(value: "\(store.data.beans.count)", label: "支豆子")
                    settingsDivider
                    settingsStat(value: "\(store.data.recipes.count)", label: "杯配方")
                    settingsDivider
                    settingsStat(value: "\(store.data.customIngredients.count)", label: "个自定义材料")
                }

                PrimaryActionButton(title: busy ? "处理中…" : "导出数据库", icon: "square.and.arrow.up", enabled: !busy) {
                    exportDatabase()
                }
                HStack(spacing: 8) {
                    Button {
                        pendingAction = .merge
                        showImporter = true
                    } label: {
                        Text("合并导入")
                            .font(.system(size: 12.5, weight: .medium))
                            .foregroundStyle(C.ink)
                            .frame(maxWidth: .infinity)
                            .frame(height: 42)
                            .background(C.card)
                            .clipShape(Capsule())
                            .overlay(Capsule().stroke(C.line, lineWidth: 1))
                    }
                    .buttonStyle(.plain)

                    Button {
                        showReplaceAlert = true
                    } label: {
                        Text("覆盖恢复")
                            .font(.system(size: 12.5, weight: .medium))
                            .foregroundStyle(C.ink)
                            .frame(maxWidth: .infinity)
                            .frame(height: 42)
                            .background(C.card)
                            .clipShape(Capsule())
                            .overlay(Capsule().stroke(C.line, lineWidth: 1))
                    }
                    .buttonStyle(.plain)

                    Button {
                        pendingAction = .recipe
                        showImporter = true
                    } label: {
                        Text("导入配方")
                            .font(.system(size: 12.5, weight: .medium))
                            .foregroundStyle(C.ink)
                            .frame(maxWidth: .infinity)
                            .frame(height: 42)
                            .background(C.card)
                            .clipShape(Capsule())
                            .overlay(Capsule().stroke(C.line, lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                }

                Text("合并导入不会覆盖已有记录；覆盖恢复会替换本机全部数据；「导入配方」只导入单个离线配方文件。备份文件包含照片，请只发送给你信任的人。")
                    .font(.system(size: 10))
                    .foregroundStyle(C.muted)
                    .lineSpacing(3)
            }
        }
    }

    // MARK: - 关于

    private var aboutSection: some View {
        settingsCard(title: "关于", overline: "ABOUT", symbol: "info.circle") {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("忆杯手记")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(C.ink)
                        Text("YIBEI · 把每一杯，慢慢写厚。")
                            .font(.mono(8.5))
                            .foregroundStyle(C.muted)
                    }
                    Spacer()
                    Text("v\(appVersion) (\(buildNumber))")
                        .font(.mono(10))
                        .foregroundStyle(C.inkSoft)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(C.paperSoft)
                        .clipShape(Capsule())
                }
                HorizontalRule()
                aboutLine(
                    symbol: "person.crop.circle",
                    title: "开发者",
                    text: "芝士鸡cheeseE"
                )
                bilibiliLinkRow
                aboutLine(
                    symbol: "lock.shield",
                    title: "隐私",
                    text: "所有豆子、配方、材料、图片和设置都保存在本机；只有你主动使用 AI 生成时，才会把你填写的描述与配方信息发送到你配置的 AI 服务。"
                )
                aboutLine(
                    symbol: "photo",
                    title: "相册",
                    text: "宣传卡片导出时只会向系统申请「仅添加照片」权限，用于把 PNG 存进相册。"
                )
                aboutLine(
                    symbol: "square.and.arrow.up",
                    title: "分享",
                    text: "单杯配方仍可在配方详情页「分享文件」；整库备份 / 迁移已迁移到本页。"
                )
            }
        }
    }

    /// 关于页里的 B 站主页链接（可点击，深色模式同样可读）。
    private var bilibiliLinkRow: some View {
        Link(destination: URL(string: "https://space.bilibili.com/397554088")!) {
            HStack(alignment: .top, spacing: 9) {
                Image(systemName: "play.rectangle.fill")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(C.forest)
                    .frame(width: 24, height: 24)
                    .background(C.limeSoft)
                    .clipShape(Circle())
                VStack(alignment: .leading, spacing: 3) {
                    Text("哔哩哔哩")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(C.ink)
                    Text("space.bilibili.com/397554088")
                        .font(.system(size: 10.5))
                        .foregroundStyle(C.forest)
                        .underline()
                        .lineLimit(1)
                        .truncationMode(.middle)
                }
                Spacer(minLength: 0)
                Image(systemName: "arrow.up.right")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(C.muted)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel("打开芝士鸡cheeseE的哔哩哔哩主页")
    }

    private func aboutLine(symbol: String, title: String, text: String) -> some View {
        HStack(alignment: .top, spacing: 9) {
            Image(systemName: symbol)
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(C.forest)
                .frame(width: 24, height: 24)
                .background(C.limeSoft)
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(C.ink)
                Text(text)
                    .font(.system(size: 10.5))
                    .foregroundStyle(C.muted)
                    .lineSpacing(3)
            }
        }
    }

    // MARK: - 卡片外壳

    private func settingsCard<Content: View>(
        title: String,
        overline: String,
        symbol: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 9) {
                Image(systemName: symbol)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(C.forest)
                    .frame(width: 28, height: 28)
                    .background(C.limeSoft)
                    .clipShape(Circle())
                VStack(alignment: .leading, spacing: 2) {
                    Overline(text: overline)
                    Text(title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(C.ink)
                }
                Spacer()
            }
            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(C.card)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(C.lineSoft, lineWidth: 1)
        }
    }

    private func settingsStat(value: String, label: String) -> some View {
        VStack(spacing: 2) {
            Text(value).font(.mono(15, weight: .medium)).foregroundStyle(C.ink)
            Text(label).font(.system(size: 9)).foregroundStyle(C.muted).lineLimit(1)
        }
        .frame(maxWidth: .infinity)
    }

    private var settingsDivider: some View {
        Rectangle().fill(C.lineSoft).frame(width: 1, height: 24)
    }

    private func messageView(_ text: String, isError: Bool) -> some View {
        Text(text)
            .font(.system(size: 11.5))
            .foregroundStyle(isError ? C.roseDeep : C.forest)
            .padding(13)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(isError ? C.rose.opacity(0.75) : C.limeSoft)
            .clipShape(RoundedRectangle(cornerRadius: 15, style: .continuous))
    }

    /// DEBUG 截图辅助：启动后自动滚到「数据备份」/「关于」区块。
    private func runDebugScrollIfNeeded(proxy: ScrollViewProxy) {
        #if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        let target: String?
        if arguments.contains("-uiTestSettingsVision") {
            target = "settings_vision_api"
        } else if arguments.contains("-uiTestSettingsAI") {
            target = "settings_image_api"
        } else if arguments.contains("-uiTestSettingsText") {
            target = "settings_text_api"
        } else if arguments.contains("-uiTestSettingsData") {
            target = "settings_data"
        } else if arguments.contains("-uiTestSettingsAbout") {
            target = "settings_about"
        } else {
            target = nil
        }
        guard let target else { return }
        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 1_200_000_000)
            withAnimation(.easeOut(duration: 0.3)) {
                proxy.scrollTo(target, anchor: .top)
            }
        }
        #endif
    }

    /// DEBUG 截图辅助：启动后自动点一次「测试连接」，用于验证 AI API 配置联通。
    private func runDebugAITestIfNeeded() {
        #if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        if arguments.contains("-uiTestAITest") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 1_200_000_000)
                testAIConnection()
            }
        }
        if arguments.contains("-uiTestAIImageTest") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 1_200_000_000)
                testImageConnection()
            }
        }
        #endif
    }

    // MARK: - AI 行为

    private func currentSettings() -> AISettings {
        AISettings(
            baseURL: aiBaseURL,
            apiKey: aiAPIKey,
            model: aiModel,
            imageBaseURL: aiImageBaseURL,
            imageAPIKey: aiImageAPIKey,
            imageModel: aiImageModel,
            imageSize: aiImageSize,
            visionBaseURL: aiVisionBaseURL,
            visionAPIKey: aiVisionAPIKey,
            visionModel: aiVisionModel
        )
    }

    private func saveTextAPI() {
        AISettings.save(currentSettings())
        aiStatus = "文案 API 配置已保存。可以在卡片编辑器里用「AI 文案」生成标题/标签。"
        aiStatusIsError = false
        haptic()
    }

    private func saveImageAPI() {
        AISettings.save(currentSettings())
        imageStatus = "插画 API 配置已保存。可以到卡片编辑器里用「AI 插画」生成主视觉。"
        imageStatusIsError = false
        haptic()
    }

    private func testAIConnection() {
        guard !testing else { return }
        AISettings.save(currentSettings())
        testing = true
        aiStatus = nil
        let settings = currentSettings()
        Task { @MainActor in
            defer { testing = false }
            do {
                aiStatus = try await AIClient.testConnection(settings: settings)
                aiStatusIsError = false
            } catch {
                aiStatus = error.localizedDescription
                aiStatusIsError = true
            }
        }
    }

    private func testImageConnection() {
        guard !testingImage else { return }
        AISettings.save(currentSettings())
        testingImage = true
        imageStatus = nil
        let settings = currentSettings()
        Task { @MainActor in
            defer { testingImage = false }
            do {
                imageStatus = try await AIClient.testImageConnection(settings: settings)
                imageStatusIsError = false
            } catch {
                imageStatus = error.localizedDescription
                imageStatusIsError = true
            }
        }
    }

    private func saveVisionAPI() {
        AISettings.save(currentSettings())
        visionStatus = "图片理解 API 配置已保存。可以在卡片编辑器「AI 插画 → 从实物图生成」中使用。"
        visionStatusIsError = false
        haptic()
    }

    private func testVisionConnection() {
        guard !testingVision else { return }
        AISettings.save(currentSettings())
        testingVision = true
        visionStatus = nil
        let settings = currentSettings()
        Task { @MainActor in
            defer { testingVision = false }
            do {
                visionStatus = try await AIClient.testVisionConnection(settings: settings)
                visionStatusIsError = false
            } catch {
                visionStatus = error.localizedDescription
                visionStatusIsError = true
            }
        }
    }

    private func restoreTextDefaults() {
        AISettings.resetText()
        aiBaseURL = AISettings.defaultBaseURL
        aiAPIKey = ""
        aiModel = AISettings.defaultModel
        aiStatus = "文案 API 已恢复默认（OpenAI，API Key 已清空）。"
        aiStatusIsError = false
    }

    private func restoreImageDefaults() {
        AISettings.resetImage()
        aiImageBaseURL = AISettings.defaultImageBaseURL
        aiImageAPIKey = ""
        aiImageModel = AISettings.defaultImageModel
        aiImageSize = AISettings.defaultImageSize
        imageStatus = "插画 API 已恢复默认（智谱 BigModel · GLM-Image，API Key 已清空）。"
        imageStatusIsError = false
    }

    private func restoreVisionDefaults() {
        AISettings.resetVision()
        aiVisionBaseURL = AISettings.defaultVisionBaseURL
        aiVisionAPIKey = ""
        aiVisionModel = AISettings.defaultVisionModel
        visionStatus = "图片理解 API 已恢复默认（智谱 GLM-4V-Flash，API Key 已清空）。"
        visionStatusIsError = false
    }

    // MARK: - 数据行为

    private func exportDatabase() {
        guard let raw = store.exportDatabaseData() else {
            showMessage("数据库导出失败。", error: true)
            return
        }
        exportDocument = JSONDocument(data: raw)
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyyMMdd-HHmm"
        exportName = "YIBEI-Backup-\(formatter.string(from: Date()))"
        showExporter = true
    }

    private func importFile(_ url: URL) {
        let accessed = url.startAccessingSecurityScopedResource()
        defer {
            if accessed { url.stopAccessingSecurityScopedResource() }
        }
        guard let raw = try? Data(contentsOf: url) else {
            showMessage("文件读不出来。", error: true)
            return
        }
        let kind = BackupKindDetector.kind(of: raw)
        busy = true
        defer { busy = false }
        do {
            switch pendingAction {
            case .merge, .replace:
                guard kind == DatabaseBundle.kindValue else {
                    throw TransferError.message("这不是数据库备份文件。若要导入一杯配方，请使用「导入配方」。")
                }
                let text = try store.importDatabaseData(raw, replace: pendingAction == .replace)
                showMessage(text)
            case .recipe:
                guard kind == RecipeShareBundle.kindValue else {
                    throw TransferError.message("这不是离线配方文件。若要恢复整库，请使用「合并导入 / 覆盖恢复」。")
                }
                let text = try store.importRecipeData(raw)
                showMessage(text)
            }
        } catch {
            showMessage(error.localizedDescription, error: true)
        }
    }

    private func showMessage(_ text: String, error: Bool = false) {
        message = text
        messageIsError = error
    }

    // MARK: - 版本

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.2"
    }

    private var buildNumber: String {
        Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "3"
    }
}
