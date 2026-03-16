package org.feichao.wordking.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户配置实体
 * 对应需求文档中的 UserConfig 实体
 * 敏感信息（Git SSH Key, AI API Key）加密存储，不保存在数据库
 */
@Entity(tableName = "user_config")
data class UserConfig(
    @PrimaryKey
    val id: Int = 1,                   // 固定为1，只有单条配置
    val currentLanguage: String = "en",  // 当前学习语言，默认英语
    val dailyNewWordLimit: Int = 300,  // 每日新单词上限
    val vibrateEnabled: Boolean = true, // 振动反馈开关
    val autoGenerateWord: Boolean = true, // 自动生成单词开关
    val checkUpdateOnStart: Boolean = true  // 启动时检查更新
    // 敏感信息通过 EncryptedSharedPreferences 单独存储：
    // - gitRepoUrl
    // - gitSshKey
    // - aiApiUrl
    // - aiApiKey
    // - aiModelId
)
