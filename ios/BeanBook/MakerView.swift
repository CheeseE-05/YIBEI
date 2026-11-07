import Foundation
import SwiftUI

struct MakerView: View {
    @EnvironmentObject private var store: AppStore
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.dismiss) private var dismiss

    let onOpenRecipe: (String) -> Void
    let onEditSaved: (String) -> Void
    let onEditCancelled: () -> Void

    @State private var showBeanPicker = false
    @State private var showCustom = false
    @State private var showSave = false
    @State private var showClear = false
    @State private var editingLayer: LayerIndex?
    @State private var pourTimer: Timer?
    @State private var pouringID: String?

    private var selectedBean: Bean? { store.bean(store.draft.beanId) }
    private var isWide: Bool { horizontalSizeClass == .regular }
    private var isEditingRecipe: Bool { store.isEditingRecipe }

    var body: some View {
        VStack(spacing: 0) {
            topBar
            if isEditingRecipe { editingBanner }
            beanRow
            HStack(spacing: 12) {
                cupPanel
                layerPanel
                    .frame(width: isWide ? 200 : 128)
            }
            .padding(.horizontal, 24)
            .padding(.top, 4)
            .frame(maxHeight: .infinity)
            controls
            ingredientTray
        }
        .background(PaperBackground())
        .onDisappear { stopPouring() }
        .sheet(isPresented: $showBeanPicker) {
            BeanPickerSheet(selected: store.draft.beanId) { id in
                store.selectBean(id)
                showBeanPicker = false
            }
            .environmentObject(store)
            .presentationDetents([.medium, .large])
        }
        .sheet(isPresented: $showCustom) {
            CustomIngredientSheet()
                .environmentObject(store)
                .presentationDetents([.large])
        }
        .sheet(isPresented: $showSave) {
            SaveCupSheet(
                editingRecipe: store.editingRecipe,
                suggestedName: store.suggestedRecipeName(),
                initialBeanId: store.draft.beanId,
                totalGrams: store.draft.totalGrams,
                layerCount: store.draft.layers.count
            ) { id, wasEditing in
                showSave = false
                if wasEditing {
                    store.editingRecipeID = nil
                    onEditSaved(id)
                } else {
                    onOpenRecipe(id)
                }
            }
            .environmentObject(store)
            .presentationDetents([.large])
        }
        .sheet(item: $editingLayer) { item in
            let index = item.id
            if store.draft.layers.indices.contains(index) {
                let layer = store.draft.layers[index]
                LayerEditSheet(
                    layer: layer,
                    maxGrams: layer.grams + store.draft.remaining
                ) { grams in
                    store.editLayer(index: index, grams: grams)
                    editingLayer = nil
                } onRemove: {
                    store.removeLayer(index: index)
                    editingLayer = nil
                }
                .presentationDetents([.medium])
            }
        }
        .alert("倒掉这一杯？", isPresented: $showClear) {
            Button("倒掉", role: .destructive) { store.clearCup() }
            Button("再留一下", role: .cancel) {}
        } message: {
            Text(
                isEditingRecipe
                    ? "只清空正在编辑的这一杯；原配方要等「保存修改」后才会变。"
                    : "杯里的层次会清空，豆子和材料不会受影响。"
            )
        }
        .toolbar(.hidden, for: .navigationBar)
        .onAppear { runDebugEditActionsIfNeeded() }
    }

    /// 截图脚本用：编辑态下自动打开「保存修改」弹窗或自动点「取消编辑」，
    /// 走的都是真实按钮对应的代码路径，便于在模拟器里稳定截图验证。只在 Debug 构建生效。
    private func runDebugEditActionsIfNeeded() {
        #if DEBUG
        guard isEditingRecipe else { return }
        if DebugEditFlow.shouldShowSaveSheet {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: 1_200_000_000)
                if store.isEditingRecipe && !showSave { showSave = true }
            }
        }
        if let delay = DebugEditFlow.autoCancelAfter {
            Task { @MainActor in
                try? await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
                guard store.editingRecipeID != nil else { return }
                store.cancelEditingRecipe()
                onEditCancelled()
            }
        }
        #endif
    }

    private var topBar: some View {
        HStack {
            VStack(alignment: .leading, spacing: 3) {
                Overline(text: isEditingRecipe ? "EDIT RECIPE" : "MAKE A CUP")
                Text("调一杯")
                    .font(.display(28))
                    .foregroundStyle(C.ink)
            }
            Spacer()
            RoundIconButton(systemName: "arrow.uturn.backward", accessibilityLabel: "撤销一层", size: 40) {
                store.undoLayer()
            }
            Button {
                if !store.draft.layers.isEmpty { showSave = true }
            } label: {
                HStack(spacing: 5) {
                    Image(systemName: "checkmark").font(.system(size: 11, weight: .bold))
                    Text(isEditingRecipe ? "保存修改" : "存一杯")
                        .font(.system(size: 12, weight: .medium))
                }
                .foregroundStyle(store.draft.layers.isEmpty ? C.muted : C.onAccent)
                .padding(.horizontal, 15)
                .frame(height: 40)
                .background(store.draft.layers.isEmpty ? C.lineSoft : C.lime)
                .clipShape(Capsule())
            }
            .buttonStyle(.plain)
            .disabled(store.draft.layers.isEmpty)
        }
        .padding(.horizontal, 20)
        .padding(.top, 8)
        .padding(.bottom, 6)
    }

    /// 编辑态提示条：显示正在修改哪一杯，并提供「取消编辑」。
    private var editingBanner: some View {
        HStack(spacing: 10) {
            ZStack {
                Circle().fill(C.lime).frame(width: 30, height: 30)
                Image(systemName: "pencil")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(C.ink)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text("正在修改配方 · \(store.editingRecipe?.name ?? "已删除的配方")")
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(C.ink)
                    .lineLimit(1)
                Text("保存后更新原配方，不会新增一杯")
                    .font(.system(size: 9.5))
                    .foregroundStyle(C.forest)
            }
            Spacer(minLength: 6)
            Button {
                store.cancelEditingRecipe()
                onEditCancelled()
            } label: {
                Text("取消编辑")
                    .font(.system(size: 11.5, weight: .medium))
                    .foregroundStyle(C.ink)
                    .padding(.horizontal, 12)
                    .frame(height: 30)
                    .background(C.card)
                    .clipShape(Capsule())
                    .overlay(Capsule().stroke(C.lineSoft, lineWidth: 1))
            }
            .buttonStyle(.plain)
            .accessibilityLabel("取消编辑，保留原配方")
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(C.limeSoft)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(C.lime.opacity(0.45), lineWidth: 1)
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 8)
    }

    private var beanRow: some View {
        HStack {
            Button {
                showBeanPicker = true
            } label: {
                HStack(spacing: 6) {
                    Circle()
                        .fill(selectedBean == nil ? C.mutedLight : C.lime)
                        .frame(width: 6, height: 6)
                    Text(selectedBean?.name ?? "未记豆子")
                        .font(.system(size: 12))
                        .foregroundStyle(C.inkSoft)
                        .lineLimit(1)
                    Image(systemName: "chevron.right")
                        .font(.system(size: 9, weight: .semibold))
                        .foregroundStyle(C.muted)
                }
                .padding(.horizontal, 11)
                .frame(height: 32)
                .background(C.card)
                .clipShape(Capsule())
                .overlay(Capsule().stroke(C.lineSoft, lineWidth: 1))
            }
            .buttonStyle(.plain)
            Spacer()
            Text("\(store.draft.totalGrams) / \(CUP_CAPACITY) g")
                .font(.mono(10))
                .foregroundStyle(C.muted)
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 6)
    }

    private var cupPanel: some View {
        ZStack {
            Circle()
                .fill(C.card.opacity(0.6))
                .frame(maxWidth: 260, maxHeight: 260)
                .aspectRatio(1, contentMode: .fit)
                .blur(radius: 1)
            CupCanvasView(
                layers: store.draft.layers,
                mixed: store.draft.stirred,
                normalized: false
            )
            // 保持杯子的修长比例（约 1:1.55），避免被拉伸成过长的细条
            .aspectRatio(0.64, contentMode: .fit)
            .frame(maxWidth: 280, maxHeight: 380)
            .padding(.horizontal, 6)
            .padding(.vertical, 4)
            if store.draft.layers.isEmpty {
                VStack(spacing: 7) {
                    Image(systemName: "leaf")
                        .font(.system(size: 16))
                        .foregroundStyle(C.mutedLight)
                        .rotationEffect(.degrees(-24))
                    Text("空杯子")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(C.muted)
                }
                .offset(y: 28)
            }
        }
        .padding(.horizontal, 4)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var layerPanel: some View {
        BrewCard {
            VStack(alignment: .leading, spacing: 8) {
                Overline(text: "IN THE CUP")
                HStack(alignment: .firstTextBaseline, spacing: 3) {
                    Text("\(store.draft.totalGrams)")
                        .font(.mono(25, weight: .medium))
                        .foregroundStyle(C.ink)
                    Text("g · \(store.draft.layers.count) 层")
                        .font(.mono(9))
                        .foregroundStyle(C.muted)
                }
                ProgressBar(
                    value: Double(store.draft.totalGrams) / Double(CUP_CAPACITY),
                    tint: C.lime,
                    height: 4
                )
                if store.draft.stirred {
                    Text("已搅拌 · 成品色")
                        .font(.system(size: 9.5))
                        .foregroundStyle(C.forest)
                }
                Rectangle().fill(C.line).frame(width: 22, height: 1)
                if store.draft.layers.isEmpty {
                    Text("按住下方材料\n向杯中倒入")
                        .font(.system(size: 11.5))
                        .foregroundStyle(C.muted)
                        .lineSpacing(4)
                    Spacer()
                } else {
                    ScrollView(showsIndicators: false) {
                        VStack(spacing: 2) {
                            ForEach(Array(store.draft.layers.indices).reversed(), id: \.self) { index in
                                let layer = store.draft.layers[index]
                                Button {
                                    editingLayer = LayerIndex(id: index)
                                } label: {
                                    HStack(spacing: 6) {
                                        Circle()
                                            .fill(Color(hex: layer.colorHex))
                                            .frame(width: 9, height: 9)
                                        Text(layer.name)
                                            .font(.system(size: 10.5))
                                            .foregroundStyle(C.inkSoft)
                                            .lineLimit(1)
                                        Spacer(minLength: 2)
                                        Text("\(layer.grams)g")
                                            .font(.mono(8))
                                            .foregroundStyle(C.muted)
                                    }
                                    .padding(.vertical, 5)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    Spacer(minLength: 4)
                    Text("点一层可微调")
                        .font(.system(size: 8.5))
                        .foregroundStyle(C.mutedLight)
                }
            }
            .padding(13)
        }
    }

    private var controls: some View {
        HStack(spacing: 8) {
            Button {
                store.toggleStir()
            } label: {
                HStack(spacing: 5) {
                    Image(systemName: "arrow.triangle.2.circlepath")
                        .font(.system(size: 12, weight: .medium))
                    Text(store.draft.stirred ? "恢复分层" : "搅拌均匀")
                        .font(.system(size: 12, weight: .medium))
                }
                .foregroundStyle(store.draft.stirred ? C.lime : C.inkSoft)
                .padding(.horizontal, 11)
                .frame(height: 32)
                .background(store.draft.stirred ? C.ink : C.card)
                .clipShape(Capsule())
                .overlay {
                    if !store.draft.stirred {
                        Capsule().stroke(C.lineSoft, lineWidth: 1)
                    }
                }
            }
            .buttonStyle(.plain)

            Button {
                if !store.draft.layers.isEmpty { showClear = true }
            } label: {
                Text("倒掉重来")
                    .font(.system(size: 12))
                    .foregroundStyle(store.draft.layers.isEmpty ? C.mutedLight : C.muted)
                    .padding(.horizontal, 7)
                    .frame(height: 32)
            }
            .buttonStyle(.plain)
            .disabled(store.draft.layers.isEmpty)

            Spacer()
            Text(store.draft.remaining == 0 ? "已满" : "按住材料 · 松手停止")
                .font(.system(size: 9))
                .foregroundStyle(store.draft.remaining == 0 ? C.limeDeep : C.muted)
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 5)
    }

    private var ingredientTray: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Overline(text: "INGREDIENT TRAY")
                Text("按住倒入")
                    .font(.system(size: 9))
                    .foregroundStyle(C.mutedLight)
                Spacer()
                Button {
                    showCustom = true
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "plus").font(.system(size: 9, weight: .bold))
                        Text("自定义").font(.system(size: 9.5, weight: .medium))
                    }
                    .foregroundStyle(C.forest)
                    .padding(.horizontal, 10)
                    .frame(height: 26)
                    .background(C.limeSoft)
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 20)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(store.allIngredients) { spec in
                        IngredientCard(
                            spec: spec,
                            onStart: { startPouring(spec) },
                            onEnd: { stopPouring() }
                        )
                    }
                    Button {
                        showCustom = true
                    } label: {
                        VStack(spacing: 6) {
                            ZStack {
                                Circle().fill(C.card).frame(width: 38, height: 38)
                                Image(systemName: "plus")
                                    .font(.system(size: 16, weight: .medium))
                                    .foregroundStyle(C.forest)
                            }
                            Text("新原料").font(.system(size: 10.5, weight: .medium)).foregroundStyle(C.forest)
                            Text("自选颜色").font(.system(size: 8)).foregroundStyle(C.muted)
                        }
                        .frame(width: 76, height: 96)
                        .background(C.limeSoft.opacity(0.55))
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                        .overlay {
                            RoundedRectangle(cornerRadius: 18)
                                .stroke(C.lime.opacity(0.35), lineWidth: 1)
                        }
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 12)
            }
        }
        .padding(.top, 9)
        .background(C.card)
        .clipShape(RoundedRectangle(cornerRadius: 30, style: .continuous))
        .overlay(alignment: .top) {
            RoundedRectangle(cornerRadius: 30, style: .continuous)
                .stroke(C.lineSoft, lineWidth: 1)
        }
        .padding(.horizontal, 14)
    }

    private func startPouring(_ spec: IngredientSpec) {
        stopPouring()
        haptic()
        pouringID = spec.id
        _ = store.pour(spec, requested: max(spec.rate / 2, 8))
        let timer = Timer.scheduledTimer(withTimeInterval: 0.12, repeats: true) { _ in
            Task { @MainActor in
                guard pouringID == spec.id else { return }
                if store.pour(spec, requested: max(spec.rate / 12, 2)) == 0 {
                    stopPouring()
                }
            }
        }
        RunLoop.main.add(timer, forMode: .common)
        pourTimer = timer
    }

    private func stopPouring() {
        pourTimer?.invalidate()
        pourTimer = nil
        pouringID = nil
    }
}

