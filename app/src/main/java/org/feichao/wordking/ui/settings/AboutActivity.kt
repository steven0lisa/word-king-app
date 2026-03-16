package org.feichao.wordking.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import org.feichao.wordking.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 关于页面
 */
class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // 设置标题
        title = "关于"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 获取构建时间（从 APK 或使用编译时时间戳）
        val buildTime = getBuildTime()
        val versionName = getVersionName()

        findViewById<TextView>(R.id.tv_version).text = "v$versionName"
        findViewById<TextView>(R.id.tv_build_time).text = "构建时间: $buildTime"

        // GitHub 仓库点击事件
        findViewById<MaterialCardView>(R.id.card_github).setOnClickListener {
            openGitHubRepo()
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
