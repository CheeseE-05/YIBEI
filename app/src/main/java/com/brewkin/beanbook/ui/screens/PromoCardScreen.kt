package com.brewkin.beanbook.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brewkin.beanbook.cutout.BackgroundRemover
import com.brewkin.beanbook.data.AiCardService
import com.brewkin.beanbook.data.Bean
import com.brewkin.beanbook.data.CardCopy
import com.brewkin.beanbook.data.CardFontSizes
import com.brewkin.beanbook.data.CardPrinter
import com.brewkin.beanbook.data.ElementPlacement
import com.brewkin.beanbook.data.AiIllustrationProcessor
import com.brewkin.beanbook.data.AiVisionService
import com.brewkin.beanbook.data.GallerySaver
import com.brewkin.beanbook.data.ImageGenService
import com.brewkin.beanbook.data.ImageProvider
import com.brewkin.beanbook.data.ImageSizes
import com.brewkin.beanbook.data.ImageUtils
import com.brewkin.beanbook.data.Recipe
import com.brewkin.beanbook.data.RecipeCardConfig
import com.brewkin.beanbook.data.SettingsStore
import com.brewkin.beanbook.ui.card.CardElement
import com.brewkin.beanbook.ui.card.CardLayoutEngine
import com.brewkin.beanbook.ui.card.CardRenderer
import com.brewkin.beanbook.ui.components.AlertTriangleIcon
import com.brewkin.beanbook.ui.components.AlignCenterIcon
import com.brewkin.beanbook.ui.components.AlignLeftIcon
import com.brewkin.beanbook.ui.components.AlignRightIcon
import com.brewkin.beanbook.ui.components.BackIcon
import com.brewkin.beanbook.ui.components.BeanIcon
import com.brewkin.beanbook.ui.components.BrewTextField
import com.brewkin.beanbook.ui.components.BrushIcon
import com.brewkin.beanbook.ui.components.CheckCircleIcon
import com.brewkin.beanbook.ui.components.CheckIcon
import com.brewkin.beanbook.ui.components.CloseIcon
import com.brewkin.beanbook.ui.components.ColorWheelIcon
import com.brewkin.beanbook.ui.components.CupIcon
import com.brewkin.beanbook.ui.components.DownloadTrayIcon
import com.brewkin.beanbook.ui.components.EyeIcon
import com.brewkin.beanbook.ui.components.EyeOffIcon
import com.brewkin.beanbook.ui.components.HandIcon
import com.brewkin.beanbook.ui.components.Overline
import com.brewkin.beanbook.ui.components.PhotoImportIcon
import com.brewkin.beanbook.ui.components.ResetIcon
import com.brewkin.beanbook.ui.components.ShareIcon
import com.brewkin.beanbook.ui.components.SlidersIcon
import com.brewkin.beanbook.ui.components.SparkIcon
import com.brewkin.beanbook.ui.components.ZoomInIcon
import com.brewkin.beanbook.ui.components.ZoomOutIcon
import com.brewkin.beanbook.ui.theme.BrewColors
import com.brewkin.beanbook.ui.theme.DMMono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import java.io.File
import java.io.FileOutputStream

