package org.feichao.wordking.service

import android.content.Context
import android.util.Log
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.feichao.wordking.data.entity.Word
import org.feichao.wordking.util.Constants
import org.feichao.wordking.util.EncryptUtils
import org.feichao.wordking.util.IdGenerator
import java.util.concurrent.TimeUnit

/**
 * AI生成单词服务
 */
class AiGenerateService(private val context: Context) {

    // 使用 LOWER_CASE_WITH_UNDERSCORES 将 JSON 的 snake_case 映射到 Kotlin 的 camelCase
    private val gson: Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "AiGenerateService"
    }

    /**
     * 生成单词
     */
    suspend fun generateWords(
        languageCode: String,
        count: Int,
        type: String = "MANUAL"
    ): Result<List<Word>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始生成单词: language=$languageCode, count=$count")

            // 获取配置
            val apiUrl = EncryptUtils.decryptGet(Constants.PrefsKeys.AI_API_URL)
            val apiKey = EncryptUtils.decryptGet(Constants.PrefsKeys.AI_API_KEY)
            val modelId = EncryptUtils.decryptGet(Constants.PrefsKeys.AI_MODEL_ID)

            Log.d(TAG, "API配置: url=$apiUrl, model=$modelId, keyLen=${apiKey.length}")

            if (apiUrl.isEmpty() || apiKey.isEmpty() || modelId.isEmpty()) {
                Log.e(TAG, "AI配置未完成")
                return@withContext Result.failure(Exception("请先在设置中配置AI信息"))
            }

            // 构建请求
            val languageName = Constants.LANGUAGE_NAMES[languageCode] ?: languageCode
            val prompt = buildPrompt(languageName, count)

            Log.d(TAG, "发送请求...")
            Log.d(TAG, "prompt: $prompt")

            // 使用 Gson 来构建 JSON，避免特殊字符问题
            val requestMap = mapOf(
                "model" to modelId,
                "messages" to listOf(mapOf("role" to "user", "content" to prompt))
            )
            val requestJson = gson.toJson(requestMap)
            Log.d(TAG, "requestJson: $requestJson")

            val mediaType = "application/json".toMediaType()
            val requestBody = requestJson.toRequestBody(mediaType)

            // 打印完整请求信息
            Log.d(TAG, "=== 完整请求信息 ===")
            Log.d(TAG, "URL: $apiUrl")
            Log.d(TAG, "Headers: Authorization=Bearer ${apiKey.take(10)}..., Content-Type=application/json")
            Log.d(TAG, "Body: $requestJson")
            Log.d(TAG, "====================")

            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            var response = okHttpClient.newCall(request).execute()

            // 如果是429错误，重试3次
            var retryCount = 0
            while (response.code == 429 && retryCount < 3) {
                retryCount++
                Log.w(TAG, "触发限流，${retryCount}秒后重试...")
                kotlinx.coroutines.delay(retryCount * 2000L) // 等待2/4/6秒
                response = okHttpClient.newCall(request).execute()
            }

            Log.d(TAG, "响应: ${response.code}")

            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                Log.e(TAG, "API错误: ${response.code}, body: $errorBody")
                return@withContext Result.failure(Exception("API调用失败: ${response.code}\n$errorBody"))
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("响应为空"))

            Log.d(TAG, "响应内容长度: ${responseBody.length}")
            Log.d(TAG, "响应内容前500字符: ${responseBody.take(500)}")
            Log.d(TAG, "响应内容后500字符: ${responseBody.takeLast(500)}")

            // 解析响应
            val words = parseWordsFromResponse(responseBody, languageCode)
            Log.d(TAG, "解析到 ${words.size} 个单词")

            Result.success(words)
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "请求超时: ${e.message}")
            Result.failure(Exception("网络超时，请检查手机网络是否正常访问API"))
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "无法解析域名: ${e.message}")
            Result.failure(Exception("无法访问API，请检查网络或API地址是否正确"))
        } catch (e: Exception) {
            Log.e(TAG, "生成失败: ${e.message}", e)
            Result.failure(e)
        }
    }

    // 单词领域分类
    private val WORD_TOPICS = listOf(
        "衣服服饰" to "如T恤、裤子、裙子、鞋子、帽子、外套等",
        "食物餐饮" to "如米饭、面条、面包、水果、蔬菜、肉类、饮料等",
        "居住环境" to "如房子、房间、门、窗、床、桌子、椅子等",
        "交通出行" to "如汽车、公交车、地铁、飞机、船、自行车等",
        "情绪情感" to "如开心、悲伤、生气、害怕、喜欢、讨厌等",
        "日常物品" to "如手机、电脑、书、笔、钱、钥匙等",
        "形容词" to "如大、小、新、旧、好、坏、漂亮、丑等",
        "时间日期" to "如今天、明天、昨天、早上、中午、晚上等",
        "数字数量" to "如一、二、三、十、百、千、万等",
        "颜色外观" to "如红、蓝、绿、黄、黑、白、紫等",
        "动物世界" to "如狗、猫、鱼、鸟、马、牛、猪等",
        "家庭成员" to "如爸爸、妈妈、哥哥、姐姐、弟弟、妹妹等",
        "学校学习" to "如老师、学生、书本、教室、作业、考试等",
        "工作职业" to "如医生、护士、警察、司机、厨师、工人等",
        "自然环境" to "如太阳、月亮、星星、雨、雪、风、山、海等",
        "身体部位" to "如头、手、脚、眼睛、鼻子、耳朵、嘴巴等",
        "社交礼貌" to "如你好、再见、谢谢、对不起、请、抱歉等",
        "方位方向" to "如上、下、左、右、前、后、里面、外面等"
    )

    private fun buildPrompt(languageName: String, count: Int): String {
        // 随机选择一个领域
        val randomTopic = WORD_TOPICS.random()
        return """
            生成${count}个${languageName}日常高频单词，主题领域：${randomTopic.first}（${randomTopic.second}）
            直接输出JSON数组，不要其他内容。格式： [{"original_word":"原词","chinese_translation":"中文翻译"}]
        """.trimIndent()
    }

    private fun parseWordsFromResponse(content: String, languageCode: String): List<Word> {
        try {
            // 解析chat completion响应格式
            val responseObj = gson.fromJson(content, ChatCompletionResponse::class.java)
            val assistantMessage = responseObj.choices?.firstOrNull()?.message?.content
            if (assistantMessage == null) {
                Log.e(TAG, "未找到assistant消息")
                return emptyList()
            }
            Log.d(TAG, "AI回复内容: ${assistantMessage.take(200)}")

            // 从AI回复中提取JSON数组
            val jsonArray = extractJsonArray(assistantMessage)
            if (jsonArray == null) {
                Log.e(TAG, "未找到JSON数组，内容: ${assistantMessage.take(200)}")
                return emptyList()
            }
            Log.d(TAG, "提取的JSON: ${jsonArray.take(300)}")

            val type = object : TypeToken<List<GeneratedWord>>() {}.type
            val generatedWords: List<GeneratedWord> = try {
                gson.fromJson(jsonArray, type)
            } catch (e: Exception) {
                Log.e(TAG, "解析JSON失败: $e, JSON: $jsonArray")
                return emptyList()
            }

            Log.d(TAG, "解析到原始数据: $generatedWords")

            // 过滤掉null值
            return generatedWords
                .filter { !it.original_word.isNullOrBlank() && !it.chinese_translation.isNullOrBlank() }
                .map {
                    Word(
                        id = IdGenerator.generateGlobalId(),
                        languageCode = languageCode,
                        originalWord = it.original_word ?: "",
                        chineseTranslation = it.chinese_translation ?: "",
                        stage = 0,
                        createTime = System.currentTimeMillis()
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "解析响应失败: $e")
            return emptyList()
        }
    }

    private fun extractJsonArray(content: String): String? {
        // 方法1: 尝试从 markdown code block 中提取
        // 查找 ```json ... ``` 格式
        val codeBlockPattern = Regex("""```json\s*(\[[\s\S]*?\])\s*```""")
        codeBlockPattern.find(content)?.let { match ->
            return match.groupValues[1]
        }

        // 方法2: 查找普通的JSON数组
        val startIndex = content.indexOf('[')
        val endIndex = content.lastIndexOf(']')
        return if (startIndex >= 0 && endIndex > startIndex) {
            content.substring(startIndex, endIndex + 1)
        } else {
            null
        }
    }

    /**
     * 解析JSON的辅助类（字段设为可空以处理异常数据）
     */
    private data class GeneratedWord(
        val original_word: String? = null,
        val chinese_translation: String? = null
    )

    /**
     * ChatGPT API 响应结构
     */
    private data class ChatCompletionResponse(
        val id: String? = null,
        val `object`: String? = null,
        val created: Long? = null,
        val model: String? = null,
        val choices: List<Choice>? = null,
        val usage: Usage? = null
    )

    private data class Choice(
        val index: Int? = null,
        val message: Message? = null,
        val finish_reason: String? = null
    )

    private data class Message(
        val role: String? = null,
        val content: String? = null
    )

    private data class Usage(
        val prompt_tokens: Int? = null,
        val completion_tokens: Int? = null,
        val total_tokens: Int? = null
    )
}
