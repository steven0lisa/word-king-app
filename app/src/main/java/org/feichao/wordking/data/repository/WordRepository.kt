package org.feichao.wordking.data.repository

import kotlinx.coroutines.flow.Flow
import org.feichao.wordking.data.dao.WordDao
import org.feichao.wordking.data.entity.Word

class WordRepository(private val wordDao: WordDao) {

    fun getAllWords(): Flow<List<Word>> = wordDao.getAllWords()

    fun getWordsByLanguage(languageCode: String): Flow<List<Word>> =
        wordDao.getWordsByLanguage(languageCode)

    suspend fun getWordById(id: String): Word? = wordDao.getWordById(id)

    suspend fun getUnlearnedWords(languageCode: String, limit: Int): List<Word> =
        wordDao.getUnlearnedWords(languageCode, limit)

    suspend fun getLearningWords(languageCode: String): List<Word> =
        wordDao.getLearningWords(languageCode)

    fun getMasteredWords(languageCode: String): Flow<List<Word>> =
        wordDao.getMasteredWords(languageCode)

    suspend fun getMasteredWordsSync(languageCode: String): List<Word> =
        wordDao.getMasteredWordsSync(languageCode)

    suspend fun getWordsNeedingReview(languageCode: String): List<Word> {
        val currentTime = System.currentTimeMillis()
        return wordDao.getWordsNeedingReview(languageCode, currentTime)
    }

    suspend fun getUnlearnedWordCount(languageCode: String): Int =
        wordDao.getUnlearnedWordCount(languageCode)

    suspend fun getLearningWordCount(languageCode: String): Int =
        wordDao.getLearningWordCount(languageCode)

    suspend fun getMasteredWordCount(languageCode: String): Int =
        wordDao.getMasteredWordCount(languageCode)

    suspend fun getTotalWordCount(languageCode: String): Int =
        wordDao.getTotalWordCount(languageCode)

    suspend fun getAllOriginalWords(languageCode: String): List<String> =
        wordDao.getAllOriginalWords(languageCode)

    fun searchWords(languageCode: String, keyword: String): Flow<List<Word>> =
        wordDao.searchWords(languageCode, keyword)

    suspend fun insertWord(word: Word) = wordDao.insertWord(word)

    suspend fun insertWords(words: List<Word>) = wordDao.insertWords(words)

    suspend fun updateWord(word: Word) = wordDao.updateWord(word)

    suspend fun deleteWord(word: Word) = wordDao.deleteWord(word)

    suspend fun deleteInvalidWords(validIds: List<String>) =
        wordDao.deleteInvalidWords(validIds)

    suspend fun deleteWordsByLanguage(languageCode: String) =
        wordDao.deleteWordsByLanguage(languageCode)

    suspend fun getAllWordsSync(): List<Word> = wordDao.getAllWordsSync()
}
