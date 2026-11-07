package com.brewkin.beanbook.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brewkin.beanbook.BuildConfig
import com.brewkin.beanbook.data.AiCardService
import com.brewkin.beanbook.data.AiConfig
import com.brewkin.beanbook.data.AppState
import com.brewkin.beanbook.data.DatabaseImportMode
import com.brewkin.beanbook.data.ImageGenConfig
import com.brewkin.beanbook.data.ImageGenService
import com.brewkin.beanbook.data.ImageProvider
import com.brewkin.beanbook.data.ImageSizes
import com.brewkin.beanbook.data.SettingsStore
import com.brewkin.beanbook.data.VisionConfig
import com.brewkin.beanbook.data.TextProvider
import com.brewkin.beanbook.data.ThemeMode
import com.brewkin.beanbook.data.TransferOutcome
import com.brewkin.beanbook.ui.components.BackIcon
import com.brewkin.beanbook.ui.components.BrewTextField
import com.brewkin.beanbook.ui.components.CircleIconButton
import com.brewkin.beanbook.ui.components.Overline
import com.brewkin.beanbook.ui.components.PrimaryButton
import com.brewkin.beanbook.ui.components.SecondaryButton
import com.brewkin.beanbook.ui.components.SmallChoice
import com.brewkin.beanbook.ui.components.SurfaceCard
import com.brewkin.beanbook.ui.theme.BrewColors
import kotlinx.coroutines.launch

/**
 * 1.3 设置页：文案 API（OpenAI chat，内置 DeepSeek/OpenAI 预设）+ 插画生成 API（OpenAI 兼容
 * images/generations，内置智谱 BigModel / 硅基流动预设）+ 外观（跟随系统/浅色/深色）+
 * 数据与备份（从原数据页迁入）+ 关于/版本/隐私。
 */
