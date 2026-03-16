package org.feichao.wordking.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.feichao.wordking.util.Constants
import org.feichao.wordking.util.DeviceUtils
import org.feichao.wordking.util.NetworkUtils
import org.feichao.wordking.util.VersionUtils
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class UpdateService(private val context: Context) {

    private val gson = Gson()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun checkUpdate(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                return@withContext Result.failure(Exception("无网络连接"))
            }

            val currentVersion = DeviceUtils.getAppVersionName(context)

            val request = Request.Builder()
                .url(Constants.GitHub.RELEASES_API)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("检查更新失败: ${response.code}"))
            }

            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("响应为空"))

            val release = gson.fromJson(body, GithubRelease::class.java)

            if (release.isPreRelease == true) {
                return@withContext Result.success(null)
            }

            val compareResult = VersionUtils.compareVersion(currentVersion, release.tagName ?: "")

            if (compareResult <= 0) {
                return@withContext Result.success(null)
            }

            val deviceAbi = DeviceUtils.getCpuAbi()
            val targetAsset = release.assets?.find { asset ->
                (deviceAbi == "arm64-v8a" && asset.name?.contains("arm64") == true)
                        || (deviceAbi == "x86_64" && asset.name?.contains("x86_64") == true)
                        || (deviceAbi == "arm64-v8a" && asset.name?.contains("-aarch64") == true)
            }

            Result.success(
                UpdateInfo(
                    tagName = release.tagName ?: "",
                    body = release.body ?: "",
                    isPreRelease = release.isPreRelease ?: false,
                    asset = targetAsset
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadApk(updateInfo: UpdateInfo, onProgress: (Int) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        try {
            val asset = updateInfo.asset
                ?: return@withContext Result.failure(Exception("无适配当前架构的APK"))

            val downloadUrl = asset.downloadUrl
                ?: return@withContext Result.failure(Exception("下载链接无效"))

            val downloadDir = File(context.filesDir, Constants.PathConfig.APK_DOWNLOAD_DIR)
            downloadDir.mkdirs()

            val apkFile = File(downloadDir, "word-king-${updateInfo.tagName}.apk")

            val request = Request.Builder()
                .url(downloadUrl)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("下载失败: ${response.code}"))
            }

            val body = response.body
                ?: return@withContext Result.failure(Exception("下载内容为空"))

            val contentLength = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        if (contentLength > 0) {
                            val progress = ((downloadedBytes * 100) / contentLength).toInt()
                            onProgress(progress)
                        }
                    }
                }
            }

            Result.success(apkFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun installApk(apkFile: File) {
        val intent = Intent(Intent.ACTION_VIEW)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
        } else {
            val uri = Uri.fromFile(apkFile)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun checkInstallPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            intent.data = Uri.parse("package:${context.packageName}")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    data class UpdateInfo(
        val tagName: String,
        val body: String,
        val isPreRelease: Boolean,
        val asset: ReleaseAsset?
    )

    data class GithubRelease(
        val tagName: String?,
        val body: String?,
        val isPreRelease: Boolean?,
        val assets: List<ReleaseAsset>?
    )

    data class ReleaseAsset(
        val name: String?,
        val downloadUrl: String?
    )
}
