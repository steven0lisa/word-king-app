package org.feichao.wordking.data.dao

import androidx.room.*
import org.feichao.wordking.data.entity.OfflineLog

@Dao
interface OfflineLogDao {

    @Query("SELECT * FROM offline_logs ORDER BY createTime DESC")
    suspend fun getAllLogs(): List<OfflineLog>

    @Query("SELECT * FROM offline_logs WHERE isSync = 0 ORDER BY createTime ASC")
    suspend fun getUnsyncedLogs(): List<OfflineLog>

    @Query("SELECT COUNT(*) FROM offline_logs WHERE isSync = 0")
    suspend fun getUnsyncedCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: OfflineLog)

    @Update
    suspend fun updateLog(log: OfflineLog)

    @Query("UPDATE offline_logs SET isSync = 1 WHERE logId = :logId")
    suspend fun markAsSynced(logId: String)

    @Query("UPDATE offline_logs SET isSync = 1")
    suspend fun markAllAsSynced()

    @Delete
    suspend fun deleteLog(log: OfflineLog)

    @Query("DELETE FROM offline_logs WHERE isSync = 1")
    suspend fun deleteSyncedLogs()
}
