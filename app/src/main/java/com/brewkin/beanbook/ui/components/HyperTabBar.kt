package com.brewkin.beanbook.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brewkin.beanbook.ui.theme.BrewColors
import kotlin.math.roundToInt
import kotlin.random.Random

/** 底部导航胶囊内的单个入口。 */
data class HyperTabItem(
    val route: String,
    val label: String,
    val icon: @Composable (Modifier, Color) -> Unit
)

private val CapsuleHeight = 62.dp
private val CapsuleRadius = CapsuleHeight / 2

/**
 * 选中玻璃滑块相对胶囊的统一内缩（上下左右都是 8dp）：
 * 滑块尺寸 = (itemWidth - 16dp) × (CapsuleHeight - 16dp)，圆角 = CapsuleRadius - 8dp = 23dp，
 * 因此滑块与外胶囊圆弧**同心**；滑块是半透明白玻璃，不是整块灰底。
 */
private val HighlightInset = 4.dp
private val HighlightHeight = CapsuleHeight - HighlightInset * 2
private val HighlightRadius = CapsuleRadius - HighlightInset

/** 选中滑块：浅灰玻璃（有存在感，但不是点击态灰块）。 */
private val HyperHighlight = Color(0xFFDEDED9)

private val FabSize = 58.dp

/** 参考视频里的 HyperOS 品牌绿（绿色圆形动作按钮，保持实心，不玻璃化）。 */
private val HyperFabGreen = Color(0xFF35D06A)

/** 玻璃胶囊底色：暖白半透明（隐约透出背景） */
private const val CapsuleFillAlpha = 0.90f

/**
 * 小米澎湃 OS 4 风格底部导航（v5：整块毛玻璃胶囊，无选中底块）。
 *
 * - 悬浮暖白半透明胶囊（alpha 0.90），整块背景叠磨砂层：暖白磨砂雾 + 4 个大范围径向色斑
 *   （`Modifier.blur(22dp)`，API 31+ 为真实 RenderEffect，低版本自动降级渐变）+ 200 颗细噪点，
 *   让身后列表内容只以柔和色块的形式隐约透出（毛玻璃观感，而不是清晰透出）；
 * - 1dp 白色高光描边（顶亮底淡）+ 极轻外阴影，不做重投影；
 * - 胶囊内 4 个「图标 + 文字」tab：选中项背后是**浅灰玻璃滑块**（覆盖图标+文字、与外胶囊同心、
 *   短促 spring 滑动），点击无灰色按压/水波纹效果（滑块本身即选中提示）；
 * - 胶囊右侧独立绿色圆形 FAB（58dp，实心品牌绿 + 1dp 白色内高光），选中时放大 + 柔光环。
 */
