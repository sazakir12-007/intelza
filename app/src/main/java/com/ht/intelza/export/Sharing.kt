package com.ht.intelza.export

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.ht.intelza.ui.common.findActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** Writes files the teacher chooses to share, and hands them to other apps or the printer. */
object Sharing {

    /** A file in the app's private export folder, replacing any older file with that name. */
    fun exportFile(context: Context, fileName: String): File {
        val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        return File(dir, safeFileName(fileName)).apply { delete() }
    }

    fun share(context: Context, file: File, mimeType: String, chooserTitle: String): Boolean {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(send, chooserTitle)
        // Starting another app from outside a screen needs a new task.
        if (context.findActivity() == null) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(chooser)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    /**
     * Sends a PDF file to Android's print dialog. Printing must start from a screen, so
     * [context] has to belong to an activity; returns false otherwise.
     */
    fun printPdf(context: Context, file: File, jobName: String): Boolean {
        val activity = context.findActivity() ?: return false
        val printManager = activity.getSystemService(PrintManager::class.java) ?: return false
        printManager.print(jobName, PdfFilePrintAdapter(file), PrintAttributes.Builder().build())
        return true
    }

    fun safeFileName(name: String): String =
        name.replace(Regex("""[\\/:*?"<>|\u0000-\u001F]"""), "_").trim().ifEmpty { "export" }

    private const val EXPORT_DIR = "exports"
}

private class PdfFilePrintAdapter(private val file: File) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(file.name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()
        callback.onLayoutFinished(info, oldAttributes != newAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback,
    ) {
        try {
            FileInputStream(file).use { input ->
                FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) }
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.message)
        }
    }
}
