package com.brewkin.beanbook.ui.card

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.brewkin.beanbook.data.Bean
import com.brewkin.beanbook.data.ElementPlacement
import com.brewkin.beanbook.data.Recipe
import com.brewkin.beanbook.data.RecipeCardConfig
import kotlin.math.min

/** 卡片元素 id（与持久化 key 一致）。 */
object CardElement {
    const val CATEGORY = "CATEGORY"
    const val META = "META"
    const val PRICE = "PRICE"
    const val ENGLISH = "ENGLISH"
    const val CHINESE = "CHINESE"
    const val INGREDIENTS = "INGREDIENTS"
    const val FLAVORS = "FLAVORS"
    const val KEYWORDS = "KEYWORDS"
    const val ILLUSTRATION = "ILLUSTRATION"

    val all = listOf(CATEGORY, META, PRICE, ENGLISH, CHINESE, INGREDIENTS, FLAVORS, KEYWORDS, ILLUSTRATION)
    val textOnes = listOf(CATEGORY, META, PRICE, ENGLISH, CHINESE, INGREDIENTS, FLAVORS, KEYWORDS)

    fun label(id: String): String = when (id) {
        CATEGORY -> "分类/产地"
        META -> "烘焙度/处理法"
        PRICE -> "价格"
        ENGLISH -> "英文名"
        CHINESE -> "中文名"
        INGREDIENTS -> "配料行"
        FLAVORS -> "风味参考"
        KEYWORDS -> "底部关键词"
        else -> "插画/图片"
    }
}

/** 文字样式（颜色/字体/字号在绘制时按这里解析）。 */
enum class CardTextStyle {
    CATEGORY, META, PRICE, ENGLISH, CHINESE,
    INGREDIENTS_CN, INGREDIENTS_EN, FLAVOR_LABEL, FLAVOR_VALUES, FLAVOR_EN, KEYWORDS
}

data class CardTextLine(
    val text: String,
    val style: CardTextStyle,
    val fontSize: Float,
    val align: String
)

data class CardTextBlock(
    val lines: List<CardTextLine>,
    val lineGap: Float
) {
    fun height(): Float {
        if (lines.isEmpty()) return 0f
        var h = 0f
        lines.forEachIndexed { index, line ->
            h += line.fontSize
            if (index < lines.lastIndex) h += lineGap
        }
        return h
    }
}

data class CardLayoutResult(
    val width: Int,
    val height: Int,
    val portrait: Boolean,
    val boxes: Map<String, RectF>,
    val texts: Map<String, CardTextBlock>,
    val illustration: RectF?,
    val hairlineY: Float,
    val mx: Float,
    val overlappingIds: Set<String>,
    val outOfBoundsIds: Set<String>
) {
    val hasCollision: Boolean get() = overlappingIds.isNotEmpty() || outOfBoundsIds.isNotEmpty()
    val visibleIds: List<String> get() = boxes.keys.toList()
}

/**
 * 卡片排版引擎（默认自动排版 + 手动归一化定位 + 碰撞检测）。
 *
 * - 默认排版按「测量后的真实文本高度」流式排布，画布/方向/文案变化会自动重排；
 * - 手动排版存归一化坐标（0..1），同一份数据同时适配社交/A6 打印尺寸；
 * - 每元素独立字号（pt，8–72；横/竖版分别保存）优先于 scale；
 * - 所有元素框用于渲染、点击选中与碰撞检测，文字/插画都限制在卡面内。
 */
object CardLayoutEngine {
    data class Resolved(val id: String, val text: String)

    fun orientationName(portrait: Boolean): String = if (portrait) "PORTRAIT" else "LANDSCAPE"

    fun resolveTexts(
        recipe: Recipe,
        config: RecipeCardConfig,
        bean: Bean?,
        price: Int?
    ): Map<String, CardTextLine> {
        val copy = config.copy
        // 配料：新拆分字段 → 旧合并字段 → 自动推导
        val legacyIngredients = copy.ingredients.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        val ingredientsCn = copy.ingredientsChinese.ifBlank {
            legacyIngredients.firstOrNull().orEmpty()
        }.ifBlank { autoIngredientsChinese(recipe) }
        val ingredientsEn = copy.ingredientsEnglish.ifBlank {
            legacyIngredients.drop(1).joinToString(" ").ifBlank { "" }
        }.ifBlank { autoIngredientsEnglish(recipe) }

        // 风味参考：新拆分字段 → 旧合并字段 → 自动推导
        val legacyFlavors = copy.flavors.trim()
        val flavorsCn = copy.flavorsChinese.ifBlank { legacyFlavors }.ifBlank {
            autoFlavorKeywords(recipe, bean).joinToString("、")
        }
        val flavorsEn = copy.flavorsEnglish.ifBlank { autoFlavorEnglish(recipe, bean) }

        return mapOf(
            CardElement.CATEGORY to CardTextLine(
                copy.category.ifBlank { autoCategory(recipe) }.trim(),
                CardTextStyle.CATEGORY, 0f, "LEFT"
            ),
            CardElement.META to CardTextLine(
                copy.meta.ifBlank { autoMeta(bean) }.trim(),
                CardTextStyle.META, 0f, "RIGHT"
            ),
            CardElement.PRICE to CardTextLine(
                copy.price.ifBlank { price?.takeIf { it > 0 }?.let { "¥$it" } ?: "" }.trim(),
                CardTextStyle.PRICE, 0f, "RIGHT"
            ),
            CardElement.ENGLISH to CardTextLine(
                copy.englishName.ifBlank { autoEnglish(recipe, bean) }.trim(),
                CardTextStyle.ENGLISH, 0f, "LEFT"
            ),
            CardElement.CHINESE to CardTextLine(
                copy.chineseName.ifBlank { config.title.ifBlank { recipe.name } }.trim(),
                CardTextStyle.CHINESE, 0f, "LEFT"
            ),
            CardElement.INGREDIENTS to CardTextLine(
                "$ingredientsCn\n$ingredientsEn".trim(),
                CardTextStyle.INGREDIENTS_CN, 0f, "LEFT"
            ),
            CardElement.FLAVORS to CardTextLine(
                listOf(flavorsCn, flavorsEn).filter { it.isNotBlank() }.joinToString("\n"),
                CardTextStyle.FLAVOR_VALUES, 0f, "LEFT"
            ),
            CardElement.KEYWORDS to CardTextLine(
                copy.keywords.ifBlank {
                    autoFlavorKeywords(recipe, bean).take(3).joinToString("　")
                }.trim(),
                CardTextStyle.KEYWORDS, 0f, "CENTER"
            )
        )
    }

