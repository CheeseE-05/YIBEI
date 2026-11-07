package com.brewkin.beanbook.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brewkin.beanbook.data.AppState
import com.brewkin.beanbook.data.Bean
import com.brewkin.beanbook.data.IngredientKind
import com.brewkin.beanbook.data.Recipe
import com.brewkin.beanbook.data.Roast
import com.brewkin.beanbook.data.status
import com.brewkin.beanbook.ui.components.CupIcon
import com.brewkin.beanbook.ui.components.DotSeparator
import com.brewkin.beanbook.ui.components.Overline
import com.brewkin.beanbook.ui.components.PrimaryButton
import com.brewkin.beanbook.ui.components.SelectableChip
import com.brewkin.beanbook.ui.components.SectionHeading
import com.brewkin.beanbook.ui.components.SurfaceCard
import com.brewkin.beanbook.ui.components.TagPill
import com.brewkin.beanbook.ui.theme.BrewColors
import com.brewkin.beanbook.ui.theme.DMMono
import com.brewkin.beanbook.ui.theme.formatGrams
import com.brewkin.beanbook.ui.theme.hexColor
import com.brewkin.beanbook.ui.theme.toLocalDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private enum class StatRange(val label: String, val days: Int?) {
    LAST_30("近 30 天", 30),
    LAST_7("近 7 天", 7),
    ALL("全部", null)
}

private data class FlavorProfile(
    val acid: Float,
    val sweet: Float,
    val body: Float,
    val milk: Float,
    val fresh: Float
)

private data class BeanRank(
    val name: String,
    val cups: Int,
    val grams: Int,
    val roast: Roast?
)

private data class IngredientRank(
    val name: String,
    val colorHex: String,
    val count: Int
)

@Composable
fun StatsScreen(
    state: AppState,
    onMake: () -> Unit,
    onSeeBeans: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var rangeName by rememberSaveable { mutableStateOf(StatRange.LAST_30.name) }
    val range = StatRange.valueOf(rangeName)
    val now = System.currentTimeMillis()
    val threshold = range.days?.let { now - it * 24L * 60L * 60L * 1000L } ?: 0L
    val cups = state.recipes.filter { it.createdAt >= threshold }.sortedByDescending { it.createdAt }
    val beanById = state.beans.associateBy { it.id }
    val profile = flavourProfile(cups, beanById)
    val average = if (cups.isEmpty()) 0.0 else cups.map { it.rating }.average()
    val beanIds = cups.mapNotNull { it.beanId }.distinct()
    val beanUsed = cups.sumOf { it.beanGrams }
    val beanRanking = beanRanking(cups, beanById)
    val topIngredients = ingredientRanking(cups)
    val previousCount = range.days?.let { days ->
        val previousStart = now - 2L * days * 24L * 60L * 60L * 1000L
        state.recipes.count { it.createdAt in previousStart until threshold }
    }
    val streak = brewStreak(state.recipes)
    val monthCups = state.recipes.count {
        it.createdAt.toLocalDate().let { d ->
            d.year == LocalDate.now().year && d.monthValue == LocalDate.now().monthValue
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BrewColors.Paper)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Overline("YOUR COFFEE DATA")
                Spacer(Modifier.height(5.dp))
                Text(
                    "统计",
                    style = MaterialTheme.typography.displayMedium,
                    color = BrewColors.Ink
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "把每一杯，慢慢变成自己的口味地图。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrewColors.Muted
                )
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StatRange.entries) { item ->
                    SelectableChip(item.label, rangeName == item.name, { rangeName = item.name })
                }
            }
        }
        item {
            StatsHero(
                cups = cups.size,
                average = average,
                beanCount = beanIds.size,
                beanUsed = beanUsed,
                streak = streak,
                previousCount = previousCount,
                rangeLabel = range.label,
                onMake = onMake
            )
        }
        item {
            SectionHeading(
                title = "冲煮节奏",
                overline = "BREW RHYTHM"
            )
        }
        item {
            ActivityCard(state.recipes)
        }
        item {
            SectionHeading(
                title = "风味指纹",
                overline = "TASTE PROFILE",
                action = null
            )
        }
        item {
            FlavorCard(profile, cups.size)
        }
        item {
            SectionHeading(
                title = "喝得最多的豆",
                overline = "MOST BREWED"
            )
        }
        item {
            BeanRankingCard(beanRanking, cups.size)
        }
        item {
            SectionHeading(
                title = "烘焙光谱",
                overline = "ROAST SPECTRUM"
            )
        }
        item {
            RoastDistributionCard(cups, beanById)
        }
        item {
            SectionHeading(
                title = "常驻材料",
                overline = "OFTEN POURED"
            )
        }
        item {
            IngredientRankingCard(topIngredients)
        }
        item {
            SectionHeading(
                title = "豆仓现状",
                overline = "INVENTORY"
            )
        }
        item {
            InventoryCard(state, onSeeBeans)
        }
        item {
            StatsFooter(cups = cups.size, monthCups = monthCups)
        }
        // 1.3：设置入口放在统计页最底部。
        item {
            SettingsEntryCard(onOpenSettings)
        }
    }
}

