import Foundation
import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var store: AppStore
    let onOpenBean: (String) -> Void
    let onOpenRecipe: (String) -> Void
    let onSeeBeans: () -> Void
    let onSeeMenu: () -> Void
    let onAddBean: () -> Void
    let onMake: () -> Void
    let onStats: () -> Void

    private var latest: Recipe? { store.recentRecipes.first }
    private var average: Double {
        guard !store.data.recipes.isEmpty else { return 0 }
        return Double(store.data.recipes.map(\.rating).reduce(0, +)) / Double(store.data.recipes.count)
    }

    var body: some View {
        AdaptivePage {
            VStack(alignment: .leading, spacing: 22) {
                header
                greeting
                quickPour
                statStrip
                recentBeans
                recentRecipes
                footer
            }
            .padding(.top, 12)
        }
    }

    private var header: some View {
        HStack(spacing: 11) {
            ZStack {
                Circle().fill(C.contrast).frame(width: 42, height: 42)
                Image(systemName: "cup.and.saucer.fill")
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundStyle(C.lime)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text("忆杯手记")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(C.ink)
                Text("YIBEI · VOL. 01")
                    .font(.mono(8))
                    .tracking(1)
                    .foregroundStyle(C.muted)
            }
            Spacer()
            HStack(spacing: 5) {
                Image(systemName: "leaf.fill")
                    .font(.system(size: 10))
                Text("\(store.data.beans.count) 支")
                    .font(.system(size: 12, weight: .medium))
            }
            .foregroundStyle(C.forest)
            .padding(.horizontal, 11)
            .frame(height: 30)
            .background(C.limeSoft)
            .clipShape(Capsule())

            RoundIconButton(systemName: "chart.bar.xaxis", accessibilityLabel: "查看统计", size: 40, action: onStats)
        }
    }

    private var greeting: some View {
        VStack(alignment: .leading, spacing: 7) {
            Text(greetingText)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(C.muted)
            Text("今天，喝点什么？")
                .font(.display(34))
                .foregroundStyle(C.ink)
                .lineSpacing(0)
            HStack(spacing: 7) {
                Circle().fill(C.lime).frame(width: 5, height: 5)
                Text("把最近喜欢的味道，再调一遍。")
                    .font(.system(size: 13))
                    .foregroundStyle(C.muted)
            }
        }
    }

    private var quickPour: some View {
        Button(action: onMake) {
            HStack(spacing: 10) {
                VStack(alignment: .leading, spacing: 10) {
                    Overline(text: "QUICK POUR", color: C.lime)
                    Spacer(minLength: 10)
                    Text("调一杯")
                        .font(.system(size: 28, weight: .semibold))
                        .foregroundStyle(.white)
                    Text(latest == nil ? "从空杯开始，倒进今天的味道" : "把最近这一杯，再做一次")
                        .font(.system(size: 12))
                        .foregroundStyle(.white.opacity(0.62))
                        .lineLimit(2)
                    Spacer(minLength: 8)
                    HStack(spacing: 5) {
                        Text("开始调配").font(.system(size: 13, weight: .semibold))
                        Image(systemName: "chevron.right").font(.system(size: 10, weight: .bold))
                    }
                    .foregroundStyle(C.onAccent)
                    .padding(.horizontal, 14)
                    .frame(height: 32)
                    .background(C.lime)
                    .clipShape(Capsule())
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                ZStack {
                    MiniCupView(layers: latest?.layers ?? [], mixed: latest?.stirred ?? false)
                        .frame(width: 120, height: 160)
                    if latest == nil {
                        Text("空杯").font(.mono(10)).foregroundStyle(.white.opacity(0.4))
                    }
                }
                .frame(width: 134, height: 165)
            }
            .padding(.leading, 22)
            .padding(.trailing, 16)
            .padding(.vertical, 18)
            .frame(minHeight: 186)
            .background(C.contrast)
            .clipShape(RoundedRectangle(cornerRadius: 30, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 30, style: .continuous)
                    .stroke(C.lime.opacity(0.18), lineWidth: 1)
            }
        }
        .buttonStyle(.plain)
    }

    private var statStrip: some View {
        HStack(spacing: 0) {
            HomeStat(value: "\(store.data.beans.count)", label: "支豆子")
            StatDivider()
            HomeStat(value: formatGrams(store.data.totalRemaining), label: "豆仓余量")
            StatDivider()
            HomeStat(value: "\(store.data.recipes.count)", label: "杯记录")
            StatDivider()
            HomeStat(value: average == 0 ? "—" : String(format: "%.1f", average), label: "平均评分")
        }
        .padding(.vertical, 15)
        .background(C.card.opacity(0.9))
        .clipShape(RoundedRectangle(cornerRadius: 25, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 25).stroke(C.lineSoft, lineWidth: 1)
        }
    }

    private var recentBeans: some View {
        VStack(alignment: .leading, spacing: 13) {
            SectionHeading(
                title: "最近在用",
                overline: "BEAN SHELF",
                actionTitle: store.data.beans.isEmpty ? nil : "全部豆子",
                action: onSeeBeans
            )
            if store.recentBeans.isEmpty {
                EmptyState(
                    title: "豆仓还是空的",
                    message: "先收一支正在喝的豆子，之后的每一杯都能找到来处。",
                    actionTitle: "添加豆子",
                    action: onAddBean
                )
                .background(C.card)
                .clipShape(RoundedRectangle(cornerRadius: 26))
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(Array(store.recentBeans.prefix(5))) { bean in
                            HomeBeanCard(bean: bean, action: { onOpenBean(bean.id) })
                        }
                        Button(action: onAddBean) {
                            VStack(spacing: 11) {
                                ZStack {
                                    Circle().fill(C.card).frame(width: 48, height: 48)
                                    Image(systemName: "plus")
                                        .font(.system(size: 18, weight: .medium))
                                        .foregroundStyle(C.ink)
                                }
                                Text("再收一支")
                                    .font(.system(size: 12, weight: .medium))
                                    .foregroundStyle(C.inkSoft)
                            }
                            .frame(width: 108, height: 248)
                            .background(C.paperSoft)
                            .clipShape(RoundedRectangle(cornerRadius: 27))
                            .overlay {
                                RoundedRectangle(cornerRadius: 27).stroke(C.line, lineWidth: 1)
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private var recentRecipes: some View {
        VStack(alignment: .leading, spacing: 13) {
            SectionHeading(
                title: "最近调过",
                overline: "RECENT CUPS",
                actionTitle: store.data.recipes.isEmpty ? nil : "翻菜单",
                action: onSeeMenu
            )
            if store.recentRecipes.isEmpty {
                EmptyState(
                    title: "还没有第一杯",
                    message: "选一支豆子，把今天的味道一层层倒进杯子里。",
                    actionTitle: "去调一杯",
                    action: onMake
                )
                .background(C.card)
                .clipShape(RoundedRectangle(cornerRadius: 26))
            } else {
                VStack(spacing: 10) {
                    ForEach(Array(store.recentRecipes.prefix(3))) { recipe in
                        HomeRecipeRow(recipe: recipe) {
                            onOpenRecipe(recipe.id)
                        }
                    }
                }
            }
        }
    }

    private var footer: some View {
        VStack(spacing: 6) {
            Rectangle().fill(C.mutedLight).frame(width: 48, height: 1)
            Text("每一杯，都是今天的注脚。")
                .font(.system(size: 12))
                .foregroundStyle(C.muted)
            Text("BREWED SLOWLY · \(monthTitle())")
                .font(.mono(8))
                .foregroundStyle(C.mutedLight)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 10)
    }

    private var greetingText: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 5...10: return "早上好"
        case 11...13: return "中午好"
        case 14...17: return "下午好"
        case 18...22: return "晚上好"
        default: return "夜深了"
        }
    }

    private func monthTitle() -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy · MM"
        return f.string(from: Date())
    }
}

private struct HomeStat: View {
    let value: String
    let label: String

    var body: some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.mono(17, weight: .medium))
                .foregroundStyle(C.ink)
                .lineLimit(1)
            Text(label)
                .font(.system(size: 9.5))
                .foregroundStyle(C.muted)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct StatDivider: View {
    var body: some View {
        Rectangle().fill(C.lineSoft).frame(width: 1, height: 28)
    }
}

private struct HomeBeanCard: View {
    let bean: Bean
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 0) {
                BeanArtwork(bean: bean)
                    .frame(height: 132)
                    .padding(8)
                VStack(alignment: .leading, spacing: 4) {
                    Text(bean.name)
                        .font(.system(size: 14.5, weight: .medium))
                        .foregroundStyle(C.ink)
                        .lineLimit(1)
                    Text("\(bean.roaster.isEmpty ? bean.process : bean.roaster) · \(bean.roast.short)")
                        .font(.system(size: 11.5))
                        .foregroundStyle(C.muted)
                        .lineLimit(1)
                    Spacer(minLength: 8)
                    HStack(spacing: 5) {
                        Text(formatGrams(bean.remainingGrams))
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(C.forest)
                        Text("/ \(formatGrams(bean.totalGrams))")
                            .font(.mono(9))
                            .foregroundStyle(C.muted)
                        Spacer()
                        Text("第\(bean.ageDays)天")
                            .font(.mono(9))
                            .foregroundStyle(C.muted)
                    }
                    ProgressBar(value: bean.remainingRatio, tint: Color(hex: bean.status.inkHex).opacity(0.72))
                        .padding(.top, 3)
                }
                .padding(.horizontal, 14)
                .padding(.bottom, 14)
                .padding(.top, 3)
            }
            .frame(width: 210, height: 248, alignment: .top)
            .background(C.card)
            .clipShape(RoundedRectangle(cornerRadius: 27))
            .overlay {
                RoundedRectangle(cornerRadius: 27).stroke(C.lineSoft, lineWidth: 1)
            }
        }
        .buttonStyle(.plain)
    }
}

