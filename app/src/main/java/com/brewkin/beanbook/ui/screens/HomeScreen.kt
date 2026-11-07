package com.brewkin.beanbook.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brewkin.beanbook.data.AppState
import com.brewkin.beanbook.data.ageDays
import com.brewkin.beanbook.data.Bean
import com.brewkin.beanbook.data.Recipe
import com.brewkin.beanbook.data.status
import com.brewkin.beanbook.ui.components.BeanArtwork
import com.brewkin.beanbook.ui.components.BeanIcon
import com.brewkin.beanbook.ui.components.CupDisplay
import com.brewkin.beanbook.ui.components.CupIcon
import com.brewkin.beanbook.ui.components.EmptyHint
import com.brewkin.beanbook.ui.components.MetaRow
import com.brewkin.beanbook.ui.components.MiniCup
import com.brewkin.beanbook.ui.components.Overline
import com.brewkin.beanbook.ui.components.PrimaryButton
import com.brewkin.beanbook.ui.components.RatingBeans
import com.brewkin.beanbook.ui.components.SectionHeading
import com.brewkin.beanbook.ui.components.StatusTag
import com.brewkin.beanbook.ui.components.SurfaceCard
import com.brewkin.beanbook.ui.components.ChartIcon
import com.brewkin.beanbook.ui.components.ChevronRightIcon
import com.brewkin.beanbook.ui.components.PlusIcon
import com.brewkin.beanbook.ui.theme.BrewColors
import com.brewkin.beanbook.ui.theme.DMMono
import com.brewkin.beanbook.ui.theme.formatGrams
import com.brewkin.beanbook.ui.theme.greeting
import com.brewkin.beanbook.ui.theme.hexColor
import com.brewkin.beanbook.ui.theme.monthTitle
import com.brewkin.beanbook.ui.theme.toTimeLabel
import java.io.File

@Composable
fun HomeScreen(
    state: AppState,
    imageFile: (Bean) -> File?,
    onOpenBean: (String) -> Unit,
    onOpenRecipe: (String) -> Unit,
    onSeeBeans: () -> Unit,
    onSeeBooklet: () -> Unit,
    onAddBean: () -> Unit,
    onMake: () -> Unit,
    onStats: () -> Unit
) {
    val recentBeans = state.recentBeans(4)
    val recentRecipes = state.recentRecipes(3)
    val lastLayers = recentRecipes.firstOrNull()?.layers.orEmpty()
    val totalRemaining = state.totalRemaining()
    val avgRating = if (state.recipes.isEmpty()) 0.0 else state.recipes.map { it.rating }.average()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BrewColors.PaperSoft, BrewColors.Paper)))
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            HomeHeader(
                beanCount = state.beans.size,
                onStats = onStats
            )
        }
        item {
            Column {
                Text(
                    greeting(),
                    style = MaterialTheme.typography.labelLarge,
                    color = BrewColors.Muted
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    "今天，喝点什么？",
                    style = MaterialTheme.typography.displayMedium,
                    color = BrewColors.Ink
                )
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(BrewColors.Lime)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "把最近喜欢的味道，再调一遍。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrewColors.Muted
                    )
                }
            }
        }
        item {
            QuickBrewCard(
                layers = lastLayers,
                mixed = recentRecipes.firstOrNull()?.stirred == true,
                onClick = onMake
            )
        }
        item {
            HomeStats(
                beanCount = state.beans.size,
                remaining = totalRemaining,
                recipeCount = state.recipes.size,
                avgRating = avgRating
            )
        }
        item {
            SectionHeading(
                title = "最近在用",
                overline = "BEAN SHELF",
                action = if (state.beans.isEmpty()) null else "全部豆子",
                onAction = onSeeBeans
            )
        }
        item {
            if (recentBeans.isEmpty()) {
                EmptyHint(
                    title = "豆仓还是空的",
                    body = "先收一支正在喝的豆子，之后的每一杯都能找到来处。",
                    action = {
                        PrimaryButton("添加豆子", onAddBean, Modifier.width(150.dp))
                    }
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recentBeans, key = { it.id }) { bean ->
                        HomeBeanCard(
                            bean = bean,
                            imageFile = imageFile(bean),
                            onClick = { onOpenBean(bean.id) }
                        )
                    }
                    item {
                        AddBeanMiniCard(onClick = onAddBean)
                    }
                }
            }
        }
        item {
            SectionHeading(
                title = "最近调过",
                overline = "RECENT CUPS",
                action = if (state.recipes.isEmpty()) null else "翻目录",
                onAction = onSeeBooklet
            )
        }
        if (recentRecipes.isEmpty()) {
            item {
                EmptyHint(
                    title = "还没有第一杯",
                    body = "选一支豆子，把今天的味道一层层倒进杯子里。",
                    action = {
                        PrimaryButton("去调一杯", onMake, Modifier.width(160.dp))
                    }
                )
            }
        } else {
            items(recentRecipes, key = { it.id }) { recipe ->
                RecentRecipeCard(
                    recipe = recipe,
                    onClick = { onOpenRecipe(recipe.id) }
                )
            }
        }
        item {
            HomeFooter()
        }
    }
}