/**
 * 随杯卡编辑器 v3（iOS 1.2 同款页面结构）。
 *
 * 顶部栏（返回 / 宣传卡片 / ✓保存）→ 提示条 → LIVE PREVIEW（预览 + 尺寸说明 + 编辑布局面板）
 * → 两行工具栏（设计工具 / AI 插画 / 导入图片 / AI 文案；编辑布局 / 导出到相册 / 分享打印）
 * → 导出尺寸选择 → 保存到配方 → 「怎么用」卡片。
 * 设计工具 / AI 插画 / AI 文案均为底部 sheet；全元素字段编辑、显示隐藏、拖拽/碰撞回退、
 * 每元素字号 8–72pt + 对齐、图片缩放 50–200%、横竖版独立持久化。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromoCardScreen(
    recipe: Recipe,
    bean: Bean?,
    beanImageFile: File?,
    settings: SettingsStore,
    onBack: () -> Unit,
    onSaveConfig: (RecipeCardConfig) -> Unit,
    onUpdatePrice: (Int?) -> Unit,
    onSaveCoverImage: (Bitmap, (String?) -> Unit) -> Unit,
    onLoadCoverFile: (String?) -> File?,
    onOpenSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val aiConfig by settings.aiConfig.collectAsStateWithLifecycle()
    val imageConfig by settings.imageConfig.collectAsStateWithLifecycle()
    val visionConfig by settings.visionConfig.collectAsStateWithLifecycle()

    // —— 初始配置（复用旧数据；旧字段缺省时按 iOS 默认值补全） ——
    val initial: RecipeCardConfig = remember(recipe.id) {
        recipe.card ?: RecipeCardConfig(
            title = recipe.name,
            subtitle = defaultSubtitle(recipe, bean),
            copy = CardCopy(chineseName = recipe.name),
            tags = CardLayoutEngine.autoFlavorKeywords(recipe, bean).take(4),
            themeColorHex = "#A3CD39",
            template = "LIGHT",
            coverKind = "CUP",
            orientation = "PORTRAIT",
            exportSize = CardRenderer.EXPORT_PRINT_A6
        )
    }

    var template by remember { mutableStateOf(initial.template.ifBlank { "LIGHT" }) }
    var themeHex by remember { mutableStateOf(initial.themeColorHex.ifBlank { "A3CD39" }) }
    var orientation by remember { mutableStateOf(initial.orientation.ifBlank { "PORTRAIT" }) }
    var exportSize by remember {
        mutableStateOf(initial.exportSize.ifBlank { CardRenderer.EXPORT_SOCIAL })
    }
    var coverKind by remember { mutableStateOf(initial.coverKind.ifBlank { "CUP" }) }
    var coverImagePath by remember { mutableStateOf(initial.coverImagePath) }
    var illustrationPath by remember { mutableStateOf(initial.illustrationPath) }
    var illustrationRawPath by remember { mutableStateOf(initial.illustrationRawPath) }
    var photoPromptImagePath by remember { mutableStateOf(initial.photoPromptImagePath) }
    var promptSource by remember { mutableStateOf(initial.promptSource) }
    var photoPromptBusy by remember { mutableStateOf(false) }
    var photoPromptStatus by remember { mutableStateOf<String?>(null) }
    var photoDescription by remember { mutableStateOf("") }
    var illustrationSource by remember { mutableStateOf(initial.illustrationSource) }
    var illustrationPrompt by remember { mutableStateOf(initial.illustrationPrompt) }
    var importedImagePath by remember { mutableStateOf(initial.importedImagePath) }
    var importedOriginalImagePath by remember { mutableStateOf(initial.importedOriginalImagePath) }
    var illustrationShadow by remember { mutableStateOf(initial.illustrationShadow) }
    var priceText by remember { mutableStateOf(recipe.price?.takeIf { it > 0 }?.toString() ?: "") }
    var copy by remember { mutableStateOf(initial.copy) }
    var subtitle by remember {
        mutableStateOf(initial.subtitle.ifBlank { defaultSubtitle(recipe, bean) })
    }
    var tagsText by remember {
        mutableStateOf(
            initial.tags.ifEmpty {
                CardLayoutEngine.autoFlavorKeywords(recipe, bean).take(4)
            }.joinToString("，")
        )
    }
    var hidden by remember { mutableStateOf(initial.hidden) }
    var landscapeLayout by remember { mutableStateOf(initial.landscapeLayout) }
    var portraitLayout by remember { mutableStateOf(initial.portraitLayout) }
    var fontSizes by remember { mutableStateOf(initial.fontSizes) }
    var aiPrompt by remember { mutableStateOf(initial.aiPrompt) }

    // —— 页面 UI 状态 ——
    var banner by remember { mutableStateOf<String?>(null) }
    var editLayout by remember { mutableStateOf(false) }
    var selectedElement by remember { mutableStateOf<String?>(null) }
    var dragElementId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragConflict by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf<String?>(null) }
    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }
    var shareDialog by remember { mutableStateOf(false) }
    var cutoutFailed by remember { mutableStateOf(false) }
    var showDesign by remember { mutableStateOf(false) }
    var showIllustration by remember { mutableStateOf(false) }
    var showAiCopy by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var preparingShare by remember { mutableStateOf(false) }
    var cutoutBusy by remember { mutableStateOf(false) }
    var imageBusy by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf<String?>(null) }
    var imageStatus by remember { mutableStateOf<String?>(null) }
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var aiBusy by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var aiSummary by remember { mutableStateOf<List<Pair<String, String>>?>(null) }

    val designListState = rememberLazyListState()
    val mainListState = rememberLazyListState()

    val tags = remember(tagsText) { parseTags(tagsText).take(4) }
    val coverKinds = remember { listOf("ILLUSTRATION", "IMPORTED", "CUP", "BEAN") }

    // —— 图片资源 ——
    val coverBitmap by produceState<Bitmap?>(null, coverImagePath) {
        value = withContext(Dispatchers.IO) {
            onLoadCoverFile(coverImagePath)?.let { file ->
                runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
            }
        }
    }
    val illustrationBitmap by produceState<Bitmap?>(null, illustrationPath) {
        value = withContext(Dispatchers.IO) {
            onLoadCoverFile(illustrationPath)?.let { file ->
                runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
            }
        }
    }
    val photoPromptBitmap by produceState<Bitmap?>(null, photoPromptImagePath) {
        value = withContext(Dispatchers.IO) {
            onLoadCoverFile(photoPromptImagePath)?.let { file ->
                runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
            }
        }
    }
    val importedBitmap by produceState<Bitmap?>(null, importedImagePath) {
        value = withContext(Dispatchers.IO) {
            onLoadCoverFile(importedImagePath)?.let { file ->
                runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
            }
        }
    }
    val beanBitmap by produceState<Bitmap?>(null, beanImageFile) {
        value = withContext(Dispatchers.IO) {
            beanImageFile?.let { file -> runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull() }
        }
    }

    val exportCardSize = remember(orientation, exportSize) {
        CardRenderer.exportSize(orientation, exportSize)
    }
    val previewCardSize = remember(orientation, exportSize) {
        CardRenderer.previewSize(orientation, exportSize)
    }
    val cardRatio = remember(exportCardSize) {
        exportCardSize.width.toFloat() / exportCardSize.height.toFloat()
    }

    fun snapshot(): RecipeCardConfig = RecipeCardConfig(
        template = template,
        themeColorHex = themeHex,
        title = copy.chineseName.ifBlank { recipe.name }.trim(),
        subtitle = subtitle.trim(),
        tags = tags,
        coverKind = coverKind,
        coverImagePath = coverImagePath,
        orientation = orientation,
        illustrationPath = illustrationPath,
        illustrationRawPath = illustrationRawPath,
        photoPromptImagePath = photoPromptImagePath,
        promptSource = promptSource,
        importedImagePath = importedImagePath,
        importedOriginalImagePath = importedOriginalImagePath,
        illustrationSource = illustrationSource,
        illustrationPrompt = illustrationPrompt,
        illustrationShadow = illustrationShadow,
        exportSize = exportSize,
        brand = "忆杯手记",
        copy = copy,
        hidden = hidden,
        landscapeLayout = landscapeLayout,
        portraitLayout = portraitLayout,
        fontSizes = fontSizes,
        aiPrompt = aiPrompt.trim(),
        updatedAt = System.currentTimeMillis()
    )

    // updatedAt 不参与渲染/比较，避免每次重组都触发重绘。
    val config = snapshot().copy(updatedAt = 0L)
    val priceValue = priceText.toIntOrNull()?.takeIf { it > 0 }

    val preview by produceState<Bitmap?>(
        null,
        config,
        coverBitmap,
        beanBitmap,
        illustrationBitmap,
        importedBitmap,
        previewCardSize
    ) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                CardRenderer.render(
                    recipe = recipe,
                    config = config,
                    coverBitmap = coverBitmap,
                    beanBitmap = beanBitmap,
                    illustrationBitmap = illustrationBitmap,
                    importedBitmap = importedBitmap,
                    size = previewCardSize,
                    bean = bean,
                    priceOverride = priceValue
                )
            }.onFailure { android.util.Log.e("PromoRender", "render failed", it) }.getOrNull()
        }
    }

    val layout = remember(config, previewCardSize) {
        CardLayoutEngine.layout(recipe, config, previewCardSize, bean, priceValue)
    }

    /** 当前导出尺寸下的真实排版（用于保存/导出前的遮挡拦截）。 */
    fun conflictsFor(cfg: RecipeCardConfig, size: CardRenderer.CardSize): Boolean =
        CardLayoutEngine.layout(recipe, cfg, size, bean, priceValue).hasCollision

    fun hasConflict(cfg: RecipeCardConfig = snapshot()): Boolean = conflictsFor(cfg, exportCardSize)

    val currentExportSize = CardRenderer.exportSize(orientation, exportSize)
    val conflict = remember(config, currentExportSize) { conflictsFor(config, currentExportSize) }

    fun activeMap(): Map<String, ElementPlacement> =
        if (orientation == "PORTRAIT") portraitLayout else landscapeLayout

    fun commitMap(map: Map<String, ElementPlacement>) {
        if (orientation == "PORTRAIT") portraitLayout = map else landscapeLayout = map
    }

    fun commitFontSizes(map: Map<String, Float>) {
        fontSizes = if (orientation == "PORTRAIT") {
            fontSizes.copy(portrait = map)
        } else {
            fontSizes.copy(landscape = map)
        }
    }

    /** 第一次手动编辑时把当前自动排版的真实位置冻结成手动布局（其余元素保持原位）。 */
    fun freezeAutoLayout() {
        if (activeMap().isNotEmpty()) return
        val size = CardRenderer.exportSize(orientation, exportSize)
        val auto = CardLayoutEngine.layout(recipe, snapshot(), size, bean, priceValue)
        val map = mutableMapOf<String, ElementPlacement>()
        auto.boxes.forEach { (id, box) ->
            map[id] = ElementPlacement(
                x = (box.left / auto.width).coerceIn(0f, 1f),
                y = (box.top / auto.height).coerceIn(0f, 1f),
                scale = 1f,
                align = "AUTO",
                fontSizePt = 0f,
                width = (box.width() / auto.width).coerceIn(0.01f, 1f),
                height = (box.height() / auto.height).coerceIn(0.005f, 1f)
            )
        }
        if (map.isNotEmpty()) commitMap(map)
    }

    fun updatePlacement(id: String, transform: (ElementPlacement) -> ElementPlacement) {
        freezeAutoLayout()
        val map = activeMap().toMutableMap()
        val current = map[id] ?: ElementPlacement(0.05f, 0.05f)
        map[id] = transform(current)
        commitMap(map)
    }

    fun setAlignment(id: String, align: String) {
        updatePlacement(id) { it.copy(align = align) }
    }

    fun adjustScale(id: String, delta: Float) {
        updatePlacement(id) { it.copy(scale = (it.scale + delta).coerceIn(0.5f, 2f)) }
    }

    fun currentFontPt(id: String): Float = CardLayoutEngine.effectiveFontPt(
        id, snapshot(), exportCardSize.width.toFloat(), exportCardSize.height.toFloat()
    )

    fun setFontSize(id: String, pt: Float) {
        if (id == CardElement.ILLUSTRATION) return
        val map = fontSizes.forOrientation(orientation).toMutableMap()
        map[id] = pt.coerceIn(8f, 72f)
        commitFontSizes(map)
    }

    fun resetFontSize(id: String) {
        val map = fontSizes.forOrientation(orientation).toMutableMap()
        map.remove(id)
        commitFontSizes(map)
    }

    fun toggleHidden(id: String) {
        hidden = if (id in hidden) hidden - id else hidden + id
        if (id in hidden && selectedElement == id) selectedElement = null
    }

    fun restoreAutoLayout() {
        if (orientation == "PORTRAIT") portraitLayout = emptyMap() else landscapeLayout = emptyMap()
        selectedElement = null
        dragElementId = null
        dragOffset = Offset.Zero
        dragConflict = false
        editLayout = false
        banner = "已恢复自动排版：按真实文本高度流式重排，元素不会互相遮挡。"
    }

    fun switchOrientation(next: String) {
        if (next == orientation) return
        orientation = next
        // iOS：切换横竖版自动清空手动排版并重排；字号按方向分别保留。
        landscapeLayout = emptyMap()
        portraitLayout = emptyMap()
        selectedElement = null
        dragElementId = null
        dragOffset = Offset.Zero
        banner = "切换横竖版后已自动重排。"
    }

    fun switchTemplate(next: String) {
        if (next == template) return
        template = next
        // iOS：切换模板后自动清空手动排版并重排。
        landscapeLayout = emptyMap()
        portraitLayout = emptyMap()
        selectedElement = null
        dragElementId = null
        dragOffset = Offset.Zero
        banner = "切换模板后已自动重排。"
    }

    fun saveToRecipe() {
        if (conflictsFor(snapshot(), CardRenderer.exportSize(orientation, exportSize))) {
            banner = "元素有重叠，请先拖开或恢复自动排版，再保存/导出。"
            return
        }
        val cfg = snapshot()
        onSaveConfig(cfg)
        onUpdatePrice(priceValue)
        val pricePart = priceValue?.let { " · 菜单价格 ¥$it" } ?: ""
        banner = "已保存到「${recipe.name}」$pricePart"
    }

    fun renderExport(): Bitmap? {
        // 注意：这里按当前 state 重新取尺寸（调试脚本等场景下闭包可能早于状态更新）。
        val size = CardRenderer.exportSize(orientation, exportSize)
        return runCatching {
            CardRenderer.render(
                recipe = recipe,
                config = snapshot().copy(updatedAt = 0L),
                coverBitmap = coverBitmap,
                beanBitmap = beanBitmap,
                illustrationBitmap = illustrationBitmap,
                importedBitmap = importedBitmap,
                size = size,
                bean = bean,
                priceOverride = priceValue
            )
        }.getOrNull()
    }

    fun doExportInternal() {
        if (exporting) return
        if (conflictsFor(snapshot(), CardRenderer.exportSize(orientation, exportSize))) {
            banner = "元素有重叠，请先拖开或恢复自动排版，再保存/导出。"
            return
        }
        exporting = true
        saving = true
        scope.launch {
            val bitmap = withContext(Dispatchers.Default) { renderExport() }
            if (bitmap == null) {
                exporting = false
                saving = false
                dialogMessage = "导出失败：渲染失败，请重试。"
                return@launch
            }
            val fileName = "beanbook_card_${System.currentTimeMillis()}.png"
            val result = withContext(Dispatchers.IO) { GallerySaver.savePng(context, bitmap, fileName) }
            exporting = false
            saving = false
            result.onSuccess {
                onSaveConfig(snapshot())
                onUpdatePrice(priceValue)
                val savedSize = CardRenderer.exportSize(orientation, exportSize)
                exportSuccessMessage =
                    "「${copy.chineseName.ifBlank { recipe.name }}」的随杯卡已保存到相册" +
                        "（${savedSize.width}×${savedSize.height} px），可以直接分享或打印。"
            }.onFailure { dialogMessage = "导出失败：${it.message}" }
        }
    }

    val exportPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) doExportInternal() else dialogMessage = "没有存储权限，无法保存到相册。"
    }

    fun doExport() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            exportPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            doExportInternal()
        }
    }

    fun shareFile(bitmap: Bitmap): File? = runCatching {
        val title = copy.chineseName.ifBlank { recipe.name }
            .replace(Regex("[\\\\/:*?\"<>|\\s]"), "_").ifBlank { "card" }
        val label = if (exportSize == CardRenderer.EXPORT_PRINT_A6) "A6-300dpi" else "social"
        val dir = File(context.cacheDir, "card_exports").apply { mkdirs() }
        val file = File(dir, "$title-$label-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        file
    }.getOrNull()

    fun doShare() {
        if (preparingShare) return
        if (conflictsFor(snapshot(), CardRenderer.exportSize(orientation, exportSize))) {
            banner = "元素有重叠，请先拖开或恢复自动排版，再保存/导出。"
            return
        }
        preparingShare = true
        scope.launch {
            val bitmap = withContext(Dispatchers.Default) { renderExport() }
            preparingShare = false
            val file = bitmap?.let { shareFile(it) }
            if (file == null) {
                dialogMessage = "分享失败：生成图片失败，请重试。"
                return@launch
            }
            runCatching {
                val uri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = android.content.ClipData.newRawUri("随杯卡", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(share, "分享随杯卡").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                onSaveConfig(snapshot())
                onUpdatePrice(priceValue)
            }.onFailure { dialogMessage = "分享失败：${it.message}" }
        }
    }

    fun doPrint() {
        if (conflictsFor(snapshot(), CardRenderer.exportSize(orientation, exportSize))) {
            banner = "元素有重叠，请先拖开或恢复自动排版，再保存/导出。"
            return
        }
        scope.launch {
            val bitmap = withContext(Dispatchers.Default) { renderExport() }
            if (bitmap == null) {
                dialogMessage = "打印失败：生成图片失败，请重试。"
                return@launch
            }
            runCatching {
                CardPrinter.print(context, "忆杯手记 · 随杯卡", bitmap)
                onSaveConfig(snapshot())
                onUpdatePrice(priceValue)
            }.onFailure { dialogMessage = "打印失败：${it.message}" }
        }
    }

    // —— 相册导入 + 自动抠图 ——
    fun useOriginalImport() {
        val original = importedOriginalImagePath
        if (original.isNullOrBlank()) {
            dialogMessage = "找不到导入的原图，请重新导入一张。"
            return
        }
        importedImagePath = original
        illustrationSource = "IMPORTED"
        coverKind = "IMPORTED"
        banner = "已改用原图（未抠图），也可以随时重新抠图。"
    }

    fun runCutout(uri: Uri) {
        scope.launch {
            cutoutBusy = true
            banner = null
            val bitmap = withContext(Dispatchers.IO) { ImageUtils.loadBitmap(context, uri, maxSide = 1600) }
            if (bitmap == null) {
                cutoutBusy = false
                dialogMessage = "这张照片读不出来，换一张试试。"
                return@launch
            }
            var originalPath: String? = null
            onSaveCoverImage(bitmap) { path -> originalPath = path }
            // 等原图落盘（回调在主线程异步返回）。
            repeat(50) { if (originalPath == null) delay(60) else return@repeat }
            // ① 图片本身已带透明背景（PNG alpha）→ 直接保留透明，跳过 U²-Net 抠图
            val alreadyTransparent = withContext(Dispatchers.Default) {
                ImageUtils.hasSignificantTransparency(bitmap)
            }
            if (alreadyTransparent) {
                val kept = withContext(Dispatchers.Default) {
                    runCatching { ImageUtils.cropTransparentWithPadding(bitmap) }.getOrDefault(bitmap)
                }
                var keptPath: String? = null
                onSaveCoverImage(kept) { path -> keptPath = path }
                repeat(50) { if (keptPath == null) delay(60) else return@repeat }
                cutoutBusy = false
                val path = keptPath
                if (path != null) {
                    importedImagePath = path
                    importedOriginalImagePath = originalPath
                    illustrationSource = "IMPORTED"
                    coverKind = "IMPORTED"
                    banner = "已使用透明背景导入：保留原始 PNG 透明通道，直接叠在奶油纸上（未触发抠图）。"
                } else {
                    importedOriginalImagePath = originalPath
                    dialogMessage = "透明图片读取成功，但保存到本机失败，请重试。"
                }
                return@launch
            }
            // ② 无透明背景 → 走现有 U²-Net 自动抠图
            val result = withContext(Dispatchers.IO) { BackgroundRemover.removeBackground(context, bitmap) }
            val generated = result.getOrNull()
            if (generated == null) {
                cutoutBusy = false
                importedOriginalImagePath = originalPath
                cutoutFailed = true
                return@launch
            }
            var cutoutPath: String? = null
            onSaveCoverImage(generated) { path -> cutoutPath = path }
            repeat(50) { if (cutoutPath == null) delay(60) else return@repeat }
            cutoutBusy = false
            val path = cutoutPath
            if (path != null) {
                importedImagePath = path
                importedOriginalImagePath = originalPath
                illustrationSource = "IMPORTED"
                coverKind = "IMPORTED"
                banner = "已自动抠图：背景已变透明，可直接叠在奶油纸上（可重新导入或改用原图）。"
            } else {
                importedOriginalImagePath = originalPath
                dialogMessage = "抠图成功但保存失败，请重试。"
            }
        }
    }

    val pickImported = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) runCutout(uri)
    }

    fun launchImportedPicker() {
        pickImported.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    // —— 实物图 → 插画 prompt（图片理解 API；未配置/失败走本地降级）——
    fun generatePromptFromPhoto(uri: Uri) {
        scope.launch {
            photoPromptBusy = true
            photoPromptStatus = null
            imageError = null
            val photo = withContext(Dispatchers.IO) { ImageUtils.loadBitmap(context, uri, maxSide = 1600) }
            if (photo == null) {
                photoPromptBusy = false
                photoPromptStatus = "照片读取失败，换一张试试"
                return@launch
            }
            onSaveCoverImage(photo) { path -> if (path != null) photoPromptImagePath = path }
            var generated: String? = null
            var usedVision = false
            if (visionConfig.isReady) {
                val result = withContext(Dispatchers.IO) {
                    AiVisionService.describeForIllustration(visionConfig, photo, recipe, bean)
                }
                result.onSuccess { text ->
                    if (text.isNotBlank()) {
                        generated = text
                        usedVision = true
                    }
                }
            }
            if (generated.isNullOrBlank()) {
                generated = withContext(Dispatchers.Default) {
                    AiVisionService.localFallbackPrompt(photo, recipe, bean)
                }
            }
            val prompt = generated.orEmpty()
            if (prompt.isBlank()) {
                photoPromptBusy = false
                photoPromptStatus = "没能根据这张照片生成 prompt，换一张试试"
                return@launch
            }
            photoDescription = prompt
            illustrationPrompt = ImageGenService.composePrompt(prompt, recipe, bean, priceValue)
            promptSource = if (usedVision) "PHOTO_VISION" else "PHOTO_LOCAL"
            photoPromptStatus = if (usedVision) {
                "已用图片理解 API 根据实物图生成 prompt（可修改后生成插画）"
            } else {
                "未配置图片理解 API（或调用失败），已按照片主色+配方生成 prompt"
            }
            photoPromptBusy = false
        }
    }

    val photoPromptPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) generatePromptFromPhoto(uri)
    }

    // —— AI 插画 ——
    fun generateIllustration() {
        if (imageBusy) return
        if (!imageConfig.isReady) {
            imageError = "请先到 设置 →「插画生成 API」里配置 Base URL / Key / 模型"
            return
        }
        imageBusy = true
        imageError = null
        imageStatus = null
        generatedBitmap = null
        scope.launch {
            val prompt = illustrationPrompt.trim().ifBlank {
                ImageGenService.buildIllustrationPrompt(recipe, bean, priceValue)
            }
            val oriented = imageConfig.copy(
                size = ImageSizes.forOrientation(orientation, imageConfig.size)
            )
            val result = withContext(Dispatchers.Default) { ImageGenService.generate(oriented, prompt) }
            val raw = result.getOrElse { err ->
                imageBusy = false
                imageError = err.message ?: "插画生成失败，请重试"
                return@launch
            }
            // 1) 保留 raw 原图（可回退/重试，不覆盖）
            onSaveCoverImage(raw) { rawPath -> illustrationRawPath = rawPath }
            // 2) 纯色背景颜色抠图 + 本地去 AI 水印（不使用 U²-Net）
            val processed = withContext(Dispatchers.Default) {
                runCatching { AiIllustrationProcessor.processSync(raw) }.getOrElse { err ->
                    com.brewkin.beanbook.data.AiIllustrationProcessResult(
                        raw, false, 0, "插画后处理失败，已保留原图（${err.message ?: "未知原因"}）"
                    )
                }
            }
            val finalBitmap = if (processed.backgroundKeyed) processed.bitmap else raw
            generatedBitmap = finalBitmap
            onSaveCoverImage(finalBitmap) { path ->
                imageBusy = false
                if (path != null) {
                    illustrationPath = path
                    illustrationSource = "AI"
                    illustrationPrompt = prompt
                    coverKind = "ILLUSTRATION"
                    imageError = null
                    imageStatus = when {
                        processed.backgroundKeyed && processed.watermarkBlobsRemoved > 0 ->
                            "AI 插画已生成：已自动去纯色背景（透明 PNG），并清理 ${processed.watermarkBlobsRemoved} 处 AI 水印/角标"
                        processed.backgroundKeyed ->
                            "AI 插画已生成：已自动去除纯色背景（透明 PNG），设为卡片主视觉"
                        else ->
                            "AI 插画已生成，但${processed.warning ?: "背景不是纯色，未抠图"}；已保留原图"
                    }
                    banner = if (processed.backgroundKeyed) {
                        "AI 插画已生成并套用：${copy.chineseName.ifBlank { recipe.name }} 的透明背景主视觉"
                    } else {
                        "AI 插画已生成并套用（原图）"
                    }
                } else {
                    imageError = "插画已生成，但保存到本机失败"
                }
            }
        }
    }

    // —— AI 文案 ——
    fun generateAiCopy() {
        if (aiBusy) return
        if (!aiConfig.isReady) {
            aiError = "请先到 设置 里配置 AI API"
            return
        }
        aiBusy = true
        aiError = null
        aiSummary = null
        val description = aiPrompt.trim().ifBlank {
            "一杯用 ${recipe.beanName.ifBlank { "咖啡" }} 调制的 ${recipe.name}"
        }
        scope.launch {
            val result = AiCardService.generateCard(
                config = aiConfig,
                description = description,
                recipeName = recipe.name,
                existingTags = tags
            )
            aiBusy = false
            result.onSuccess { draft ->
                if (draft.title.isNotBlank()) copy = copy.copy(chineseName = draft.title)
                if (draft.subtitle.isNotBlank()) subtitle = draft.subtitle
                if (draft.tags.isNotEmpty()) {
                    tagsText = draft.tags.joinToString("，")
                    copy = copy.copy(flavorsChinese = draft.tags.joinToString("、"))
                }
                draft.template?.let { t ->
                    template = when (t.uppercase()) {
                        "DARK", "BLACK", "NIGHT", "POSTER" -> "DARK"
                        "LIME", "GREEN", "MINIMAL" -> "LIME"
                        else -> "LIGHT"
                    }
                }
                draft.themeColorHex?.let { t ->
                    themeHex = when (t.uppercase()) {
                        "LIME" -> "A3CD39"
                        "BUTTER" -> "D7BC62"
                        "ROSE" -> "B77A6A"
                        "SKY" -> "6E8F9D"
                        "INK" -> "26231F"
                        else -> t.removePrefix("#").takeIf { it.length == 6 } ?: themeHex
                    }
                }
                aiSummary = listOf(
                    "标题" to draft.title,
                    "副标题" to draft.subtitle,
                    "标签" to draft.tags.joinToString("、"),
                    "主题" to (draft.themeColorHex ?: themeHex),
                    "模板" to CardRenderer.templateLabel(template)
                )
                banner = "AI 已生成并套用：${draft.title.ifBlank { recipe.name }}"
            }.onFailure { err ->
                aiError = err.message ?: "AI 文案生成失败，请重试"
            }
        }
    }

    // —— 自定义主题色 ——
    var customHue by remember { mutableStateOf(120f) }
    var customSat by remember { mutableStateOf(0.72f) }
    var customVal by remember { mutableStateOf(0.80f) }

    // ———————————————— 页面 ————————————————
    Column(
        Modifier
            .fillMaxSize()
            .background(BrewColors.Paper)
    ) {
        // 顶部栏
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BrewColors.Card)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                BackIcon(Modifier.size(19.dp), BrewColors.Ink)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Overline("PROMO CARD")
                Spacer(Modifier.height(2.dp))
                Text("宣传卡片", style = iosText(18f, FontWeight.SemiBold), color = BrewColors.Ink)
            }
            Row(
                modifier = Modifier
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(BrewColors.Lime)
                    .clickable { saveToRecipe() }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheckIcon(Modifier.size(12.dp), BrewColors.InkSolid)
                Spacer(Modifier.width(5.dp))
                Text("保存", style = iosText(12f, FontWeight.Medium), color = BrewColors.InkSolid)
            }
        }

        LazyColumn(
            state = mainListState,
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 提示条
            banner?.let { text ->
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(15.dp))
                            .background(BrewColors.LimeSoft)
                            .padding(horizontal = 13.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheckCircleIcon(
                            Modifier.size(15.dp),
                            color = BrewColors.Forest,
                            checkColor = BrewColors.LimeSoft
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text,
                            modifier = Modifier.weight(1f),
                            style = iosText(11.5f, FontWeight.Medium),
                            color = BrewColors.InkSoft,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { banner = null },
                            contentAlignment = Alignment.Center
                        ) {
                            CloseIcon(Modifier.size(9.dp), BrewColors.Muted)
                        }
                    }
                }
            }

            // LIVE PREVIEW
            item {
                Column(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Overline("LIVE PREVIEW")
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${CardRenderer.templateLabel(template)} · ${CardRenderer.ratioLabel(orientation)} · " +
                                if (exportSize == CardRenderer.EXPORT_PRINT_A6) "A6 打印" else "社交",
                            style = monoText(9f),
                            color = BrewColors.Muted
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    PreviewCard(
                        bitmap = preview,
                        ratio = cardRatio,
                        layout = layout,
                        editLayout = editLayout,
                        selectedElement = selectedElement,
                        draggingId = dragElementId,
                        dragOffset = dragOffset,
                        dragConflict = dragConflict,
                        onTapElement = { id ->
                            if (editLayout) selectedElement = id
                        },
                        onDragStart = { id ->
                            if (editLayout && id != null) {
                                selectedElement = id
                                freezeAutoLayout()
                                dragElementId = id
                                dragOffset = Offset.Zero
                                dragConflict = false
                            }
                        },
                        onDrag = { amount ->
                            if (dragElementId != null) {
                                dragOffset += amount
                                dragConflict = run {
                                    val id = dragElementId ?: return@run false
                                    val box = layout.boxes[id] ?: return@run false
                                    val moved = android.graphics.RectF(
                                        box.left, box.top, box.right, box.bottom
                                    ).apply { offset(dragOffset.x, dragOffset.y) }
                                    layout.boxes.any { (otherId, other) ->
                                        otherId != id && otherId !in hidden &&
                                            other.left < moved.right && moved.left < other.right &&
                                            other.top < moved.bottom && moved.top < other.bottom
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            val id = dragElementId
                            if (id != null) {
                                val displayW = previewCardSize.width.toFloat()
                                val displayH = previewCardSize.height.toFloat()
                                val box = layout.boxes[id]
                                if (box != null) {
                                    val before = activeMap()
                                    val widthNorm = box.width() / layout.width
                                    val heightNorm = box.height() / layout.height
                                    val nx = ((box.left + dragOffset.x * layout.width / displayW) / layout.width)
                                        .coerceIn(0f, (1f - widthNorm).coerceAtLeast(0f))
                                    val ny = ((box.top + dragOffset.y * layout.height / displayH) / layout.height)
                                        .coerceIn(0f, (1f - heightNorm).coerceAtLeast(0f))
                                    val map = before.toMutableMap()
                                    map[id] = (map[id] ?: ElementPlacement(nx, ny)).copy(x = nx, y = ny)
                                    commitMap(map)
                                    if (hasConflict()) {
                                        commitMap(before)
                                        banner = "元素会互相遮挡，已回退到上一个位置；也可以点「恢复自动排版」。"
                                    }
                                }
                            }
                            dragElementId = null
                            dragOffset = Offset.Zero
                            dragConflict = false
                        },
                        onDragCancel = {
                            dragElementId = null
                            dragOffset = Offset.Zero
                            dragConflict = false
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (exportSize == CardRenderer.EXPORT_PRINT_A6) {
                            "A6 300dpi 打印尺寸：${CardRenderer.exportPixelText(orientation, exportSize)}，浅色纸打印效果最佳"
                        } else {
                            "社交分享尺寸：${CardRenderer.exportPixelText(orientation, exportSize)}，浅色纸打印效果最佳"
                        },
                        style = iosText(9.5f),
                        color = BrewColors.Muted
                    )
                    if (editLayout) {
                        Spacer(Modifier.height(10.dp))
                        LayoutEditPanel(
                            layout = layout,
                            selected = selectedElement,
                            hidden = hidden,
                            conflict = conflict || dragConflict,
                            scaleOf = { id -> activeMap()[id]?.scale ?: 1f },
                            fontPtOf = { id -> currentFontPt(id) },
                            onSelect = { selectedElement = it },
                            onToggleHidden = { toggleHidden(it) },
                            onAlign = { id, align -> setAlignment(id, align) },
                            onScale = { id, delta -> adjustScale(id, delta) },
                            onFontSize = { id, pt -> setFontSize(id, pt) },
                            onResetFont = { id -> resetFontSize(id) },
                            onRestore = { restoreAutoLayout() }
                        )
                    }
                }
            }

            // 工具栏 + 导出尺寸
            item {
                Column(Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToolButton(Modifier.weight(1f), "设计工具") { showDesign = true }
                        ToolButton(Modifier.weight(1f), "AI 插画") {
                            showIllustration = true
                        }
                        ToolButton(Modifier.weight(1f), if (importedImagePath == null) "导入图片" else "换导入图") {
                            launchImportedPicker()
                        }
                        ToolButton(Modifier.weight(1f), "AI 文案") { showAiCopy = true }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToolButton(Modifier.weight(1f), if (editLayout) "完成布局" else "编辑布局", active = editLayout) {
                            editLayout = !editLayout
                            selectedElement = null
                            dragElementId = null
                            dragOffset = Offset.Zero
                        }
                        ToolButton(Modifier.weight(1f), if (exporting) "导出中…" else "导出到相册") {
                            doExport()
                        }
                        ToolButton(Modifier.weight(1f), if (preparingShare) "准备中…" else "分享 / 打印") {
                            shareDialog = true
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CardRenderer.exportSizes().forEach { (id, label) ->
                            ExportSizeChip(label, exportSize == id) { exportSize = id }
                        }
                        Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${CardRenderer.exportCaption(exportSize)} · " +
                            "${CardRenderer.exportPixelText(orientation, exportSize)} · PNG",
                        style = iosText(9.5f),
                        color = BrewColors.Muted
                    )
                }
            }

            // 保存到配方
            item {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(BrewColors.Lime)
                            .clickable { saveToRecipe() },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheckIcon(Modifier.size(15.dp), BrewColors.InkSolid)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "保存到「${recipe.name}」",
                            style = iosText(15f, FontWeight.SemiBold),
                            color = BrewColors.InkSolid,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(
                        "卡片配置会跟着这杯配方一起保存在本机，导出 / 备份数据时一起带走。",
                        style = iosText(10.5f),
                        color = BrewColors.Muted,
                        lineHeight = 16.sp
                    )
                }
            }

            // 怎么用
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(BrewColors.PaperSoft)
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(BrewColors.LimeSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        SparkIcon(Modifier.size(13.dp), BrewColors.Forest)
                    }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text("怎么用", style = iosText(13f, FontWeight.SemiBold), color = BrewColors.Ink)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "先用「设计工具」挑横版/竖版和封面；「AI 插画」会按这杯配方画一张手绘水彩咖啡插画作为主视觉；" +
                                "「AI 文案」写标题/副标题/标签。满意后导出 PNG 到相册，或直接分享/打印成咖啡店随杯卡。",
                            style = iosText(10.5f),
                            color = BrewColors.Muted,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }

    // ———————————————— 设计工具 sheet ————————————————
    if (showDesign) {
        ModalBottomSheet(
            onDismissRequest = { showDesign = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = BrewColors.Card,
            dragHandle = { SheetHandle() }
        ) {
            Column(Modifier.fillMaxWidth()) {
                SheetHeader("设计工具") { showDesign = false }
                LazyColumn(
                    state = designListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 实时预览
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(cardRatio)
                                .shadow(
                                    12.dp,
                                    RoundedCornerShape(8.dp),
                                    clip = false,
                                    ambientColor = Color.Black.copy(alpha = 0.12f),
                                    spotColor = Color.Black.copy(alpha = 0.12f)
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrewColors.PaperSoft)
                        ) {
                            preview?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "卡片预览",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                    // 比例
                    item {
                        Column {
                            IosSectionTitle("比例", "RATIO")
                            Spacer(Modifier.height(9.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CardRenderer.orientations.forEach { o ->
                                    ChoicePill(o.label, orientation == o.id) { switchOrientation(o.id) }
                                }
                            }
                        }
                    }
                    // 模板
                    item {
                        Column {
                            IosSectionTitle("模板", "TEMPLATE")
                            Spacer(Modifier.height(9.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CardRenderer.templates.forEach { t ->
                                    val selected = template == t.id
                                    Column(
                                        Modifier
                                            .weight(1f)
                                            .height(78.dp)
                                            .clip(RoundedCornerShape(15.dp))
                                            .background(if (selected) BrewColors.LimeSoft else BrewColors.PaperSoft)
                                            .border(
                                                1.dp,
                                                if (selected) BrewColors.Lime.copy(alpha = 0.5f) else BrewColors.LineSoft,
                                                RoundedCornerShape(15.dp)
                                            )
                                            .clickable { switchTemplate(t.id) }
                                            .padding(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                t.label,
                                                style = iosText(13f, FontWeight.SemiBold),
                                                color = BrewColors.Ink,
                                                maxLines = 1
                                            )
                                            Spacer(Modifier.weight(1f))
                                            if (selected) {
                                                CheckCircleIcon(
                                                    Modifier.size(13.dp),
                                                    color = BrewColors.Forest,
                                                    checkColor = BrewColors.Card
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(5.dp))
                                        Text(
                                            t.caption,
                                            style = iosText(9f).copy(lineHeight = 11.5.sp),
                                            color = BrewColors.Muted,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // 主题色
                    item {
                        Column {
                            IosSectionTitle("主题色", "THEME COLOR")
                            Spacer(Modifier.height(9.dp))
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CardRenderer.themeOptions.forEach { theme ->
                                    val selected = themeHex.equals(theme.hex, ignoreCase = true)
                                    Box(
                                        Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(hexToColor(theme.hex))
                                            .border(1.dp, BrewColors.Line, CircleShape)
                                            .clickable { themeHex = theme.hex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selected) {
                                            CheckIcon(
                                                Modifier.size(11.dp),
                                                if (isLightHex(theme.hex)) BrewColors.InkSolid else Color.White
                                            )
                                        }
                                    }
                                }
                                Box(
                                    Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, BrewColors.Line, CircleShape)
                                        .clickable {
                                            val existing = hexToColor(themeHex)
                                            val hsv = FloatArray(3)
                                            android.graphics.Color.RGBToHSV(
                                                (existing.red * 255).toInt(),
                                                (existing.green * 255).toInt(),
                                                (existing.blue * 255).toInt(),
                                                hsv
                                            )
                                            customHue = hsv[0]
                                            customSat = hsv[1].coerceIn(0.1f, 1f)
                                            customVal = hsv[2].coerceIn(0.1f, 1f)
                                            showColorPicker = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(Modifier.fillMaxSize().padding(2.dp)) {
                                        drawCircle(
                                            brush = Brush.sweepGradient(
                                                listOf(
                                                    Color.Red, Color.Magenta, Color.Blue, Color.Cyan,
                                                    Color.Green, Color.Yellow, Color.Red
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // 封面
                    item {
                        Column {
                            IosSectionTitle("封面", "COVER")
                            Spacer(Modifier.height(9.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                coverKinds.forEach { kind ->
                                    val selected = coverKind == kind
                                    Column(
                                        Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                            .clip(RoundedCornerShape(15.dp))
                                            .background(if (selected) BrewColors.LimeSoft else BrewColors.PaperSoft)
                                            .border(
                                                1.dp,
                                                if (selected) BrewColors.Lime.copy(alpha = 0.5f) else BrewColors.LineSoft,
                                                RoundedCornerShape(15.dp)
                                            )
                                            .clickable {
                                                when (kind) {
                                                    "ILLUSTRATION" -> {
                                                        coverKind = "ILLUSTRATION"
                                                        if (illustrationPath == null) showIllustration = true
                                                    }
                                                    "IMPORTED" -> {
                                                        if (importedImagePath == null) launchImportedPicker()
                                                        else coverKind = "IMPORTED"
                                                    }
                                                    else -> coverKind = kind
                                                }
                                            }
                                            .padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        CoverKindIcon(kind, selected)
                                        Spacer(Modifier.height(5.dp))
                                        Text(
                                            coverKindLabel(kind),
                                            style = iosText(10f, FontWeight.Medium),
                                            color = BrewColors.InkSoft,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (coverKind == "ILLUSTRATION" && illustrationPath == null) {
                                Spacer(Modifier.height(8.dp))
                                HintBar(
                                    icon = { SparkIcon(Modifier.size(13.dp), BrewColors.Forest) },
                                    text = "还没有 AI 插画：先根据这杯配方生成一张手绘咖啡插画。",
                                    actionLabel = "去生成"
                                ) { showIllustration = true }
                            }
                            if (coverKind == "IMPORTED") {
                                Spacer(Modifier.height(8.dp))
                                ImportedCoverControls(
                                    hasImage = importedImagePath != null,
                                    cutout = illustrationSource == "IMPORTED",
                                    onReimport = { launchImportedPicker() },
                                    onRecut = { launchImportedPicker() },
                                    onUseOriginal = { useOriginalImport() }
                                )
                            }
                            if (coverKind == "IMPORTED" || coverKind == "ILLUSTRATION") {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(BrewColors.PaperSoft)
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "主视觉柔和阴影",
                                            style = iosText(12.5f, FontWeight.Medium),
                                            color = BrewColors.InkSoft
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            "参考手绘卡可直接叠在纸上，默认不加阴影",
                                            style = iosText(9.5f),
                                            color = BrewColors.Muted
                                        )
                                    }
                                    Switch(
                                        checked = illustrationShadow,
                                        onCheckedChange = { illustrationShadow = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = BrewColors.Lime,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = BrewColors.Line,
                                            uncheckedBorderColor = BrewColors.Line
                                        )
                                    )
                                }
                            }
                        }
                    }
                    // 文字
                    item {
                        Column {
                            IosSectionTitle("文字", "TEXT")
                            Spacer(Modifier.height(9.dp))
                            BrewTextField(
                                value = copy.chineseName,
                                onValueChange = { copy = copy.copy(chineseName = it) },
                                label = "中文名（大标题）",
                                placeholder = "这一杯的中文名",
                                maxLength = 40
                            )
                            Spacer(Modifier.height(9.dp))
                            BrewTextField(
                                value = copy.englishName,
                                onValueChange = { copy = copy.copy(englishName = it) },
                                label = "英文名（衬线大字）",
                                placeholder = CardLayoutEngine.autoEnglish(recipe, bean),
                                maxLength = 44
                            )
                            Spacer(Modifier.height(9.dp))
                            PriceField(
                                value = priceText,
                                onValueChange = { priceText = it.filter { ch -> ch.isDigit() }.take(5) }
                            )
                            Spacer(Modifier.height(9.dp))
                            BrewTextField(
                                value = subtitle,
                                onValueChange = { subtitle = it },
                                label = "副标题（可选，小字）",
                                placeholder = "风味、口感、适合的场景",
                                maxLength = 80
                            )
                            Spacer(Modifier.height(9.dp))
                            BrewTextField(
                                value = copy.category,
                                onValueChange = { copy = copy.copy(category = it) },
                                label = "分类（左上角小字）",
                                placeholder = CardLayoutEngine.autoCategory(recipe),
                                maxLength = 20
                            )
                            Spacer(Modifier.height(9.dp))
                            BrewTextField(
                                value = copy.meta,
                                onValueChange = { copy = copy.copy(meta = it) },
                                label = "烘焙度 / 处理法（用逗号分隔）",
                                placeholder = CardLayoutEngine.autoMeta(bean).replace("\n", "，"),
                                maxLength = 40
                            )
                            Spacer(Modifier.height(9.dp))
                            BrewTextField(
                                value = copy.ingredientsChinese,
                                onValueChange = { copy = copy.copy(ingredientsChinese = it) },
                                label = "配料 · 中文",
                                placeholder = CardLayoutEngine.autoIngredientsChinese(recipe),
                                maxLength = 60
                            )
                            Spacer(Modifier.height(9.dp))
                            BrewTextField(
                                value = copy.ingredientsEnglish,
                                onValueChange = { copy = copy.copy(ingredientsEnglish = it) },
                                label = "配料 · 英文 / 豆名",
                                placeholder = CardLayoutEngine.autoIngredientsEnglish(recipe),
                                maxLength = 80
                            )
                            Spacer(Modifier.height(9.dp))
                            BrewTextField(
                                value = copy.flavorsChinese,
                                onValueChange = { copy = copy.copy(flavorsChinese = it) },
                                label = "风味参考 · 中文",
                                placeholder = CardLayoutEngine.autoFlavorKeywords(recipe, bean).joinToString("、"),
                                maxLength = 60
                            )
                            Spacer(Modifier.height(9.dp))
                            BrewTextField(
                                value = copy.flavorsEnglish,
                                onValueChange = { copy = copy.copy(flavorsEnglish = it) },
                                label = "风味参考 · 英文",
                                placeholder = CardLayoutEngine.autoFlavorEnglish(recipe, bean),
                                maxLength = 80
                            )
                            Spacer(Modifier.height(9.dp))
                            BrewTextField(
                                value = copy.keywords,
                                onValueChange = { copy = copy.copy(keywords = it) },
                                label = "底部关键词（逗号分隔）",
                                placeholder = CardLayoutEngine.autoFlavorKeywords(recipe, bean).take(3)
                                    .joinToString("，"),
                                maxLength = 40
                            )
                            Spacer(Modifier.height(9.dp))
                            BrewTextField(
                                value = tagsText,
                                onValueChange = { tagsText = it },
                                label = "标签（兼容旧数据，用逗号分隔）",
                                placeholder = "荔枝，气泡，清爽",
                                maxLength = 60
                            )
                            if (tags.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    tags.forEach { tag ->
                                        Text(
                                            tag,
                                            style = iosText(10.5f, FontWeight.Medium),
                                            color = BrewColors.InkSoft,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(BrewColors.PaperSoft)
                                                .padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // 完成
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(CircleShape)
                                .background(BrewColors.Lime)
                                .clickable { showDesign = false },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("完成", style = iosText(15f, FontWeight.SemiBold), color = BrewColors.InkSolid)
                        }
                    }
                }
            }
        }
    }

    // ———————————————— AI 插画 sheet ————————————————
    if (showIllustration) {
        ModalBottomSheet(
            onDismissRequest = { showIllustration = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = BrewColors.Card,
            dragHandle = { SheetHandle() }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
            ) {
                SheetHeader("AI 插画") { showIllustration = false }
                Spacer(Modifier.height(8.dp))
                Overline("AI ILLUSTRATION")
                Spacer(Modifier.height(5.dp))
                Text("给这杯画一张手绘插画", style = iosText(24f, FontWeight.SemiBold), color = BrewColors.Ink)
                Spacer(Modifier.height(6.dp))
                Text(
                    "会按配方名、豆子、风味、颜色和口感自动写 prompt，生成透明背景 PNG、暖色调、无文字的正方形咖啡插画，用在这杯的随杯卡上。",
                    style = iosText(11.5f),
                    color = BrewColors.Muted,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(16.dp))
                if (!imageConfig.isReady) {
                    NotConfiguredCard("还没有配置 AI 服务") {
                        showIllustration = false
                        onOpenSettings?.invoke()
                    }
                    Spacer(Modifier.height(16.dp))
                }
                // —— 从实物图生成插画 prompt ——
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(17.dp))
                        .background(BrewColors.PaperSoft)
                        .padding(14.dp)
                ) {
                    Text(
                        "从实物图生成插画",
                        style = iosText(12.5f, FontWeight.SemiBold),
                        color = BrewColors.Ink
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "选/拍一张成品咖啡或原料照片：优先用「图片理解 API」生成插画 prompt；未配置或失败则按照片主色+配方本地生成。生成后可改 prompt 再画。",
                        style = iosText(10f),
                        color = BrewColors.Muted,
                        lineHeight = 15.sp
                    )
                    photoPromptBitmap?.let { bitmap ->
                        Spacer(Modifier.height(10.dp))
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "实物图",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    photoPromptStatus?.let { message ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            message,
                            style = iosText(10f),
                            color = if (message.startsWith("未配置")) BrewColors.Muted else BrewColors.Forest,
                            lineHeight = 15.sp
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftCapsuleButton(
                            if (photoPromptBitmap == null) "选择 / 拍摄实物图" else "换一张实物图"
                        ) {
                            photoPromptPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        if (photoPromptBusy) {
                            Spacer(Modifier.width(10.dp))
                            CircularProgressIndicator(
                                Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = BrewColors.LimeDeep
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("正在生成 prompt…", style = iosText(10f), color = BrewColors.Muted)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                BrewTextField(
                    value = illustrationPrompt,
                    onValueChange = { illustrationPrompt = it },
                    label = "插画 prompt（完整显示，可编辑）",
                    placeholder = "手绘水彩咖啡插画，透明背景 PNG，无文字无水印……",
                    singleLine = false,
                    minHeight = 140.dp,
                    maxLength = 1000
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "生成时使用编辑框全文，不截断；超长请删减描述、保留透明底后缀",
                        style = iosText(9.5f),
                        color = BrewColors.MutedLight,
                        modifier = Modifier.weight(1f),
                        lineHeight = 14.sp
                    )
                    Text(
                        "${illustrationPrompt.length}/1000",
                        style = monoText(9.5f),
                        color = if (illustrationPrompt.length > 990) BrewColors.RoseDeep else BrewColors.Muted
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "模型：${imageConfig.model} · 尺寸 ${ImageSizes.forOrientation(orientation, imageConfig.size)}",
                        style = monoText(8.5f),
                        color = BrewColors.MutedLight,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "补全提示词",
                        style = iosText(10.5f, FontWeight.Medium),
                        color = BrewColors.Forest,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                val desc = photoDescription.ifBlank { illustrationPrompt.trim() }
                                if (desc.isNotBlank()) {
                                    illustrationPrompt = ImageGenService.composePrompt(desc, recipe, bean, priceValue)
                                } else {
                                    illustrationPrompt = ImageGenService.buildIllustrationPrompt(recipe, bean, priceValue)
                                }
                            }
                            .padding(6.dp)
                    )
                    Text(
                        "按配方重写 prompt",
                        style = iosText(10.5f, FontWeight.Medium),
                        color = BrewColors.Forest,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                illustrationPrompt = ImageGenService.buildIllustrationPrompt(recipe, bean, priceValue)
                                photoDescription = ""
                            }
                            .padding(6.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(CircleShape)
                        .background(if (!imageBusy && imageConfig.isReady) BrewColors.Lime else BrewColors.LineSoft)
                        .clickable(enabled = !imageBusy && imageConfig.isReady) { generateIllustration() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (imageBusy) "正在画插画…"
                        else if (illustrationPath != null) "重新生成一张"
                        else "根据这杯配方生成插画",
                        style = iosText(15f, FontWeight.SemiBold),
                        color = if (!imageBusy && imageConfig.isReady) BrewColors.InkSolid else BrewColors.Muted
                    )
                }
                if (imageBusy) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(17.dp))
                            .background(BrewColors.LimeSoft.copy(alpha = 0.7f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = BrewColors.LimeDeep
                        )
                        Spacer(Modifier.width(11.dp))
                        Column {
                            Text(
                                "正在根据这杯配方画插画…",
                                style = iosText(12.5f, FontWeight.SemiBold),
                                color = BrewColors.Ink
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "透明背景 PNG · 手绘水彩 · 暖色调 · 无文字 · 正方形",
                                style = iosText(10f),
                                color = BrewColors.Muted
                            )
                        }
                    }
                }
                imageError?.let { message ->
                    Spacer(Modifier.height(14.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(17.dp))
                            .background(BrewColors.Rose)
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AlertTriangleIcon(Modifier.size(14.dp), BrewColors.RoseDeep)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "插画生成失败",
                                style = iosText(12.5f, FontWeight.SemiBold),
                                color = BrewColors.Ink
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(message, style = iosText(11f), color = BrewColors.Muted, lineHeight = 17.sp)
                    }
                }
                val shown = generatedBitmap ?: illustrationBitmap
                if (shown != null && !imageBusy) {
                    Spacer(Modifier.height(14.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(17.dp))
                            .background(BrewColors.PaperSoft)
                            .padding(12.dp)
                    ) {
                        Text(
                            "已生成并套用到卡片",
                            style = iosText(12.5f, FontWeight.SemiBold),
                            color = BrewColors.Ink
                        )
                        Spacer(Modifier.height(10.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrewColors.Card),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = shown.asImageBitmap(),
                                contentDescription = "AI 插画预览",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SoftCapsuleButton("重新生成") { generateIllustration() }
                            SoftCapsuleButton("改用本地封面") {
                                coverKind = "CUP"
                                showIllustration = false
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        SoftCapsuleButton("保存插画到相册") {
                            scope.launch {
                                val target = shown
                                val result = withContext(Dispatchers.IO) {
                                    GallerySaver.saveAiIllustration(
                                        context, target, "yibei-ai-${System.currentTimeMillis()}.png"
                                    )
                                }
                                imageStatus = result.fold(
                                    { "已保存透明插画到相册：Pictures/忆杯手记/AI插画" },
                                    { "保存插画失败：${it.message ?: "未知原因"}" }
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(CircleShape)
                                .background(BrewColors.Lime)
                                .clickable { showIllustration = false },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "完成，回到卡片",
                                style = iosText(14f, FontWeight.SemiBold),
                                color = BrewColors.InkSolid
                            )
                        }
                    }
                }
                imageStatus?.let { status ->
                    Spacer(Modifier.height(12.dp))
                    Text(status, style = iosText(10.5f), color = BrewColors.Forest)
                }
            }
        }
    }

    // ———————————————— AI 文案 sheet ————————————————
    if (showAiCopy) {
        ModalBottomSheet(
            onDismissRequest = { showAiCopy = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = BrewColors.Card,
            dragHandle = { SheetHandle() }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
            ) {
                SheetHeader("AI 灵感") { showAiCopy = false }
                Spacer(Modifier.height(8.dp))
                Overline("AI COPYWRITER")
                Spacer(Modifier.height(5.dp))
                Text("让 AI 写一版文案", style = iosText(24f, FontWeight.SemiBold), color = BrewColors.Ink)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (aiConfig.isReady) {
                        "描述这杯的味道、场景或想给人的感觉，AI 会生成标题/副标题/标签，并挑一套主题与模板。当前模型：${aiConfig.model}"
                    } else {
                        "还没有配置 AI。请到 统计 → 设置 里填写 Base URL / API Key / 模型名；不配置也可以手动设计。"
                    },
                    style = iosText(11.5f),
                    color = BrewColors.Muted,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(16.dp))
                if (!aiConfig.isReady) {
                    NotConfiguredCard("还没有配置文案 AI") {
                        showAiCopy = false
                        onOpenSettings?.invoke()
                    }
                    Spacer(Modifier.height(16.dp))
                }
                BrewTextField(
                    value = aiPrompt,
                    onValueChange = { aiPrompt = it },
                    label = "描述这杯的味道 / 场景",
                    placeholder = "例：周末下午的荔枝气泡冷萃，清爽带果香，适合发朋友圈",
                    singleLine = false,
                    minHeight = 80.dp,
                    maxLength = 300
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(CircleShape)
                        .background(if (!aiBusy && aiConfig.isReady) BrewColors.Lime else BrewColors.LineSoft)
                        .clickable(enabled = !aiBusy && aiConfig.isReady) { generateAiCopy() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (aiBusy) "生成中…" else "AI 生成文案",
                        style = iosText(15f, FontWeight.SemiBold),
                        color = if (!aiBusy && aiConfig.isReady) BrewColors.InkSolid else BrewColors.Muted
                    )
                }
                aiError?.let { message ->
                    Spacer(Modifier.height(14.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(17.dp))
                            .background(BrewColors.Rose)
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AlertTriangleIcon(Modifier.size(14.dp), BrewColors.RoseDeep)
                            Spacer(Modifier.width(6.dp))
                            Text("生成失败", style = iosText(12.5f, FontWeight.SemiBold), color = BrewColors.Ink)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(message, style = iosText(11f), color = BrewColors.Muted, lineHeight = 17.sp)
                    }
                }
                aiSummary?.let { rows ->
                    Spacer(Modifier.height(14.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(17.dp))
                            .background(BrewColors.PaperSoft)
                            .padding(14.dp)
                    ) {
                        Text("已生成并套用", style = iosText(12.5f, FontWeight.SemiBold), color = BrewColors.Ink)
                        Spacer(Modifier.height(10.dp))
                        rows.filter { it.second.isNotBlank() }.forEach { (label, value) ->
                            Row(Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    label,
                                    style = iosText(11f),
                                    color = BrewColors.Muted,
                                    modifier = Modifier.width(48.dp)
                                )
                                Text(value, style = iosText(11.5f, FontWeight.Medium), color = BrewColors.InkSoft)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(CircleShape)
                                .background(BrewColors.Lime)
                                .clickable { showAiCopy = false },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "带着这张卡片继续设计",
                                style = iosText(14f, FontWeight.SemiBold),
                                color = BrewColors.InkSolid
                            )
                        }
                    }
                }
            }
        }
    }

    // ———————————————— 弹窗 ————————————————
    exportSuccessMessage?.let { message ->
        IosAlert(
            title = "已保存到相册",
            body = message,
            onConfirm = { exportSuccessMessage = null }
        )
    }

    dialogMessage?.let { message ->
        IosAlert(
            title = "提示",
            body = message,
            onConfirm = { dialogMessage = null }
        )
    }

    if (cutoutFailed) {
        IosAlert(
            title = "自动抠图没有成功",
            body = "这张图没有识别到清晰前景（或背景太复杂）。可以保留原图直接用，也可以换一张背景干净的照片。",
            confirm = "使用原图",
            onConfirm = {
                cutoutFailed = false
                useOriginalImport()
            },
            dismiss = "换一张",
            onDismissAction = {
                cutoutFailed = false
                launchImportedPicker()
            }
        )
    }

    if (shareDialog) {
        Dialog(onDismissRequest = { shareDialog = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(BrewColors.Card)
                    .padding(18.dp)
            ) {
                Text(
                    "分享 / 打印",
                    style = iosText(17f, FontWeight.SemiBold),
                    color = BrewColors.Ink,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${CardRenderer.exportPixelText(orientation, exportSize)} · PNG",
                    style = iosText(10.5f),
                    color = BrewColors.Muted,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                DialogActionRow("分享图片") {
                    shareDialog = false
                    doShare()
                }
                Spacer(Modifier.height(9.dp))
                DialogActionRow("打印 A6 300dpi") {
                    shareDialog = false
                    doPrint()
                }
                Spacer(Modifier.height(9.dp))
                DialogActionRow("取消", primary = false) { shareDialog = false }
            }
        }
    }

    if (showColorPicker) {
        CustomColorDialog(
            hue = customHue,
            saturation = customSat,
            value = customVal,
            onHue = { customHue = it },
            onSaturation = { customSat = it },
            onValue = { customVal = it },
            onConfirm = {
                val c = Color.hsv(customHue, customSat, customVal)
                themeHex = String.format(
                    "%02X%02X%02X",
                    (c.red * 255).toInt(),
                    (c.green * 255).toInt(),
                    (c.blue * 255).toInt()
                )
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    // 抠图进行中的全屏浮层
    if (cutoutBusy) {
        Box(
            Modifier
                .fillMaxSize()
                .background(BrewColors.Ink.copy(alpha = 0.18f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(BrewColors.Card)
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp, color = BrewColors.Lime)
                Spacer(Modifier.height(10.dp))
                Text("正在自动抠图…", style = iosText(14f, FontWeight.SemiBold), color = BrewColors.Ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    "本机离线处理，背景会变成透明 PNG",
                    style = iosText(11f),
                    color = BrewColors.Muted
                )
            }
        }
    }

    // —— 截图验证驱动（仅由 intent extra "promoDebug" 触发，正常使用无影响） ——
    val debugScript = remember {
        ((context as? Activity)?.intent)?.getStringExtra("promoDebug")
    }
    LaunchedEffect(debugScript) {
        if (debugScript.isNullOrBlank()) return@LaunchedEffect
        delay(700)
        debugScript.split(";").map { it.trim() }.filter { it.isNotEmpty() }.forEach { action ->
            when {
                action == "reset" -> {
                    template = "LIGHT"
                    themeHex = "A3CD39"
                    orientation = "PORTRAIT"
                    exportSize = CardRenderer.EXPORT_PRINT_A6
                    coverKind = "CUP"
                    coverImagePath = null
                    illustrationPath = null
                    importedImagePath = null
                    importedOriginalImagePath = null
                    illustrationSource = "NONE"
                    illustrationPrompt = ""
                    illustrationShadow = false
                    priceText = recipe.price?.toString() ?: ""
                    copy = CardCopy(chineseName = recipe.name)
                    subtitle = defaultSubtitle(recipe, bean)
                    tagsText = CardLayoutEngine.autoFlavorKeywords(recipe, bean).take(4).joinToString("，")
                    hidden = emptyList()
                    landscapeLayout = emptyMap()
                    portraitLayout = emptyMap()
                    fontSizes = CardFontSizes()
                    banner = null
                    editLayout = false
                    selectedElement = null
                    onSaveConfig(snapshot().copy(updatedAt = System.currentTimeMillis()))
                }
                action == "layout" -> {
                    editLayout = true
                    selectedElement = null
                    banner = null
                }
                action.startsWith("select:") -> {
                    val id = action.removePrefix("select:")
                    editLayout = true
                    selectedElement = id
                }
                action.startsWith("drag:") -> {
                    val parts = action.removePrefix("drag:").split(",")
                    val id = parts.getOrNull(0)
                    val nx = parts.getOrNull(1)?.toFloatOrNull()
                    val ny = parts.getOrNull(2)?.toFloatOrNull()
                    if (id != null && nx != null && ny != null) {
                        editLayout = true
                        freezeAutoLayout()
                        val map = activeMap().toMutableMap()
                        map[id] = (map[id] ?: ElementPlacement(nx, ny)).copy(x = nx, y = ny)
                        commitMap(map)
                        selectedElement = id
                        banner = null
                    }
                }
                action.startsWith("overlap:") -> {
                    val parts = action.removePrefix("overlap:").split(",")
                    val id = parts.getOrNull(0)
                    val nx = parts.getOrNull(1)?.toFloatOrNull()
                    val ny = parts.getOrNull(2)?.toFloatOrNull()
                    if (id != null && nx != null && ny != null) {
                        editLayout = true
                        freezeAutoLayout()
                        val map = activeMap().toMutableMap()
                        map[id] = (map[id] ?: ElementPlacement(nx, ny)).copy(x = nx, y = ny)
                        commitMap(map)
                        selectedElement = id
                    }
                }
                action.startsWith("scale:") -> {
                    val parts = action.removePrefix("scale:").split(",")
                    val id = parts.getOrNull(0)
                    val pct = parts.getOrNull(1)?.toFloatOrNull()
                    if (id != null && pct != null) {
                        editLayout = true
                        freezeAutoLayout()
                        val map = activeMap().toMutableMap()
                        val base = map[id] ?: ElementPlacement(0.1f, 0.1f)
                        map[id] = base.copy(
                            scale = (pct / 100f).coerceIn(0.5f, 2f),
                            // 放大插画时按比例上移，保持与底部关键词留白（截图验证用）。
                            y = if (id == CardElement.ILLUSTRATION && pct > 100f) {
                                (base.y - 0.0022f * (pct - 100f)).coerceAtLeast(0.02f)
                            } else base.y
                        )
                        commitMap(map)
                        selectedElement = id
                    }
                }
                action.startsWith("fontsize:") -> {
                    val parts = action.removePrefix("fontsize:").split(",")
                    val id = parts.getOrNull(0)
                    val pt = parts.getOrNull(1)?.toFloatOrNull()
                    if (id != null && pt != null) {
                        editLayout = true
                        setFontSize(id, pt)
                        selectedElement = id
                    }
                }
                action == "restore" -> {
                    editLayout = true
                    restoreAutoLayout()
                }
                action == "save" -> saveToRecipe()
                action == "export" -> doExport()
                action == "exporta6" -> {
                    exportSize = CardRenderer.EXPORT_PRINT_A6
                    delay(500)
                    doExport()
                }
                action == "share" -> shareDialog = true
                action == "landscape" -> switchOrientation("LANDSCAPE")
                action == "portrait" -> switchOrientation("PORTRAIT")
                action == "social" -> exportSize = CardRenderer.EXPORT_SOCIAL
                action == "a6" -> exportSize = CardRenderer.EXPORT_PRINT_A6
                action == "design" -> showDesign = true
                action == "designtext" -> {
                    showDesign = true
                    delay(600)
                    designListState.scrollToItem(5)
                }
                action == "illustration" -> showIllustration = true
                action == "aicopy" -> showAiCopy = true
            }
            delay(650)
        }
    }
}

// ———————————————————————— 页面组件 ————————————————————————

@Composable
private fun PreviewCard(
    bitmap: Bitmap?,
    ratio: Float,
    layout: com.brewkin.beanbook.ui.card.CardLayoutResult,
    editLayout: Boolean,
    selectedElement: String?,
    draggingId: String?,
    dragOffset: Offset,
    dragConflict: Boolean,
    onTapElement: (String?) -> Unit,
    onDragStart: (String?) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    val density = LocalDensity.current
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val scaleX = if (layout.width > 0 && boxSize.width > 0) boxSize.width.toFloat() / layout.width else 1f
    val scaleY = if (layout.height > 0 && boxSize.height > 0) boxSize.height.toFloat() / layout.height else 1f
    val visibleOverlap = remember(layout.overlappingIds, layout.outOfBoundsIds, draggingId) {
        layout.overlappingIds + layout.outOfBoundsIds
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .shadow(
                18.dp,
                RoundedCornerShape(8.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.14f),
                spotColor = Color.Black.copy(alpha = 0.14f)
            )
            .clip(RoundedCornerShape(8.dp))
            .background(BrewColors.PaperSoft)
            .onSizeChanged { boxSize = it }
            .pointerInput(editLayout, layout, boxSize) {
                if (!editLayout) return@pointerInput
                detectTapGestures { pos ->
                    if (boxSize.width <= 0) return@detectTapGestures
                    val cx = pos.x / scaleX
                    val cy = pos.y / scaleY
                    val hit = CardElement.all.reversed().firstOrNull { id ->
                        layout.boxes[id]?.contains(cx, cy) == true
                    }
                    onTapElement(hit)
                }
            }
            .pointerInput(editLayout, layout, boxSize, scaleX, scaleY) {
                if (!editLayout) return@pointerInput
                detectDragGestures(
                    onDragStart = { pos ->
                        if (boxSize.width <= 0) return@detectDragGestures
                        val cx = pos.x / scaleX
                        val cy = pos.y / scaleY
                        val hit = CardElement.all.reversed().firstOrNull { id ->
                            layout.boxes[id]?.contains(cx, cy) == true
                        }
                        onDragStart(hit)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        onDrag(amount)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragCancel() }
                )
            }
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "随杯卡预览",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        if (editLayout) {
            layout.boxes.forEach { (id, box) ->
                val isSelected = id == selectedElement
                val isDragging = id == draggingId
                val overlap = (id in visibleOverlap) || (dragConflict && isDragging)
                val dx = if (isDragging) with(density) { dragOffset.x.toDp() } else 0.dp
                val dy = if (isDragging) with(density) { dragOffset.y.toDp() } else 0.dp
                val left = with(density) { (box.left * scaleX).toDp() } + dx
                val top = with(density) { (box.top * scaleY).toDp() } + dy
                val wDp = with(density) { (box.width() * scaleX).toDp() }.coerceAtLeast(16.dp)
                val hDp = with(density) { (box.height() * scaleY).toDp() }.coerceAtLeast(16.dp)
                Box(
                    Modifier
                        .offset(x = left, y = top)
                        .size(wDp, hDp)
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        if (isSelected) {
                            drawRoundRect(
                                color = BrewColors.Lime.copy(alpha = 0.10f),
                                cornerRadius = CornerRadius(6.dp.toPx())
                            )
                        }
                        drawRoundRect(
                            color = when {
                                overlap -> Color(0xFFE26D5A)
                                isSelected -> BrewColors.LimeDeep
                                else -> BrewColors.Line
                            },
                            cornerRadius = CornerRadius(6.dp.toPx()),
                            style = Stroke(
                                width = if (isSelected || overlap) 1.6.dp.toPx() else 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
                            )
                        )
                    }
                    if (isSelected) {
                        Text(
                            CardElement.label(id),
                            modifier = Modifier
                                .offset(y = (-15).dp, x = (-1).dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(BrewColors.Lime)
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            style = iosText(8f, FontWeight.Medium),
                            color = BrewColors.InkSolid,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayoutEditPanel(
    layout: com.brewkin.beanbook.ui.card.CardLayoutResult,
    selected: String?,
    hidden: List<String>,
    conflict: Boolean,
    scaleOf: (String) -> Float,
    fontPtOf: (String) -> Float,
    onSelect: (String) -> Unit,
    onToggleHidden: (String) -> Unit,
    onAlign: (String, String) -> Unit,
    onScale: (String, Float) -> Unit,
    onFontSize: (String, Float) -> Unit,
    onResetFont: (String) -> Unit,
    onRestore: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BrewColors.Card)
            .border(1.dp, BrewColors.LineSoft, RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("编辑布局", style = iosText(13f, FontWeight.SemiBold), color = BrewColors.Ink)
                Spacer(Modifier.height(2.dp))
                Text(
                    "在卡片上点选/拖动元素；文字可调字号与对齐，图片可缩放",
                    style = iosText(9.5f),
                    color = BrewColors.Muted,
                    lineHeight = 14.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "恢复自动排版",
                style = iosText(10.5f, FontWeight.Medium),
                color = BrewColors.Forest,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(BrewColors.LimeSoft)
                    .clickable(onClick = onRestore)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        // 元素选择行（横向滚动，每个元素带显示/隐藏眼睛）
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardElement.all.forEach { id ->
                val isHidden = id in hidden
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(if (selected == id) BrewColors.Lime else BrewColors.PaperSoft)
                        .padding(start = 9.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        CardElement.label(id),
                        style = iosText(
                            10.5f,
                            if (selected == id) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = when {
                            selected == id -> BrewColors.InkSolid
                            isHidden -> BrewColors.MutedLight
                            else -> BrewColors.InkSoft
                        },
                        modifier = Modifier
                            .clickable { onSelect(id) }
                            .padding(vertical = 6.dp)
                    )
                    Box(
                        Modifier
                            .width(24.dp)
                            .height(28.dp)
                            .clickable { onToggleHidden(id) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isHidden) {
                            EyeOffIcon(Modifier.size(12.dp), BrewColors.MutedLight)
                        } else {
                            EyeIcon(Modifier.size(12.dp), BrewColors.Forest)
                        }
                    }
                }
            }
        }

        val sel = selected
        if (sel != null && sel !in hidden) {
            Spacer(Modifier.height(10.dp))
            // 属性行：元素名 + 对齐（文字）+ 缩放
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(BrewColors.PaperSoft)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    CardElement.label(sel),
                    style = iosText(11f, FontWeight.Medium),
                    color = BrewColors.InkSoft,
                    modifier = Modifier.widthIn(min = 42.dp),
                    maxLines = 1
                )
                if (sel != CardElement.ILLUSTRATION) {
                    val align = layout.let { _ ->
                        // 当前对齐从布局后的首行推断（LEFT / CENTER / RIGHT）
                        "AUTO"
                    }
                    IconActionButton(Modifier.size(30.dp), { onAlign(sel, "LEFT") }) {
                        AlignLeftIcon(Modifier.size(15.dp), BrewColors.InkSoft)
                    }
                    IconActionButton(Modifier.size(30.dp), { onAlign(sel, "CENTER") }) {
                        AlignCenterIcon(Modifier.size(15.dp), BrewColors.InkSoft)
                    }
                    IconActionButton(Modifier.size(30.dp), { onAlign(sel, "RIGHT") }) {
                        AlignRightIcon(Modifier.size(15.dp), BrewColors.InkSoft)
                    }
                }
                Spacer(Modifier.weight(1f))
                IconActionButton(Modifier.size(30.dp), { onScale(sel, -0.05f) }) {
                    ZoomOutIcon(Modifier.size(16.dp), BrewColors.InkSoft)
                }
                Text(
                    "${(scaleOf(sel) * 100).roundToInt().coerceIn(50, 200)}%",
                    style = monoText(10f),
                    color = BrewColors.Muted,
                    modifier = Modifier.width(38.dp),
                    textAlign = TextAlign.Center
                )
                IconActionButton(Modifier.size(30.dp), { onScale(sel, 0.05f) }) {
                    ZoomInIcon(Modifier.size(16.dp), BrewColors.InkSoft)
                }
            }
            if (sel != CardElement.ILLUSTRATION) {
                Spacer(Modifier.height(8.dp))
                // 字号行：8–72pt Slider + NNpt + 重置
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(BrewColors.PaperSoft)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("字号", style = iosText(11f, FontWeight.Medium), color = BrewColors.InkSoft)
                    Spacer(Modifier.width(6.dp))
                    Slider(
                        value = fontPtOf(sel).coerceIn(8f, 72f),
                        onValueChange = { onFontSize(sel, it.roundToInt().toFloat()) },
                        valueRange = 8f..72f,
                        steps = 0,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = BrewColors.Lime,
                            inactiveTrackColor = BrewColors.Line
                        )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${fontPtOf(sel).coerceIn(8f, 72f).toInt()}pt",
                        style = monoText(10f),
                        color = BrewColors.Muted,
                        modifier = Modifier.width(38.dp),
                        textAlign = TextAlign.End
                    )
                    IconActionButton(Modifier.size(24.dp), { onResetFont(sel) }) {
                        ResetIcon(Modifier.size(13.dp), BrewColors.Muted)
                    }
                }
            }
        }

        if (conflict) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrewColors.Rose.copy(alpha = 0.7f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlertTriangleIcon(Modifier.size(13.dp), BrewColors.RoseDeep)
                Spacer(Modifier.width(7.dp))
                Text(
                    "元素有重叠：请拖开或点「恢复自动排版」（重叠时不能保存/导出）",
                    style = iosText(10.5f, FontWeight.Medium),
                    color = BrewColors.RoseDeep,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun ToolButton(
    modifier: Modifier = Modifier,
    label: String,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (active) BrewColors.LimeSoft else BrewColors.Card)
            .border(1.dp, BrewColors.LineSoft, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val iconColor = BrewColors.Forest
        when (label) {
            "设计工具" -> SlidersIcon(Modifier.size(18.dp), iconColor)
            "AI 插画" -> BrushIcon(Modifier.size(18.dp), iconColor)
            "导入图片", "换导入图" -> PhotoImportIcon(Modifier.size(18.dp), iconColor)
            "AI 文案" -> SparkIcon(Modifier.size(18.dp), iconColor)
            "编辑布局", "完成布局" -> HandIcon(Modifier.size(18.dp), iconColor)
            "导出到相册" -> DownloadTrayIcon(Modifier.size(18.dp), iconColor)
            else -> ShareIcon(Modifier.size(18.dp), iconColor)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = iosText(10.5f, FontWeight.Medium),
            color = BrewColors.InkSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExportSizeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(if (selected) BrewColors.LimeSoft else BrewColors.Card)
            .border(
                1.dp,
                if (selected) BrewColors.Lime else BrewColors.LineSoft,
                CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = iosText(12f, if (selected) FontWeight.SemiBold else FontWeight.Normal),
            color = if (selected) BrewColors.Forest else BrewColors.InkSoft
        )
    }
}

@Composable
private fun ChoicePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(if (selected) BrewColors.LimeSoft else BrewColors.PaperSoft)
            .border(
                1.dp,
                if (selected) BrewColors.Lime.copy(alpha = 0.6f) else BrewColors.LineSoft,
                CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = iosText(12f, if (selected) FontWeight.SemiBold else FontWeight.Normal),
            color = if (selected) BrewColors.Forest else BrewColors.InkSoft
        )
    }
}

@Composable
private fun SoftCapsuleButton(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(BrewColors.PaperSoft)
            .border(1.dp, BrewColors.LineSoft, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = iosText(11.5f, FontWeight.Medium), color = BrewColors.InkSoft)
    }
}

@Composable
private fun IconActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun DialogActionRow(label: String, primary: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(if (primary) BrewColors.Lime else BrewColors.PaperSoft)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = iosText(14f, FontWeight.Medium),
            color = if (primary) BrewColors.InkSolid else BrewColors.InkSoft
        )
    }
}

@Composable
private fun IosSectionTitle(title: String, overline: String) {
    Column {
        Overline(overline)
        Spacer(Modifier.height(5.dp))
        Text(title, style = iosText(19f, FontWeight.SemiBold), color = BrewColors.Ink)
    }
}

@Composable
private fun PriceField(value: String, onValueChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BrewColors.PaperSoft)
            .padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("菜单价格 ¥", style = iosText(12f, FontWeight.Medium), color = BrewColors.InkSoft)
            Spacer(Modifier.height(2.dp))
            Text(
                "显示在卡片右上角与菜单列表，可留空",
                style = iosText(9.5f),
                color = BrewColors.Muted
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .width(84.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BrewColors.Card)
                .padding(horizontal = 10.dp, vertical = 9.dp),
            textStyle = monoText(15f).copy(color = BrewColors.Ink),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (value.isEmpty()) {
                        Text("未设置", style = monoText(15f), color = BrewColors.MutedLight)
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun NotConfiguredCard(title: String, onOpenSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(BrewColors.PaperSoft)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SparkIcon(Modifier.size(13.dp), BrewColors.Brown)
            Spacer(Modifier.width(7.dp))
            Text(title, style = iosText(13f, FontWeight.SemiBold), color = BrewColors.Ink)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "在「统计 → 设置 → 插画 API」里选择 Provider（推荐智谱 BigModel · GLM-Image）并填入 API Key；不配置也可以用杯图/豆插画/相册图。",
            style = iosText(11f),
            color = BrewColors.Muted,
            lineHeight = 17.sp
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .height(34.dp)
                .clip(CircleShape)
                .background(BrewColors.Lime)
                .clickable(onClick = onOpenSettings)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("去设置", style = iosText(12f, FontWeight.Medium), color = BrewColors.InkSolid)
        }
    }
}

@Composable
private fun HintBar(
    icon: @Composable () -> Unit,
    text: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(BrewColors.PaperSoft)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = iosText(10.5f),
            color = BrewColors.Muted,
            maxLines = 2,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        Row(
            Modifier
                .height(30.dp)
                .clip(CircleShape)
                .background(BrewColors.Lime)
                .clickable(onClick = onAction)
                .padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(actionLabel, style = iosText(11f, FontWeight.Medium), color = BrewColors.InkSolid)
        }
    }
}

@Composable
private fun ImportedCoverControls(
    hasImage: Boolean,
    cutout: Boolean,
    onReimport: () -> Unit,
    onRecut: () -> Unit,
    onUseOriginal: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(BrewColors.PaperSoft)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CheckCircleIcon(
                Modifier.size(13.dp),
                color = if (cutout) BrewColors.Forest else BrewColors.Muted,
                checkColor = BrewColors.PaperSoft
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (hasImage) {
                    if (cutout) "已导入：透明背景 PNG" else "已导入：原图（未抠图）"
                } else {
                    "从相册选一张咖啡/饮品照片，自动抠出主体、背景变透明，再叠到奶油纸卡片上。"
                },
                style = iosText(11f, FontWeight.Medium),
                color = BrewColors.InkSoft,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftCapsuleButton(if (hasImage) "重新导入" else "去导入", onReimport)
            SoftCapsuleButton(if (cutout) "重新抠图" else "自动抠图", onRecut)
            if (cutout) SoftCapsuleButton("改用原图", onUseOriginal)
        }
    }
}

@Composable
private fun CustomColorDialog(
    hue: Float,
    saturation: Float,
    value: Float,
    onHue: (Float) -> Unit,
    onSaturation: (Float) -> Unit,
    onValue: (Float) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val color = Color.hsv(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(BrewColors.Card)
                .padding(20.dp)
        ) {
            Text("自定义主题色", style = iosText(17f, FontWeight.SemiBold), color = BrewColors.Ink)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, BrewColors.Line, CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    String.format(
                        "#%02X%02X%02X",
                        (color.red * 255).toInt(),
                        (color.green * 255).toInt(),
                        (color.blue * 255).toInt()
                    ),
                    style = monoText(13f),
                    color = BrewColors.InkSoft
                )
            }
            Spacer(Modifier.height(14.dp))
            ColorSliderRow("色相", hue, 0f, 360f, onHue)
            ColorSliderRow("饱和度", saturation, 0f, 1f, onSaturation)
            ColorSliderRow("明度", value, 0f, 1f, onValue)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(BrewColors.PaperSoft)
                        .clickable(onClick = onDismiss),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("取消", style = iosText(14f, FontWeight.Medium), color = BrewColors.Muted)
                }
                Row(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(BrewColors.Lime)
                        .clickable(onClick = onConfirm),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("好", style = iosText(14f, FontWeight.SemiBold), color = BrewColors.InkSolid)
                }
            }
        }
    }
}

@Composable
private fun ColorSliderRow(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = iosText(11f), color = BrewColors.Muted, modifier = Modifier.width(44.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = min..max,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = BrewColors.Lime,
                inactiveTrackColor = BrewColors.Line
            )
        )
    }
}

@Composable
private fun CoverKindIcon(kind: String, selected: Boolean) {
    val color = if (selected) BrewColors.Forest else BrewColors.Muted
    when (kind) {
        "ILLUSTRATION" -> SparkIcon(Modifier.size(16.dp), color)
        "IMPORTED" -> PhotoImportIcon(Modifier.size(16.dp), color)
        "CUP" -> CupIcon(Modifier.size(16.dp), color)
        else -> BeanIcon(Modifier.size(16.dp), color)
    }
}

private fun coverKindLabel(kind: String): String = when (kind) {
    "ILLUSTRATION" -> "AI 插画"
    "IMPORTED" -> "相册导入"
    "CUP" -> "杯图"
    "BEAN" -> "豆插画"
    "GALLERY" -> "相册原图"
    else -> "杯图"
}

@Composable
private fun IosAlert(
    title: String,
    body: String,
    confirm: String = "好",
    onConfirm: () -> Unit,
    dismiss: String? = null,
    onDismissAction: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = { onDismissAction?.invoke() ?: onConfirm() }) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(BrewColors.Card)
                .padding(20.dp)
        ) {
            Text(title, style = iosText(18f, FontWeight.SemiBold), color = BrewColors.Ink)
            Spacer(Modifier.height(8.dp))
            Text(body, style = iosText(13f), color = BrewColors.InkSoft, lineHeight = 20.sp)
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(CircleShape)
                    .background(if (dismiss == null) BrewColors.PaperSoft else BrewColors.Lime)
                    .clickable(onClick = onConfirm),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    confirm,
                    style = iosText(14f, FontWeight.Medium),
                    color = if (dismiss == null) BrewColors.Ink else BrewColors.InkSolid
                )
            }
            if (dismiss != null && onDismissAction != null) {
                Spacer(Modifier.height(9.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(CircleShape)
                        .background(BrewColors.PaperSoft)
                        .clickable(onClick = onDismissAction),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(dismiss, style = iosText(14f, FontWeight.Medium), color = BrewColors.Muted)
                }
            }
        }
    }
}

@Composable
private fun SheetHandle() {
    Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(38.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(BrewColors.Line)
        )
    }
}

@Composable
private fun SheetHeader(title: String, onDone: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp)
    ) {
        Text(
            title,
            style = iosText(17f, FontWeight.SemiBold),
            color = BrewColors.Ink,
            modifier = Modifier.align(Alignment.Center)
        )
        Row(
            Modifier
                .align(Alignment.CenterEnd)
                .height(34.dp)
                .clip(CircleShape)
                .background(BrewColors.Card)
                .border(1.dp, BrewColors.LineSoft, CircleShape)
                .clickable(onClick = onDone)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("完成", style = iosText(13f, FontWeight.Medium), color = BrewColors.Ink)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(BrewColors.LineSoft))
}

// ———————————————————————— 工具函数 ————————————————————————

private fun defaultSubtitle(recipe: Recipe, bean: Bean?): String = listOfNotNull(
    bean?.name?.takeIf { it.isNotBlank() },
    recipe.beanName.takeIf { it.isNotBlank() },
    recipe.mouthfeel.takeIf { it.isNotBlank() },
    recipe.sweetness.takeIf { it.isNotBlank() }
).distinct().joinToString(" · ")

private fun parseTags(text: String): List<String> = text
    .split(',', '，', '、', ' ', '\n')
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .distinct()

private fun iosText(size: Float, weight: FontWeight = FontWeight.Normal): TextStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * 1.4f).sp
)

private fun monoText(size: Float): TextStyle = TextStyle(
    fontFamily = DMMono,
    fontWeight = FontWeight.Normal,
    fontSize = size.sp,
    lineHeight = (size * 1.4f).sp
)

private fun hexToColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
}.getOrElse { BrewColors.LimeDeep }

private fun isLightHex(hex: String): Boolean = runCatching {
    val c = android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
    val luminance = (0.299 * android.graphics.Color.red(c) +
        0.587 * android.graphics.Color.green(c) +
        0.114 * android.graphics.Color.blue(c)) / 255.0
    luminance > 0.6
}.getOrElse { false }
