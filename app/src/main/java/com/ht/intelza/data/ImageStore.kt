package com.ht.intelza.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Stores question pictures as JPEG files in the app's private storage. */
class ImageStore(private val context: Context) {

    val directory: File = File(context.filesDir, DIRECTORY).apply { mkdirs() }

    fun file(name: String) = File(directory, name)

    /** A temporary file the camera app can write a new photo into. */
    fun newCaptureUri(): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "capture.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Copies a picture into the store, scaled down to at most [MAX_SIZE] pixels and turned
     * upright. Returns the stored file name.
     */
    suspend fun import(uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Not a picture" }

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIZE) sample *= 2
        val decoded = resolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Could not read the picture")

        val rotation = resolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        val scale = minOf(1f, MAX_SIZE.toFloat() / maxOf(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation)
        }
        val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        val name = "${UUID.randomUUID()}.jpg"
        file(name).outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        if (upright !== decoded) upright.recycle()
        decoded.recycle()
        name
    }

    suspend fun load(name: String, maxSize: Int = MAX_SIZE): Bitmap? = withContext(Dispatchers.IO) {
        val file = file(name)
        if (!file.exists()) return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSize) sample *= 2
        BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    /** Deletes pictures that no question refers to any more. */
    suspend fun deleteUnreferenced(referenced: Collection<String>) = withContext(Dispatchers.IO) {
        val keep = referenced.toHashSet()
        directory.listFiles()?.filter { it.name !in keep }?.forEach { it.delete() }
    }

    companion object {
        const val DIRECTORY = "images"
        const val MAX_SIZE = 1600
    }
}