@Composable
private fun SettingsEntryCard(onClick: () -> Unit) {
    SurfaceCard(Modifier.fillMaxWidth(), color = BrewColors.Card) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
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
                Text("⚙", style = MaterialTheme.typography.headlineSmall, color = BrewColors.Forest)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("设置", style = MaterialTheme.typography.titleLarge, color = BrewColors.Ink)
                Spacer(Modifier.height(3.dp))
                Text(
                    "AI API · 黑暗模式 · 数据备份 · 关于",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrewColors.Muted
                )
            }
            com.brewkin.beanbook.ui.components.ChevronRightIcon(Modifier.size(15.dp), BrewColors.Muted)
        }
    }
}

@Composable
private fun StatsHero(
    cups: Int,
    average: Double,
    beanCount: Int,
    beanUsed: Int,
    streak: Int,
    previousCount: Int?,
    rangeLabel: String,
    onMake: () -> Unit
) {
    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        color = BrewColors.InkSolid,
        border = null
    ) {
        Box {
            Canvas(Modifier.fillMaxWidth().height(210.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(BrewColors.Lime.copy(alpha = 0.32f), Color.Transparent),
                        center = Offset(size.width * 0.83f, size.height * 0.2f),
                        radius = size.width * 0.55f
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(size.width * 0.83f, size.height * 0.2f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = size.width * 0.22f,
                    center = Offset(size.width * 0.87f, size.height * 0.66f)
                )
            }
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Overline("TOTAL BREWS · $rangeLabel", color = BrewColors.Lime)
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CupIcon(Modifier.size(23.dp), BrewColors.Lime)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        cups.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = DMMono,
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "杯",
                        modifier = Modifier.padding(bottom = 11.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                    Spacer(Modifier.weight(1f))
                    val deltaText = when {
                        previousCount == null -> "本机记录"
                        cups == 0 && previousCount == 0 -> "等第一杯"
                        cups > previousCount -> "+${cups - previousCount} 杯"
                        cups < previousCount -> "${cups - previousCount} 杯"
                        else -> "持平"
                    }
                    TagPill(
                        text = deltaText,
                        background = Color.White.copy(alpha = 0.09f),
                        foreground = BrewColors.Lime,
                        dotColor = if (previousCount != null) BrewColors.Lime else null
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (cups == 0) "杯里还空着，去倒进今天的味道。" else "和上一个${rangeLabel.removePrefix("近 ").replace(" ", "")}比",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.48f)
                )
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                Spacer(Modifier.height(14.dp))
                Row {
                    DarkStat("${String.format(Locale.US, "%.1f", average)}", "平均评分", Modifier.weight(1f))
                    DarkStat("$beanCount", "用过的豆", Modifier.weight(1f))
                    DarkStat("${beanUsed}g", "累计用豆", Modifier.weight(1f))
                    DarkStat(if (streak == 0) "—" else "$streak", "连续天数", Modifier.weight(1f))
                }
                if (cups == 0) {
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(
                        text = "去调第一杯",
                        onClick = onMake,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun DarkStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = DMMono),
            color = Color.White
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1.sp),
            color = Color.White.copy(alpha = 0.42f)
        )
    }
}