    /**
     * 每元素默认字号（设计点 pt）。与 iOS `promoDefaultFontSize` 完全同比例：
     * 设计高度 = 640 / 宽高比；u = 卡宽 / 640，渲染 px = pt * u。
     */
    fun defaultFontPt(id: String, width: Float, height: Float): Float {
        if (width <= 0f) return 0f
        val designHeight = 640f * height / width
        return when (id) {
            CardElement.CATEGORY -> designHeight * 0.0145f
            CardElement.META -> designHeight * 0.012f
            CardElement.PRICE -> designHeight * 0.038f
            CardElement.ENGLISH -> min(designHeight * 0.076f, 64f)
            CardElement.CHINESE -> designHeight * 0.030f
            CardElement.INGREDIENTS -> designHeight * 0.0148f
            CardElement.FLAVORS -> designHeight * 0.0138f
            CardElement.KEYWORDS -> designHeight * 0.0132f
            else -> 0f
        }
    }

    /** 当前生效的独立字号（pt）；未设置时返回默认设计字号。 */
    fun effectiveFontPt(
        id: String,
        config: RecipeCardConfig,
        width: Float,
        height: Float
    ): Float {
        if (id == CardElement.ILLUSTRATION) return 0f
        val portrait = height > width
        val orientation = orientationName(portrait)
        config.fontSizes.forOrientation(orientation)[id]?.takeIf { it > 0f }?.let { return it.coerceIn(8f, 72f) }
        val manual = (if (portrait) config.portraitLayout else config.landscapeLayout)[id]
        manual?.fontSizePt?.takeIf { it > 0f }?.let { return it.coerceIn(8f, 72f) }
        return defaultFontPt(id, width, height).coerceIn(8f, 72f)
    }

