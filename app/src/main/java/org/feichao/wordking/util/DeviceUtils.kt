package org.feichao.wordking.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * 设备工具类
 */
object DeviceUtils {

    /**
     * 获取设备ID（Android ID）
     */
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    /**
     * 获取设备架构
     */
    fun getCpuAbi(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS.firstOrNull { it in Constants.SUPPORT_CPU_ABI } ?: "unknown"
        } else {
            @Suppress("DEPRECATION")
            Build.CPU_ABI
        }
    }

    /**
     * 获取设备制造商
     */
    fun getManufacturer(): String {
        return Build.MANUFACTURER
    }

    /**
     * 获取设备型号
     */
    fun getModel(): String {
        return Build.MODEL
    }

    /**
     * 获取Android版本
     */
    fun getAndroidVersion(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }

    /**
     * 获取应用版本号
     */
    fun getAppVersionCode(context: Context): Long {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)).longVersionCode
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
            }
        } catch (e: Exception) {
            1L
        }
    }

    /**
     * 获取应用版本名称
     */
    fun getAppVersionName(context: Context): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)).versionName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }
        } catch (e: Exception) {
            "1.0.0"
        } ?: "1.0.0"
    }

    /**
     * 检查是否支持振动
     */
    fun hasVibrator(context: Context): Boolean {
        return context.getSystemService(Context.VIBRATOR_SERVICE) != null
    }

    /**
     * 读取SSH密钥文件内容
     */
    fun readSshKeyFile(path: String): String? {
        return try {
            val file = File(path.expandTilde())
            if (file.exists() && file.canRead()) {
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 展开 ~ 为用户目录
     */
    private fun String.expandTilde(): String {
        return if (startsWith("~")) {
            val homeDir = System.getProperty("user.home")
            replaceFirst("~", homeDir)
        } else {
            this
        }
    }
}
