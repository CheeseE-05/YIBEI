import Foundation
import SwiftUI
import UIKit

enum AppTab: String, CaseIterable, Identifiable, Equatable {
    case home, beans, maker, menu, stats

    var id: String { rawValue }

    var label: String {
        switch self {
        case .home: return "首页"
        case .beans: return "豆子"
        case .maker: return "调一杯"
        case .menu: return "菜单"
        case .stats: return "统计"
        }
    }

    var symbol: String {
        switch self {
        case .home: return "house"
        case .beans: return "leaf"
        case .maker: return "cup.and.saucer"
        case .menu: return "book.closed"
        case .stats: return "chart.bar.xaxis"
        }
    }

    /// 底部 Tab Bar 图标：用固定 pointSize 的 SF Symbol（模板渲染）。
    /// 仍然是系统原生 TabView / UITabBar 在渲染，只是交给系统的图片本身更小，
    /// 系统 Tab Bar 的布局、文字与点击区域保持不变。
    var tabBarIcon: Image {
        let configuration = UIImage.SymbolConfiguration(pointSize: 19, weight: .medium, scale: .medium)
        guard let image = UIImage(systemName: symbol, withConfiguration: configuration) else {
            return Image(systemName: symbol)
        }
        return Image(uiImage: image.withRenderingMode(.alwaysTemplate))
    }
}

enum Route: Hashable {
    case bean(String)
    case newBean
    case editBean(String)
    case recipe(String)
    case promoCard(String)
    case settings
}

extension AppTab {
    /// CI / 截图脚本专用：启动参数直达某一页，例如 `-uiTestTab beans`。
    /// 只在 Debug 构建中读取，Release 包行为不受影响。
    static var launchArgumentSelection: AppTab? {
        #if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        if let index = arguments.firstIndex(of: "-uiTestTab"), arguments.indices.contains(index + 1) {
            return AppTab(rawValue: arguments[index + 1].lowercased())
        }
        if let value = ProcessInfo.processInfo.environment["UI_TEST_TAB"] {
            return AppTab(rawValue: value.lowercased())
        }
        #endif
        return nil
    }

    /// CI / 截图脚本专用：自动轮流切换五个 tab（秒/次），用于录制导航栏切换动画验证视频。
    /// 例如 `-uiTestAutoCycle 1.6`。只在 Debug 构建中读取。
    static var launchArgumentAutoCycleInterval: Double? {
        #if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        if let index = arguments.firstIndex(of: "-uiTestAutoCycle"),
           arguments.indices.contains(index + 1),
           let value = Double(arguments[index + 1]),
           value > 0 {
            return value
        }
        #endif
        return nil
    }
}

extension Route {
    /// CI / 截图脚本专用：启动后自动打开某个二级页面，便于对详情页做 UI 冒烟截图。
    /// 例如 `-uiTestTab beans -uiTestRoute bean`（豆子详情页）。只在 Debug 构建中读取。
    static var launchArgumentValue: String? {
        #if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        if let index = arguments.firstIndex(of: "-uiTestRoute"), arguments.indices.contains(index + 1) {
            return arguments[index + 1].lowercased()
        }
        if let value = ProcessInfo.processInfo.environment["UI_TEST_ROUTE"] {
            return value.lowercased()
        }
        #endif
        return nil
    }
}

/// CI / 截图脚本专用：进入「编辑配方」流程，便于在模拟器里稳定截到编辑态与保存/取消后的页面。
/// 只在 Debug 构建中读取：
///   -uiTestEditRecipe                   启动后把最近一杯载入编辑态，并切到「调一杯」；
///   -uiTestEditShowSaveSheet            编辑态自动打开「保存修改」弹窗（验证预填与按钮文案）；
///   -uiTestEditAutoSave                 弹窗里自动改名字/笔记并调用与保存按钮相同的保存逻辑；
///   -uiTestEditCancelAfter 2            编辑态等待 2 秒后点「取消编辑」（验证原配方不受影响）。
enum DebugEditFlow {
    static var shouldBegin: Bool { hasFlag("-uiTestEditRecipe") }
    static var shouldShowSaveSheet: Bool { hasFlag("-uiTestEditShowSaveSheet") }
    static var autoSave: Bool { hasFlag("-uiTestEditAutoSave") }

    static func seconds(_ flag: String) -> Double? {
        #if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        if let index = arguments.firstIndex(of: flag), arguments.indices.contains(index + 1),
           let value = Double(arguments[index + 1]), value >= 0 {
            return value
        }
        #endif
        return nil
    }

    static var autoCancelAfter: Double? { seconds("-uiTestEditCancelAfter") }

    private static func hasFlag(_ flag: String) -> Bool {
        #if DEBUG
        return ProcessInfo.processInfo.arguments.contains(flag)
        #else
        return false
        #endif
    }
}

/// App 根视图：完全使用系统原生 `TabView`。
///
/// iOS 26/27 上 `TabView` 会自动采用系统标准 Liquid Glass Tab Bar：
/// 毛玻璃胶囊底栏 + 点击时在入口之间滑动/弹出的大号透明玻璃选中滑块
///（与 Apple Music / News 等系统 App 完全一致的实现，不再手写滑块）。
struct RootView: View {
    @EnvironmentObject private var store: AppStore

