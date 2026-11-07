package com.brewkin.beanbook.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.brewkin.beanbook.data.Bean
import com.brewkin.beanbook.data.Recipe
import com.brewkin.beanbook.data.ageDays
import com.brewkin.beanbook.data.peakHint
import com.brewkin.beanbook.data.status
import com.brewkin.beanbook.ui.components.BackIcon
import com.brewkin.beanbook.ui.components.BeanPlaceholder
import com.brewkin.beanbook.ui.components.CircleIconButton
import com.brewkin.beanbook.ui.components.DotSeparator
import com.brewkin.beanbook.ui.components.EditIcon
import com.brewkin.beanbook.ui.components.EmptyHint
import com.brewkin.beanbook.ui.components.HyperBottomActionBar
import com.brewkin.beanbook.ui.components.MiniCup
import com.brewkin.beanbook.ui.components.Overline
import com.brewkin.beanbook.ui.components.PrimaryButton
import com.brewkin.beanbook.ui.components.RatingBeans
import com.brewkin.beanbook.ui.components.SecondaryButton
import com.brewkin.beanbook.ui.components.SectionHeading
import com.brewkin.beanbook.ui.components.StatusTag
import com.brewkin.beanbook.ui.components.SurfaceCard
import com.brewkin.beanbook.ui.components.TagPill
import com.brewkin.beanbook.ui.theme.BrewColors
import com.brewkin.beanbook.ui.theme.DMMono
import com.brewkin.beanbook.ui.theme.formatGrams
import com.brewkin.beanbook.ui.theme.hexColor
import com.brewkin.beanbook.ui.theme.originFlag
import com.brewkin.beanbook.ui.theme.toShortDate
import com.brewkin.beanbook.ui.theme.toTimeLabel
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BeanDetailScreen(
    bean: Bean,
    recipes: List<Recipe>,
    imageFile: File?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onMake: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val status = bean.status()
    val related = recipes.filter { it.beanId == bean.id }.sortedByDescending { it.createdAt }

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
                        onClick = onEdit,
                        description = "编辑豆子",
                        background = BrewColors.Card,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
                    ) { EditIcon(Modifier.size(18.dp), BrewColors.Ink) }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(306.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(status.bgHex.hexColor()),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        drawCircle(
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.30f),
                            radius = size.minDimension * 0.36f,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.52f)
                        )
                        drawCircle(
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f),
                            radius = size.minDimension * 0.20f,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.22f, size.height * 0.24f)
                        )
                    }
                    if (imageFile != null) {
                        Image(
                            painter = rememberAsyncImagePainter(imageFile),
                            contentDescription = "${bean.name} 豆袋",
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        BeanPlaceholder(bean, Modifier.fillMaxSize().padding(30.dp))
                    }
                    Row(
                        modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusTag(status)
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp)
                            .clip(CircleShape)
                            .background(BrewColors.Card.copy(alpha = 0.82f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "第 ${bean.ageDays()} 天",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrewColors.InkSoft
                        )
                    }
                }
            }
            item {
                Column {
                    Text(
                        bean.name,
                        style = MaterialTheme.typography.displayMedium,
                        color = BrewColors.Ink
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${originFlag(bean.origin)} ${bean.displayOrigin}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrewColors.Muted
                        )
                        Spacer(Modifier.width(7.dp))
                        DotSeparator()
                        Spacer(Modifier.width(7.dp))
                        Text(
                            bean.roaster.ifBlank { "未记录烘焙商" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrewColors.Muted
                        )
                    }
                }
            }
            if (bean.flavors.isNotEmpty()) {
                item {
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        bean.flavors.forEach {
                            TagPill(
                                it,
                                background = BrewColors.Card,
                                foreground = BrewColors.InkSoft,
                                dotColor = BrewColors.Lime
                            )
                        }
                    }
                }
            }
            item {
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Overline("BEST WINDOW")
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    status.label,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = status.inkHex.hexColor()
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    formatGrams(bean.remainingGrams),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontFamily = DMMono,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = BrewColors.Ink
                                )
                                Text(
                                    "余量 / ${formatGrams(bean.totalGrams)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrewColors.Muted
                                )
                            }
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
                                    .fillMaxWidth(bean.remainingRatio())
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(status.inkHex.hexColor().copy(alpha = 0.78f))
                            )
                        }
                        Spacer(Modifier.height(9.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                bean.peakHint(),
                                style = MaterialTheme.typography.bodySmall,
                                color = BrewColors.Muted
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                bean.storage.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = BrewColors.Muted
                            )
                        }
                    }
                }
            }
            item {
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Overline("BEAN DETAILS")
                        Spacer(Modifier.height(13.dp))
                        Row {
                            DetailCell("烘焙度", bean.roast.label, Modifier.weight(1f))
                            DetailCell("处理法", bean.process, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(16.dp))
                        Row {
                            DetailCell("产区", bean.region.ifBlank { "未记录" }, Modifier.weight(1f))
                            DetailCell("烘焙日", bean.roastDate.toShortDate(), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(16.dp))
                        Row {
                            DetailCell("总克重", formatGrams(bean.totalGrams), Modifier.weight(1f))
                            DetailCell("剩余", formatGrams(bean.remainingGrams), Modifier.weight(1f))
                        }
                    }
                }
            }
            if (bean.note.isNotBlank()) {
                item {
                    Row(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(54.dp)
                                .clip(CircleShape)
                                .background(BrewColors.Lime)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Overline("TASTING NOTE")
                            Spacer(Modifier.height(5.dp))
                            Text(
                                bean.note,
                                style = MaterialTheme.typography.bodyLarge,
                                color = BrewColors.InkSoft
                            )
                        }
                    }
                }
            }
            item {
                SectionHeading(
                    title = "用这支豆调过",
                    overline = "CUPS WITH THIS BEAN"
                )
            }
            if (related.isEmpty()) {
                item {
                    EmptyHint(
                        title = "还没有用过这支豆",
                        body = "去调一杯，它会自动记到这支豆的名下。",
                        action = {
                            PrimaryButton("用这支豆调一杯", onMake, Modifier.width(200.dp))
                        }
                    )
                }
            } else {
                items(related.size) { index ->
                    val recipe = related[index]
                    BeanRecipeRow(recipe) { onOpenRecipe(recipe.id) }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { confirmDelete = true }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "删除这支豆子",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrewColors.Muted
                    )
                }
            }
        }

        HyperBottomActionBar(modifier = Modifier.align(Alignment.BottomCenter)) {
            SecondaryButton("编辑", onEdit, Modifier.weight(1f))
            PrimaryButton("调一杯", onMake, Modifier.weight(1.4f))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = BrewColors.Card,
            title = { Text("删除这支豆子？", color = BrewColors.Ink) },
            text = {
                Text(
                    "豆子会从豆仓移除；已经调过的配方还会留在目录里。",
                    color = BrewColors.Muted
                )
            },
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
private fun DetailCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = BrewColors.Muted
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = BrewColors.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BeanRecipeRow(recipe: Recipe, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = BrewColors.Card,
        border = androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 58.dp, height = 70.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BrewColors.PaperSoft),
                contentAlignment = Alignment.Center
            ) {
                MiniCup(
                    recipe.layers,
                    recipe.stirred,
                    Modifier.fillMaxSize().padding(8.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = BrewColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        recipe.createdAt.toTimeLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = BrewColors.Muted
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
            }
            RatingBeans(recipe.rating, filledColor = BrewColors.Ink)
        }
    }
}
