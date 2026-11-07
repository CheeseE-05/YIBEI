package com.brewkin.beanbook.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brewkin.beanbook.MakerDraft
import com.brewkin.beanbook.data.AppState
import com.brewkin.beanbook.data.Bean
import com.brewkin.beanbook.data.CUP_CAPACITY
import com.brewkin.beanbook.data.CustomIngredient
import com.brewkin.beanbook.data.IngredientKind
import com.brewkin.beanbook.data.IngredientSpec
import com.brewkin.beanbook.data.Ingredients
import com.brewkin.beanbook.data.Recipe
import com.brewkin.beanbook.data.RecipeLayer
import com.brewkin.beanbook.data.ageDays
import com.brewkin.beanbook.data.status
import com.brewkin.beanbook.ui.components.BeanArtwork
import com.brewkin.beanbook.ui.components.BeanIcon
import com.brewkin.beanbook.ui.components.BrewTextField
import com.brewkin.beanbook.ui.components.CheckIcon
import com.brewkin.beanbook.ui.components.ChevronRightIcon
import com.brewkin.beanbook.ui.components.CircleIconButton
import com.brewkin.beanbook.ui.components.CloseIcon
import com.brewkin.beanbook.ui.components.CupDisplay
import com.brewkin.beanbook.ui.components.DotSeparator
import com.brewkin.beanbook.ui.components.EditIcon
import com.brewkin.beanbook.ui.components.MiniCup
import com.brewkin.beanbook.ui.components.Overline
import com.brewkin.beanbook.ui.components.PlusIcon
import com.brewkin.beanbook.ui.components.PrimaryButton
import com.brewkin.beanbook.ui.components.RatingBeans
import com.brewkin.beanbook.ui.components.SecondaryButton
import com.brewkin.beanbook.ui.components.SmallChoice
import com.brewkin.beanbook.ui.components.StirIcon
import com.brewkin.beanbook.ui.components.TrashIcon
import com.brewkin.beanbook.ui.components.UndoIcon
import com.brewkin.beanbook.ui.components.blendedColor
import com.brewkin.beanbook.ui.theme.BrewColors
import com.brewkin.beanbook.ui.theme.DMMono
import com.brewkin.beanbook.ui.theme.formatGrams
import com.brewkin.beanbook.ui.theme.hexColor
import com.brewkin.beanbook.ui.theme.ratingText
import com.brewkin.beanbook.ui.theme.toShortDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakerScreen(
    state: AppState,
    draft: MakerDraft,
    editingRecipe: Recipe?,
    imageFile: (Bean) -> File?,
    onCancelEdit: () -> Unit,
    onPour: (IngredientSpec, Int) -> Int,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onStir: () -> Unit,
    onSelectBean: (String?) -> Unit,
    onAddCustom: (String, String, IngredientKind) -> Unit,
    onDeleteCustom: (String) -> Unit,
    onEditLayer: (Int, Int) -> Unit,
    onRemoveLayer: (Int) -> Unit,
    onSave: (
        name: String,
        beanId: String?,
        rating: Int,
        notes: String,
        mouthfeel: String,
        sweetness: String,
        beanGrams: Int,
        price: Int?,
        onSaved: (String) -> Unit
    ) -> Unit,
    onOpenRecipe: (String) -> Unit
) {
    val selectedBean = state.beanById(draft.beanId)
    val allIngredients = remember(state.customIngredients) {
        state.customIngredients.map { it.toSpec() } + Ingredients.builtIn
    }
    var showBeanPicker by rememberSaveable { mutableStateOf(false) }
    var showSaveSheet by rememberSaveable { mutableStateOf(false) }
    var showCustomSheet by rememberSaveable { mutableStateOf(false) }
    var showClearConfirm by rememberSaveable { mutableStateOf(false) }
    var editingLayer by rememberSaveable { mutableStateOf<Int?>(null) }
    var saving by remember { mutableStateOf(false) }
    val layerToEdit = editingLayer?.let { draft.layers.getOrNull(it) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrewColors.Paper)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = 92.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Overline(if (editingRecipe != null) "EDITING RECIPE" else "MAKE A CUP")
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (editingRecipe != null) "修改这一杯" else "调一杯",
                        style = MaterialTheme.typography.displayMedium,
                        color = BrewColors.Ink
                    )
                }
                CircleIconButton(
                    onClick = onUndo,
                    description = "撤销一层",
                    background = BrewColors.Card,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft),
                    size = 40.dp
                ) {
                    UndoIcon(
                        Modifier.size(17.dp),
                        if (draft.layers.isEmpty()) BrewColors.MutedLight else BrewColors.Ink
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    onClick = { if (draft.layers.isNotEmpty()) showSaveSheet = true },
                    shape = CircleShape,
                    color = if (draft.layers.isEmpty()) BrewColors.LineSoft else BrewColors.Lime,
                    modifier = Modifier.height(40.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheckIcon(
                            Modifier.size(14.dp),
                            if (draft.layers.isEmpty()) BrewColors.Muted else BrewColors.Ink
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            if (editingRecipe != null) "保存修改" else "存一杯",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (draft.layers.isEmpty()) BrewColors.Muted else BrewColors.Ink
                        )
                    }
                }
            }

            if (editingRecipe != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 16.dp, top = 8.dp)
                        .height(40.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = BrewColors.PaperSoft,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EditIcon(Modifier.size(13.dp), BrewColors.Forest)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "正在修改配方 · ${editingRecipe.name}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                            color = BrewColors.InkSoft,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "取消编辑",
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onCancelEdit)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = BrewColors.Brown
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { showBeanPicker = true },
                    shape = CircleShape,
                    color = BrewColors.Card,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
                ) {
                    Row(
                        Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (selectedBean == null) BrewColors.MutedLight else BrewColors.Lime)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            selectedBean?.name ?: "未记豆子",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrewColors.InkSoft,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(3.dp))
                        ChevronRightIcon(Modifier.size(12.dp), BrewColors.Muted)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${draft.totalGrams} / $CUP_CAPACITY g",
                    style = MaterialTheme.typography.labelMedium,
                    color = BrewColors.Muted
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.72f), Color.Transparent),
                                center = Offset(size.width * 0.5f, size.height * 0.52f),
                                radius = size.minDimension * 0.55f
                            ),
                            radius = size.minDimension * 0.55f,
                            center = Offset(size.width * 0.5f, size.height * 0.52f)
                        )
                        drawCircle(
                            color = BrewColors.Lime.copy(alpha = 0.08f),
                            radius = size.minDimension * 0.12f,
                            center = Offset(size.width * 0.22f, size.height * 0.15f)
                        )
                    }
                    CupDisplay(
                        layers = draft.layers,
                        mixed = draft.stirred,
                        modifier = Modifier.fillMaxSize().padding(vertical = 4.dp),
                        emptyHint = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = 46.dp)
                            ) {
                                BeanIcon(Modifier.size(20.dp), BrewColors.MutedLight)
                                Spacer(Modifier.height(7.dp))
                                Text(
                                    "空杯子",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = BrewColors.Muted
                                )
                            }
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .width(133.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(BrewColors.Card)
                        .border(1.dp, BrewColors.LineSoft, RoundedCornerShape(24.dp))
                        .padding(13.dp)
                ) {
                    Overline("IN THE CUP")
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            draft.totalGrams.toString(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = DMMono,
                                fontWeight = FontWeight.Medium
                            ),
                            color = BrewColors.Ink
                        )
                        Text(
                            "g",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrewColors.Muted
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "· ${draft.layers.size} 层",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrewColors.Muted
                        )
                    }
                    Spacer(Modifier.height(9.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(BrewColors.LineSoft)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((draft.totalGrams.toFloat() / CUP_CAPACITY).coerceIn(0f, 1f))
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(BrewColors.Lime)
                        )
                    }
                    if (draft.stirred) {
                        Spacer(Modifier.height(7.dp))
                        Text(
                            "已搅拌 · 成品色",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrewColors.Forest
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .width(22.dp)
                            .height(1.dp)
                            .background(BrewColors.Line)
                    )
                    Spacer(Modifier.height(8.dp))
                    if (draft.layers.isEmpty()) {
                        Text(
                            "按住下方材料\n向杯中倒入",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrewColors.Muted,
                            lineHeight = 18.sp
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            draft.layers.reversed().forEachIndexed { reverseIndex, layer ->
                                val realIndex = draft.layers.lastIndex - reverseIndex
                                LayerRow(
                                    layer = layer,
                                    onClick = { editingLayer = realIndex }
                                )
                            }
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(
                            "点一层可微调",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1.sp),
                            color = BrewColors.MutedLight
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onStir,
                    shape = CircleShape,
                    color = if (draft.stirred) BrewColors.InkSolid else BrewColors.Card,
                    border = if (draft.stirred) null else androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StirIcon(
                            Modifier.size(14.dp),
                            if (draft.stirred) BrewColors.Lime else BrewColors.InkSoft
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (draft.stirred) "恢复分层" else "搅拌均匀",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (draft.stirred) BrewColors.Lime else BrewColors.InkSoft
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "倒掉重来",
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { if (draft.layers.isNotEmpty()) showClearConfirm = true }
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (draft.layers.isEmpty()) BrewColors.MutedLight else BrewColors.Muted
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (draft.remaining == 0) "已满" else "按住材料 · 松手停止",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (draft.remaining == 0) BrewColors.LimeDeep else BrewColors.Muted
                )
            }

            // 材料栏：与 iOS 一致的全圆角卡片（左右/底部留白，不再贴屏幕底边平切）。
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                color = BrewColors.Card,
                shape = RoundedCornerShape(30.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
            ) {
                Column(Modifier.padding(top = 13.dp, bottom = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Overline("INGREDIENT TRAY")
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "按住倒入",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrewColors.MutedLight
                        )
                        Spacer(Modifier.weight(1f))
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(BrewColors.LimeSoft)
                                .clickable { showCustomSheet = true }
                                .semantics { contentDescription = "添加自定义原料" }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlusIcon(Modifier.size(12.dp), BrewColors.Forest)
                            Spacer(Modifier.width(4.dp))
                            Text("自定义", style = MaterialTheme.typography.labelSmall, color = BrewColors.Forest)
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    LazyRow(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allIngredients, key = { it.id }) { spec ->
                            IngredientCard(
                                spec = spec,
                                onPour = onPour
                            )
                        }
                        item {
                            AddCustomCard { showCustomSheet = true }
                        }
                        item { Spacer(Modifier.width(12.dp)) }
                    }
                }
            }
        }
    }

    if (showBeanPicker) {
        BeanPickerSheet(
            state = state,
            selectedBeanId = draft.beanId,
            imageFile = imageFile,
            onSelect = {
                onSelectBean(it)
                showBeanPicker = false
            },
            onDismiss = { showBeanPicker = false }
        )
    }
    if (showCustomSheet) {
        CustomIngredientSheet(
            existing = state.customIngredients,
            onDelete = onDeleteCustom,
            onAdd = { name, color, kind ->
                onAddCustom(name, color, kind)
                showCustomSheet = false
            },
            onDismiss = { showCustomSheet = false }
        )
    }
    if (showSaveSheet) {
        SaveCupSheet(
            draft = draft,
            state = state,
            editing = editingRecipe,
            saving = saving,
            onDismiss = { if (!saving) showSaveSheet = false },
            onSave = { name, beanId, rating, notes, mouthfeel, sweetness, grams, price ->
                saving = true
                onSave(name, beanId, rating, notes, mouthfeel, sweetness, grams, price) { id ->
                    saving = false
                    showSaveSheet = false
                    onOpenRecipe(id)
                }
            }
        )
    }
    if (layerToEdit != null) {
        val index = editingLayer
        LayerEditSheet(
            layer = layerToEdit,
            maxGrams = layerToEdit.grams + draft.remaining,
            onDismiss = { editingLayer = null },
            onRemove = {
                if (index != null) onRemoveLayer(index)
                editingLayer = null
            },
            onApply = { grams ->
                if (index != null) onEditLayer(index, grams)
                editingLayer = null
            }
        )
    }
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = BrewColors.Card,
            title = { Text("倒掉这一杯？", color = BrewColors.Ink) },
            text = { Text("杯里的层次会清空，豆子和材料不会受影响。", color = BrewColors.Muted) },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    onClear()
                }) { Text("倒掉", color = BrewColors.Brown) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("再留一下", color = BrewColors.Muted) }
            }
        )
    }
}

