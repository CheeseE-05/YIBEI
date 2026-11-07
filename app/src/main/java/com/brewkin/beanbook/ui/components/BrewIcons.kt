package com.brewkin.beanbook.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val ICON_VIEW = 24f

private fun DrawScope.iconStroke(width: Dp = 1.8.dp): Stroke = Stroke(
    width = width.toPx() * (size.minDimension / (ICON_VIEW.dp.toPx())),
    cap = StrokeCap.Round,
    join = StrokeJoin.Round
)

private fun DrawScope.p(x: Float, y: Float): Offset =
    Offset(size.width * x / ICON_VIEW, size.height * y / ICON_VIEW)

private fun DrawScope.s(v: Float): Float = size.minDimension * v / ICON_VIEW

@Composable
private fun Glyph(modifier: Modifier, color: Color, draw: DrawScope.(Color) -> Unit) {
    Canvas(modifier = modifier) { draw(color) }
}

@Composable
fun HomeIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke()
    val roof = Path().apply {
        moveTo(p(3f, 10.5f).x, p(3f, 10.5f).y)
        lineTo(p(12f, 3.5f).x, p(12f, 3.5f).y)
        lineTo(p(21f, 10.5f).x, p(21f, 10.5f).y)
    }
    drawPath(roof, c, style = stroke)
    val body = Path().apply {
        moveTo(p(5.2f, 9.3f).x, p(5.2f, 9.3f).y)
        lineTo(p(5.2f, 20f).x, p(5.2f, 20f).y)
        lineTo(p(18.8f, 20f).x, p(18.8f, 20f).y)
        lineTo(p(18.8f, 9.3f).x, p(18.8f, 9.3f).y)
    }
    drawPath(body, c, style = Stroke(width = stroke.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawLine(c, p(10f, 20f), p(10f, 14.4f), stroke.width, StrokeCap.Round)
    drawLine(c, p(10f, 14.4f), p(14f, 14.4f), stroke.width, StrokeCap.Round)
    drawLine(c, p(14f, 14.4f), p(14f, 20f), stroke.width, StrokeCap.Round)
}

@Composable
fun BeanIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val bean = Path().apply {
        moveTo(p(6.3f, 4.9f).x, p(6.3f, 4.9f).y)
        cubicTo(
            p(10.7f, 2.1f).x, p(10.7f, 2.1f).y,
            p(18.2f, 3.8f).x, p(18.2f, 3.8f).y,
            p(19.7f, 9.0f).x, p(19.7f, 9.0f).y
        )
        cubicTo(
            p(21.2f, 14.2f).x, p(21.2f, 14.2f).y,
            p(18.2f, 20.2f).x, p(18.2f, 20.2f).y,
            p(12.9f, 20.9f).x, p(12.9f, 20.9f).y
        )
        cubicTo(
            p(7.6f, 21.6f).x, p(7.6f, 21.6f).y,
            p(2.9f, 17.4f).x, p(2.9f, 17.4f).y,
            p(3.2f, 12.2f).x, p(3.2f, 12.2f).y
        )
        cubicTo(
            p(3.4f, 8.7f).x, p(3.4f, 8.7f).y,
            p(4.2f, 6.2f).x, p(4.2f, 6.2f).y,
            p(6.3f, 4.9f).x, p(6.3f, 4.9f).y
        )
        close()
    }
    drawPath(bean, c)
    val crease = Path().apply {
        moveTo(p(11.4f, 6.7f).x, p(11.4f, 6.7f).y)
        cubicTo(
            p(14.0f, 9.0f).x, p(14.0f, 9.0f).y,
            p(9.2f, 11.0f).x, p(9.2f, 11.0f).y,
            p(10.3f, 13.0f).x, p(10.3f, 13.0f).y
        )
        cubicTo(
            p(11.6f, 15.2f).x, p(11.6f, 15.2f).y,
            p(13.3f, 16.3f).x, p(13.3f, 16.3f).y,
            p(12.8f, 18.2f).x, p(12.8f, 18.2f).y
        )
    }
    drawPath(
        crease,
        Color.White.copy(alpha = 0.82f),
        style = Stroke(width = iconStroke().width * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

@Composable
fun CupIcon(modifier: Modifier = Modifier, color: Color = Color.Black, filled: Boolean = false) =
    Glyph(modifier, color) { c ->
        val stroke = iconStroke()
        val cup = Path().apply {
            moveTo(p(4.8f, 7.6f).x, p(4.8f, 7.6f).y)
            lineTo(p(17.2f, 7.6f).x, p(17.2f, 7.6f).y)
            lineTo(p(16.2f, 17.1f).x, p(16.2f, 17.1f).y)
            cubicTo(
                p(16f, 18.7f).x, p(16f, 18.7f).y,
                p(14.8f, 20f).x, p(14.8f, 20f).y,
                p(13.2f, 20f).x, p(13.2f, 20f).y
            )
            lineTo(p(8.8f, 20f).x, p(8.8f, 20f).y)
            cubicTo(
                p(7.2f, 20f).x, p(7.2f, 20f).y,
                p(6f, 18.7f).x, p(6f, 18.7f).y,
                p(5.8f, 17.1f).x, p(5.8f, 17.1f).y
            )
            close()
        }
        if (filled) {
            drawPath(cup, c)
        } else {
            drawPath(cup, c, style = stroke)
            val handle = Path().apply {
                moveTo(p(17f, 9.3f).x, p(17f, 9.3f).y)
                cubicTo(
                    p(21.8f, 9f).x, p(21.8f, 9f).y,
                    p(21.2f, 15.1f).x, p(21.2f, 15.1f).y,
                    p(16f, 14.7f).x, p(16f, 14.7f).y
                )
            }
            drawPath(handle, c, style = stroke)
            val steam = Path().apply {
                moveTo(p(9.2f, 5f).x, p(9.2f, 5f).y)
                cubicTo(
                    p(7.8f, 3.5f).x, p(7.8f, 3.5f).y,
                    p(10.4f, 2.8f).x, p(10.4f, 2.8f).y,
                    p(9f, 1.4f).x, p(9f, 1.4f).y
                )
                moveTo(p(13.6f, 5f).x, p(13.6f, 5f).y)
                cubicTo(
                    p(12.2f, 3.5f).x, p(12.2f, 3.5f).y,
                    p(14.8f, 2.8f).x, p(14.8f, 2.8f).y,
                    p(13.4f, 1.4f).x, p(13.4f, 1.4f).y
                )
            }
            drawPath(steam, c, style = Stroke(width = stroke.width * 0.8f, cap = stroke.cap, join = stroke.join))
        }
    }

@Composable
fun BookIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke()
    val left = Path().apply {
        moveTo(p(12f, 5.1f).x, p(12f, 5.1f).y)
        cubicTo(
            p(9.4f, 3f).x, p(9.4f, 3f).y,
            p(5.2f, 3.5f).x, p(5.2f, 3.5f).y,
            p(3.5f, 5f).x, p(3.5f, 5f).y
        )
        lineTo(p(3.5f, 18.6f).x, p(3.5f, 18.6f).y)
        cubicTo(
            p(6f, 17.2f).x, p(6f, 17.2f).y,
            p(9.3f, 17.5f).x, p(9.3f, 17.5f).y,
            p(12f, 19.2f).x, p(12f, 19.2f).y
        )
    }
    drawPath(left, c, style = stroke)
    val right = Path().apply {
        moveTo(p(12f, 5.1f).x, p(12f, 5.1f).y)
        cubicTo(
            p(14.6f, 3f).x, p(14.6f, 3f).y,
            p(18.8f, 3.5f).x, p(18.8f, 3.5f).y,
            p(20.5f, 5f).x, p(20.5f, 5f).y
        )
        lineTo(p(20.5f, 18.6f).x, p(20.5f, 18.6f).y)
        cubicTo(
            p(18f, 17.2f).x, p(18f, 17.2f).y,
            p(14.7f, 17.5f).x, p(14.7f, 17.5f).y,
            p(12f, 19.2f).x, p(12f, 19.2f).y
        )
    }
    drawPath(right, c, style = stroke)
    drawLine(c, p(12f, 5.1f), p(12f, 19.2f), stroke.width, StrokeCap.Round)
}

@Composable
fun ChartIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke()
    drawLine(c, p(4f, 20f), p(20f, 20f), stroke.width, StrokeCap.Round)
    drawLine(c, p(5.5f, 20f), p(5.5f, 13.5f), stroke.width * 1.25f, StrokeCap.Round)
    drawLine(c, p(12f, 20f), p(12f, 8.2f), stroke.width * 1.25f, StrokeCap.Round)
    drawLine(c, p(18.5f, 20f), p(18.5f, 4.2f), stroke.width * 1.25f, StrokeCap.Round)
}

@Composable
fun PlusIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke(2.dp)
    drawLine(c, p(12f, 5f), p(12f, 19f), stroke.width, StrokeCap.Round)
    drawLine(c, p(5f, 12f), p(19f, 12f), stroke.width, StrokeCap.Round)
}

