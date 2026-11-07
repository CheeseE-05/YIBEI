import Foundation
import SwiftUI
import PhotosUI
import UIKit

// MARK: - 宣传卡片编辑器
//
// 实时预览 + 设计工具（模板/比例/主题色/封面/文字）+ AI 生成文案 + 导出 PNG 到相册 +
// 保存配置到配方（Recipe.promoCard）。

struct PromoCardEditorView: View {
    @EnvironmentObject private var store: AppStore
    @Environment(\.dismiss) private var dismiss

    let recipe: Recipe
    let bean: Bean?
    let onOpenSettings: () -> Void

    @State private var config: PromoCardConfig
    @State private var priceText: String
    @State private var photoImage: UIImage?
    @State private var beanImage: UIImage?
    @State private var illustrationImage: UIImage?
    @State private var importedImage: UIImage?

    @State private var importedPickerItem: PhotosPickerItem?
    @State private var showImportedPicker = false
    @State private var isCuttingOut = false
    @State private var pendingOriginalImage: UIImage?

    // 布局编辑（task-20）
    @State private var isLayoutEditing = false
    @State private var selectedElement: PromoCardElement?
    @State private var elementFrames: [String: CGRect] = [:]
    @State private var cardSize: CGSize = .zero
    @State private var layoutConflict = false
    @State private var dragStartLayout: PromoCardElementLayout?
    @State private var dragStartElement: PromoCardElement?

    @State private var showDesign = false
    @State private var showAI = false
    @State private var showIllustration = false
    @State private var illustrationAuto = false
    @State private var exporting = false
    @State private var sharing = false
    @State private var showShare = false
    @State private var shareURL: URL?
    @State private var didLoad = false
    @State private var alert: PromoCardAlert?
    @State private var banner: String?
    @State private var aiApplied = false
    @State private var aiSheetAuto = false
    @State private var didRunDebugActions = false

    init(recipe: Recipe, bean: Bean?, onOpenSettings: @escaping () -> Void) {
        self.recipe = recipe
        self.bean = bean
        self.onOpenSettings = onOpenSettings
        _config = State(initialValue: recipe.promoCard ?? PromoCardConfig.makeDefault(for: recipe, bean: bean))
        _priceText = State(initialValue: recipe.price.map { "\($0)" } ?? "")
    }

    private var priceValue: Int? {
        let value = Int(priceText) ?? 0
        return value > 0 ? value : nil
    }

