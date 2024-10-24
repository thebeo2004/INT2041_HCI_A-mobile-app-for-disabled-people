package com.example.amobileappfordisabledpeople

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import java.nio.ByteBuffer

class YuvToRgbConverter(context: Context) {

    fun yuvToRgb(image: Image, output: Bitmap) {
        assert(image.format == ImageFormat.YUV_420_888)

        val yPlane = image.planes[0].buffer
        val uPlane = image.planes[1].buffer
        val vPlane = image.planes[2].buffer

        val width = image.width
        val height = image.height
        val yRowStride = image.planes[0].rowStride
        val uvRowStride = image.planes[1].rowStride
        val uvPixelStride = image.planes[1].pixelStride

        var yp: Int
        var up: Int
        var vp: Int
        var yVal: Int
        var uVal: Int
        var vVal: Int
        var r: Int
        var g: Int
        var b: Int

        val argbArray = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                yp = y * yRowStride + x
                up = (y shr 1) * uvRowStride + (x shr 1) * uvPixelStride
                vp = (y shr 1) * uvRowStride + (x shr 1) * uvPixelStride

                yVal = yPlane[yp].toInt() and 0xFF
                uVal = uPlane[up].toInt() and 0xFF - 128
                vVal = vPlane[vp].toInt() and 0xFF - 128

                r = yVal + (1.370705 * vVal).toInt()
                g = yVal - (0.337633 * uVal).toInt() - (0.698001 * vVal).toInt()
                b = yVal + (1.732446 * uVal).toInt()

                argbArray[y * width + x] = (0xFF shl 24) or
                        (r.coerceIn(0, 255) shl 16) or
                        (g.coerceIn(0, 255) shl 8) or
                        b.coerceIn(0, 255)
            }
        }

        output.setPixels(argbArray, 0, width, 0, 0, width, height)
    }
}
