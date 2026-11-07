import Foundation
import SwiftUI

struct BeanDetailView: View {
    @EnvironmentObject private var store: AppStore
    @Environment(\.dismiss) private var dismiss

    let bean: Bean
    let onEdit: () -> Void
    let onMake: () -> Void
    let onOpenRecipe: (String) -> Void

    @State private var confirmDelete = false

    private var related: [Recipe] {
        store.data.recipes.filter { $0.beanId == bean.id }.sorted { $0.createdAt > $1.createdAt }
    }

    var body: some View {
        VStack(spacing: 0) {
            topBar
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 18) {
                    BeanArtwork(bean: bean)
                        .frame(height: 300)
                    titleBlock
                    flavorTags
                    bestWindow
                    details
                    if !bean.note.isEmpty { noteBlock }
                    recentRecipes
                    deleteButton
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
                    OutlineActionButton(title: "编辑", action: onEdit)
                    PrimaryActionButton(title: "调一杯", action: onMake)
                }
            }
        }
        .alert("删除这支豆子？", isPresented: $confirmDelete) {
            Button("删除", role: .destructive) {
                store.deleteBean(bean)
                dismiss()
            }
            Button("再想想", role: .cancel) {}
        } message: {
            Text("豆子会从豆仓移除；已经调过的配方还会留在菜单里。")
        }
        .toolbar(.hidden, for: .navigationBar)
    }

    private var topBar: some View {
        HStack {
            RoundIconButton(systemName: "chevron.left", accessibilityLabel: "返回") {
                dismiss()
            }
            Spacer()
            RoundIconButton(systemName: "pencil", accessibilityLabel: "编辑豆子", action: onEdit)
        }
        .padding(.horizontal, 20)
        .padding(.top, 8)
        .padding(.bottom, 10)
    }

    private var titleBlock: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(bean.name)
                .font(.display(34))
                .foregroundStyle(C.ink)
            HStack(spacing: 7) {
                Text("\(originFlag(bean.origin)) \(bean.displayOrigin)")
                MetaDot()
                Text(bean.roaster.isEmpty ? "未记录烘焙商" : bean.roaster)
            }
            .font(.system(size: 13))
            .foregroundStyle(C.muted)
        }
    }

    private var flavorTags: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 7) {
                if bean.flavors.isEmpty {
                    TagPill(title: bean.process, dot: C.lime)
                } else {
                    ForEach(bean.flavors, id: \.self) { flavor in
                        TagPill(title: flavor, dot: C.lime)
                    }
                }
            }
        }
    }

    private var bestWindow: some View {
        BrewCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Overline(text: "BEST WINDOW")
                        Text(bean.status.label)
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(Color(hex: bean.status.inkHex))
                    }
                    Spacer()
                    VStack(alignment: .trailing, spacing: 2) {
                        Text(formatGrams(bean.remainingGrams))
                            .font(.mono(30, weight: .medium))
                            .foregroundStyle(C.ink)
                        Text("余量 / \(formatGrams(bean.totalGrams))")
                            .font(.system(size: 9.5))
                            .foregroundStyle(C.muted)
                    }
                }
                ProgressBar(value: bean.remainingRatio, tint: Color(hex: bean.status.inkHex).opacity(0.75), height: 6)
                HStack {
                    Text(bean.peakHint)
                    Spacer()
                    Text(bean.storage.label)
                }
                .font(.system(size: 11))
                .foregroundStyle(C.muted)
            }
            .padding(18)
        }
    }

    private var details: some View {
        BrewCard {
            VStack(alignment: .leading, spacing: 14) {
                Overline(text: "BEAN DETAILS")
                LazyVGrid(
                    columns: [GridItem(.flexible()), GridItem(.flexible())],
                    alignment: .leading,
                    spacing: 16
                ) {
                    DetailCell(label: "烘焙度", value: bean.roast.label)
                    DetailCell(label: "处理法", value: bean.process)
                    DetailCell(label: "产区", value: bean.region.isEmpty ? "未记录" : bean.region)
                    DetailCell(label: "烘焙日", value: bean.roastDate.date.dayLabel)
                    DetailCell(label: "总克重", value: formatGrams(bean.totalGrams))
                    DetailCell(label: "剩余", value: formatGrams(bean.remainingGrams))
                }
            }
            .padding(18)
        }
    }

    private var noteBlock: some View {
        HStack(alignment: .top, spacing: 12) {
            Capsule().fill(C.lime).frame(width: 3, height: 54)
            VStack(alignment: .leading, spacing: 5) {
                Overline(text: "TASTING NOTE")
                Text(bean.note)
                    .font(.system(size: 14))
                    .foregroundStyle(C.inkSoft)
                    .lineSpacing(3)
            }
        }
    }

    private var recentRecipes: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeading(title: "用这支豆调过", overline: "CUPS WITH THIS BEAN")
            if related.isEmpty {
                EmptyState(
                    title: "还没有用过这支豆",
                    message: "去调一杯，它会自动记到这支豆的名下。",
                    actionTitle: "用这支豆调一杯",
                    action: onMake
                )
                .background(C.card)
                .clipShape(RoundedRectangle(cornerRadius: 26))
            } else {
                ForEach(related) { recipe in
                    Button {
                        onOpenRecipe(recipe.id)
                    } label: {
                        HStack(spacing: 12) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 16).fill(C.paperSoft)
                                MiniCupView(layers: recipe.layers, mixed: recipe.stirred)
                                    .padding(7)
                            }
                            .frame(width: 58, height: 70)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(recipe.name)
                                    .font(.system(size: 14, weight: .medium))
                                    .foregroundStyle(C.ink)
                                    .lineLimit(1)
                                Text("\(recipe.dateTitle) · \(recipe.totalGrams)g · \(recipe.layers.count) 层")
                                    .font(.system(size: 10.5))
                                    .foregroundStyle(C.muted)
                            }
                            Spacer()
                            BeanRating(rating: recipe.rating, size: 13)
                        }
                        .padding(12)
                        .background(C.card)
                        .clipShape(RoundedRectangle(cornerRadius: 22))
                        .overlay {
                            RoundedRectangle(cornerRadius: 22).stroke(C.lineSoft, lineWidth: 1)
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var deleteButton: some View {
        Button {
            confirmDelete = true
        } label: {
            Text("删除这支豆子")
                .font(.system(size: 11.5))
                .foregroundStyle(C.muted)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
        }
        .buttonStyle(.plain)
    }
}

private struct DetailCell: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.mono(9))
                .foregroundStyle(C.muted)
            Text(value)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(C.ink)
                .lineLimit(1)
        }
    }
}
