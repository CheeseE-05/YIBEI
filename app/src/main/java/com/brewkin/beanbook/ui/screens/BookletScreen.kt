package com.brewkin.beanbook.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brewkin.beanbook.data.AppState
import com.brewkin.beanbook.data.Recipe
import com.brewkin.beanbook.ui.components.ChevronRightIcon
import com.brewkin.beanbook.ui.components.CupIcon
import com.brewkin.beanbook.ui.components.DotSeparator
import com.brewkin.beanbook.ui.components.EmptyHint
import com.brewkin.beanbook.ui.components.MiniCup
import com.brewkin.beanbook.ui.components.Overline
import com.brewkin.beanbook.ui.components.PrimaryButton
import com.brewkin.beanbook.ui.components.RatingBeans
import com.brewkin.beanbook.ui.components.SectionHeading
import com.brewkin.beanbook.ui.components.SelectableChip
import com.brewkin.beanbook.ui.components.SurfaceCard
import com.brewkin.beanbook.ui.components.blendedColor
import com.brewkin.beanbook.ui.theme.BrewColors
import com.brewkin.beanbook.ui.theme.DMMono
import com.brewkin.beanbook.ui.theme.dayPart
import com.brewkin.beanbook.ui.theme.monthTitle
import com.brewkin.beanbook.ui.theme.toLocalDate
import com.brewkin.beanbook.ui.theme.toTimeLabel
import java.time.LocalDate
import java.util.Locale

private enum class BookletFilter(val label: String) {
    ALL("全部"),
    MONTH("本月"),
    BEST("高分")
}

@Composable
fun BookletScreen(
    state: AppState,
    onOpenSettings: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onOpenCard: (String) -> Unit,
    onMake: () -> Unit
) {
    var filterName by rememberSaveable { mutableStateOf(BookletFilter.ALL.name) }
    val filter = BookletFilter.valueOf(filterName)
    val now = LocalDate.now()
    val recipes = state.recipes
        .filter {
            when (filter) {
                BookletFilter.ALL -> true
                BookletFilter.MONTH -> it.createdAt.toLocalDate().let { d ->
                    d.year == now.year && d.monthValue == now.monthValue
                }
                BookletFilter.BEST -> it.rating >= 4
            }
        }
        .sortedByDescending { it.createdAt }
    val grouped = recipes.groupBy { it.createdAt.toLocalDate() }.toSortedMap(compareByDescending { it })
    val average = if (state.recipes.isEmpty()) 0.0 else state.recipes.map { it.rating }.average()
    val beanCount = state.recipes.mapNotNull { it.beanId }.distinct().size
    val monthCups = state.recipes.count {
        it.createdAt.toLocalDate().let { d ->
            d.year == now.year && d.monthValue == now.monthValue
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BrewColors.Paper)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Overline("THE MENU")
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "菜单",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.displayMedium,
                        color = BrewColors.Ink
                    )
                    Text(
                        "${state.recipes.size} 杯",
                        style = MaterialTheme.typography.labelLarge,
                        color = BrewColors.Muted
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "配方目录、数据库备份，还有离线分享。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrewColors.Muted
                )
            }
        }
        item {
            BookletSummary(
                cupCount = state.recipes.size,
                average = average,
                beanCount = beanCount,
                monthCups = monthCups
            )
        }
        item {
            DataAccessCard(onOpenSettings)
        }
        item {
            SectionHeading(
                title = "配方目录",
                overline = "RECIPE BOOK"
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BookletFilter.entries) { item ->
                    SelectableChip(
                        item.label,
                        filterName == item.name,
                        { filterName = item.name }
                    )
                }
            }
        }
        if (recipes.isEmpty()) {
            item {
                EmptyHint(
                    title = if (state.recipes.isEmpty()) "目录还是空的" else "这个筛选下还没有",
                    body = if (state.recipes.isEmpty()) {
                        "调好的每一杯都会自动收进这里，像一本慢慢写厚的咖啡日记。"
                    } else {
                        "换一个筛选条件，或者去调一杯新的。"
                    },
                    action = {
                        PrimaryButton("去调一杯", onMake, Modifier.width(160.dp))
                    }
                )
            }
        } else {
            grouped.forEach { (date, cups) ->
                item(key = "day-${date}") {
                    DayHeader(date = date, count = cups.size)
                }
                items(cups, key = { it.id }) { recipe ->
                    BookletRecipeCard(recipe, onClick = { onOpenRecipe(recipe.id) }, onCard = { onOpenCard(recipe.id) })
                }
            }
            item {
                BookletFooter()
            }
        }
    }
}