    fun layout(
        recipe: Recipe,
        config: RecipeCardConfig,
        size: CardRenderer.CardSize,
        bean: Bean?,
        priceOverride: Int? = null
    ): CardLayoutResult {
        val w = size.width.toFloat()
        val h = size.height.toFloat()
        val portrait = h > w
        val price = priceOverride ?: recipe.price
        val resolved = resolveTexts(recipe, config, bean, price)
        val dark = config.template == "DARK"
        val hidden = config.hidden.toSet()
        val manual = if (portrait) config.portraitLayout else config.landscapeLayout
        val customPts = config.fontSizes.forOrientation(orientationName(portrait))
        val mx = if (portrait) w * 0.078f else w * 0.045f
        val my = if (portrait) h * 0.048f else h * 0.075f

        fun scaleOf(id: String): Float = manual[id]?.scale ?: 1f
        /** 设计单位：iOS 以 640pt 宽卡面为基准，u = 卡宽 / 640。 */
        val u = w / 640f
        fun ptToPx(pt: Float): Float = pt * u
        fun defaultPx(id: String): Float = defaultFontPt(id, w, h) * u
        fun customFontPx(id: String): Float? {
            customPts[id]?.takeIf { it > 0f }?.let { return ptToPx(it.coerceIn(8f, 72f)) }
            manual[id]?.fontSizePt?.takeIf { it > 0f }?.let { return ptToPx(it.coerceIn(8f, 72f)) }
            return null
        }
        fun alignOf(id: String, fallback: String): String =
            manual[id]?.align?.takeIf { it != "AUTO" } ?: fallback

        val boxes = mutableMapOf<String, RectF>()
        val texts = mutableMapOf<String, CardTextBlock>()
        var computedIll: RectF? = null

        // 预留插画区域（横版右侧 / 竖版中部）
        val illustrationArea: RectF
        if (portrait) {
            val side = min(w * 0.52f, h * 0.36f)
            val keywordsHeight = h * 0.045f
            val areaTop = h * 0.50f
            val areaBottom = h - h * 0.115f - keywordsHeight
            val drawSide = min(side, (areaBottom - areaTop).coerceAtLeast(h * 0.12f))
            illustrationArea = RectF((w - drawSide) / 2f, areaTop, (w + drawSide) / 2f, areaTop + drawSide)
        } else {
            val side = h * 0.72f
            val top = my + h * 0.13f
            illustrationArea = RectF(w - mx - side, top, w - mx, top + side)
        }

        fun makeBlock(id: String, defaultAlign: String, maxWidth: Float): CardTextBlock? {
            if (id in hidden) return null
            val base = resolved[id] ?: return null
            if (base.text.isBlank()) return null
            val fontScale = scaleOf(id)
            val sizeOverride = customFontPx(id)
            val align = alignOf(id, defaultAlign)
            val lines = mutableListOf<CardTextLine>()
            when (id) {
                CardElement.META -> {
                    val parts = base.text.split("\n", ",", "，").filter { it.isNotBlank() }
                    val fs = sizeOverride ?: (defaultPx(CardElement.META) * fontScale)
                    parts.take(2).forEach { lines.add(CardTextLine(it, CardTextStyle.META, fs, align)) }
                }
                CardElement.INGREDIENTS -> {
                    val parts = base.text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                    val fs = sizeOverride ?: (defaultPx(CardElement.INGREDIENTS) * fontScale)
                    wrapLines(parts.firstOrNull().orEmpty(), fs, maxWidth, CardTextStyle.INGREDIENTS_CN, u).forEach {
                        lines.add(CardTextLine(it, CardTextStyle.INGREDIENTS_CN, fs, align))
                    }
                    val en = parts.drop(1).joinToString(" ").ifBlank { "" }
                    if (en.isNotBlank()) {
                        wrapLines(en, fs * 0.9f, maxWidth, CardTextStyle.INGREDIENTS_EN, u).forEach {
                            lines.add(CardTextLine(it, CardTextStyle.INGREDIENTS_EN, fs * 0.9f, align))
                        }
                    }
                }
                CardElement.FLAVORS -> {
                    val fs = sizeOverride ?: (defaultPx(CardElement.FLAVORS) * fontScale)
                    val parts = base.text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                    lines.add(CardTextLine("风味参考：", CardTextStyle.FLAVOR_LABEL, fs * 0.92f, align))
                    wrapLines(parts.firstOrNull().orEmpty(), fs, maxWidth, CardTextStyle.FLAVOR_VALUES, u).take(2).forEach {
                        lines.add(CardTextLine(it, CardTextStyle.FLAVOR_VALUES, fs, align))
                    }
                    val en = parts.drop(1).joinToString(" ").ifBlank { "" }
                    if (en.isNotBlank()) {
                        wrapLines(en, fs * 0.9f, maxWidth, CardTextStyle.FLAVOR_EN, u).take(2).forEach {
                            lines.add(CardTextLine(it, CardTextStyle.FLAVOR_EN, fs * 0.9f, align))
                        }
                    }
                }
                CardElement.KEYWORDS -> {
                    val fs = sizeOverride ?: (defaultPx(CardElement.KEYWORDS) * fontScale)
                    wrapLines(base.text, fs, maxWidth, CardTextStyle.KEYWORDS, u).take(2).forEach {
                        lines.add(CardTextLine(it, CardTextStyle.KEYWORDS, fs, align))
                    }
                }
                CardElement.ENGLISH, CardElement.CHINESE -> {
                    val fs = sizeOverride ?: (defaultPx(id) * fontScale)
                    // 与 iOS 参考一致：英文名/中文名最多两行；用户自定义字号时允许 3 行以完整显示。
                    val maxLines = if (sizeOverride != null) 3 else 2
                    val style = if (id == CardElement.ENGLISH) CardTextStyle.ENGLISH else CardTextStyle.CHINESE
                    var size = fs
                    var wrapped = wrapLines(base.text, size, maxWidth, style, u)
                    if (sizeOverride == null) {
                        while (wrapped.size > maxLines && size > fs * 0.45f) {
                            size *= 0.92f
                            wrapped = wrapLines(base.text, size, maxWidth, style, u)
                        }
                    }
                    wrapped.take(maxLines).forEach { lines.add(CardTextLine(it, style, size, align)) }
                }
                else -> {
                    val fs = sizeOverride ?: (defaultPx(id) * fontScale)
                    val style = when (id) {
                        CardElement.CATEGORY -> CardTextStyle.CATEGORY
                        CardElement.PRICE -> CardTextStyle.PRICE
                        else -> CardTextStyle.CATEGORY
                    }
                    wrapLines(base.text, fs, maxWidth).take(1).forEach {
                        lines.add(CardTextLine(it, style, fs, align))
                    }
                }
            }
            if (lines.isEmpty()) return null
            val primaryFs = lines.first().fontSize
            // 与 iOS 各元素 VStack spacing 对齐（单位 u）；英文名 lineSpacing -1u。
            val gap = when (id) {
                CardElement.META -> 2f * u
                CardElement.INGREDIENTS -> 2.5f * u
                CardElement.FLAVORS -> 3f * u
                CardElement.ENGLISH -> -1f * u
                else -> primaryFs * 0.22f
            }
            return CardTextBlock(lines, lineGap = gap)
        }

        // —— 自动排版 ——
        if (portrait) {
            val categoryBlock = makeBlock(CardElement.CATEGORY, "LEFT", w * 0.50f)
            categoryBlock?.let {
                boxes[CardElement.CATEGORY] = RectF(mx, my, mx + w * 0.50f, my + it.height())
                texts[CardElement.CATEGORY] = it
            }
            val metaBlock = makeBlock(CardElement.META, "RIGHT", w * 0.32f)
            var rightBottom = my
            metaBlock?.let {
                val width = w * 0.32f
                boxes[CardElement.META] = RectF(w - mx - width, my, w - mx, my + it.height())
                texts[CardElement.META] = it
                rightBottom = my + it.height()
            }
            val priceBlock = makeBlock(CardElement.PRICE, "RIGHT", w * 0.28f)
            priceBlock?.let {
                val width = w * 0.28f
                val top = rightBottom + h * 0.014f
                boxes[CardElement.PRICE] = RectF(w - mx - width, top, w - mx, top + it.height())
                texts[CardElement.PRICE] = it
                rightBottom = top + it.height()
            }
            val contentWidth = w - mx * 2
            val headerBottom = maxOf(
                boxes[CardElement.CATEGORY]?.bottom ?: my,
                rightBottom
            )
            val hairlineY = headerBottom + h * 0.022f
            var cursor = hairlineY + h * 0.075f
            fun placeFlow(id: String, defaultAlign: String) {
                val block = makeBlock(id, defaultAlign, contentWidth) ?: return
                if (id == CardElement.KEYWORDS) return
                if (id == CardElement.ENGLISH || id == CardElement.CHINESE ||
                    id == CardElement.INGREDIENTS || id == CardElement.FLAVORS
                ) {
                    boxes[id] = RectF(mx, cursor, mx + contentWidth, cursor + block.height())
                    texts[id] = block
                    cursor = cursor + block.height() + h * if (id == CardElement.FLAVORS) 0.012f else 0.030f
                }
            }
            placeFlow(CardElement.ENGLISH, "LEFT")
            placeFlow(CardElement.CHINESE, "LEFT")
            placeFlow(CardElement.INGREDIENTS, "LEFT")
            placeFlow(CardElement.FLAVORS, "LEFT")

            // 插画：放文字流下方、关键词上方，绝不重叠
            val keywordsBlock = makeBlock(CardElement.KEYWORDS, "CENTER", contentWidth)
            val keywordsHeight = keywordsBlock?.height() ?: 0f
            val keywordsTop = h - h * 0.055f - keywordsHeight
            keywordsBlock?.let {
                boxes[CardElement.KEYWORDS] = RectF(mx, keywordsTop, w - mx, keywordsTop + keywordsHeight)
                texts[CardElement.KEYWORDS] = it
            }
            val textBottom = boxes
                .filterKeys { it in CardElement.textOnes && it != CardElement.KEYWORDS }
                .values
                .maxOfOrNull { it.bottom } ?: (illustrationArea.top - h * 0.02f)
            val desiredTop = maxOf(illustrationArea.top, textBottom + h * 0.025f)
            val maxBottom = keywordsTop - h * 0.02f
            val maxSide = (maxBottom - desiredTop).coerceAtLeast(h * 0.10f)
            val side = minOf(illustrationArea.width(), maxSide)
            var illRect = RectF(
                (w - side) / 2f, desiredTop,
                (w + side) / 2f, desiredTop + side
            )
            manual[CardElement.ILLUSTRATION]?.let { m ->
                val manualSide = illRect.width() * m.scale
                illRect = RectF(m.x * w, m.y * h, m.x * w + manualSide, m.y * h + manualSide)
            }
            computedIll = illRect
        } else {
            // 先排关键词（底部居中），插画区域必须收在其上方，避免手动布局/导出前误报重叠。
            val keywordsBlock = makeBlock(CardElement.KEYWORDS, "CENTER", illustrationArea.width())
            val keywordsHeight = keywordsBlock?.height() ?: 0f
            val keywordsTop = h - my - keywordsHeight
            val maxIllSide = (keywordsTop - h * 0.02f - illustrationArea.top).coerceAtLeast(h * 0.12f)
            val illSide = minOf(illustrationArea.width(), maxIllSide)
            var landscapeIllRect = RectF(
                illustrationArea.centerX() - illSide / 2f,
                illustrationArea.top,
                illustrationArea.centerX() + illSide / 2f,
                illustrationArea.top + illSide
            )
            val textW = w - mx * 2 - illSide - h * 0.05f
            val categoryBlock = makeBlock(CardElement.CATEGORY, "LEFT", textW * 0.8f)
            var headerBottom = my
            categoryBlock?.let {
                boxes[CardElement.CATEGORY] = RectF(mx, my, mx + textW, my + it.height())
                texts[CardElement.CATEGORY] = it
                headerBottom = my + it.height()
            }
            val priceBlock = makeBlock(CardElement.PRICE, "RIGHT", w * 0.3f)
            priceBlock?.let {
                val width = w * 0.3f
                boxes[CardElement.PRICE] = RectF(w - mx - width, my, w - mx, my + it.height())
                texts[CardElement.PRICE] = it
                headerBottom = maxOf(headerBottom, my + it.height())
            }
            val metaBlock = makeBlock(CardElement.META, "LEFT", textW * 0.9f)
            metaBlock?.let {
                val top = h - my - it.height()
                boxes[CardElement.META] = RectF(mx, top, mx + textW, top + it.height())
                texts[CardElement.META] = it
            }
            val hairlineY = headerBottom + h * 0.035f
            var cursor = hairlineY + h * 0.085f
            listOf(
                CardElement.ENGLISH to 0.030f,
                CardElement.CHINESE to 0.030f,
                CardElement.INGREDIENTS to 0.028f,
                CardElement.FLAVORS to 0.014f
            ).forEach { (id, gap) ->
                val block = makeBlock(id, "LEFT", textW)
                if (block != null) {
                    boxes[id] = RectF(mx, cursor, mx + textW, cursor + block.height())
                    texts[id] = block
                    cursor += block.height() + h * gap
                }
            }
            keywordsBlock?.let {
                boxes[CardElement.KEYWORDS] =
                    RectF(illustrationArea.centerX() - illSide / 2f, keywordsTop,
                        illustrationArea.centerX() + illSide / 2f, keywordsTop + it.height())
                texts[CardElement.KEYWORDS] = it
            }
            var illRect = landscapeIllRect
            manual[CardElement.ILLUSTRATION]?.let { m ->
                val manualSide = illSide * m.scale
                illRect = RectF(m.x * w, m.y * h, m.x * w + manualSide, m.y * h + manualSide)
            }
            computedIll = illRect
        }

        // —— 手动排版覆盖（文字） ——
        val autoBoxes = boxes.toMap()
        val autoTexts = texts.toMap()
        CardElement.textOnes.forEach { id ->
            val m: ElementPlacement = manual[id] ?: return@forEach
            if (id in hidden) return@forEach
            val auto = autoBoxes[id] ?: return@forEach
            val autoBlock = autoTexts[id] ?: return@forEach
            // 手动位置 x/y 为左上角归一化；宽度优先用首次冻结时记录的真实宽度（保证换行稳定、
            // 且未编辑的元素不会因碰撞框变宽而误报重叠），旧数据缺失时退回右侧边距反推。
            val contentWidth = m.width.takeIf { it > 0.01f }?.let { it * w }
                ?: (w - mx - m.x * w).coerceAtLeast(w * 0.18f)
            // 字号已在自动阶段按 scale / 独立字号计算，这里不再二次相乘（避免双倍缩放）。
            val fs = autoBlock.lines.firstOrNull()?.fontSize ?: return@forEach
            val align = m.align.takeIf { it != "AUTO" } ?: autoBlock.lines.first().align
            val defaultStyle = autoBlock.lines.first().style
            val wrapped = when (id) {
                CardElement.META -> autoBlock.lines.size.coerceAtMost(2)
                    .let { count -> (1..count).map { i -> autoBlock.lines.getOrNull(i - 1)?.text ?: "" } }
                CardElement.INGREDIENTS -> listOf(autoBlock.lines.first().text)
                CardElement.FLAVORS -> listOf(autoBlock.lines.drop(1).firstOrNull()?.text.orEmpty())
                CardElement.KEYWORDS, CardElement.ENGLISH, CardElement.CHINESE ->
                    wrapLines(autoBlock.lines.joinToString(" ") { it.text }, fs, contentWidth, defaultStyle, u).take(2)
                else -> wrapLines(autoBlock.lines.first().text, fs, contentWidth, defaultStyle, u).take(1)
            }.filter { it.isNotBlank() }
            if (wrapped.isEmpty()) return@forEach
            val lines = mutableListOf<CardTextLine>()
            when (id) {
                CardElement.INGREDIENTS -> {
                    lines.add(CardTextLine(wrapped.first(), CardTextStyle.INGREDIENTS_CN, fs, align))
                    val en = autoBlock.lines.drop(1).joinToString(" ") { it.text }
                    if (en.isNotBlank()) {
                        wrapLines(en, fs * 0.9f, contentWidth, CardTextStyle.INGREDIENTS_EN, u).take(2).forEach {
                            lines.add(CardTextLine(it, CardTextStyle.INGREDIENTS_EN, fs * 0.9f, align))
                        }
                    }
                }
                CardElement.FLAVORS -> {
                    lines.add(CardTextLine("风味参考：", CardTextStyle.FLAVOR_LABEL, fs * 0.92f, align))
                    wrapLines(wrapped.first(), fs, contentWidth, CardTextStyle.FLAVOR_VALUES, u).take(2).forEach {
                        lines.add(CardTextLine(it, CardTextStyle.FLAVOR_VALUES, fs, align))
                    }
                    val en = autoBlock.lines.lastOrNull()?.takeIf { it.style == CardTextStyle.FLAVOR_EN }?.text
                        ?: autoBlock.lines.drop(2).joinToString(" ")
                    if (en.isNotBlank()) {
                        wrapLines(en, fs * 0.9f, contentWidth, CardTextStyle.FLAVOR_EN, u).take(2).forEach {
                            lines.add(CardTextLine(it, CardTextStyle.FLAVOR_EN, fs * 0.9f, align))
                        }
                    }
                }
                else -> wrapped.forEach { lines.add(CardTextLine(it, defaultStyle, fs, align)) }
            }
            val block = CardTextBlock(lines, lineGap = fs * 0.22f)
            val left = (m.x * w).coerceIn(0f, w - w * 0.05f)
            val top = (m.y * h).coerceIn(0f, h - h * 0.03f)
            val width = contentWidth.coerceAtMost(w - left)
            boxes[id] = RectF(left, top, left + width, top + block.height())
            texts[id] = block
        }

        // 插画框（使用分支里按文字流计算出的最终位置）
        val illBox = computedIll ?: illustrationArea
        if (CardElement.ILLUSTRATION !in hidden) boxes[CardElement.ILLUSTRATION] = illBox

        // —— 越界 & 碰撞检测 ——
        val outOfBounds = mutableSetOf<String>()
        boxes.forEach { (id, rect) ->
            if (rect.left < -1f || rect.top < -1f || rect.right > w + 1f || rect.bottom > h + 1f) {
                outOfBounds.add(id)
            }
        }
        val overlapping = mutableSetOf<String>()
        val ids = boxes.keys.toList()
        for (i in ids.indices) {
            for (j in i + 1 until ids.size) {
                val a = boxes[ids[i]] ?: continue
                val b = boxes[ids[j]] ?: continue
                val pad = 1.5f
                if (a.left + pad < b.right - pad && b.left + pad < a.right - pad &&
                    a.top + pad < b.bottom - pad && b.top + pad < a.bottom - pad
                ) {
                    overlapping.add(ids[i])
                    overlapping.add(ids[j])
                }
            }
        }
        val hairline = if (portrait) {
            maxOf(
                boxes[CardElement.CATEGORY]?.bottom ?: my,
                boxes[CardElement.META]?.bottom ?: my,
                boxes[CardElement.PRICE]?.bottom ?: my
            ) + h * 0.022f
        } else {
            maxOf(
                boxes[CardElement.CATEGORY]?.bottom ?: my,
                boxes[CardElement.PRICE]?.bottom ?: my
            ) + h * 0.035f
        }
        return CardLayoutResult(
            width = size.width,
            height = size.height,
            portrait = portrait,
            boxes = boxes,
            texts = texts,
            illustration = if (CardElement.ILLUSTRATION in hidden) null else illBox,
            hairlineY = hairline,
            mx = mx,
            overlappingIds = overlapping,
            outOfBoundsIds = outOfBounds
        )
    }

