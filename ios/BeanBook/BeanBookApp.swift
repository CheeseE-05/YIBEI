import SwiftUI

@main
struct BeanBookApp: App {
    @StateObject private var store = AppStore()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(store)
                .tint(C.ink)
        }
    }
}