@Composable
private fun LayerRow(layer: RecipeLayer, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(layer.colorHex.hexColor())
            )
            Spacer(Modifier.width(6.dp))
            Text(
                layer.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = BrewColors.InkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${layer.grams}g",
                style = MaterialTheme.typography.labelSmall,
                color = BrewColors.Muted
            )
        }
    }
}

@Composable
private fun IngredientCard(
    spec: IngredientSpec,
    onPour: (IngredientSpec, Int) -> Int
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(120), label = "pressScale")
    val color = spec.colorHex.hexColor()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(if (pressed) BrewColors.LimeSoft else BrewColors.PaperSoft)
            .pointerInput(spec.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    var started = false
                    var cancelled = false
                    val pourJob = scope.launch {
                        // A short delay separates a tap from a tray swipe.
                        delay(150)
                        if (!cancelled) {
                            started = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPour(spec, (spec.rate / 2).coerceAtLeast(8))
                            while (isActive) {
                                delay(115)
                                if (onPour(spec, (spec.rate / 12).coerceAtLeast(2)) == 0) break
                            }
                        }
                    }
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                            cancelled = true
                            pourJob.cancel()
                            break
                        }
                        if (!change.pressed) {
                            if (!started && !cancelled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPour(spec, (spec.rate / 2).coerceAtLeast(8))
                            }
                            pourJob.cancel()
                            break
                        }
                    }
                    pressed = false
                }
            }
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .semantics {
                contentDescription = "倒入${spec.name}"
                onClick(label = "倒入${spec.name}") {
                    onPour(spec, (spec.rate / 2).coerceAtLeast(8))
                    true
                }
            }
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = (spec.alpha * 0.9f).coerceIn(0.35f, 1f))),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(22.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.1f)
                    cubicTo(
                        size.width * 0.9f, size.height * 0.45f,
                        size.width * 0.86f, size.height * 0.86f,
                        size.width * 0.5f, size.height * 0.92f
                    )
                    cubicTo(
                        size.width * 0.14f, size.height * 0.86f,
                        size.width * 0.1f, size.height * 0.45f,
                        size.width * 0.5f, size.height * 0.1f
                    )
                    close()
                }
                drawPath(path, Color.White.copy(alpha = 0.35f), style = Stroke(width = 1.1.dp.toPx()))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            spec.name,
            style = MaterialTheme.typography.bodySmall,
            color = BrewColors.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            if (spec.isCustom) "自定义" else "约${spec.rate}g/s",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
            color = if (spec.isCustom) BrewColors.Forest else BrewColors.Muted
        )
    }
}

