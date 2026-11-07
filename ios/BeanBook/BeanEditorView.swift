import Foundation
import SwiftUI
import UIKit
import PhotosUI

struct BeanEditorView: View {
    @EnvironmentObject private var store: AppStore
    @Environment(\.dismiss) private var dismiss

    let bean: Bean?

    @State private var name: String
    @State private var roaster: String
    @State private var origin: String
    @State private var region: String
    @State private var process: String
    @State private var roast: Roast
    @State private var storage: Storage
    @State private var flavors: [String]
    @State private var totalText: String
    @State private var remainingText: String
    @State private var roastDate: Date
    @State private var note: String
    @State private var customFlavor = ""
    @State private var pickerItem: PhotosPickerItem?
    @State private var showPicker = false
    @State private var selectedImage: UIImage?
    @State private var existingImage: UIImage?
    @State private var isProcessing = false
    @State private var cutoutSucceeded = false
    @State private var message: String?
    @State private var error: String?
    @State private var saving = false

    private let flavorSuggestions = [
        "茉莉", "柠檬", "红茶感", "蓝莓", "草莓", "柑橘", "黑加仑",
        "焦糖", "坚果", "可可", "蜂蜜", "奶油", "白葡萄", "荔枝"
    ]

    init(bean: Bean?) {
        self.bean = bean
        _name = State(initialValue: bean?.name ?? "")
        _roaster = State(initialValue: bean?.roaster ?? "")
        _origin = State(initialValue: bean?.origin ?? "")
        _region = State(initialValue: bean?.region ?? "")
        _process = State(initialValue: bean?.process ?? "水洗")
        _roast = State(initialValue: bean?.roast ?? .medium)
        _storage = State(initialValue: bean?.storage ?? .room)
        _flavors = State(initialValue: bean?.flavors ?? [])
        _totalText = State(initialValue: bean.map { Self.plainNumber($0.totalGrams) } ?? "150")
        _remainingText = State(initialValue: bean.map { Self.plainNumber($0.remainingGrams) } ?? "150")
        _roastDate = State(initialValue: bean.map { Date(timeIntervalSince1970: Double($0.roastDate) / 1000) } ?? Date())
        _note = State(initialValue: bean?.note ?? "")
    }

    private var previewImage: UIImage? { selectedImage ?? existingImage }

