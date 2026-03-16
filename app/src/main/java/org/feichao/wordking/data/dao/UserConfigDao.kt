package org.feichao.wordking.data.dao

import androidx.room.*
import org.feichao.wordking.data.entity.UserConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface UserConfigDao {

    @Query("SELECT * FROM user_config WHERE id = 1")
    fun getUserConfig(): Flow<UserConfig?>

    @Query("SELECT * FROM user_config WHERE id = 1")
    suspend fun getUserConfigSync(): UserConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserConfig(config: UserConfig)

    @Update
    suspend fun updateUserConfig(config: UserConfig)

    @Query("UPDATE user_config SET currentLanguage = :languageCode WHERE id = 1")
    suspend fun updateCurrentLanguage(languageCode: String)

    @Query("UPDATE user_config SET dailyNewWordLimit = :limit WHERE id = 1")
    suspend fun updateDailyLimit(limit: Int)

    @Query("UPDATE user_config SET vibrateEnabled = :enabled WHERE id = 1")
    suspend fun updateVibrateEnabled(enabled: Boolean)

    @Query("UPDATE user_config SET autoGenerateWord = :enabled WHERE id = 1")
    suspend fun updateAutoGenerate(enabled: Boolean)

    @Query("UPDATE user_config SET checkUpdateOnStart = :enabled WHERE id = 1")
    suspend fun updateCheckUpdate(enabled: Boolean)
}