@Composable
private fun ActivityCard(allRecipes: List<Recipe>) {
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val counts = days.map { date -> allRecipes.count { it.createdAt.toLocalDate() == date } }
    val max = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
    val total = counts.sum()
    SurfaceCard(Modifier.fillMaxWidth(), color = BrewColors.Card) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "最近 7 天",
                        style = MaterialTheme.typography.titleMedium,
                        color = BrewColors.Ink
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "一周的咖啡因轨迹",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrewColors.Muted
                    )
                }
                Text(
                    "$total 杯",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrewColors.Forest
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEachIndexed { index, date ->
                    val count = counts[index]
                    val isToday = date == today
                    val barHeight = if (count == 0) 5.dp else (8 + count.toFloat() / max * 52f).dp
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (count == 0) "·" else "$count",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (count == 0) BrewColors.MutedLight else BrewColors.InkSoft
                        )
                        Spacer(Modifier.height(5.dp))
                        Box(
                            modifier = Modifier.height(64.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                Modifier
                                    .width(if (isToday) 24.dp else 20.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            count == 0 -> BrewColors.LineSoft
                                            isToday -> BrewColors.Lime
                                            else -> BrewColors.Lime.copy(alpha = 0.46f)
                                        }
                                    )
                            )
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(
                            weekdayShort(date),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                            color = if (isToday) BrewColors.Ink else BrewColors.Muted
                        )
                    }
                }
            }
        }
    }
}

private fun weekdayShort(date: LocalDate): String = when (date.dayOfWeek.value) {
    1 -> "一"
    2 -> "二"
    3 -> "三"
    4 -> "四"
    5 -> "五"
    6 -> "六"
    else -> "日"
}

@Composable
private fun FlavorCard(profile: FlavorProfile, cups: Int) {
    SurfaceCard(Modifier.fillMaxWidth(), color = BrewColors.Card) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("这一阵的味道", style = MaterialTheme.typography.titleMedium, color = BrewColors.Ink)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (cups == 0) "有记录后，会慢慢长成你的口味轮廓。" else "参考各杯的豆子、材料和比例，不评判好坏。",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrewColors.Muted
                    )
                }
                Text(
                    "5 分制",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrewColors.MutedLight
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FlavorBubble(
                    label = "酸度",
                    value = profile.acid,
                    note = acidNote(profile.acid),
                    background = BrewColors.LimeSoft,
                    modifier = Modifier.weight(1f)
                )
                FlavorBubble(
                    label = "甜度",
                    value = profile.sweet,
                    note = sweetNote(profile.sweet),
                    background = BrewColors.Butter,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(18.dp))
            FlavorLine("醇厚度", profile.body, BrewColors.Brown)
            Spacer(Modifier.height(12.dp))
            FlavorLine("奶感", profile.milk, BrewColors.ButterDeep)
            Spacer(Modifier.height(12.dp))
            FlavorLine("清爽感", profile.fresh, BrewColors.SkyDeep)
        }
    }
}

@Composable
private fun FlavorBubble(
    label: String,
    value: Float,
    note: String,
    background: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(background)
                .padding(13.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    color = BrewColors.Ink
                )
                Spacer(Modifier.weight(1f))
                Text(
                    String.format(Locale.US, "%.1f", value),
                    style = MaterialTheme.typography.labelMedium,
                    color = BrewColors.InkSoft
                )
            }
            Spacer(Modifier.height(11.dp))
            FlavorDots(value, 5)
            Spacer(Modifier.height(8.dp))
            Text(
                note,
                style = MaterialTheme.typography.bodySmall,
                color = BrewColors.InkSoft.copy(alpha = 0.62f)
            )
        }
    }
}

