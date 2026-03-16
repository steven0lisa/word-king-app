package org.feichao.wordking.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.transport.*
import org.feichao.wordking.data.entity.*
import org.feichao.wordking.util.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Git 同步服务
 * 支持 HTTPS + Token 认证、Clone、Fetch、Pull、Commit、Push
 */
class GitSyncService(private val context: Context) {

    companion object {
        private const val TAG = "GitSyncService"
        // Gitee HTTPS 认证用户名（Token 认证时可以使用任意用户名，某些服务需要特定用户名）
        private const val GIT_USERNAME = "oauth2"
    }

    private val gitDir: File by lazy {
        File(context.filesDir, Constants.PathConfig.GIT_LOCAL_DIR)
    }

    private val syncDir: File by lazy {
        File(context.filesDir, Constants.PathConfig.SYNC_LOCAL_DIR)
    }

    /**
     * 执行完整同步
     */
    suspend fun fullSync(
        words: List<Word>,
        records: List<LearningRecord>,
        syncMeta: SyncMeta
    ): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            val repoUrl = EncryptUtils.decryptGet(Constants.PrefsKeys.GIT_REPO_URL)
            val token = EncryptUtils.decryptGet(Constants.PrefsKeys.GIT_SSH_KEY)
            val branch = EncryptUtils.decryptGet(Constants.PrefsKeys.GIT_BRANCH).ifEmpty { "master" }

            if (repoUrl.isEmpty() || token.isEmpty()) {
                return@withContext Result.failure(Exception("Git配置未完成，请先配置仓库地址和访问令牌"))
            }

            if (!NetworkUtils.isNetworkAvailable(context)) {
                return@withContext Result.failure(Exception("无网络连接"))
            }

            // 备份本地数据
            backupLocalData(words, records, syncMeta)

            // 初始化或更新Git仓库
            val git = initOrOpenGit(repoUrl, token, branch)

            // 拉取远程更新
            pullRemoteChanges(git, branch, token)

            // 合并远程数据到本地
            mergeSyncFiles()

            // 导出本地同步文件
            exportLocalSyncFiles(words, records, syncMeta)

            // 提交并推送更改
            commitAndPush(git, branch, token)

            // 导入合并后的数据
            val (remoteWords, remoteRecords, remoteMeta) = importRemoteSyncFiles()