    var body: some View {
        VStack(spacing: 0) {
            topBar
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 16) {
                    if let banner {
                        bannerView(banner)
                    }
                    previewCard
                    actionRow
                    saveSection
                    tipsSection
                }
                .padding(.horizontal, 20)
                .padding(.top, 2)
                .padding(.bottom, 40)
                .frame(maxWidth: 760)
                .frame(maxWidth: .infinity)
            }
        }
        .background(PaperBackground())
        .photosPicker(isPresented: $showImportedPicker, selection: $importedPickerItem, matching: .images)
        .overlay {
            if isCuttingOut {
                ZStack {
                    C.ink.opacity(0.18).ignoresSafeArea()
                    VStack(spacing: 10) {
                        ProgressView().tint(C.lime)
                        Text("正在自动抠图…")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(C.ink)
                        Text("本机离线处理，背景会变成透明 PNG")
                            .font(.system(size: 11))
                            .foregroundStyle(C.muted)
                    }
                    .padding(22)
                    .background(C.card)
                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                    .shadow(color: Color.black.opacity(0.14), radius: 18, x: 0, y: 8)
                }
            }
        }
        .onChange(of: importedPickerItem) { _, newValue in
            guard let newValue else { return }
            loadImportedPhoto(newValue)
        }
        .onChange(of: config.template) { _, _ in
            clearManualLayoutForAutoReflow(message: "切换模板后已自动重排。")
        }
        .onChange(of: config.ratio) { _, _ in
            clearManualLayoutForAutoReflow(message: "切换横竖版后已自动重排。")
        }
        .onAppear { loadImagesIfNeeded() }
        .onAppear { runDebugActionsIfNeeded() }
        .sheet(isPresented: $showDesign) {
            PromoCardDesignSheet(
                config: $config,
                recipe: recipe,
                bean: bean,
                beanImage: beanImage,
                illustrationImage: illustrationImage,
                photoImage: $photoImage,
                priceText: $priceText,
                importedImage: importedImage,
                importedCutout: config.importedCutout == true,
                onRequestImport: {
                    showDesign = false
                    Task { @MainActor in
                        try? await Task.sleep(nanoseconds: 500_000_000)
                        showImportedPicker = true
                    }
                },
                onUseOriginalImport: {
                    showDesign = false
                    useOriginalImportedImage()
                },
                onReCutoutImport: {
                    showDesign = false
                    Task { @MainActor in
                        try? await Task.sleep(nanoseconds: 400_000_000)
                        recutImportedImage()
                    }
                },
                onRequestIllustration: {
                    showDesign = false
                    Task { @MainActor in
                        try? await Task.sleep(nanoseconds: 500_000_000)
                        illustrationAuto = true
                        showIllustration = true
                    }
                }
            )
            .environmentObject(store)
            .presentationDetents([.large])
        }
        .sheet(isPresented: $showIllustration) {
            PromoCardIllustrationSheet(
                recipe: recipe,
                bean: bean,
                config: $config,
                illustrationImage: $illustrationImage,
                autoRun: illustrationAuto,
                onApplied: { summary in
                    banner = "AI 插画已生成并套用：\(summary)"
                },
                onOpenSettings: {
                    showIllustration = false
                    onOpenSettings()
                }
            )
            .environmentObject(store)
            .presentationDetents([.large])
        }
        .sheet(isPresented: $showShare) {
            if let shareURL {
                ShareSheet(items: [shareURL])
            }
        }
        .sheet(isPresented: $showAI) {
            PromoCardAISheet(
                recipe: recipe,
                bean: bean,
                config: $config,
                autoRun: aiSheetAuto,
                onApplied: { summary in
                    aiApplied = true
                    banner = "AI 已生成并套用：\(summary)"
                },
                onOpenSettings: {
                    showAI = false
                    onOpenSettings()
                }
            )
            .environmentObject(store)
            .presentationDetents([.large])
        }
        .alert(item: $alert) { item in
            switch item {
            case .exportSuccess:
                return Alert(
                    title: Text("已保存到相册"),
                    message: Text("「\(config.title)」的随杯卡已保存到相册（\(exportPixelText)），可以直接分享或打印。"),
                    dismissButton: .default(Text("好"))
                )
            case .cutoutFailed:
                return Alert(
                    title: Text("自动抠图没有成功"),
                    message: Text("这张图没有识别到清晰前景（或背景太复杂）。可以保留原图直接用，也可以换一张背景干净的照片。"),
                    primaryButton: .default(Text("使用原图")) {
                        useOriginalImportedImage()
                    },
                    secondaryButton: .cancel(Text("换一张")) {
                        showImportedPicker = true
                    }
                )
            case .permissionDenied:
                return Alert(
                    title: Text("无法保存到相册"),
                    message: Text(CardExportError.permissionDenied.localizedDescription),
                    primaryButton: .default(Text("去系统设置")) {
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    },
                    secondaryButton: .cancel(Text("取消"))
                )
            case .failure(let message):
                return Alert(
                    title: Text("导出失败"),
                    message: Text(message),
                    dismissButton: .default(Text("好"))
                )
            }
        }
        .toolbar(.hidden, for: .navigationBar)
    }

    // MARK: - 顶部

    private var topBar: some View {
        HStack(spacing: 10) {
            RoundIconButton(systemName: "chevron.left", accessibilityLabel: "返回") {
                dismiss()
            }
            VStack(alignment: .leading, spacing: 2) {
                Overline(text: "PROMO CARD")
                Text("宣传卡片")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(C.ink)
            }
            Spacer()
            Button {
                saveToRecipe()
            } label: {
                HStack(spacing: 5) {
                    Image(systemName: "checkmark").font(.system(size: 11, weight: .bold))
                    Text("保存").font(.system(size: 12, weight: .medium))
                }
                .foregroundStyle(C.onAccent)
                .padding(.horizontal, 16)
                .frame(height: 38)
                .background(C.lime)
                .clipShape(Capsule())
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 20)
        .padding(.top, 8)
        .padding(.bottom, 10)
    }

    private var previewCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Overline(text: "LIVE PREVIEW")
                Spacer()
                Text("\(config.template.label) · \(config.ratio.label) · \(config.effectiveExportSize == .printA6 ? "A6 打印" : "社交")")
                    .font(.mono(9))
                    .foregroundStyle(C.muted)
            }
            cardWithLayoutOverlay
                .frame(maxWidth: .infinity)
                .shadow(color: Color.black.opacity(0.14), radius: 18, x: 0, y: 10)
            if config.effectiveExportSize == .printA6 {
                Text("A6 300dpi 打印尺寸：\(exportPixelText)，浅色纸打印效果最佳")
                    .font(.system(size: 9.5))
                    .foregroundStyle(C.muted)
            }
            if isLayoutEditing {
                layoutPanel
            }
        }
    }

    /// 卡片 + 布局编辑覆盖层（虚线选框 / 拖拽 / 比例坐标换算）。
    private var cardWithLayoutOverlay: some View {
        GeometryReader { geo in
            ZStack(alignment: .topLeading) {
                PromoCardView(
                    recipe: recipe,
                    bean: bean,
                    config: config,
                    price: priceValue,
                    photoImage: photoImage,
                    beanImage: beanImage,
                    illustrationImage: illustrationImage,
                    importedImage: importedImage,
                    onElementFrames: { frames in
                        elementFrames = frames
                        if cardSize == .zero { cardSize = geo.size }
                    }
                )
                .onAppear { cardSize = geo.size }
                .onChange(of: geo.size) { _, newValue in cardSize = newValue }

                if isLayoutEditing {
                    layoutOverlay(cardSize: geo.size)
                }
            }
        }
        .aspectRatio(config.ratio.aspectRatio(for: config.effectiveExportSize), contentMode: .fit)
    }

    private func layoutOverlay(cardSize: CGSize) -> some View {
        ZStack(alignment: .topLeading) {
            ForEach(PromoCardElement.allCases.filter { !hiddenElements.contains($0) }) { element in
                if let frame = displayedFrame(for: element, cardSize: cardSize) {
                    let isSelected = selectedElement == element
                    RoundedRectangle(cornerRadius: 6, style: .continuous)
                        .stroke(
                            isSelected ? C.limeDeep : C.line,
                            style: StrokeStyle(lineWidth: isSelected ? 1.6 : 1, dash: [5, 4])
                        )
                        .background(
                            RoundedRectangle(cornerRadius: 6, style: .continuous)
                                .fill(isSelected ? C.lime.opacity(0.10) : Color.clear)
                        )
                        .overlay(alignment: .topLeading) {
                            if isSelected {
                                Text(element.label)
                                    .font(.system(size: 8, weight: .medium))
                                    .foregroundStyle(C.onAccent)
                                    .padding(.horizontal, 4)
                                    .padding(.vertical, 2)
                                    .background(C.lime)
                                    .clipShape(RoundedRectangle(cornerRadius: 4))
                                    .offset(y: -14)
                            }
                        }
                        .frame(width: max(frame.width, 16), height: max(frame.height, 16))
                        .position(x: frame.midX, y: frame.midY)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            selectedElement = element
                            haptic()
                        }
                        .gesture(
                            DragGesture(minimumDistance: 1)
                                .onChanged { value in
                                    selectedElement = element
                                    dragElement(element, translation: value.translation, cardSize: cardSize)
                                }
                                .onEnded { _ in
                                    endDrag()
                                }
                        )
                }
            }
        }
    }

    private var actionRow: some View {
        VStack(spacing: 8) {
            HStack(spacing: 8) {
                toolButton(title: "设计工具", symbol: "slider.horizontal.3") { showDesign = true }
                toolButton(title: "AI 插画", symbol: "paintbrush.pointed") {
                    illustrationAuto = true
                    showIllustration = true
                }
                toolButton(title: importedImage == nil ? "导入图片" : "换导入图", symbol: "photo.badge.arrow.down") {
                    showImportedPicker = true
                }
                toolButton(title: "AI 文案", symbol: "sparkles") {
                    aiSheetAuto = false
                    showAI = true
                }
            }
            HStack(spacing: 8) {
                toolButton(title: isLayoutEditing ? "完成布局" : "编辑布局", symbol: "hand.draw") {
                    isLayoutEditing.toggle()
                    if isLayoutEditing {
                        selectedElement = nil
                        layoutConflict = hasLayoutConflict()
                    } else {
                        selectedElement = nil
                    }
                }
                toolButton(title: exporting ? "导出中…" : "导出到相册", symbol: "square.and.arrow.down") {
                    exportCard()
                }
                .disabled(exporting)
                toolButton(title: sharing ? "准备中…" : "分享 / 打印", symbol: "square.and.arrow.up") {
                    shareCard()
                }
                .disabled(sharing)
            }
            exportSizePicker
        }
    }

    private var exportSizePicker: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                ForEach(PromoCardExportSize.allCases) { size in
                    SmallChoiceButton(title: size.label, selected: config.effectiveExportSize == size) {
                        config.exportSize = size
                    }
                }
                Spacer(minLength: 0)
            }
            Text("\(config.effectiveExportSize.caption) · \(exportPixelText) · PNG")
                .font(.system(size: 9.5))
                .foregroundStyle(C.muted)
        }
    }

    private var exportPixelText: String {
        let size = PromoCardExporter.exportPixelSize(for: config)
        return "\(Int(size.width))×\(Int(size.height)) px"
    }

    private func toolButton(title: String, symbol: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: symbol)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(C.forest)
                Text(title)
                    .font(.system(size: 10.5, weight: .medium))
                    .foregroundStyle(C.inkSoft)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 68)
            .background(C.card)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(C.lineSoft, lineWidth: 1)
            }
        }
        .buttonStyle(.plain)
    }

    private var saveSection: some View {
        VStack(alignment: .leading, spacing: 9) {
            PrimaryActionButton(title: "保存到「\(recipe.name)」", icon: "checkmark") {
                saveToRecipe()
            }
            Text("卡片配置会跟着这杯配方一起保存在本机，导出 / 备份数据时一起带走。")
                .font(.system(size: 10.5))
                .foregroundStyle(C.muted)
                .lineSpacing(2)
        }
    }

    private var tipsSection: some View {
        HStack(alignment: .top, spacing: 9) {
            Image(systemName: "sparkles")
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(C.forest)
                .frame(width: 28, height: 28)
                .background(C.limeSoft)
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 4) {
                Text("怎么用")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(C.ink)
                Text("先用「设计工具」挑横版/竖版和封面；「AI 插画」会按这杯配方画一张手绘水彩咖啡插画作为主视觉；「AI 文案」写标题/副标题/标签。满意后导出 PNG 到相册，或直接分享/打印成咖啡店随杯卡。")
                    .font(.system(size: 10.5))
                    .foregroundStyle(C.muted)
                    .lineSpacing(3)
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(C.paperSoft)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private func bannerView(_ text: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(C.forest)
            Text(text)
                .font(.system(size: 11.5, weight: .medium))
                .foregroundStyle(C.inkSoft)
                .lineLimit(2)
            Spacer(minLength: 0)
            Button {
                banner = nil
            } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundStyle(C.muted)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 13)
        .padding(.vertical, 10)
        .background(C.limeSoft)
        .clipShape(RoundedRectangle(cornerRadius: 15, style: .continuous))
    }

    // MARK: - 布局编辑（task-20）

    private var hiddenElements: Set<PromoCardElement> { Set(config.hiddenElements ?? []) }
    private var visibleElements: [PromoCardElement] { PromoCardElement.allCases.filter { !hiddenElements.contains($0) } }

    private var currentLayouts: [String: PromoCardElementLayout] {
        config.manualLayout?.layouts(for: config.ratio) ?? [:]
    }

    private func setCurrentLayouts(_ layouts: [String: PromoCardElementLayout]) {
        var manual = config.manualLayout ?? PromoCardManualLayout()
        manual.setLayouts(layouts, for: config.ratio)
        config.manualLayout = manual.isEmpty ? nil : manual
    }

    private func baseFrame(for element: PromoCardElement, cardSize: CGSize) -> CGRect? {
        if let layout = currentLayouts[element.rawValue],
           let width = layout.width, let height = layout.height {
            return CGRect(
                x: 0, y: 0,
                width: width * cardSize.width,
                height: height * cardSize.height
            )
        }
        return elementFrames[element.rawValue]
    }

    /// 当前显示位置：手动布局按归一化中心 + 缩放；自动布局用测量到的真实 frame。
    private func displayedFrame(for element: PromoCardElement, cardSize: CGSize) -> CGRect? {
        guard cardSize.width > 1, cardSize.height > 1 else { return nil }
        if let layout = currentLayouts[element.rawValue],
           let width = layout.width, let height = layout.height {
            let scale = CGFloat(layout.scale)
            let w = width * cardSize.width * scale
            let h = height * cardSize.height * scale
            return CGRect(
                x: layout.centerX * cardSize.width - w / 2,
                y: layout.centerY * cardSize.height - h / 2,
                width: w,
                height: h
            )
        }
        guard let frame = elementFrames[element.rawValue], frame.width > 0, frame.height > 0 else { return nil }
        return frame
    }

    /// 第一次拖动/缩放时，把当前自动排版的真实位置冻结为手动布局（其余元素保持原位）。
    private func enterManualLayoutIfNeeded(cardSize: CGSize) {
        guard currentLayouts.isEmpty else { return }
        var layouts: [String: PromoCardElementLayout] = [:]
        for element in visibleElements {
            guard let frame = elementFrames[element.rawValue], frame.width > 1, frame.height > 1 else { continue }
            layouts[element.rawValue] = PromoCardElementLayout(
                centerX: frame.midX / cardSize.width,
                centerY: frame.midY / cardSize.height,
                scale: 1,
                width: frame.width / cardSize.width,
                height: frame.height / cardSize.height,
                alignment: nil
            )
        }
        guard !layouts.isEmpty else { return }
        setCurrentLayouts(layouts)
        // 手动模式不再上报自动 frame，保留最后一份用于参考。
        selectedElement = selectedElement ?? visibleElements.first
    }

    private func dragElement(_ element: PromoCardElement, translation: CGSize, cardSize: CGSize) {
        guard cardSize.width > 1, cardSize.height > 1 else { return }
        enterManualLayoutIfNeeded(cardSize: cardSize)
        if dragStartElement != element {
            dragStartElement = element
            dragStartLayout = currentLayouts[element.rawValue]
        }
        guard var layout = currentLayouts[element.rawValue], let start = dragStartLayout else { return }
        let dx = Double(translation.width / cardSize.width)
        let dy = Double(translation.height / cardSize.height)
        let halfW = (layout.width ?? 0.2) * layout.scale / 2
        let halfH = (layout.height ?? 0.1) * layout.scale / 2
        let cx = min(max(start.centerX + dx, halfW), 1 - halfW)
        let cy = min(max(start.centerY + dy, halfH), 1 - halfH)
        layout.centerX = cx
        layout.centerY = cy
        var layouts = currentLayouts
        layouts[element.rawValue] = layout
        setCurrentLayouts(layouts)
        layoutConflict = hasLayoutConflict()
    }

    private func endDrag() {
        guard let element = dragStartElement else { return }
        layoutConflict = hasLayoutConflict()
        if layoutConflict, let start = dragStartLayout {
            var layouts = currentLayouts
            layouts[element.rawValue] = start
            setCurrentLayouts(layouts)
            layoutConflict = hasLayoutConflict()
            banner = "元素会互相遮挡，已回退到上一个位置；也可以点「恢复自动排版」。"
        }
        dragStartElement = nil
        dragStartLayout = nil
    }

    /// 手动布局元素两两求交：任何重叠都视为 conflict（保存/导出前必须消除）。
    private func hasLayoutConflict() -> Bool {
        guard !currentLayouts.isEmpty, cardSize.width > 1 else { return false }
        var frames: [(PromoCardElement, CGRect)] = []
        for element in visibleElements {
            if let frame = displayedFrame(for: element, cardSize: cardSize) {
                frames.append((element, frame.insetBy(dx: 1.5, dy: 1.5)))
            }
        }
        for i in frames.indices {
            for j in frames.indices where j > i {
                if frames[i].1.intersects(frames[j].1) { return true }
            }
        }
        return false
    }

    private func selectedLayoutBinding() -> Binding<Double>? {
        nil
    }

    private func currentFontSize(for element: PromoCardElement) -> Double {
        let sizes = config.fontSizes?.sizes(for: config.ratio) ?? [:]
        if let custom = sizes[element.rawValue] {
            return min(max(custom, 8), 72)
        }
        return Double(promoDefaultFontSize(for: element, config: config))
    }

    private func setFontSize(_ value: Double, for element: PromoCardElement) {
        guard element.isText else { return }
        let clamped = min(max(value, 8), 72)
        var sizes = config.fontSizes ?? PromoCardFontSizes()
        var current = sizes.sizes(for: config.ratio)
        current[element.rawValue] = clamped
        sizes.setSizes(current, for: config.ratio)
        config.fontSizes = sizes.isEmpty ? nil : sizes
        layoutConflict = hasLayoutConflict()
    }

    private func resetFontSize(for element: PromoCardElement) {
        var sizes = config.fontSizes ?? PromoCardFontSizes()
        var current = sizes.sizes(for: config.ratio)
        current[element.rawValue] = nil
        sizes.setSizes(current, for: config.ratio)
        config.fontSizes = sizes.isEmpty ? nil : sizes
        layoutConflict = hasLayoutConflict()
    }

    private func adjustScale(_ delta: Double) {
        guard let selectedElement else { return }
        enterManualLayoutIfNeeded(cardSize: cardSize)
        guard var layout = currentLayouts[selectedElement.rawValue] else { return }
        layout.scale = min(max(layout.scale + delta, 0.5), 2.0)
        var layouts = currentLayouts
        layouts[selectedElement.rawValue] = layout
        setCurrentLayouts(layouts)
        layoutConflict = hasLayoutConflict()
    }

    private func setAlignment(_ alignment: TextAlignment) {
        guard let selectedElement, selectedElement.isText else { return }
        enterManualLayoutIfNeeded(cardSize: cardSize)
        guard var layout = currentLayouts[selectedElement.rawValue] else { return }
        switch alignment {
        case .center: layout.alignment = "center"
        case .trailing: layout.alignment = "right"
        default: layout.alignment = "left"
        }
        var layouts = currentLayouts
        layouts[selectedElement.rawValue] = layout
        setCurrentLayouts(layouts)
        layoutConflict = hasLayoutConflict()
    }

    private func toggleElementVisibility(_ element: PromoCardElement) {
        var hidden = hiddenElements
        if hidden.contains(element) { hidden.remove(element) } else { hidden.insert(element) }
        config.hiddenElements = hidden.isEmpty ? nil : Array(hidden)
        if selectedElement == element, hidden.contains(element) { selectedElement = nil }
        layoutConflict = hasLayoutConflict()
        haptic()
    }

    private func restoreAutoLayout() {
        isLayoutEditing = false
        if var manual = config.manualLayout {
            manual.setLayouts([:], for: config.ratio)
            config.manualLayout = manual.isEmpty ? nil : manual
        }
        selectedElement = nil
        layoutConflict = false
        dragStartLayout = nil
        dragStartElement = nil
        haptic()
        banner = "已恢复自动排版：按真实文本高度流式重排，元素不会互相遮挡。"
    }

    private func applyDebugMove(_ element: PromoCardElement, centerX: Double, centerY: Double) {
        guard var layout = currentLayouts[element.rawValue] else { return }
        layout.centerX = min(max(centerX, 0.05), 0.95)
        layout.centerY = min(max(centerY, 0.05), 0.95)
        var layouts = currentLayouts
        layouts[element.rawValue] = layout
        setCurrentLayouts(layouts)
        layoutConflict = hasLayoutConflict()
    }

    private func clearManualLayoutForAutoReflow(message: String) {
        config.manualLayout = nil
        selectedElement = nil
        layoutConflict = false
        dragStartLayout = nil
        dragStartElement = nil
        guard isLayoutEditing else { return }
        banner = message
    }

    private var layoutPanel: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("编辑布局")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(C.ink)
                    Text("在卡片上点选/拖动元素；文字可调字号与对齐，图片可缩放")
                        .font(.system(size: 9.5))
                        .foregroundStyle(C.muted)
                }
                Spacer(minLength: 0)
                Button {
                    restoreAutoLayout()
                } label: {
                    Text("恢复自动排版")
                        .font(.system(size: 10.5, weight: .medium))
                        .foregroundStyle(C.forest)
                        .padding(.horizontal, 10)
                        .frame(height: 30)
                        .background(C.limeSoft)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 7) {
                    ForEach(PromoCardElement.allCases) { element in
                        HStack(spacing: 4) {
                            Button {
                                selectedElement = element
                            } label: {
                                Text(element.label)
                                    .font(.system(size: 10.5, weight: selectedElement == element ? .semibold : .regular))
                                    .foregroundStyle(selectedElement == element ? C.onAccent : (hiddenElements.contains(element) ? C.mutedLight : C.inkSoft))
                                    .padding(.horizontal, 9)
                                    .frame(height: 28)
                                    .background(selectedElement == element ? C.lime : C.paperSoft)
                                    .clipShape(Capsule())
                            }
                            .buttonStyle(.plain)
                            Button {
                                toggleElementVisibility(element)
                            } label: {
                                Image(systemName: hiddenElements.contains(element) ? "eye.slash" : "eye")
                                    .font(.system(size: 9))
                                    .foregroundStyle(hiddenElements.contains(element) ? C.mutedLight : C.forest)
                                    .frame(width: 22, height: 28)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }

            if let selected = selectedElement, !hiddenElements.contains(selected) {
                HStack(spacing: 8) {
                    Text(selected.label)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(C.inkSoft)
                    if selected.isText {
                        Button { setAlignment(.leading) } label: {
                            Image(systemName: "text.alignleft")
                                .font(.system(size: 11))
                                .frame(width: 30, height: 30)
                        }
                        .buttonStyle(.plain)
                        Button { setAlignment(.center) } label: {
                            Image(systemName: "text.aligncenter")
                                .font(.system(size: 11))
                                .frame(width: 30, height: 30)
                        }
                        .buttonStyle(.plain)
                        Button { setAlignment(.trailing) } label: {
                            Image(systemName: "text.alignright")
                                .font(.system(size: 11))
                                .frame(width: 30, height: 30)
                        }
                        .buttonStyle(.plain)
                    }
                    Spacer(minLength: 0)
                    Button { adjustScale(-0.05) } label: {
                        Image(systemName: "minus.magnifyingglass").font(.system(size: 12))
                            .frame(width: 30, height: 30)
                    }
                    .buttonStyle(.plain)
                    Text("\(Int(((currentLayouts[selected.rawValue]?.scale ?? 1) * 100).rounded()))%")
                        .font(.mono(10))
                        .foregroundStyle(C.muted)
                        .frame(width: 34)
                    Button { adjustScale(0.05) } label: {
                        Image(systemName: "plus.magnifyingglass").font(.system(size: 12))
                            .frame(width: 30, height: 30)
                    }
                    .buttonStyle(.plain)
                }
                .foregroundStyle(C.inkSoft)
                .padding(.horizontal, 10)
                .frame(height: 40)
                .background(C.paperSoft)
                .clipShape(RoundedRectangle(cornerRadius: 13, style: .continuous))

                if selected.isText {
                    HStack(spacing: 8) {
                        Text("字号")
                            .font(.system(size: 11, weight: .medium))
                            .foregroundStyle(C.inkSoft)
                        Slider(
                            value: Binding(
                                get: { currentFontSize(for: selected) },
                                set: { setFontSize($0, for: selected) }
                            ),
                            in: 8...72,
                            step: 1
                        )
                        .tint(C.lime)
                        Text("\(Int(currentFontSize(for: selected).rounded()))pt")
                            .font(.mono(10))
                            .foregroundStyle(C.muted)
                            .frame(width: 40, alignment: .trailing)
                        Button {
                            resetFontSize(for: selected)
                        } label: {
                            Image(systemName: "arrow.counterclockwise")
                                .font(.system(size: 10))
                                .foregroundStyle(C.muted)
                                .frame(width: 24, height: 24)
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(.horizontal, 10)
                    .frame(height: 40)
                    .background(C.paperSoft)
                    .clipShape(RoundedRectangle(cornerRadius: 13, style: .continuous))
                }
            }

            if layoutConflict {
                HStack(spacing: 7) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 11))
                        .foregroundStyle(C.roseDeep)
                    Text("元素有重叠：请拖开或点「恢复自动排版」（重叠时不能保存/导出）")
                        .font(.system(size: 10.5, weight: .medium))
                        .foregroundStyle(C.roseDeep)
                        .lineLimit(2)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(C.rose.opacity(0.7))
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
        }
        .padding(12)
        .background(C.card)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(C.lineSoft, lineWidth: 1)
        }
    }

    private func guardBeforeOutput() -> Bool {
        // 自动布局由真实文本高度流式排版，天然不重叠；手动布局需要检查碰撞。
        if hasLayoutConflict() {
            layoutConflict = true
            banner = "元素有重叠，请先拖开或恢复自动排版，再保存/导出。"
            return false
        }
        return true
    }

    // MARK: - 行为

    private func loadImagesIfNeeded() {
        guard !didLoad else { return }
        didLoad = true
        if let path = config.coverImagePath {
            photoImage = store.uiImage(path: path)
        }
        if let path = config.illustrationImagePath {
            illustrationImage = store.uiImage(path: path)
        }
        if let path = config.importedImagePath {
            importedImage = store.uiImage(path: path)
        }
        if let bean {
            beanImage = store.uiImage(for: bean)
        }
    }

    private func saveToRecipe() {
        guard guardBeforeOutput() else { return }
        guard let updated = store.savePromoCard(config, for: recipe.id) else {
            alert = .failure("保存失败：这杯配方可能已经被删除。")
            return
        }
        config = updated.promoCard ?? config
        store.updateRecipePrice(priceValue, for: recipe.id)
        haptic()
        if let priceValue {
            banner = "已保存到「\(updated.name)」· 菜单价格 ¥\(priceValue)"
        } else {
            banner = "已保存到「\(updated.name)」"
        }
    }

    private func exportCard() {
        guard !exporting, guardBeforeOutput() else { return }
        exporting = true
        Task { @MainActor in
            guard let data = PromoCardExporter.renderPNG(
                recipe: recipe,
                bean: bean,
                config: config,
                price: priceValue,
                photoImage: photoImage,
                beanImage: beanImage,
                illustrationImage: illustrationImage,
                importedImage: importedImage
            ) else {
                exporting = false
                alert = .failure(CardExportError.renderFailed.localizedDescription)
                return
            }
            do {
                try await PromoCardExporter.saveToPhotoLibrary(data)
                exporting = false
                haptic()
                alert = .exportSuccess
            } catch CardExportError.permissionDenied {
                exporting = false
                alert = .permissionDenied
            } catch {
                exporting = false
                alert = .failure(error.localizedDescription)
            }
        }
    }

    private func shareCard() {
        guard !sharing, guardBeforeOutput() else { return }
        sharing = true
        Task { @MainActor in
            defer { sharing = false }
            guard let url = PromoCardExporter.makeShareFile(
                recipe: recipe,
                bean: bean,
                config: config,
                price: priceValue,
                photoImage: photoImage,
                beanImage: beanImage,
                illustrationImage: illustrationImage,
                importedImage: importedImage
            ) else {
                alert = .failure(CardExportError.renderFailed.localizedDescription)
                return
            }
            haptic()
            shareURL = url
            showShare = true
        }
    }

    // MARK: - 相册导入 + 自动抠图

    private func loadImportedPhoto(_ item: PhotosPickerItem) {
        Task { @MainActor in
            guard
                let data = try? await item.loadTransferable(type: Data.self),
                let image = UIImage(data: data)
            else {
                alert = .failure("这张照片读不出来，换一张试试。")
                importedPickerItem = nil
                return
            }
            pendingOriginalImage = image.bbDownscaled(maxDimension: 1600)
            await applyImportedPhoto(pendingOriginalImage)
            importedPickerItem = nil
        }
    }

    private func applyImportedPhoto(_ image: UIImage?) async {
        guard let image else { return }
        pendingOriginalImage = image
        banner = nil

        // 相册导入：如果图片本身带透明背景（PNG alpha），直接保留透明，
        // 不再跑 Vision / 本地抠图，避免破坏已有 alpha 或错误抠掉主体。
        if image.bbHasSignificantTransparency() {
            isCuttingOut = false
            let trimmed = image.bbTrimmingTransparentMargins(paddingFraction: 0.015)
                .bbNormalizedAlphaImage()
            let originalPath = store.saveImage(image)
            let transparentPath = store.saveImage(trimmed) ?? originalPath
            importedImage = trimmed
            config.importedImagePath = transparentPath
            config.importedOriginalImagePath = originalPath
            config.importedCutout = true
            config.illustrationSource = .imported
            config.cover = .imported
            pendingOriginalImage = nil
            haptic()
            #if DEBUG
            print("[ImportCutout] 检测到透明 PNG：保留原 alpha，跳过 Vision/自动抠图")
            #endif
            banner = "已使用透明背景导入：保留原 alpha 通道，未触发自动抠图，可直接叠在奶油纸上"
            return
        }

        isCuttingOut = true
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("-uiTestPromoImportCutoutSlow") {
            try? await Task.sleep(nanoseconds: 2_800_000_000)
        }
        #endif
        var cutout = await ImageCutout.removeBackground(from: image)
        var usedLocalFallback = false
        if cutout == nil {
            // Vision 前景蒙版在当前环境不可用（例如部分模拟器）时，用本地背景色分离降级，
            // 对「干净浅色背景」的照片同样能得到透明 PNG。
            cutout = await ImageCutout.removeUniformBackground(from: image)
            usedLocalFallback = cutout != nil
        }
        isCuttingOut = false
        let normalizedCutout = cutout?.bbNormalizedAlphaImage()

        // 原图先存一份，抠图失败时可以一键回退。
        let originalPath = store.saveImage(image)

        if let cutout = normalizedCutout {
            let cutoutPath = store.saveImage(cutout)
            importedImage = cutout
            config.importedImagePath = cutoutPath
            config.importedOriginalImagePath = originalPath
            config.importedCutout = true
            config.illustrationSource = .imported
            config.cover = .imported
            pendingOriginalImage = nil
            haptic()
            banner = usedLocalFallback
                ? "已抠图（当前环境不支持 Vision 前景蒙版，使用本地背景分离降级）：透明背景可直接叠在奶油纸上"
                : "已自动抠图：背景已变透明，可直接叠在奶油纸上（可重新导入或改用原图）"
        } else {
            config.importedOriginalImagePath = originalPath
            alert = .cutoutFailed
        }
    }

    private func useOriginalImportedImage() {
        guard let original = pendingOriginalImage ?? store.uiImage(path: config.importedOriginalImagePath) else {
            alert = .failure("找不到导入的原图，请重新导入一张。")
            return
        }
        let path = store.saveImage(original) ?? config.importedOriginalImagePath
        importedImage = original
        config.importedImagePath = path
        config.importedCutout = false
        config.illustrationSource = .imported
        config.cover = .imported
        pendingOriginalImage = nil
        haptic()
        banner = "已改用原图（未抠图），也可以随时重新抠图"
    }

    private func recutImportedImage() {
        let source = pendingOriginalImage ?? store.uiImage(path: config.importedOriginalImagePath) ?? importedImage
        guard let source else {
            alert = .failure("找不到导入的原图，请重新导入一张。")
            return
        }
        Task { @MainActor in
            await applyImportedPhoto(source)
        }
    }

    // MARK: - DEBUG 截图辅助

    private func runDebugActionsIfNeeded() {
        #if DEBUG
        guard !didRunDebugActions else { return }
        didRunDebugActions = true
        let arguments = ProcessInfo.processInfo.arguments
        if arguments.contains("-uiTestPromoDesign") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 1_400_000_000)
                showDesign = true
            }
        }
        if arguments.contains("-uiTestPromoAI") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 1_400_000_000)
                aiSheetAuto = true
                showAI = true
            }
        }
        // 截图脚本用：AI 生成并套用后自动收起 AI 弹窗，露出已套用的卡片预览。
        if arguments.contains("-uiTestPromoAIApply") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 4_200_000_000)
                showAI = false
            }
        }
        if arguments.contains("-uiTestPromoExport") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 2_000_000_000)
                exportCard()
            }
        }
        if let index = arguments.firstIndex(of: "-uiTestPromoPrice"),
           arguments.indices.contains(index + 1) {
            priceText = arguments[index + 1]
        }
        if let index = arguments.firstIndex(of: "-uiTestPromoImportSource"),
           arguments.indices.contains(index + 1) {
            let path = arguments[index + 1]
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 1_200_000_000)
                if let image = store.uiImage(path: path) {
                    await applyImportedPhoto(image.bbDownscaled(maxDimension: 1600))
                } else {
                    alert = .failure("DEBUG 测试图片不存在：\(path)")
                }
            }
        }
        if arguments.contains("-uiTestPromoImportForceFail") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 1_500_000_000)
                pendingOriginalImage = UIImage(systemName: "photo")
                alert = .cutoutFailed
            }
        }
        if arguments.contains("-uiTestPromoImportSave") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 8_000_000_000)
                saveToRecipe()
            }
        }
        if arguments.contains("-uiTestCardFontSize") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 3_200_000_000)
                isLayoutEditing = true
                try? await Task.sleep(nanoseconds: 700_000_000)
                selectedElement = .englishName
                setFontSize(46, for: .englishName)
                setFontSize(34, for: .chineseName)
            }
        }
        if arguments.contains("-uiTestAISaveCardAfterApply") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 9_000_000_000)
                saveToRecipe()
            }
        }
        if arguments.contains("-uiTestCardSaveLayout") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 9_500_000_000)
                saveToRecipe()
            }
        }
        if arguments.contains("-uiTestCardEditLayout") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 2_500_000_000)
                isLayoutEditing = true
            }
        }
        if arguments.contains("-uiTestCardDragText") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 3_200_000_000)
                isLayoutEditing = true
                try? await Task.sleep(nanoseconds: 700_000_000)
                enterManualLayoutIfNeeded(cardSize: cardSize)
                selectedElement = .price
                // 把价格从右上拖到卡片右侧中部空白处，演示文字元素自由定位且不遮挡。
                applyDebugMove(.price, centerX: 0.80, centerY: 0.46)
            }
        }
        if arguments.contains("-uiTestCardScaleImage") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 3_200_000_000)
                isLayoutEditing = true
                try? await Task.sleep(nanoseconds: 700_000_000)
                enterManualLayoutIfNeeded(cardSize: cardSize)
                selectedElement = .illustration
                if var layout = currentLayouts[PromoCardElement.illustration.rawValue] {
                    layout.scale = 1.10
                    var layouts = currentLayouts
                    layouts[PromoCardElement.illustration.rawValue] = layout
                    setCurrentLayouts(layouts)
                }
                layoutConflict = hasLayoutConflict()
            }
        }
        if arguments.contains("-uiTestCardCollision") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 3_200_000_000)
                isLayoutEditing = true
                try? await Task.sleep(nanoseconds: 700_000_000)
                enterManualLayoutIfNeeded(cardSize: cardSize)
                selectedElement = .chineseName
                if let layout = currentLayouts[PromoCardElement.englishName.rawValue] {
                    applyDebugMove(.chineseName, centerX: layout.centerX, centerY: layout.centerY + 0.015)
                }
                layoutConflict = hasLayoutConflict()
            }
        }
        if arguments.contains("-uiTestCardRestoreAuto") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 3_200_000_000)
                isLayoutEditing = true
                try? await Task.sleep(nanoseconds: 700_000_000)
                enterManualLayoutIfNeeded(cardSize: cardSize)
                try? await Task.sleep(nanoseconds: 900_000_000)
                restoreAutoLayout()
            }
        }
        if arguments.contains("-uiTestPromoPortrait") {
            config.ratio = .portrait
        }
        if arguments.contains("-uiTestPromoLandscape") {
            config.ratio = .wide
        }
        if let index = arguments.firstIndex(of: "-uiTestAIProcessSource"),
           arguments.indices.contains(index + 1) {
            let path = arguments[index + 1]
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 2_200_000_000)
                guard let image = store.uiImage(path: path) else {
                    alert = .failure("DEBUG 测试图不存在：\(path)")
                    return
                }
                let processed = await AIIllustrationProcessor.process(image)
                illustrationImage = processed.image
                config.cover = .illustration
                config.illustrationSource = .ai
                if let saved = store.saveCardIllustration(processed.image) {
                    config.illustrationImagePath = saved
                }
                if let rawPath = store.saveCardIllustration(image) {
                    config.illustrationRawImagePath = rawPath
                }
                config.illustrationProcessNote = processed.fallbackReason
                    ?? "\(processed.method.label) · 清理 \(processed.removedWatermarkBlocks) 处"
                config.illustrationRemovedWatermarkBlocks = processed.removedWatermarkBlocks
                banner = processed.fallbackReason
                    ?? "\(processed.method.label) · 透明像素 \(Int(processed.transparentFraction * 100))% · 清理 \(processed.removedWatermarkBlocks) 处水印"
            }
        }
        if let index = arguments.firstIndex(of: "-uiTestTransparencyProbe"),
           arguments.indices.contains(index + 1) {
            let path = arguments[index + 1]
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 1_800_000_000)
                if let image = store.uiImage(path: path) {
                    let fraction = image.bbTransparencyFraction()
                    banner = "透明检测：\(String(format: "%.1f", fraction * 100))% 像素 alpha<250 · hasAlpha=\(image.cgImage?.alphaInfo != .none)"
                } else {
                    banner = "透明检测：图片读取失败"
                }
            }
        }
        if let index = arguments.firstIndex(of: "-uiTestAICutoutImpure"),
           arguments.indices.contains(index + 1) {
            let path = arguments[index + 1]
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 2_200_000_000)
                guard let image = store.uiImage(path: path) else {
                    alert = .failure("DEBUG 测试图不存在：\(path)")
                    return
                }
                let processed = await AIIllustrationProcessor.process(image)
                illustrationImage = processed.image
                config.cover = .illustration
                config.illustrationSource = .ai
                if let saved = store.saveCardIllustration(processed.image) {
                    config.illustrationImagePath = saved
                }
                if let rawPath = store.saveCardIllustration(image) {
                    config.illustrationRawImagePath = rawPath
                }
                config.illustrationProcessNote = processed.fallbackReason
                banner = processed.fallbackReason ?? "AI 插画处理完成"
            }
        }
        if arguments.contains("-uiTestPromoIllustrationOnly") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 1_400_000_000)
                illustrationAuto = false
                showIllustration = true
            }
        }
        if arguments.contains("-uiTestPromoIllustration") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 1_400_000_000)
                illustrationAuto = true
                showIllustration = true
            }
        }
        // 截图脚本用：AI 插画生成后自动收起弹窗，露出已套用插画的卡片预览。
        if arguments.contains("-uiTestPromoIllustrationApply") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 7_000_000_000)
                showIllustration = false
            }
        }
        // 截图脚本用：AI 插画套用后自动保存到配方，验证持久化。
        if arguments.contains("-uiTestPromoIllustrationSave") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 9_000_000_000)
                saveToRecipe()
            }
        }
        if arguments.contains("-uiTestPromoExportSocial") {
            config.exportSize = .social
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 2_000_000_000)
                exportCard()
            }
        }
        if arguments.contains("-uiTestPromoExportPrint") {
            config.exportSize = .printA6
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 2_000_000_000)
                exportCard()
            }
        }
        if arguments.contains("-uiTestPromoShare") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 2_000_000_000)
                shareCard()
            }
        }
        if arguments.contains("-uiTestPromoSave") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 1_500_000_000)
                if !config.title.contains("（已保存验证）") {
                    config.title = config.title + "（已保存验证）"
                }
                saveToRecipe()
            }
        }
        #endif
    }
}

