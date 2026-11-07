package com.brewkin.beanbook.ui.card

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.brewkin.beanbook.data.Bean
import com.brewkin.beanbook.data.Recipe
import com.brewkin.beanbook.data.RecipeCardConfig
import kotlin.math.min
import kotlin.random.Random

/**
 * 手绘特调咖啡「随杯卡」渲染器 v3（Android Canvas 本地渲染）。
 *
 * - 奶油纹理纸底 + 极细纸纹颗粒；只放这一杯单品内容；
 * - 版式由 [CardLayoutEngine] 统一计算（默认按测量文本高度流式排版；
 *   手动拖拽/缩放为归一化位置），渲染与编辑器共用同一套布局；
 * - 支持 AI 插画 / 相册抠图透明 PNG / 杯图 / 豆插画 / 相册原图；
 * - 社交 1200×675、1200×1600；A6 300dpi 1748×1240、1240×1748。
 */
object CardRenderer {
    const val WIDTH = 1200
    const val HEIGHT = 675

    /** iOS 同款三模板：卡片本身渲染不变（底色/纸纹），只对齐名称与说明。 */
    val templates = listOf(
        CardTemplate("LIGHT", "杂志", "左图右文，像一本咖啡杂志"),
        CardTemplate("DARK", "海报", "满版封面 + 大字标题"),
        CardTemplate("LIME", "留白", "奶油纸底，安静留白")
    )

    /** iOS 同款 8 个主题色。 */
    val themeOptions = listOf(
        CardTheme("#A3CD39", "青柠"),
        CardTheme("#6B4A33", "咖啡"),
        CardTheme("#26231F", "墨色"),
        CardTheme("#D7BC62", "奶油"),
        CardTheme("#B77A6A", "玫瑰"),
        CardTheme("#6E8F9D", "天空"),
        CardTheme("#7E9C4F", "抹茶"),
        CardTheme("#C08A3E", "南瓜")
    )

    val orientations = listOf(
        Orientation("LANDSCAPE", "A6 横版"),
        Orientation("PORTRAIT", "A6 竖版")
    )

    const val EXPORT_SOCIAL = "SOCIAL"
    const val EXPORT_PRINT_A6 = "PRINT_A6"

    fun exportSizes() = listOf("SOCIAL" to "社交尺寸", "PRINT_A6" to "A6 打印 300dpi")

    fun exportCaption(exportSize: String): String =
        if (exportSize == EXPORT_PRINT_A6) "咖啡店随杯卡可打印" else "导出分享图"

    fun templateLabel(templateId: String): String =
        templates.firstOrNull { it.id == templateId }?.label ?: "杂志"

    fun ratioLabel(orientation: String): String =
        if (orientation == "PORTRAIT") "A6 竖版" else "A6 横版"

    fun socialSize(orientation: String): CardSize =
        if (orientation == "PORTRAIT") CardSize("SOCIAL_PORTRAIT", "社交竖版", 1200, 1600)
        else CardSize("SOCIAL_LANDSCAPE", "社交横版", 1200, 675)

    fun printSize(orientation: String): CardSize =
        if (orientation == "PORTRAIT") CardSize("PRINT_A6_PORTRAIT", "A6 竖版打印", 1240, 1748)
        else CardSize("PRINT_A6_LANDSCAPE", "A6 横版打印", 1748, 1240)

    /** 按方向 + 导出尺寸取真实导出像素尺寸（WYSIWYG）。 */
    fun exportSize(orientation: String, exportSize: String): CardSize =
        if (exportSize == EXPORT_PRINT_A6) printSize(orientation) else socialSize(orientation)

    /** 预览缩略图尺寸：等比缩小到最大边 ~1000px，布局比例与导出完全一致。 */
    fun previewSize(orientation: String, exportSize: String): CardSize {
        val full = exportSize(orientation, exportSize)
        val scale = 1000f / maxOf(full.width, full.height)
        return CardSize(
            "PREVIEW",
            "预览",
            (full.width * scale).toInt().coerceAtLeast(2),
            (full.height * scale).toInt().coerceAtLeast(2)
        )
    }

    fun exportPixelText(orientation: String, exportSize: String): String {
        val size = exportSize(orientation, exportSize)
        return "${size.width}×${size.height} px"
    }