private struct IngredientCard: View {
    let spec: IngredientSpec
    let onStart: () -> Void
    let onEnd: () -> Void

    @State private var pressing = false
    @State private var pouring = false

    var body: some View {
        VStack(spacing: 6) {
            ZStack {
                Circle()
                    .fill(Color(hex: spec.colorHex).opacity(max(spec.alpha, 0.35)))
                    .frame(width: 38, height: 38)
                Image(systemName: "drop")
                    .font(.system(size: 14))
                    .foregroundStyle(.white.opacity(0.4))
            }
            Text(spec.name)
                .font(.system(size: 10.5))
                .foregroundStyle(C.ink)
                .lineLimit(1)
            Text(spec.isCustom ? "自定义" : "约\(spec.rate)g/s")
                .font(.mono(8))
                .foregroundStyle(spec.isCustom ? C.forest : C.muted)
        }
        .frame(width: 76, height: 96)
        .background(pressing ? C.limeSoft : C.paperSoft)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .scaleEffect(pressing ? 0.94 : 1)
        .animation(.easeOut(duration: 0.12), value: pressing)
        // 注意：之前「长按 + DragGesture(minimumDistance: 0)」会把横向滑动手势吃掉，
        // 导致原料托盘无法左右滚动。这里改为纯长按手势：
        //  - 长按成功后开始倒入；松手（或手势失败）即停止；
        //  - 手指移动超过 maximumDistance 时长按失败，ScrollView 可以正常接管横向滑动。
        .onLongPressGesture(minimumDuration: 0.18, maximumDistance: 22) {
            pouring = true
            onStart()
        } onPressingChanged: { isPressing in
            if isPressing {
                pressing = true
            } else {
                pressing = false
                if pouring {
                    pouring = false
                    onEnd()
                }
            }
        }
        .accessibilityLabel("按住倒入\(spec.name)")
    }
}

