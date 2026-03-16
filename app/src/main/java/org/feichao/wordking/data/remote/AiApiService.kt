package org.feichao.wordking.data.remote

import retrofit2.Response
import retrofit2.http.*

/**
 * AI API 服务接口
 * 用于调用智谱 GLM 模型
 */
interface AiApiService {

    @POST("")
    suspend fun generateWords(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: AiChatRequest
    ): Response<AiChatResponse>
}

/**
 * AI 聊天请求
 */
data class AiChatRequest(
    val model: String,
    val messages: List<AiMessage>,
    val temperature: Double = 0.7
)

/**
 * AI 消息
 */
data class AiMessage(
    val role: String,
    val content: String
)

/**
 * AI 聊天响应
 */
data class AiChatResponse(
    val id: String?,
    val model: String?,
    val choices: List<AiChoice>?,
    val usage: AiUsage?,
    val error: AiError?
)

/**
 * AI 选项
 */
data class AiChoice(
    val index: Int?,
    val message: AiMessage?,
    val finish_reason: String?
)

/**
 * AI 使用统计
 */
data class AiUsage(
    val prompt_tokens: Int?,
    val completion_tokens: Int?,
    val total_tokens: Int?
)

/**
 * AI 错误
 */
data class AiError(
    val message: String?,
    val type: String?,
    val code: String?
)

/**
 * 单词生成结果
 */
data class GeneratedWord(
    val original_word: String,
    val chinese_translation: String
)
