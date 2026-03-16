package org.feichao.wordking.data.dao

import androidx.room.*
import org.feichao.wordking.data.entity.LearningRecord

@Dao
interface LearningRecordDao {

    @Query("SELECT * FROM learning_records ORDER BY reviewTime DESC")
    suspend fun getAllRecords(): List<LearningRecord>

    @Query("SELECT * FROM learning_records WHERE languageCode = :languageCode ORDER BY reviewTime DESC")
    suspend fun getRecordsByLanguage(languageCode: String): List<LearningRecord>

    @Query("SELECT * FROM learning_records WHERE reviewTime >= :startTime")
    suspend fun getRecordsSince(startTime: Long): List<LearningRecord>

    @Query("SELECT * FROM learning_records WHERE reviewTime >= :startTime AND reviewTime < :endTime")
    suspend fun getRecordsBetween(startTime: Long, endTime: Long): List<LearningRecord>

    @Query("SELECT COUNT(DISTINCT wordId) FROM learning_records WHERE reviewTime >= :startTime AND stageBefore = 0")
    suspend fun getNewWordCountSince(startTime: Long): Int

    @Query("SELECT COUNT(DISTINCT wordId) FROM learning_records WHERE reviewTime >= :startTime AND stageBefore > 0")
    suspend fun getReviewWordCountSince(startTime: Long): Int

    @Query("SELECT COUNT(*) FROM learning_records WHERE isCorrect = 1")
    suspend fun getCorrectCount(): Int

    @Query("SELECT COUNT(*) FROM learning_records")
    suspend fun getTotalCount(): Int

    // 最近7天正确率
    @Query("SELECT COUNT(*) FROM learning_records WHERE isCorrect = 1 AND reviewTime >= :startTime")
    suspend fun getCorrectCountSince(startTime: Long): Int

    @Query("SELECT COUNT(*) FROM learning_records WHERE reviewTime >= :startTime")
    suspend fun getTotalCountSince(startTime: Long): Int

    @Query("SELECT COUNT(DISTINCT wordId) FROM learning_records WHERE wordId = :wordId")
    suspend fun getRecordCountByWordId(wordId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: LearningRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<LearningRecord>)

    @Delete
    suspend fun deleteRecord(record: LearningRecord)

    @Query("DELETE FROM learning_records WHERE wordId NOT IN (:validWordIds)")
    suspend fun deleteInvalidRecords(validWordIds: List<String>)
}
