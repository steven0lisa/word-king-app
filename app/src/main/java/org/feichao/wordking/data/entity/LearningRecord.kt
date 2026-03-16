package org.feichao.wordking.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 学习记录实体
 * 对应需求文档中的 LearningRecord 实体
 */
@Entity(tableName = "learning_records")
data class LearningRecord(
    @PrimaryKey
    val id: String,                    // 全局唯一ID
    val languageCode: String,          // 语言代码
    val wordId: String,                // 关联的单词ID
    val reviewTime: Long,              // 复习时间（时间戳）
    val isCorrect: Boolean,            // 是否回答正确
    val stageBefore: Int,              // 复习前阶段
    val stageAfter: Int                // 复习后阶段
)
