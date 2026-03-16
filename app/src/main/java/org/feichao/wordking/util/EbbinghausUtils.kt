package org.feichao.wordking.util

/**
 * 艾宾浩斯遗忘曲线复习工具
 * 对应需求文档中的复习时间计算函数
 */
object EbbinghausUtils {

    /**
     * 计算下次复习时间
     * @param currentStage 当前阶段（1-12）
     * @return 下次复习的时间戳
     */
    fun calculateNextReviewTime(currentStage: Int): Long {
        val interval = Constants.REVIEW_INTERVAL[currentStage] ?: 0L
        return System.currentTimeMillis() + interval
    }

    /**
     * 计算更新后的阶段
     * @param currentStage 当前阶段
     * @param isCorrect 是否答对
     * @return 新阶段
     */
    fun calculateNewStage(currentStage: Int, isCorrect: Boolean): Int {
        return if (isCorrect) {
            // 答对：阶段+1，最高12
            minOf(currentStage + 1, 12)
        } else {
            // 答错：阶段-2，最低1
            maxOf(currentStage - 2, 1)
        }
    }

    /**
     * 获取阶段名称
     */
    fun getStageName(stage: Int): String {
        return when (stage) {
            0 -> "未学"
            12 -> "已掌握"
            else -> "第${stage}阶段"
        }
    }

    /**
     * 获取阶段描述
     */
    fun getStageDescription(stage: Int): String {
        return when (stage) {
            0 -> "尚未开始学习"
            1 -> "初次记忆"
            2 -> "短期记忆"
            3 -> "初步巩固"
            4 -> "持续巩固"
            5 -> "中期记忆"
            6 -> "强化记忆"
            7 -> "深度记忆"
            8 -> "稳定记忆"
            9 -> "长期记忆"
            10 -> "强化长期"
            11 -> "接近永久"
            12 -> "永久掌握"
            else -> "未知阶段"
        }
    }

    /**
     * 获取下次复习的时间间隔描述
     */
    fun getIntervalDescription(stage: Int): String {
        val interval = Constants.REVIEW_INTERVAL[stage] ?: return "未知"
        return formatInterval(interval)
    }

    /**
     * 格式化时间间隔
     */
    private fun formatInterval(millis: Long): String {
        val minutes = millis / (60 * 1000)
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "${days}天"
            hours > 0 -> "${hours}小时"
            minutes > 0 -> "${minutes}分钟"
            else -> "刚刚"
        }
    }

    /**
     * 检查是否需要复习
     */
    fun needsReview(nextReviewTime: Long): Boolean {
        return System.currentTimeMillis() >= nextReviewTime
    }
}
