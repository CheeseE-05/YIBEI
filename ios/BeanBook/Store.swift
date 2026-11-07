import Foundation
import Combine
import UIKit

@MainActor
final class AppStore: ObservableObject {
    @Published var data: AppData
    @Published var draft: MakerDraft
    /// 正在编辑的配方 id；nil 表示调一杯页是「全新一杯」。
    /// 由菜单/首页/豆子页的配方详情点击「编辑配方」写入，保存或取消后清空。
    @Published var editingRecipeID: String? = nil

    private let baseURL: URL
    private let imageDirectory: URL
    private let dataURL: URL
    private let encoder: JSONEncoder
    private let decoder: JSONDecoder

    init() {
        let support = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        baseURL = support.appendingPathComponent("BeanBook", isDirectory: true)
        imageDirectory = baseURL.appendingPathComponent(IMAGE_DIRECTORY, isDirectory: true)
        dataURL = baseURL.appendingPathComponent("data.json")

        try? FileManager.default.createDirectory(at: imageDirectory, withIntermediateDirectories: true)

        encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        decoder = JSONDecoder()

        // 注意：`data`/`draft` 都是 @Published 包装属性，在全部存储属性初始化完成前
        // 读取 self.data 会触发「'self' used before all stored properties are initialized」，
        // 所以先用局部常量算出初始数据，再分别赋值。
        let initialData: AppData
        if let raw = try? Data(contentsOf: dataURL),
           let loaded = try? decoder.decode(AppData.self, from: raw) {
            initialData = loaded
        } else {
            initialData = DemoData.make()
        }
        data = initialData
        draft = MakerDraft(beanId: initialData.beans.sorted { $0.lastUsedAt > $1.lastUsedAt }.first?.id)
        if !FileManager.default.fileExists(atPath: dataURL.path) {
            save()
        }
    }

    // MARK: - Reads

    var recentBeans: [Bean] {
        data.beans.sorted {
            if $0.lastUsedAt == $1.lastUsedAt { return $0.createdAt > $1.createdAt }
            return $0.lastUsedAt > $1.lastUsedAt
        }
    }

    var recentRecipes: [Recipe] {
        data.recipes.sorted { $0.createdAt > $1.createdAt }
    }

    func bean(_ id: String?) -> Bean? { data.bean(id) }
    func recipe(_ id: String) -> Recipe? { data.recipe(id) }

    func imageURL(for bean: Bean) -> URL? {
        imageURL(forPath: bean.imagePath)
    }

    /// 按 App 私有目录里的相对路径取图片（豆子照片、宣传卡片相册封面等通用）。
    func imageURL(forPath path: String?) -> URL? {
        guard let path, !path.isEmpty else { return nil }
        let url = baseURL.appendingPathComponent(path)
        return FileManager.default.fileExists(atPath: url.path) ? url : nil
    }

    func uiImage(for bean: Bean) -> UIImage? {
        uiImage(path: bean.imagePath)
    }

    func uiImage(path: String?) -> UIImage? {
        guard let url = imageURL(forPath: path) else { return nil }
        return UIImage(contentsOfFile: url.path)
    }

    // MARK: - Persistence

    func save() {
        do {
            let raw = try encoder.encode(data)
            try raw.write(to: dataURL, options: .atomic)
        } catch {
            print("BeanBook save failed: \(error)")
        }
    }

    @discardableResult
    func saveImage(_ image: UIImage) -> String? {
        guard let raw = image.pngData() else { return nil }
        let name = "bean_\(UUID().uuidString).png"
        let url = imageDirectory.appendingPathComponent(name)
        do {
            try raw.write(to: url, options: .atomic)
            return "\(IMAGE_DIRECTORY)/\(name)"
        } catch {
            return nil
        }
    }

    func deleteImage(path: String?) {
        guard let path, !path.isEmpty else { return }
        try? FileManager.default.removeItem(at: baseURL.appendingPathComponent(path))
    }

    // MARK: - Beans

    func addBean(_ bean: Bean) {
        data.beans.insert(bean, at: 0)
        save()
    }

