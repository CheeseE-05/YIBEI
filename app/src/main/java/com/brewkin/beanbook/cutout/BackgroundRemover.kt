package com.brewkin.beanbook.cutout

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer

/**
 * Offline U²-Netp matting. The 4.6 MB model is bundled in assets, so cutout works
 * without Google Play services or a network connection.
 */
object BackgroundRemover {
    private const val SIZE = 320

    @Volatile
    private var session: OrtSession? = null

    @Volatile
    private var loadError: Throwable? = null

    private fun getSession(context: Context): OrtSession {
        session?.let { return it }
        synchronized(this) {
            session?.let { return it }
            loadError?.let { throw IllegalStateException("抠图模型加载失败", it) }
            return try {
                val env = OrtEnvironment.getEnvironment()
                val bytes = context.assets.open("u2netp.onnx").use { it.readBytes() }
                env.createSession(bytes, OrtSession.SessionOptions()).also { session = it }
            } catch (t: Throwable) {
                loadError = t
                throw t
            }
        }
    }

    suspend fun removeBackground(context: Context, source: Bitmap): Result<Bitmap> =
        withContext(Dispatchers.Default) {
            runCatching {
                require(source.width > 0 && source.height > 0)
                val sess = getSession(context)
                val inputName = sess.inputNames.first()
                val pixels = IntArray(SIZE * SIZE)
                val scaled = Bitmap.createScaledBitmap(source, SIZE, SIZE, true)
                scaled.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE)
                if (scaled !== source) scaled.recycle()

                val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
                val std = floatArrayOf(0.229f, 0.224f, 0.225f)
                val chw = FloatArray(3 * SIZE * SIZE)
                var idx = 0
                for (c in 0..2) {
                    for (y in 0 until SIZE) {
                        for (x in 0 until SIZE) {
                            val p = pixels[y * SIZE + x]
                            val value = when (c) {
                                0 -> (p shr 16) and 0xFF
                                1 -> (p shr 8) and 0xFF
                                else -> p and 0xFF
                            } / 255f
                            chw[idx++] = (value - mean[c]) / std[c]
                        }
                    }
                }

                val tensor = OnnxTensor.createTensor(
                    OrtEnvironment.getEnvironment(),
                    FloatBuffer.wrap(chw),
                    longArrayOf(1, 3, SIZE.toLong(), SIZE.toLong())
                )
                val mask = FloatArray(SIZE * SIZE)
                tensor.use {
                    sess.run(mapOf(inputName to it)).use { result ->
                        val output = result[0] as OnnxTensor
                        val buffer = output.floatBuffer
                        val count = minOf(buffer.remaining(), mask.size)
                        buffer.get(mask, 0, count)
                    }
                }

                var min = Float.MAX_VALUE
                var max = -Float.MAX_VALUE
                for (v in mask) {
                    if (v < min) min = v
                    if (v > max) max = v
                }
                val scale = if (max - min > 0.00001f) 1f / (max - min) else 0f

                val alphaSmall = IntArray(SIZE * SIZE)
                for (i in mask.indices) {
                    val normalized = ((mask[i] - min) * scale).coerceIn(0f, 1f)
                    // A soft threshold keeps the bean bag crisp while dropping haze.
                    val a = ((normalized - 0.06f) / 0.88f).coerceIn(0f, 1f)
                    alphaSmall[i] = ((a * 255f).toInt().coerceIn(0, 255)) shl 24
                }
                val alphaBitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
                alphaBitmap.setPixels(alphaSmall, 0, SIZE, 0, 0, SIZE, SIZE)
                val alphaFull = Bitmap.createScaledBitmap(alphaBitmap, source.width, source.height, true)
                alphaBitmap.recycle()

                val srcPixels = IntArray(source.width * source.height)
                source.getPixels(srcPixels, 0, source.width, 0, 0, source.width, source.height)
                val alphaPixels = IntArray(source.width * source.height)
                alphaFull.getPixels(alphaPixels, 0, source.width, 0, 0, source.width, source.height)
                alphaFull.recycle()

                for (i in srcPixels.indices) {
                    val a = (alphaPixels[i] ushr 24) and 0xFF
                    val srcAlpha = (srcPixels[i] ushr 24) and 0xFF
                    val finalAlpha = ((a * srcAlpha) / 255f).toInt()
                    srcPixels[i] = (srcPixels[i] and 0x00FFFFFF) or (finalAlpha shl 24)
                }

                val cut = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
                cut.setPixels(srcPixels, 0, source.width, 0, 0, source.width, source.height)
                cropTransparent(cut)
            }
        }

    private fun cropTransparent(bitmap: Bitmap): Bitmap {
        var left = bitmap.width
        var top = bitmap.height
        var right = -1
        var bottom = -1
        val row = IntArray(bitmap.width)
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            for (x in row.indices) {
                if ((row[x] ushr 24) > 18) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }
        if (right <= left || bottom <= top) return bitmap
        val padX = (bitmap.width * 0.035f).toInt().coerceAtLeast(8)
        val padY = (bitmap.height * 0.035f).toInt().coerceAtLeast(8)
        val x = (left - padX).coerceAtLeast(0)
        val y = (top - padY).coerceAtLeast(0)
        val w = (right + padX + 1).coerceAtMost(bitmap.width) - x
        val h = (bottom + padY + 1).coerceAtMost(bitmap.height) - y
        if (w <= 0 || h <= 0) return bitmap
        val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)
        if (cropped !== bitmap) bitmap.recycle()
        return cropped
    }
}