    // —— 自动文案（与 iOS PromoCard.swift 的推导规则对齐） ——

    fun autoCategory(recipe: Recipe): String {
        val ids = recipe.layers.map { it.ingredientId }.toSet()
        return when {
            "matcha" in ids -> "抹茶拿铁"
            "coldbrew" in ids -> "冷萃咖啡"
            "soda" in ids || "sparkling" in ids -> "气泡特调"
            "espresso" in ids && ("milk" in ids || "oat" in ids || "coconut" in ids || "soy" in ids) ->
                if (ids.any { it in setOf("foam", "cheesefoam", "cream") }) "奶咖特调" else "意式拿铁"
            "espresso" in ids -> "意式浓缩"
            ids.any { it in setOf("water", "blacktea", "oolong") } -> "手冲咖啡"
            else -> "特调咖啡"
        }
    }

    fun autoCategoryEnglish(recipe: Recipe): String = when (autoCategory(recipe)) {
        "抹茶拿铁" -> "Matcha Latte"
        "冷萃咖啡" -> "Cold Brew"
        "气泡特调" -> "Sparkling Special"
        "意式拿铁" -> "Latte"
        "奶咖特调" -> "Signature Latte"
        "意式浓缩" -> "Espresso"
        "手冲咖啡" -> "Pour Over"
        else -> "Signature Drink"
    }