    /// 外观偏好（跟随系统 / 浅色 / 深色），设置页写入、这里全局生效。
    @AppStorage(AppAppearance.storageKey) private var appearanceRaw = AppAppearance.system.rawValue

    @State private var tab: AppTab = AppTab.launchArgumentSelection ?? .home
    /// 每个 Tab 各自维护自己的导航栈，切换 Tab 时保留各自页面状态。
    @State private var paths: [AppTab: [Route]] = [:]
    @State private var didOpenLaunchRoute = false
    @State private var autoCycleTask: Task<Void, Never>?
    /// 从哪个 Tab 的配方详情进入「编辑配方」；保存 / 取消编辑后回到那里。
    @State private var editingReturnTab: AppTab?
    /// 进入编辑的那杯配方 id：如果编辑是从「调一杯」Tab 自己的详情页进入的，
    /// 取消编辑后需要把这杯详情推回去，恢复编辑前的页面状态。
    @State private var editingReturnRecipeID: String?
    @State private var didStartDebugEditFlow = false

    var body: some View {
        tabViewContent
            .preferredColorScheme(AppAppearance(rawValue: appearanceRaw)?.colorScheme)
    }

    private var tabViewContent: some View {
        TabView(selection: $tab) {
            tabRoot(.home, path: pathBinding(.home)) { homeRoot }
            tabRoot(.beans, path: pathBinding(.beans)) { beansRoot }
            tabRoot(.maker, path: pathBinding(.maker)) { makerRoot }
            tabRoot(.menu, path: pathBinding(.menu)) { menuRoot }
            tabRoot(.stats, path: pathBinding(.stats)) { statsRoot }
        }
        .tint(C.ink)
        .background(PaperBackground())
        .onAppear {
            openLaunchRouteIfNeeded()
            startDebugEditFlowIfNeeded()
            startAutoCycleIfNeeded()
        }
        .onDisappear {
            autoCycleTask?.cancel()
            autoCycleTask = nil
        }
    }

    // MARK: - 每个 Tab 的 NavigationStack

    private func pathBinding(_ tab: AppTab) -> Binding<[Route]> {
        Binding(
            get: { paths[tab] ?? [] },
            set: { paths[tab] = $0 }
        )
    }

    @ViewBuilder
    private func tabRoot<Content: View>(
        _ tab: AppTab,
        path: Binding<[Route]>,
        @ViewBuilder content: () -> Content
    ) -> some View {
        NavigationStack(path: path) {
            content()
                .navigationDestination(for: Route.self) { route in
                    destination(route, in: tab)
                        .toolbar(.hidden, for: .tabBar)
                }
                .toolbar(.hidden, for: .navigationBar)
        }
        .tabItem {
            Label {
                Text(tab.label)
            } icon: {
                tab.tabBarIcon
            }
        }
        .tag(tab)
    }

    // MARK: - 各 Tab 首页

    private var homeRoot: some View {
        HomeView(
            onOpenBean: { push(.bean($0), in: .home) },
            onOpenRecipe: { push(.recipe($0), in: .home) },
            onSeeBeans: { switchTab(.beans) },
            onSeeMenu: { switchTab(.menu) },
            onAddBean: { push(.newBean, in: .home) },
            onMake: { switchTab(.maker) },
            onStats: { switchTab(.stats) }
        )
    }

    private var beansRoot: some View {
        BeansView(
            onAddBean: { push(.newBean, in: tab) },
            onOpenBean: { push(.bean($0), in: .beans) }
        )
    }

    private var makerRoot: some View {
        MakerView(
            onOpenRecipe: { push(.recipe($0), in: .maker) },
            onEditSaved: { id in
                let target = editingReturnTab ?? .menu
                editingReturnTab = nil
                editingReturnRecipeID = nil
                if target == .maker {
                    // 编辑前 Maker 栈里那杯详情已被收起：保存后放回更新后的详情。
                    paths[.maker] = [.recipe(id)]
                }
                switchTab(target)
            },
            onEditCancelled: {
                let target = editingReturnTab ?? .menu
                let restored = editingReturnRecipeID
                editingReturnTab = nil
                editingReturnRecipeID = nil
                if target == .maker, let restored {
                    // 恢复编辑前的页面状态：这杯详情还在，原配方未被改动。
                    paths[.maker] = [.recipe(restored)]
                }
                switchTab(target)
            }
        )
    }

    private var menuRoot: some View {
        MenuView(
            onOpenSettings: { push(.settings, in: .menu) },
            onOpenRecipe: { push(.recipe($0), in: .menu) },
            onOpenPromoCard: { push(.promoCard($0), in: .menu) },
            onMake: { switchTab(.maker) }
        )
    }

    private var statsRoot: some View {
        StatsView(
            onMake: { switchTab(.maker) },
            onSeeBeans: { switchTab(.beans) },
            onOpenSettings: { push(.settings, in: .stats) }
        )
    }

    // MARK: - 路由