private struct BeanPickerSheet: View {
    @EnvironmentObject private var store: AppStore
    @Environment(\.dismiss) private var dismiss
    let selected: String?
    let onSelect: (String?) -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text("这杯用了什么豆？")
                        .font(.system(size: 22, weight: .semibold))
                    Text("标了豆，这杯才会记到它的名下，也会从豆仓扣掉克数。")
                        .font(.system(size: 12))
                        .foregroundStyle(C.muted)

                    SmallChoiceButton(title: "不记豆子", selected: selected == nil) {
                        onSelect(nil)
                        dismiss()
                    }

                    ForEach(store.recentBeans) { bean in
                        Button {
                            onSelect(bean.id)
                            dismiss()
                        } label: {
                            HStack(spacing: 11) {
                                BeanArtwork(bean: bean, showStatus: false)
                                    .frame(width: 58, height: 58)
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(bean.name)
                                        .font(.system(size: 14, weight: .medium))
                                        .foregroundStyle(C.ink)
                                    Text("\(bean.origin) · \(bean.roast.label) · 第\(bean.ageDays)天")
                                        .font(.system(size: 10.5))
                                        .foregroundStyle(C.muted)
                                }
                                Spacer()
                                Text(formatGrams(bean.remainingGrams))
                                    .font(.system(size: 11.5, weight: .medium))
                                    .foregroundStyle(C.forest)
                                if selected == bean.id {
                                    Image(systemName: "checkmark")
                                        .font(.system(size: 12, weight: .semibold))
                                        .foregroundStyle(C.forest)
                                }
                            }
                            .padding(9)
                            .background(selected == bean.id ? C.limeSoft : C.paperSoft)
                            .clipShape(RoundedRectangle(cornerRadius: 18))
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(20)
            }
            .background(C.card)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { dismiss() }
                        .foregroundStyle(C.ink)
                }
            }
        }
    }
}