@Composable
private fun HomeHeader(beanCount: Int, onStats: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(BrewColors.InkSolid),
            contentAlignment = Alignment.Center
        ) {
            CupIcon(Modifier.size(22.dp), BrewColors.Lime, filled = true)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "忆杯手记",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = BrewColors.Ink
            )
            Text(
                "YIBEI · VOL. 01",
                style = MaterialTheme.typography.labelSmall,
                color = BrewColors.Muted
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TagCount(count = beanCount)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BrewColors.Card)
                    .border(1.dp, BrewColors.LineSoft, CircleShape)
                    .clickable(onClick = onStats)
                    .semantics { contentDescription = "查看统计" },
                contentAlignment = Alignment.Center
            ) {
                ChartIcon(Modifier.size(19.dp), BrewColors.InkSoft)
            }
        }
    }
}

@Composable
private fun TagCount(count: Int) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(BrewColors.LimeSoft)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BeanIcon(Modifier.size(14.dp), BrewColors.Forest)
        Spacer(Modifier.width(5.dp))
        Text(
            "$count 支",
            style = MaterialTheme.typography.labelLarge,
            color = BrewColors.Forest
        )
    }
}

@Composable
private fun QuickBrewCard(
    layers: List<com.brewkin.beanbook.data.RecipeLayer>,
    mixed: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(186.dp),
        shape = RoundedCornerShape(30.dp),
        color = BrewColors.InkSolid,
        contentColor = Color.White
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(BrewColors.Lime.copy(alpha = 0.34f), Color.Transparent),
                        center = Offset(size.width * 0.86f, size.height * 0.45f),
                        radius = size.width * 0.46f
                    ),
                    radius = size.width * 0.46f,
                    center = Offset(size.width * 0.86f, size.height * 0.45f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.045f),
                    radius = size.height * 0.23f,
                    center = Offset(size.width * 0.82f, size.height * 0.50f)
                )
                repeat(4) { i ->
                    drawCircle(
                        color = BrewColors.Lime.copy(alpha = 0.16f),
                        radius = 2.2.dp.toPx(),
                        center = Offset(size.width * (0.64f + i * 0.06f), size.height * (0.17f + i * 0.05f))
                    )
                }
            }
            CupDisplay(
                layers = layers,
                mixed = mixed,
                normalizeFill = true,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 18.dp)
                    .width(134.dp)
                    .height(174.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 22.dp, top = 20.dp, end = 155.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Overline("QUICK POUR", color = BrewColors.Lime)
                Column {
                    Text(
                        "调一杯",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (layers.isEmpty()) "从空杯开始，倒进今天的味道" else "把最近这一杯，再做一次",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.62f),
                        maxLines = 2
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BrewColors.Lime)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "开始调配",
                        style = MaterialTheme.typography.labelLarge,
                        color = BrewColors.Ink,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(5.dp))
                    ChevronRightIcon(Modifier.size(13.dp), BrewColors.Ink)
                }
            }
        }
    }
}