@Composable
private fun BookletSummary(cupCount: Int, average: Double, beanCount: Int, monthCups: Int) {
    SurfaceCard(Modifier.fillMaxWidth(), color = BrewColors.Card) {
        Box {
            Canvas(Modifier.fillMaxWidth().height(122.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(BrewColors.Lime.copy(alpha = 0.26f), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.15f),
                        radius = size.width * 0.52f
                    ),
                    radius = size.width * 0.52f,
                    center = Offset(size.width * 0.88f, size.height * 0.15f)
                )
                repeat(3) { i ->
                    drawCircle(
                        color = BrewColors.LimeDeep.copy(alpha = 0.15f),
                        radius = (3f + i).dp.toPx(),
                        center = Offset(size.width * (0.72f + i * 0.075f), size.height * (0.22f + i * 0.12f))
                    )
                }
            }
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Overline("EDITION · ${monthTitle()}", color = BrewColors.Forest)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "一共 ${cupCount} 杯",
                            style = MaterialTheme.typography.headlineMedium,
                            color = BrewColors.Ink
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            if (cupCount == 0) "等第一杯落笔" else "每一杯都比上一杯更懂你一点",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrewColors.Muted
                        )
                    }
                    Box(
                        Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(BrewColors.LimeSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        CupIcon(Modifier.size(27.dp), BrewColors.Forest)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SummaryCell("${String.format(Locale.US, "%.1f", average)}", "平均评分", Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(28.dp).background(BrewColors.LineSoft))
                    SummaryCell("$beanCount", "用过的豆子", Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(28.dp).background(BrewColors.LineSoft))
                    SummaryCell("$monthCups", "本月手记", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SummaryCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = DMMono, fontWeight = FontWeight.Medium),
            color = BrewColors.Ink
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = BrewColors.Muted)
    }
}

@Composable
private fun DataAccessCard(onOpenSettings: () -> Unit) {
    SurfaceCard(Modifier.fillMaxWidth(), color = BrewColors.Card) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenSettings)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BrewColors.LimeSoft),
                contentAlignment = Alignment.Center
            ) {
                Text("⇅", style = MaterialTheme.typography.headlineSmall, color = BrewColors.Forest)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("设置与数据", style = MaterialTheme.typography.titleLarge, color = BrewColors.Ink)
                Spacer(Modifier.height(3.dp))
                Text(
                    "AI API · 黑暗模式 · 数据备份与导入",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrewColors.Muted
                )
            }
            ChevronRightIcon(Modifier.size(15.dp), BrewColors.Muted)
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            dayPart(date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()),
            style = MaterialTheme.typography.titleSmall,
            color = BrewColors.InkSoft
        )
        Spacer(Modifier.width(7.dp))
        Text(
            "$count 杯",
            style = MaterialTheme.typography.labelSmall,
            color = BrewColors.MutedLight
        )
        Spacer(Modifier.weight(1f))
        Box(Modifier.width(20.dp).height(1.dp).background(BrewColors.Line))
    }
}

@Composable
private fun BookletRecipeCard(recipe: Recipe, onClick: () -> Unit, onCard: () -> Unit) {
    val cupBg = lerp(BrewColors.PaperSoft, blendedColor(recipe.layers), 0.14f)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = BrewColors.Card,
        border = androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 78.dp, height = 92.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(cupBg),
                contentAlignment = Alignment.Center
            ) {
                MiniCup(
                    layers = recipe.layers,
                    mixed = recipe.stirred,
                    modifier = Modifier.fillMaxSize().padding(10.dp)
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Overline(recipe.createdAt.toTimeLabel(), color = BrewColors.Muted)
                    Spacer(Modifier.weight(1f))
                    recipe.price?.takeIf { it > 0 }?.let { price ->
                        Surface(
                            shape = CircleShape,
                            color = BrewColors.LimeSoft
                        ) {
                            Text(
                                "¥$price",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = BrewColors.Forest
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    RatingBeans(recipe.rating, filledColor = BrewColors.Ink)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    recipe.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = BrewColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        recipe.beanName.ifBlank { "未记豆子" },
                        style = MaterialTheme.typography.bodySmall,
                        color = BrewColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(7.dp))
                    DotSeparator()
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "${recipe.totalGrams}g · ${recipe.layers.size} 层",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrewColors.Muted
                    )
                }
                if (recipe.notes.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        recipe.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = BrewColors.InkSoft.copy(alpha = 0.68f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    onClick = onCard,
                    shape = CircleShape,
                    color = BrewColors.LimeSoft
                ) {
                    Text(
                        "卡片",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = BrewColors.Forest
                    )
                }
                Spacer(Modifier.height(6.dp))
                ChevronRightIcon(Modifier.size(14.dp), BrewColors.MutedLight)
            }
        }
    }
}

@Composable
private fun BookletFooter() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.width(36.dp).height(1.dp).background(BrewColors.Line))
        Spacer(Modifier.height(10.dp))
        Text(
            "一杯一杯，慢慢写厚。",
            style = MaterialTheme.typography.bodySmall,
            color = BrewColors.Muted
        )
        Spacer(Modifier.height(3.dp))
        Text(
            "YIBEI · KEEP DRINKING SLOWLY",
            style = MaterialTheme.typography.labelSmall,
            color = BrewColors.MutedLight
        )
    }
}