struct ProgressBar: View {
    let value: Double
    var tint: Color = C.lime
    var height: CGFloat = 4

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(C.lineSoft)
                Capsule()
                    .fill(tint)
                    .frame(width: max(geo.size.width * CGFloat(min(max(value, 0), 1)), value > 0 ? CGFloat(4) : 0))
            }
        }
        .frame(height: height)
    }
}

private struct HomeRecipeRow: View {
    let recipe: Recipe
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 13) {
                ZStack {
                    RoundedRectangle(cornerRadius: 19, style: .continuous).fill(C.paperSoft)
                    MiniCupView(layers: recipe.layers, mixed: recipe.stirred)
                        .padding(9)
                }
                .frame(width: 76, height: 90)

                VStack(alignment: .leading, spacing: 3) {
                    HStack {
                        Overline(text: recipe.dateTitle)
                        Spacer()
                        BeanRating(rating: recipe.rating, size: 13)
                    }
                    Text(recipe.name)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(C.ink)
                        .lineLimit(1)
                    HStack(spacing: 6) {
                        Text(recipe.beanName.isEmpty ? "未记豆子" : recipe.beanName)
                            .lineLimit(1)
                        MetaDot()
                        Text("\(recipe.totalGrams)g · \(recipe.layers.count) 层")
                    }
                    .font(.system(size: 11))
                    .foregroundStyle(C.muted)
                    if !recipe.notes.isEmpty {
                        Text(recipe.notes)
                            .font(.system(size: 10.8))
                            .foregroundStyle(C.inkSoft.opacity(0.7))
                            .lineLimit(1)
                    }
                }
                Spacer(minLength: 2)
                Image(systemName: "chevron.right")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(C.mutedLight)
            }
            .padding(12)
            .background(C.card)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 24).stroke(C.lineSoft, lineWidth: 1)
            }
        }
        .buttonStyle(.plain)
    }
}