    func updateBean(_ bean: Bean) {
        if let index = data.beans.firstIndex(where: { $0.id == bean.id }) {
            data.beans[index] = bean
        } else {
            data.beans.insert(bean, at: 0)
        }
        save()
    }

    func deleteBean(_ bean: Bean) {
        data.beans.removeAll { $0.id == bean.id }
        deleteImage(path: bean.imagePath)
        if draft.beanId == bean.id {
            draft.beanId = recentBeans.first?.id
        }
        save()
    }

    // MARK: - Ingredients

    func addCustomIngredient(name: String, colorHex: String, kind: IngredientKind) {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        data.customIngredients.append(
            CustomIngredient(
                id: "custom_\(UUID().uuidString)",
                name: trimmed,
                colorHex: colorHex,
                kind: kind,
                createdAt: nowMillis()
            )
        )
        save()
    }

    func deleteCustomIngredient(_ id: String) {
        data.customIngredients.removeAll { $0.id == id }
        save()
    }

    var allIngredients: [IngredientSpec] {
        data.customIngredients.map(\.spec) + Ingredients.builtIn
    }

    // MARK: - Maker

    func selectBean(_ id: String?) {
        draft.beanId = id
    }

    @discardableResult
    func pour(_ spec: IngredientSpec, requested: Int) -> Int {
        guard requested > 0, draft.remaining > 0 else { return 0 }
        let amount = min(requested, draft.remaining)
        if let last = draft.layers.last, last.ingredientId == spec.id {
            draft.layers[draft.layers.count - 1].grams += amount
        } else {
            draft.layers.append(
                RecipeLayer(
                    ingredientId: spec.id,
                    name: spec.name,
                    colorHex: spec.colorHex,
                    kind: spec.kind,
                    grams: amount,
                    alpha: spec.alpha
                )
            )
        }
        draft.stirred = false
        return amount
    }

    func undoLayer() {
        guard !draft.layers.isEmpty else { return }
        draft.layers.removeLast()
        draft.stirred = false
    }

    func clearCup() {
        draft.layers.removeAll()
        draft.stirred = false
    }

    func toggleStir() {
        draft.stirred.toggle()
    }

    func editLayer(index: Int, grams: Int) {
        guard draft.layers.indices.contains(index) else { return }
        let other = draft.layers.enumerated().reduce(0) { partial, item in
            item.offset == index ? partial : partial + item.element.grams
        }
        draft.layers[index].grams = min(max(grams, 1), max(CUP_CAPACITY - other, 1))
        draft.stirred = false
    }

    func removeLayer(index: Int) {
        guard draft.layers.indices.contains(index) else { return }
        draft.layers.remove(at: index)
        draft.stirred = false
    }

    func loadRecipeIntoDraft(_ recipe: Recipe) {
        // 「再调一杯」是复制一杯新的，不进入编辑态。
        editingRecipeID = nil
        draft = MakerDraft(layers: recipe.layers, beanId: recipe.beanId, stirred: recipe.stirred)
    }

    // MARK: - 编辑配方

    var isEditingRecipe: Bool { editingRecipeID != nil }

    /// 正在编辑的配方（可能已被删除，因此是可选值）。
    var editingRecipe: Recipe? { editingRecipeID.flatMap { recipe($0) } }

    /// 进入编辑态：把配方载入调一杯的 draft，并记住正在编辑哪一杯。
    func beginEditingRecipe(_ recipe: Recipe) {
        editingRecipeID = recipe.id
        draft = MakerDraft(layers: recipe.layers, beanId: recipe.beanId, stirred: recipe.stirred)
    }

    /// 退出编辑态：不写库，原配方保持不动；draft 恢复成一杯新的。
    func cancelEditingRecipe() {
        editingRecipeID = nil
        draft = MakerDraft(beanId: recentBeans.first?.id)
    }

    func suggestedRecipeName() -> String {
        let ids = Set(draft.layers.map(\.ingredientId))
        if ids.contains("matcha") && ids.contains("milk") { return "抹茶拿铁" }
        if ids.contains("oat") && ids.contains("espresso") { return "燕麦拿铁" }
        if ids.contains("osmanthus") && ids.contains("espresso") { return "桂花拿铁" }
        if ids.contains("coldbrew") && ids.contains("soda") { return "冷萃气泡" }
        if ids.contains("milk") && ids.contains("espresso") && !ids.contains("foam") { return "Dirty" }
        if ids.contains("espresso") && ids.contains("milk") && ids.contains("foam") { return "经典拿铁" }
        if ids.contains("soda") || ids.contains("sparkling") { return "气泡特调" }
        return "\(draft.layers.last?.name ?? "今日")特调"
    }