@Composable
fun HyperTabBar(
    tabs: List<HyperTabItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onMake: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (tabs.isEmpty()) return
    val isDark = BrewColors.isDark
    val makerSelected = currentRoute == "maker"
    val tabIndex = tabs.indexOfFirst { it.route == currentRoute }

    // 选中滑块停留在最后选中的 tab 上；去「调一杯」（FAB）时滑块淡出。
    var lastTabIndex by remember { mutableStateOf(tabIndex.coerceAtLeast(0)) }
    if (tabIndex >= 0) lastTabIndex = tabIndex
    val highlightProgress by animateFloatAsState(
        targetValue = lastTabIndex.toFloat(),
        // 小米系统应用式的短促顺滑。
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f),
        label = "hyperTabHighlight"
    )
    val highlightAlpha by animateFloatAsState(
        targetValue = if (tabIndex >= 0) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 500f),
        label = "hyperTabHighlightAlpha"
    )
    val fabScale by animateFloatAsState(
        targetValue = if (makerSelected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "hyperFabScale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // —— 悬浮毛玻璃胶囊导航条 ——
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(CapsuleHeight)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(CapsuleRadius),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.10f),
                    spotColor = Color.Black.copy(alpha = 0.10f)
                )
                .clip(RoundedCornerShape(CapsuleRadius))
        ) {
            // 1) 半透明暖白底：背景/列表在边缘与内部隐约透出
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        if (isDark) Color(0xFF23241C).copy(alpha = 0.84f)
                        else BrewColors.Card.copy(alpha = CapsuleFillAlpha)
                    )
            )
            // 2) 轻量磨砂层：只作用于胶囊内的小面积模糊色斑（API<31 自动降级为渐变）
            FrostBlobs()
            // 3) 极细白色噪点，消除平板玻璃感
            FrostGrain()
            // 4) 均匀奶雾：柔化身后真实内容的锐利轮廓，让「透」更像毛玻璃而不是纯透明
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            if (isDark) listOf(
                                Color(0xFF14140F).copy(alpha = 0.42f),
                                Color(0xFF191913).copy(alpha = 0.34f),
                                Color(0xFF1E1E16).copy(alpha = 0.26f)
                            ) else listOf(
                                Color.White.copy(alpha = 0.30f),
                                Color.White.copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.17f)
                            )
                        )
                    )
            )
            // 5) 顶部柔光：上亮下暗
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(CapsuleHeight * 0.62f)
                    .background(
                        Brush.verticalGradient(
                            if (isDark) listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)
                            else listOf(Color.White.copy(alpha = 0.26f), Color.Transparent)
                        )
                    )
            )
            // 6) 1dp 白色高光描边：顶亮底淡
            Box(
                Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            if (isDark) listOf(
                                Color.White.copy(alpha = 0.20f),
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.05f)
                            ) else listOf(
                                Color.White.copy(alpha = 0.90f),
                                Color.White.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.20f)
                            )
                        ),
                        shape = RoundedCornerShape(CapsuleRadius)
                    )
            )

            val itemWidth = maxWidth / tabs.size

            // 7) 选中玻璃滑块：半透明白玻璃 + 1dp 高光边，与外胶囊同心；
            //    不是「整块变灰」的底色块，点击也不产生灰色按压效果。
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (itemWidth.toPx() * highlightProgress + HighlightInset.toPx()).roundToInt(),
                            y = HighlightInset.roundToPx()
                        )
                    }
                    .width(itemWidth - HighlightInset * 2)
                    .height(HighlightHeight)
                    .clip(RoundedCornerShape(HighlightRadius))
                    .background(
                        Brush.verticalGradient(
                            if (isDark) listOf(
                                Color(0xFF3D3F31).copy(alpha = 0.96f * highlightAlpha),
                                Color(0xFF36382B).copy(alpha = 0.94f * highlightAlpha),
                                Color(0xFF2F3126).copy(alpha = 0.92f * highlightAlpha)
                            ) else listOf(
                                HyperHighlight.copy(alpha = 0.92f * highlightAlpha),
                                HyperHighlight.copy(alpha = 0.84f * highlightAlpha),
                                HyperHighlight.copy(alpha = 0.74f * highlightAlpha)
                            )
                        )
                    )
            ) {
                // 顶部白高光 + 1dp 浅色 rim，让滑块像一块贴住胶囊的浅灰玻璃片。
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = (if (isDark) 0.12f else 0.55f) * highlightAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.98f * highlightAlpha),
                                    Color.White.copy(alpha = (if (isDark) 0.12f else 0.55f) * highlightAlpha),
                                    Color(0xFFDEDDD6).copy(alpha = 0.45f * highlightAlpha)
                                )
                            ),
                            shape = RoundedCornerShape(HighlightRadius)
                        )
                )
            }

            // 8) 4 个入口
            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEach { item ->
                    val isSelected = item.route == currentRoute
                    // 不画任何“按下去”的灰色底块：只靠颜色/字重区分选中项。
                    val contentColor = when {
                        isDark && isSelected -> Color(0xFFF6F3EA)
                        isDark -> Color.White.copy(alpha = 0.64f)
                        isSelected -> BrewColors.Ink
                        else -> BrewColors.Ink.copy(alpha = 0.55f)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onNavigate(item.route) }
                            )
                            .semantics {
                                contentDescription = item.label
                                selected = isSelected
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        item.icon(Modifier.size(22.dp), contentColor)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                letterSpacing = 0.2.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Visible
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // —— 右侧独立绿色圆形动作按钮：「调一杯」（实心品牌绿 + 1dp 白色内高光）——
        // 注意：容器宽度固定为 FabSize+16dp，选中放大/光环不改变布局宽度，
        // 否则 FAB 变宽会把左侧 weight(1f) 的胶囊挤小（用户反馈的真实 bug）。
        Box(
            modifier = Modifier.size(FabSize + 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (makerSelected) {
                Box(
                    Modifier
                        .size(FabSize + 12.dp)
                        .background(HyperFabGreen.copy(alpha = 0.18f), CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(FabSize)
                    .scale(fabScale)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.12f),
                        spotColor = Color.Black.copy(alpha = 0.12f)
                    )
                    .clip(CircleShape)
                    .background(HyperFabGreen)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onMake
                    )
                    .semantics {
                        contentDescription = "调一杯"
                        selected = makerSelected
                    }
                    .semantics {
                        contentDescription = "调一杯"
                        selected = makerSelected
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .border(1.dp, Color.White.copy(alpha = 0.32f), CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.18f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CupIcon(Modifier.size(27.dp), Color.White)
                }
            }
        }
    }
}