@Composable
private fun AddCustomCard(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(BrewColors.LimeSoft.copy(alpha = 0.55f))
            .border(1.dp, BrewColors.Lime.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(BrewColors.Card),
            contentAlignment = Alignment.Center
        ) {
            PlusIcon(Modifier.size(18.dp), BrewColors.Forest)
        }
        Spacer(Modifier.height(6.dp))
        Text("新原料", style = MaterialTheme.typography.bodySmall, color = BrewColors.Forest)
        Spacer(Modifier.height(2.dp))
        Text("自选颜色", style = MaterialTheme.typography.labelSmall, color = BrewColors.Muted)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeanPickerSheet(
    state: AppState,
    selectedBeanId: String?,
    imageFile: (Bean) -> File?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BrewColors.Card,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            Text("这杯用了什么豆？", style = MaterialTheme.typography.headlineSmall, color = BrewColors.Ink)
            Spacer(Modifier.height(4.dp))
            Text(
                "标了豆，这杯才会记到它的名下，也会从豆仓扣掉克数。",
                style = MaterialTheme.typography.bodySmall,
                color = BrewColors.Muted
            )
            Spacer(Modifier.height(16.dp))
            Row {
                SmallChoice("不记豆子", selectedBeanId == null, { onSelect(null) })
            }
            Spacer(Modifier.height(12.dp))
            state.beans.sortedByDescending { it.lastUsedAt }.forEach { bean ->
                val selected = bean.id == selectedBeanId
                Surface(
                    onClick = { onSelect(bean.id) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    color = if (selected) BrewColors.LimeSoft else BrewColors.PaperSoft,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        BeanArtwork(
                            bean = bean,
                            imageFile = imageFile(bean),
                            modifier = Modifier.size(60.dp),
                            radius = 15.dp,
                            showStatus = false
                        )
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(bean.name, style = MaterialTheme.typography.titleMedium, color = BrewColors.Ink)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${bean.origin} · ${bean.roast.label} · 第${bean.ageDays()}天",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrewColors.Muted
                            )
                        }
                        Text(
                            formatGrams(bean.remainingGrams),
                            style = MaterialTheme.typography.labelLarge,
                            color = BrewColors.Forest
                        )
                        if (selected) {
                            Spacer(Modifier.width(8.dp))
                            CheckIcon(Modifier.size(16.dp), BrewColors.Forest)
                        }
                    }
                }
            }
            if (state.beans.isEmpty()) {
                Text(
                    "豆仓还没有豆子，也可以先调一杯，保存为一杯不记豆的配方。",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrewColors.Muted,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CustomIngredientSheet(
    existing: List<CustomIngredient>,
    onDelete: (String) -> Unit,
    onAdd: (String, String, IngredientKind) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf(Ingredients.palette.first()) }
    var kind by rememberSaveable { mutableStateOf(IngredientKind.LIQUID.name) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BrewColors.Card,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 90.dp)
        ) {
            Text("自定义原料", style = MaterialTheme.typography.headlineSmall, color = BrewColors.Ink)
            Spacer(Modifier.height(4.dp))
            Text(
                "给它一个名字和颜色。倒法、克数调整、保存方式，都和托盘里的材料一样。",
                style = MaterialTheme.typography.bodySmall,
                color = BrewColors.Muted
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(color.hexColor()),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(22.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.5f, size.height * 0.1f)
                            cubicTo(size.width * 0.9f, size.height * 0.45f, size.width * 0.86f, size.height * 0.86f, size.width * 0.5f, size.height * 0.92f)
                            cubicTo(size.width * 0.14f, size.height * 0.86f, size.width * 0.1f, size.height * 0.45f, size.width * 0.5f, size.height * 0.1f)
                        }
                        drawPath(path, Color.White.copy(alpha = 0.5f), style = Stroke(1.dp.toPx()))
                    }
                }
                Spacer(Modifier.width(12.dp))
                BrewTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "材料名",
                    placeholder = "如 海盐奶盖 / 自制桂花蜜",
                    modifier = Modifier.weight(1f),
                    maxLength = 14
                )
            }
            Spacer(Modifier.height(16.dp))
            Overline("选择成品色")
            Spacer(Modifier.height(9.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Ingredients.palette.forEach { hex ->
                    val selected = color == hex
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(hex.hexColor())
                            .border(
                                if (selected) 3.dp else 1.dp,
                                if (selected) BrewColors.Ink else BrewColors.LineSoft,
                                CircleShape
                            )
                            .clickable { color = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            CheckIcon(Modifier.size(15.dp), Color.White)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Overline("质地")
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                IngredientKind.entries.forEach { entry ->
                    SmallChoice(entry.label, kind == entry.name, { kind = entry.name })
                }
            }
            if (existing.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Overline("已有的自定义材料")
                Spacer(Modifier.height(4.dp))
                existing.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(item.colorHex.hexColor())
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            item.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrewColors.InkSoft
                        )
                        CircleIconButton(
                            onClick = { onDelete(item.id) },
                            description = "删除 ${item.name}",
                            size = 32.dp,
                            background = BrewColors.PaperSoft
                        ) {
                            CloseIcon(Modifier.size(13.dp), BrewColors.Muted)
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            PrimaryButton(
                "加入托盘",
                onClick = {
                    if (name.isNotBlank()) onAdd(name.trim(), color, IngredientKind.valueOf(kind))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveCupSheet(
    draft: MakerDraft,
    state: AppState,
    editing: Recipe?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String?, Int, String, String, String, Int, Int?) -> Unit
) {
    val suggested = suggestedName(draft.layers)
    var name by remember(editing?.id, suggested) { mutableStateOf(editing?.name ?: suggested) }
    var beanId by remember(editing?.id, draft.beanId) { mutableStateOf(editing?.beanId ?: draft.beanId) }
    var rating by remember(editing?.id) { mutableStateOf(editing?.rating ?: 4) }
    var mouthfeel by remember(editing?.id) { mutableStateOf(editing?.mouthfeel ?: "均衡") }
    var sweetness by remember(editing?.id) { mutableStateOf(editing?.sweetness ?: "甜感适中") }
    var gramsText by remember(editing?.id) { mutableStateOf(editing?.beanGrams?.toString() ?: "15") }
    var priceText by remember(editing?.id) {
        mutableStateOf(editing?.price?.takeIf { it > 0 }?.toString() ?: "")
    }
    var notes by remember(editing?.id) { mutableStateOf(editing?.notes ?: "") }
    var showBeanList by remember { mutableStateOf(false) }
    var gramsInitialized by remember(editing?.id) { mutableStateOf(false) }
    var lastBeanId by remember(editing?.id) { mutableStateOf<String?>(null) }
    val selectedBean = state.beanById(beanId)
    LaunchedEffect(beanId) {
        // 编辑态保留原配方的用豆克数；只有用户换了豆子才重新按余量预填，避免打开就被覆盖。
        val keepEditedGrams = editing != null && !gramsInitialized
        if (keepEditedGrams) {
            gramsInitialized = true
            lastBeanId = beanId
        } else if (beanId != lastBeanId) {
            gramsInitialized = true
            lastBeanId = beanId
            gramsText = if (selectedBean == null) "0"
            else selectedBean.remainingGrams.toInt().coerceAtMost(15).toString()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = BrewColors.Card,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.93f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
            ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Overline(if (editing != null) "UPDATE THIS CUP" else "FILE THIS CUP")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (editing != null) "修改这一杯" else "存进目录",
                        style = MaterialTheme.typography.headlineSmall,
                        color = BrewColors.Ink
                    )
                }
                Box(
                    modifier = Modifier
                        .size(width = 54.dp, height = 66.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrewColors.PaperSoft),
                    contentAlignment = Alignment.Center
                ) {
                    MiniCup(draft.layers, draft.stirred, Modifier.fillMaxSize().padding(8.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            BrewTextField(
                value = name,
                onValueChange = { name = it },
                label = "配方名称",
                placeholder = "给这一杯起个名字",
                maxLength = 30
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                onClick = { showBeanList = !showBeanList },
                shape = RoundedCornerShape(18.dp),
                color = BrewColors.PaperSoft,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "用豆",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrewColors.Muted
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            selectedBean?.name ?: "不记豆子",
                            style = MaterialTheme.typography.bodyLarge,
                            color = BrewColors.Ink
                        )
                    }
                    ChevronRightIcon(Modifier.size(15.dp), BrewColors.Muted)
                }
            }
            if (showBeanList) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    SmallChoice("不记豆子", beanId == null, { beanId = null })
                    state.beans.forEach { bean ->
                        SmallChoice(bean.name, beanId == bean.id, { beanId = bean.id })
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("这一杯怎么样", style = MaterialTheme.typography.titleSmall, color = BrewColors.InkSoft)
                    Spacer(Modifier.height(2.dp))
                    Text(ratingText(rating), style = MaterialTheme.typography.bodySmall, color = BrewColors.Muted)
                }
                RatingBeans(
                    rating = rating,
                    filledColor = BrewColors.Ink,
                    onRating = { rating = it }
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("口感", style = MaterialTheme.typography.titleSmall, color = BrewColors.InkSoft)
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("清爽", "均衡", "浓郁").forEach {
                    SmallChoice(it, mouthfeel == it, { mouthfeel = it })
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("甜度", style = MaterialTheme.typography.titleSmall, color = BrewColors.InkSoft)
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("微甜", "甜感适中", "明显偏甜").forEach {
                    SmallChoice(it, sweetness == it, { sweetness = it })
                }
            }
            if (selectedBean != null) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("用豆克数", style = MaterialTheme.typography.titleSmall, color = BrewColors.InkSoft)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (editing != null) {
                                "剩余 ${formatGrams(selectedBean.remainingGrams)}，修改配方不会重复扣豆"
                            } else {
                                "剩余 ${formatGrams(selectedBean.remainingGrams)}，会从豆仓扣除"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = BrewColors.Muted
                        )
                    }
                    BrewTextField(
                        value = gramsText,
                        onValueChange = { gramsText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = "g",
                        modifier = Modifier.width(82.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            BrewTextField(
                value = priceText,
                onValueChange = { priceText = it.filter { ch -> ch.isDigit() }.take(4) },
                label = "菜单价格 ¥（可留空）",
                placeholder = "如 36",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(Modifier.height(12.dp))
            BrewTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "品鉴笔记",
                placeholder = "这一杯的香气、口感和下次想改的地方。",
                singleLine = false,
                minHeight = 62.dp,
                maxLength = 300
            )
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${draft.totalGrams}g · ${draft.layers.size} 层",
                    style = MaterialTheme.typography.labelMedium,
                    color = BrewColors.Muted
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (editing != null) "保存后会更新原配方" else "配方存在本机",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrewColors.MutedLight
                )
            }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                PrimaryButton(
                    text = when {
                        saving -> "正在保存…"
                        editing != null -> "保存修改"
                        else -> "存进目录"
                    },
                    onClick = {
                        val entered = gramsText.toIntOrNull() ?: 0
                        val max = selectedBean?.remainingGrams?.toInt() ?: Int.MAX_VALUE
                        val grams = if (selectedBean == null) 0 else entered.coerceIn(0, max)
                        val price = priceText.toIntOrNull()?.takeIf { it > 0 }
                        onSave(name, beanId, rating, notes, mouthfeel, sweetness, grams, price)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saving && name.isNotBlank()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayerEditSheet(
    layer: RecipeLayer,
    maxGrams: Int,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onApply: (Int) -> Unit
) {
    var grams by remember(layer, maxGrams) {
        mutableFloatStateOf(layer.grams.toFloat().coerceIn(1f, maxGrams.coerceAtLeast(1).toFloat()))
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BrewColors.Card,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 72.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(layer.colorHex.hexColor())
                )
                Spacer(Modifier.width(8.dp))
                Text(layer.name, style = MaterialTheme.typography.headlineSmall, color = BrewColors.Ink)
                Spacer(Modifier.weight(1f))
                Text(
                    "${grams.roundToInt()} g",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = DMMono),
                    color = BrewColors.Ink
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "这支材料在这一杯里的克数。最多还能加到 ${maxGrams}g。",
                style = MaterialTheme.typography.bodySmall,
                color = BrewColors.Muted
            )
            Spacer(Modifier.height(8.dp))
            Slider(
                value = grams,
                onValueChange = { grams = it },
                valueRange = 1f..maxGrams.coerceAtLeast(1).toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = BrewColors.Ink,
                    activeTrackColor = BrewColors.Lime,
                    inactiveTrackColor = BrewColors.LineSoft
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("1g", style = MaterialTheme.typography.labelSmall, color = BrewColors.Muted)
                Spacer(Modifier.weight(1f))
                Text("${maxGrams}g", style = MaterialTheme.typography.labelSmall, color = BrewColors.Muted)
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onRemove,
                    shape = CircleShape,
                    color = BrewColors.PaperSoft,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TrashIcon(Modifier.size(15.dp), BrewColors.Brown)
                        Spacer(Modifier.width(6.dp))
                        Text("删除", color = BrewColors.Brown, style = MaterialTheme.typography.labelLarge)
                    }
                }
                PrimaryButton(
                    "完成",
                    { onApply(grams.roundToInt()) },
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SheetHandle() {
    Box(
        Modifier
            .padding(top = 10.dp, bottom = 10.dp)
            .width(38.dp)
            .height(4.dp)
            .clip(CircleShape)
            .background(BrewColors.Line)
    )
}

private fun suggestedName(layers: List<RecipeLayer>): String {
    if (layers.isEmpty()) return "今日特调"
    val ids = layers.map { it.ingredientId }.toSet()
    return when {
        ids.contains("matcha") && ids.contains("milk") -> "抹茶拿铁"
        ids.contains("oat") && ids.contains("espresso") -> "燕麦拿铁"
        ids.contains("osmanthus") && ids.contains("espresso") -> "桂花拿铁"
        ids.contains("coldbrew") && ids.contains("soda") -> "冷萃气泡"
        ids.contains("milk") && ids.contains("espresso") && !ids.contains("foam") -> "Dirty"
        ids.contains("espresso") && ids.contains("milk") && ids.contains("foam") -> "经典拿铁"
        ids.contains("sparkling") || ids.contains("soda") -> "气泡特调"
        else -> layers.lastOrNull()?.name?.let { "${it}特调" } ?: "今日特调"
    }
}
