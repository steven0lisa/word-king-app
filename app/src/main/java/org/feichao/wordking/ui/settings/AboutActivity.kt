package org.feichao.wordking.ui.settings

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import org.feichao.wordking.R
import org.feichao.wordking.service.UpdateService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 关于页面
 */
class AboutActivity : AppCompatActivity() {

    private lateinit var updateService: UpdateService
    private lateinit var tvUpdateStatus: TextView
    private var currentVersion = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // 设置标题
        title = "关于"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        updateService = UpdateService(this)
        tvUpdateStatus = findViewById(R.id.tv_update_status)

        // 获取构建时间（从 APK 或使用编译时时间戳）
        val buildTime = getBuildTime()
        currentVersion = getVersionName()

        findViewById<TextView>(R.id.tv_version).text = "v$currentVersion"
        findViewById<TextView>(R.id.tv_build_time).text = "构建时间: $buildTime"

        // GitHub 仓库点击事件
        findViewById<MaterialCardView>(R.id.card_github).setOnClickListener {
            openGitHubRepo()
        }

        // 检查更新点击事件
        findViewById<MaterialCardView>(R.id.card_check_update).setOnClickListener {
            checkForUpdate()
        }
    }

    /**
     * 检查更新
     */
    private fun checkForUpdate() {
        // 检查权限
        if (!updateService.checkInstallPermission()) {
            AlertDialog.Builder(this)
                .setTitle("需要安装权限")
                .setMessage("为了安装更新，需要授予安装未知应用的权限")
                .setPositiveButton("去设置") { _, _ ->
                    updateService.openInstallPermissionSettings()
                }
                .setNegativeButton("取消", null)
                .show()
            return
        }

        val progressDialog = ProgressDialog(this).apply {
            setMessage("正在检查更新...")
            setCancelable(false)
            show()
        }

        lifecycleScope.launch {
            val result = updateService.checkUpdate()
            progressDialog.dismiss()

            result.onSuccess { updateInfo ->
                if (updateInfo == null) {
                    tvUpdateStatus.text = "已是最新版本"
                    Toast.makeText(this@AboutActivity, "已是最新版本", Toast.LENGTH_SHORT).show()
                } else {
                    showUpdateDialog(updateInfo)
                }
            }.onFailure { e ->
                tvUpdateStatus.text = "检查失败，请重试"
                Toast.makeText(this@AboutActivity, "检查更新失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 显示更新对话框
     */
    private fun showUpdateDialog(updateInfo: UpdateService.UpdateInfo) {
        val message = StringBuilder()
            .appendLine("发现新版本: ${updateInfo.tagName}")
            .appendLine()

        // 解析更新说明（过滤掉不需要的行）
        val notes = updateInfo.body
            .lines()
            .filterNot { it.startsWith("## Full Changelog") }
            .filterNot { it.startsWith("**Full Changelog**") }
            .filter { it.isNotBlank() }
            .joinToString("\n")

        message.append(notes)

        AlertDialog.Builder(this)
            .setTitle("发现新版本")
            .setMessage(message.toString())
            .setPositiveButton("立即更新") { _, _ ->
                downloadAndInstall(updateInfo)
            }
            .setNegativeButton("稍后提醒", null)
            .show()
    }

    /**
     * 下载并安装更新
     */
    private fun downloadAndInstall(updateInfo: UpdateService.UpdateInfo) {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("正在下载更新...")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            setCancelable(false)
            max = 100
            show()
        }

        lifecycleScope.launch {
            val result = updateService.downloadApk(updateInfo) { progress ->
                progressDialog.progress = progress
            }

            progressDialog.dismiss()

            result.onSuccess { apkFile ->
                tvUpdateStatus.text = "下载完成，正在安装"
                Toast.makeText(this@AboutActivity, "下载完成，正在安装", Toast.LENGTH_SHORT).show()
                updateService.installApk(apkFile)
            }.onFailure { e ->
                tvUpdateStatus.text = "下载失败，请重试"
                Toast.makeText(this@AboutActivity, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 获取构建时间
     * 尝试从 APK 获取，如果失败则返回默认值
     */
    private fun getBuildTime(): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            // 使用 APK 安装时间作为构建时间的近似值
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            sdf.format(Date(packageInfo.lastUpdateTime))
        } catch (e: Exception) {
            "未知"
        }
    }

    /**
     * 获取版本号
     */
    private fun getVersionName(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            "1.0.1"
        }
    }

    /**
     * 打开 GitHub 仓库
     */
    private fun openGitHubRepo() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/steven0lisa/word-king-app/"))
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
