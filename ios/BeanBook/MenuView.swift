import Foundation
import SwiftUI

private enum MenuFilter: String, CaseIterable, Identifiable, Equatable {
    case all, month, best
    var id: String { rawValue }
    var label: String {
        switch self {
        case .all: return "全部"
        case .month: return "本月"
        case .best: return "高分"
        }
    }
}

struct MenuView: View {
    @EnvironmentObject private var store: AppStore
    let onOpenSettings: () -> Void
    let onOpenRecipe: (String) -> Void
    let onOpenPromoCard: (String) -> Void
    let onMake: () -> Void

    @State private var filter: MenuFilter = .all

    private var recipes: [Recipe] {
        let calendar = Calendar.current
        return store.recentRecipes.filter { recipe in
            switch filter {
            case .all: return true
            case .month:
                return calendar.isDate(recipe.createdAt.date, equalTo: Date(), toGranularity: .month)
            case .best:
                return recipe.rating >= 4
            }
        }
    }

    private var grouped: [MenuDay] {
        let calendar = Calendar.current
        let groups = Dictionary(grouping: recipes) { calendar.startOfDay(for: $0.createdAt.date) }
        return groups.sorted { $0.key > $1.key }.map {
            MenuDay(id: $0.key, recipes: $0.value.sorted { $0.createdAt > $1.createdAt })
        }
    }

    private var average: Double {
        guard !store.data.recipes.isEmpty else { return 0 }
        return Double(store.data.recipes.map(\.rating).reduce(0, +)) / Double(store.data.recipes.count)
    }

    private var monthCount: Int {
        let calendar = Calendar.current
        return store.data.recipes.filter {
            calendar.isDate($0.createdAt.date, equalTo: Date(), toGranularity: .month)
        }.count
    }

    var body: some View {
        AdaptivePage(maxWidth: 920) {
            VStack(alignment: .leading, spacing: 16) {
                header
                summary
                dataCard
                SectionHeading(title: "配方目录", overline: "RECIPE BOOK")
                filters
                if recipes.isEmpty {
                    EmptyState(
                        title: store.data.recipes.isEmpty ? "菜单还是空的" : "这个筛选下还没有",
                        message: store.data.recipes.isEmpty
                            ? "调好的每一杯都会自动收进这里，像一本慢慢写厚的咖啡日记。"
                            : "换一个筛选条件，或者去调一杯新的。",
                        actionTitle: "去调一杯",
                        action: onMake
                    )
                    .background(C.card)
                    .clipShape(RoundedRectangle(cornerRadius: 28))
                } else {
                    ForEach(grouped) { group in
                        dayHeader(group.id, count: group.recipes.count)
                        ForEach(group.recipes) { recipe in
                            HStack(spacing: 8) {
                                Button {
                                    onOpenRecipe(recipe.id)
                                } label: {
                                    MenuRecipeCard(recipe: recipe)
                                }
                                .buttonStyle(.plain)
                                promoCardButton(recipe)
                            }
                        }
                    }
                    footer
                }
            }
            .padding(.top, 12)
        }
    }

    private var header: some View {
        HStack(alignment: .bottom) {
            VStack(alignment: .leading, spacing: 5) {
                Overline(text: "THE MENU")
                Text("菜单")
                    .font(.display(34))
                    .foregroundStyle(C.ink)
                Text("配方目录、设置与数据备份，还有离线分享。")
                    .font(.system(size: 13))
                    .foregroundStyle(C.muted)
            }
            Spacer()
            Text("\(store.data.recipes.count) 杯")
                .font(.system(size: 12))
                .foregroundStyle(C.muted)
        }
    }

