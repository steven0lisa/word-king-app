package org.feichao.wordking.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.feichao.wordking.WordKingApplication
import org.feichao.wordking.databinding.ActivityGitConfigBinding
import org.feichao.wordking.service.GitSyncService
import org.feichao.wordking.util.Constants
import org.feichao.wordking.util.EncryptUtils
import java.io.File
import java.util.Properties

class GitConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGitConfigBinding
    private lateinit var gitSyncService: GitSyncService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGitConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gitSyncService = GitSyncService(this)

        loadEnvConfig()
        loadSavedConfig()
        setupButtons()
    }

    /**
     * 从.env文件加载默认配置
     */
    private fun loadEnvConfig() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val envFile = File(baseContext.filesDir.parentFile?.parentFile, ".env")
                if (envFile.exists()) {
                    val properties = Properties()
                    envFile.inputStream().use { properties.load(it) }

                    val syncUrl = properties.getProperty("SYNC_CLOUD_URL", "")
                    val syncBranch = properties.getProperty("SYNC_CLOUD_BRANCH", "master")
                    val syncKey = properties.getProperty("SYNC_CLOUD_KEY", "")

                    withContext(Dispatchers.Main) {
                        // 如果输入框为空，则填充.env中的默认值
                        if (binding.etRepoUrl.text.isNullOrEmpty() && syncUrl.isNotEmpty()) {
                            binding.etRepoUrl.setText(syncUrl)
                        }
                        if (binding.etBranch.text.isNullOrEmpty()) {
                            binding.etBranch.setText(syncBranch)
                        }
                        if (binding.etSshKey.text.isNullOrEmpty() && syncKey.isNotEmpty()) {
                            binding.etSshKey.setText(syncKey)
                        }
                    }
                }
            } catch (e: Exception) {
                // .env文件不存在或读取失败，使用默认值
            }
        }
    }

    private fun loadSavedConfig() {
        binding.etRepoUrl.setText(EncryptUtils.decryptGet(Constants.PrefsKeys.GIT_REPO_URL))
        binding.etSshKey.setText(EncryptUtils.decryptGet(Constants.PrefsKeys.GIT_SSH_KEY))
        binding.etBranch.setText(EncryptUtils.decryptGet(Constants.PrefsKeys.GIT_BRANCH).ifEmpty { "master" })
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            val repoUrl = binding.etRepoUrl.text.toString().trim()
            val token = binding.etSshKey.text.toString().trim()
            val branch = binding.etBranch.text.toString().trim().ifEmpty { "master" }

            if (repoUrl.isEmpty() || token.isEmpty()) {
                Toast.makeText(this, "请填写仓库地址和访问令牌", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            EncryptUtils.encryptSave(Constants.PrefsKeys.GIT_REPO_URL, repoUrl)
            EncryptUtils.encryptSave(Constants.PrefsKeys.GIT_SSH_KEY, token)
            EncryptUtils.encryptSave(Constants.PrefsKeys.GIT_BRANCH, branch)

            Toast.makeText(this, "Git配置已保存", Toast.LENGTH_SHORT).show()
        }

        binding.btnSync.setOnClickListener {
            syncNow()
        }
    }

    private fun syncNow() {
        binding.progress.visibility = android.view.View.VISIBLE
        binding.btnSync.isEnabled = false

        lifecycleScope.launch {
            val database = WordKingApplication.instance.database
            val words = database.wordDao().getAllWordsSync()
            val records = database.learningRecordDao().getAllRecords()
            val meta = database.syncMetaDao().getSyncMetaSync()
                ?: org.feichao.wordking.data.entity.SyncMeta()

            val result = gitSyncService.fullSync(words, records, meta)

            binding.progress.visibility = android.view.View.GONE
            binding.btnSync.isEnabled = true

            result.onSuccess {
                Toast.makeText(this@GitConfigActivity, "同步成功", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(this@GitConfigActivity, "同步失败：${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
