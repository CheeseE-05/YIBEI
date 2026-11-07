package com.brewkin.beanbook.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brewkin.beanbook.data.Bean
import com.brewkin.beanbook.data.Recipe
import com.brewkin.beanbook.data.status
import com.brewkin.beanbook.ui.components.BackIcon
import com.brewkin.beanbook.ui.components.ChevronRightIcon
import com.brewkin.beanbook.ui.components.CircleIconButton
import com.brewkin.beanbook.ui.components.HyperBottomActionBar
import com.brewkin.beanbook.ui.components.CupDisplay
import com.brewkin.beanbook.ui.components.MoreIcon
import com.brewkin.beanbook.ui.components.Overline
import com.brewkin.beanbook.ui.components.PrimaryButton
import com.brewkin.beanbook.ui.components.RatingBeans
import com.brewkin.beanbook.ui.components.SecondaryButton
import com.brewkin.beanbook.ui.components.SectionHeading
import com.brewkin.beanbook.ui.components.ShareIcon
import com.brewkin.beanbook.ui.components.SurfaceCard
import com.brewkin.beanbook.ui.components.TagPill
import com.brewkin.beanbook.ui.components.blendedColor
import com.brewkin.beanbook.ui.theme.BrewColors
import com.brewkin.beanbook.ui.theme.DMMono
import com.brewkin.beanbook.ui.theme.formatGrams
import com.brewkin.beanbook.ui.theme.hexColor
import com.brewkin.beanbook.ui.theme.ratingText
import com.brewkin.beanbook.ui.theme.toDateLabel
import com.brewkin.beanbook.ui.theme.toTimeLabel
import java.util.Locale