    var body: some View {
        VStack(spacing: 0) {
            topBar
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 18) {
                    photoSection
                    basicSection
                    roastSection
                    weightSection
                    if let error {
                        Text(error)
                            .font(.system(size: 12))
                            .foregroundStyle(Color(hex: "B45B4A"))
                            .padding(.horizontal, 4)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 4)
                .padding(.bottom, 24)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .background(PaperBackground())
        .safeAreaInset(edge: .bottom, spacing: 0) {
            BottomActionBar {
                PrimaryActionButton(
                    title: saving ? "正在收进豆仓…" : (bean == nil ? "收进豆仓" : "保存修改"),
                    enabled: !saving
                ) {
                    save()
                }
            }
        }
        .photosPicker(isPresented: $showPicker, selection: $pickerItem, matching: .images)
        .onChange(of: pickerItem) { _, item in
            guard let item else { return }
            Task { await load(item) }
        }
        .onAppear {
            if existingImage == nil, let bean, let image = store.uiImage(for: bean) {
                existingImage = image
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
    }

    private var topBar: some View {
        HStack {
            RoundIconButton(systemName: "chevron.left", accessibilityLabel: "返回") {
                dismiss()
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 3) {
                Overline(text: bean == nil ? "NEW BEAN" : "EDIT BEAN")
                Text(bean == nil ? "收进豆仓" : "编辑豆子")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(C.ink)
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 8)
        .padding(.bottom, 10)
    }

    private var photoSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            ZStack {
                RoundedRectangle(cornerRadius: 30, style: .continuous)
                    .fill(C.peach)
                if let previewImage {
                    Image(uiImage: previewImage)
                        .resizable()
                        .scaledToFit()
                        .padding(18)
                } else {
                    VStack(spacing: 12) {
                        ZStack {
                            Circle().fill(C.card.opacity(0.64)).frame(width: 70, height: 70)
                            Image(systemName: "camera")
                                .font(.system(size: 26, weight: .regular))
                                .foregroundStyle(C.inkSoft)
                        }
                        Text("添加豆袋照片")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(C.ink)
                        Text("自动抠图 · 推荐干净的浅色背景")
                            .font(.system(size: 11.5))
                            .foregroundStyle(C.muted)
                    }
                }

                if cutoutSucceeded {
                    VStack {
                        HStack {
                            Spacer()
                            HStack(spacing: 4) {
                                Image(systemName: "checkmark").font(.system(size: 9, weight: .bold))
                                Text("已抠图").font(.system(size: 9.5, weight: .medium))
                            }
                            .foregroundStyle(C.forest)
                            .padding(.horizontal, 9)
                            .padding(.vertical, 5)
                            .background(C.limeSoft)
                            .clipShape(Capsule())
                        }
                        Spacer()
                    }
                    .padding(12)
                }

                if isProcessing {
                    RoundedRectangle(cornerRadius: 30, style: .continuous)
                        .fill(C.ink.opacity(0.46))
                    VStack(spacing: 10) {
                        ProgressView().tint(C.lime)
                        Text("正在分离豆子与背景")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundStyle(.white)
                        Text("第一次会稍慢一点")
                            .font(.system(size: 11))
                            .foregroundStyle(.white.opacity(0.65))
                    }
                }
            }
            .frame(height: 244)

            HStack(spacing: 8) {
                Button {
                    showPicker = true
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "camera").font(.system(size: 13, weight: .medium))
                        Text(previewImage == nil ? "从相册选择" : "换一张照片")
                            .font(.system(size: 12.5, weight: .medium))
                    }
                    .foregroundStyle(C.ink)
                    .padding(.horizontal, 14)
                    .frame(height: 38)
                    .background(C.card)
                    .clipShape(Capsule())
                    .overlay(Capsule().stroke(C.lineSoft, lineWidth: 1))
                }
                .buttonStyle(.plain)

                if selectedImage != nil {
                    Button {
                        runCutout()
                    } label: {
                        Text("重新抠图")
                            .font(.system(size: 12.5, weight: .medium))
                            .foregroundStyle(C.inkSoft)
                            .padding(.horizontal, 14)
                            .frame(height: 38)
                            .background(C.paperSoft)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
                Spacer()
            }

            Text(message ?? "照片会在本机离线抠图，不会上传。")
                .font(.system(size: 11.5))
                .foregroundStyle(cutoutSucceeded ? C.forest : C.muted)
        }
    }

    private var basicSection: some View {
        BrewCard {
            VStack(alignment: .leading, spacing: 13) {
                SectionHeading(title: "豆子档案", overline: "01 · BEAN")
                BrewTextField(label: "豆名 *", placeholder: "如 Ethiopia Guji / 云南红蜜", text: $name, limit: 40)
                BrewTextField(label: "烘焙商 / 豆商", placeholder: "如 Coffee Buff", text: $roaster, limit: 40)
                HStack(spacing: 10) {
                    BrewTextField(label: "产地", placeholder: "埃塞俄比亚", text: $origin, limit: 30)
                    BrewTextField(label: "产区 / 庄园", placeholder: "Guji", text: $region, limit: 30)
                }
                VStack(alignment: .leading, spacing: 8) {
                    Text("处理法")
                        .font(.system(size: 12.5, weight: .medium))
                        .foregroundStyle(C.inkSoft)
                    LazyVGrid(
                        columns: [GridItem(.adaptive(minimum: 64), spacing: 7)],
                        alignment: .leading,
                        spacing: 7
                    ) {
                        ForEach(["水洗", "日晒", "蜜处理", "湿刨", "厌氧"], id: \.self) { item in
                            SmallChoiceButton(title: item, selected: process == item) {
                                process = item
                            }
                        }
                    }
                }
            }
            .padding(18)
        }
    }

    private var roastSection: some View {
        BrewCard {
            VStack(alignment: .leading, spacing: 14) {
                SectionHeading(title: "烘焙与风味", overline: "02 · ROAST & FLAVOR")
                Text("烘焙度")
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(C.inkSoft)
                HStack(spacing: 5) {
                    ForEach(Roast.allCases) { level in
                        Button {
                            roast = level
                        } label: {
                            Text(level.label)
                                .font(.system(size: 10.5, weight: .medium))
                                .foregroundStyle(roast == level ? .white : C.inkSoft)
                                .lineLimit(1)
                                .minimumScaleFactor(0.7)
                                .frame(maxWidth: .infinity)
                                .frame(height: 34)
                                .background(roast == level ? Color(hex: level.toneHex) : C.paperSoft)
                                .clipShape(RoundedRectangle(cornerRadius: 11, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    }
                }

                Text("风味标签 · 最多 7 个")
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(C.inkSoft)
                LazyVGrid(
                    columns: [GridItem(.adaptive(minimum: 62), spacing: 7)],
                    alignment: .leading,
                    spacing: 7
                ) {
                    ForEach(flavorSuggestions, id: \.self) { flavor in
                        SmallChoiceButton(title: flavor, selected: flavors.contains(flavor)) {
                            toggleFlavor(flavor)
                        }
                    }
                }

                HStack(spacing: 8) {
                    BrewTextField(label: "自定义风味", placeholder: "如 荔枝壳 / 海盐", text: $customFlavor, limit: 12)
                    Button {
                        addCustomFlavor()
                    } label: {
                        Image(systemName: "plus")
                            .font(.system(size: 17, weight: .medium))
                            .foregroundStyle(C.ink)
                            .frame(width: 46, height: 46)
                            .background(C.lime)
                            .clipShape(Circle())
                    }
                    .buttonStyle(.plain)
                }
                if !flavors.isEmpty {
                    Text(flavors.joined(separator: " · "))
                        .font(.system(size: 11.5))
                        .foregroundStyle(C.forest)
                }
            }
            .padding(18)
        }
    }

    private var weightSection: some View {
        BrewCard {
            VStack(alignment: .leading, spacing: 13) {
                SectionHeading(title: "克重与保存", overline: "03 · WEIGHT")
                HStack(spacing: 10) {
                    BrewTextField(
                        label: "总克重 (g)",
                        placeholder: "150",
                        text: Binding(
                            get: { totalText },
                            set: { totalText = Self.numeric($0) }
                        ),
                        keyboard: .decimalPad,
                        limit: 6
                    )
                    BrewTextField(
                        label: "剩余克重 (g)",
                        placeholder: "150",
                        text: Binding(
                            get: { remainingText },
                            set: { remainingText = Self.numeric($0) }
                        ),
                        keyboard: .decimalPad,
                        limit: 6
                    )
                }

                DatePicker(
                    "烘焙日期",
                    selection: $roastDate,
                    displayedComponents: .date
                )
                .font(.system(size: 13, weight: .medium))
                .tint(C.limeDeep)

                Text("保存方式")
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(C.inkSoft)
                HStack(spacing: 7) {
                    ForEach(Storage.allCases) { storageValue in
                        SmallChoiceButton(
                            title: storageValue.label,
                            selected: storage == storageValue
                        ) {
                            storage = storageValue
                        }
                    }
                }

                BrewTextEditor(
                    label: "备注",
                    placeholder: "记录这支豆子的小脾气，或下一次想怎么冲。",
                    text: $note
                )
            }
            .padding(18)
        }
    }

    private func toggleFlavor(_ flavor: String) {
        if flavors.contains(flavor) {
            flavors.removeAll { $0 == flavor }
        } else if flavors.count < 7 {
            flavors.append(flavor)
        }
    }

    private func addCustomFlavor() {
        let clean = customFlavor.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty, flavors.count < 7, !flavors.contains(clean) else { return }
        flavors.append(clean)
        customFlavor = ""
    }

    private func load(_ item: PhotosPickerItem) async {
        guard let data = try? await item.loadTransferable(type: Data.self),
              let image = UIImage(data: data) else {
            message = "这张照片读不出来，换一张试试。"
            return
        }
        selectedImage = image.bbDownscaled(maxDimension: 1400)
        cutoutSucceeded = false
        message = nil
        runCutout()
    }

    private func runCutout() {
        guard let image = selectedImage else { return }
        isProcessing = true
        message = nil
        Task {
            let result = await ImageCutout.removeBackground(from: image)
            isProcessing = false
            if let result {
                selectedImage = result
                cutoutSucceeded = true
                message = "背景已自动去掉，可以保存了。"
            } else {
                message = "自动抠图没有成功，将保留原图。"
            }
        }
    }

    private func save() {
        guard !saving else { return }
        let cleanName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanName.isEmpty else {
            error = "先给这支豆子起一个名字。"
            return
        }
        let total = Double(totalText) ?? 0
        guard total > 0 else {
            error = "总克重需要大于 0。"
            return
        }
        let remaining = min(max(Double(remainingText) ?? total, 0), total)
        saving = true

        var imagePath = bean?.imagePath
        if let selectedImage, let path = store.saveImage(selectedImage) {
            imagePath = path
        }
        let newBean = Bean(
            id: bean?.id ?? UUID().uuidString,
            name: cleanName,
            roaster: roaster.trimmingCharacters(in: .whitespacesAndNewlines),
            origin: origin.trimmingCharacters(in: .whitespacesAndNewlines),
            region: region.trimmingCharacters(in: .whitespacesAndNewlines),
            process: process,
            roast: roast,
            flavors: flavors,
            totalGrams: total,
            remainingGrams: remaining,
            roastDate: Int64(Calendar.current.startOfDay(for: roastDate).timeIntervalSince1970 * 1000),
            openedAt: bean?.openedAt ?? nowMillis(),
            storage: storage,
            imagePath: imagePath,
            note: note.trimmingCharacters(in: .whitespacesAndNewlines),
            createdAt: bean?.createdAt ?? nowMillis(),
            lastUsedAt: bean?.lastUsedAt ?? 0
        )
        if let bean {
            store.updateBean(newBean)
            if bean.imagePath != imagePath {
                store.deleteImage(path: bean.imagePath)
            }
        } else {
            store.addBean(newBean)
        }
        saving = false
        dismiss()
    }

    private static func numeric(_ value: String) -> String {
        let filtered = value.filter { $0.isNumber || $0 == "." }
        let parts = filtered.split(separator: ".", omittingEmptySubsequences: false)
        if parts.count > 2 {
            return String(parts[0]) + "." + parts.dropFirst().joined()
        }
        return String(filtered.prefix(6))
    }

    private static func plainNumber(_ value: Double) -> String {
        value.rounded() == value ? "\(Int(value))" : String(value)
    }
}