@Composable
fun BackIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke()
    drawLine(c, p(19f, 12f), p(5f, 12f), stroke.width, StrokeCap.Round)
    drawLine(c, p(5f, 12f), p(11f, 6f), stroke.width, StrokeCap.Round)
    drawLine(c, p(5f, 12f), p(11f, 18f), stroke.width, StrokeCap.Round)
}

@Composable
fun CloseIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke(2.dp)
    drawLine(c, p(6f, 6f), p(18f, 18f), stroke.width, StrokeCap.Round)
    drawLine(c, p(18f, 6f), p(6f, 18f), stroke.width, StrokeCap.Round)
}

@Composable
fun ChevronRightIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke()
    drawLine(c, p(9f, 5.5f), p(15.5f, 12f), stroke.width, StrokeCap.Round)
    drawLine(c, p(15.5f, 12f), p(9f, 18.5f), stroke.width, StrokeCap.Round)
}

@Composable
fun UndoIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke()
    val path = Path().apply {
        moveTo(p(4.5f, 9f).x, p(4.5f, 9f).y)
        lineTo(p(4.5f, 4.5f).x, p(4.5f, 4.5f).y)
        moveTo(p(4.8f, 8.7f).x, p(4.8f, 8.7f).y)
        cubicTo(
            p(7f, 4.2f).x, p(7f, 4.2f).y,
            p(14.2f, 3.2f).x, p(14.2f, 3.2f).y,
            p(18.4f, 7.1f).x, p(18.4f, 7.1f).y
        )
        cubicTo(
            p(22.9f, 11.2f).x, p(22.9f, 11.2f).y,
            p(20.1f, 20f).x, p(20.1f, 20f).y,
            p(11.7f, 20f).x, p(11.7f, 20f).y
        )
    }
    drawPath(path, c, style = stroke)
}

