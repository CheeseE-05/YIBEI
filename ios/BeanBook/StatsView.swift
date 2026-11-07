import Foundation
import SwiftUI

private enum StatRange: String, CaseIterable, Identifiable, Equatable {
    case last30, last7, all
    var id: String { rawValue }
    var label: String {
        switch self {
        case .last30: return "近 30 天"
        case .last7: return "近 7 天"
        case .all: return "全部"
        }
    }
    var days: Int? {
        switch self {
        case .last30: return 30
        case .last7: return 7
        case .all: return nil
        }
    }
}

private struct FlavorProfile {
    var acid: Double = 0
    var sweet: Double = 0
    var body: Double = 0
    var milk: Double = 0
    var fresh: Double = 0
}

private struct BeanRank: Identifiable {
    let id: String
    let name: String
    let cups: Int
    let grams: Int
}

private struct IngredientRank: Identifiable {
    let id: String
    let name: String
    let colorHex: String
    let count: Int
}

struct StatsView: View {
    @EnvironmentObject private var store: AppStore
    let onMake: () -> Void
    let onSeeBeans: () -> Void
    let onOpenSettings: () -> Void

    @State private var range: StatRange = .last30

    private var scoped: [Recipe] {
        guard let days = range.days else { return store.data.recipes }
        let cutoff = nowMillis() - Int64(days) * 86_400_000
        return store.data.recipes.filter { $0.createdAt >= cutoff }
    }

    private var average: Double {
        guard !scoped.isEmpty else { return 0 }
        return Double(scoped.map(\.rating).reduce(0, +)) / Double(scoped.count)
    }

    private var beanUsed: Int { scoped.reduce(0) { $0 + $1.beanGrams } }
    private var beanCount: Int { Set(scoped.compactMap(\.beanId)).count }
    private var streak: Int { brewStreak(store.data.recipes) }
    private var monthCount: Int {
        let calendar = Calendar.current
        return store.data.recipes.filter {
            calendar.isDate($0.createdAt.date, equalTo: Date(), toGranularity: .month)
        }.count
    }

    private var previousCount: Int? {
        guard let days = range.days else { return nil }
        let now = nowMillis()
        let currentStart = now - Int64(days) * 86_400_000
        let previousStart = now - Int64(days * 2) * 86_400_000
        return store.data.recipes.filter { $0.createdAt >= previousStart && $0.createdAt < currentStart }.count
    }

    private var profile: FlavorProfile { flavourProfile(scoped) }
    private var beanRanks: [BeanRank] { beanRanking(scoped) }
    private var ingredientRanks: [IngredientRank] { ingredientRanking(scoped) }

    var body: some View {
        AdaptivePage(maxWidth: 920, onScrollProxy: { proxy in
            runDebugBottomScrollIfNeeded(proxy)
        }) {
            VStack(alignment: .leading, spacing: 15) {
                header
                chips
                hero
                SectionHeading(title: "冲煮节奏", overline: "BREW RHYTHM")
                activity
                SectionHeading(title: "风味指纹", overline: "TASTE PROFILE")
                flavorCard
                SectionHeading(title: "喝得最多的豆", overline: "MOST BREWED")
                beanRankCard
                SectionHeading(title: "烘焙光谱", overline: "ROAST SPECTRUM")
                roastCard
                SectionHeading(title: "常驻材料", overline: "OFTEN POURED")
                ingredientCard
                SectionHeading(title: "豆仓现状", overline: "INVENTORY")
                inventory
                footer
                settingsEntry
            }
            .padding(.top, 12)
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 5) {
            Overline(text: "YOUR COFFEE DATA")
            Text("统计")
                .font(.display(34))
                .foregroundStyle(C.ink)
            Text("把每一杯，慢慢变成自己的口味地图。")
                .font(.system(size: 13))
                .foregroundStyle(C.muted)
        }
    }

