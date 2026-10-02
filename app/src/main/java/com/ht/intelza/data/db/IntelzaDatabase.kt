package com.ht.intelza.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ClassEntity::class,
        StudentEntity::class,
        SubjectEntity::class,
        TopicEntity::class,
        QuestionEntity::class,
        SessionEntity::class,
        SessionStudentEntity::class,
        SessionQuestionEntity::class,
        ResponseEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class IntelzaDatabase : RoomDatabase() {
    abstract fun classDao(): ClassDao
    abstract fun studentDao(): StudentDao
    abstract fun subjectDao(): SubjectDao
    abstract fun topicDao(): TopicDao
    abstract fun questionDao(): QuestionDao
    abstract fun sessionDao(): SessionDao
    abstract fun reportDao(): ReportDao

    companion object {
        const val FILE_NAME = "intelza.db"
        const val SCHEMA_VERSION = 1

        val DEFAULT_SUBJECTS = listOf("Maths", "English", "Science", "Social Studies")

        fun build(context: Context): IntelzaDatabase =
            Room.databaseBuilder(context, IntelzaDatabase::class.java, FILE_NAME)
                .addCallback(
                    object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            for (name in DEFAULT_SUBJECTS) {
                                db.execSQL("INSERT INTO subjects (name) VALUES (?)", arrayOf(name))
                            }
                        }
                    },
                )
                .build()
    }
}
