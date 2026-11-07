import Foundation
import SwiftUI

private enum BeanFilter: String, CaseIterable, Identifiable, Equatable {
    case all, active, peak, resting, frozen

    var id: String { rawValue }

    var label: String {
        switch self {
        case .all: return "全部"
        case .active: return "冲煮中"
        case .peak: return "最佳赏味"
        case .resting: return "养豆中"
        case .frozen: return "冷冻"
        }
    }
}

struct BeansView: View {
    @EnvironmentObject private var store: AppStore
    let onAddBean: () -> Void
    let onOpenBean: (String) -> Void

    @State private var filter: BeanFilter = .all

    private var filtered: [Bean] {
        store.recentBeans.filter { bean in
            switch filter {
            case .all: return true
            case .active: return bean.remainingGrams > 0
            case .peak: return bean.status == .peak
            case .resting: return bean.status == .resting
            case .frozen: return bean.status == .frozen
            }
        }
    }

    private var columns: [GridItem] {
        [GridItem(.adaptive(minimum: 175, maximum: 260), spacing: 12, alignment: .top)]
    }

    var body: some View {
        AdaptivePage(maxWidth: 1040) {
            VStack(alignment: .leading, spacing: 18) {
                header
                inventoryLine
                chips
                if store.data.beans.isEmpty {
                    EmptyState(
                        title: "收下第一支豆子",
                        message: "拍一张豆袋照片，自动把背景去掉。豆名、烘焙度、风味和克重，都可以慢慢补。",
                        actionTitle: "添加豆子",
                        action: onAddBean
                    )
                    .background(C.card)
                    .clipShape(RoundedRectangle(cornerRadius: 28))
                } else if filtered.isEmpty {
                    EmptyState(
                        title: "这里暂时没有豆子",
                        message: "换一个筛选条件，或者再收一支新豆。",
                        actionTitle: "添加豆子",
                        action: onAddBean
                    )
                    .background(C.card)
                    .clipShape(RoundedRectangle(cornerRadius: 28))
                } else {
                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(filtered) { bean in
                            Button {
                                onOpenBean(bean.id)
                            } label: {
                                BeanShelfCard(bean: bean)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    note
                }
            }
            .padding(.top, 12)
        }
    }

    private var header: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 5) {
                Overline(text: "BEAN SHELF")
                Text("豆仓")
                    .font(.display(34))
                    .foregroundStyle(C.ink)
            }
            Spacer()
            Button(action: onAddBean) {
                Image(systemName: "plus")
                    .font(.system(size: 20, weight: .medium))
                    .foregroundStyle(C.ink)
                    .frame(width: 48, height: 48)
                    .background(C.lime)
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("添加豆子")
        }
    }

    private var inventoryLine: some View {
        HStack(spacing: 7) {
            Text("豆仓总库存").font(.system(size: 13)).foregroundStyle(C.muted)
            Text(formatGrams(store.data.totalRemaining))
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(C.forest)
            Circle().fill(C.lime).frame(width: 4, height: 4)
            Text("\(store.data.beans.count) 支在架")
                .font(.system(size: 11.5))
                .foregroundStyle(C.muted)
        }
    }

    private var chips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(BeanFilter.allCases) { item in
                    SelectableChip(title: item.label, selected: filter == item) {
                        filter = item
                    }
                }
            }
        }
    }

    private var note: some View {
        HStack(spacing: 9) {
            Circle().fill(C.lime).frame(width: 6, height: 6)
            Text("照片会自动抠图。点开豆子，可以调克数、看赏味期和用它调过的杯。")
                .font(.system(size: 11.5))
                .foregroundStyle(C.muted)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 13)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(C.paperSoft)
        .clipShape(RoundedRectangle(cornerRadius: 19))
    }
}

private struct BeanShelfCard: View {
    let bean: Bean

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            BeanArtwork(bean: bean)
                .frame(height: 155)
                .padding(8)
            VStack(alignment: .leading, spacing: 4) {
                Text(bean.name)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(C.ink)
                    .lineLimit(1)
                Text("\(originFlag(bean.origin)) \(bean.displayOrigin)")
                    .font(.system(size: 11))
                    .foregroundStyle(C.muted)
                    .lineLimit(1)
                Text("\(bean.roaster.isEmpty ? "未记录烘焙商" : bean.roaster) · \(bean.process)")
                    .font(.system(size: 11))
                    .foregroundStyle(C.muted)
                    .lineLimit(1)
                Spacer(minLength: 8)
                HStack {
                    Text(formatGrams(bean.remainingGrams))
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(Color(hex: bean.status.inkHex))
                    Text("/ \(formatGrams(bean.totalGrams))")
                        .font(.mono(9))
                        .foregroundStyle(C.muted)
                    Spacer()
                    Text(bean.roast.short)
                        .font(.mono(9))
                        .foregroundStyle(C.inkSoft)
                        .padding(.horizontal, 7)
                        .padding(.vertical, 3)
                        .background(C.paperSoft)
                        .clipShape(Capsule())
                }
                ProgressBar(value: bean.remainingRatio, tint: Color(hex: bean.status.inkHex).opacity(0.72))
                    .padding(.top, 2)
            }
            .padding(.horizontal, 14)
            .padding(.top, 2)
            .padding(.bottom, 14)
        }
        .background(C.card)
        .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 26).stroke(C.lineSoft, lineWidth: 1)
        }
    }
}
