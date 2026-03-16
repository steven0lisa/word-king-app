package org.feichao.wordking.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 同步元信息实体
 * 对应需求文档中的 SyncMeta 实体
 */
@Entity(tableName = "sync_meta")
data class SyncMeta(
    @PrimaryKey
    val id: Int = 1,                   // 固定为1，只有单条记录
    val lastSyncTimestamp: Long = 0,   // 上次同步时间戳
    val lastSyncDeviceId: String = "", // 上次同步设备ID
    val currentVersion: String = "",   // 当前版本（v+时间戳）
    val conflictCount: Int = 0         // 冲突次数
)