    fun processEnglish(process: String): String {
        val clean = process.trim()
        return when {
            clean.contains("水洗") -> "WASHED"
            clean.contains("日晒") || clean.contains("自然") -> "NATURAL"
            clean.contains("蜜") -> "HONEY"
            clean.contains("厌氧") -> "ANAEROBIC"
            clean.contains("湿刨") -> "WET HULLED"
            clean.contains("桶") -> "BARREL"
            clean.isBlank() -> "PROCESS"
            else -> clean.uppercase()
        }
    }

    fun autoMeta(bean: Bean?): String {
        if (bean == null) return ""
        val process = bean.process.trim()
        return if (process.isBlank()) {
            bean.roast.label
        } else {
            "${bean.roast.label}\n△$process ${processEnglish(process)}"
        }
    }

    private val FLAVOR_ENGLISH_MAP = mapOf(
        "茉莉" to "Jasmine", "柠檬" to "Lemon", "红茶感" to "Black Tea", "荔枝" to "Lychee",
        "白葡萄" to "White Grape", "焦糖" to "Caramel", "巧克力" to "Chocolate", "坚果" to "Nutty",
        "玫瑰" to "Rose", "桂花" to "Osmanthus", "蜂蜜" to "Honey", "杏子" to "Apricot",
        "西柚" to "Grapefruit", "柑橘" to "Citrus", "莓果" to "Berries", "香草" to "Vanilla",
        "抹茶" to "Matcha", "奶油" to "Cream", "黑巧" to "Dark Chocolate", "可可" to "Cocoa",
        "莓" to "Berry", "桃" to "Peach", "葡萄" to "Grape", "苹果" to "Apple", "乌龙" to "Oolong",
        "清爽" to "Refreshing", "均衡" to "Balanced", "浓郁" to "Rich",
        "微甜" to "Lightly Sweet", "甜感适中" to "Medium Sweet", "明显偏甜" to "Sweet"
    )