            Result.success(SyncResult(
                words = remoteWords ?: words,
                records = remoteRecords ?: records,
                syncMeta = remoteMeta ?: syncMeta
            ))

        } catch (e: Exception) {
            Log.e(TAG, "同步失败", e)
            Result.failure(e)
        }
    }

    /**
     * 初始化或打开Git仓库
     */
    private fun initOrOpenGit(repoUrl: String, token: String, branch: String): Git {
        return if (gitDir.exists() && File(gitDir, ".git").exists()) {
            // 已有仓库，直接打开
            Log.d(TAG, "打开已有Git仓库")
            Git.open(gitDir)
        } else {
            // 首次同步，克隆仓库
            Log.d(TAG, "首次同步，克隆仓库: $repoUrl")
            cloneRepository(repoUrl, token, branch)
        }
    }

    /**
     * 克隆远程仓库（HTTPS + Token 认证）
     */
    private fun cloneRepository(repoUrl: String, token: String, branch: String): Git {
        val credentialsProvider = createCredentialsProvider(token)

        return Git.cloneRepository()
            .setURI(repoUrl)
            .setDirectory(gitDir)
            .setBranch(branch)
            .setCredentialsProvider(credentialsProvider)
            .setCloneAllBranches(false)
            .call()

        Log.d(TAG, "克隆成功")
    }

    /**
     * 拉取远程更新（HTTPS + Token 认证）
     */
    private fun pullRemoteChanges(git: Git, branch: String, token: String) {
        val credentialsProvider = createCredentialsProvider(token)

        // 先 Fetch 获取远程更新
        try {
            git.fetch()
                .setCredentialsProvider(credentialsProvider)
                .call()
            Log.d(TAG, "Fetch 成功")
        } catch (e: Exception) {
            Log.w(TAG, "Fetch 失败: ${e.message}")
        }

        // 然后 Pull 合并
        try {
            git.pull()
                .setCredentialsProvider(credentialsProvider)
                .setRemoteBranchName(branch)
                .call()
            Log.d(TAG, "Pull 成功")
        } catch (e: Exception) {
            Log.w(TAG, "Pull 失败，可能没有远程更新: ${e.message}")
        }
    }

    /**
     * 创建 HTTPS 认证凭证
     * 使用 Token 作为密码
     */
    private fun createCredentialsProvider(token: String): UsernamePasswordCredentialsProvider {
        // 对于 Gitee/GitHub 等，用户名可以是任意值（如 "oauth2" 或 "token"）
        // 重要的是 Token 作为密码
        return UsernamePasswordCredentialsProvider(GIT_USERNAME, token)
    }

    /**
     * 提交并推送更改（HTTPS + Token 认证）
     */
    private fun commitAndPush(git: Git, branch: String, token: String) {
        val credentialsProvider = createCredentialsProvider(token)

        val syncFilesDir = File(gitDir, "word-king")
        if (!syncFilesDir.exists()) {
            syncFilesDir.mkdirs()
        }

        // 复制同步文件到Git目录
        syncDir.listFiles()?.forEach { file ->
            file.copyTo(File(syncFilesDir, file.name), overwrite = true)
        }

        // 添加所有文件
        git.add()
            .addFilepattern("word-king/")
            .call()

        // 检查是否有更改需要提交
        val status = git.status().call()
        val hasChanges = status.modified.isNotEmpty() ||
                        status.added.isNotEmpty() ||
                        status.changed.isNotEmpty() ||
                        status.untracked.isNotEmpty()

        if (hasChanges) {
            // 提交更改
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val commitMessage = "Sync: ${dateFormat.format(Date())}"

            git.commit()
                .setMessage(commitMessage)
                .call()

            Log.d(TAG, "提交成功，准备推送")

            // 推送更改
            try {
                git.push()
                    .setCredentialsProvider(credentialsProvider)
                    .call()
                Log.d(TAG, "推送成功")
            } catch (e: Exception) {
                Log.w(TAG, "推送失败: ${e.message}")
                throw e
            }
        } else {
            Log.d(TAG, "没有需要提交的更改")
        }
    }

    private fun backupLocalData(
        words: List<Word>,
        records: List<LearningRecord>,
        syncMeta: SyncMeta
    ) {
        val backupDir = File(context.filesDir, "${Constants.PathConfig.BACKUP_DIR}/${System.currentTimeMillis()}")
        backupDir.mkdirs()
        File(backupDir, Constants.SyncFiles.WORD_BASE).writeText(CsvUtils.wordsToCsv(words))
        File(backupDir, Constants.SyncFiles.LEARNING_RECORDS).writeText(CsvUtils.recordsToCsv(records))
        File(backupDir, Constants.SyncFiles.SYNC_META).writeText(CsvUtils.syncMetaToCsv(syncMeta))
    }

    private fun exportLocalSyncFiles(words: List<Word>, records: List<LearningRecord>, syncMeta: SyncMeta) {
        syncDir.mkdirs()
        File(syncDir, Constants.SyncFiles.WORD_BASE).writeText(CsvUtils.wordsToCsv(words))
        File(syncDir, Constants.SyncFiles.WORD_PROGRESS).writeText(CsvUtils.progressToCsv(words))
        File(syncDir, Constants.SyncFiles.LEARNING_RECORDS).writeText(CsvUtils.recordsToCsv(records))
        File(syncDir, Constants.SyncFiles.SYNC_META).writeText(CsvUtils.syncMetaToCsv(syncMeta))
    }

    private fun mergeSyncFiles() {
        val remoteWordBase = File(gitDir, "word-king/${Constants.SyncFiles.WORD_BASE}")
        val remoteProgress = File(gitDir, "word-king/${Constants.SyncFiles.WORD_PROGRESS}")
        val remoteRecords = File(gitDir, "word-king/${Constants.SyncFiles.LEARNING_RECORDS}")

        if (remoteWordBase.exists()) mergeWordBase(remoteWordBase)
        if (remoteProgress.exists()) mergeWordProgress(remoteProgress)
        if (remoteRecords.exists()) mergeLearningRecords(remoteRecords)
    }

    private fun mergeWordBase(remoteFile: File) {
        val localFile = File(syncDir, Constants.SyncFiles.WORD_BASE)
        val localWords = if (localFile.exists()) CsvUtils.csvToWords(localFile.readText()) else emptyList()
        val remoteWords = CsvUtils.csvToWords(remoteFile.readText())
        val remoteIds = remoteWords.map { it.id }.toSet()
        val localUniqueWords = localWords.filter { it.id !in remoteIds }
        val mergedWords = remoteWords + localUniqueWords

        localFile.writeText(CsvUtils.wordsToCsv(mergedWords))
        val targetDir = File(gitDir, "word-king")
        targetDir.mkdirs()
        File(targetDir, Constants.SyncFiles.WORD_BASE).writeText(CsvUtils.wordsToCsv(mergedWords))
    }

    private fun mergeWordProgress(remoteFile: File) {
        val localFile = File(syncDir, Constants.SyncFiles.WORD_PROGRESS)
        val targetDir = File(gitDir, "word-king")
        val localProgress = if (localFile.exists()) CsvUtils.csvToProgressMap(localFile.readText()) else emptyMap()
        val remoteProgress = CsvUtils.csvToProgressMap(remoteFile.readText())
        val allIds = localProgress.keys + remoteProgress.keys
        val mergedProgress = allIds.associateWith { id ->
            val local = localProgress[id]
            val remote = remoteProgress[id]
            when {
                local == null -> remote!!
                remote == null -> local
                local.lastReviewTime > remote.lastReviewTime -> local
                else -> remote
            }
        }

        val mergedWords = mergedProgress.map { (id, progress) ->
            Word(id = id, languageCode = "", originalWord = "", chineseTranslation = "", stage = progress.stage, nextReviewTime = progress.nextReviewTime, correctStreak = progress.correctStreak, lastReviewTime = progress.lastReviewTime)
        }

        localFile.writeText(CsvUtils.progressToCsv(mergedWords))
        File(targetDir, Constants.SyncFiles.WORD_PROGRESS).writeText(CsvUtils.progressToCsv(mergedWords))
    }

    private fun mergeLearningRecords(remoteFile: File) {
        val localFile = File(syncDir, Constants.SyncFiles.LEARNING_RECORDS)
        val targetDir = File(gitDir, "word-king")
        val localRecords = if (localFile.exists()) CsvUtils.csvToRecords(localFile.readText()) else emptyList()
        val remoteRecords = CsvUtils.csvToRecords(remoteFile.readText())

        val seen = mutableSetOf<String>()
        val mergedRecords = mutableListOf<LearningRecord>()
        (localRecords + remoteRecords).forEach { record ->
            val key = "${record.wordId}_${record.reviewTime}_${record.isCorrect}"
            if (key !in seen) {
                seen.add(key)
                mergedRecords.add(record)
            }
        }

        localFile.writeText(CsvUtils.recordsToCsv(mergedRecords))
        File(targetDir, Constants.SyncFiles.LEARNING_RECORDS).writeText(CsvUtils.recordsToCsv(mergedRecords))
    }

    private fun importRemoteSyncFiles(): Triple<List<Word>?, List<LearningRecord>?, SyncMeta?> {
        val wordBase = File(syncDir, Constants.SyncFiles.WORD_BASE).takeIf { it.exists() }
        val records = File(syncDir, Constants.SyncFiles.LEARNING_RECORDS).takeIf { it.exists() }
        val meta = File(syncDir, Constants.SyncFiles.SYNC_META).takeIf { it.exists() }
        return Triple(
            wordBase?.let { CsvUtils.csvToWords(it.readText()) },
            records?.let { CsvUtils.csvToRecords(it.readText()) },
            meta?.let { CsvUtils.csvToSyncMeta(it.readText()) }
        )
    }

    data class SyncResult(
        val words: List<Word>,
        val records: List<LearningRecord>,
        val syncMeta: SyncMeta
    )
}