private struct CustomIngredientSheet: View {
    @EnvironmentObject private var store: AppStore
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var color = palette[0]
    @State private var kind: IngredientKind = .liquid

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("自定义原料")
                        .font(.system(size: 24, weight: .semibold))
                    Text("给它一个名字和颜色。倒法、克数调整、保存方式，都和托盘里的材料一样。")
                        .font(.system(size: 12))
                        .foregroundStyle(C.muted)
                    HStack(spacing: 12) {
                        ZStack {
                            Circle().fill(Color(hex: color)).frame(width: 54, height: 54)
                            Image(systemName: "drop")
                                .font(.system(size: 18))
                                .foregroundStyle(.white.opacity(0.45))
                        }
                        BrewTextField(label: "材料名", placeholder: "如 海盐奶盖 / 自制桂花蜜", text: $name, limit: 14)
                    }
                    Overline(text: "选择成品色")
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 8), spacing: 9) {
                        ForEach(palette, id: \.self) { hex in
                            Button {
                                color = hex
                            } label: {
                                ZStack {
                                    Circle().fill(Color(hex: hex)).frame(width: 36, height: 36)
                                    if color == hex {
                                        Image(systemName: "checkmark")
                                            .font(.system(size: 12, weight: .bold))
                                            .foregroundStyle(.white)
                                    }
                                }
                                .overlay(Circle().stroke(color == hex ? C.ink : C.lineSoft, lineWidth: color == hex ? 2 : 1))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    Overline(text: "质地")
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 64), spacing: 7)], alignment: .leading, spacing: 7) {
                        ForEach(IngredientKind.allCases) { item in
                            SmallChoiceButton(title: item.label, selected: kind == item) {
                                kind = item
                            }
                        }
                    }
                    if !store.data.customIngredients.isEmpty {
                        Overline(text: "已有的自定义材料")
                        ForEach(store.data.customIngredients) { item in
                            HStack {
                                Circle().fill(Color(hex: item.colorHex)).frame(width: 14, height: 14)
                                Text(item.name).font(.system(size: 13)).foregroundStyle(C.inkSoft)
                                Spacer()
                                Button {
                                    store.deleteCustomIngredient(item.id)
                                } label: {
                                    Image(systemName: "xmark")
                                        .font(.system(size: 10, weight: .semibold))
                                        .foregroundStyle(C.muted)
                                        .frame(width: 30, height: 30)
                                        .background(C.paperSoft)
                                        .clipShape(Circle())
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    PrimaryActionButton(title: "加入托盘", enabled: !name.trimmingCharacters(in: .whitespaces).isEmpty) {
                        store.addCustomIngredient(name: name, colorHex: color, kind: kind)
                        dismiss()
                    }
                    .padding(.top, 6)
                }
                .padding(20)
                .padding(.bottom, 30)
            }
            .background(C.card)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { dismiss() }.foregroundStyle(C.ink)
                }
            }
        }
    }
}

