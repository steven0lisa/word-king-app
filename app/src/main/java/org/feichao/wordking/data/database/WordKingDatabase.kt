package org.feichao.wordking.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.feichao.wordking.data.dao.*
import org.feichao.wordking.data.entity.*

@Database(
    entities = [
        Word::class,
        LearningRecord::class,
        UserConfig::class,
        SyncMeta::class,
        OfflineLog::class
    ],
    version = 1,
    exportSchema = true
)
abstract class WordKingDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao
    abstract fun learningRecordDao(): LearningRecordDao
    abstract fun userConfigDao(): UserConfigDao
    abstract fun syncMetaDao(): SyncMetaDao
    abstract fun offlineLogDao(): OfflineLogDao

    companion object {
        @Volatile
        private var INSTANCE: WordKingDatabase? = null

        fun getDatabase(context: Context): WordKingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WordKingDatabase::class.java,
                    "word_king_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
