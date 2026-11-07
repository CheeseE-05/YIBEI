package com.brewkin.beanbook.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.brewkin.beanbook.cutout.BackgroundRemover
import com.brewkin.beanbook.data.Bean
import com.brewkin.beanbook.data.ImageUtils
import com.brewkin.beanbook.data.IngredientKind
import com.brewkin.beanbook.data.Roast
import com.brewkin.beanbook.data.Storage
import com.brewkin.beanbook.ui.components.BackIcon
import com.brewkin.beanbook.ui.components.BrewTextField
import com.brewkin.beanbook.ui.components.CameraIcon
import com.brewkin.beanbook.ui.components.CheckIcon
import com.brewkin.beanbook.ui.components.CircleIconButton
import com.brewkin.beanbook.ui.components.EditIcon
import com.brewkin.beanbook.ui.components.HyperBottomActionBar
import com.brewkin.beanbook.ui.components.Overline
import com.brewkin.beanbook.ui.components.PrimaryButton
import com.brewkin.beanbook.ui.components.SectionHeading
import com.brewkin.beanbook.ui.components.SmallChoice
import com.brewkin.beanbook.ui.components.SurfaceCard
import com.brewkin.beanbook.ui.theme.BrewColors
import com.brewkin.beanbook.ui.theme.DMMono
import com.brewkin.beanbook.ui.theme.formatGrams
import com.brewkin.beanbook.ui.theme.hexColor
import com.brewkin.beanbook.ui.theme.toShortDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