    /** 兼容旧调用：按 config.orientation 选择社交尺寸渲染。 */
    fun render(
        recipe: Recipe,
        config: RecipeCardConfig,
        coverBitmap: Bitmap? = null,
        beanBitmap: Bitmap? = null,
        illustrationBitmap: Bitmap? = null,
        importedBitmap: Bitmap? = null,
        size: CardSize = socialSize(config.orientation),
        bean: Bean? = null,
        priceOverride: Int? = null
    ): Bitmap {
        val layout = CardLayoutEngine.layout(recipe, config, size, bean, priceOverride)
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = size.width.toFloat()
        val h = size.height.toFloat()
        val dark = config.template == "DARK"
        val lime = config.template == "LIME"
        val theme = parseColor(config.themeColorHex) ?: Color.parseColor("#A3CD39")
        val ink = if (dark) Color.parseColor("#F1EDE4") else Color.parseColor("#2A2620")

        drawBackground(canvas, w, h, config.template)

        // 插画（在文字之前绘制；默认排版保证不重叠，手动画布上有碰撞检测）
        val illustration = when (config.coverKind) {
            "ILLUSTRATION" -> illustrationBitmap
            "IMPORTED" -> importedBitmap
            "GALLERY" -> coverBitmap
            "BEAN" -> beanBitmap
            else -> null
        }
        layout.illustration?.let { rect ->
            if (CardElement.ILLUSTRATION !in config.hidden) {
                if (illustration != null) {
                    drawArtwork(canvas, illustration, rect, config.illustrationShadow)
                } else {
                    drawCupArt(canvas, rect, recipe, theme, dark)
                }
            }
        }

        // 细横线
        val hairlineRight = if (!layout.portrait) {
            (layout.illustration?.left ?: (w - layout.mx)) - h * 0.03f
        } else {
            w - layout.mx
        }
        canvas.drawRect(
            RectF(layout.mx, layout.hairlineY, hairlineRight, layout.hairlineY + 1.6f),
            Paint().apply { color = withAlpha(ink, if (dark) 0.24f else 0.18f) }
        )

        // 文本元素
        CardElement.textOnes.forEach { id ->
            val block = layout.texts[id] ?: return@forEach
            val box = layout.boxes[id] ?: return@forEach
            var y = box.top
            block.lines.forEach { line ->
                val paint = CardLayoutEngine.paintFor(line.style, line.fontSize, dark, w / 640f)
                y += line.fontSize * 0.86f
                val x = when (line.align) {
                    "CENTER" -> box.centerX() - paint.measureText(line.text) / 2f
                    "RIGHT" -> box.right - paint.measureText(line.text)
                    else -> box.left
                }
                canvas.drawText(line.text, x, y, paint)
                y += line.fontSize * 0.14f + block.lineGap
            }
        }

        // 竖版关键词下方的极细主题色短线（保持原设计细节）
        if (layout.portrait && CardElement.KEYWORDS !in config.hidden) {
            layout.boxes[CardElement.KEYWORDS]?.let { box ->
                val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(theme, 0.75f) }
                canvas.drawRect(
                    RectF(box.centerX() - w * 0.035f, box.bottom + h * 0.012f,
                        box.centerX() + w * 0.035f, box.bottom + h * 0.014f),
                    accent
                )
            }
        }

