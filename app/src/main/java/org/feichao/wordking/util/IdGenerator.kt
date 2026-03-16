package org.feichao.wordking.util

import java.util.UUID

/**
 * ID生成工具
 * 对应需求文档中的 generateGlobalId() 函数
 */
object IdGenerator {

    /**
     * 生成全局唯一ID
     * 格式：时间戳_4位随机数
     */
    fun generateGlobalId(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "${timestamp}_$random"
    }

    /**
     * 生成简单的UUID
     */
    fun generateUUID(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * 生成日志ID
     */
    fun generateLogId(): String {
        return "log_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}