@Composable
fun TrashIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke()
    drawLine(c, p(4.5f, 6.5f), p(19.5f, 6.5f), stroke.width, StrokeCap.Round)
    drawLine(c, p(9.2f, 3.8f), p(14.8f, 3.8f), stroke.width, StrokeCap.Round)
    val can = Path().apply {
        moveTo(p(6.4f, 6.8f).x, p(6.4f, 6.8f).y)
        lineTo(p(7.3f, 19.4f).x, p(7.3f, 19.4f).y)
        cubicTo(
            p(7.4f, 20.2f).x, p(7.4f, 20.2f).y,
            p(8f, 20.6f).x, p(8f, 20.6f).y,
            p(8.8f, 20.6f).x, p(8.8f, 20.6f).y
        )
        lineTo(p(15.2f, 20.6f).x, p(15.2f, 20.6f).y)
        cubicTo(
            p(16f, 20.6f).x, p(16f, 20.6f).y,
            p(16.6f, 20.2f).x, p(16.6f, 20.2f).y,
            p(16.7f, 19.4f).x, p(16.7f, 19.4f).y
        )
        lineTo(p(17.6f, 6.8f).x, p(17.6f, 6.8f).y)
    }
    drawPath(can, c, style = stroke)
    drawLine(c, p(10.3f, 10f), p(10.6f, 17.3f), stroke.width * 0.8f, StrokeCap.Round)
    drawLine(c, p(13.7f, 10f), p(13.4f, 17.3f), stroke.width * 0.8f, StrokeCap.Round)
}

@Composable
fun EditIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke()
    val path = Path().apply {
        moveTo(p(5f, 19f).x, p(5f, 19f).y)
        lineTo(p(5.7f, 14.3f).x, p(5.7f, 14.3f).y)
        lineTo(p(15.5f, 4.5f).x, p(15.5f, 4.5f).y)
        lineTo(p(19.5f, 8.5f).x, p(19.5f, 8.5f).y)
        lineTo(p(9.7f, 18.3f).x, p(9.7f, 18.3f).y)
        close()
    }
    drawPath(path, c, style = stroke)
    drawLine(c, p(13.4f, 6.6f), p(17.4f, 10.6f), stroke.width, StrokeCap.Round)
}

@Composable
fun CameraIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke()
    drawRoundRect(
        color = c,
        topLeft = p(3.2f, 7.3f),
        size = Size(s(17.6f), s(12.2f)),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(3.1f)),
        style = stroke
    )
    drawRoundRect(
        color = c,
        topLeft = p(8f, 4.5f),
        size = Size(s(8f), s(4.2f)),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(1.5f)),
        style = stroke
    )
    drawCircle(c, s(3.2f), p(12f, 13.4f), style = stroke)
}

