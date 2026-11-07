package com.brewkin.beanbook.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.brewkin.beanbook.data.CUP_CAPACITY
import com.brewkin.beanbook.data.IngredientKind
import com.brewkin.beanbook.data.RecipeLayer
import com.brewkin.beanbook.ui.theme.BrewColors
import com.brewkin.beanbook.ui.theme.hexColor
import kotlin.math.min

fun depth(color: Color, amount: Float = 0.22f): Color = lerp(color, Color.Black, amount)

fun lift(color: Color, amount: Float = 0.22f): Color = lerp(color, Color.White, amount)

fun blendedColor(layers: List<RecipeLayer>, fallback: Color = BrewColors.Line): Color {
    if (layers.isEmpty()) return fallback
    var r = 0f
    var g = 0f
    var b = 0f
    var w = 0f
    layers.forEach { layer ->
        val weight = layer.grams.toFloat() * (0.55f + 0.45f * layer.alpha)
        val c = layer.colorHex.hexColor()
        r += c.red * weight
        g += c.green * weight
        b += c.blue * weight
        w += weight
    }
    return if (w <= 0f) fallback else Color(r / w, g / w, b / w)
}

@Composable
fun CupDisplay(
    layers: List<RecipeLayer>,
    mixed: Boolean,
    modifier: Modifier = Modifier,
    normalizeFill: Boolean = false,
    emptyHint: (@Composable () -> Unit)? = null
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        CupCanvas(
            layers = layers,
            mixed = mixed,
            modifier = Modifier.fillMaxSize(),
            normalizeFill = normalizeFill
        )
        if (layers.isEmpty() && emptyHint != null) {
            emptyHint()
        }
    }
}

@Composable
fun MiniCup(
    layers: List<RecipeLayer>,
    mixed: Boolean,
    modifier: Modifier = Modifier
) {
    CupCanvas(layers, mixed, modifier, compact = true, normalizeFill = true)
}

@Composable
private fun CupCanvas(
    layers: List<RecipeLayer>,
    mixed: Boolean,
    modifier: Modifier,
    compact: Boolean = false,
    normalizeFill: Boolean = false
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val topY = h * 0.075f
        val bottomY = h * 0.94f
        val topHalf = w * 0.345f
        val bottomHalf = w * 0.265f
        val cx = w / 2f
        val stroke = (if (compact) 1.2f else 2.2f).dp.toPx()

        val outer = Path().apply {
            moveTo(cx - topHalf, topY)
            lineTo(cx + topHalf, topY)
            lineTo(cx + bottomHalf, bottomY - h * 0.055f)
            cubicTo(
                cx + bottomHalf, bottomY - h * 0.012f,
                cx + bottomHalf - w * 0.045f, bottomY,
                cx + bottomHalf - w * 0.095f, bottomY
            )
            lineTo(cx - bottomHalf + w * 0.095f, bottomY)
            cubicTo(
                cx - bottomHalf + w * 0.045f, bottomY,
                cx - bottomHalf, bottomY - h * 0.012f,
                cx - bottomHalf, bottomY - h * 0.055f
            )
            close()
        }
        val inner = Path().apply {
            val inset = (if (compact) 2.4f else 5f).dp.toPx()
            moveTo(cx - topHalf + inset, topY + inset)
            lineTo(cx + topHalf - inset, topY + inset)
            lineTo(cx + bottomHalf - inset, bottomY - h * 0.058f)
            cubicTo(
                cx + bottomHalf - inset, bottomY - h * 0.022f,
                cx + bottomHalf - w * 0.055f, bottomY - inset,
                cx + bottomHalf - w * 0.10f, bottomY - inset
            )
            lineTo(cx - bottomHalf + w * 0.10f, bottomY - inset)
            cubicTo(
                cx - bottomHalf + w * 0.055f, bottomY - inset,
                cx - bottomHalf + inset, bottomY - h * 0.022f,
                cx - bottomHalf + inset, bottomY - h * 0.058f
            )
            close()
        }

        // Table shadow.
        drawOval(
            color = BrewColors.Ink.copy(alpha = 0.08f),
            topLeft = Offset(cx - w * 0.29f, bottomY + h * 0.006f),
            size = Size(w * 0.58f, h * 0.038f)
        )

        // Glass base tint.
        drawPath(outer, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.20f))))
        clipPath(inner) {
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.10f))
                ),
                topLeft = Offset(cx - topHalf, topY),
                size = Size(topHalf * 2f, bottomY - topY)
            )
            drawLayers(layers, mixed, inner, topY, bottomY, h, w, compact, normalizeFill)
        }

        // Rim and outline.
        drawOval(
            color = Color.White.copy(alpha = 0.82f),
            topLeft = Offset(cx - topHalf, topY - h * 0.010f),
            size = Size(topHalf * 2f, h * 0.048f)
        )
        drawOval(
            color = BrewColors.Ink.copy(alpha = 0.32f),
            topLeft = Offset(cx - topHalf, topY - h * 0.010f),
            size = Size(topHalf * 2f, h * 0.048f),
            style = Stroke(width = stroke)
        )
        drawPath(outer, Color.White.copy(alpha = 0.75f), style = Stroke(width = stroke * 2.1f))
        drawPath(outer, BrewColors.Ink.copy(alpha = 0.56f), style = Stroke(width = stroke))
        drawPath(inner, BrewColors.Ink.copy(alpha = 0.10f), style = Stroke(width = stroke * 0.7f))

        // Glass highlight.
        val highlight = Path().apply {
            moveTo(cx - topHalf + w * 0.065f, topY + h * 0.065f)
            cubicTo(
                cx - topHalf + w * 0.025f, topY + h * 0.26f,
                cx - bottomHalf + w * 0.06f, bottomY - h * 0.28f,
                cx - bottomHalf + w * 0.11f, bottomY - h * 0.10f
            )
        }
        drawPath(
            highlight,
            Color.White.copy(alpha = 0.48f),
            style = Stroke(width = (if (compact) 1.4f else 3f).dp.toPx(), cap = StrokeCap.Round)
        )
        if (layers.isEmpty()) {
            drawPath(
                inner,
                BrewColors.Muted.copy(alpha = 0.40f),
                style = Stroke(
                    width = stroke * 0.8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()))
                )
            )
        }
    }
}

