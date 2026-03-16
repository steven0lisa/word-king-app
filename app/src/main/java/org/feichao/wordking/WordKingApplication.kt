package org.feichao.wordking

import android.app.Application
import org.feichao.wordking.data.database.WordKingDatabase

class WordKingApplication : Application() {

    val database: WordKingDatabase by lazy {
        WordKingDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: WordKingApplication
            private set
    }
}