@Composable
fun ShareIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke()
    drawRoundRect(
        color = c,
        topLeft = p(4.5f, 9.5f),
        size = Size(s(15f), s(11f)),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(2f)),
        style = stroke
    )
    val arrow = Path().apply {
        moveTo(p(12f, 3.5f).x, p(12f, 3.5f).y)
        lineTo(p(12f, 13.5f).x, p(12f, 13.5f).y)
        moveTo(p(8.2f, 7.1f).x, p(8.2f, 7.1f).y)
        lineTo(p(12f, 3.4f).x, p(12f, 3.4f).y)
        lineTo(p(15.8f, 7.1f).x, p(15.8f, 7.1f).y)
    }
    drawPath(arrow, c, style = stroke)
}

@Composable
fun StirIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke()
    val path = Path().apply {
        moveTo(p(19.5f, 12f).x, p(19.5f, 12f).y)
        cubicTo(
            p(19.5f, 16.1f).x, p(19.5f, 16.1f).y,
            p(16.1f, 19.5f).x, p(16.1f, 19.5f).y,
            p(12f, 19.5f).x, p(12f, 19.5f).y
        )
        cubicTo(
            p(7.9f, 19.5f).x, p(7.9f, 19.5f).y,
            p(4.5f, 16.1f).x, p(4.5f, 16.1f).y,
            p(4.5f, 12f).x, p(4.5f, 12f).y
        )
        cubicTo(
            p(4.5f, 7.9f).x, p(4.5f, 7.9f).y,
            p(7.9f, 4.5f).x, p(7.9f, 4.5f).y,
            p(12f, 4.5f).x, p(12f, 4.5f).y
        )
        cubicTo(
            p(15.2f, 4.5f).x, p(15.2f, 4.5f).y,
            p(18f, 6.4f).x, p(18f, 6.4f).y,
            p(19.2f, 9.2f).x, p(19.2f, 9.2f).y
        )
        moveTo(p(19.2f, 4.7f).x, p(19.2f, 4.7f).y)
        lineTo(p(19.2f, 9.2f).x, p(19.2f, 9.2f).y)
        lineTo(p(14.8f, 9.2f).x, p(14.8f, 9.2f).y)
    }
    drawPath(path, c, style = stroke)
}

@Composable
fun CheckIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke(2.dp)
    drawLine(c, p(5f, 12.3f), p(10f, 17.2f), stroke.width, StrokeCap.Round)
    drawLine(c, p(10f, 17.2f), p(19f, 6.8f), stroke.width, StrokeCap.Round)
}

@Composable
fun MoreIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    drawCircle(c, s(1.6f), p(5.5f, 12f))
    drawCircle(c, s(1.6f), p(12f, 12f))
    drawCircle(c, s(1.6f), p(18.5f, 12f))
}

@Composable
fun BeanRatingGlyph(modifier: Modifier = Modifier, color: Color, filled: Boolean) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        rotate(-22f) {
            if (filled) {
                drawOval(color, topLeft = Offset(w * 0.16f, h * 0.05f), size = Size(w * 0.66f, h * 0.9f))
                drawOval(
                    color = Color.White.copy(alpha = 0.0f),
                    topLeft = Offset(w * 0.42f, h * 0.1f),
                    size = Size(w * 0.08f, h * 0.8f)
                )
                val crease = Path().apply {
                    moveTo(w * 0.5f, h * 0.13f)
                    cubicTo(w * 0.67f, h * 0.31f, w * 0.34f, h * 0.65f, w * 0.5f, h * 0.87f)
                }
                drawPath(
                    crease,
                    Color.White.copy(alpha = 0.72f),
                    style = Stroke(width = w * 0.055f, cap = StrokeCap.Round)
                )
            } else {
                drawOval(
                    color.copy(alpha = 0.26f),
                    topLeft = Offset(w * 0.16f, h * 0.05f),
                    size = Size(w * 0.66f, h * 0.9f),
                    style = Stroke(width = w * 0.07f)
                )
            }
        }
    }
}

@Composable
fun DotRow(modifier: Modifier = Modifier, count: Int = 5, filled: Int, color: Color = Color.Black) {
    Canvas(modifier = modifier) {
        val radius = size.height * 0.18f
        val gap = size.width / count
        for (i in 0 until count) {
            drawCircle(
                color = if (i < filled) color else color.copy(alpha = 0.16f),
                radius = radius,
                center = Offset(gap * i + gap / 2f, size.height / 2f)
            )
        }
    }
}