    @discardableResult
    func saveRecipe(
        name: String,
        beanId: String?,
        rating: Int,
        notes: String,
        mouthfeel: String,
        sweetness: String,
        beanGrams: Int,
        price: Int? = nil
    ) -> Recipe? {
        guard !draft.layers.isEmpty else { return nil }
        let bean = data.bean(beanId)
        let cleanName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let recipe = Recipe(
            id: UUID().uuidString,
            name: cleanName.isEmpty ? suggestedRecipeName() : cleanName,
            beanId: bean?.id,
            beanName: bean?.name ?? "",
            layers: draft.layers,
            rating: min(max(rating, 1), 5),
            notes: notes.trimmingCharacters(in: .whitespacesAndNewlines),
            mouthfeel: mouthfeel,
            sweetness: sweetness,
            beanGrams: max(beanGrams, 0),
            stirred: draft.stirred,
            createdAt: nowMillis(),
            price: (price ?? 0) > 0 ? price : nil
        )
        data.recipes.insert(recipe, at: 0)
        if let beanId, let index = data.beans.firstIndex(where: { $0.id == beanId }) {
            data.beans[index].remainingGrams = max(data.beans[index].remainingGrams - Double(beanGrams), 0)
            data.beans[index].lastUsedAt = nowMillis()
        }
        save()
        draft = MakerDraft(beanId: recentBeans.first?.id)
        return recipe
    }

    /// 保存「编辑配方」的修改：更新原记录（保留 id / createdAt 等未暴露的字段），不新增一杯。
    /// 与 `saveRecipe` 不同，编辑保存不会再次从豆仓扣除克数，避免同一杯被重复扣豆；
    /// 用豆克数只更新配方里的记录值。
    @discardableResult
    func updateRecipe(
        id: String,
        name: String,
        beanId: String?,
        rating: Int,
        notes: String,
        mouthfeel: String,
        sweetness: String,
        beanGrams: Int,
        price: Int? = nil
    ) -> Recipe? {
        guard !draft.layers.isEmpty else { return nil }
        guard let index = data.recipes.firstIndex(where: { $0.id == id }) else { return nil }
        let bean = data.bean(beanId)
        let cleanName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        var recipe = data.recipes[index]
        recipe.name = cleanName.isEmpty ? suggestedRecipeName() : cleanName
        recipe.beanId = bean?.id
        recipe.beanName = bean?.name ?? ""
        recipe.layers = draft.layers
        recipe.rating = min(max(rating, 1), 5)
        recipe.notes = notes.trimmingCharacters(in: .whitespacesAndNewlines)
        recipe.mouthfeel = mouthfeel
        recipe.sweetness = sweetness
        recipe.beanGrams = max(beanGrams, 0)
        recipe.price = (price ?? 0) > 0 ? price : nil
        recipe.stirred = draft.stirred
        data.recipes[index] = recipe
        editingRecipeID = nil
        save()
        draft = MakerDraft(beanId: recentBeans.first?.id)
        return recipe
    }

    func deleteRecipe(_ recipe: Recipe) {
        data.recipes.removeAll { $0.id == recipe.id }
        if editingRecipeID == recipe.id {
            editingRecipeID = nil
            draft = MakerDraft(beanId: recentBeans.first?.id)
        }
        save()
    }

    /// 只更新菜单价格（卡片编辑器里改价格用）。
    @discardableResult
    func updateRecipePrice(_ price: Int?, for recipeID: String) -> Recipe? {
        guard let index = data.recipes.firstIndex(where: { $0.id == recipeID }) else { return nil }
        var updated = data.recipes[index]
        updated.price = (price ?? 0) > 0 ? price : nil
        data.recipes[index] = updated
        save()
        return updated
    }

    // MARK: - 宣传卡片