private val flavorSuggestions = listOf(
    "茉莉", "柠檬", "红茶感", "蓝莓", "草莓", "柑橘", "黑加仑",
    "焦糖", "坚果", "可可", "蜂蜜", "奶油", "白葡萄", "荔枝"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BeanEditScreen(
    existing: Bean?,
    existingImage: File?,
    onBack: () -> Unit,
    onSave: (Bean, Bitmap?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var roaster by rememberSaveable(existing?.id) { mutableStateOf(existing?.roaster.orEmpty()) }
    var origin by rememberSaveable(existing?.id) { mutableStateOf(existing?.origin.orEmpty()) }
    var region by rememberSaveable(existing?.id) { mutableStateOf(existing?.region.orEmpty()) }
    var process by rememberSaveable(existing?.id) { mutableStateOf(existing?.process ?: "水洗") }
    var roastName by rememberSaveable(existing?.id) { mutableStateOf((existing?.roast ?: Roast.MEDIUM).name) }
    var storageName by rememberSaveable(existing?.id) { mutableStateOf((existing?.storage ?: Storage.ROOM).name) }
    var totalText by rememberSaveable(existing?.id) { mutableStateOf(plainNumber(existing?.totalGrams ?: 150.0)) }
    var remainingText by rememberSaveable(existing?.id) { mutableStateOf(plainNumber(existing?.remainingGrams ?: (existing?.totalGrams ?: 150.0))) }
    var roastDate by rememberSaveable(existing?.id) { mutableStateOf(existing?.roastDate ?: System.currentTimeMillis()) }
    var flavors by remember(existing?.id) { mutableStateOf(existing?.flavors ?: emptyList()) }
    var customFlavor by rememberSaveable(existing?.id) { mutableStateOf("") }
    var note by rememberSaveable(existing?.id) { mutableStateOf(existing?.note.orEmpty()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var chosenImage by remember { mutableStateOf<Bitmap?>(null) }
    var originalImage by remember { mutableStateOf<Bitmap?>(null) }
    var processing by remember { mutableStateOf(false) }
    var cutoutSucceeded by remember { mutableStateOf(false) }
    var saveAttempted by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var cutoutMessage by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = roastDate.toLocalDateAtStartUtc()
    )

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                processing = true
                cutoutMessage = null
                chosenImage = null
                cutoutSucceeded = false
                val loaded = ImageUtils.loadBitmap(context, uri, maxSide = 1600)
                if (loaded == null) {
                    processing = false
                    cutoutMessage = "这张照片读不出来，换一张试试。"
                    return@launch
                }
                originalImage = loaded
                val result = BackgroundRemover.removeBackground(context, loaded)
                result.onSuccess { cut ->
                    chosenImage = cut
                    cutoutSucceeded = true
                    cutoutMessage = "背景已自动去掉，可以保存了。"
                }.onFailure {
                    chosenImage = loaded
                    cutoutSucceeded = false
                    cutoutMessage = "自动抠图没有成功，先用原图，保存后也能正常显示。"
                }
                processing = false
            }
        }
    }

    fun addCustomFlavor() {
        val f = customFlavor.trim()
        if (f.isNotBlank() && flavors.size < 7 && f !in flavors) {
            flavors = flavors + f
            customFlavor = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrewColors.Paper)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleIconButton(
                        onClick = onBack,
                        description = "返回",
                        background = BrewColors.Card,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
                    ) {
                        BackIcon(Modifier.size(18.dp), BrewColors.Ink)
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Overline(if (existing == null) "NEW BEAN" else "EDIT BEAN")
                        Spacer(Modifier.height(3.dp))
                        Text(
                            if (existing == null) "收进豆仓" else "编辑豆子",
                            style = MaterialTheme.typography.headlineSmall,
                            color = BrewColors.Ink
                        )
                    }
                }
            }
            item {
                PhotoSection(
                    existingImage = existingImage,
                    chosenImage = chosenImage,
                    processing = processing,
                    cutoutSucceeded = cutoutSucceeded,
                    cutoutMessage = cutoutMessage,
                    onPick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRetry = {
                        val original = originalImage
                        if (original != null) {
                            scope.launch {
                                processing = true
                                cutoutMessage = null
                                val result = BackgroundRemover.removeBackground(context, original)
                                result.onSuccess {
                                    chosenImage = it
                                    cutoutSucceeded = true
                                    cutoutMessage = "背景已自动去掉，可以保存了。"
                                }.onFailure {
                                    chosenImage = original
                                    cutoutSucceeded = false
                                    cutoutMessage = "自动抠图没有成功，先用原图。"
                                }
                                processing = false
                            }
                        }
                    }
                )
            }
            item {
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        SectionHeading(title = "豆子档案", overline = "01 · BEAN")
                        Spacer(Modifier.height(14.dp))
                        BrewTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "豆名 *",
                            placeholder = "如 Ethiopia Guji / 云南红蜜"
                        )
                        Spacer(Modifier.height(10.dp))
                        BrewTextField(
                            value = roaster,
                            onValueChange = { roaster = it },
                            label = "烘焙商 / 豆商",
                            placeholder = "如 Coffee Buff"
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BrewTextField(
                                value = origin,
                                onValueChange = { origin = it },
                                label = "产地",
                                placeholder = "埃塞俄比亚",
                                modifier = Modifier.weight(1f)
                            )
                            BrewTextField(
                                value = region,
                                onValueChange = { region = it },
                                label = "产区 / 庄园",
                                placeholder = "Guji",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        FieldLabel("处理法")
                        Spacer(Modifier.height(7.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf("水洗", "日晒", "蜜处理", "湿刨", "厌氧").forEach {
                                SmallChoice(it, process == it, { process = it })
                            }
                        }
                    }
                }
            }
            item {
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        SectionHeading(title = "烘焙与风味", overline = "02 · ROAST & FLAVOR")
                        Spacer(Modifier.height(14.dp))
                        FieldLabel("烘焙度")
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Roast.entries.forEach { level ->
                                val selected = roastName == level.name
                                Surface(
                                    onClick = { roastName = level.name },
                                    modifier = Modifier.weight(1f),
                                    color = if (selected) level.toneHex.hexColor() else BrewColors.PaperSoft,
                                    shape = RoundedCornerShape(12.dp),
                                    border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
                                ) {
                                    Text(
                                        level.label,
                                        modifier = Modifier.padding(vertical = 9.dp),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                                            letterSpacing = 0.sp,
                                            fontSize = 11.sp
                                        ),
                                        color = if (selected) Color.White else BrewColors.InkSoft
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        FieldLabel("风味标签 · 最多 7 个")
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            flavorSuggestions.forEach { flavor ->
                                SmallChoice(
                                    flavor,
                                    flavors.contains(flavor),
                                    {
                                        flavors = if (flavors.contains(flavor)) flavors - flavor
                                        else if (flavors.size < 7) flavors + flavor else flavors
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BrewTextField(
                                value = customFlavor,
                                onValueChange = { customFlavor = it },
                                label = "自定义风味",
                                placeholder = "如 荔枝壳 / 海盐",
                                modifier = Modifier.weight(1f),
                                maxLength = 12
                            )
                            Spacer(Modifier.width(8.dp))
                            CircleIconButton(
                                onClick = { addCustomFlavor() },
                                description = "添加自定义风味",
                                background = BrewColors.Lime,
                                size = 46.dp
                            ) {
                                com.brewkin.beanbook.ui.components.PlusIcon(Modifier.size(18.dp), BrewColors.Ink)
                            }
                        }
                        if (flavors.isNotEmpty()) {
                            Spacer(Modifier.height(9.dp))
                            Text(
                                flavors.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = BrewColors.Forest
                            )
                        }
                    }
                }
            }
            item {
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        SectionHeading(title = "克重与保存", overline = "03 · WEIGHT")
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BrewTextField(
                                value = totalText,
                                onValueChange = { totalText = numeric(it) },
                                label = "总克重 (g)",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            BrewTextField(
                                value = remainingText,
                                onValueChange = { remainingText = numeric(it) },
                                label = "剩余克重 (g)",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            onClick = { showDatePicker = true },
                            color = BrewColors.PaperSoft,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "烘焙日期",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = DMMono),
                                        color = BrewColors.Muted
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        roastDate.toShortDate(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = BrewColors.Ink
                                    )
                                }
                                EditIcon(Modifier.size(17.dp), BrewColors.Muted)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        FieldLabel("保存方式")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Storage.entries.forEach {
                                SmallChoice(it.label, storageName == it.name, { storageName = it.name })
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        BrewTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = "备注",
                            placeholder = "记录这支豆子的小脾气，或下一次想怎么冲。",
                            singleLine = false,
                            minHeight = 58.dp,
                            maxLength = 240
                        )
                    }
                }
            }
            if (saveAttempted && formError != null) {
                item {
                    Text(
                        formError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        HyperBottomActionBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
        ) {
            Box(Modifier.weight(1f)) {
                PrimaryButton(
                    text = if (saving) "正在收进豆仓…" else if (existing == null) "收进豆仓" else "保存修改",
                    onClick = {
                        saveAttempted = true
                        if (saving) return@PrimaryButton
                        val total = totalText.toDoubleOrNull() ?: 0.0
                        if (name.isBlank()) {
                            formError = "先给这支豆子起一个名字。"
                            return@PrimaryButton
                        }
                        if (total <= 0.0) {
                            formError = "总克重需要大于 0。"
                            return@PrimaryButton
                        }
                        formError = null
                        saving = true
                        val remaining = (remainingText.toDoubleOrNull() ?: total).coerceIn(0.0, total.coerceAtLeast(0.0))
                        val bean = Bean(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            roaster = roaster.trim(),
                            origin = origin.trim(),
                            region = region.trim(),
                            process = process,
                            roast = Roast.valueOf(roastName),
                            flavors = flavors,
                            totalGrams = total,
                            remainingGrams = remaining,
                            roastDate = roastDate,
                            openedAt = existing?.openedAt ?: System.currentTimeMillis(),
                            storage = Storage.valueOf(storageName),
                            imagePath = existing?.imagePath,
                            note = note.trim(),
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                            lastUsedAt = existing?.lastUsedAt ?: 0L
                        )
                        onSave(bean, chosenImage)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saving
                )
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = dateState.selectedDateMillis ?: roastDate
                    val localDate = Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    roastDate = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    showDatePicker = false
                }) { Text("确定", color = BrewColors.Ink) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消", color = BrewColors.Muted) }
            },
        ) {
            DatePicker(state = dateState)
        }
    }
}

