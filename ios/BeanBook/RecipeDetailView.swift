import Foundation
import SwiftUI

struct RecipeDetailView: View {
    @EnvironmentObject private var store: AppStore
    @Environment(\.dismiss) private var dismiss

    let initialRecipe: Recipe
    let onRepeat: () -> Void
    let onOpenBean: (String) -> Void
    let onPromoCard: () -> Void
    let onEdit: () -> Void
    let onDelete: () -> Void

    @State private var showShare = false
    @State private var shareURL: URL?
    @State private var confirmDelete = false
    @State private var errorMessage: String?

    /// 实时从 Store 取最新的一杯：编辑保存后详情页无需重新进入就能看到更新。
    private var recipe: Recipe { store.recipe(initialRecipe.id) ?? initialRecipe }

    private var bean: Bean? { store.bean(recipe.beanId) }
    private var baseTint: Color {
        let colors = recipe.layers.map { (Color(hex: $0.colorHex), Double($0.grams)) }
        return Color.blend(colors).opacity(0.18)
    }

    var body: some View {
        VStack(spacing: 0) {
            topBar
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 17) {
                    cupCard
                    titleBlock
                    statsCard
                    promoCardEntry
                    SectionHeading(title: "配方层次", overline: "LAYER BY LAYER")
                    ForEach(Array(recipe.layers.indices), id: \.self) { index in
                        layerRow(index)
                    }
                    if !recipe.notes.isEmpty { notesCard }
                    if let bean { beanCard(bean) }
                    Button {
                        confirmDelete = true
                    } label: {
                        Text("从菜单删除这一杯")
                            .font(.system(size: 11.5))
                            .foregroundStyle(C.muted)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 13)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 20)
                .padding(.top, 4)
                .padding(.bottom, 24)
            }
        }
        .background(PaperBackground())
        .safeAreaInset(edge: .bottom, spacing: 0) {
            BottomActionBar {
                HStack(spacing: 10) {
                    OutlineActionButton(title: "分享文件", action: shareRecipeFile)
                    PrimaryActionButton(title: "再调一杯", action: onRepeat)
                }
            }
        }
        .sheet(isPresented: $showShare) {
            if let shareURL {
                ShareSheet(items: [shareURL])
            }
        }
        .alert("删除这一杯？", isPresented: $confirmDelete) {
            Button("删除", role: .destructive) { onDelete() }
            Button("再想想", role: .cancel) {}
        } message: {
            Text("删除后会从菜单消失；豆仓里的豆子不会恢复。")
        }
        .alert("无法分享", isPresented: Binding(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } }
        )) {
            Button("好", role: .cancel) {}
        } message: {
            Text(errorMessage ?? "")
        }
        .toolbar(.hidden, for: .navigationBar)
    }

    private var topBar: some View {
        HStack {
            RoundIconButton(systemName: "chevron.left", accessibilityLabel: "返回") {
                dismiss()
            }
            Spacer()
            RoundIconButton(
                systemName: "pencil",
                accessibilityLabel: "编辑配方",
                background: C.lime,
                foreground: C.onAccent
            ) {
                onEdit()
            }
            RoundIconButton(systemName: "square.and.arrow.up", accessibilityLabel: "分享配方文件") {
                shareRecipeFile()
            }
            Menu {
                Button("宣传卡片") { onPromoCard() }
                Button("编辑配方") { onEdit() }
                Button("删除这一杯", role: .destructive) { confirmDelete = true }
            } label: {
                Image(systemName: "ellipsis")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(C.ink)
                    .frame(width: 44, height: 44)
                    .background(C.card)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(C.lineSoft, lineWidth: 1))
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 8)
        .padding(.bottom, 10)
    }

    private var cupCard: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 32, style: .continuous)
                .fill(C.paperSoft)
            baseTint
                .clipShape(RoundedRectangle(cornerRadius: 32, style: .continuous))
            Circle().fill(C.card.opacity(0.35)).frame(width: 250, height: 250)
            CupCanvasView(layers: recipe.layers, mixed: recipe.stirred, normalized: true)
                .padding(.horizontal, 34)
                .padding(.vertical, 14)
            VStack {
                HStack {
                    TagPill(title: "RECIPE", dot: C.lime)
                    Spacer()
                    Text(recipe.createdAt.date.dayLabel)
                        .font(.mono(9))
                        .foregroundStyle(C.inkSoft)
                        .padding(.horizontal, 9)
                        .padding(.vertical, 6)
                        .background(C.card.opacity(0.82))
                        .clipShape(Capsule())
                }
                Spacer()
            }
            .padding(12)
        }
        .frame(height: 326)
    }

    private var titleBlock: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Overline(text: recipe.dateTitle)
                BeanRating(rating: recipe.rating, size: 15)
                if let price = recipe.price {
                    Text("¥\(price)")
                        .font(.mono(12, weight: .medium))
                        .foregroundStyle(C.forest)
                }
                Spacer()
                Text(ratingLabel)
                    .font(.system(size: 9.5))
                    .foregroundStyle(C.muted)
            }
            Text(recipe.name)
                .font(.display(32))
                .foregroundStyle(C.ink)
            HStack(spacing: 7) {
                if let bean {
                    Button {
                        onOpenBean(bean.id)
                    } label: {
                        HStack(spacing: 3) {
                            Text(bean.name)
                            Image(systemName: "chevron.right").font(.system(size: 8, weight: .semibold))
                        }
                        .font(.system(size: 12.5))
                        .foregroundStyle(C.muted)
                    }
                    .buttonStyle(.plain)
                } else {
                    Text(recipe.beanName.isEmpty ? "未记豆子" : recipe.beanName)
                        .font(.system(size: 12.5))
                        .foregroundStyle(C.muted)
                }
                MetaDot()
                Text("用豆 \(recipe.beanGrams)g")
                    .font(.system(size: 12.5))
                    .foregroundStyle(C.muted)
            }
            HStack(spacing: 7) {
                TagPill(title: recipe.mouthfeel, dot: C.butterDeep)
                TagPill(title: recipe.sweetness, dot: C.roseDeep)
                if recipe.stirred { TagPill(title: "已搅拌", dot: C.limeDeep) }
            }
            .padding(.top, 3)
        }
    }

    /// 宣传卡片入口：随杯卡（1.2 新增）。
    private var promoCardEntry: some View {
        Button(action: onPromoCard) {
            HStack(spacing: 12) {
                ZStack {
                    Circle().fill(C.limeSoft).frame(width: 42, height: 42)
                    Image(systemName: "rectangle.portrait.on.rectangle.portrait")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(C.forest)
                }
                VStack(alignment: .leading, spacing: 3) {
                    Text("做一张宣传卡片")
                        .font(.system(size: 14.5, weight: .semibold))
                        .foregroundStyle(C.ink)
                    Text("随杯卡 · 可 AI 生成文案")
                        .font(.system(size: 10.5))
                        .foregroundStyle(C.muted)
                }
                Spacer()
                if recipe.promoCard != nil {
                    TagPill(title: "已保存", dot: C.lime)
                }
                Image(systemName: "chevron.right")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(C.mutedLight)
            }
            .padding(14)
            .background(C.card)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .stroke(C.lineSoft, lineWidth: 1)
            }
        }
        .buttonStyle(.plain)
    }

    private var statsCard: some View {
        HStack {
            RecipeStat(value: "\(recipe.totalGrams)g", label: "总量")
            Divider().frame(height: 28)
            RecipeStat(value: "\(recipe.layers.count)", label: "层数")
            Divider().frame(height: 28)
            RecipeStat(value: "\(recipe.beanGrams)g", label: "用豆")
            Divider().frame(height: 28)
            RecipeStat(value: String(format: "%.1f", Double(recipe.rating)), label: "评分")
        }
        .padding(.vertical, 15)
        .background(C.card)
        .clipShape(RoundedRectangle(cornerRadius: 23))
        .overlay {
            RoundedRectangle(cornerRadius: 23).stroke(C.lineSoft, lineWidth: 1)
        }
    }

    private func layerRow(_ index: Int) -> some View {
        let layer = recipe.layers[index]
        let percent = recipe.totalGrams == 0 ? 0 : Int(Double(layer.grams) / Double(recipe.totalGrams) * 100)
        return HStack(spacing: 10) {
            Circle().fill(Color(hex: layer.colorHex)).frame(width: 12, height: 12)
            VStack(alignment: .leading, spacing: 5) {
                Text(layer.name)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(C.ink)
                ProgressBar(
                    value: Double(layer.grams) / Double(max(recipe.totalGrams, 1)),
                    tint: Color(hex: layer.colorHex).opacity(0.72),
                    height: 3
                )
            }
            Spacer()
            Text("\(layer.grams)g")
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(C.inkSoft)
            Text("\(percent)%")
                .font(.mono(8))
                .foregroundStyle(C.mutedLight)
                .frame(width: 30, alignment: .trailing)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(C.card)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .overlay {
            RoundedRectangle(cornerRadius: 18).stroke(C.lineSoft, lineWidth: 1)
        }
    }

    private var notesCard: some View {
        VStack(alignment: .leading, spacing: 7) {
            Overline(text: "TASTING NOTE", color: C.forest)
            Text(recipe.notes)
                .font(.system(size: 14))
                .foregroundStyle(C.inkSoft)
                .lineSpacing(3)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(C.limeSoft.opacity(0.6))
        .clipShape(RoundedRectangle(cornerRadius: 23))
    }

    private func beanCard(_ bean: Bean) -> some View {
        Button {
            onOpenBean(bean.id)
        } label: {
            HStack(spacing: 12) {
                ZStack {
                    Circle().fill(Color(hex: bean.status.bgHex)).frame(width: 46, height: 46)
                    Text(String(bean.name.prefix(1)))
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(Color(hex: bean.status.inkHex))
                }
                VStack(alignment: .leading, spacing: 3) {
                    Text(bean.name)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(C.ink)
                        .lineLimit(1)
                    Text("\(bean.origin) · \(bean.roast.label) · \(bean.process)")
                        .font(.system(size: 10.5))
                        .foregroundStyle(C.muted)
                }
                Spacer()
                Text(formatGrams(bean.remainingGrams))
                    .font(.system(size: 11.5, weight: .medium))
                    .foregroundStyle(C.forest)
                Image(systemName: "chevron.right")
                    .font(.system(size: 9, weight: .semibold))
                    .foregroundStyle(C.mutedLight)
            }
            .padding(14)
            .background(C.card)
            .clipShape(RoundedRectangle(cornerRadius: 22))
            .overlay {
                RoundedRectangle(cornerRadius: 22).stroke(C.lineSoft, lineWidth: 1)
            }
        }
        .buttonStyle(.plain)
    }

    private var ratingLabel: String {
        switch recipe.rating {
        case 5: return "惊艳"
        case 4: return "很好喝"
        case 3: return "还不错"
        case 2: return "一般"
        default: return "再试试"
        }
    }

    private func shareRecipeFile() {
        if let url = store.makeRecipeShareFile(recipe) {
            shareURL = url
            showShare = true
        } else {
            errorMessage = "生成离线配方文件失败，请稍后再试。"
        }
    }
}