@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    bean: Bean?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onMakeCard: () -> Unit,
    onRepeat: () -> Unit,
    onOpenBean: (String) -> Unit,
    onShareFile: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    val tint = lerp(BrewColors.PaperSoft, blendedColor(recipe.layers), 0.18f)

    fun share() {
        val text = buildString {
            appendLine("「${recipe.name}」")
            appendLine("一杯 ${recipe.totalGrams}g、${recipe.layers.size} 层的咖啡。")
            if (recipe.beanName.isNotBlank()) appendLine("用豆：${recipe.beanName} · ${recipe.beanGrams}g")
            appendLine()
            recipe.layers.forEachIndexed { index, layer ->
                appendLine("${index + 1}. ${layer.name} ${layer.grams}g")
            }
            if (recipe.notes.isNotBlank()) {
                appendLine()
                appendLine(recipe.notes)
            }
            appendLine()
            appendLine("记录自「忆杯手记」")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, recipe.name)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "分享这一杯"))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrewColors.Paper)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleIconButton(
                        onClick = onBack,
                        description = "返回",
                        background = BrewColors.Card,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
                    ) { BackIcon(Modifier.size(18.dp), BrewColors.Ink) }
                    Spacer(Modifier.weight(1f))
                    CircleIconButton(
                        onClick = { share() },
                        description = "分享这一杯",
                        background = BrewColors.Card,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
                    ) { ShareIcon(Modifier.size(18.dp), BrewColors.Ink) }
                    Spacer(Modifier.width(8.dp))
                    Box {
                        CircleIconButton(
                            onClick = { showMore = true },
                            description = "更多",
                            background = BrewColors.Card,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
                        ) { MoreIcon(Modifier.size(18.dp), BrewColors.Ink) }
                        DropdownMenu(
                            expanded = showMore,
                            onDismissRequest = { showMore = false },
                            containerColor = BrewColors.Card
                        ) {
                            DropdownMenuItem(
                                text = { Text("编辑配方", color = BrewColors.InkSoft) },
                                onClick = {
                                    showMore = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("制作宣传卡片", color = BrewColors.InkSoft) },
                                onClick = {
                                    showMore = false
                                    onMakeCard()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("发送配方文件（离线）", color = BrewColors.InkSoft) },
                                onClick = {
                                    showMore = false
                                    onShareFile()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("删除这一杯", color = BrewColors.Brown) },
                                onClick = {
                                    showMore = false
                                    confirmDelete = true
                                }
                            )
                        }
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(338.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(tint),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.32f),
                            radius = size.minDimension * 0.37f,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.52f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.18f),
                            radius = size.minDimension * 0.18f,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.23f, size.height * 0.2f)
                        )
                        drawCircle(
                            color = BrewColors.Lime.copy(alpha = 0.12f),
                            radius = size.minDimension * 0.08f,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.78f)
                        )
                    }
                    CupDisplay(
                        layers = recipe.layers,
                        mixed = recipe.stirred,
                        normalizeFill = true,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 16.dp)
                    )
                    Row(
                        modifier = Modifier.align(Alignment.TopStart).padding(14.dp)
                    ) {
                        TagPill(
                            "RECIPE",
                            background = BrewColors.Card.copy(alpha = 0.8f),
                            foreground = BrewColors.InkSoft,
                            dotColor = BrewColors.Lime
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp)
                            .clip(CircleShape)
                            .background(BrewColors.Card.copy(alpha = 0.8f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            recipe.createdAt.toDateLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            color = BrewColors.InkSoft
                        )
                    }
                }
            }
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Overline(recipe.createdAt.toTimeLabel())
                        Spacer(Modifier.width(8.dp))
                        RatingBeans(recipe.rating, filledColor = BrewColors.Ink)
                        Spacer(Modifier.weight(1f))
                        Text(
                            ratingText(recipe.rating),
                            style = MaterialTheme.typography.labelSmall,
                            color = BrewColors.Muted
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        recipe.name,
                        style = MaterialTheme.typography.displayMedium,
                        color = BrewColors.Ink
                    )
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (recipe.beanName.isBlank()) "未记豆子" else recipe.beanName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrewColors.Muted
                        )
                        if (bean != null) {
                            Spacer(Modifier.width(3.dp))
                            ChevronRightIcon(
                                Modifier
                                    .size(12.dp)
                                    .clickable { onOpenBean(bean.id) },
                                BrewColors.Muted
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("·", color = BrewColors.MutedLight)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "用豆 ${recipe.beanGrams}g",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrewColors.Muted
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        TagPill(recipe.mouthfeel, background = BrewColors.Card, dotColor = BrewColors.ButterDeep)
                        TagPill(recipe.sweetness, background = BrewColors.Card, dotColor = BrewColors.RoseDeep)
                        if (recipe.stirred) {
                            TagPill("已搅拌", background = BrewColors.Card, dotColor = BrewColors.LimeDeep)
                        }
                    }
                }
            }
            item {
                SurfaceCard(Modifier.fillMaxWidth(), color = BrewColors.Card) {
                    Row(Modifier.padding(vertical = 16.dp)) {
                        RecipeStat("${recipe.totalGrams}g", "总量", Modifier.weight(1f))
                        RecipeStat("${recipe.layers.size}", "层数", Modifier.weight(1f))
                        RecipeStat("${recipe.beanGrams}g", "用豆", Modifier.weight(1f))
                        RecipeStat("${String.format(Locale.US, "%.1f", recipe.rating.toDouble())}", "评分", Modifier.weight(1f))
                    }
                }
            }
            item {
                SectionHeading("配方层次", overline = "LAYER BY LAYER")
            }
            items(recipe.layers.size) { index ->
                val layer = recipe.layers[index]
                val percent = if (recipe.totalGrams == 0) 0 else layer.grams * 100 / recipe.totalGrams
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = BrewColors.Card,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(layer.colorHex.hexColor())
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                layer.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = BrewColors.Ink
                            )
                            Spacer(Modifier.height(5.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(BrewColors.LineSoft)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(layer.grams.toFloat() / recipe.totalGrams.coerceAtLeast(1))
                                        .height(3.dp)
                                        .clip(CircleShape)
                                        .background(layer.colorHex.hexColor().copy(alpha = 0.72f))
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            "${layer.grams}g",
                            style = MaterialTheme.typography.labelLarge,
                            color = BrewColors.InkSoft
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$percent%",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrewColors.MutedLight,
                            modifier = Modifier.width(30.dp)
                        )
                    }
                }
            }
            if (recipe.notes.isNotBlank()) {
                item {
                    SurfaceCard(Modifier.fillMaxWidth(), color = BrewColors.LimeSoft.copy(alpha = 0.55f), border = null) {
                        Column(Modifier.padding(18.dp)) {
                            Overline("TASTING NOTE", color = BrewColors.Forest)
                            Spacer(Modifier.height(7.dp))
                            Text(
                                recipe.notes,
                                style = MaterialTheme.typography.bodyLarge,
                                color = BrewColors.InkSoft
                            )
                        }
                    }
                }
            }
            val beanForSection = bean
            if (beanForSection != null) {
                item {
                    SectionHeading("用到的豆子", overline = "THE BEAN")
                }
                item {
                    Surface(
                        onClick = { onOpenBean(beanForSection.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = BrewColors.Card,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(beanForSection.status().bgHex.hexColor()),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    beanForSection.name.take(1),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = beanForSection.status().inkHex.hexColor()
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    beanForSection.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BrewColors.Ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${beanForSection.origin} · ${beanForSection.roast.label} · ${beanForSection.process}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrewColors.Muted
                                )
                            }
                            Text(
                                formatGrams(beanForSection.remainingGrams),
                                style = MaterialTheme.typography.labelLarge,
                                color = BrewColors.Forest
                            )
                            Spacer(Modifier.width(3.dp))
                            ChevronRightIcon(Modifier.size(14.dp), BrewColors.MutedLight)
                        }
                    }
                }
            }
            item {
                Surface(
                    onClick = onMakeCard,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = BrewColors.Card,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(BrewColors.LimeSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▣", style = MaterialTheme.typography.titleLarge, color = BrewColors.Forest)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("宣传卡片", style = MaterialTheme.typography.titleLarge, color = BrewColors.Ink)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "做一张横版宣传卡片，可 AI 生成文案",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrewColors.Muted
                            )
                        }
                        ChevronRightIcon(Modifier.size(14.dp), BrewColors.Muted)
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { confirmDelete = true }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "从目录删除这一杯",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrewColors.Muted
                    )
                }
            }
        }

        HyperBottomActionBar(modifier = Modifier.align(Alignment.BottomCenter)) {
            SecondaryButton("分享", { share() }, Modifier.weight(1f))
            SecondaryButton("编辑", onEdit, Modifier.weight(1f))
            PrimaryButton("再调一杯", onRepeat, Modifier.weight(1.5f))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = BrewColors.Card,
            title = { Text("删除这一杯？", color = BrewColors.Ink) },
            text = { Text("删除后会从目录消失；豆仓里的豆子不会恢复。", color = BrewColors.Muted) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("删除", color = BrewColors.Brown) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("再想想", color = BrewColors.Muted) }
            }
        )
    }
}

@Composable
private fun RecipeStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = DMMono,
                fontWeight = FontWeight.Medium
            ),
            color = BrewColors.Ink
        )
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = BrewColors.Muted)
    }
}