@Composable
fun SettingsScreen(
    state: AppState,
    settings: SettingsStore,
    onBack: () -> Unit,
    onExportDatabase: (Uri, (Result<Unit>) -> Unit) -> Unit,
    onImportDatabase: (Uri, DatabaseImportMode, (Result<TransferOutcome>) -> Unit) -> Unit,
    onImportRecipe: (Uri, (Result<TransferOutcome>) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val aiConfig by settings.aiConfig.collectAsStateWithLifecycle()
    val imageConfig by settings.imageConfig.collectAsStateWithLifecycle()
    val themeMode by settings.themeMode.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var textProvider by remember(aiConfig) { mutableStateOf(TextProvider.CUSTOM) }
    var baseUrl by remember(aiConfig) { mutableStateOf(aiConfig.baseUrl) }
    var apiKey by remember(aiConfig) { mutableStateOf(aiConfig.apiKey) }
    var model by remember(aiConfig) { mutableStateOf(aiConfig.model) }
    var showKey by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var statusError by remember { mutableStateOf(false) }

    var imageProvider by remember(imageConfig) { mutableStateOf(imageConfig.provider) }
    var imageBaseUrl by remember(imageConfig) { mutableStateOf(imageConfig.baseUrl) }
    var imageApiKey by remember(imageConfig) { mutableStateOf(imageConfig.apiKey) }
    var imageModel by remember(imageConfig) { mutableStateOf(imageConfig.model) }
    var imageSize by remember(imageConfig) { mutableStateOf(imageConfig.size) }
    var showImageKey by remember { mutableStateOf(false) }
    var testingImage by remember { mutableStateOf(false) }
    var testBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageStatus by remember { mutableStateOf<String?>(null) }
    var imageStatusError by remember { mutableStateOf(false) }

    fun currentTextConfig() = AiConfig(baseUrl = baseUrl, apiKey = apiKey, model = model)
    fun currentImageConfig() = ImageGenConfig(
        provider = imageProvider,
        baseUrl = imageBaseUrl,
        apiKey = imageApiKey,
        model = imageModel,
        size = imageSize
    )

    fun showStatus(text: String, error: Boolean) {
        statusText = text
        statusError = error
    }

    fun showImageStatus(text: String, error: Boolean) {
        imageStatus = text
        imageStatusError = error
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BrewColors.Paper)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(
                    onClick = onBack,
                    description = "返回",
                    background = BrewColors.Card,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
                ) { BackIcon(Modifier.size(18.dp), BrewColors.Ink) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Overline("SETTINGS")
                    Spacer(Modifier.height(3.dp))
                    Text("设置", style = MaterialTheme.typography.displayMedium, color = BrewColors.Ink)
                }
            }
        }
        item {
            Text(
                "AI 分成「文案」和「插画」两套 API，可分别配置。所有设置只保存在这台设备上。",
                style = MaterialTheme.typography.bodyMedium,
                color = BrewColors.Muted
            )
        }

        // —— 文案 API ——
        item {
            SurfaceCard(Modifier.fillMaxWidth(), color = BrewColors.Card) {
                Column(Modifier.padding(18.dp)) {
                    Overline("AI COPY · CHAT/COMPLETIONS")
                    Spacer(Modifier.height(6.dp))
                    Text("文案 API", style = MaterialTheme.typography.headlineSmall, color = BrewColors.Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "OpenAI 兼容 chat/completions，用于生成卡片文案 JSON。推荐 DeepSeek（便宜）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrewColors.Muted
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextProvider.all.forEach { provider ->
                            SmallChoice(TextProvider.label(provider), textProvider == provider, {
                                textProvider = provider
                                TextProvider.defaults(provider).let { (base, mdl) ->
                                    if (base.isNotBlank()) {
                                        baseUrl = base
                                        model = mdl
                                    }
                                }
                            })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    BrewTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = "Base URL",
                        placeholder = "https://api.deepseek.com/v1",
                        maxLength = 200
                    )
                    Spacer(Modifier.height(9.dp))
                    BrewTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = "API Key",
                        placeholder = "sk-...",
                        maxLength = 200,
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation()
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        if (showKey) "隐藏 Key" else "显示 Key",
                        modifier = Modifier
                            .clip(CircleShape)
                            .androidxClickable { showKey = !showKey }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = BrewColors.Brown
                    )
                    Spacer(Modifier.height(4.dp))
                    BrewTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = "模型名",
                        placeholder = "deepseek-chat",
                        maxLength = 80
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton(
                            text = "保存配置",
                            onClick = {
                                settings.saveAiConfig(currentTextConfig())
                                showStatus("文案 API 配置已保存", false)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryButton(
                            text = if (testing) "测试中…" else "测试连接",
                            onClick = {
                                if (testing) return@SecondaryButton
                                settings.saveAiConfig(currentTextConfig())
                                testing = true
                                statusText = null
                                scope.launch {
                                    val result = AiCardService.testConnection(currentTextConfig())
                                    testing = false
                                    result.onSuccess { reply -> showStatus(reply, false) }
                                        .onFailure { err -> showStatus("连接失败：${err.message}", true) }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !testing
                        )
                    }
                    if (testing) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = BrewColors.LimeDeep)
                            Spacer(Modifier.width(8.dp))
                            Text("正在连接文案 API…", style = MaterialTheme.typography.bodySmall, color = BrewColors.Muted)
                        }
                    }
                    statusText?.let { status ->
                        Spacer(Modifier.height(10.dp))
                        Text(
                            status,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (statusError) BrewColors.RoseDeep else BrewColors.Forest
                        )
                    }
                }
            }
        }

        // —— 插画生成 API ——
        item {
            SurfaceCard(Modifier.fillMaxWidth(), color = BrewColors.Card) {
                Column(Modifier.padding(18.dp)) {
                    Overline("AI IMAGE · IMAGES/GENERATIONS")
                    Spacer(Modifier.height(6.dp))
                    Text("插画生成 API", style = MaterialTheme.typography.headlineSmall, color = BrewColors.Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "OpenAI 兼容 images/generations，用于生成随杯卡的手绘水彩咖啡插画。默认预设「智谱 BigModel · GLM-Image」，官方价约 0.1 元/次（Key 需自填）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrewColors.Muted
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ImageProvider.all.forEach { provider ->
                            SmallChoice(ImageProvider.label(provider), imageProvider == provider, {
                                imageProvider = provider
                                ImageProvider.defaults(provider).let { (base, mdl) ->
                                    imageBaseUrl = base
                                    imageModel = mdl
                                }
                            })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    BrewTextField(
                        value = imageBaseUrl,
                        onValueChange = { imageBaseUrl = it },
                        label = "Base URL",
                        placeholder = "https://open.bigmodel.cn/api/paas/v4",
                        maxLength = 200
                    )
                    Spacer(Modifier.height(9.dp))
                    BrewTextField(
                        value = imageApiKey,
                        onValueChange = { imageApiKey = it },
                        label = "API Key",
                        placeholder = "插画服务的 Key",
                        maxLength = 200,
                        visualTransformation = if (showImageKey) VisualTransformation.None else PasswordVisualTransformation()
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        if (showImageKey) "隐藏 Key" else "显示 Key",
                        modifier = Modifier
                            .clip(CircleShape)
                            .androidxClickable { showImageKey = !showImageKey }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = BrewColors.Brown
                    )
                    Spacer(Modifier.height(4.dp))
                    BrewTextField(
                        value = imageModel,
                        onValueChange = { imageModel = it },
                        label = "插画模型",
                        placeholder = "glm-image",
                        maxLength = 80
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("插画尺寸", style = MaterialTheme.typography.titleSmall, color = BrewColors.InkSoft)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ImageSizes.presets.take(2).forEach { (size, label) ->
                            SmallChoice("$size(${label})", imageSize == size, { imageSize = size })
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ImageSizes.presets.drop(2).forEach { (size, label) ->
                            SmallChoice("$size(${label})", imageSize == size, { imageSize = size })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    BrewTextField(
                        value = if (imageSize in ImageSizes.presetValues) "" else imageSize,
                        onValueChange = { imageSize = it },
                        label = "自定义尺寸（可留空）",
                        placeholder = "如 1024x1440（512–2048，32 的倍数）",
                        maxLength = 20
                    )
                    val sizeError = ImageSizes.validate(imageSize)
                    if (imageSize.isNotBlank() && imageSize !in ImageSizes.presetValues && sizeError != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(sizeError, style = MaterialTheme.typography.bodySmall, color = BrewColors.RoseDeep)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton(
                            text = "保存配置",
                            onClick = {
                                ImageSizes.validate(currentImageConfig().size)?.let {
                                    showImageStatus("尺寸不合法：$it", true)
                                    return@PrimaryButton
                                }
                                settings.saveImageConfig(currentImageConfig())
                                showImageStatus("插画 API 配置已保存", false)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryButton(
                            text = if (testingImage) "出图中…" else "测试出图",
                            onClick = {
                                if (testingImage) return@SecondaryButton
                                if (imageApiKey.isBlank()) {
                                    showImageStatus("请先填写插画 API Key", true)
                                    return@SecondaryButton
                                }
                                ImageSizes.validate(currentImageConfig().size)?.let {
                                    showImageStatus("尺寸不合法：$it", true)
                                    return@SecondaryButton
                                }
                                settings.saveImageConfig(currentImageConfig())
                                testingImage = true
                                testBitmap = null
                                imageStatus = null
                                scope.launch {
                                    val result = ImageGenService.generate(
                                        currentImageConfig(),
                                        "a simple hand-painted watercolor illustration of a coffee cup on cream paper, centered, no text, no letters"
                                    )
                                    testingImage = false
                                    result.onSuccess { bitmap ->
                                        testBitmap = bitmap
                                        showImageStatus("出图成功：${bitmap.width}×${bitmap.height}", false)
                                    }.onFailure { err ->
                                        showImageStatus("出图失败：${err.message}", true)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !testingImage
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "提示：「测试出图」会真实请求一次 images/generations，消耗一次 API 额度（智谱 CogView-3-Flash 有免费额度）。真实智谱返回 url，App 会自动下载；第三方网关的 b64_json 也支持。",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrewColors.MutedLight
                    )
                    if (testingImage) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = BrewColors.LimeDeep)
                            Spacer(Modifier.width(8.dp))
                            Text("正在请求 images/generations…", style = MaterialTheme.typography.bodySmall, color = BrewColors.Muted)
                        }
                    }
                    testBitmap?.let { bitmap ->
                        Spacer(Modifier.height(12.dp))
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "测试出图",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    imageStatus?.let { status ->
                        Spacer(Modifier.height(10.dp))
                        Text(
                            status,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (imageStatusError) BrewColors.RoseDeep else BrewColors.Forest
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "恢复默认（智谱 GLM-Image）",
                        modifier = Modifier
                            .clip(CircleShape)
                            .androidxClickable {
                                settings.resetImageConfig()
                                showImageStatus("已恢复默认插画配置（智谱 GLM-Image）", false)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = BrewColors.Muted
                    )
                }
            }
        }

        // —— 图片理解 API（实物图 → 插画 prompt）——
        item {
            VisionApiSection(settings)
        }

        // —— 外观 ——
        item {
            SurfaceCard(Modifier.fillMaxWidth(), color = BrewColors.Card) {
                Column(Modifier.padding(18.dp)) {
                    Overline("APPEARANCE")
                    Spacer(Modifier.height(6.dp))
                    Text("黑暗模式", style = MaterialTheme.typography.headlineSmall, color = BrewColors.Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "跟随系统或手动锁定浅色/深色；修改后立即生效并保存。",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrewColors.Muted
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.all.forEach { mode ->
                            SmallChoice(ThemeMode.label(mode), themeMode == mode, { settings.setThemeMode(mode) })
                        }
                    }
                }
            }
        }

        // —— 数据与备份 ——
        item {
            Column {
                Overline("DATA & BACKUP")
                Spacer(Modifier.height(6.dp))
                Text("数据与备份", style = MaterialTheme.typography.headlineSmall, color = BrewColors.Ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    "整库备份、迁移与单杯配方文件导入。单杯分享仍在配方目录的每一杯里。",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrewColors.Muted
                )
            }
        }
        item {
            DataTransferSection(
                state = state,
                onExportDatabase = onExportDatabase,
                onImportDatabase = onImportDatabase,
                onImportRecipe = onImportRecipe
            )
        }

        // —— 关于 ——
        item {
            SurfaceCard(Modifier.fillMaxWidth(), color = BrewColors.Card) {
                Column(Modifier.padding(18.dp)) {
                    Overline("ABOUT")
                    Spacer(Modifier.height(6.dp))
                    Text("忆杯手记 YIBEI", style = MaterialTheme.typography.headlineSmall, color = BrewColors.Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "版本 ${BuildConfig.VERSION_NAME}（build ${BuildConfig.VERSION_CODE}）",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrewColors.Muted
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("开发者", style = MaterialTheme.typography.labelSmall, color = BrewColors.Muted)
                    Spacer(Modifier.height(2.dp))
                    Text("芝士鸡cheeseE", style = MaterialTheme.typography.bodyMedium, color = BrewColors.Ink)
                    Spacer(Modifier.height(10.dp))
                    Text("开发者主页", style = MaterialTheme.typography.labelSmall, color = BrewColors.Muted)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "B 站：space.bilibili.com/397554088",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrewColors.LimeDeep,
                        modifier = Modifier.androidxClickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://space.bilibili.com/397554088"))
                                )
                            }
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "隐私说明：豆子、配方、照片与所有设置都只保存在本机；App 不会上传任何数据。只有在设置里主动点击测试连接/测试出图，或在卡片里点击 AI 生成时，才会把描述与配置发送到你填写的 AI 服务。",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrewColors.InkSoft
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "一杯一杯，慢慢写厚。 · YIBEI",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrewColors.MutedLight
                    )
                }
            }
        }
    }
}

@Composable
private fun VisionApiSection(settings: SettingsStore) {
    val config by settings.visionConfig.collectAsStateWithLifecycle()
    var baseUrl by remember(config) { mutableStateOf(config.baseUrl) }
    var apiKey by remember(config) { mutableStateOf(config.apiKey) }
    var model by remember(config) { mutableStateOf(config.model) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    SurfaceCard(Modifier.fillMaxWidth(), color = BrewColors.Card) {
        Column(Modifier.padding(18.dp)) {
            Overline("VISION · CHAT + IMAGE_URL")
            Spacer(Modifier.height(6.dp))
            Text("图片理解 API", style = MaterialTheme.typography.headlineSmall, color = BrewColors.Ink)
            Spacer(Modifier.height(4.dp))
            Text(
                "用于「从实物图生成」：让视觉模型看照片，描述主体/杯型/层次/配料/颜色，生成插画 prompt。" +
                    "默认智谱 GLM-4V-Flash（Key 需自填），Key 只存本机。",
                style = MaterialTheme.typography.bodySmall,
                color = BrewColors.Muted
            )
            Spacer(Modifier.height(14.dp))
            BrewTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = "Base URL",
                placeholder = "https://open.bigmodel.cn/api/paas/v4",
                maxLength = 120
            )
            Spacer(Modifier.height(9.dp))
            BrewTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = "API Key",
                placeholder = "图片理解服务的 Key",
                maxLength = 200
            )
            Spacer(Modifier.height(9.dp))
            BrewTextField(
                value = model,
                onValueChange = { model = it },
                label = "图片理解模型",
                placeholder = "glm-4v-flash",
                maxLength = 80
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(
                    text = "保存配置",
                    onClick = {
                        settings.saveVisionConfig(VisionConfig(baseUrl, apiKey, model))
                        status = "图片理解 API 配置已保存"
                        statusError = false
                    },
                    modifier = Modifier.weight(1f)
                )
                SecondaryButton(
                    text = if (busy) "测试中…" else "测试连接",
                    onClick = {
                        busy = true
                        status = null
                        scope.launch {
                            val result = AiCardService.testConnection(
                                AiConfig(baseUrl, apiKey, model)
                            )
                            busy = false
                            result.onSuccess {
                                status = it
                                statusError = false
                            }.onFailure {
                                status = it.message ?: "连接失败"
                                statusError = true
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !busy
                )
            }
            status?.let { message ->
                Spacer(Modifier.height(10.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (statusError) BrewColors.RoseDeep else BrewColors.Forest
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "恢复默认（智谱 glm-4v-flash）",
                modifier = Modifier
                    .clip(CircleShape)
                    .androidxClickable {
                        settings.resetVisionConfig()
                        status = "已恢复默认图片理解配置（智谱 glm-4v-flash）"
                        statusError = false
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = BrewColors.Muted
            )
        }
    }
}

/** 局部 clickable 别名。 */
private fun Modifier.androidxClickable(onClick: () -> Unit): Modifier = clickable(onClick = onClick)