    @ViewBuilder
    private func destination(_ route: Route, in owner: AppTab) -> some View {
        switch route {
        case .bean(let id):
            if let bean = store.bean(id) {
                BeanDetailView(
                    bean: bean,
                    onEdit: { push(.editBean(id), in: owner) },
                    onMake: {
                        store.draft.beanId = id
                        switchTab(.maker)
                    },
                    onOpenRecipe: { push(.recipe($0), in: owner) }
                )
            } else {
                MissingView()
            }
        case .newBean:
            BeanEditorView(bean: nil)
        case .editBean(let id):
            if let bean = store.bean(id) {
                BeanEditorView(bean: bean)
            } else {
                MissingView()
            }
        case .recipe(let id):
            if let recipe = store.recipe(id) {
                RecipeDetailView(
                    initialRecipe: recipe,
                    onRepeat: {
                        store.loadRecipeIntoDraft(recipe)
                        editingReturnTab = nil
                        editingReturnRecipeID = nil
                        paths[.maker] = []
                        switchTab(.maker)
                    },
                    onOpenBean: { push(.bean($0), in: owner) },
                    onPromoCard: { push(.promoCard(recipe.id), in: owner) },
                    onEdit: {
                        store.beginEditingRecipe(recipe)
                        editingReturnTab = owner
                        editingReturnRecipeID = recipe.id
                        // 收起 Maker 栈里可能压着的旧详情，保证切过去能看到编辑态。
                        paths[.maker] = []
                        switchTab(.maker)
                    },
                    onDelete: {
                        store.deleteRecipe(recipe)
                        if !store.isEditingRecipe {
                            editingReturnTab = nil
                            editingReturnRecipeID = nil
                        }
                        if !(paths[tab] ?? []).isEmpty {
                            paths[tab]?.removeLast()
                        }
                    }
                )
            } else {
                MissingView()
            }
        case .promoCard(let id):
            if let recipe = store.recipe(id) {
                PromoCardEditorView(
                    recipe: recipe,
                    bean: store.bean(recipe.beanId),
                    onOpenSettings: { push(.settings, in: owner) }
                )
            } else {
                MissingView()
            }
        case .settings:
            SettingsView()
        }
    }

    private func push(_ route: Route, in tab: AppTab) {
        paths[tab, default: []].append(route)
    }

    private func switchTab(_ value: AppTab) {
        tab = value
    }

    // MARK: - Debug：截图 / 录屏辅助

    private func openLaunchRouteIfNeeded() {
        #if DEBUG
        guard !didOpenLaunchRoute else { return }
        didOpenLaunchRoute = true
        guard let value = Route.launchArgumentValue else { return }
        switch value {
        case "bean":
            if let id = store.recentBeans.first?.id {
                push(.bean(id), in: tab)
            }
        case "recipe":
            if let id = store.recentRecipes.first?.id {
                push(.recipe(id), in: tab)
            }
        case "settings":
            push(.settings, in: tab)
        case "promocard", "promo-card", "card":
            if let id = store.recentRecipes.first?.id {
                push(.promoCard(id), in: tab)
            }
        case "data":
            // 旧截图脚本仍传 data：1.2 功能升级后数据管理已迁入设置页。
            push(.settings, in: tab)
        case "newbean", "new-bean", "beaneditor":
            push(.newBean, in: tab)
        default:
            break
        }
        #endif
    }

    /// 截图脚本用「编辑配方」流程：把最近一杯载入编辑态并切到调一杯页。
    /// 后续的「打开保存弹窗 / 自动保存 / 取消编辑」由 MakerView 内部按启动参数驱动，
    /// 走的都是真实按钮对应的代码路径（保存 = SaveCupSheet.save → Store.updateRecipe）。
    private func startDebugEditFlowIfNeeded() {
        #if DEBUG
        guard !didStartDebugEditFlow else { return }
        guard DebugEditFlow.shouldBegin else { return }
        didStartDebugEditFlow = true
        guard let recipe = store.recentRecipes.first else { return }
        store.beginEditingRecipe(recipe)
        editingReturnTab = .menu
        tab = .maker
        #endif
    }

    private func startAutoCycleIfNeeded() {
        #if DEBUG
        guard autoCycleTask == nil, let interval = AppTab.launchArgumentAutoCycleInterval else { return }
        autoCycleTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: 1_200_000_000)
            while !Task.isCancelled {
                for value in AppTab.allCases {
                    guard !Task.isCancelled else { return }
                    withAnimation {
                        tab = value
                    }
                    try? await Task.sleep(nanoseconds: UInt64(interval * 1_000_000_000))
                }
            }
        }
        #endif
    }
}

private struct MissingView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "questionmark.folder")
                .font(.system(size: 34))
                .foregroundStyle(C.muted)
            Text("这一页已经不在手边了").font(.system(size: 19, weight: .semibold))
            Text("它可能已经被删除。").font(.system(size: 13)).foregroundStyle(C.muted)
            Button("回到上一页") { dismiss() }
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(C.ink)
                .padding(.horizontal, 20)
                .frame(height: 44)
                .background(C.lime)
                .clipShape(Capsule())
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(PaperBackground())
    }
}
