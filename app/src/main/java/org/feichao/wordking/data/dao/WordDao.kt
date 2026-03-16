package org.feichao.wordking.data.dao

import androidx.room.*
import org.feichao.wordking.data.entity.Word
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    @Query("SELECT * FROM words ORDER BY createTime DESC")
    fun getAllWords(): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE languageCode = :languageCode ORDER BY createTime DESC")
    fun getWordsByLanguage(languageCode: String): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getWordById(id: String): Word?

    @Query("SELECT * FROM words WHERE stage = 0 AND languageCode = :languageCode ORDER BY createTime ASC LIMIT :limit")
    suspend fun getUnlearnedWords(languageCode: String, limit: Int): List<Word>

    @Query("SELECT * FROM words WHERE stage > 0 AND stage < 12 AND languageCode = :languageCode ORDER BY nextReviewTime ASC")
    suspend fun getLearningWords(languageCode: String): List<Word>

    @Query("SELECT * FROM words WHERE stage = 12 AND languageCode = :languageCode")
    fun getMasteredWords(languageCode: String): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE stage = 12 AND languageCode = :languageCode")
    suspend fun getMasteredWordsSync(languageCode: String): List<Word>

    @Query("SELECT * FROM words WHERE nextReviewTime <= :currentTime AND languageCode = :languageCode AND stage > 0 AND stage < 12")
    suspend fun getWordsNeedingReview(languageCode: String, currentTime: Long): List<Word>

    @Query("SELECT COUNT(*) FROM words WHERE stage = 0 AND languageCode = :languageCode")
    suspend fun getUnlearnedWordCount(languageCode: String): Int

    @Query("SELECT COUNT(*) FROM words WHERE stage > 0 AND stage < 12 AND languageCode = :languageCode")
    suspend fun getLearningWordCount(languageCode: String): Int

    @Query("SELECT COUNT(*) FROM words WHERE stage = 12 AND languageCode = :languageCode")
    suspend fun getMasteredWordCount(languageCode: String): Int

    @Query("SELECT COUNT(*) FROM words WHERE languageCode = :languageCode")
    suspend fun getTotalWordCount(languageCode: String): Int

    // Flow 版本的计数方法 - 用于监听数据变化
    @Query("SELECT COUNT(*) FROM words WHERE stage = 0 AND languageCode = :languageCode")
    fun getUnlearnedWordCountFlow(languageCode: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE stage > 0 AND stage < 12 AND languageCode = :languageCode")
    fun getLearningWordCountFlow(languageCode: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE stage = 12 AND languageCode = :languageCode")
    fun getMasteredWordCountFlow(languageCode: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE languageCode = :languageCode")
    fun getTotalWordCountFlow(languageCode: String): Flow<Int>

    @Query("SELECT DISTINCT originalWord FROM words WHERE languageCode = :languageCode")
    suspend fun getAllOriginalWords(languageCode: String): List<String>

    @Query("SELECT * FROM words WHERE languageCode = :languageCode AND (originalWord LIKE '%' || :keyword || '%' OR chineseTranslation LIKE '%' || :keyword || '%')")
    fun searchWords(languageCode: String, keyword: String): Flow<List<Word>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: Word)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<Word>)

    @Update
    suspend fun updateWord(word: Word)

    @Delete
    suspend fun deleteWord(word: Word)

    @Query("DELETE FROM words WHERE id NOT IN (:validIds)")
    suspend fun deleteInvalidWords(validIds: List<String>)

    @Query("DELETE FROM words WHERE languageCode = :languageCode")
    suspend fun deleteWordsByLanguage(languageCode: String)

    @Query("SELECT * FROM words")
    suspend fun getAllWordsSync(): List<Word>
}
