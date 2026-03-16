package org.feichao.wordking.util

/**
 * 版本对比工具
 * 对应需求文档中的 compareVersion() 函数
 */
object VersionUtils {

    /**
     * 比较版本号
     * @param version1 当前版本
     * @param version2 线上版本
     * @return 1=线上新, 0=相同, -1=当前新
     */
    fun compareVersion(version1: String, version2: String): Int {
        val v1 = removePrefix(version1).split(".")
        val v2 = removePrefix(version2).split(".")

        val maxLen = maxOf(v1.size, v2.size)

        for (i in 0 until maxLen) {
            val num1 = if (i < v1.size) v1[i].toIntOrNull() ?: 0 else 0
            val num2 = if (i < v2.size) v2[i].toIntOrNull() ?: 0 else 0

            when {
                num2 > num1 -> return 1   // 线上版本更新
                num2 < num1 -> return -1  // 当前版本更新
            }
        }

        return 0  // 版本相同
    }

    /**
     * 移除版本前缀（v或V）
     */
    private fun removePrefix(version: String): String {
        return version.removePrefix("v").removePrefix("V")
    }

    /**
     * 格式化版本号（确保有v前缀）
     */
    fun formatVersion(version: String): String {
        return if (version.startsWith("v") || version.startsWith("V")) {
            version
        } else {
            "v$version"
        }
    }

    /**
     * 检查版本是否有效
     */
    fun isValidVersion(version: String): Boolean {
        val pattern = Regex("^v?\\d+(\\.\\d+)*$")
        return pattern.matches(version)
    }
}
