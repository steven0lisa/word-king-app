package org.feichao.wordking.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 单词实体
 * 对应需求文档中的 Word 实体
 */
@Entity(tableName = "words")
data class Word(
    @PrimaryKey
    val id: String,                    // 全局唯一ID，格式：时间戳_4位随机数
    val languageCode: String,          // 语言代码：en/id/th/ko/ja/es/pt/fr
    val originalWord: String,          // 原语言单词/短句
    val chineseTranslation: String,    // 中文翻译
    val exampleSentence: String = "",  // 可选例句
    val stage: Int = 0,                // 记忆阶段：0=未学，1-12=学习中，12=永久掌握
    val nextReviewTime: Long = 0,      // 下次复习时间（时间戳）
    val correctStreak: Int = 0,        // 连续答对次数
    val lastReviewTime: Long = 0,      // 上次复习时间（时间戳）
    val createTime: Long = System.currentTimeMillis()  // 创建时间
)