    private val SWEET_WORDS = setOf("Refreshing", "Balanced", "Rich", "Lightly Sweet", "Medium Sweet", "Sweet")

    fun autoFlavorKeywords(recipe: Recipe, bean: Bean?): List<String> {
        val keywords = mutableListOf<String>()
        bean?.flavors?.filter { it.isNotBlank() }?.take(2)?.let { keywords.addAll(it) }
        if (recipe.mouthfeel.isNotBlank()) keywords.add(recipe.mouthfeel)
        if (recipe.sweetness.isNotBlank()) keywords.add(recipe.sweetness)
        return keywords.distinct().take(4)
    }

    fun autoFlavorEnglish(recipe: Recipe, bean: Bean?): String {
        val keywords = autoFlavorKeywords(recipe, bean)
        return keywords.mapNotNull { keyword ->
            FLAVOR_ENGLISH_MAP[keyword]
                ?: FLAVOR_ENGLISH_MAP.entries.firstOrNull { keyword.contains(it.key) }?.value
        }.joinToString(", ")
    }

    fun autoEnglish(recipe: Recipe, bean: Bean?): String {
        val flavorEnglish = autoFlavorKeywords(recipe, bean).mapNotNull { keyword ->
            val mapped = FLAVOR_ENGLISH_MAP[keyword]
                ?: FLAVOR_ENGLISH_MAP.entries.firstOrNull { keyword.contains(it.key) }?.value
            mapped?.takeIf { it !in SWEET_WORDS }
        }.take(2)
        val prefix = flavorEnglish.joinToString(" ")
        val category = autoCategoryEnglish(recipe)
        return if (prefix.isBlank()) category else "$prefix $category"
    }

