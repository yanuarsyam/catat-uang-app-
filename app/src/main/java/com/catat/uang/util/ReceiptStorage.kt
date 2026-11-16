package com.catat.uang.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ReceiptStorage {
    private const val RECEIPT_DIR = "receipts"

    fun saveReceiptFromUri(context: Context, uri: Uri): String? {
        return try {
            val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

            val original = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            val rotated = rotateBitmapIfRequired(original, orientation)
            val maxDim = 1600
            val scale = minOf(maxDim.toFloat() / rotated.width, maxDim.toFloat() / rotated.height, 1f)
            val finalBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    rotated,
                    (rotated.width * scale).toInt(),
                    (rotated.height * scale).toInt(),
                    true
                )
            } else {
                rotated
            }

            val dir = File(context.filesDir, RECEIPT_DIR).apply { if (!exists()) mkdirs() }
            val destFile = File(dir, "struk_${System.currentTimeMillis()}.jpg")
            FileOutputStream(destFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 82, out)
                out.flush()
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun deleteReceiptFile(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            val file = File(path)
            if (file.exists()) file.delete()
        } catch (_: Exception) {
        }
    }

    private fun rotateBitmapIfRequired(bitmap: Bitmap, orientation: Int): Bitmap {
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
