package com.ht.intelza

import android.app.Application
import com.ht.intelza.data.BackupManager
import com.ht.intelza.data.ClassRepository
import com.ht.intelza.data.ImageStore
import com.ht.intelza.data.QuestionRepository
import com.ht.intelza.data.ReportRepository
import com.ht.intelza.data.SessionRepository
import com.ht.intelza.data.SettingsRepository
import com.ht.intelza.data.db.IntelzaDatabase
import com.ht.intelza.export.ReportExporter

/** Creates and holds the app's long-lived objects. */
class AppContainer(val application: Application) {
    val database: IntelzaDatabase = IntelzaDatabase.build(application)
    val images = ImageStore(application)
    val settings = SettingsRepository(application)
    val classes = ClassRepository(database)
    val questions = QuestionRepository(database, images)
    val sessions = SessionRepository(database)
    val reports = ReportRepository(database, sessions)
    val exporter = ReportExporter(application, reports, classes, settings)
    val backups = BackupManager(application, database, images)
}