@Composable
private fun PhotoSection(
    existingImage: File?,
    chosenImage: Bitmap?,
    processing: Boolean,
    cutoutSucceeded: Boolean,
    cutoutMessage: String?,
    onPick: () -> Unit,
    onRetry: () -> Unit
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(244.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(BrewColors.Peach),
            contentAlignment = Alignment.Center
        ) {
            if (chosenImage != null) {
                Image(
                    bitmap = chosenImage.asImageBitmap(),
                    contentDescription = "豆子照片预览",
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                    contentScale = ContentScale.Fit
                )
            } else if (existingImage != null) {
                Image(
                    painter = rememberAsyncImagePainter(existingImage),
                    contentDescription = "已有的豆子照片",
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(BrewColors.Card.copy(alpha = 0.64f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CameraIcon(Modifier.size(27.dp), BrewColors.InkSoft)
                    }
                    Spacer(Modifier.height(13.dp))
                    Text(
                        "添加豆袋照片",
                        style = MaterialTheme.typography.titleMedium,
                        color = BrewColors.Ink
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "自动抠图 · 推荐干净的浅色背景",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrewColors.Muted
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (chosenImage != null && cutoutSucceeded) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(BrewColors.LimeSoft)
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheckIcon(Modifier.size(11.dp), BrewColors.Forest)
                        Spacer(Modifier.width(4.dp))
                        Text("已抠图", style = MaterialTheme.typography.labelSmall, color = BrewColors.Forest)
                    }
                }
            }

            if (processing) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(BrewColors.Ink.copy(alpha = 0.46f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = BrewColors.Lime,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "正在分离豆子与背景",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "第一次会稍慢一点",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }
        if (!processing) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onPick,
                    shape = CircleShape,
                    color = BrewColors.Card,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
                ) {
                    Row(
                        Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CameraIcon(Modifier.size(15.dp), BrewColors.Ink)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (chosenImage == null && existingImage == null) "从相册选择" else "换一张照片",
                            style = MaterialTheme.typography.labelLarge,
                            color = BrewColors.Ink
                        )
                    }
                }
                if (chosenImage != null) {
                    Surface(
                        onClick = onRetry,
                        shape = CircleShape,
                        color = BrewColors.PaperSoft
                    ) {
                        Text(
                            "重新抠图",
                            modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = BrewColors.InkSoft
                        )
                    }
                }
            }
        }
        if (cutoutMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                cutoutMessage,
                style = MaterialTheme.typography.bodySmall,
                color = if (cutoutSucceeded) BrewColors.Forest else BrewColors.Muted
            )
        } else {
            Spacer(Modifier.height(8.dp))
            Text(
                "照片会在本机离线抠图，不会上传。",
                style = MaterialTheme.typography.bodySmall,
                color = BrewColors.Muted
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = BrewColors.InkSoft
    )
}

private fun numeric(value: String): String {
    val filtered = value.filter { it.isDigit() || it == '.' }
    if (filtered.count { it == '.' } > 1) {
        val first = filtered.indexOf('.')
        return filtered.substring(0, first + 1) + filtered.substring(first + 1).replace(".", "")
    }
    return filtered.take(6)
}

private fun plainNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

private fun Long.toLocalDateAtStartUtc(): Long {
    val date = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
}