@Composable
private fun FlavorDots(value: Float, max: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        val rounded = value.roundToInt().coerceIn(0, max)
        for (i in 1..max) {
            Box(
                Modifier
                    .size(if (i <= rounded) 11.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (i <= rounded) BrewColors.Ink else BrewColors.Ink.copy(alpha = 0.13f))
            )
        }
    }
}

@Composable
private fun FlavorLine(label: String, value: Float, color: Color) {
    val fraction = (value / 5f).coerceIn(0f, 1f)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = BrewColors.Muted)
            Spacer(Modifier.weight(1f))
            Text(
                String.format(Locale.US, "%.1f", value),
                style = MaterialTheme.typography.labelSmall,
                color = BrewColors.Muted
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape)
                .background(BrewColors.LineSoft)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.78f))
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BeanRankingCard(ranks: List<BeanRank>, cups: Int) {
    SurfaceCard(Modifier.fillMaxWidth(), color = BrewColors.Card) {
        Column(Modifier.padding(18.dp)) {
            if (ranks.isEmpty()) {
                Text(
                    if (cups == 0) "还没有记录，先去调一杯。" else "这些杯子还没有记豆子。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrewColors.Muted
                )
            } else {
                val max = ranks.maxOf { it.cups }.coerceAtLeast(1)
                ranks.take(4).forEachIndexed { index, rank ->
                    if (index > 0) Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "0${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (index == 0) BrewColors.LimeDeep else BrewColors.MutedLight
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                rank.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = BrewColors.InkSolid,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(5.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(BrewColors.LineSoft)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(rank.cups.toFloat() / max)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(if (index == 0) BrewColors.Lime else BrewColors.Ink.copy(alpha = 0.28f))
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${rank.cups} 杯",
                                style = MaterialTheme.typography.labelMedium,
                                color = BrewColors.InkSoft
                            )
                            Text(
                                "${rank.grams}g 用豆",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrewColors.MutedLight
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoastDistributionCard(cups: List<Recipe>, beanById: Map<String, Bean>) {
    val counts = Roast.entries.associateWith { roast ->
        cups.count { recipe -> recipe.beanId?.let { beanById[it]?.roast } == roast }
    }.filterValues { it > 0 }
    SurfaceCard(Modifier.fillMaxWidth(), color = BrewColors.Card) {
        if (counts.isEmpty()) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    "还没有足够的烘焙数据",
                    style = MaterialTheme.typography.titleMedium,
                    color = BrewColors.Ink
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "在配方里标上豆子，烘焙光谱就会慢慢长出来。",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrewColors.Muted
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val total = counts.values.sum().coerceAtLeast(1)
                Box(contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(120.dp)) {
                        val stroke = 17.dp.toPx()
                        drawArc(
                            color = BrewColors.LineSoft,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Butt)
                        )
                        var start = -90f
                        counts.forEach { (roast, count) ->
                            val sweep = count.toFloat() / total * 360f
                            drawArc(
                                color = roast.toneHex.hexColor(),
                                startAngle = start + 2f,
                                sweepAngle = (sweep - 4f).coerceAtLeast(1f),
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Butt)
                            )
                            start += sweep
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$total",
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = DMMono),
                            color = BrewColors.Ink
                        )
                        Text(
                            "杯带豆",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrewColors.Muted
                        )
                    }
                }
                Spacer(Modifier.width(22.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    counts.forEach { (roast, count) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(roast.toneHex.hexColor())
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                roast.label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = BrewColors.InkSoft
                            )
                            Text(
                                "$count",
                                style = MaterialTheme.typography.labelMedium,
                                color = BrewColors.Muted
                            )
                            Text(
                                " 杯",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrewColors.Muted
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IngredientRankingCard(ranks: List<IngredientRank>) {
    SurfaceCard(Modifier.fillMaxWidth(), color = BrewColors.Card) {
        Column(Modifier.padding(18.dp)) {
            if (ranks.isEmpty()) {
                Text(
                    "还没有倒入过材料。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrewColors.Muted
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "最常出现的是 ${ranks.first().name}",
                            style = MaterialTheme.typography.titleMedium,
                            color = BrewColors.Ink
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "按出现的杯数排序，不是克数。",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrewColors.Muted
                        )
                    }
                    Text(
                        "${ranks.first().count} 次",
                        style = MaterialTheme.typography.labelMedium,
                        color = BrewColors.Forest
                    )
                }
                Spacer(Modifier.height(15.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ranks.take(7).forEach { rank ->
                        TagPill(
                            text = "${rank.name} ×${rank.count}",
                            background = BrewColors.PaperSoft,
                            foreground = BrewColors.InkSoft,
                            dotColor = rank.colorHex.hexColor()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryCard(state: AppState, onSeeBeans: () -> Unit) {
    val total = state.beans.sumOf { it.totalGrams }
    val remaining = state.totalRemaining()
    val ratio = if (total <= 0.0) 0f else (remaining / total).toFloat().coerceIn(0f, 1f)
    val peak = state.beans.count { it.status() == com.brewkin.beanbook.data.BeanStatus.PEAK }
    SurfaceCard(Modifier.fillMaxWidth(), color = BrewColors.Card) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        formatGrams(remaining),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = DMMono,
                            fontWeight = FontWeight.Medium
                        ),
                        color = BrewColors.Ink
                    )
                    Text(
                        "豆仓总余量 / ${formatGrams(total)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrewColors.Muted
                    )
                }
                TagPill(
                    text = if (peak == 0) "暂无最佳赏味" else "$peak 支正当时",
                    background = BrewColors.LimeSoft,
                    foreground = BrewColors.Forest,
                    dotColor = BrewColors.Lime
                )
            }
            Spacer(Modifier.height(13.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(BrewColors.LineSoft)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(ratio)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(BrewColors.Lime)
                )
            }
            Spacer(Modifier.height(13.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${state.beans.size} 支在架",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrewColors.Muted
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    onClick = onSeeBeans,
                    shape = CircleShape,
                    color = BrewColors.PaperSoft
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "去豆仓",
                            style = MaterialTheme.typography.labelLarge,
                            color = BrewColors.InkSoft
                        )
                        Spacer(Modifier.width(3.dp))
                        com.brewkin.beanbook.ui.components.ChevronRightIcon(
                            Modifier.size(12.dp),
                            BrewColors.Muted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsFooter(cups: Int, monthCups: Int) {
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (cups == 0) "从第一杯开始，慢慢有自己的答案。"
            else "本月 $monthCups 杯 · 数据只留在这台设备上",
            style = MaterialTheme.typography.bodySmall,
            color = BrewColors.Muted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(5.dp))
        Text(
            "DRINK SLOWLY · NOTICE MORE",
            style = MaterialTheme.typography.labelSmall,
            color = BrewColors.MutedLight
        )
    }
}

private fun acidNote(value: Float): String = when {
    value >= 4f -> "明亮，果汁感"
    value >= 3f -> "酸甜平衡"
    value >= 2f -> "柔和，低酸"
    else -> "以醇厚为主"
}

private fun sweetNote(value: Float): String = when {
    value >= 4f -> "甜感明显"
    value >= 3f -> "圆润回甘"
    value >= 2f -> "轻微甜感"
    else -> "干净清爽"
}

private fun flavourProfile(cups: List<Recipe>, beanById: Map<String, Bean>): FlavorProfile {
    if (cups.isEmpty()) return FlavorProfile(0f, 0f, 0f, 0f, 0f)
    var acid = 0f
    var sweet = 0f
    var body = 0f
    var milk = 0f
    var fresh = 0f
    cups.forEach { recipe ->
        val roast = recipe.beanId?.let { beanById[it] }?.roast
        val roastAcid = when (roast) {
            Roast.LIGHT -> 4.0f
            Roast.MEDIUM_LIGHT -> 3.5f
            Roast.MEDIUM -> 2.9f
            Roast.MEDIUM_DARK -> 2.3f
            Roast.DARK -> 1.8f
            null -> 3.0f
        }
        val ids = recipe.layers.map { it.ingredientId }.toSet()
        val total = recipe.totalGrams.coerceAtLeast(1).toFloat()
        val fruit = recipe.layers.filter {
            it.ingredientId in setOf("passion", "lemon", "mango", "strawberry")
        }.sumOf { it.grams }
        val syrups = recipe.layers.filter { it.kind == IngredientKind.SYRUP }.sumOf { it.grams }
        val milkGrams = recipe.layers.filter { it.ingredientId in setOf("milk", "oat", "coconut", "soy", "foam", "cheesefoam", "cream") }.sumOf { it.grams }
        val coffeeGrams = recipe.layers.filter { it.ingredientId in setOf("espresso", "coldbrew") }.sumOf { it.grams }
        val freshGrams = recipe.layers.filter { it.ingredientId in setOf("ice", "soda") }.sumOf { it.grams }
        acid += (roastAcid + (fruit / total * 2.2f) + (if ("coldbrew" in ids) 0.35f else 0f)).coerceIn(0f, 5f)
        sweet += (2.1f + syrups / 22f + (if (milkGrams > 0) 0.5f else 0f)).coerceIn(0f, 5f)
        body += (2.4f + coffeeGrams / 45f + (if (roast == Roast.DARK) 0.6f else 0f) + (if (milkGrams > 70) 0.35f else 0f)).coerceIn(0f, 5f)
        milk += (milkGrams / total * 5f).coerceIn(0f, 5f)
        fresh += ((freshGrams + fruit) / total * 5f).coerceIn(0f, 5f)
    }
    val n = cups.size.toFloat()
    return FlavorProfile(
        acid = (acid / n).coerceIn(0f, 5f),
        sweet = (sweet / n).coerceIn(0f, 5f),
        body = (body / n).coerceIn(0f, 5f),
        milk = (milk / n).coerceIn(0f, 5f),
        fresh = (fresh / n).coerceIn(0f, 5f)
    )
}

private fun beanRanking(cups: List<Recipe>, beanById: Map<String, Bean>): List<BeanRank> {
    val grouped = cups.groupBy { it.beanId }
    return grouped.map { (id, recipesForBean) ->
        val bean = id?.let { beanById[it] }
        BeanRank(
            name = bean?.name ?: recipesForBean.firstOrNull()?.beanName?.ifBlank { "未记豆子" } ?: "未记豆子",
            cups = recipesForBean.size,
            grams = recipesForBean.sumOf { it.beanGrams },
            roast = bean?.roast
        )
    }.sortedWith(compareByDescending<BeanRank> { it.cups }.thenByDescending { it.grams })
}

private fun ingredientRanking(cups: List<Recipe>): List<IngredientRank> {
    val map = mutableMapOf<String, IngredientRank>()
    cups.forEach { recipe ->
        recipe.layers.forEach { layer ->
            val existing = map[layer.ingredientId]
            map[layer.ingredientId] = IngredientRank(
                name = layer.name,
                colorHex = layer.colorHex,
                count = (existing?.count ?: 0) + 1
            )
        }
    }
    return map.values.sortedWith(compareByDescending<IngredientRank> { it.count }.thenBy { it.name })
}

private fun brewStreak(recipes: List<Recipe>): Int {
    if (recipes.isEmpty()) return 0
    val days = recipes.map { it.createdAt.toLocalDate() }.toSet()
    var cursor = LocalDate.now()
    if (cursor !in days) cursor = cursor.minusDays(1)
    var streak = 0
    while (cursor in days) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}
