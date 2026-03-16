package org.feichao.wordking.data.dao

import androidx.room.*
import org.feichao.wordking.data.entity.SyncMeta
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetaDao {

    @Query("SELECT * FROM sync_meta WHERE id = 1")
    fun getSyncMeta(): Flow<SyncMeta?>

    @Query("SELECT * FROM sync_meta WHERE id = 1")
    suspend fun getSyncMetaSync(): SyncMeta?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncMeta(meta: SyncMeta)

    @Update
    suspend fun updateSyncMeta(meta: SyncMeta)

    @Query("UPDATE sync_meta SET lastSyncTimestamp = :timestamp, lastSyncDeviceId = :deviceId WHERE id = 1")
    suspend fun updateLastSync(timestamp: Long, deviceId: String)

    @Query("UPDATE sync_meta SET conflictCount = conflictCount + 1 WHERE id = 1")
    suspend fun incrementConflictCount()
}