    private var chips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(StatRange.allCases) { item in
                    SelectableChip(title: item.label, selected: range == item) {
                        range = item
                    }
                }
            }
        }
    }

    private var hero: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Overline(text: "TOTAL BREWS · \(range.label)", color: C.lime)
                Spacer()
                ZStack {
                    Circle().fill(.white.opacity(0.08)).frame(width: 44, height: 44)
                    Image(systemName: "cup.and.saucer")
                        .font(.system(size: 19, weight: .medium))
                        .foregroundStyle(C.lime)
                }
            }
            HStack(alignment: .bottom, spacing: 5) {
                Text("\(scoped.count)")
                    .font(.mono(54, weight: .medium))
                    .foregroundStyle(.white)
                Text("杯")
                    .font(.system(size: 12))
                    .foregroundStyle(.white.opacity(0.55))
                    .padding(.bottom, 12)
                Spacer()
                let deltaText: String = {
                    guard let previous = previousCount else { return "本机记录" }
                    let delta = scoped.count - previous
                    if delta > 0 { return "+\(delta) 杯" }
                    if delta < 0 { return "\(delta) 杯" }
                    return "持平"
                }()
                TagPill(title: deltaText, background: .white.opacity(0.09), foreground: C.lime, dot: previousCount == nil ? nil : C.lime)
            }
            Text(scoped.isEmpty ? "杯里还空着，去倒进今天的味道。" : "和上一个周期比，慢慢找到自己的节奏。")
                .font(.system(size: 11))
                .foregroundStyle(.white.opacity(0.48))
            HorizontalRule(color: .white.opacity(0.08))
            HStack {
                DarkStat(value: average == 0 ? "—" : String(format: "%.1f", average), label: "平均评分")
                DarkStat(value: "\(beanCount)", label: "用过的豆")
                DarkStat(value: "\(beanUsed)g", label: "累计用豆")
                DarkStat(value: streak == 0 ? "—" : "\(streak)", label: "连续天数")
            }
            if scoped.isEmpty {
                PrimaryActionButton(title: "去调第一杯", action: onMake)
            }
        }
        .padding(20)
        .background(C.contrast)
        .clipShape(RoundedRectangle(cornerRadius: 29))
    }

    private var activity: some View {
        let calendar = Calendar.current
        let days = (0..<7).map { calendar.startOfDay(for: Date().addingTimeInterval(-Double(6 - $0) * 86400)) }
        let counts = days.map { day in store.data.recipes.filter { calendar.isDate($0.createdAt.date, inSameDayAs: day) }.count }
        let maxCount = max(counts.max() ?? 1, 1)
        return BrewCard {
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("最近 7 天").font(.system(size: 16, weight: .medium)).foregroundStyle(C.ink)
                        Text("一周的咖啡因轨迹").font(.system(size: 11)).foregroundStyle(C.muted)
                    }
                    Spacer()
                    Text("\(counts.reduce(0, +)) 杯")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(C.forest)
                }
                HStack(alignment: .bottom, spacing: 7) {
                    ForEach(days.indices, id: \.self) { index in
                        VStack(spacing: 4) {
                            Text(counts[index] == 0 ? "·" : "\(counts[index])")
                                .font(.mono(8))
                                .foregroundStyle(counts[index] == 0 ? C.mutedLight : C.inkSoft)
                            ZStack(alignment: .bottom) {
                                Color.clear.frame(height: 64)
                                Capsule()
                                    .fill(counts[index] == 0 ? C.lineSoft : (calendar.isDateInToday(days[index]) ? C.lime : C.lime.opacity(0.46)))
                                    .frame(
                                        width: calendar.isDateInToday(days[index]) ? 24 : 20,
                                        height: counts[index] == 0 ? CGFloat(5) : CGFloat(8) + CGFloat(counts[index]) / CGFloat(maxCount) * 52
                                    )
                            }
                            Text(shortWeekday(days[index]))
                                .font(.system(size: 9))
                                .foregroundStyle(calendar.isDateInToday(days[index]) ? C.ink : C.muted)
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
            }
            .padding(18)
        }
    }

    private var flavorCard: some View {
        BrewCard {
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("这一阵的味道").font(.system(size: 16, weight: .medium)).foregroundStyle(C.ink)
                        Text(scoped.isEmpty ? "有记录后，会慢慢长成你的口味轮廓。" : "参考豆子、材料和比例，不评判好坏。")
                            .font(.system(size: 11))
                            .foregroundStyle(C.muted)
                    }
                    Spacer()
                    Text("5 分制").font(.mono(8)).foregroundStyle(C.mutedLight)
                }
                HStack(spacing: 10) {
                    FlavorBubble(title: "酸度", value: profile.acid, note: acidNote(profile.acid), color: C.limeSoft)
                    FlavorBubble(title: "甜度", value: profile.sweet, note: sweetNote(profile.sweet), color: C.butter)
                }
                FlavorLine(title: "醇厚度", value: profile.body, color: C.brown)
                FlavorLine(title: "奶感", value: profile.milk, color: C.butterDeep)
                FlavorLine(title: "清爽感", value: profile.fresh, color: C.skyDeep)
            }
            .padding(18)
        }
    }

    private var beanRankCard: some View {
        BrewCard {
            VStack(alignment: .leading, spacing: 13) {
                if beanRanks.isEmpty {
                    Text(scoped.isEmpty ? "还没有记录，先去调一杯。" : "这些杯子还没有记豆子。")
                        .font(.system(size: 12.5))
                        .foregroundStyle(C.muted)
                } else {
                    let maxCups = max(beanRanks.map(\.cups).max() ?? 1, 1)
                    ForEach(0..<min(beanRanks.count, 4), id: \.self) { index in
                        let rank = beanRanks[index]
                        HStack(spacing: 9) {
                            Text(String(format: "%02d", index + 1))
                                .font(.mono(9))
                                .foregroundStyle(index == 0 ? C.limeDeep : C.mutedLight)
                            VStack(alignment: .leading, spacing: 5) {
                                Text(rank.name)
                                    .font(.system(size: 12.5, weight: .medium))
                                    .foregroundStyle(C.ink)
                                    .lineLimit(1)
                                ProgressBar(
                                    value: Double(rank.cups) / Double(maxCups),
                                    tint: index == 0 ? C.lime : C.ink.opacity(0.28)
                                )
                            }
                            VStack(alignment: .trailing, spacing: 2) {
                                Text("\(rank.cups) 杯").font(.mono(9)).foregroundStyle(C.inkSoft)
                                Text("\(rank.grams)g 用豆").font(.mono(8)).foregroundStyle(C.mutedLight)
                            }
                        }
                    }
                }
            }
            .padding(18)
        }
    }

    private var roastCard: some View {
        let counts = Roast.allCases.compactMap { roast -> (Roast, Int)? in
            let count = scoped.filter { recipe in
                guard let beanId = recipe.beanId, let bean = store.bean(beanId) else { return false }
                return bean.roast == roast
            }.count
            return count > 0 ? (roast, count) : nil
        }
        return BrewCard {
            VStack(alignment: .leading, spacing: 12) {
                if counts.isEmpty {
                    Text("还没有足够的烘焙数据").font(.system(size: 14, weight: .medium)).foregroundStyle(C.ink)
                    Text("在配方里标上豆子，烘焙光谱就会慢慢长出来。")
                        .font(.system(size: 11)).foregroundStyle(C.muted)
                } else {
                    HStack(spacing: 18) {
                        DonutChart(entries: counts.map { ($0.0.toneHex, $0.1) })
                            .frame(width: 118, height: 118)
                        VStack(alignment: .leading, spacing: 8) {
                            ForEach(0..<counts.count, id: \.self) { index in
                                let item = counts[index]
                                HStack(spacing: 7) {
                                    Circle().fill(Color(hex: item.0.toneHex)).frame(width: 8, height: 8)
                                    Text(item.0.label)
                                        .font(.system(size: 11))
                                        .foregroundStyle(C.inkSoft)
                                    Spacer()
                                    Text("\(item.1) 杯")
                                        .font(.mono(8))
                                        .foregroundStyle(C.muted)
                                }
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .padding(18)
        }
    }

    private var ingredientCard: some View {
        BrewCard {
            VStack(alignment: .leading, spacing: 12) {
                if ingredientRanks.isEmpty {
                    Text("还没有倒入过材料。").font(.system(size: 12.5)).foregroundStyle(C.muted)
                } else {
                    HStack {
                        VStack(alignment: .leading, spacing: 3) {
                            Text("最常出现的是 \(ingredientRanks[0].name)")
                                .font(.system(size: 15, weight: .medium))
                                .foregroundStyle(C.ink)
                            Text("按出现的杯数排序，不是克数。")
                                .font(.system(size: 10.5))
                                .foregroundStyle(C.muted)
                        }
                        Spacer()
                        Text("\(ingredientRanks[0].count) 次")
                            .font(.mono(9))
                            .foregroundStyle(C.forest)
                    }
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 100), spacing: 7)], alignment: .leading, spacing: 7) {
                        ForEach(ingredientRanks.prefix(7)) { rank in
                            TagPill(
                                title: "\(rank.name) ×\(rank.count)",
                                background: C.paperSoft,
                                foreground: C.inkSoft,
                                dot: Color(hex: rank.colorHex)
                            )
                        }
                    }
                }
            }
            .padding(18)
        }
    }

    private var inventory: some View {
        let total = store.data.beans.reduce(0) { $0 + $1.totalGrams }
        let remaining = store.data.totalRemaining
        let peak = store.data.beans.filter { $0.status == .peak }.count
        return BrewCard {
            VStack(alignment: .leading, spacing: 11) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(formatGrams(remaining))
                            .font(.mono(27, weight: .medium))
                            .foregroundStyle(C.ink)
                        Text("豆仓总余量 / \(formatGrams(total))")
                            .font(.system(size: 9.5))
                            .foregroundStyle(C.muted)
                    }
                    Spacer()
                    TagPill(
                        title: peak == 0 ? "暂无最佳赏味" : "\(peak) 支正当时",
                        background: C.limeSoft,
                        foreground: C.forest,
                        dot: C.lime
                    )
                }
                ProgressBar(value: total == 0 ? 0 : remaining / total, tint: C.lime, height: 6)
                HStack {
                    Text("\(store.data.beans.count) 支在架")
                        .font(.system(size: 11))
                        .foregroundStyle(C.muted)
                    Spacer()
                    Button(action: onSeeBeans) {
                        HStack(spacing: 3) {
                            Text("去豆仓").font(.system(size: 11.5, weight: .medium))
                            Image(systemName: "chevron.right").font(.system(size: 9, weight: .semibold))
                        }
                        .foregroundStyle(C.inkSoft)
                        .padding(.horizontal, 12)
                        .frame(height: 30)
                        .background(C.paperSoft)
                        .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(18)
        }
    }

    private var footer: some View {
        VStack(spacing: 5) {
            Text(scoped.isEmpty ? "从第一杯开始，慢慢有自己的答案。" : "本月 \(monthCount) 杯 · 数据只留在这台设备上")
                .font(.system(size: 11))
                .foregroundStyle(C.muted)
                .multilineTextAlignment(.center)
            Text("DRINK SLOWLY · NOTICE MORE")
                .font(.mono(8))
                .foregroundStyle(C.mutedLight)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
    }

    /// 统计页最底部的设置入口（1.2 新增：AI API / 黑暗模式 / 数据备份 / 关于）。
    private var settingsEntry: some View {
        Button(action: onOpenSettings) {
            HStack(spacing: 13) {
                ZStack {
                    Circle().fill(C.limeSoft).frame(width: 44, height: 44)
                    Image(systemName: "gearshape")
                        .font(.system(size: 17, weight: .medium))
                        .foregroundStyle(C.forest)
                }
                VStack(alignment: .leading, spacing: 3) {
                    Text("设置")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(C.ink)
                    Text("外观 · AI API · 数据备份 · 关于")
                        .font(.system(size: 11.5))
                        .foregroundStyle(C.muted)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(C.muted)
            }
            .padding(16)
            .background(C.card)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .stroke(C.lineSoft, lineWidth: 1)
            }
        }
        .buttonStyle(.plain)
        .padding(.top, 2)
        .accessibilityIdentifier("settings_entry")
        .id("stats_settings")
    }

    /// DEBUG 截图辅助：启动后自动滚到统计页底部的「设置」入口。
    private func runDebugBottomScrollIfNeeded(_ proxy: ScrollViewProxy) {
        #if DEBUG
        guard ProcessInfo.processInfo.arguments.contains("-uiTestStatsBottom") else { return }
        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 1_200_000_000)
            withAnimation(.easeOut(duration: 0.3)) {
                proxy.scrollTo("stats_settings", anchor: .bottom)
            }
        }
        #endif
    }

    private func shortWeekday(_ date: Date) -> String {
        let symbols = ["日", "一", "二", "三", "四", "五", "六"]
        let index = Calendar.current.component(.weekday, from: date) - 1
        return symbols[max(0, min(index, 6))]
    }

    private func acidNote(_ value: Double) -> String {
        if value >= 4 { return "明亮，果汁感" }
        if value >= 3 { return "酸甜平衡" }
        if value >= 2 { return "柔和，低酸" }
        return "以醇厚为主"
    }

    private func sweetNote(_ value: Double) -> String {
        if value >= 4 { return "甜感明显" }
        if value >= 3 { return "圆润回甘" }
        if value >= 2 { return "轻微甜感" }
        return "干净清爽"
    }

    private func flavourProfile(_ recipes: [Recipe]) -> FlavorProfile {
        guard !recipes.isEmpty else { return FlavorProfile() }
        var result = FlavorProfile()
        for recipe in recipes {
            let bean = store.bean(recipe.beanId)
            let roastAcid: Double = {
                switch bean?.roast {
                case .some(.light): return 4
                case .some(.mediumLight): return 3.5
                case .some(.medium): return 2.9
                case .some(.mediumDark): return 2.3
                case .some(.dark): return 1.8
                case .none: return 3
                }
            }()
            let total = Double(max(recipe.totalGrams, 1))
            let fruit = Double(recipe.layers.filter { ["passion", "lemon", "mango", "strawberry"].contains($0.ingredientId) }.reduce(0) { $0 + $1.grams })
            let syrup = Double(recipe.layers.filter { $0.kind == .syrup }.reduce(0) { $0 + $1.grams })
            let milk = Double(recipe.layers.filter { ["milk", "oat", "coconut", "soy", "foam", "cheesefoam", "cream"].contains($0.ingredientId) }.reduce(0) { $0 + $1.grams })
            let coffee = Double(recipe.layers.filter { ["espresso", "coldbrew"].contains($0.ingredientId) }.reduce(0) { $0 + $1.grams })
            let fresh = Double(recipe.layers.filter { ["ice", "soda"].contains($0.ingredientId) }.reduce(0) { $0 + $1.grams })
            result.acid += min(max(roastAcid + fruit / total * 2.2, 0), 5)
            result.sweet += min(max(2.1 + syrup / 22 + (milk > 0 ? 0.5 : 0), 0), 5)
            result.body += min(max(2.4 + coffee / 45 + (bean?.roast == .dark ? 0.6 : 0) + (milk > 70 ? 0.35 : 0), 0), 5)
            result.milk += min(max(milk / total * 5, 0), 5)
            result.fresh += min(max((fresh + fruit) / total * 5, 0), 5)
        }
        let count = Double(recipes.count)
        return FlavorProfile(
            acid: min(max(result.acid / count, 0), 5),
            sweet: min(max(result.sweet / count, 0), 5),
            body: min(max(result.body / count, 0), 5),
            milk: min(max(result.milk / count, 0), 5),
            fresh: min(max(result.fresh / count, 0), 5)
        )
    }

    private func beanRanking(_ recipes: [Recipe]) -> [BeanRank] {
        let groups = Dictionary(grouping: recipes) { $0.beanId ?? "none" }
        return groups.map { pair in
            let key = pair.key
            let list = pair.value
            let bean = store.bean(key)
            let fallbackName = list.first?.beanName ?? ""
            return BeanRank(
                id: key,
                name: bean?.name ?? (fallbackName.isEmpty ? "未记豆子" : fallbackName),
                cups: list.count,
                grams: list.reduce(0) { $0 + $1.beanGrams }
            )
        }
        .sorted {
            if $0.cups == $1.cups { return $0.grams > $1.grams }
            return $0.cups > $1.cups
        }
    }

    private func ingredientRanking(_ recipes: [Recipe]) -> [IngredientRank] {
        var map: [String: IngredientRank] = [:]
        for recipe in recipes {
            for layer in recipe.layers {
                let previous = map[layer.ingredientId]?.count ?? 0
                map[layer.ingredientId] = IngredientRank(
                    id: layer.ingredientId,
                    name: layer.name,
                    colorHex: layer.colorHex,
                    count: previous + 1
                )
            }
        }
        return map.values.sorted { $0.count > $1.count }
    }

    private func brewStreak(_ recipes: [Recipe]) -> Int {
        guard !recipes.isEmpty else { return 0 }
        let calendar = Calendar.current
        let days = Set(recipes.map { calendar.startOfDay(for: $0.createdAt.date) })
        var cursor = calendar.startOfDay(for: Date())
        if !days.contains(cursor) { cursor = calendar.date(byAdding: .day, value: -1, to: cursor) ?? cursor }
        var count = 0
        while days.contains(cursor) {
            count += 1
            cursor = calendar.date(byAdding: .day, value: -1, to: cursor) ?? cursor.addingTimeInterval(-86400)
        }
        return count
    }
}