private fun DrawScope.drawLayers(
    layers: List<RecipeLayer>,
    mixed: Boolean,
    inner: Path,
    topY: Float,
    bottomY: Float,
    h: Float,
    w: Float,
    compact: Boolean,
    normalizeFill: Boolean
) {
    if (layers.isEmpty()) return
    val total = layers.sumOf { it.grams }.coerceAtLeast(1)
    val denominator = if (normalizeFill) total.toFloat() else CUP_CAPACITY.toFloat()
    val fillHeight = (bottomY - topY - h * 0.065f) * min(1f, total.toFloat() / denominator)
    val fillBottom = bottomY - h * 0.012f
    if (mixed) {
        val color = blendedColor(layers)
        drawRect(
            brush = Brush.verticalGradient(
                listOf(lift(color, 0.08f), color, depth(color, 0.18f)),
                startY = fillBottom - fillHeight,
                endY = fillBottom
            ),
            topLeft = Offset(0f, fillBottom - fillHeight),
            size = Size(w, fillHeight)
        )
        // A few quiet swirls mark a stirred drink.
        val swirl = Path().apply {
            moveTo(w * 0.38f, fillBottom - fillHeight * 0.8f)
            cubicTo(
                w * 0.55f, fillBottom - fillHeight * 0.6f,
                w * 0.40f, fillBottom - fillHeight * 0.35f,
                w * 0.62f, fillBottom - fillHeight * 0.15f
            )
        }
        drawPath(
            swirl,
            Color.White.copy(alpha = 0.13f),
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        )
        return
    }

    var y = fillBottom
    layers.forEach { layer ->
        val share = layer.grams.toFloat() / total
        val layerHeight = fillHeight * share
        val color = layer.colorHex.hexColor()
        val alpha = layer.alpha.coerceIn(0.28f, 1f)
        drawRect(
            brush = Brush.verticalGradient(
                listOf(lift(color, 0.04f).copy(alpha = alpha), depth(color, 0.10f).copy(alpha = alpha)),
                startY = y - layerHeight,
                endY = y
            ),
            topLeft = Offset(0f, y - layerHeight),
            size = Size(w, layerHeight)
        )
        // The lip where the next ingredient meets the last one.
        drawLine(
            color = Color.White.copy(alpha = 0.22f),
            start = Offset(0f, y - layerHeight),
            end = Offset(w, y - layerHeight),
            strokeWidth = (if (compact) 0.8f else 1.4f).dp.toPx()
        )
        when (layer.kind) {
            IngredientKind.ICE -> {
                val cube = (if (compact) 5f else 13f).dp.toPx()
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.24f),
                    topLeft = Offset(w * 0.36f, y - layerHeight * 0.65f),
                    size = Size(cube, cube),
                    cornerRadius = CornerRadius(cube * 0.25f)
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.17f),
                    topLeft = Offset(w * 0.57f, y - layerHeight * 0.48f),
                    size = Size(cube * 0.8f, cube * 0.8f),
                    cornerRadius = CornerRadius(cube * 0.2f)
                )
            }
            IngredientKind.SPARKLING -> {
                repeat(if (compact) 3 else 8) { i ->
                    val px = w * (0.30f + 0.045f * (i % 5))
                    val py = y - layerHeight * (0.25f + 0.12f * (i % 4))
                    drawCircle(Color.White.copy(alpha = 0.35f), radius = (if (compact) 1.2f else 2.3f).dp.toPx(), center = Offset(px, py))
                }
            }
            IngredientKind.FOAM, IngredientKind.SYRUP -> {
                repeat(if (compact) 2 else 5) { i ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.20f),
                        radius = (if (compact) 2.5f else 6f).dp.toPx() * (0.5f + i * 0.08f),
                        center = Offset(w * (0.33f + i * 0.09f), y - layerHeight * 0.55f)
                    )
                }
            }
            IngredientKind.CHUNK, IngredientKind.PUREE -> {
                repeat(if (compact) 2 else 4) { i ->
                    drawCircle(
                        color = lift(color, 0.25f).copy(alpha = 0.55f),
                        radius = (if (compact) 2.6f else 5.5f).dp.toPx(),
                        center = Offset(w * (0.32f + i * 0.11f), y - layerHeight * 0.5f)
                    )
                }
            }
            else -> Unit
        }
        y -= layerHeight
    }
}
