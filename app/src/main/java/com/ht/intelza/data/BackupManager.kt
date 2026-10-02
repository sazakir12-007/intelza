package com.ht.intelza.data

import android.app.Application
import android.content.Intent
import android.net.Uri
import com.ht.intelza.data.db.IntelzaDatabase
import com.ht.intelza.export.Sharing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Saves everything (database and question pictures) into one file and restores it,
 * e.g. when moving to a new phone (requirement G3).
 */
class BackupManager(
    private val app: Application,
    private val database: IntelzaDatabase,
    private val images: ImageStore,
) {
    class InvalidBackupException(message: String) : IOException(message)

    suspend fun createBackup(fileName: String): File = withContext(Dispatchers.IO) {
        // Fold the write-ahead log into the main file so it holds every change.
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        val dbFile = app.getDatabasePath(IntelzaDatabase.FILE_NAME)
        val file = Sharing.exportFile(app, fileName)
        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(
                JSONObject()
                    .put("app", APP_ID)
                    .put("schemaVersion", IntelzaDatabase.SCHEMA_VERSION)
                    .put("createdAt", System.currentTimeMillis())
                    .toString()
                    .toByteArray(),
            )
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(DATABASE_ENTRY))
            dbFile.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
            images.directory.listFiles()?.forEach { image ->
                zip.putNextEntry(ZipEntry("${ImageStore.DIRECTORY}/${image.name}"))
                image.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        file
    }

    /** Copies a backup file to a location the teacher picked. */
    suspend fun copyTo(file: File, destination: Uri) = withContext(Dispatchers.IO) {
        app.contentResolver.openOutputStream(destination)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        } ?: throw IOException("Could not write the backup")
    }

    /**
     * Replaces all data with the backup's. The app restarts afterwards so the database is
     * reopened cleanly.
     */
    suspend fun restore(source: Uri) = withContext(Dispatchers.IO) {
        val staging = File(app.cacheDir, "restore").apply {
            deleteRecursively()
            mkdirs()
        }
        var manifest: JSONObject? = null
        val input = app.contentResolver.openInputStream(source) ?: throw InvalidBackupException("Cannot open file")
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                if (entry.isDirectory) continue
                // Reject paths that try to escape the staging folder.
                val target = File(staging, name).canonicalFile
                if (!target.path.startsWith(staging.canonicalPath + File.separator)) {
                    throw InvalidBackupException("Unexpected entry $name")
                }
                if (name == MANIFEST) {
                    manifest = JSONObject(zip.readBytes().toString(Charsets.UTF_8))
                } else {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { zip.copyTo(it) }
                }
            }
        }
        val info = manifest ?: throw InvalidBackupException("Not an Intelza backup")
        if (info.optString("app") != APP_ID) throw InvalidBackupException("Not an Intelza backup")
        if (info.optInt("schemaVersion", Int.MAX_VALUE) > IntelzaDatabase.SCHEMA_VERSION) {
            throw InvalidBackupException("Backup is from a newer version of Intelza")
        }
        val stagedDb = File(staging, DATABASE_ENTRY)
        if (!stagedDb.exists()) throw InvalidBackupException("Backup has no data")

        database.close()
        val dbFile = app.getDatabasePath(IntelzaDatabase.FILE_NAME)
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
        stagedDb.copyTo(dbFile, overwrite = true)

        images.directory.listFiles()?.forEach { it.delete() }
        File(staging, ImageStore.DIRECTORY).listFiles()?.forEach { it.copyTo(File(images.directory, it.name), overwrite = true) }
        staging.deleteRecursively()
    }

    /** Relaunches the app in a fresh process. */
    fun restartApp() {
        val intent = app.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        app.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    private companion object {
        const val APP_ID = "com.ht.intelza"
        const val MANIFEST = "backup.json"
        const val DATABASE_ENTRY = "database/intelza.db"
    }
}