        drawPaperGrain(canvas, w, h, dark)
        return bitmap
    }

    private fun drawBackground(canvas: Canvas, w: Float, h: Float, template: String) {
        val dark = template == "DARK"
        val lime = template == "LIME"
        val colors = when {
            dark -> intArrayOf(Color.parseColor("#17150F"), Color.parseColor("#23201A"))
            lime -> intArrayOf(Color.parseColor("#F3F7E4"), Color.parseColor("#DEE99F"))
            else -> intArrayOf(Color.parseColor("#F7F3E8"), Color.parseColor("#F1EBDD"))
        }
        canvas.drawRect(0f, 0f, w, h, Paint().apply {
            shader = LinearGradient(0f, 0f, w, h, colors, null, Shader.TileMode.CLAMP)
        })
        if (!dark) {
            canvas.drawRect(0f, 0f, w, h, Paint().apply {
                shader = RadialGradient(
                    w * 0.5f, h * 0.32f, maxOf(w, h) * 0.72f,
                    intArrayOf(Color.argb(26, 255, 245, 220), Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
            })
        }
    }

    private fun drawArtwork(canvas: Canvas, bitmap: Bitmap, rect: RectF, shadow: Boolean) {
        val scale = min(rect.width() / bitmap.width, rect.height() / bitmap.height)
        val dw = bitmap.width * scale
        val dh = bitmap.height * scale
        val dx = rect.left + (rect.width() - dw) / 2f
        val dy = rect.top + (rect.height() - dh) / 2f
        if (shadow && bitmap.hasAlpha()) {
            val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(26, 60, 45, 25)
                maskFilter = android.graphics.BlurMaskFilter(
                    maxOf(8f, rect.width() * 0.035f),
                    android.graphics.BlurMaskFilter.Blur.NORMAL
                )
            }
            canvas.drawOval(
                RectF(dx + dw * 0.18f, dy + dh * 0.86f, dx + dw * 0.82f, dy + dh * 0.95f),
                shadow
            )
        }
        canvas.drawBitmap(bitmap, null, RectF(dx, dy, dx + dw, dy + dh), Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private fun drawCupArt(canvas: Canvas, rect: RectF, recipe: Recipe, theme: Int, dark: Boolean) {
        val cx = rect.centerX()
        val top = rect.top + rect.height() * 0.10f
        val cupH = rect.height() * 0.70f
        val topW = rect.width() * 0.52f
        val bottomW = rect.width() * 0.40f
        val cup = Path().apply {
            moveTo(cx - topW / 2f, top)
            lineTo(cx + topW / 2f, top)
            lineTo(cx + bottomW / 2f, top + cupH)
            quadTo(cx + bottomW / 2f + 10f, top + cupH + 26f, cx + bottomW / 2f - 12f, top + cupH + 26f)
            lineTo(cx - bottomW / 2f + 12f, top + cupH + 26f)
            quadTo(cx - bottomW / 2f - 10f, top + cupH + 26f, cx - bottomW / 2f, top + cupH)
            close()
        }
        canvas.drawPath(cup, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(52, 255, 255, 255) })
        canvas.save()
        canvas.clipPath(cup)
        val total = recipe.layers.sumOf { it.grams }.coerceAtLeast(1)
        var bottom = top + cupH + 26f
        recipe.layers.forEach { layer ->
            val lh = cupH * (layer.grams.toFloat() / total)
            val color = parseColor(layer.colorHex) ?: Color.parseColor("#C9A875")
            canvas.drawRect(
                cx - topW / 2f - 10f, bottom - lh, cx + topW / 2f + 10f, bottom,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = withAlpha(color, 0.92f) }
            )
            bottom -= lh
        }
        canvas.restore()
        canvas.drawPath(
            cup,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
                color = withAlpha(Color.parseColor("#2A2620"), 0.55f)
                strokeJoin = Paint.Join.ROUND
            }
        )
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(theme, 0.5f) }
        canvas.drawCircle(rect.left + rect.width() * 0.12f, rect.top + rect.height() * 0.10f, rect.width() * 0.02f, dot)
    }

    private fun drawPaperGrain(canvas: Canvas, w: Float, h: Float, dark: Boolean) {
        val random = Random(0xA6C0)
        val light = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(if (dark) 14 else 18, 255, 255, 255)
        }
        val darkDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(if (dark) 10 else 12, 90, 70, 42)
        }
        repeat(520) {
            val x = random.nextFloat() * w
            val y = random.nextFloat() * h
            val r = 0.5f + random.nextFloat() * 1.3f
            if (random.nextBoolean()) canvas.drawCircle(x, y, r, light) else canvas.drawCircle(x, y, r, darkDot)
        }
    }

    private fun parseColor(raw: String?): Int? = runCatching {
        Color.parseColor(raw ?: "#A3CD39")
    }.getOrNull()

    private fun withAlpha(color: Int, alpha: Float): Int = Color.argb(
        (alpha.coerceIn(0f, 1f) * 255).toInt(),
        Color.red(color), Color.green(color), Color.blue(color)
    )

    data class CardTemplate(val id: String, val label: String, val caption: String = "")
    data class CardTheme(val hex: String, val label: String)
    data class Orientation(val id: String, val label: String)
    data class CardSize(val id: String, val label: String, val width: Int, val height: Int) {
        val isPrint: Boolean get() = id.startsWith("PRINT")
        val isPortrait: Boolean get() = height > width
    }
}