    fun autoIngredientsChinese(recipe: Recipe): String {
        val names = recipe.layers.map { it.name }.filter { it.isNotBlank() }
        return if (names.isEmpty()) "咖啡" else names.joinToString(" + ")
    }

    private val INGREDIENT_ENGLISH_MAP = mapOf(
        "espresso" to "Espresso", "coldbrew" to "Cold Brew", "water" to "Hot Water", "matcha" to "Matcha",
        "blacktea" to "Black Tea", "oolong" to "Oolong Tea", "milk" to "Milk", "oat" to "Oat Milk",
        "coconut" to "Coconut Milk", "soy" to "Soy Milk", "foam" to "Milk Foam", "cheesefoam" to "Cheese Foam",
        "cream" to "Cream", "caramel" to "Caramel Syrup", "vanilla" to "Vanilla Syrup", "choc" to "Chocolate",
        "osmanthus" to "Osmanthus Syrup", "condensed" to "Condensed Milk", "brownsugar" to "Brown Sugar",
        "hazelnut" to "Hazelnut Syrup", "honey" to "Honey", "passion" to "Passion Fruit", "lemon" to "Lemon",
        "mango" to "Mango", "strawberry" to "Strawberry", "ice" to "Ice", "soda" to "Sparkling Water",
        "sparkling" to "Sparkling Water"
    )

    fun autoIngredientsEnglish(recipe: Recipe): String {
        val names = recipe.layers.mapNotNull { layer ->
            INGREDIENT_ENGLISH_MAP[layer.ingredientId] ?: layer.name.takeIf { it.isNotBlank() }
        }
        return if (names.isEmpty()) "Coffee" else names.joinToString(" + ")
    }

    /** 兼容旧调用：自动风味关键词列表。 */
    fun autoFlavors(recipe: Recipe, config: RecipeCardConfig, bean: Bean?): List<String> =
        autoFlavorKeywords(recipe, bean)

    /** 兼容旧调用：自动英文名（不区分豆子风味）。 */
    fun autoEnglish(recipe: Recipe): String = autoEnglish(recipe, null)

    fun layerEnglish(id: String): String? = INGREDIENT_ENGLISH_MAP[id]