@Composable
private fun HomeStats(beanCount: Int, remaining: Double, recipeCount: Int, avgRating: Double) {
    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        color = BrewColors.Card.copy(alpha = 0.86f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatCell("$beanCount", "支豆子", Modifier.weight(1f))
            VerticalHairline()
            StatCell(formatGrams(remaining), "豆仓余量", Modifier.weight(1f))
            VerticalHairline()
            StatCell("$recipeCount", "杯记录", Modifier.weight(1f))
            VerticalHairline()
            StatCell(if (avgRating == 0.0) "—" else String.format("%.1f", avgRating), "平均评分", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = DMMono,
                fontWeight = FontWeight.Medium
            ),
            color = BrewColors.Ink,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1.sp), color = BrewColors.Muted)
    }
}

@Composable
private fun VerticalHairline() {
    Box(
        Modifier
            .width(1.dp)
            .height(28.dp)
            .background(BrewColors.LineSoft)
    )
}

@Composable
private fun HomeBeanCard(bean: Bean, imageFile: File?, onClick: () -> Unit) {
    val status = bean.status()
    Surface(
        onClick = onClick,
        modifier = Modifier.width(210.dp),
        shape = RoundedCornerShape(28.dp),
        color = BrewColors.Card,
        border = androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
    ) {
        Column {
            BeanArtwork(
                bean = bean,
                imageFile = imageFile,
                modifier = Modifier.fillMaxWidth().height(132.dp).padding(8.dp),
                radius = 22.dp
            )
            Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 14.dp)) {
                Text(
                    bean.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = BrewColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "${bean.roaster.ifBlank { bean.process }} · ${bean.roast.short}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrewColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatGrams(bean.remainingGrams),
                        style = MaterialTheme.typography.labelLarge,
                        color = BrewColors.Forest
                    )
                    Text(
                        " / ${formatGrams(bean.totalGrams)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrewColors.Muted
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "第${bean.ageDays()}天",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrewColors.Muted
                    )
                }
                Spacer(Modifier.height(7.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(BrewColors.LineSoft)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(bean.remainingRatio())
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(status.inkHex.hexColor().copy(alpha = 0.72f))
                    )
                }
            }
        }
    }
}

@Composable
private fun AddBeanMiniCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(112.dp)
            .height(248.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(BrewColors.PaperSoft)
            .border(1.dp, BrewColors.Line, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "添加豆子" },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BrewColors.Card),
                contentAlignment = Alignment.Center
            ) {
                PlusIcon(Modifier.size(21.dp), BrewColors.Ink)
            }
            Spacer(Modifier.height(10.dp))
            Text("再收一支", style = MaterialTheme.typography.labelLarge, color = BrewColors.InkSoft)
        }
    }
}

@Composable
private fun RecentRecipeCard(recipe: Recipe, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = BrewColors.Card,
        border = androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 78.dp, height = 94.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BrewColors.PaperSoft),
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
                    RatingBeans(recipe.rating, filledColor = BrewColors.Ink)
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    recipe.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = BrewColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                MetaRow {
                    Text(
                        recipe.beanName.ifBlank { "未记豆子" },
                        style = MaterialTheme.typography.bodySmall,
                        color = BrewColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    com.brewkin.beanbook.ui.components.DotSeparator()
                    Text(
                        "${recipe.totalGrams}g · ${recipe.layers.size} 层",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrewColors.Muted
                    )
                }
                if (recipe.notes.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        recipe.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = BrewColors.InkSoft.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            ChevronRightIcon(Modifier.size(15.dp), BrewColors.MutedLight)
        }
    }
}

@Composable
private fun HomeFooter() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(Modifier.width(58.dp).height(16.dp)) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, size.height * 0.5f)
                cubicTo(
                    size.width * 0.28f, size.height * 0.1f,
                    size.width * 0.65f, size.height * 0.9f,
                    size.width, size.height * 0.35f
                )
            }
            drawPath(path, BrewColors.MutedLight, style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "每一杯，都是今天的注脚。",
            style = MaterialTheme.typography.bodySmall,
            color = BrewColors.Muted
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "BREWED SLOWLY · ${monthTitle()}",
            style = MaterialTheme.typography.labelSmall,
            color = BrewColors.MutedLight
        )
    }
}
