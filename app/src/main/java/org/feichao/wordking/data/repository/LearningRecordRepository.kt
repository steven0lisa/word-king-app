package org.feichao.wordking.data.repository

import org.feichao.wordking.data.dao.LearningRecordDao
import org.feichao.wordking.data.entity.LearningRecord

class LearningRecordRepository(private val learningRecordDao: LearningRecordDao) {

    suspend fun getAllRecords(): List<LearningRecord> = learningRecordDao.getAllRecords()

    suspend fun getRecordsByLanguage(languageCode: String): List<LearningRecord> =
        learningRecordDao.getRecordsByLanguage(languageCode)

    suspend fun getRecordsSince(startTime: Long): List<LearningRecord> =
        learningRecordDao.getRecordsSince(startTime)

    suspend fun getRecordsBetween(startTime: Long, endTime: Long): List<LearningRecord> =
        learningRecordDao.getRecordsBetween(startTime, endTime)

    suspend fun getNewWordCountSince(startTime: Long): Int =
        learningRecordDao.getNewWordCountSince(startTime)

    suspend fun getReviewWordCountSince(startTime: Long): Int =
        learningRecordDao.getReviewWordCountSince(startTime)

    suspend fun getCorrectCount(): Int = learningRecordDao.getCorrectCount()

    suspend fun getTotalCount(): Int = learningRecordDao.getTotalCount()

    suspend fun getRecordCountByWordId(wordId: String): Int =
        learningRecordDao.getRecordCountByWordId(wordId)

    suspend fun insertRecord(record: LearningRecord) =
        learningRecordDao.insertRecord(record)

    suspend fun insertRecords(records: List<LearningRecord>) =
        learningRecordDao.insertRecords(records)

    suspend fun deleteRecord(record: LearningRecord) =
        learningRecordDao.deleteRecord(record)

    suspend fun deleteInvalidRecords(validWordIds: List<String>) =
        learningRecordDao.deleteInvalidRecords(validWordIds)
}