private enum PromoCardAlert: Identifiable {
    case exportSuccess
    case permissionDenied
    case cutoutFailed
    case failure(String)

    var id: String {
        switch self {
        case .exportSuccess: return "success"
        case .permissionDenied: return "permission"
        case .cutoutFailed: return "cutoutFailed"
        case .failure(let message): return "failure-\(message)"
        }
    }
}

// MARK: - 设计工具面板

private struct PromoCardDesignSheet: View {
    @EnvironmentObject private var store: AppStore
    @Environment(\.dismiss) private var dismiss

    @Binding var config: PromoCardConfig
    let recipe: Recipe
    let bean: Bean?
    let beanImage: UIImage?
    let illustrationImage: UIImage?
    @Binding var photoImage: UIImage?
    @Binding var priceText: String
    let importedImage: UIImage?
    let importedCutout: Bool
    let onRequestImport: () -> Void
    let onUseOriginalImport: () -> Void
    let onReCutoutImport: () -> Void
    let onRequestIllustration: () -> Void

    @State private var photoItem: PhotosPickerItem?
    @State private var photoLoading = false
    @State private var tagText: String
    @State private var customColor: Color

    init(
        config: Binding<PromoCardConfig>,
        recipe: Recipe,
        bean: Bean?,
        beanImage: UIImage?,
        illustrationImage: UIImage?,
        photoImage: Binding<UIImage?>,
        priceText: Binding<String>,
        importedImage: UIImage?,
        importedCutout: Bool,
        onRequestImport: @escaping () -> Void,
        onUseOriginalImport: @escaping () -> Void,
        onReCutoutImport: @escaping () -> Void,
        onRequestIllustration: @escaping () -> Void
    ) {
        _config = config
        self.recipe = recipe
        self.bean = bean
        self.beanImage = beanImage
        self.illustrationImage = illustrationImage
        _photoImage = photoImage
        _priceText = priceText
        self.importedImage = importedImage
        self.importedCutout = importedCutout
        self.onRequestImport = onRequestImport
        self.onUseOriginalImport = onUseOriginalImport
        self.onReCutoutImport = onReCutoutImport
        self.onRequestIllustration = onRequestIllustration
        _tagText = State(initialValue: config.wrappedValue.tags.joined(separator: "，"))
        _customColor = State(initialValue: Color(hex: config.wrappedValue.themeHex))
    }