    /// 保存某杯配方的宣传卡片配置（可选字段，随配方一起持久化 / 导出 / 备份）。
    @discardableResult
    func savePromoCard(_ config: PromoCardConfig, for recipeID: String) -> Recipe? {
        guard let index = data.recipes.firstIndex(where: { $0.id == recipeID }) else { return nil }
        var updated = data.recipes[index]
        var config = config
        config.updatedAt = nowMillis()
        updated.promoCard = config
        data.recipes[index] = updated
        save()
        return updated
    }

    /// 取某杯配方默认 / 已保存的宣传卡片配置。
    func promoCardConfig(for recipe: Recipe) -> PromoCardConfig {
        recipe.promoCard ?? PromoCardConfig.makeDefault(for: recipe, bean: bean(recipe.beanId))
    }

    /// 保存相册选中的卡片封面图片（复用私有图片目录）。
    func saveCardCoverImage(_ image: UIImage) -> String? {
        saveImage(image)
    }

    /// 保存 AI 生成的咖啡插画（复用私有图片目录，路径随卡片配置持久化）。
    func saveCardIllustration(_ image: UIImage) -> String? {
        saveImage(image)
    }

    // MARK: - Transfer

    func exportDatabaseData() -> Data? {
        var images: [String: String] = [:]
        for bean in data.beans {
            guard let path = bean.imagePath, !path.isEmpty else { continue }
            let url = baseURL.appendingPathComponent(path)
            if let raw = try? Data(contentsOf: url) {
                images[path] = raw.base64EncodedString()
            }
        }
        let bundle = DatabaseBundle(data: data, images: images)
        return try? encoder.encode(bundle)
    }

    func importDatabaseData(_ raw: Data, replace: Bool) throws -> String {
        let bundle = try decoder.decode(DatabaseBundle.self, from: raw)
        guard bundle.kind == DatabaseBundle.kindValue else {
            throw TransferError.message("这不是「忆杯手记」的数据库备份，请选择由本 App 导出的文件。")
        }
        writeImages(bundle.images)
        if replace {
            data = bundle.data
            save()
            draft.beanId = recentBeans.first?.id
            return "已恢复 \(data.beans.count) 支豆子、\(data.recipes.count) 杯配方"
        }
        let recipeIds = Set(data.recipes.map(\.id))
        let beanIds = Set(data.beans.map(\.id))
        let ingredientIds = Set(data.customIngredients.map(\.id))
        let newRecipes = bundle.data.recipes.filter { !recipeIds.contains($0.id) }
        let newBeans = bundle.data.beans.filter { !beanIds.contains($0.id) }
        let newIngredients = bundle.data.customIngredients.filter { !ingredientIds.contains($0.id) }
        data.beans.append(contentsOf: newBeans)
        data.recipes.insert(contentsOf: newRecipes, at: 0)
        data.customIngredients.append(contentsOf: newIngredients)
        save()
        return "已合并导入 \(newBeans.count) 支豆子、\(newRecipes.count) 杯配方"
    }

    func makeRecipeShareData(_ recipe: Recipe) -> Data? {
        let bean = data.bean(recipe.beanId)
        var imageBase64: String?
        if let path = bean?.imagePath {
            let url = baseURL.appendingPathComponent(path)
            imageBase64 = (try? Data(contentsOf: url))?.base64EncodedString()
        }
        let custom = data.customIngredients.filter { custom in
            recipe.layers.contains { $0.ingredientId == custom.id }
        }
        let bundle = RecipeShareBundle(
            recipe: recipe,
            bean: bean,
            beanImageBase64: imageBase64,
            customIngredients: custom
        )
        return try? encoder.encode(bundle)
    }

    func makeRecipeShareFile(_ recipe: Recipe) -> URL? {
        guard let raw = makeRecipeShareData(recipe) else { return nil }
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent("BeanBookExports", isDirectory: true)
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let safeName = recipe.name
            .replacingOccurrences(of: "[\\\\/:*?\"<>|\\s]+", with: "_", options: .regularExpression)
            .prefix(28)
        let filename = "\(safeName.isEmpty ? "recipe" : String(safeName))-\(Int(Date().timeIntervalSince1970)).brewkinrecipe.json"
        let url = directory.appendingPathComponent(filename)
        do {
            try raw.write(to: url, options: .atomic)
            return url
        } catch {
            return nil
        }
    }

