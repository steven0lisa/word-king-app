package org.feichao.wordking.util

/**
 * 常量定义
 * 对应需求文档中的常量定义
 */
object Constants {

    // 支持的语言
    val SUPPORT_LANGUAGES = listOf("en", "id", "th", "ko", "ja", "es", "pt", "fr")

    val LANGUAGE_NAMES = mapOf(
        "en" to "英语",
        "id" to "印尼语",
        "th" to "泰语",
        "ko" to "韩语",
        "ja" to "日语",
        "es" to "西班牙语",
        "pt" to "葡萄牙语",
        "fr" to "法语"
    )

    // 支持的CPU架构
    val SUPPORT_CPU_ABI = listOf("arm64-v8a", "x86_64")

    // 艾宾浩斯复习间隔（毫秒）
    val REVIEW_INTERVAL = mapOf(
        1 to 5 * 60 * 1000L,           // 5分钟
        2 to 30 * 60 * 1000L,          // 30分钟
        3 to 12 * 3600 * 1000L,        // 12小时
        4 to 24 * 3600 * 1000L,        // 1天
        5 to 2 * 24 * 3600 * 1000L,    // 2天
        6 to 4 * 24 * 3600 * 1000L,    // 4天
        7 to 7 * 24 * 3600 * 1000L,    // 7天
        8 to 15 * 24 * 3600 * 1000L,   // 15天
        9 to 30 * 24 * 3600 * 1000L,   // 30天
        10 to 90 * 24 * 3600 * 1000L,  // 90天
        11 to 180 * 24 * 3600 * 1000L, // 180天
        12 to 365 * 24 * 3600 * 1000L  // 365天
    )

    // Git同步文件清单
    object SyncFiles {
        const val WORD_BASE = "word_base.csv"
        const val WORD_PROGRESS = "word_progress.csv"
        const val LEARNING_RECORDS = "learning_records.csv"
        const val SYNC_META = "sync_meta.csv"
    }

    // Git冲突规则
    object ConflictRules {
        const val WORD_BASE = "远程覆盖本地（仅新增本地独有单词）"
        const val WORD_PROGRESS = "按lastReviewTime最新为准"
        const val LEARNING_RECORDS = "合并去重（wordId+reviewTime+isCorrect）"
    }

    // AI生成配置
    object AiConfig {
        const val AUTO_GENERATE_THRESHOLD = 50   // 未学习单词<50自动生成
        const val MANUAL_GENERATE_MAX = 50       // 单次手动生成上限
        const val AUTO_GENERATE_DAILY_MAX = 100  // 每日自动生成上限
        const val DEFAULT_AUTO_GENERATE_COUNT = 30
    }

    // 路径常量
    object PathConfig {
        const val GIT_LOCAL_DIR = "word-king-git"
        const val SYNC_LOCAL_DIR = "word-king-sync"
        const val BACKUP_DIR = "backup"
        const val OFFLINE_LOG = "offline_log.csv"
        const val APK_DOWNLOAD_DIR = "Download"
    }

    // SharedPreferences 键名
    object PrefsKeys {
        const val GIT_REPO_URL = "git_repo_url"
        const val GIT_SSH_KEY = "git_ssh_key"
        const val GIT_BRANCH = "git_branch"
        const val AI_API_URL = "ai_api_url"
        const val AI_API_KEY = "ai_api_key"
        const val AI_MODEL_ID = "ai_model_id"
        const val FIRST_LAUNCH = "first_launch"
    }

    // GitHub
    object GitHub {
        const val REPO_OWNER = "steven0lisa"
        const val REPO_NAME = "word-king-app"
        const val RELEASES_API = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
    }

    // 振动时长（毫秒）
    object Vibrate {
        const val CORRECT = 100L
        const val INCORRECT = 500L
    }

    // 默认值
    object Defaults {
        const val DAILY_NEW_WORD_LIMIT = 300
        const val DEFAULT_LANGUAGE = "en"
    }
}