private struct SaveCupSheet: View {
    @EnvironmentObject private var store: AppStore
    @Environment(\.dismiss) private var dismiss

    let editingRecipe: Recipe?
    let suggestedName: String
    let initialBeanId: String?
    let totalGrams: Int
    let layerCount: Int
    let onSaved: (String, Bool) -> Void

    @State private var name: String
    @State private var beanId: String?
    @State private var rating: Int
    @State private var mouthfeel: String
    @State private var sweetness: String
    @State private var gramsText: String
    @State private var priceText: String
    @State private var notes: String
    @State private var saving = false
    @State private var error: String?

    init(
        editingRecipe: Recipe?,
        suggestedName: String,
        initialBeanId: String?,
        totalGrams: Int,
        layerCount: Int,
        onSaved: @escaping (String, Bool) -> Void
    ) {
        self.editingRecipe = editingRecipe
        self.suggestedName = suggestedName
        self.initialBeanId = initialBeanId
        self.totalGrams = totalGrams
        self.layerCount = layerCount
        self.onSaved = onSaved
        _name = State(initialValue: editingRecipe?.name ?? suggestedName)
        _beanId = State(initialValue: editingRecipe?.beanId ?? initialBeanId)
        _rating = State(initialValue: editingRecipe?.rating ?? 4)
        _mouthfeel = State(initialValue: editingRecipe?.mouthfeel ?? "均衡")
        _sweetness = State(initialValue: editingRecipe?.sweetness ?? "甜感适中")
        _gramsText = State(initialValue: editingRecipe.map { "\(max($0.beanGrams, 0))" } ?? "15")
        _priceText = State(initialValue: editingRecipe?.price.map { "\($0)" } ?? "")
        _notes = State(initialValue: editingRecipe?.notes ?? "")
    }

