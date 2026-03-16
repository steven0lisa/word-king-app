package org.feichao.wordking.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 离线日志实体
 * 对应需求文档中的 OfflineLog 实体
 */
@Entity(tableName = "offline_logs")
data class OfflineLog(
    @PrimaryKey
    val logId: String,                 // 日志ID
    val operationType: String,         // 操作类型：ADD_WORD/UPDATE_PROGRESS/ADD_RECORD
    val operationData: String,         // 操作数据（JSON格式）
    val createTime: Long = System.currentTimeMillis(),  // 创建时间
    val isSync: Boolean = false        // 是否已同步
)
