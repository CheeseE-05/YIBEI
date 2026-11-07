package com.brewkin.beanbook

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brewkin.beanbook.data.AppState
import com.brewkin.beanbook.data.BackupManager
import com.brewkin.beanbook.data.Bean
import com.brewkin.beanbook.data.BeanRepository
import com.brewkin.beanbook.data.CUP_CAPACITY
import com.brewkin.beanbook.data.CustomIngredient
import com.brewkin.beanbook.data.DatabaseImportMode
import com.brewkin.beanbook.data.IngredientSpec
import com.brewkin.beanbook.data.Recipe
import com.brewkin.beanbook.data.RecipeCardConfig
import com.brewkin.beanbook.data.RecipeLayer
import com.brewkin.beanbook.data.SettingsStore
import com.brewkin.beanbook.data.TransferOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class MakerDraft(
    val layers: List<RecipeLayer> = emptyList(),
    val beanId: String? = null,
    val stirred: Boolean = false
) {
    val totalGrams: Int get() = layers.sumOf { it.grams }
    val remaining: Int get() = (CUP_CAPACITY - totalGrams).coerceAtLeast(0)
    val isEmpty: Boolean get() = layers.isEmpty()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BeanRepository(application)

    /** 1.3 设置：AI 配置 + 外观模式（SharedPreferences 持久化）。 */
    val settings = SettingsStore(application)

    val state: StateFlow<AppState> = repository.state

    private val _draft = MutableStateFlow(
        MakerDraft(beanId = repository.state.value.recentBeans(1).firstOrNull()?.id)
    )
    val draft: StateFlow<MakerDraft> = _draft.asStateFlow()

    /** 当前正在编辑的配方；非 null 时 Maker 处于「修改配方」态，保存会更新它而不是新增。 */
    private val _editingRecipe = MutableStateFlow<Recipe?>(null)
    val editingRecipe: StateFlow<Recipe?> = _editingRecipe.asStateFlow()

    /** 进入编辑前那一杯的草稿，「取消编辑」时原样恢复。 */
    private var draftBeforeEditing: MakerDraft? = null

    fun beanImageFile(path: String?) = repository.beanImageFile(path)

    fun saveBeanImage(bitmap: android.graphics.Bitmap) = repository.saveBeanImage(bitmap)

    fun pour(spec: IngredientSpec, requested: Int): Int {
        val current = _draft.value
        val allowed = current.remaining
        if (allowed <= 0 || requested <= 0) return 0
        val amount = requested.coerceAtMost(allowed)
        val last = current.layers.lastOrNull()
        val layers = if (last != null && last.ingredientId == spec.id) {
            current.layers.dropLast(1) + last.copy(grams = last.grams + amount)
        } else {
            current.layers + RecipeLayer(
                ingredientId = spec.id,
                name = spec.name,
                colorHex = spec.colorHex,
                kind = spec.kind,
                grams = amount,
                alpha = spec.alpha
            )
        }
        _draft.value = current.copy(layers = layers, stirred = false)
        return amount
    }

    fun undo() {
        _draft.update { draft ->
            if (draft.layers.isEmpty()) draft else draft.copy(layers = draft.layers.dropLast(1), stirred = false)
        }
    }

    fun clearCup() {
        _draft.update { it.copy(layers = emptyList(), stirred = false) }
    }

    fun toggleStir() {
        _draft.update { it.copy(stirred = !it.stirred) }
    }

    fun selectBean(beanId: String?) {
        _draft.update { it.copy(beanId = beanId) }
    }

    fun editLayer(index: Int, grams: Int) {
        _draft.update { draft ->
            if (index !in draft.layers.indices) return@update draft
            val others = draft.layers.sumOf { it.grams } - draft.layers[index].grams
            val clamped = grams.coerceIn(1, (CUP_CAPACITY - others).coerceAtLeast(1))
            val layers = draft.layers.toMutableList()
            layers[index] = layers[index].copy(grams = clamped)
            draft.copy(layers = layers, stirred = false)
        }
    }

    fun removeLayer(index: Int) {
        _draft.update { draft ->
            if (index !in draft.layers.indices) draft
            else draft.copy(layers = draft.layers.filterIndexed { i, _ -> i != index }, stirred = false)
        }
    }

    fun loadIntoMaker(recipe: Recipe) {
        // 「再调一杯」是明确的另起一杯，退出任何编辑态（不恢复编辑前的草稿）。
        clearEditingSession()
        _draft.value = MakerDraft(
            layers = recipe.layers,
            beanId = recipe.beanId,
            stirred = recipe.stirred
        )
    }

    /** 从配方详情进入 Maker 编辑态：把这一杯载入草稿，并记住编辑前的草稿。 */
    fun beginEditRecipe(recipe: Recipe) {
        if (_editingRecipe.value?.id != recipe.id) {
            draftBeforeEditing = _draft.value
            _editingRecipe.value = recipe
        }
        _draft.value = MakerDraft(
            layers = recipe.layers,
            beanId = recipe.beanId,
            stirred = recipe.stirred
        )
    }

    /** 「取消编辑」：放弃这一轮的改动，草稿恢复成进入编辑前的样子。 */
    fun cancelEditRecipe() {
        if (_editingRecipe.value == null) return
        val restore = draftBeforeEditing
        clearEditingSession()
        restore?.let { _draft.value = it }
    }

    private fun clearEditingSession() {
        _editingRecipe.value = null
        draftBeforeEditing = null
    }

    fun previewBeanForMaker(bean: Bean) {
        // 从豆子详情进 Maker 同样视作另起一杯，避免误把新豆子写进正在编辑的配方。
        _editingRecipe.value = null
        draftBeforeEditing = null
        _draft.update { it.copy(beanId = bean.id) }
    }

    fun addBean(bean: Bean, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.update { it.copy(beans = listOf(bean) + it.beans) }
            onDone?.invoke()
        }
    }

    fun saveBean(bean: Bean, newImage: android.graphics.Bitmap?, onDone: (Bean) -> Unit) {
        val previous = state.value.beanById(bean.id)
        viewModelScope.launch {
            val path = if (newImage != null) {
                withContext(Dispatchers.IO) { repository.saveBeanImage(newImage) } ?: bean.imagePath
            } else bean.imagePath
            val finalBean = bean.copy(imagePath = path)
            repository.update { state ->
                val beans = if (previous == null) {
                    listOf(finalBean) + state.beans
                } else {
                    state.beans.map { if (it.id == finalBean.id) finalBean else it }
                }
                state.copy(beans = beans)
            }
            if (previous?.imagePath != null && previous.imagePath != path) {
                repository.deleteBeanImage(previous.imagePath)
            }
            onDone(finalBean)
        }
    }

    fun updateBean(bean: Bean, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.update { state ->
                state.copy(beans = state.beans.map { if (it.id == bean.id) bean else it })
            }
            onDone?.invoke()
        }
    }

    fun deleteBean(beanId: String) {
        val bean = state.value.beanById(beanId) ?: return
        viewModelScope.launch {
            repository.update { state -> state.copy(beans = state.beans.filterNot { it.id == beanId }) }
            repository.deleteBeanImage(bean.imagePath)
            if (_draft.value.beanId == beanId) {
                _draft.update { it.copy(beanId = null) }
            }
        }
    }

    fun addCustomIngredient(name: String, colorHex: String, kind: com.brewkin.beanbook.data.IngredientKind) {
        val ingredient = CustomIngredient(
            id = "custom_${UUID.randomUUID()}",
            name = name.trim(),
            colorHex = colorHex,
            kind = kind
        )
        if (ingredient.name.isBlank()) return
        viewModelScope.launch {
            repository.update { it.copy(customIngredients = it.customIngredients + ingredient) }
        }
    }

    fun deleteCustomIngredient(id: String) {
        viewModelScope.launch {
            repository.update { it.copy(customIngredients = it.customIngredients.filterNot { item -> item.id == id }) }
        }
    }

    fun saveRecipe(
        name: String,
        beanId: String?,
        rating: Int,
        notes: String,
        mouthfeel: String,
        sweetness: String,
        beanGrams: Int,
        price: Int?,
        onSaved: (String) -> Unit
    ) {
        val layers = _draft.value.layers
        if (layers.isEmpty()) return
        clearEditingSession()
        val bean = state.value.beanById(beanId)
        val recipe = Recipe(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { layers.lastOrNull()?.name?.let { "一杯$it" } ?: "今日特调" },
            beanId = bean?.id,
            beanName = bean?.name ?: "",
            layers = layers,
            rating = rating.coerceIn(1, 5),
            notes = notes.trim(),
            mouthfeel = mouthfeel,
            sweetness = sweetness,
            beanGrams = beanGrams.coerceAtLeast(0),
            stirred = _draft.value.stirred,
            createdAt = System.currentTimeMillis(),
            price = price?.takeIf { it > 0 }
        )
        viewModelScope.launch {
            repository.update { state ->
                val updatedBeans = state.beans.map { existing ->
                    if (existing.id == beanId) {
                        existing.copy(
                            remainingGrams = (existing.remainingGrams - beanGrams).coerceAtLeast(0.0),
                            lastUsedAt = System.currentTimeMillis()
                        )
                    } else existing
                }
                state.copy(recipes = listOf(recipe) + state.recipes, beans = updatedBeans)
            }
            _draft.value = MakerDraft(beanId = repository.state.value.recentBeans(1).firstOrNull()?.id)
            onSaved(recipe.id)
        }
    }

    fun deleteRecipe(id: String) {
        viewModelScope.launch {
            repository.update { state -> state.copy(recipes = state.recipes.filterNot { it.id == id }) }
            // 正在编辑的这一杯被删掉时，退回编辑前的草稿，避免之后保存到不存在的配方上。
            if (_editingRecipe.value?.id == id) cancelEditRecipe()
        }
    }

    /** 用同一 id 覆盖原配方（编辑保存），并回调以便页面跳转/刷新。 */
    /** 1.3：只更新配方菜单价格（卡片编辑器/列表快捷修改）。 */
    fun updateRecipePrice(recipeId: String, price: Int?) {
        viewModelScope.launch {
            repository.update { state ->
                state.copy(recipes = state.recipes.map {
                    if (it.id == recipeId) it.copy(price = price?.takeIf { p -> p > 0 }) else it
                })
            }
        }
    }

    /** 1.3：把随杯卡配置写回配方（旧数据 card=null，序列化兼容）。 */
    fun updateRecipeCard(recipeId: String, card: RecipeCardConfig) {
        viewModelScope.launch {
            repository.update { state ->
                state.copy(recipes = state.recipes.map { if (it.id == recipeId) it.copy(card = card) else it })
            }
        }
    }

    /** 把相册选择的封面图存进 App 私有目录，返回可持久化的相对路径。 */
    fun saveCardCoverImage(bitmap: android.graphics.Bitmap, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(getApplication<Application>().filesDir, "card_images").apply { mkdirs() }
                    val file = File(dir, "card_${UUID.randomUUID()}.png")
                    file.outputStream().use { out ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                    }
                    "card_images/${file.name}"
                }.getOrNull()
            }
            onDone(path)
        }
    }

    fun cardCoverImageFile(path: String?): File? {
        if (path.isNullOrBlank()) return null
        val file = File(getApplication<Application>().filesDir, path)
        return file.takeIf { it.exists() }
    }

    fun updateRecipe(recipe: Recipe, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.update { state ->
                state.copy(recipes = state.recipes.map { if (it.id == recipe.id) recipe else it })
            }
            onDone?.invoke()
        }
    }

    /**
     * Maker 编辑态保存：保留原配方的 id 与 createdAt，只覆盖这一次改过的内容。
     * 不做「再扣一次豆仓克数」——编辑配方不应重复扣豆。
     */
    fun saveEditedRecipe(
        name: String,
        beanId: String?,
        rating: Int,
        notes: String,
        mouthfeel: String,
        sweetness: String,
        beanGrams: Int,
        price: Int?,
        onSaved: (String) -> Unit
    ) {
        val original = _editingRecipe.value ?: return
        val layers = _draft.value.layers
        if (layers.isEmpty()) return
        val bean = state.value.beanById(beanId)
        val updated = original.copy(
            name = name.trim().ifBlank { original.name },
            beanId = bean?.id,
            beanName = bean?.name ?: "",
            layers = layers,
            rating = rating.coerceIn(1, 5),
            notes = notes.trim(),
            mouthfeel = mouthfeel,
            sweetness = sweetness,
            beanGrams = beanGrams.coerceAtLeast(0),
            stirred = _draft.value.stirred,
            price = price?.takeIf { it > 0 }
        )
        updateRecipe(updated) {
            clearEditingSession()
            _draft.value = MakerDraft(beanId = repository.state.value.recentBeans(1).firstOrNull()?.id)
            onSaved(updated.id)
        }
    }

    fun addSampleRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.update { state -> state.copy(recipes = listOf(recipe) + state.recipes) }
        }
    }

    fun exportDatabase(uri: Uri, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { BackupManager.exportDatabase(getApplication(), state.value, uri) }
            onResult(result)
        }
    }

    fun importDatabase(
        uri: Uri,
        mode: DatabaseImportMode,
        onResult: (Result<TransferOutcome>) -> Unit
    ) {
        viewModelScope.launch {
            val result = runCatching {
                val (next, outcome) = BackupManager.importDatabase(getApplication(), uri, state.value, mode)
                repository.update { next }
                resetDraftBeanIfMissing()
                outcome
            }
            onResult(result)
        }
    }

    fun importRecipeFile(uri: Uri, onResult: (Result<TransferOutcome>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                val (next, outcome) = BackupManager.importRecipeShare(getApplication(), uri, state.value)
                repository.update { next }
                resetDraftBeanIfMissing()
                outcome
            }
            onResult(result)
        }
    }

    fun createRecipeShareFile(recipeId: String, onResult: (Result<File>) -> Unit) {
        val recipe = state.value.recipeById(recipeId)
        if (recipe == null) {
            onResult(Result.failure(IllegalArgumentException("找不到这杯配方。")))
            return
        }
        val bean = state.value.beanById(recipe.beanId)
        viewModelScope.launch {
            val result = runCatching {
                BackupManager.createRecipeShareFile(
                    getApplication(),
                    recipe,
                    bean,
                    state.value.customIngredients
                )
            }
            onResult(result)
        }
    }

    private fun resetDraftBeanIfMissing() {
        val current = state.value
        if (current.beanById(_draft.value.beanId) == null) {
            _draft.update { it.copy(beanId = current.recentBeans(1).firstOrNull()?.id) }
        }
        val editing = _editingRecipe.value
        if (editing != null && current.recipeById(editing.id) == null) {
            clearEditingSession()
        }
    }
}