    private var selectedBean: Bean? { store.bean(beanId) }
    private var isEditing: Bool { editingRecipe != nil }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    HStack {
                        VStack(alignment: .leading, spacing: 3) {
                            Overline(text: isEditing ? "EDIT THIS RECIPE" : "FILE THIS CUP")
                            Text(isEditing ? "保存修改" : "存进菜单")
                                .font(.system(size: 22, weight: .semibold))
                        }
                        Spacer()
                        MiniCupView(layers: store.draft.layers, mixed: store.draft.stirred)
                            .padding(8)
                            .frame(width: 50, height: 62)
                            .background(C.paperSoft)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    if isEditing, let editingRecipe {
                        Text("正在修改「\(editingRecipe.name)」，保存后更新原配方，不会新增一杯、不影响豆仓库存。")
                            .font(.system(size: 11))
                            .foregroundStyle(C.muted)
                            .lineSpacing(2)
                    }
                    BrewTextField(label: "配方名称", placeholder: "给这一杯起个名字", text: $name, limit: 30)
                    Menu {
                        Button("不记豆子") { beanId = nil }
                        ForEach(store.data.beans) { bean in
                            Button(bean.name) {
                                beanId = bean.id
                                gramsText = "\(min(Int(bean.remainingGrams), 15))"
                            }
                        }
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("用豆").font(.mono(9)).foregroundStyle(C.muted)
                                Text(selectedBean?.name ?? "不记豆子")
                                    .font(.system(size: 14))
                                    .foregroundStyle(C.ink)
                            }
                            Spacer()
                            Image(systemName: "chevron.right").font(.system(size: 11)).foregroundStyle(C.muted)
                        }
                        .padding(14)
                        .background(C.paperSoft)
                        .clipShape(RoundedRectangle(cornerRadius: 17))
                    }

                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("这一杯怎么样").font(.system(size: 13, weight: .medium)).foregroundStyle(C.inkSoft)
                            Text(ratingText).font(.system(size: 10.5)).foregroundStyle(C.muted)
                        }
                        Spacer()
                        HStack(spacing: 7) {
                            ForEach(1...5, id: \.self) { value in
                                Button {
                                    rating = value
                                } label: {
                                    Image(systemName: value <= rating ? "leaf.fill" : "leaf")
                                        .font(.system(size: 15))
                                        .foregroundStyle(value <= rating ? C.ink : C.line)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    Text("口感").font(.system(size: 13, weight: .medium)).foregroundStyle(C.inkSoft)
                    HStack(spacing: 7) {
                        ForEach(["清爽", "均衡", "浓郁"], id: \.self) { item in
                            SmallChoiceButton(title: item, selected: mouthfeel == item) { mouthfeel = item }
                        }
                    }
                    Text("甜度").font(.system(size: 13, weight: .medium)).foregroundStyle(C.inkSoft)
                    HStack(spacing: 7) {
                        ForEach(["微甜", "甜感适中", "明显偏甜"], id: \.self) { item in
                            SmallChoiceButton(title: item, selected: sweetness == item) { sweetness = item }
                        }
                    }
                    if let selectedBean {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("用豆克数").font(.system(size: 13, weight: .medium)).foregroundStyle(C.inkSoft)
                                Text(
                                    isEditing
                                        ? "剩余 \(formatGrams(selectedBean.remainingGrams))，保存修改不会重复扣豆、不影响库存"
                                        : "剩余 \(formatGrams(selectedBean.remainingGrams))，会从豆仓扣除"
                                )
                                .font(.system(size: 10.5)).foregroundStyle(C.muted)
                            }
                            Spacer()
                            TextField("15", text: $gramsText)
                                .keyboardType(.numberPad)
                                .multilineTextAlignment(.trailing)
                                .font(.mono(16))
                                .frame(width: 68, height: 40)
                                .padding(.horizontal, 10)
                                .background(C.paperSoft)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                    }
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("菜单价格").font(.system(size: 13, weight: .medium)).foregroundStyle(C.inkSoft)
                            Text("会显示在菜单列表和随杯卡上，可以留空")
                                .font(.system(size: 10.5)).foregroundStyle(C.muted)
                        }
                        Spacer()
                        HStack(spacing: 3) {
                            Text("¥").font(.mono(13)).foregroundStyle(C.muted)
                            TextField("未设置", text: $priceText)
                                .keyboardType(.numberPad)
                                .multilineTextAlignment(.trailing)
                                .font(.mono(15))
                        }
                        .frame(width: 96, height: 40)
                        .padding(.horizontal, 10)
                        .background(C.paperSoft)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    BrewTextEditor(label: "品鉴笔记", placeholder: "这一杯的香气、口感和下次想改的地方。", text: $notes)
                    if let error {
                        Text(error).font(.system(size: 11.5)).foregroundStyle(Color(hex: "B45B4A"))
                    }
                    HStack {
                        Text("\(totalGrams)g · \(layerCount) 层").font(.mono(9)).foregroundStyle(C.muted)
                        Spacer()
                        Text("配方存在本机").font(.mono(8)).foregroundStyle(C.mutedLight)
                    }
                    PrimaryActionButton(
                        title: saving
                            ? (isEditing ? "正在保存修改…" : "正在存进菜单…")
                            : (isEditing ? "保存修改" : "存进菜单"),
                        enabled: !saving && !name.trimmingCharacters(in: .whitespaces).isEmpty
                    ) {
                        save()
                    }
                }
                .padding(20)
                .padding(.bottom, 30)
            }
            .background(C.card)
            .navigationTitle("")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("取消") { dismiss() }.foregroundStyle(C.ink)
                }
            }
        }
        .onAppear { runDebugAutoSaveIfNeeded() }
    }

    /// 截图脚本用：弹窗出现后自动改名字/笔记并调用与「保存修改」按钮相同的 save()。
    /// 只在 Debug 构建生效。
    private func runDebugAutoSaveIfNeeded() {
        #if DEBUG
        guard DebugEditFlow.autoSave, isEditing, let editingRecipe else { return }
        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 1_000_000_000)
            guard !saving else { return }
            if let soda = store.allIngredients.first(where: { $0.id == "soda" }) {
                _ = store.pour(soda, requested: 10)
            }
            name = "\(editingRecipe.name) · 加满气泡"
            notes = editingRecipe.notes + " 编辑验证：再补 10g 气泡水，气泡感更足。"
            gramsText = "\(max(editingRecipe.beanGrams, 0))"
            save()
        }
        #endif
    }

    private var ratingText: String {
        switch rating {
        case 5: return "惊艳"
        case 4: return "很好喝"
        case 3: return "还不错"
        case 2: return "一般"
        default: return "再试试"
        }
    }

    private func save() {
        guard !saving else { return }
        let grams = selectedBean == nil ? 0 : Int(gramsText) ?? 0
        let price = Int(priceText)
        saving = true
        let saved: Recipe?
        if let editingRecipe {
            saved = store.updateRecipe(
                id: editingRecipe.id,
                name: name,
                beanId: beanId,
                rating: rating,
                notes: notes,
                mouthfeel: mouthfeel,
                sweetness: sweetness,
                beanGrams: grams,
                price: price
            )
        } else {
            saved = store.saveRecipe(
                name: name,
                beanId: beanId,
                rating: rating,
                notes: notes,
                mouthfeel: mouthfeel,
                sweetness: sweetness,
                beanGrams: grams,
                price: price
            )
        }
        if let saved {
            onSaved(saved.id, isEditing)
            dismiss()
        } else {
            saving = false
            error = isEditing ? "找不到原来那杯配方了，可能刚被删除。" : "杯里还没有内容。"
        }
    }
}