    private var summary: some View {
        BrewCard {
            VStack(alignment: .leading, spacing: 13) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 5) {
                        Overline(text: "EDITION · \(monthTitle())", color: C.forest)
                        Text("一共 \(store.data.recipes.count) 杯")
                            .font(.system(size: 25, weight: .semibold))
                            .foregroundStyle(C.ink)
                        Text(store.data.recipes.isEmpty ? "等第一杯落笔" : "每一杯都比上一杯更懂你一点")
                            .font(.system(size: 11.5))
                            .foregroundStyle(C.muted)
                    }
                    Spacer()
                    ZStack {
                        Circle().fill(C.limeSoft).frame(width: 52, height: 52)
                        Image(systemName: "cup.and.saucer")
                            .font(.system(size: 23, weight: .medium))
                            .foregroundStyle(C.forest)
                    }
                }
                HStack {
                    SummaryCell(value: average == 0 ? "—" : String(format: "%.1f", average), label: "平均评分")
                    SummaryDivider()
                    SummaryCell(value: "\(Set(store.data.recipes.compactMap(\.beanId)).count)", label: "用过的豆子")
                    SummaryDivider()
                    SummaryCell(value: "\(monthCount)", label: "本月手记")
                }
            }
            .padding(18)
        }
    }

    private var dataCard: some View {
        Button(action: onOpenSettings) {
            HStack(spacing: 12) {
                ZStack {
                    Circle().fill(C.limeSoft).frame(width: 44, height: 44)
                    Image(systemName: "gearshape")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(C.forest)
                }
                VStack(alignment: .leading, spacing: 3) {
                    Text("设置与数据")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(C.ink)
                    Text("AI API · 黑暗模式 · 数据备份 · 关于")
                        .font(.system(size: 11.5))
                        .foregroundStyle(C.muted)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(C.muted)
            }
            .padding(18)
            .background(C.card)
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .overlay {
                RoundedRectangle(cornerRadius: 24).stroke(C.lineSoft, lineWidth: 1)
            }
        }
        .buttonStyle(.plain)
    }

    /// 菜单里每杯配方右侧的「宣传卡片」入口（1.2 新增）。
    private func promoCardButton(_ recipe: Recipe) -> some View {
        Button {
            onOpenPromoCard(recipe.id)
        } label: {
            VStack(spacing: 4) {
                Image(systemName: "rectangle.portrait.on.rectangle.portrait")
                    .font(.system(size: 15, weight: .medium))
                Text("卡片")
                    .font(.system(size: 9, weight: .medium))
            }
            .foregroundStyle(C.forest)
            .frame(width: 48)
            .frame(maxHeight: .infinity)
            .background(C.limeSoft.opacity(0.85))
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(C.lime.opacity(0.35), lineWidth: 1)
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel("制作宣传卡片")
    }

    private var filters: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(MenuFilter.allCases) { item in
                    SelectableChip(title: item.label, selected: filter == item) {
                        filter = item
                    }
                }
            }
        }
    }

    private func dayHeader(_ date: Date, count: Int) -> some View {
        HStack(spacing: 7) {
            Text(dayTitle(date))
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(C.inkSoft)
            Text("\(count) 杯")
                .font(.mono(9))
                .foregroundStyle(C.mutedLight)
            Spacer()
            Rectangle().fill(C.line).frame(width: 20, height: 1)
        }
        .padding(.top, 8)
    }

    private var footer: some View {
        VStack(spacing: 5) {
            Rectangle().fill(C.line).frame(width: 36, height: 1)
            Text("一杯一杯，慢慢写厚。")
                .font(.system(size: 11))
                .foregroundStyle(C.muted)
            Text("YIBEI · KEEP DRINKING SLOWLY")
                .font(.mono(8))
                .foregroundStyle(C.mutedLight)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 24)
    }

    private func monthTitle() -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy · MM"
        return f.string(from: Date())
    }

    private func dayTitle(_ date: Date) -> String {
        let calendar = Calendar.current
        if calendar.isDateInToday(date) { return "今天" }
        if calendar.isDateInYesterday(date) { return "昨天" }
        let f = DateFormatter()
        f.locale = Locale(identifier: "zh_CN")
        f.dateFormat = "M月d日 EEE"
        return f.string(from: date)
    }
}

private struct SummaryCell: View {
    let value: String
    let label: String
    var body: some View {
        VStack(spacing: 2) {
            Text(value).font(.mono(17, weight: .medium)).foregroundStyle(C.ink)
            Text(label).font(.system(size: 9.5)).foregroundStyle(C.muted)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct SummaryDivider: View {
    var body: some View { Rectangle().fill(C.lineSoft).frame(width: 1, height: 27) }
}

private struct MenuRecipeCard: View {
    let recipe: Recipe

    var body: some View {
        HStack(spacing: 13) {
            ZStack {
                RoundedRectangle(cornerRadius: 21).fill(C.paperSoft)
                MiniCupView(layers: recipe.layers, mixed: recipe.stirred)
                    .padding(10)
            }
            .frame(width: 78, height: 92)

            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 7) {
                    Overline(text: recipe.dateTitle)
                    Spacer()
                    if let price = recipe.price {
                        Text("¥\(price)")
                            .font(.mono(11, weight: .medium))
                            .foregroundStyle(C.forest)
                    }
                    BeanRating(rating: recipe.rating, size: 13)
                }
                Text(recipe.name)
                    .font(.system(size: 15.5, weight: .semibold))
                    .foregroundStyle(C.ink)
                    .lineLimit(1)
                HStack(spacing: 6) {
                    Text(recipe.beanName.isEmpty ? "未记豆子" : recipe.beanName).lineLimit(1)
                    MetaDot()
                    Text("\(recipe.totalGrams)g · \(recipe.layers.count) 层")
                }
                .font(.system(size: 10.5))
                .foregroundStyle(C.muted)
                if !recipe.notes.isEmpty {
                    Text(recipe.notes)
                        .font(.system(size: 10.5))
                        .foregroundStyle(C.inkSoft.opacity(0.7))
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 2)
            Image(systemName: "chevron.right")
                .font(.system(size: 10, weight: .semibold))
                .foregroundStyle(C.mutedLight)
        }
        .padding(12)
        .background(C.card)
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .overlay {
            RoundedRectangle(cornerRadius: 24).stroke(C.lineSoft, lineWidth: 1)
        }
    }
}


private struct MenuDay: Identifiable {
    let id: Date
    let recipes: [Recipe]
}
