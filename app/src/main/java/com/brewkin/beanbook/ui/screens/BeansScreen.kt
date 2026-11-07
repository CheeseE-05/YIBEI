package com.brewkin.beanbook.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brewkin.beanbook.data.AppState
import com.brewkin.beanbook.data.Bean
import com.brewkin.beanbook.data.BeanStatus
import com.brewkin.beanbook.data.ageDays
import com.brewkin.beanbook.data.status
import com.brewkin.beanbook.ui.components.BeanArtwork
import com.brewkin.beanbook.ui.components.EmptyHint
import com.brewkin.beanbook.ui.components.Overline
import com.brewkin.beanbook.ui.components.PlusIcon
import com.brewkin.beanbook.ui.components.PrimaryButton
import com.brewkin.beanbook.ui.components.SelectableChip
import com.brewkin.beanbook.ui.components.StatusTag
import com.brewkin.beanbook.ui.theme.BrewColors
import com.brewkin.beanbook.ui.theme.formatGrams
import com.brewkin.beanbook.ui.theme.hexColor
import com.brewkin.beanbook.ui.theme.originFlag
import java.io.File

private enum class BeanFilter(val label: String) {
    ALL("全部"),
    ACTIVE("冲煮中"),
    PEAK("最佳赏味"),
    RESTING("养豆中"),
    FROZEN("冷冻")
}

@Composable
fun BeansScreen(
    state: AppState,
    imageFile: (Bean) -> File?,
    onAddBean: () -> Unit,
    onOpenBean: (String) -> Unit
) {
    var selectedFilter by rememberSaveable { mutableStateOf(BeanFilter.ALL.name) }
    val filter = BeanFilter.valueOf(selectedFilter)
    val filtered = state.beans
        .filter { bean ->
            when (filter) {
                BeanFilter.ALL -> true
                BeanFilter.ACTIVE -> bean.remainingGrams > 0
                BeanFilter.PEAK -> bean.status() == BeanStatus.PEAK
                BeanFilter.RESTING -> bean.status() == BeanStatus.RESTING
                BeanFilter.FROZEN -> bean.status() == BeanStatus.FROZEN
            }
        }
        .sortedWith(compareByDescending<Bean> { it.lastUsedAt }.thenByDescending { it.createdAt })

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BrewColors.Paper)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Overline("BEAN SHELF")
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "豆仓",
                        style = MaterialTheme.typography.displayMedium,
                        color = BrewColors.Ink
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BrewColors.Lime)
                        .border(0.dp, BrewColors.Lime, CircleShape)
                        .clickable(onClick = onAddBean)
                        .semantics { contentDescription = "添加豆子" },
                    contentAlignment = Alignment.Center
                ) {
                    PlusIcon(Modifier.size(22.dp), BrewColors.Ink)
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "豆仓总库存",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrewColors.Muted
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    formatGrams(state.totalRemaining()),
                    style = MaterialTheme.typography.titleLarge,
                    color = BrewColors.Forest
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(BrewColors.Lime)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    "${state.beans.size} 支在架",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrewColors.Muted
                )
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BeanFilter.entries) { item ->
                    SelectableChip(
                        text = item.label,
                        selected = selectedFilter == item.name,
                        onClick = { selectedFilter = item.name }
                    )
                }
            }
        }
        if (state.beans.isEmpty()) {
            item {
                EmptyHint(
                    title = "收下第一支豆子",
                    body = "拍一张豆袋照片，我们会自动把背景去掉。豆名、烘焙度、风味和克重，都可以慢慢补。",
                    action = {
                        PrimaryButton("添加豆子", onAddBean, Modifier.width(170.dp))
                    }
                )
            }
        } else if (filtered.isEmpty()) {
            item {
                EmptyHint(
                    title = "这里暂时没有豆子",
                    body = "换一个筛选条件，或者再收一支新豆。",
                )
            }
        } else {
            val pairs = filtered.chunked(2)
            items(pairs.size) { index ->
                val rowItems = pairs[index]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { bean ->
                        BeanShelfCard(
                            bean = bean,
                            imageFile = imageFile(bean),
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenBean(bean.id) }
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            item {
                BeanShelfNote()
            }
        }
    }
}

@Composable
private fun BeanShelfCard(
    bean: Bean,
    imageFile: File?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val status = bean.status()
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = BrewColors.Card,
        border = androidx.compose.foundation.BorderStroke(1.dp, BrewColors.LineSoft)
    ) {
        Column {
            Box {
                BeanArtwork(
                    bean = bean,
                    imageFile = imageFile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(158.dp)
                        .padding(8.dp),
                    radius = 22.dp
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(BrewColors.Card.copy(alpha = 0.82f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "第${bean.ageDays()}天",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
                        color = BrewColors.InkSoft
                    )
                }
            }
            Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 5.dp, bottom = 15.dp)) {
                Text(
                    bean.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = BrewColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${originFlag(bean.origin)} ${bean.displayOrigin}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrewColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${bean.roaster.ifBlank { "未记录烘焙商" }} · ${bean.process}",
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
                        color = status.inkHex.hexColor()
                    )
                    Text(
                        " / ${formatGrams(bean.totalGrams)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrewColors.Muted
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(BrewColors.PaperSoft)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            bean.roast.short,
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
                            color = BrewColors.InkSoft
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(BrewColors.LineSoft)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(bean.remainingRatio())
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(status.inkHex.hexColor().copy(alpha = 0.72f))
                    )
                }
            }
        }
    }
}

@Composable
private fun BeanShelfNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BrewColors.PaperSoft)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(BrewColors.Lime)
        )
        Spacer(Modifier.width(9.dp))
        Text(
            "照片会自动抠图。点开豆子，可以调克数、看赏味期和用它调过的杯。",
            style = MaterialTheme.typography.bodySmall,
            color = BrewColors.Muted
        )
    }
}