    var body: some View {
        NavigationStack {
            ScrollViewReader { proxy in
                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 20) {
                        preview
                        ratioSection
                        templateSection
                        themeSection
                        coverSection
                        textSection
                            .id("design_text")
                        PrimaryActionButton(title: "完成") { dismiss() }
                            .padding(.top, 2)
                    }
                    .padding(20)
                    .padding(.bottom, 20)
                }
                .background(C.card)
                .onAppear { runDebugTextScrollIfNeeded(proxy: proxy) }
            }
            .navigationTitle("设计工具")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("完成") { dismiss() }.foregroundStyle(C.ink)
                }
            }
        }
        .onChange(of: tagText) { _, newValue in
            let tags = parsePromoCardTags(newValue)
            if !tags.isEmpty { config.tags = Array(tags.prefix(4)) }
        }
        .onChange(of: photoItem) { _, newValue in
            guard let newValue else { return }
            loadPhoto(newValue)
        }
    }

    private func runDebugTextScrollIfNeeded(proxy: ScrollViewProxy) {
        #if DEBUG
        guard ProcessInfo.processInfo.arguments.contains("-uiTestPromoDesignText") else { return }
        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 1_200_000_000)
            withAnimation(.easeOut(duration: 0.3)) {
                proxy.scrollTo("design_text", anchor: .top)
            }
        }
        #endif
    }

    private var preview: some View {
        PromoCardView(
            recipe: recipe,
            bean: bean,
            config: config,
            price: Int(priceText).flatMap { $0 > 0 ? $0 : nil },
            photoImage: photoImage,
            beanImage: beanImage,
            illustrationImage: illustrationImage,
            importedImage: importedImage
        )
        .aspectRatio(config.ratio.aspectRatio(for: config.effectiveExportSize), contentMode: .fit)
        .shadow(color: Color.black.opacity(0.12), radius: 12, x: 0, y: 6)
    }

    private var ratioSection: some View {
        VStack(alignment: .leading, spacing: 9) {
            sectionTitle("比例", overline: "RATIO")
            HStack(spacing: 8) {
                ForEach(PromoCardRatio.pickerCases) { ratio in
                    SmallChoiceButton(title: ratio.label, selected: config.ratio == ratio) {
                        config.ratio = ratio
                    }
                }
            }
        }
    }

    private var templateSection: some View {
        VStack(alignment: .leading, spacing: 9) {
            sectionTitle("模板", overline: "TEMPLATE")
            HStack(spacing: 8) {
                ForEach(PromoCardTemplate.allCases) { template in
                    Button {
                        config.template = template
                    } label: {
                        VStack(alignment: .leading, spacing: 5) {
                            HStack(spacing: 5) {
                                Text(template.label)
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundStyle(C.ink)
                                Spacer(minLength: 0)
                                if config.template == template {
                                    Image(systemName: "checkmark.circle.fill")
                                        .font(.system(size: 12))
                                        .foregroundStyle(C.forest)
                                }
                            }
                            Text(template.caption)
                                .font(.system(size: 9))
                                .foregroundStyle(C.muted)
                                .lineLimit(2)
                                .multilineTextAlignment(.leading)
                        }
                        .padding(11)
                        .frame(maxWidth: .infinity, minHeight: 62, alignment: .topLeading)
                        .background(config.template == template ? C.limeSoft : C.paperSoft)
                        .clipShape(RoundedRectangle(cornerRadius: 15, style: .continuous))
                        .overlay {
                            RoundedRectangle(cornerRadius: 15, style: .continuous)
                                .stroke(config.template == template ? C.lime.opacity(0.5) : C.lineSoft, lineWidth: 1)
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var themeSection: some View {
        VStack(alignment: .leading, spacing: 9) {
            sectionTitle("主题色", overline: "THEME COLOR")
            HStack(spacing: 9) {
                ForEach(PromoCardThemes.presets) { preset in
                    Button {
                        config.themeHex = preset.hex
                        customColor = Color(hex: preset.hex)
                    } label: {
                        ZStack {
                            Circle()
                                .fill(Color(hex: preset.hex))
                                .frame(width: 30, height: 30)
                            if config.themeHex.uppercased() == preset.hex.uppercased() {
                                Image(systemName: "checkmark")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundStyle(colorOnHex(preset.hex))
                            }
                        }
                        .overlay(Circle().stroke(C.line, lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("主题色 \(preset.name)")
                }
                ColorPicker("", selection: $customColor, supportsOpacity: false)
                    .labelsHidden()
                    .frame(width: 30, height: 30)
                    .onChange(of: customColor) { _, newValue in
                        config.themeHex = newValue.hexString
                    }
            }
        }
    }

    private var coverSection: some View {
        VStack(alignment: .leading, spacing: 9) {
            sectionTitle("封面", overline: "COVER")
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 4), spacing: 8) {
                ForEach(PromoCardCoverKind.pickerCases) { kind in
                    Button {
                        config.cover = kind
                    } label: {
                        VStack(spacing: 5) {
                            Image(systemName: kind.symbol)
                                .font(.system(size: 15, weight: .medium))
                                .foregroundStyle(config.cover == kind ? C.forest : C.muted)
                            Text(kind.label)
                                .font(.system(size: 10, weight: .medium))
                                .foregroundStyle(C.inkSoft)
                                .lineLimit(1)
                                .minimumScaleFactor(0.8)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(config.cover == kind ? C.limeSoft : C.paperSoft)
                        .clipShape(RoundedRectangle(cornerRadius: 15, style: .continuous))
                        .overlay {
                            RoundedRectangle(cornerRadius: 15, style: .continuous)
                                .stroke(config.cover == kind ? C.lime.opacity(0.5) : C.lineSoft, lineWidth: 1)
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
            if config.cover == .illustration, illustrationImage == nil {
                HStack(spacing: 8) {
                    Image(systemName: "sparkles")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(C.forest)
                    Text("还没有 AI 插画：先根据这杯配方生成一张手绘咖啡插画。")
                        .font(.system(size: 10.5))
                        .foregroundStyle(C.muted)
                        .lineLimit(2)
                    Spacer(minLength: 0)
                    Button {
                        onRequestIllustration()
                    } label: {
                        Text("去生成")
                            .font(.system(size: 11, weight: .medium))
                            .foregroundStyle(C.ink)
                            .padding(.horizontal, 11)
                            .frame(height: 30)
                            .background(C.lime)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 11)
                .padding(.vertical, 8)
                .background(C.paperSoft)
                .clipShape(RoundedRectangle(cornerRadius: 13, style: .continuous))
            }
            if config.cover == .photo {
                PhotosPicker(selection: $photoItem, matching: .images) {
                    HStack(spacing: 7) {
                        Image(systemName: "photo.on.rectangle.angled")
                            .font(.system(size: 13))
                        Text(photoLoading ? "正在读取图片…" : (photoImage == nil ? "从相册选择图片" : "更换相册图片"))
                            .font(.system(size: 12, weight: .medium))
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.system(size: 10, weight: .semibold))
                    }
                    .foregroundStyle(C.inkSoft)
                    .padding(.horizontal, 13)
                    .frame(height: 42)
                    .background(C.paperSoft)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .disabled(photoLoading)
            }
            if config.cover == .imported {
                importedCoverControls
            }
            if config.cover == .imported || config.cover == .illustration {
                Toggle(isOn: illustrationShadowBinding) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("主视觉柔和阴影")
                            .font(.system(size: 12.5, weight: .medium))
                            .foregroundStyle(C.inkSoft)
                        Text("参考手绘卡可直接叠在纸上，默认不加阴影")
                            .font(.system(size: 9.5))
                            .foregroundStyle(C.muted)
                    }
                }
                .tint(C.lime)
                .padding(.horizontal, 12)
                .padding(.vertical, 9)
                .background(C.paperSoft)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
        }
    }

    @ViewBuilder
    private var importedCoverControls: some View {
        if importedImage != nil {
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 6) {
                    Image(systemName: importedCutout ? "checkmark.seal.fill" : "photo")
                        .font(.system(size: 11))
                        .foregroundStyle(importedCutout ? C.forest : C.muted)
                    Text(importedCutout ? "已导入：透明背景 PNG" : "已导入：原图（未抠图）")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(C.inkSoft)
                    Spacer(minLength: 0)
                }
                HStack(spacing: 8) {
                    Button(action: onRequestImport) {
                        Text("重新导入")
                            .font(.system(size: 11.5, weight: .medium))
                            .foregroundStyle(C.ink)
                            .frame(maxWidth: .infinity)
                            .frame(height: 36)
                            .background(C.limeSoft)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                    Button(action: onReCutoutImport) {
                        Text(importedCutout ? "重新抠图" : "自动抠图")
                            .font(.system(size: 11.5, weight: .medium))
                            .foregroundStyle(C.inkSoft)
                            .frame(maxWidth: .infinity)
                            .frame(height: 36)
                            .background(C.paperSoft)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                    if importedCutout {
                        Button(action: onUseOriginalImport) {
                            Text("改用原图")
                                .font(.system(size: 11.5, weight: .medium))
                                .foregroundStyle(C.muted)
                                .frame(maxWidth: .infinity)
                                .frame(height: 36)
                                .background(C.paperSoft)
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(C.paperSoft)
            .clipShape(RoundedRectangle(cornerRadius: 15, style: .continuous))
        } else {
            HStack(spacing: 9) {
                Image(systemName: "photo.badge.arrow.down")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(C.forest)
                Text("从相册选一张咖啡/饮品照片，自动抠出主体、背景变透明，再叠到奶油纸卡片上。")
                    .font(.system(size: 10.5))
                    .foregroundStyle(C.muted)
                    .lineLimit(2)
                Spacer(minLength: 0)
                Button(action: onRequestImport) {
                    Text("去导入")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(C.onAccent)
                        .padding(.horizontal, 12)
                        .frame(height: 30)
                        .background(C.lime)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
            .padding(11)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(C.paperSoft)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
    }

    private var illustrationShadowBinding: Binding<Bool> {
        Binding(
            get: { config.illustrationShadow == true },
            set: { config.illustrationShadow = $0 }
        )
    }

    private var textSection: some View {
        VStack(alignment: .leading, spacing: 9) {
            sectionTitle("文字", overline: "TEXT")
            BrewTextField(label: "中文名（大标题）", placeholder: "这一杯的中文名", text: $config.title, limit: 40)
            BrewTextField(
                label: "英文名（衬线大字）",
                placeholder: promoDefaultEnglishName(recipe: recipe, bean: bean),
                text: englishNameBinding,
                limit: 44
            )
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("菜单价格 ¥").font(.system(size: 12, weight: .medium)).foregroundStyle(C.inkSoft)
                    Text("显示在卡片右上角与菜单列表，可留空").font(.system(size: 9.5)).foregroundStyle(C.muted)
                }
                Spacer()
                TextField("未设置", text: $priceText)
                    .keyboardType(.numberPad)
                    .multilineTextAlignment(.trailing)
                    .font(.mono(15))
                    .frame(width: 84, height: 38)
                    .padding(.horizontal, 10)
                    .background(C.paperSoft)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            BrewTextField(label: "副标题（可选，小字）", placeholder: "风味、口感、适合的场景", text: $config.subtitle, limit: 80)
            BrewTextField(
                label: "分类（左上角小字）",
                placeholder: promoDrinkCategory(for: recipe),
                text: categoryOverrideBinding,
                limit: 20
            )
            BrewTextField(
                label: "烘焙度 / 处理法（用逗号分隔）",
                placeholder: promoBeanTags(bean: bean).joined(separator: "，"),
                text: roastTagsOverrideBinding,
                limit: 40
            )
            BrewTextField(
                label: "配料 · 中文",
                placeholder: promoIngredientsChinese(for: recipe),
                text: ingredientsChineseBinding,
                limit: 60
            )
            BrewTextField(
                label: "配料 · 英文 / 豆名",
                placeholder: promoIngredientsEnglish(for: recipe),
                text: ingredientsEnglishBinding,
                limit: 80
            )
            BrewTextField(
                label: "风味参考 · 中文",
                placeholder: promoFlavorKeywords(recipe: recipe, bean: bean).joined(separator: "、"),
                text: flavorReferenceBinding,
                limit: 60
            )
            BrewTextField(
                label: "风味参考 · 英文",
                placeholder: promoFlavorEnglish(recipe: recipe, bean: bean),
                text: flavorEnglishBinding,
                limit: 80
            )
            BrewTextField(
                label: "底部关键词（逗号分隔）",
                placeholder: promoFlavorKeywords(recipe: recipe, bean: bean).prefix(3).joined(separator: "，"),
                text: keywordsOverrideBinding,
                limit: 40
            )
            BrewTextField(label: "标签（兼容旧数据，用逗号分隔）", placeholder: "荔枝，气泡，清爽", text: $tagText, limit: 60)
            HStack(spacing: 6) {
                ForEach(config.tags, id: \.self) { tag in
                    Text(tag)
                        .font(.system(size: 10.5, weight: .medium))
                        .foregroundStyle(C.inkSoft)
                        .padding(.horizontal, 9)
                        .padding(.vertical, 4)
                        .background(C.paperSoft)
                        .clipShape(Capsule())
                }
                Spacer(minLength: 0)
            }
        }
    }

    private var englishNameBinding: Binding<String> {
        Binding(
            get: { config.englishName ?? "" },
            set: { newValue in
                let clean = newValue.trimmingCharacters(in: .whitespacesAndNewlines)
                config.englishName = clean.isEmpty ? nil : clean
            }
        )
    }

    private var categoryOverrideBinding: Binding<String> {
        Binding(
            get: { config.categoryOverride ?? "" },
            set: { value in
                let clean = value.trimmingCharacters(in: .whitespacesAndNewlines)
                config.categoryOverride = clean.isEmpty ? nil : clean
            }
        )
    }

    private var roastTagsOverrideBinding: Binding<String> {
        Binding(
            get: {
                if let custom = config.roastTagsOverride, !custom.isEmpty {
                    return custom.joined(separator: "，")
                }
                return ""
            },
            set: { value in
                let tags = parsePromoCardTags(value)
                config.roastTagsOverride = tags.isEmpty ? nil : tags
            }
        )
    }

    private var ingredientsChineseBinding: Binding<String> {
        Binding(
            get: { config.ingredientsChineseOverride ?? "" },
            set: { value in
                let clean = value.trimmingCharacters(in: .whitespacesAndNewlines)
                config.ingredientsChineseOverride = clean.isEmpty ? nil : clean
            }
        )
    }

    private var ingredientsEnglishBinding: Binding<String> {
        Binding(
            get: { config.ingredientsEnglishOverride ?? "" },
            set: { value in
                let clean = value.trimmingCharacters(in: .whitespacesAndNewlines)
                config.ingredientsEnglishOverride = clean.isEmpty ? nil : clean
            }
        )
    }

    private var flavorReferenceBinding: Binding<String> {
        Binding(
            get: { config.flavorReferenceOverride ?? "" },
            set: { value in
                let clean = value.trimmingCharacters(in: .whitespacesAndNewlines)
                config.flavorReferenceOverride = clean.isEmpty ? nil : clean
            }
        )
    }

    private var flavorEnglishBinding: Binding<String> {
        Binding(
            get: { config.flavorEnglishOverride ?? "" },
            set: { value in
                let clean = value.trimmingCharacters(in: .whitespacesAndNewlines)
                config.flavorEnglishOverride = clean.isEmpty ? nil : clean
            }
        )
    }

    private var keywordsOverrideBinding: Binding<String> {
        Binding(
            get: {
                if let custom = config.keywordsOverride, !custom.isEmpty {
                    return custom.joined(separator: "，")
                }
                return ""
            },
            set: { value in
                let tags = parsePromoCardTags(value)
                config.keywordsOverride = tags.isEmpty ? nil : tags
            }
        )
    }

    private func sectionTitle(_ title: String, overline: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Overline(text: overline)
            Text(title)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(C.ink)
        }
    }

    private func loadPhoto(_ item: PhotosPickerItem) {
        photoLoading = true
        Task { @MainActor in
            defer { photoLoading = false }
            guard
                let data = try? await item.loadTransferable(type: Data.self),
                let image = UIImage(data: data),
                let path = store.saveCardCoverImage(image)
            else { return }
            photoImage = image
            config.coverImagePath = path
            config.cover = .photo
        }
    }

    private func colorOnHex(_ hex: String) -> Color {
        let color = Color(hex: hex)
        let ui = UIColor(color)
        var r: CGFloat = 0
        var g: CGFloat = 0
        var b: CGFloat = 0
        var a: CGFloat = 0
        ui.getRed(&r, green: &g, blue: &b, alpha: &a)
        let luminance = 0.299 * r + 0.587 * g + 0.114 * b
        return luminance > 0.6 ? Color(hex: "26231F") : Color.white
    }
}

// MARK: - AI 插画面板

/// 保存 AI 插画到相册的成功 / 失败提示。
private struct AlbumSaveAlert: Identifiable {
    let id = UUID()
    let title: String
    let message: String
}

/// 透明背景棋盘格，用于在 AI 插画面板里证明输出 PNG 带 alpha 通道。
private struct TransparentCheckerboard: View {
    var body: some View {
        Canvas { context, size in
            let square: CGFloat = 9
            var row = 0
            var y: CGFloat = 0
            while y < size.height {
                var column = 0
                var x: CGFloat = 0
                while x < size.width {
                    let color = (row + column) % 2 == 0 ? C.paperSoft : C.lineSoft
                    context.fill(
                        Path(CGRect(x: x, y: y, width: square, height: square)),
                        with: .color(color)
                    )
                    column += 1
                    x += square
                }
                row += 1
                y += square
            }
        }
    }
}

/// 根据当前配方（名称/豆子/风味/颜色/口感）生成手绘水彩咖啡插画，
/// 作为随杯卡底部 / 主视觉，可重新生成或回退本地杯图。
private struct PromoCardIllustrationSheet: View {
    @EnvironmentObject private var store: AppStore
    @Environment(\.dismiss) private var dismiss

    let recipe: Recipe
    let bean: Bean?
    @Binding var config: PromoCardConfig
    @Binding var illustrationImage: UIImage?
    let autoRun: Bool
    let onApplied: (String) -> Void
    let onOpenSettings: () -> Void

    @State private var prompt: String
    @State private var generating = false
    @State private var errorMessage: String?
    @State private var generatedImage: UIImage?
    @State private var rawImage: UIImage?
    @State private var didAutoRun = false
    @State private var applied = false
    @State private var processingCutout = false
    @State private var processingNote: String?
    @State private var removedWatermarkBlocks = 0
    @State private var watermarkDisabled = false
    @State private var savingCutout = false
    @State private var albumAlert: AlbumSaveAlert?
    @State private var photoImage: UIImage?
    @State private var photoPickerItem: PhotosPickerItem?
    @State private var showPhotoPicker = false
    @State private var showCamera = false
    @State private var analyzingPhoto = false
    @State private var photoPromptNote: String?
    @State private var photoPromptOrigin: String?
    @State private var didRunPhotoDebug = false
    @State private var showFullPrompt = false

    init(
        recipe: Recipe,
        bean: Bean?,
        config: Binding<PromoCardConfig>,
        illustrationImage: Binding<UIImage?>,
        autoRun: Bool,
        onApplied: @escaping (String) -> Void,
        onOpenSettings: @escaping () -> Void
    ) {
        self.recipe = recipe
        self.bean = bean
        _config = config
        _illustrationImage = illustrationImage
        self.autoRun = autoRun
        self.onApplied = onApplied
        self.onOpenSettings = onOpenSettings
        let savedPrompt = config.wrappedValue.illustrationPrompt
        _prompt = State(
            initialValue: (savedPrompt?.isEmpty == false)
                ? savedPrompt!
                : AIClient.illustrationPrompt(for: recipe, bean: bean)
        )
    }

    private var settings: AISettings { AISettings.current() }
    private var isConfigured: Bool { settings.isImageConfigured }

    var body: some View {
        NavigationStack {
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 16) {
                    header
                    if !isConfigured {
                        notConfiguredCard
                    }
                    promptSection
                    photoSection
                    PrimaryActionButton(
                        title: generating ? "正在画插画…" : (generatedImage == nil ? "根据这杯配方生成插画" : "重新生成一张"),
                        enabled: !generating && isConfigured
                    ) {
                        generate()
                    }
                    if generating || processingCutout {
                        loadingCard
                    }
                    if let errorMessage {
                        errorCard(errorMessage)
                    }
                    if let generatedImage {
                        successCard(generatedImage)
                    }
                    footer
                }
                .padding(20)
                .padding(.bottom, 20)
            }
            .background(C.card)
            .navigationTitle("AI 插画")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { dismiss() }.foregroundStyle(C.ink)
                }
            }
        }
        .onAppear {
            if generatedImage == nil, let illustrationImage {
                generatedImage = illustrationImage
                removedWatermarkBlocks = config.illustrationRemovedWatermarkBlocks ?? 0
                processingNote = config.illustrationProcessNote
            }
            runDebugAutoGenerateIfNeeded()
            runDebugPhotoFlowIfNeeded()
        }
        .photosPicker(isPresented: $showPhotoPicker, selection: $photoPickerItem, matching: .images)
        .onChange(of: photoPickerItem) { _, newValue in
            guard let newValue else { return }
            loadPhotoPickerItem(newValue)
        }
        .fullScreenCover(isPresented: $showCamera) {
            PhotoCameraPicker { image in
                photoImage = image
                Task { @MainActor in await analyzePhoto(image) }
            }
        }
        .alert(item: $albumAlert) { alert in
            Alert(
                title: Text(alert.title),
                message: Text(alert.message),
                dismissButton: .default(Text("好"))
            )
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 5) {
            Overline(text: "AI ILLUSTRATION")
            Text("给这杯画一张手绘插画")
                .font(.display(24))
                .foregroundStyle(C.ink)
            Text("会按配方名、豆子、风味、颜色和口感自动写 prompt，生成透明背景 PNG、暖色调、无文字的正方形插画；返回后优先保留真实 alpha，纯色底自动颜色抠图、棋盘格透明底自动转换，并清理 AI 水印，再叠到随杯卡的奶油纸上。")
                .font(.system(size: 11.5))
                .foregroundStyle(C.muted)
                .lineSpacing(3)
        }
    }

    private var notConfiguredCard: some View {
        VStack(alignment: .leading, spacing: 9) {
            HStack(spacing: 7) {
                Image(systemName: "key")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(C.brown)
                Text("还没有配置 AI 服务")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(C.ink)
            }
            Text("在「统计 → 设置 → 插画 API」里选择 Provider（推荐智谱 BigModel · GLM-Image，约 0.1 元/次）并填入 API Key；也可以换成硅基流动或任意 OpenAI 兼容网关。不配置也可以继续用配方杯图或相册图片做封面。")
                .font(.system(size: 11))
                .foregroundStyle(C.muted)
                .lineSpacing(3)
            Button {
                onOpenSettings()
            } label: {
                Text("去设置")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(C.ink)
                    .padding(.horizontal, 14)
                    .frame(height: 34)
                    .background(C.lime)
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(C.paperSoft)
        .clipShape(RoundedRectangle(cornerRadius: 17, style: .continuous))
    }

    private var promptSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            BrewTextEditor(
                label: "插画 prompt（完整 · 可滚动多行编辑）",
                placeholder: "手绘水彩咖啡插画，透明背景，暖色调，无文字……",
                text: $prompt,
                height: 200
            )
            HStack {
                Text("模型：\(settings.effectiveImageModel) · 尺寸 \(settings.effectiveImageSize)")
                    .font(.mono(8.5))
                    .foregroundStyle(C.mutedLight)
                Spacer()
                Text("\(prompt.count)/1000")
                    .font(.mono(9))
                    .foregroundStyle(prompt.count >= 1000 ? C.roseDeep : C.muted)
                Button {
                    prompt = AIClient.illustrationPrompt(for: recipe, bean: bean)
                    if prompt.count > 1000 { prompt = String(prompt.prefix(1000)) }
                } label: {
                    Text("按配方重写 prompt")
                        .font(.system(size: 10.5, weight: .medium))
                        .foregroundStyle(C.forest)
                }
                .buttonStyle(.plain)
            }
            HStack(spacing: 5) {
                Image(systemName: "checkmark.seal")
                    .font(.system(size: 9))
                    .foregroundStyle(C.forest)
                Text("已要求透明背景 PNG · isolated illustration · no paper texture · no shadow · no text/watermark")
                    .font(.system(size: 9))
                    .foregroundStyle(C.mutedLight)
                    .lineLimit(2)
            }
            if prompt.count > 240 {
                Button {
                    withAnimation(.easeInOut(duration: 0.18)) {
                        showFullPrompt.toggle()
                    }
                } label: {
                    HStack(spacing: 5) {
                        Image(systemName: showFullPrompt ? "chevron.up" : "chevron.down")
                            .font(.system(size: 9))
                        Text(showFullPrompt ? "收起完整提示词" : "查看完整提示词（含透明底后缀）")
                            .font(.system(size: 10, weight: .medium))
                    }
                    .foregroundStyle(C.forest)
                }
                .buttonStyle(.plain)
            }
            if showFullPrompt {
                Text(prompt)
                    .font(.system(size: 11))
                    .foregroundStyle(C.inkSoft)
                    .textSelection(.enabled)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(11)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(C.paperSoft)
                    .clipShape(RoundedRectangle(cornerRadius: 13, style: .continuous))
            }
        }
        .onChange(of: prompt) { _, newValue in
            // 与生成长度规则一致：编辑器内可见截断到 1000 字符，生成时绝不隐式截断。
            if newValue.count > 1000 {
                prompt = String(newValue.prefix(1000))
            }
        }
    }

    private func completePromptFromPhoto() {
        let source = prompt.trimmingCharacters(in: .whitespacesAndNewlines)
        let completed = AIClient.buildIllustrationPrompt(description: source, recipe: recipe, bean: bean)
        prompt = completed.count > 1000 ? String(completed.prefix(1000)) : completed
        showFullPrompt = true
        photoPromptNote = "已按配方风格补全提示词（含透明底 PNG 后缀），可继续编辑或直接生成。"
        if let photoImage {
            persistPhotoPrompt(image: photoImage, prompt: prompt)
        }
        haptic()
    }

    private var loadingCard: some View {
        HStack(spacing: 11) {
            ProgressView()
            VStack(alignment: .leading, spacing: 3) {
                Text(processingCutout ? "正在抠透明背景、清理 AI 水印…" : "正在根据这杯配方画插画…")
                    .font(.system(size: 12.5, weight: .semibold))
                    .foregroundStyle(C.ink)
                Text(processingCutout
                     ? "纯色背景颜色键控 · 边缘软过渡 · 本地清理水印 · 输出透明 PNG"
                     : "透明背景 PNG · 手绘水彩 · 暖色调 · 无文字 · 正方形")
                    .font(.system(size: 10))
                    .foregroundStyle(C.muted)
            }
            Spacer(minLength: 0)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(C.limeSoft.opacity(0.7))
        .clipShape(RoundedRectangle(cornerRadius: 17, style: .continuous))
    }

    private func errorCard(_ message: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 6) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 12))
                    .foregroundStyle(C.roseDeep)
                Text("插画生成失败")
                    .font(.system(size: 12.5, weight: .semibold))
                    .foregroundStyle(C.ink)
            }
            Text(message)
                .font(.system(size: 11))
                .foregroundStyle(C.muted)
                .lineSpacing(3)
            Button {
                generate()
            } label: {
                Text("重试")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(C.ink)
                    .padding(.horizontal, 14)
                    .frame(height: 34)
                    .background(C.limeSoft)
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)
            .disabled(generating)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(C.rose.opacity(0.6))
        .clipShape(RoundedRectangle(cornerRadius: 17, style: .continuous))
    }

    private var isProcessingWarning: Bool {
        guard let processingNote else { return false }
        return processingNote.contains("背景颜色不纯")
            || processingNote.contains("未抠图")
            || processingNote.contains("已保留原图")
            || processingNote.contains("没有识别到")
            || processingNote.contains("异常")
    }

    private func successCard(_ image: UIImage) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 6) {
                Image(systemName: isProcessingWarning ? "exclamationmark.triangle.fill" : "sparkles")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(isProcessingWarning ? C.roseDeep : C.forest)
                Text(applied
                     ? (removedWatermarkBlocks > 0 ? "插画已套用：透明背景 · 已清理 \(removedWatermarkBlocks) 处水印/logo" : "插画已套用：透明背景")
                     : "插画已生成")
                    .font(.system(size: 12.5, weight: .semibold))
                    .foregroundStyle(C.ink)
                Spacer()
                Text(settings.effectiveImageModel)
                    .font(.mono(8.5))
                    .foregroundStyle(C.mutedLight)
                    .lineLimit(1)
            }
            ZStack {
                TransparentCheckerboard()
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
                    .padding(6)
            }
            .frame(maxWidth: 280, maxHeight: 280)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(C.line, lineWidth: 1)
            }
            .frame(maxWidth: .infinity, alignment: .center)

            HStack(spacing: 6) {
                Image(systemName: "checkmark.seal")
                    .font(.system(size: 10))
                    .foregroundStyle(C.forest)
                Text("透明 PNG \(Int(image.size.width))×\(Int(image.size.height)) · alpha 已保留，可直接叠在奶油纸上")
                    .font(.system(size: 9.5))
                    .foregroundStyle(C.muted)
                    .lineLimit(2)
            }
            if let processingNote, !applied || processingNote.contains("背景颜色不纯") || processingNote.contains("原图") {
                Text(processingNote)
                    .font(.system(size: 10.5, weight: .medium))
                    .foregroundStyle(C.roseDeep)
                    .lineSpacing(2)
            } else if let processingNote {
                Text(processingNote)
                    .font(.system(size: 10))
                    .foregroundStyle(C.muted)
            }

            HStack(spacing: 8) {
                Button {
                    generate()
                } label: {
                    Text("重新生成")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(C.ink)
                        .frame(maxWidth: .infinity)
                        .frame(height: 40)
                        .background(C.limeSoft)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                .disabled(generating || processingCutout)
                Button {
                    saveIllustrationToAlbum()
                } label: {
                    Text(savingCutout ? "正在保存…" : "保存透明图到相册")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(C.ink)
                        .frame(maxWidth: .infinity)
                        .frame(height: 40)
                        .background(C.paperSoft)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                .disabled(savingCutout)
            }
            HStack(spacing: 8) {
                Button {
                    revertToLocalCover()
                } label: {
                    Text("改用本地封面")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(C.inkSoft)
                        .frame(maxWidth: .infinity)
                        .frame(height: 40)
                        .background(C.paperSoft)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                if config.illustrationRawImagePath != nil {
                    Button {
                        reprocessRawIllustration()
                    } label: {
                        Text("重新处理透明背景")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(C.inkSoft)
                            .frame(maxWidth: .infinity)
                            .frame(height: 40)
                            .background(C.paperSoft)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                    .disabled(processingCutout || generating)
                }
            }
            Button {
                dismiss()
            } label: {
                Text("完成，回到卡片")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(C.onAccent)
                    .frame(maxWidth: .infinity)
                    .frame(height: 42)
                    .background(C.lime)
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(C.limeSoft.opacity(0.7))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(C.lime.opacity(0.4), lineWidth: 1)
        }
    }

    private var footer: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("接口：OpenAI 兼容 POST /images/generations（url 下载 / b64_json 均支持）；建议尺寸：竖版 1024×1440、横版 1440×1024")
                .font(.mono(8.5))
                .foregroundStyle(C.mutedLight)
            if isConfigured {
                Text("Base URL：\(settings.effectiveImageBaseURL)")
                    .font(.mono(8))
                    .foregroundStyle(C.mutedLight)
                    .lineLimit(1)
                    .truncationMode(.middle)
            }
        }
    }

    // MARK: - 从实物图生成插画 prompt

    private var photoSection: some View {
        VStack(alignment: .leading, spacing: 9) {
            HStack {
                Overline(text: "FROM PHOTO")
                Spacer()
                Text("实物照片 → 插画 prompt").font(.system(size: 9)).foregroundStyle(C.mutedLight)
            }
            HStack(spacing: 10) {
                if let photoImage {
                    Image(uiImage: photoImage)
                        .resizable()
                        .scaledToFill()
                        .frame(width: 58, height: 58)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                } else {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(C.paperSoft)
                        .frame(width: 58, height: 58)
                        .overlay {
                            Image(systemName: "camera.macro")
                                .font(.system(size: 17))
                                .foregroundStyle(C.muted)
                        }
                }
                VStack(alignment: .leading, spacing: 3) {
                    Text(photoImage == nil ? "从实物图生成" : "已选实物图")
                        .font(.system(size: 12.5, weight: .semibold))
                        .foregroundStyle(C.ink)
                    Text("选/拍一张成品咖啡、杯子或原料照片，先由视觉模型理解画面并生成插画 prompt，再交给图像模型出图。")
                        .font(.system(size: 9.5))
                        .foregroundStyle(C.muted)
                        .lineLimit(2)
                }
                Spacer(minLength: 0)
            }
            HStack(spacing: 8) {
                Button {
                    showPhotoPicker = true
                } label: {
                    Text("选一张实物照")
                        .font(.system(size: 11.5, weight: .medium))
                        .foregroundStyle(C.ink)
                        .frame(maxWidth: .infinity)
                        .frame(height: 36)
                        .background(C.limeSoft)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                if UIImagePickerController.isSourceTypeAvailable(.camera) {
                    Button {
                        showCamera = true
                    } label: {
                        Text("拍照")
                            .font(.system(size: 11.5, weight: .medium))
                            .foregroundStyle(C.inkSoft)
                            .frame(maxWidth: .infinity)
                            .frame(height: 36)
                            .background(C.paperSoft)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
            if analyzingPhoto {
                HStack(spacing: 8) {
                    ProgressView()
                    Text("正在理解实物照片，生成插画 prompt…")
                        .font(.system(size: 10.5))
                        .foregroundStyle(C.muted)
                }
            }
            if let photoPromptNote {
                Text(photoPromptNote)
                    .font(.system(size: 10))
                    .foregroundStyle(photoPromptOrigin == "vision" ? C.muted : C.roseDeep)
                    .lineSpacing(2)
            }
            if photoImage != nil, !analyzingPhoto, !prompt.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Button {
                    completePromptFromPhoto()
                } label: {
                    HStack(spacing: 5) {
                        Image(systemName: "wand.and.stars")
                            .font(.system(size: 10))
                        Text("补全提示词（配方风格 + 透明底后缀）")
                            .font(.system(size: 11.5, weight: .medium))
                    }
                    .foregroundStyle(C.forest)
                    .frame(maxWidth: .infinity)
                    .frame(height: 36)
                    .background(C.limeSoft)
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                Button {
                    generate()
                } label: {
                    Text("用这些提示词生成插画")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(C.onAccent)
                        .frame(maxWidth: .infinity)
                        .frame(height: 40)
                        .background(C.lime)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                .disabled(generating || processingCutout)
            }
        }
        .padding(13)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(C.paperSoft)
        .clipShape(RoundedRectangle(cornerRadius: 17, style: .continuous))
    }

    // MARK: - 实物图 → prompt

    private func loadPhotoPickerItem(_ item: PhotosPickerItem) {
        Task { @MainActor in
            guard
                let data = try? await item.loadTransferable(type: Data.self),
                let image = UIImage(data: data)
            else {
                errorMessage = "这张实物照片读不出来，换一张试试。"
                photoPickerItem = nil
                return
            }
            photoImage = image
            await analyzePhoto(image)
            photoPickerItem = nil
        }
    }

    private func analyzePhoto(_ image: UIImage) async {
        guard !analyzingPhoto else { return }
        analyzingPhoto = true
        photoPromptNote = nil
        errorMessage = nil
        let settings = AISettings.current()
        let hasVision = settings.isVisionConfigured
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("-uiTestPhoto2IlluSlow") {
            try? await Task.sleep(nanoseconds: 2_500_000_000)
        }
        #endif
        if hasVision {
            do {
                let generated = try await AIClient.generatePromptFromPhoto(
                    image,
                    recipe: recipe,
                    bean: bean,
                    settings: settings
                )
                prompt = generated
                photoPromptOrigin = "vision"
                photoPromptNote = "已用图片理解模型（\(settings.effectiveVisionModel)）生成 prompt，已填入上方、可继续修改；确认后点「用这些提示词生成插画」。"
                persistPhotoPrompt(image: image, prompt: generated)
            } catch {
                let fallback = AIClient.localPromptFromPhoto(image, recipe: recipe, bean: bean)
                prompt = fallback
                photoPromptOrigin = "local"
                photoPromptNote = "图片理解调用失败（\(error.localizedDescription)），已按照片主色 + 配方本地生成 prompt；可修改后生成。"
                persistPhotoPrompt(image: image, prompt: fallback)
            }
        } else {
            let fallback = AIClient.localPromptFromPhoto(image, recipe: recipe, bean: bean)
            prompt = fallback
            photoPromptOrigin = "local"
            photoPromptNote = "未配置图片理解 API，已按照片主色 + 配方本地生成 prompt；可修改后生成。"
            persistPhotoPrompt(image: image, prompt: fallback)
        }
        analyzingPhoto = false
        #if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        if arguments.contains("-uiTestPhoto2IlluComplete") {
            completePromptFromPhoto()
        }
        if arguments.contains("-uiTestPhoto2IlluFullPrompt") {
            showFullPrompt = true
        }
        #endif
    }

    private func persistPhotoPrompt(image: UIImage, prompt: String) {
        if let path = store.saveCardIllustration(image) {
            config.illustrationPhotoImagePath = path
        }
        config.illustrationPhotoPrompt = prompt
        config.illustrationPromptSource = photoPromptOrigin
    }

    private func runDebugPhotoFlowIfNeeded() {
        #if DEBUG
        guard !didRunPhotoDebug else { return }
        let arguments = ProcessInfo.processInfo.arguments
        guard let index = arguments.firstIndex(of: "-uiTestPhoto2Illu"),
              arguments.indices.contains(index + 1) else { return }
        let path = arguments[index + 1]
        didRunPhotoDebug = true
        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 1_600_000_000)
            guard let image = store.uiImage(path: path) else {
                errorMessage = "DEBUG 实物图不存在：\(path)"
                return
            }
            photoImage = image
            await analyzePhoto(image)
            if arguments.contains("-uiTestPhoto2IlluGenerate") {
                try? await Task.sleep(nanoseconds: 800_000_000)
                generate()
            }
        }
        #endif
    }

    // MARK: - 生成 / 套用 / 回退

    private func generate() {
        guard !generating, !processingCutout else { return }
        generating = true
        errorMessage = nil
        processingNote = nil
        removedWatermarkBlocks = 0

        #if DEBUG
        // 截图脚本用：人为放慢一点，方便截到「生成中」状态。
        let slow = ProcessInfo.processInfo.arguments.contains("-uiTestPromoIllustrationSlow")
        #else
        let slow = false
        #endif
        // 生成必须严格使用编辑框里的完整 prompt：先做可见归一化（trim/≤1000），
        // 再把归一化后的文本写回编辑框，确保「显示内容 == 实际发送内容」。
        var normalizedPrompt = prompt.trimmingCharacters(in: .whitespacesAndNewlines)
        if normalizedPrompt.isEmpty {
            normalizedPrompt = AIClient.illustrationPrompt(for: recipe, bean: bean)
        }
        if normalizedPrompt.count > 1000 {
            normalizedPrompt = String(normalizedPrompt.prefix(1000))
        }
        prompt = normalizedPrompt
        let currentPrompt = normalizedPrompt
        Task { @MainActor in
            if slow { try? await Task.sleep(nanoseconds: 2_800_000_000) }
            do {
                let preferredSize = config.ratio.isPortrait ? "1024x1440" : "1440x1024"
                let result = try await AIClient.generateIllustration(
                    recipe: recipe,
                    bean: bean,
                    prompt: currentPrompt,
                    preferredSize: preferredSize,
                    settings: settings
                )
                // 保持编辑框与发送内容一致，不用服务端返回/截断版本覆盖。
                watermarkDisabled = result.watermarkDisabled
                rawImage = result.image
                generating = false
                processingCutout = true
                // 纯色背景颜色抠图 + 本地去水印（不依赖 Vision）。
                let processed = await AIIllustrationProcessor.process(result.image)
                processingCutout = false
                generatedImage = processed.image
                removedWatermarkBlocks = processed.removedWatermarkBlocks
                if let reason = processed.fallbackReason {
                    processingNote = reason
                } else {
                    processingNote = processed.removedWatermarkBlocks > 0
                        ? "\(processed.method.label)：透明背景已生成，清理了 \(processed.removedWatermarkBlocks) 处 AI 水印/logo。"
                        : "\(processed.method.label)：透明背景已生成，未检测到残留水印。"
                }
                applyIllustration(
                    processed.image,
                    raw: result.image,
                    prompt: currentPrompt,
                    processed: processed
                )
                if let photoPromptOrigin {
                    config.illustrationPromptSource = photoPromptOrigin
                    config.illustrationPhotoPrompt = currentPrompt
                } else if config.illustrationPhotoPrompt == nil {
                    config.illustrationPromptSource = "recipe"
                }
                #if DEBUG
                if ProcessInfo.processInfo.arguments.contains("-uiTestAISaveCutout") {
                    try? await Task.sleep(nanoseconds: 1_200_000_000)
                    saveIllustrationToAlbum()
                }
                if ProcessInfo.processInfo.arguments.contains("-uiTestAISheetClose") {
                    try? await Task.sleep(nanoseconds: 1_500_000_000)
                    dismiss()
                }
                #endif
            } catch {
                errorMessage = error.localizedDescription
                generating = false
                processingCutout = false
            }
        }
    }

    private func applyIllustration(
        _ image: UIImage,
        raw: UIImage?,
        prompt: String,
        processed: AIIllustrationProcessingResult
    ) {
        illustrationImage = image
        config.cover = .illustration
        config.illustrationSource = .ai
        config.illustrationPrompt = prompt
        if let path = store.saveCardIllustration(image) {
            config.illustrationImagePath = path
        }
        if let raw, let rawPath = store.saveCardIllustration(raw) {
            config.illustrationRawImagePath = rawPath
        }
        if let reason = processed.fallbackReason {
            config.illustrationProcessNote = reason
        } else {
            config.illustrationProcessNote = processed.removedWatermarkBlocks > 0
                ? "\(processed.method.label) · 已清理 \(processed.removedWatermarkBlocks) 处水印/logo"
                : "\(processed.method.label) · 未检测到水印"
        }
        config.illustrationRemovedWatermarkBlocks = processed.removedWatermarkBlocks
        applied = true
        haptic()
        onApplied(processed.didCutout ? "手绘咖啡插画（透明背景）" : "手绘咖啡插画（原图）")
    }

    /// 对保留的 AI 原图重新做颜色抠图 / 去水印（raw 路径重试）。
    private func reprocessRawIllustration() {
        guard let rawPath = config.illustrationRawImagePath,
              let raw = store.uiImage(path: rawPath) else {
            errorMessage = "找不到保存的 AI 原图，请重新生成一张。"
            return
        }
        guard !processingCutout, !generating else { return }
        errorMessage = nil
        processingCutout = true
        Task { @MainActor in
            let processed = await AIIllustrationProcessor.process(raw)
            processingCutout = false
            generatedImage = processed.image
            removedWatermarkBlocks = processed.removedWatermarkBlocks
            if let reason = processed.fallbackReason {
                processingNote = reason
            } else {
                processingNote = processed.removedWatermarkBlocks > 0
                    ? "已重新处理原图（\(processed.method.label)）：清理 \(processed.removedWatermarkBlocks) 处水印/logo。"
                    : "已重新处理原图（\(processed.method.label)）：透明背景，未检测到水印。"
            }
            applyIllustration(
                processed.image,
                raw: raw,
                prompt: config.illustrationPrompt ?? prompt,
                processed: processed
            )
        }
    }

    /// 把处理后的透明 PNG 保存到系统相册。
    private func saveIllustrationToAlbum() {
        guard let image = generatedImage, !savingCutout else { return }
        guard let data = image.pngData() else {
            albumAlert = AlbumSaveAlert(title: "保存失败", message: "无法生成 PNG 图片数据。")
            return
        }
        savingCutout = true
        Task { @MainActor in
            do {
                try await PromoCardExporter.saveToPhotoLibrary(data)
                let size = "\(Int(image.size.width))×\(Int(image.size.height))"
                albumAlert = AlbumSaveAlert(
                    title: "已保存到相册",
                    message: "透明插画已保存为 PNG（\(size)，带 alpha 通道），可直接用于其他 App 或打印。"
                )
            } catch {
                albumAlert = AlbumSaveAlert(
                    title: "保存失败",
                    message: "\(error.localizedDescription)\n可以在系统设置里允许「忆杯手记」添加照片后重试。"
                )
            }
            savingCutout = false
        }
    }

    private func revertToLocalCover() {
        illustrationImage = nil
        config.illustrationImagePath = nil
        config.illustrationRawImagePath = nil
        config.illustrationProcessNote = nil
        config.illustrationRemovedWatermarkBlocks = nil
        config.illustrationPhotoImagePath = nil
        config.illustrationPhotoPrompt = nil
        config.illustrationPromptSource = nil
        config.illustrationSource = nil
        config.illustrationPrompt = nil
        config.cover = .cup
        haptic()
        dismiss()
    }

    private func runDebugAutoGenerateIfNeeded() {
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("-uiTestPhoto2Illu") { return }
        guard autoRun || ProcessInfo.processInfo.arguments.contains("-uiTestPromoIllustration") else { return }
        guard !didAutoRun else { return }
        didAutoRun = true
        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 1_200_000_000)
            generate()
        }
        #endif
    }
}

// MARK: - AI 生成面板

private struct PromoCardAISheet: View {
    @Environment(\.dismiss) private var dismiss

    let recipe: Recipe
    let bean: Bean?
    @Binding var config: PromoCardConfig
    let autoRun: Bool
    let onApplied: (String) -> Void
    let onOpenSettings: () -> Void

    @State private var description: String = ""
    @State private var generating = false
    @State private var result: AIGeneratedCopy?
    @State private var errorMessage: String?
    @State private var didAutoRun = false

    private var settings: AISettings { AISettings.current() }
    private var isConfigured: Bool { settings.isConfigured }

    var body: some View {
        NavigationStack {
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 16) {
                    header
                    if !isConfigured {
                        notConfiguredCard
                    }
                    descriptionSection
                    if generating {
                        HStack(spacing: 8) {
                            ProgressView()
                            Text("正在请求 \(settings.trimmedModel)…")
                                .font(.system(size: 12))
                                .foregroundStyle(C.muted)
                        }
                        .padding(.vertical, 4)
                    }
                    if let errorMessage {
                        errorCard(errorMessage)
                    }
                    if let result {
                        resultCard(result)
                    }
                    footer
                }
                .padding(20)
                .padding(.bottom, 20)
            }
            .background(C.card)
            .navigationTitle("AI 灵感")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { dismiss() }.foregroundStyle(C.ink)
                }
            }
        }
        .onAppear {
            if description.isEmpty {
                description = "写一条适合社交封面的宣传文案，突出这杯的风味和喝它的场景。"
            }
            runDebugAutoGenerateIfNeeded()
        }
        .onChange(of: autoRun) { _, newValue in
            if newValue { runDebugAutoGenerateIfNeeded() }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 5) {
            Overline(text: "AI COPYWRITER")
            Text("让 AI 写一张卡片")
                .font(.display(24))
                .foregroundStyle(C.ink)
            Text("描述你想要的感觉，AI 会返回标题、副标题、标签、主题色和模板建议；所有内容只在本地渲染。")
                .font(.system(size: 11.5))
                .foregroundStyle(C.muted)
                .lineSpacing(3)
        }
    }

    private var notConfiguredCard: some View {
        VStack(alignment: .leading, spacing: 9) {
            HStack(spacing: 7) {
                Image(systemName: "key")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(C.brown)
                Text("还没有配置 AI API")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(C.ink)
            }
            Text("在「统计 → 设置 → AI API」里填写 OpenAI 兼容的 Base URL、API Key 和模型名即可使用；不配置也可以手动设计卡片。")
                .font(.system(size: 11))
                .foregroundStyle(C.muted)
                .lineSpacing(3)
            Button {
                onOpenSettings()
            } label: {
                Text("去设置")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(C.ink)
                    .padding(.horizontal, 14)
                    .frame(height: 34)
                    .background(C.lime)
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(C.paperSoft)
        .clipShape(RoundedRectangle(cornerRadius: 17, style: .continuous))
    }

    private var descriptionSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            BrewTextEditor(label: "想表达的感觉", placeholder: "例如：突出荔枝香气和气泡感，适合周末下午", text: $description, height: 88)
            PrimaryActionButton(
                title: generating ? "正在生成…" : "AI 生成文案",
                enabled: !generating && !description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            ) {
                generate()
            }
        }
    }

    private func errorCard(_ message: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 6) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 12))
                    .foregroundStyle(C.roseDeep)
                Text("生成失败")
                    .font(.system(size: 12.5, weight: .semibold))
                    .foregroundStyle(C.ink)
            }
            Text(message)
                .font(.system(size: 11))
                .foregroundStyle(C.muted)
                .lineSpacing(3)
            Button {
                generate()
            } label: {
                Text("重试")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(C.ink)
                    .padding(.horizontal, 14)
                    .frame(height: 34)
                    .background(C.limeSoft)
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)
            .disabled(generating)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(C.rose.opacity(0.6))
        .clipShape(RoundedRectangle(cornerRadius: 17, style: .continuous))
    }

    private func resultCard(_ result: AIGeneratedCopy) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 6) {
                Image(systemName: "sparkles")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(C.forest)
                Text("AI 已生成并套用到卡片")
                    .font(.system(size: 12.5, weight: .semibold))
                    .foregroundStyle(C.ink)
                Spacer()
                Text(settings.trimmedModel)
                    .font(.mono(8.5))
                    .foregroundStyle(C.mutedLight)
                    .lineLimit(1)
            }
            VStack(alignment: .leading, spacing: 5) {
                Text(result.title)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(C.ink)
                Text(result.subtitle)
                    .font(.system(size: 11.5))
                    .foregroundStyle(C.inkSoft)
                    .lineSpacing(2)
                HStack(spacing: 6) {
                    ForEach(result.tags.prefix(4), id: \.self) { tag in
                        Text(tag)
                            .font(.system(size: 10, weight: .medium))
                            .foregroundStyle(C.forest)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3.5)
                            .background(C.limeSoft)
                            .clipShape(Capsule())
                    }
                    Spacer(minLength: 0)
                }
                Text("主题色 \(config.themeHex.uppercased()) · 模板 \(config.template.label)")
                    .font(.mono(8.5))
                    .foregroundStyle(C.muted)
            }
            Button {
                dismiss()
            } label: {
                Text("带着这张卡片继续设计")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(C.ink)
                    .padding(.horizontal, 14)
                    .frame(height: 36)
                    .background(C.lime)
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(C.limeSoft.opacity(0.7))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(C.lime.opacity(0.4), lineWidth: 1)
        }
    }

    private var footer: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("接口：OpenAI 兼容 chat/completions")
                .font(.mono(8.5))
                .foregroundStyle(C.mutedLight)
            if isConfigured {
                Text("Base URL：\(settings.effectiveImageBaseURL)")
                    .font(.mono(8))
                    .foregroundStyle(C.mutedLight)
                    .lineLimit(1)
                    .truncationMode(.middle)
            }
        }
    }

    // MARK: - 生成

    private func generate() {
        guard !generating else { return }
        generating = true
        errorMessage = nil
        result = nil
        let currentDescription = description

        // DEBUG：没有配置真实 API 时用本地示例结果，方便 CI 截图「AI 生成结果」页面。
        #if DEBUG
        if !isConfigured, ProcessInfo.processInfo.arguments.contains("-uiTestAIDemo") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 1_200_000_000)
                let sample = AIGeneratedCopy(
                    title: "荔枝气泡冷萃 · 午后第一口",
                    subtitle: "荔枝糖的甜香被气泡轻轻托起，冰块把尾韵收得干净，适合慢慢喝的下午。",
                    tags: ["荔枝", "气泡", "清爽", "微甜"],
                    theme: "A3CD39",
                    template: "magazine"
                )
                apply(sample)
                generating = false
            }
            return
        }
        #endif

        Task { @MainActor in
            do {
                let copy = try await AIClient.generateCardCopy(
                    for: recipe,
                    bean: bean,
                    description: currentDescription,
                    settings: settings
                )
                apply(copy)
            } catch {
                errorMessage = error.localizedDescription
            }
            generating = false
        }
    }

    private func apply(_ copy: AIGeneratedCopy) {
        config = copy.applying(to: config)
        result = copy
        onApplied("「\(copy.title)」")
        haptic()
        #if DEBUG
        // 截图脚本用：生成后自动关闭弹窗，露出已套用 AI 文案的卡片预览。
        if ProcessInfo.processInfo.arguments.contains("-uiTestPromoAIApply") {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 1_200_000_000)
                dismiss()
            }
        }
        #endif
    }

    private func runDebugAutoGenerateIfNeeded() {
        #if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        guard autoRun || arguments.contains("-uiTestPromoAI") else { return }
        guard !didAutoRun else { return }
        didAutoRun = true
        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 1_200_000_000)
            generate()
        }
        #endif
    }
}


// MARK: - 拍照（实物图 → 插画 prompt）

private struct PhotoCameraPicker: UIViewControllerRepresentable {
    let onPicked: (UIImage) -> Void
    @Environment(\.dismiss) private var dismiss

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.cameraCaptureMode = .photo
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(onPicked: onPicked, dismiss: { dismiss() })
    }

    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let onPicked: (UIImage) -> Void
        let dismiss: () -> Void

        init(onPicked: @escaping (UIImage) -> Void, dismiss: @escaping () -> Void) {
            self.onPicked = onPicked
            self.dismiss = dismiss
        }

        func imagePickerController(
            _ picker: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
        ) {
            if let image = info[.originalImage] as? UIImage {
                onPicked(image)
            }
            dismiss()
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            dismiss()
        }
    }
}