    func importRecipeData(_ raw: Data) throws -> String {
        let bundle = try decoder.decode(RecipeShareBundle.self, from: raw)
        guard bundle.kind == RecipeShareBundle.kindValue else {
            throw TransferError.message("这不是离线配方文件。")
        }
        let now = nowMillis()
        var beanId: String?
        var importedBean = false

        if let source = bundle.bean {
            if let existing = data.beans.first(where: {
                $0.name.trimmingCharacters(in: .whitespacesAndNewlines)
                    .caseInsensitiveCompare(source.name.trimmingCharacters(in: .whitespacesAndNewlines)) == .orderedSame &&
                $0.roaster.trimmingCharacters(in: .whitespacesAndNewlines)
                    .caseInsensitiveCompare(source.roaster.trimmingCharacters(in: .whitespacesAndNewlines)) == .orderedSame
            }) {
                beanId = existing.id
                if existing.imagePath == nil, let image = bundle.beanImageBase64 {
                    if let path = writeBase64Image(image) {
                        if let index = data.beans.firstIndex(where: { $0.id == existing.id }) {
                            data.beans[index].imagePath = path
                        }
                    }
                }
            } else {
                var copied = source
                if data.beans.contains(where: { $0.id == copied.id }) {
                    copied.id = UUID().uuidString
                }
                if let image = bundle.beanImageBase64 {
                    copied.imagePath = writeBase64Image(image)
                } else {
                    copied.imagePath = nil
                }
                data.beans.insert(copied, at: 0)
                beanId = copied.id
                importedBean = true
            }
        } else {
            beanId = bundle.recipe.beanId.flatMap { data.bean($0)?.id }
        }

        for ingredient in bundle.customIngredients
        where !data.customIngredients.contains(where: { $0.id == ingredient.id }) {
            data.customIngredients.append(ingredient)
        }

        var recipe = bundle.recipe
        if data.recipes.contains(where: { $0.id == recipe.id }) {
            recipe.id = UUID().uuidString
        }
        if data.recipes.contains(where: { $0.name.trimmingCharacters(in: .whitespacesAndNewlines) == recipe.name.trimmingCharacters(in: .whitespacesAndNewlines) }) {
            recipe.name = "\(recipe.name)（导入）"
        }
        recipe.beanId = beanId
        recipe.beanName = data.bean(beanId)?.name ?? recipe.beanName
        recipe.createdAt = now
        data.recipes.insert(recipe, at: 0)
        save()
        return "已导入配方「\(recipe.name)」" + (importedBean ? "，并新增了一支豆子" : "")
    }

    private func writeImages(_ images: [String: String]) {
        for (path, base64) in images {
            guard path.hasPrefix("\(IMAGE_DIRECTORY)/"), !path.contains(".."), !path.contains("\\") else { continue }
            guard let raw = Data(base64Encoded: base64) else { continue }
            let url = baseURL.appendingPathComponent(path)
            try? FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
            try? raw.write(to: url, options: .atomic)
        }
    }

    private func writeBase64Image(_ base64: String) -> String? {
        guard let raw = Data(base64Encoded: base64) else { return nil }
        let name = "import_\(UUID().uuidString).png"
        let url = imageDirectory.appendingPathComponent(name)
        do {
            try raw.write(to: url, options: .atomic)
            return "\(IMAGE_DIRECTORY)/\(name)"
        } catch {
            return nil
        }
    }
}

enum TransferError: LocalizedError {
    case message(String)

    var errorDescription: String? {
        switch self {
        case .message(let text): return text
        }
    }
}