@Composable
fun SparkIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke()
    val path = Path().apply {
        moveTo(p(12f, 3.5f).x, p(12f, 3.5f).y)
        lineTo(p(13.7f, 9f).x, p(13.7f, 9f).y)
        lineTo(p(19.7f, 10.3f).x, p(19.7f, 10.3f).y)
        lineTo(p(13.7f, 11.8f).x, p(13.7f, 11.8f).y)
        lineTo(p(12f, 20.5f).x, p(12f, 20.5f).y)
        lineTo(p(10.4f, 11.8f).x, p(10.4f, 11.8f).y)
        lineTo(p(4.5f, 10.3f).x, p(4.5f, 10.3f).y)
        lineTo(p(10.4f, 9f).x, p(10.4f, 9f).y)
        close()
    }
    drawPath(path, c, style = stroke)
}

// —— 卡片编辑器（iOS SF Symbols 风格）新增图标 ——

/** 设计工具：三条横向滑杆 + 圆点（slider.horizontal.3）。 */
@Composable
fun SlidersIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke()
    drawLine(c, p(4f, 7f), p(20f, 7f), stroke.width, StrokeCap.Round)
    drawLine(c, p(4f, 12f), p(20f, 12f), stroke.width, StrokeCap.Round)
    drawLine(c, p(4f, 17f), p(20f, 17f), stroke.width, StrokeCap.Round)
    drawCircle(c, s(1.9f), p(9f, 7f), style = Stroke(width = stroke.width))
    drawCircle(c, s(1.9f), p(15.5f, 12f), style = Stroke(width = stroke.width))
    drawCircle(c, s(1.9f), p(7.5f, 17f), style = Stroke(width = stroke.width))
}

/** AI 插画：尖头画笔（paintbrush.pointed）。 */
@Composable
fun BrushIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke()
    val path = Path().apply {
        moveTo(p(19.2f, 4.8f).x, p(19.2f, 4.8f).y)
        lineTo(p(20.2f, 10.4f).x, p(20.2f, 10.4f).y)
        lineTo(p(13.6f, 17f).x, p(13.6f, 17f).y)
        lineTo(p(9.2f, 19.4f).x, p(9.2f, 19.4f).y)
        lineTo(p(11.6f, 15f).x, p(11.6f, 15f).y)
        lineTo(p(18.2f, 8.4f).x, p(18.2f, 8.4f).y)
        close()
    }
    drawPath(path, c, style = stroke)
    drawLine(c, p(11.6f, 15f), p(9.2f, 19.4f), stroke.width, StrokeCap.Round)
}

/** 导入图片：相框 + 右下角下箭头（photo.badge.arrow.down）。 */
@Composable
fun PhotoImportIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke()
    drawRoundRect(
        color = c,
        topLeft = p(3.5f, 4.5f),
        size = Size(s(14.5f), s(14.5f)),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(3f)),
        style = stroke
    )
    drawCircle(c, s(1.4f), p(8.2f, 9.2f), style = Stroke(width = stroke.width))
    val mountain = Path().apply {
        moveTo(p(4.5f, 16.2f).x, p(4.5f, 16.2f).y)
        lineTo(p(8.6f, 12.2f).x, p(8.6f, 12.2f).y)
        lineTo(p(11.6f, 15f).x, p(11.6f, 15f).y)
        lineTo(p(14.4f, 12.4f).x, p(14.4f, 12.4f).y)
        lineTo(p(17.6f, 15.6f).x, p(17.6f, 15.6f).y)
    }
    drawPath(mountain, c, style = stroke)
    drawCircle(c, s(5.4f), p(18.4f, 18.4f), style = Stroke(width = stroke.width))
    drawLine(c, p(18.4f, 15.8f), p(18.4f, 20.2f), stroke.width, StrokeCap.Round)
    drawLine(c, p(16.4f, 18.3f), p(18.4f, 20.3f), stroke.width, StrokeCap.Round)
    drawLine(c, p(20.4f, 18.3f), p(18.4f, 20.3f), stroke.width, StrokeCap.Round)
}

