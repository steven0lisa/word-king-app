package org.feichao.wordking.util

import org.feichao.wordking.data.entity.*
import java.io.StringReader
import java.io.StringWriter

/**
 * CSV文件处理工具
 * 对应需求文档中的 CSV 读写函数
 */
object CsvUtils {

    /**
     * 将单词列表转换为CSV字符串
     */
    fun wordsToCsv(words: List<Word>): String {
        val writer = StringWriter()
        // 写入表头（仅基础信息）
        writer.write("id,languageCode,originalWord,chineseTranslation,exampleSentence\n")

        words.forEach { word ->
            writer.write("${escapeCsv(word.id)},")
            writer.write("${escapeCsv(word.languageCode)},")
            writer.write("${escapeCsv(word.originalWord)},")
            writer.write("${escapeCsv(word.chineseTranslation)},")
            writer.write("${escapeCsv(word.exampleSentence)}\n")
        }

        return writer.toString()
    }

    /**
     * 将CSV字符串转换为单词列表（仅基础信息）
     */
    fun csvToWords(csv: String): List<Word> {
        if (csv.isBlank()) return emptyList()

        val words = mutableListOf<Word>()
        val lines = csv.lines()

        // 跳过表头
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            val fields = parseCsvLine(line)
            if (fields.size >= 4) {
                words.add(
                    Word(
                        id = fields[0],
                        languageCode = fields[1],
                        originalWord = fields[2],
                        chineseTranslation = fields[3],
                        exampleSentence = fields.getOrNull(4) ?: ""
                    )
                )
            }
        }

        return words
    }

    /**
     * 将学习进度转换为CSV
     * 格式：id,stage,nextReviewTime,correctStreak,lastReviewTime
     */
    fun progressToCsv(words: List<Word>): String {
        val writer = StringWriter()
        writer.write("id,stage,nextReviewTime,correctStreak,lastReviewTime\n")

        words.forEach { word ->
            writer.write("${escapeCsv(word.id)},")
            writer.write("${word.stage},")
            writer.write("${word.nextReviewTime},")
            writer.write("${word.correctStreak},")
            writer.write("${word.lastReviewTime}\n")
        }

        return writer.toString()
    }

    /**
     * 将进度CSV转换为Map
     */
    fun csvToProgressMap(csv: String): Map<String, WordProgress> {
        if (csv.isBlank()) return emptyMap()

        val progressMap = mutableMapOf<String, WordProgress>()
        val lines = csv.lines()

        // 跳过表头
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            val fields = parseCsvLine(line)
            if (fields.size >= 5) {
                progressMap[fields[0]] = WordProgress(
                    stage = fields[1].toIntOrNull() ?: 0,
                    nextReviewTime = fields[2].toLongOrNull() ?: 0L,
                    correctStreak = fields[3].toIntOrNull() ?: 0,
                    lastReviewTime = fields[4].toLongOrNull() ?: 0L
                )
            }
        }

        return progressMap
    }

    /**
     * 将学习记录转换为CSV
     */
    fun recordsToCsv(records: List<LearningRecord>): String {
        val writer = StringWriter()
        writer.write("id,languageCode,wordId,reviewTime,isCorrect,stageBefore,stageAfter\n")

        records.forEach { record ->
            writer.write("${escapeCsv(record.id)},")
            writer.write("${escapeCsv(record.languageCode)},")
            writer.write("${escapeCsv(record.wordId)},")
            writer.write("${record.reviewTime},")
            writer.write("${record.isCorrect},")
            writer.write("${record.stageBefore},")
            writer.write("${record.stageAfter}\n")
        }

        return writer.toString()
    }

    /**
     * 将CSV转换为学习记录
     */
    fun csvToRecords(csv: String): List<LearningRecord> {
        if (csv.isBlank()) return emptyList()

        val records = mutableListOf<LearningRecord>()
        val lines = csv.lines()

        // 跳过表头
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            val fields = parseCsvLine(line)
            if (fields.size >= 7) {
                records.add(
                    LearningRecord(
                        id = fields[0],
                        languageCode = fields[1],
                        wordId = fields[2],
                        reviewTime = fields[3].toLongOrNull() ?: 0L,
                        isCorrect = fields[4].toBooleanStrictOrNull() ?: false,
                        stageBefore = fields[5].toIntOrNull() ?: 0,
                        stageAfter = fields[6].toIntOrNull() ?: 0
                    )
                )
            }
        }

        return records
    }

    /**
     * 将同步元信息转换为CSV
     */
    fun syncMetaToCsv(meta: SyncMeta): String {
        val writer = StringWriter()
        writer.write("id,lastSyncTimestamp,lastSyncDeviceId,currentVersion,conflictCount\n")
        writer.write("${meta.id},")
        writer.write("${meta.lastSyncTimestamp},")
        writer.write("${escapeCsv(meta.lastSyncDeviceId)},")
        writer.write("${escapeCsv(meta.currentVersion)},")
        writer.write("${meta.conflictCount}\n")
        return writer.toString()
    }

    /**
     * 将CSV转换为同步元信息
     */
    fun csvToSyncMeta(csv: String): SyncMeta? {
        if (csv.isBlank()) return null

        val lines = csv.lines()
        if (lines.size < 2) return null

        val fields = parseCsvLine(lines[1])
        if (fields.size >= 5) {
            return SyncMeta(
                id = fields[0].toIntOrNull() ?: 1,
                lastSyncTimestamp = fields[1].toLongOrNull() ?: 0L,
                lastSyncDeviceId = fields[2],
                currentVersion = fields[3],
                conflictCount = fields[4].toIntOrNull() ?: 0
            )
        }
        return null
    }

    /**
     * 转义CSV特殊字符
     */
    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * 解析CSV行
     */
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> {
                    inQuotes = !inQuotes
                    current.append(char)
                }
                char == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        fields.add(current.toString())

        return fields.map { it.trim() }
    }

    /**
     * 进度数据类
     */
    data class WordProgress(
        val stage: Int,
        val nextReviewTime: Long,
        val correctStreak: Int,
        val lastReviewTime: Long
    )
}
