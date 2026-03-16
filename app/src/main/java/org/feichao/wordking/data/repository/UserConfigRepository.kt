package org.feichao.wordking.data.repository

import kotlinx.coroutines.flow.Flow
import org.feichao.wordking.data.dao.UserConfigDao
import org.feichao.wordking.data.entity.UserConfig

class UserConfigRepository(private val userConfigDao: UserConfigDao) {

    fun getUserConfig(): Flow<UserConfig?> = userConfigDao.getUserConfig()

    suspend fun getUserConfigSync(): UserConfig? = userConfigDao.getUserConfigSync()

    suspend fun insertUserConfig(config: UserConfig) = userConfigDao.insertUserConfig(config)

    suspend fun updateUserConfig(config: UserConfig) = userConfigDao.updateUserConfig(config)

    suspend fun updateCurrentLanguage(languageCode: String) =
        userConfigDao.updateCurrentLanguage(languageCode)

    suspend fun updateDailyLimit(limit: Int) = userConfigDao.updateDailyLimit(limit)

    suspend fun updateVibrateEnabled(enabled: Boolean) =
        userConfigDao.updateVibrateEnabled(enabled)

    suspend fun updateAutoGenerate(enabled: Boolean) =
        userConfigDao.updateAutoGenerate(enabled)

    suspend fun updateCheckUpdate(enabled: Boolean) =
        userConfigDao.updateCheckUpdate(enabled)

    suspend fun initDefaultConfig() {
        val existing = userConfigDao.getUserConfigSync()
        if (existing == null) {
            userConfigDao.insertUserConfig(UserConfig())
        }
    }
}