/** 编辑布局：手势点按（hand.draw 简化版）。 */
@Composable
fun HandIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke()
    val palm = Path().apply {
        moveTo(p(7.2f, 12.6f).x, p(7.2f, 12.6f).y)
        lineTo(p(7.2f, 7.4f).x, p(7.2f, 7.4f).y)
        cubicTo(p(7.2f, 6.2f).x, p(7.2f, 6.2f).y, p(8.1f, 5.3f).x, p(8.1f, 5.3f).y, p(9.3f, 5.3f).x, p(9.3f, 5.3f).y)
        cubicTo(p(10.5f, 5.3f).x, p(10.5f, 5.3f).y, p(11.4f, 6.2f).x, p(11.4f, 6.2f).y, p(11.4f, 7.4f).x, p(11.4f, 7.4f).y)
        lineTo(p(11.4f, 11.4f).x, p(11.4f, 11.4f).y)
    }
    drawPath(palm, c, style = stroke)
    val fingers = Path().apply {
        moveTo(p(11.4f, 9.2f).x, p(11.4f, 9.2f).y)
        lineTo(p(11.4f, 6.6f).x, p(11.4f, 6.6f).y)
        cubicTo(p(11.4f, 5.4f).x, p(11.4f, 5.4f).y, p(12.3f, 4.5f).x, p(12.3f, 4.5f).y, p(13.5f, 4.5f).x, p(13.5f, 4.5f).y)
        cubicTo(p(14.7f, 4.5f).x, p(14.7f, 4.5f).y, p(15.6f, 5.4f).x, p(15.6f, 5.4f).y, p(15.6f, 6.6f).x, p(15.6f, 6.6f).y)
        lineTo(p(15.6f, 11f).x, p(15.6f, 11f).y)
        moveTo(p(15.6f, 10.2f).x, p(15.6f, 10.2f).y)
        lineTo(p(15.6f, 8.2f).x, p(15.6f, 8.2f).y)
        cubicTo(p(15.6f, 7f).x, p(15.6f, 7f).y, p(16.5f, 6.1f).x, p(16.5f, 6.1f).y, p(17.7f, 6.1f).x, p(17.7f, 6.1f).y)
        cubicTo(p(18.9f, 6.1f).x, p(18.9f, 6.1f).y, p(19.8f, 7f).x, p(19.8f, 7f).y, p(19.8f, 8.2f).x, p(19.8f, 8.2f).y)
        lineTo(p(19.8f, 13.4f).x, p(19.8f, 13.4f).y)
        cubicTo(p(19.8f, 17.4f).x, p(19.8f, 17.4f).y, p(16.8f, 20f).x, p(16.8f, 20f).y, p(13f, 20f).x, p(13f, 20f).y)
        cubicTo(p(9.6f, 20f).x, p(9.6f, 20f).y, p(7.6f, 18.2f).x, p(7.6f, 18.2f).y, p(6.2f, 15.6f).x, p(6.2f, 15.6f).y)
        lineTo(p(4.6f, 12.9f).x, p(4.6f, 12.9f).y)
        cubicTo(p(4f, 11.9f).x, p(4f, 11.9f).y, p(4.4f, 10.9f).x, p(4.4f, 10.9f).y, p(5.4f, 10.4f).x, p(5.4f, 10.4f).y)
        cubicTo(p(6.3f, 10f).x, p(6.3f, 10f).y, p(6.9f, 10.6f).x, p(6.9f, 10.6f).y, p(7.2f, 11.2f).x, p(7.2f, 11.2f).y)
    }
    drawPath(fingers, c, style = stroke)
}

/** 导出到相册：托盘 + 下箭头（square.and.arrow.down）。 */
@Composable
fun DownloadTrayIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke()
    val tray = Path().apply {
        moveTo(p(4.5f, 14.2f).x, p(4.5f, 14.2f).y)
        lineTo(p(4.5f, 17.8f).x, p(4.5f, 17.8f).y)
        cubicTo(p(4.5f, 19.4f).x, p(4.5f, 19.4f).y, p(5.7f, 20.4f).x, p(5.7f, 20.4f).y, p(7.2f, 20.4f).x, p(7.2f, 20.4f).y)
        lineTo(p(16.8f, 20.4f).x, p(16.8f, 20.4f).y)
        cubicTo(p(18.3f, 20.4f).x, p(18.3f, 20.4f).y, p(19.5f, 19.4f).x, p(19.5f, 19.4f).y, p(19.5f, 17.8f).x, p(19.5f, 17.8f).y)
        lineTo(p(19.5f, 14.2f).x, p(19.5f, 14.2f).y)
    }
    drawPath(tray, c, style = stroke)
    drawLine(c, p(12f, 4f), p(12f, 14.6f), stroke.width, StrokeCap.Round)
    drawLine(c, p(7.8f, 10.6f), p(12f, 14.8f), stroke.width, StrokeCap.Round)
    drawLine(c, p(16.2f, 10.6f), p(12f, 14.8f), stroke.width, StrokeCap.Round)
}