private struct LayerEditSheet: View {
    @Environment(\.dismiss) private var dismiss
    let layer: RecipeLayer
    let maxGrams: Int
    let onApply: (Int) -> Void
    let onRemove: () -> Void
    @State private var grams: Double

    init(layer: RecipeLayer, maxGrams: Int, onApply: @escaping (Int) -> Void, onRemove: @escaping () -> Void) {
        self.layer = layer
        self.maxGrams = maxGrams
        self.onApply = onApply
        self.onRemove = onRemove
        _grams = State(initialValue: Double(min(max(layer.grams, 1), max(maxGrams, 1))))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Circle().fill(Color(hex: layer.colorHex)).frame(width: 12, height: 12)
                Text(layer.name).font(.system(size: 22, weight: .semibold))
                Spacer()
                Text("\(Int(grams)) g").font(.mono(20))
            }
            Text("这支材料在这一杯里的克数。最多还能加到 \(maxGrams)g。")
                .font(.system(size: 11.5))
                .foregroundStyle(C.muted)
            Slider(value: $grams, in: 1...Double(max(maxGrams, 1)), step: 1)
                .tint(C.lime)
            HStack {
                Text("1g").font(.mono(9)).foregroundStyle(C.muted)
                Spacer()
                Text("\(maxGrams)g").font(.mono(9)).foregroundStyle(C.muted)
            }
            HStack(spacing: 8) {
                Button {
                    onRemove()
                    dismiss()
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "trash").font(.system(size: 12))
                        Text("删除").font(.system(size: 13, weight: .medium))
                    }
                    .foregroundStyle(C.brown)
                    .padding(.horizontal, 15)
                    .frame(height: 48)
                    .background(C.paperSoft)
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                PrimaryActionButton(title: "完成") {
                    onApply(Int(grams))
                    dismiss()
                }
            }
        }
        .padding(20)
        .background(C.card)
    }
}


private struct LayerIndex: Identifiable {
    let id: Int
}