private struct DarkStat: View {
    let value: String
    let label: String
    var body: some View {
        VStack(spacing: 2) {
            Text(value).font(.mono(13, weight: .medium)).foregroundStyle(.white)
            Text(label).font(.system(size: 8.5)).foregroundStyle(.white.opacity(0.42))
        }
        .frame(maxWidth: .infinity)
    }
}

private struct FlavorBubble: View {
    let title: String
    let value: Double
    let note: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(title).font(.system(size: 13, weight: .medium)).foregroundStyle(C.ink)
                Spacer()
                Text(String(format: "%.1f", value)).font(.mono(8)).foregroundStyle(C.inkSoft)
            }
            HStack(spacing: 4) {
                ForEach(1...5, id: \.self) { index in
                    Circle()
                        .fill(index <= Int(value.rounded()) ? C.ink : C.ink.opacity(0.14))
                        .frame(width: index <= Int(value.rounded()) ? 10 : 7, height: index <= Int(value.rounded()) ? 10 : 7)
                }
            }
            Text(note).font(.system(size: 10)).foregroundStyle(C.inkSoft.opacity(0.62))
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(color)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

private struct FlavorLine: View {
    let title: String
    let value: Double
    let color: Color

    var body: some View {
        VStack(spacing: 5) {
            HStack {
                Text(title).font(.system(size: 11)).foregroundStyle(C.muted)
                Spacer()
                Text(String(format: "%.1f", value)).font(.mono(8)).foregroundStyle(C.muted)
            }
            ProgressBar(value: min(max(value / 5, 0), 1), tint: color.opacity(0.78), height: 4)
        }
    }
}

private struct DonutChart: View {
    let entries: [(String, Int)]

    var body: some View {
        let total = max(entries.reduce(0) { $0 + $1.1 }, 1)
        return ZStack {
            Canvas { context, size in
                let center = CGPoint(x: size.width / 2, y: size.height / 2)
                let radius = min(size.width, size.height) / 2 - 8
                var start = -90.0
                for entry in entries {
                    let sweep = Double(entry.1) / Double(total) * 360
                    var path = Path()
                    path.addArc(
                        center: center,
                        radius: radius,
                        startAngle: .degrees(start + 1.5),
                        endAngle: .degrees(start + sweep - 1.5),
                        clockwise: false
                    )
                    context.stroke(path, with: .color(Color(hex: entry.0)), lineWidth: 15)
                    start += sweep
                }
            }
            VStack(spacing: 2) {
                Text("\(total)")
                    .font(.mono(22, weight: .medium))
                    .foregroundStyle(C.ink)
                Text("杯带豆").font(.system(size: 8)).foregroundStyle(C.muted)
            }
        }
    }
}