/** 眼睛（显示）。 */
@Composable
fun EyeIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke(1.6.dp)
    val path = Path().apply {
        moveTo(p(3f, 12f).x, p(3f, 12f).y)
        cubicTo(p(6f, 7f).x, p(6f, 7f).y, p(9f, 5.4f).x, p(9f, 5.4f).y, p(12f, 5.4f).x, p(12f, 5.4f).y)
        cubicTo(p(15f, 5.4f).x, p(15f, 5.4f).y, p(18f, 7f).x, p(18f, 7f).y, p(21f, 12f).x, p(21f, 12f).y)
        cubicTo(p(18f, 17f).x, p(18f, 17f).y, p(15f, 18.6f).x, p(15f, 18.6f).y, p(12f, 18.6f).x, p(12f, 18.6f).y)
        cubicTo(p(9f, 18.6f).x, p(9f, 18.6f).y, p(6f, 17f).x, p(6f, 17f).y, p(3f, 12f).x, p(3f, 12f).y)
        close()
    }
    drawPath(path, c, style = stroke)
    drawCircle(c, s(2.9f), p(12f, 12f), style = Stroke(width = stroke.width))
}

/** 眼睛斜线（隐藏）。 */
@Composable
fun EyeOffIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke(1.6.dp)
    val path = Path().apply {
        moveTo(p(3.2f, 12f).x, p(3.2f, 12f).y)
        cubicTo(p(6.2f, 7.2f).x, p(6.2f, 7.2f).y, p(9.1f, 5.6f).x, p(9.1f, 5.6f).y, p(12f, 5.6f).x, p(12f, 5.6f).y)
        cubicTo(p(14.4f, 5.6f).x, p(14.4f, 5.6f).y, p(16.8f, 6.7f).x, p(16.8f, 6.7f).y, p(18.8f, 9.2f).x, p(18.8f, 9.2f).y)
    }
    drawPath(path, c, style = stroke)
    val lower = Path().apply {
        moveTo(p(4.4f, 15.2f).x, p(4.4f, 15.2f).y)
        cubicTo(p(5.6f, 17f).x, p(5.6f, 17f).y, p(8.2f, 18.4f).x, p(8.2f, 18.4f).y, p(12f, 18.4f).x, p(12f, 18.4f).y)
        cubicTo(p(14.2f, 18.4f).x, p(14.2f, 18.4f).y, p(16.2f, 17.7f).x, p(16.2f, 17.7f).y, p(17.6f, 16.6f).x, p(17.6f, 16.6f).y)
    }
    drawPath(lower, c, style = stroke)
    drawLine(c, p(4.5f, 4.5f), p(19.5f, 19.5f), stroke.width, StrokeCap.Round)
}

/** 文字左对齐。 */
@Composable
fun AlignLeftIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke(1.9.dp)
    drawLine(c, p(4f, 6.5f), p(20f, 6.5f), stroke.width, StrokeCap.Round)
    drawLine(c, p(4f, 12f), p(14.5f, 12f), stroke.width, StrokeCap.Round)
    drawLine(c, p(4f, 17.5f), p(18f, 17.5f), stroke.width, StrokeCap.Round)
}

/** 文字居中。 */
@Composable
fun AlignCenterIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke(1.9.dp)
    drawLine(c, p(4f, 6.5f), p(20f, 6.5f), stroke.width, StrokeCap.Round)
    drawLine(c, p(6.8f, 12f), p(17.2f, 12f), stroke.width, StrokeCap.Round)
    drawLine(c, p(4.6f, 17.5f), p(19.4f, 17.5f), stroke.width, StrokeCap.Round)
}

/** 文字右对齐。 */
@Composable
fun AlignRightIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke(1.9.dp)
    drawLine(c, p(4f, 6.5f), p(20f, 6.5f), stroke.width, StrokeCap.Round)
    drawLine(c, p(9.5f, 12f), p(20f, 12f), stroke.width, StrokeCap.Round)
    drawLine(c, p(6f, 17.5f), p(20f, 17.5f), stroke.width, StrokeCap.Round)
}