/** 只作用于胶囊内的磨砂色斑层：模糊色块模拟「透过毛玻璃看到的模糊背景意象」。 */
@Composable
private fun FrostBlobs() {
    val dark = BrewColors.isDark
    Canvas(
        Modifier
            .fillMaxSize()
            // API 31+ 为真实高斯模糊；低版本自动降级为无 blur 的柔和径向渐变。
            .blur(22.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
    ) {
        val w = size.width
        val h = size.height
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    if (dark) Color(0xFF3D3F31).copy(alpha = 0.55f)
                    else BrewColors.PaperSoft.copy(alpha = 0.64f),
                    Color.Transparent
                )
            ),
            radius = w * 0.42f,
            center = Offset(w * 0.14f, h * 0.44f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    if (dark) BrewColors.LimeDeep.copy(alpha = 0.20f)
                    else BrewColors.LimeSoft.copy(alpha = 0.48f),
                    Color.Transparent
                )
            ),
            radius = w * 0.36f,
            center = Offset(w * 0.86f, h * 0.24f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    if (dark) Color(0xFF4A4C3C).copy(alpha = 0.16f)
                    else BrewColors.Ink.copy(alpha = 0.06f),
                    Color.Transparent
                )
            ),
            radius = w * 0.40f,
            center = Offset(w * 0.58f, h * 1.02f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    if (dark) Color(0xFF4A4C3C).copy(alpha = 0.30f)
                    else Color.White.copy(alpha = 0.55f),
                    Color.Transparent
                )
            ),
            radius = w * 0.30f,
            center = Offset(w * 0.38f, h * 0.30f)
        )
        // 整条胶囊再铺一层大范围暖白磨砂雾，弱化身后内容的锐利边缘。
        drawRect(
            brush = Brush.linearGradient(
                if (dark) listOf(
                    Color(0xFF1A1A13).copy(alpha = 0.30f),
                    Color(0xFF24251C).copy(alpha = 0.22f),
                    Color(0xFF1A1A13).copy(alpha = 0.16f)
                ) else listOf(
                    Color.White.copy(alpha = 0.20f),
                    BrewColors.PaperSoft.copy(alpha = 0.14f),
                    Color.White.copy(alpha = 0.10f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w * 0.6f, h)
            )
        )
    }
}

/** 预生成的极细白色磨砂颗粒，alpha 0.02~0.03。 */
@Composable
private fun FrostGrain() {
    val dots = remember {
        val random = Random(0x51DE)
        List(200) {
            Triple(
                random.nextFloat(),
                random.nextFloat(),
                0.022f + random.nextFloat() * 0.014f
            )
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        dots.forEach { (x, y, alpha) ->
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = 0.5.dp.toPx(),
                center = Offset(x * size.width, y * size.height)
            )
        }
    }
}

/**
 * 详情页 / 编辑页底部的悬浮白色圆角操作条（HyperOS 语言，不做玻璃高光）。
 *
 * 左右 16dp、底部 8dp（另加系统导航条 inset）、圆角 28dp、暖白 ~0.97 表面 + 柔和阴影；
 * 内容自己放在 Row 里（按钮建议 52dp 高）。
 */
@Composable
fun HyperBottomActionBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(28.dp),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.10f),
                    spotColor = Color.Black.copy(alpha = 0.10f)
                ),
            shape = RoundedCornerShape(28.dp),
            color = BrewColors.Card.copy(alpha = 0.97f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}