enum DemoData {
    static func make() -> AppData {
        let now = nowMillis()
        func daysAgo(_ days: Int) -> Int64 { now - Int64(days) * 86_400_000 }
        func hoursAgo(_ hours: Int) -> Int64 { now - Int64(hours) * 3_600_000 }

        let guji = Bean(
            id: UUID().uuidString,
            name: "Ethiopia Guji",
            roaster: "Coffee Buff",
            origin: "埃塞俄比亚",
            region: "Guji",
            process: "水洗",
            roast: .light,
            flavors: ["茉莉", "柠檬", "红茶感"],
            totalGrams: 150,
            remainingGrams: 86,
            roastDate: daysAgo(9),
            openedAt: daysAgo(7),
            storage: .room,
            imagePath: nil,
            note: "手冲很干净，做成 Dirty 会有很亮的柑橘尾韵。",
            createdAt: daysAgo(9),
            lastUsedAt: hoursAgo(20)
        )
        let lychee = Bean(
            id: UUID().uuidString,
            name: "Colombia Lychee",
            roaster: "RAW",
            origin: "哥伦比亚",
            region: "Huila",
            process: "蜜处理",
            roast: .mediumLight,
            flavors: ["荔枝", "白葡萄", "焦糖"],
            totalGrams: 200,
            remainingGrams: 146,
            roastDate: daysAgo(3),
            openedAt: daysAgo(2),
            storage: .room,
            imagePath: nil,
            note: "开袋就是荔枝糖的甜香，适合和气泡水做一杯。",
            createdAt: daysAgo(3),
            lastUsedAt: hoursAgo(4)
        )

        let lycheeSoda = Recipe(
            id: UUID().uuidString,
            name: "荔枝气泡冷萃",
            beanId: lychee.id,
            beanName: lychee.name,
            layers: [
                RecipeLayer(ingredientId: "ice", name: "冰块", colorHex: "DDE6E3", kind: .ice, grams: 60, alpha: 0.55),
                RecipeLayer(ingredientId: "soda", name: "气泡水", colorHex: "E3E9E5", kind: .sparkling, grams: 100, alpha: 0.55),
                RecipeLayer(ingredientId: "coldbrew", name: "冷萃", colorHex: "3B2414", kind: .liquid, grams: 60, alpha: 1)
            ],
            rating: 5,
            notes: "荔枝壳与白葡萄的香气被气泡托起来，最后一点焦糖甜留得很长。",
            mouthfeel: "清爽",
            sweetness: "微甜",
            beanGrams: 18,
            stirred: false,
            createdAt: hoursAgo(4)
        )
        let osmanthus = Recipe(
            id: UUID().uuidString,
            name: "桂花燕麦拿铁",
            beanId: guji.id,
            beanName: guji.name,
            layers: [
                RecipeLayer(ingredientId: "osmanthus", name: "桂花糖浆", colorHex: "D9A441", kind: .syrup, grams: 12, alpha: 1),
                RecipeLayer(ingredientId: "espresso", name: "浓缩", colorHex: "2A1708", kind: .liquid, grams: 36, alpha: 1),
                RecipeLayer(ingredientId: "oat", name: "燕麦奶", colorHex: "E8D9BC", kind: .liquid, grams: 150, alpha: 1),
                RecipeLayer(ingredientId: "foam", name: "奶泡", colorHex: "FAF5E9", kind: .foam, grams: 26, alpha: 1)
            ],
            rating: 4,
            notes: "燕麦奶的谷物甜把桂花衬得很温柔，下次奶温再低一点。",
            mouthfeel: "均衡",
            sweetness: "甜感适中",
            beanGrams: 16,
            stirred: false,
            createdAt: hoursAgo(26)
        )
        let dirty = Recipe(
            id: UUID().uuidString,
            name: "Guji Dirty",
            beanId: guji.id,
            beanName: guji.name,
            layers: [
                RecipeLayer(ingredientId: "milk", name: "牛奶", colorHex: "F1EADB", kind: .liquid, grams: 150, alpha: 1),
                RecipeLayer(ingredientId: "espresso", name: "浓缩", colorHex: "2A1708", kind: .liquid, grams: 40, alpha: 1)
            ],
            rating: 4,
            notes: "不搅拌，第一口是热的浓缩，第二口撞进冰牛奶里。",
            mouthfeel: "浓郁",
            sweetness: "微甜",
            beanGrams: 18,
            stirred: false,
            createdAt: hoursAgo(76)
        )

        return AppData(
            schemaVersion: 1,
            beans: [lychee, guji],
            recipes: [lycheeSoda, osmanthus, dirty],
            customIngredients: []
        )
    }
}