/** 缩小（minus.magnifyingglass）。 */
@Composable
fun ZoomOutIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke(1.7.dp)
    drawCircle(c, s(6.4f), p(10.5f, 10.5f), style = Stroke(width = stroke.width))
    drawLine(c, p(15.2f, 15.2f), p(20f, 20f), stroke.width, StrokeCap.Round)
    drawLine(c, p(7.4f, 10.5f), p(13.6f, 10.5f), stroke.width, StrokeCap.Round)
}

/** 放大（plus.magnifyingglass）。 */
@Composable
fun ZoomInIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke(1.7.dp)
    drawCircle(c, s(6.4f), p(10.5f, 10.5f), style = Stroke(width = stroke.width))
    drawLine(c, p(15.2f, 15.2f), p(20f, 20f), stroke.width, StrokeCap.Round)
    drawLine(c, p(7.4f, 10.5f), p(13.6f, 10.5f), stroke.width, StrokeCap.Round)
    drawLine(c, p(10.5f, 7.4f), p(10.5f, 13.6f), stroke.width, StrokeCap.Round)
}

/** 恢复/重置（arrow.counterclockwise）。 */
@Composable
fun ResetIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke(1.7.dp)
    val arc = Path().apply {
        moveTo(p(5.2f, 8.6f).x, p(5.2f, 8.6f).y)
        cubicTo(
            p(7f, 4.8f).x, p(7f, 4.8f).y,
            p(12.4f, 3.2f).x, p(12.4f, 3.2f).y,
            p(16.8f, 5.8f).x, p(16.8f, 5.8f).y
        )
        cubicTo(
            p(21.6f, 8.8f).x, p(21.6f, 8.8f).y,
            p(22.3f, 15.2f).x, p(22.3f, 15.2f).y,
            p(18.3f, 18.9f).x, p(18.3f, 18.9f).y
        )
        cubicTo(
            p(14.6f, 22.3f).x, p(14.6f, 22.3f).y,
            p(8.6f, 20.3f).x, p(8.6f, 20.3f).y,
            p(6.1f, 16.4f).x, p(6.1f, 16.4f).y
        )
    }
    drawPath(arc, c, style = stroke)
    drawLine(c, p(4.2f, 4.2f), p(4.2f, 9.4f), stroke.width, StrokeCap.Round)
    drawLine(c, p(4.2f, 4.2f), p(9.4f, 4.2f), stroke.width, StrokeCap.Round)
}

/** 实心对勾圆（checkmark.circle.fill）。 */
@Composable
fun CheckCircleIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    checkColor: Color = Color.White
) = Glyph(modifier, color) { c ->
    drawCircle(c, s(9.2f), p(12f, 12f))
    val stroke = iconStroke(2.1.dp)
    drawLine(checkColor, p(7.4f, 12.2f), p(10.5f, 15.2f), stroke.width, StrokeCap.Round)
    drawLine(checkColor, p(10.5f, 15.2f), p(16.6f, 8.6f), stroke.width, StrokeCap.Round)
}

/** 警告三角（exclamationmark.triangle.fill）。 */
@Composable
fun AlertTriangleIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke(1.7.dp)
    val path = Path().apply {
        moveTo(p(12f, 3.6f).x, p(12f, 3.6f).y)
        lineTo(p(21.6f, 20.2f).x, p(21.6f, 20.2f).y)
        lineTo(p(2.4f, 20.2f).x, p(2.4f, 20.2f).y)
        close()
    }
    drawPath(path, c, style = stroke)
    drawLine(c, p(12f, 9.4f), p(12f, 14.4f), stroke.width, StrokeCap.Round)
    drawCircle(c, s(1.05f), p(12f, 17.2f))
}

/** 取色器 / 自定义颜色（彩虹圆点）。 */
@Composable
fun ColorWheelIcon(modifier: Modifier = Modifier, color: Color = Color.Black) = Glyph(modifier, color) { c ->
    val stroke = iconStroke(1.6.dp)
    drawCircle(c, s(7.8f), p(12f, 12f), style = Stroke(width = stroke.width))
    drawLine(c, p(15.2f, 5.2f), p(18.8f, 1.8f), stroke.width, StrokeCap.Round)
    drawLine(c, p(18.8f, 1.8f), p(22.2f, 5.2f), stroke.width, StrokeCap.Round)
    drawLine(c, p(18.8f, 1.8f), p(18.8f, 8.4f), stroke.width, StrokeCap.Round)
    drawCircle(c, s(1.2f), p(9.6f, 9.6f))
    drawCircle(c, s(1.2f), p(14.4f, 14.4f))
    drawCircle(c, s(1.2f), p(14.4f, 9.6f))
}
