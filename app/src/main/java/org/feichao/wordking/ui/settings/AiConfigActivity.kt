package org.feichao.wordking.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.feichao.wordking.WordKingApplication
import org.feichao.wordking.data.repository.UserConfigRepository
import org.feichao.wordking.databinding.ActivityAiConfigBinding
import org.feichao.wordking.service.AiGenerateService
import org.feichao.wordking.util.Constants
import org.feichao.wordking.util.EncryptUtils

class AiConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiConfigBinding
    private lateinit var aiGenerateService: AiGenerateService
    private lateinit var userConfigRepository: UserConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = WordKingApplication.instance.database
        aiGenerateService = AiGenerateService(this)
        userConfigRepository = UserConfigRepository(database.userConfigDao())

        loadConfig()
        setupButtons()
    }

    private fun loadConfig() {
        val apiUrl = EncryptUtils.decryptGet(Constants.PrefsKeys.AI_API_URL)
        val apiKey = EncryptUtils.decryptGet(Constants.PrefsKeys.AI_API_KEY)
        val modelId = EncryptUtils.decryptGet(Constants.PrefsKeys.AI_MODEL_ID)

        binding.etApiUrl.setText(apiUrl)
        binding.etApiKey.setText(apiKey)
        binding.etModel.setText(modelId)

        // 显示配置状态
        val status = if (apiUrl.isNotEmpty() && apiKey.isNotEmpty() && modelId.isNotEmpty()) {
            "✅ 已配置 - Key: ${apiKey.take(8)}...${apiKey.takeLast(4)}"
        } else {
            "❌ 未配置 - 请填写完整信息"
        }
        Toast.makeText(this, status, Toast.LENGTH_LONG).show()
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            val apiUrl = binding.etApiUrl.text.toString().trim()
            val apiKey = binding.etApiKey.text.toString().trim()
            val model = binding.etModel.text.toString().trim()

            if (apiUrl.isEmpty() || apiKey.isEmpty() || model.isEmpty()) {
                Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            EncryptUtils.encryptSave(Constants.PrefsKeys.AI_API_URL, apiUrl)
            EncryptUtils.encryptSave(Constants.PrefsKeys.AI_API_KEY, apiKey)
            EncryptUtils.encryptSave(Constants.PrefsKeys.AI_MODEL_ID, model)

            Toast.makeText(this, "AI配置已保存", Toast.LENGTH_SHORT).show()
        }

        binding.btnGenerate.setOnClickListener {
            generateWords()
        }
    }

    private fun generateWords() {
        binding.progress.visibility = android.view.View.VISIBLE
        binding.btnGenerate.isEnabled = false

        lifecycleScope.launch {
            val config = userConfigRepository.getUserConfigSync()
            val language = config?.currentLanguage ?: "en"

            val result = aiGenerateService.generateWords(
                language,
                Constants.AiConfig.MANUAL_GENERATE_MAX,
                "MANUAL"
            )

            binding.progress.visibility = android.view.View.GONE
            binding.btnGenerate.isEnabled = true

            result.onSuccess { words ->
                if (words.isNotEmpty()) {
                    val database = WordKingApplication.instance.database
                    database.wordDao().insertWords(words)
                    Toast.makeText(
                        this@AiConfigActivity,
                        "生成成功！新增${words.size}个单词",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(this@AiConfigActivity, "没有新单词生成", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { e ->
                Toast.makeText(this@AiConfigActivity, "生成失败：${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