    // —— 测量 ——
    /** iOS 卡面字体映射：价格/英文名/中文名 serif，其余系统无衬线（medium/regular）。 */
    private fun typefaceFor(style: CardTextStyle): Typeface {
        val serif = style == CardTextStyle.PRICE ||
            style == CardTextStyle.ENGLISH || style == CardTextStyle.CHINESE
        val medium = when (style) {
            CardTextStyle.CATEGORY, CardTextStyle.PRICE, CardTextStyle.CHINESE,
            CardTextStyle.INGREDIENTS_CN, CardTextStyle.FLAVOR_LABEL, CardTextStyle.KEYWORDS -> true
            else -> false
        }
        return if (serif) {
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                val base = Typeface.create("serif", Typeface.NORMAL)
                Typeface.create(base, if (medium) 500 else 400, false)
            } else {
                Typeface.create("serif", if (medium) Typeface.BOLD else Typeface.NORMAL)
            }
        } else {
            Typeface.create(if (medium) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)
        }
    }

    /** iOS 卡面各元素 tracking（设计点 pt，乘以 u 后再按字号换算成 em）。 */
    private fun trackingPt(style: CardTextStyle): Float = when (style) {
        CardTextStyle.CATEGORY -> 1.4f
        CardTextStyle.META -> 0.4f
        CardTextStyle.FLAVOR_LABEL -> 1.0f
        CardTextStyle.KEYWORDS -> 2.6f
        CardTextStyle.ENGLISH -> -0.15f
        else -> 0f
    }

    fun paintFor(style: CardTextStyle, fontSize: Float, dark: Boolean, u: Float = 0f): Paint {
        val ink = if (dark) android.graphics.Color.parseColor("#F1EDE4") else android.graphics.Color.parseColor("#2A2620")
        val muted = if (dark) android.graphics.Color.parseColor("#B3AC9F") else android.graphics.Color.parseColor("#7A7365")
        val color = when (style) {
            CardTextStyle.CATEGORY -> withAlpha(ink, 0.86f)
            CardTextStyle.META -> withAlpha(muted, 0.95f)
            CardTextStyle.PRICE -> ink
            CardTextStyle.ENGLISH -> ink
            CardTextStyle.CHINESE -> ink
            CardTextStyle.INGREDIENTS_CN -> withAlpha(ink, 0.74f)
            CardTextStyle.INGREDIENTS_EN -> withAlpha(muted, 0.92f)
            CardTextStyle.FLAVOR_LABEL -> withAlpha(ink, 0.66f)
            CardTextStyle.FLAVOR_VALUES -> withAlpha(ink, 0.90f)
            CardTextStyle.FLAVOR_EN -> withAlpha(muted, 0.92f)
            CardTextStyle.KEYWORDS -> withAlpha(ink, 0.78f)
        }
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = fontSize
            typeface = typefaceFor(style)
            if (android.os.Build.VERSION.SDK_INT < 28 && style == CardTextStyle.PRICE) {
                isFakeBoldText = true
            }
            val tracking = trackingPt(style)
            if (tracking != 0f && u > 0f && fontSize > 0f) {
                letterSpacing = tracking * u / fontSize
            }
        }
    }

    fun wrapLines(
        text: String,
        fontSize: Float,
        maxWidth: Float,
        style: CardTextStyle? = null,
        u: Float = 0f
    ): List<String> {
        val paint = if (style != null) {
            paintFor(style, fontSize, false, u)
        } else {
            Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = fontSize }
        }
        val result = mutableListOf<String>()
        text.split("\n").forEach { paragraph ->
            if (paragraph.isBlank()) return@forEach
            val tokens = tokenizeForWrap(paragraph)
            val builder = StringBuilder()
            tokens.forEach { token ->
                if (builder.isEmpty() && token.isBlank()) return@forEach
                val candidate = builder.toString() + token
                if (builder.isNotEmpty() && paint.measureText(candidate.trimEnd()) > maxWidth) {
                    result.add(builder.toString().trimEnd())
                    builder.clear()
                    if (token.isBlank()) return@forEach
                    // 单个 token 比整行还宽时按字符拆开，避免溢出。
                    if (paint.measureText(token.trimEnd()) > maxWidth) {
                        token.forEach { ch ->
                            val next = builder.toString() + ch
                            if (paint.measureText(next) > maxWidth && builder.isNotEmpty()) {
                                result.add(builder.toString())
                                builder.clear()
                                builder.append(ch)
                            } else builder.append(ch)
                        }
                    } else {
                        builder.append(token)
                    }
                } else builder.append(token)
            }
            if (builder.isNotEmpty()) result.add(builder.toString().trimEnd())
        }
        return result
    }

    fun measureBlockHeight(block: CardTextBlock): Float = block.height()

    /** 中英混排换行：CJK 逐字、拉丁按词（含尾随空格），保证英文名不会从单词中间断开。 */
    private fun tokenizeForWrap(paragraph: String): List<String> {
        val tokens = mutableListOf<String>()
        val word = StringBuilder()
        fun flushWord() {
            if (word.isNotEmpty()) {
                tokens.add(word.toString())
                word.clear()
            }
        }
        paragraph.forEach { ch ->
            when {
                ch.isWhitespace() -> {
                    flushWord()
                    tokens.add(" ")
                }
                isCjk(ch) || ch in "，。、；：！？（）【】「」" -> {
                    flushWord()
                    tokens.add(ch.toString())
                }
                else -> word.append(ch)
            }
        }
        flushWord()
        return tokens
    }

    private fun isCjk(ch: Char): Boolean =
        ch.code in 0x4E00..0x9FFF || ch.code in 0x3400..0x4DBF || ch.code in 0x3000..0x303F

    private fun withAlpha(color: Int, alpha: Float): Int = android.graphics.Color.argb(
        (alpha.coerceIn(0f, 1f) * 255).toInt(),
        android.graphics.Color.red(color),
        android.graphics.Color.green(color),
        android.graphics.Color.blue(color)
    )
}
