# 词王(WordKing) Android APP 完整PRD（产品需求文档）
## 文档信息
| 项⽬                | 内容                                                                 |
|---------------------|----------------------------------------------------------------------|
| 产品名称            | 词王(WordKing)                                                       |
| 产品定位            | 多语言单词学习APP，基于艾宾浩斯遗忘曲线，支持Git同步、AI生成题库、自动更新 |
| 目标用户            | 多语言学习者（零基础/进阶），需跨设备同步学习进度的用户               |
| 核心价值            | 智能复习+多端同步+AI补库，低成本高效掌握多语言单词                     |
| 交付对象            | AI开发工具（执行伪代码生成可落地的安卓APP）                           |
| 技术栈约束          | 安卓原生（Kotlin）、Jetpack、Room、JGit、Retrofit、MPAndroidChart     |

## 一、核心功能清单
| 功能模块         | 子功能点                                                                 | 优先级 |
|------------------|--------------------------------------------------------------------------|--------|
| 基础能力         | 多语言切换、单词实体管理、应用初始化                                     | P0     |
| 艾宾浩斯复习     | 记忆阶段管理、复习时间计算、答题交互、振动反馈                           | P0     |
| Git同步          | 全题库/进度/记录同步、冲突处理、离线同步、本地备份                       | P0     |
| AI生成题库       | 自动/手动生成、词库余量提示、多语言适配、生成结果入库                   | P0     |
| 学习统计         | 多维度统计指标、GitLab风格密度图、单词状态筛选                           | P0     |
| 检查更新         | GitHub版本对比、架构匹配下载、APK安装                                    | P0     |
| 系统设置         | 敏感信息加密存储、每日学习上限配置、振动开关、权限管理                   | P0     |

## 二、全局规则（伪代码）
### 2.1 数据结构定义
```plaintext
// 核心实体
实体 Word {
    字段 id: 字符串（全局唯一，规则：时间戳_4位随机数）
    字段 languageCode: 字符串（en/id/th/ko/ja/es/pt/fr）
    字段 originalWord: 字符串（原语言单词/短句）
    字段 chineseTranslation: 字符串（中文翻译）
    字段 exampleSentence: 字符串（可选例句）
    字段 stage: 整型（0=未学 1-12=学习中 12=永久掌握）
    字段 nextReviewTime: 时间戳（下次复习时间）
    字段 correctStreak: 整型（连续答对次数）
    字段 lastReviewTime: 时间戳（上次复习时间）
    字段 createTime: 时间戳（创建时间）
}

实体 LearningRecord {
    字段 id: 字符串（全局唯一）
    字段 languageCode: 字符串
    字段 wordId: 字符串（关联Word.id）
    字段 reviewTime: 时间戳
    字段 isCorrect: 布尔型
    字段 stageBefore: 整型
    字段 stageAfter: 整型
}

实体 UserConfig {
    字段 currentLanguage: 字符串（默认en）
    字段 dailyNewWordLimit: 整型（默认300）
    字段 vibrateEnabled: 布尔型（默认true）
    字段 autoGenerateWord: 布尔型（默认true）
    // Git配置（加密存储）
    字段 gitRepoUrl: 字符串
    字段 gitSshKey: 字符串（显示打码：前4后4，中间****）
    // AI配置（加密存储）
    字段 aiApiUrl: 字符串
    字段 aiApiKey: 字符串（显示打码：前6后6，中间****）
    字段 aiModelId: 字符串
    // 更新配置
    字段 checkUpdateOnStart: 布尔型（默认true）
}

实体 SyncMeta {
    字段 lastSyncTimestamp: 时间戳
    字段 lastSyncDeviceId: 字符串
    字段 currentVersion: 字符串（v+时间戳）
    字段 conflictCount: 整型
}

实体 OfflineLog {
    字段 logId: 字符串
    字段 operationType: 字符串（ADD_WORD/UPDATE_PROGRESS/ADD_RECORD）
    字段 operationData: 字符串（JSON）
    字段 createTime: 时间戳
    字段 isSync: 布尔型
}

实体 GithubRelease {
    字段 tagName: 字符串（版本tag，如v1.0.0）
    字段 isPreRelease: 布尔型
    字段 assets: 列表<ReleaseAsset>
}

实体 ReleaseAsset {
    字段 name: 字符串（APK文件名）
    字段 downloadUrl: 字符串（下载链接）
}
```

### 2.2 常量定义
```plaintext
// 艾宾浩斯复习间隔（毫秒）
常量 REVIEW_INTERVAL = {
    1: 5*60*1000, 2: 30*60*1000, 3: 12*3600*1000, 4: 24*3600*1000,
    5: 2*24*3600*1000, 6: 4*24*3600*1000, 7: 7*24*3600*1000,
    8: 15*24*3600*1000, 9: 30*24*3600*1000, 10: 90*24*3600*1000,
    11: 180*24*3600*1000, 12: 365*24*3600*1000
}

// Git同步文件清单
常量 SYNC_FILES = {
    "word_base.csv": "总题库（id、languageCode、originalWord、chineseTranslation、exampleSentence）",
    "word_progress.csv": "学习进度（id、stage、nextReviewTime、correctStreak、lastReviewTime）",
    "learning_records.csv": "学习记录（全字段）",
    "sync_meta.csv": "同步元信息（全字段）"
}

// Git冲突规则
常量 CONFLICT_RULES = {
    "word_base": "远程覆盖本地（仅新增本地独有单词）",
    "word_progress": "按lastReviewTime最新为准",
    "learning_records": "合并去重（wordId+reviewTime+isCorrect）"
}

// 支持的语言/架构
常量 SUPPORT_LANGUAGES = ["en", "id", "th", "ko", "ja", "es", "pt", "fr"]
常量 SUPPORT_CPU_ABI = ["arm64-v8a", "x86_64"]

// 路径常量
常量 PATH_CONFIG = {
    "git_local_dir": "应用私有目录/word-king-git",
    "sync_local_dir": "应用私有目录/word-king-sync",
    "backup_dir": "应用私有目录/backup/${时间戳}",
    "offline_log": "应用私有目录/offline_log.csv",
    "apk_download_dir": "应用私有目录/Download"
}

// AI生成配置
常量 AI_CONFIG = {
    "auto_generate_threshold": 50, // 未学习单词<50自动生成
    "manual_generate_max": 500,    // 单次手动生成上限
    "auto_generate_daily_max": 1000 // 每日自动生成上限
}
```

### 2.3 通用工具函数
```plaintext
// 生成全局唯一ID
函数 generateGlobalId() -> 字符串 {
    返回 "${获取当前时间戳()}_${生成4位随机数()}"
}

// 版本对比（version1=当前版本，version2=线上版本；1=线上新，0=相同，-1=当前新）
函数 compareVersion(version1: 字符串, version2: 字符串) -> 整型 {
    v1 = 移除version1前缀"v"并按"."拆分
    v2 = 移除version2前缀"v"并按"."拆分
    maxLen = 取v1/v2最大长度
    for (i=0; i<maxLen; i++) {
        num1 = i<v1.length ? 转整型(v1[i]) : 0
        num2 = i<v2.length ? 转整型(v2[i]) : 0
        if (num2 > num1) return 1
        if (num2 < num1) return -1
    }
    return 0
}

// 加密存储敏感信息
函数 encryptSave(key: 字符串, value: 字符串) {
    加密实例 = 创建EncryptedSharedPreferences实例
    加密实例.putString(key, value)
}

// 解密获取敏感信息
函数 decryptGet(key: 字符串) -> 字符串 {
    加密实例 = 创建EncryptedSharedPreferences实例
    返回 加密实例.getString(key, "")
}

// 打码显示敏感信息
函数 maskSensitive(value: 字符串, type: 字符串) -> 字符串 {
    if (value.isEmpty()) return ""
    if (type == "SSH_KEY") return "${value.take(4)}****${value.takeLast(4)}"
    if (type == "AI_API_KEY") return "${value.take(6)}****${value.takeLast(6)}"
    return value
}

// 切换主线程
函数 runOnUiThread(task: 任务) {
    if (当前上下文是Activity) {
        Activity.runOnUiThread(task)
    } else {
        Handler(主线程Looper).post(task)
    }
}
```

## 三、核心功能模块设计
### 3.1 基础能力模块
#### 3.1.1 应用初始化流程
```plaintext
函数 appInit() {
    // 1. 初始化数据库
    初始化Room数据库()
    
    // 2. 加载用户配置
    加载UserConfig（无则初始化默认值）
    
    // 3. 首次使用初始化内置词库
    if (是首次启动) {
        内置词库 = 读取内置多语言词库（2000/语言）
        遍历内置词库 {
            单词.id = generateGlobalId()
            单词.stage = 0
            插入数据库(单词)
        }
        初始化SyncMeta（lastSyncTimestamp=0）
        显示引导页()
    } else {
        // 4. 非首次启动：自动检查更新+自动生成单词
        if (UserConfig.checkUpdateOnStart) {
            checkUpdate() // 静默检查，仅提示有更新
        }
        if (UserConfig.autoGenerateWord) {
            checkAndGenerateWords(UserConfig.currentLanguage)
        }
    }
    
    // 5. 跳转到主界面
    跳转到主界面()
}
```

#### 3.1.2 多语言切换
```plaintext
函数 switchLanguage(languageCode: 字符串) {
    // 1. 更新配置
    UserConfig.currentLanguage = languageCode
    保存UserConfig()
    
    // 2. 刷新界面
    刷新主界面待复习数()
    清空答题队列()
    刷新单词列表()
    
    // 3. 检查并自动生成单词
    if (UserConfig.autoGenerateWord) {
        checkAndGenerateWords(languageCode)
    }
}
```

### 3.2 艾宾浩斯复习模块
#### 3.2.1 复习时间计算
```plaintext
函数 calculateNextReviewTime(currentStage: 整型) -> 时间戳 {
    返回 获取当前时间戳() + REVIEW_INTERVAL.get(currentStage, 0)
}

函数 updateWordStage(word: Word, isCorrect: 布尔型) -> Word {
    if (isCorrect) {
        新阶段 = min(word.stage + 1, 12)
        新连续答对次数 = word.correctStreak + 1
    } else {
        新阶段 = max(word.stage - 2, 1)
        新连续答对次数 = 0
    }
    
    word.stage = 新阶段
    word.correctStreak = 新连续答对次数
    word.nextReviewTime = calculateNextReviewTime(新阶段)
    word.lastReviewTime = 获取当前时间戳()
    
    // 记录学习记录
    学习记录 = new LearningRecord(
        id = generateGlobalId(),
        languageCode = word.languageCode,
        wordId = word.id,
        reviewTime = 获取当前时间戳(),
        isCorrect = isCorrect,
        stageBefore = word.stage,
        stageAfter = 新阶段
    )
    插入数据库(学习记录)
    
    // 振动反馈
    vibrateForAnswer(isCorrect)
    
    返回 word
}
```

#### 3.2.2 答题交互流程
```plaintext
函数 startReview() {
    // 1. 获取今日待复习单词（去重）
    待复习列表 = getTodayReviewWords(UserConfig.currentLanguage)
    if (待复习列表.isEmpty()) {
        显示提示("今日无待复习单词，可学习新单词")
        返回
    }
    
    // 2. 初始化答题队列
    答题队列 = 随机打乱(待复习列表)
    加载下一题(答题队列)
    跳转到答题页()
}

函数 generateAnswerOptions(targetWord: Word) -> 列表<字符串> {
    正确答案 = targetWord.chineseTranslation
    干扰项列表 = 空列表
    
    while (干扰项列表.size < 3) {
        随机单词 = 从数据库查询（languageCode=targetWord.languageCode AND id!=targetWord.id）
        干扰项 = 随机单词.chineseTranslation
        if (干扰项 != 正确答案 AND 干扰项 not in 干扰项列表) {
            干扰项列表.add(干扰项)
        }
    }
    
    选项列表 = [正确答案] + 干扰项列表
    随机打乱(选项列表)
    返回 选项列表
}

函数 handleAnswer(word: Word, selectedAnswer: 字符串) {
    isCorrect = (selectedAnswer == word.chineseTranslation)
    更新后的单词 = updateWordStage(word, isCorrect)
    更新数据库(更新后的单词)
    
    // 提示结果
    if (isCorrect) {
        显示提示("正确！")
    } else {
        显示提示("错误，正确答案：${正确答案}")
        延迟3秒()
    }
    
    // 加载下一题
    答题队列.remove(0)
    if (答题队列.isEmpty()) {
        显示"复习完成"，返回主界面
    } else {
        加载下一题(答题队列)
    }
}

函数 vibrateForAnswer(isCorrect: 布尔型) {
    if (!UserConfig.vibrateEnabled) return
    if (!设备支持振动 OR 无振动权限) return
    
    振动时长 = isCorrect ? 100ms : 500ms
    触发设备振动(振动时长)
}
```

#### 3.2.3 学习新单词流程
```plaintext
函数 learnNewWords() {
    // 1. 检查每日上限
    今日已学 = 查询今日新单词学习数(UserConfig.currentLanguage)
    剩余可学 = UserConfig.dailyNewWordLimit - 今日已学
    if (剩余可学 <= 0) {
        显示提示("今日新单词已达上限（${UserConfig.dailyNewWordLimit}个）")
        返回
    }
    
    // 2. 获取未学习单词（去重）
    新单词列表 = 查询未学习单词（languageCode=当前语言，limit=剩余可学）
    if (新单词列表.isEmpty()) {
        显示提示("暂无未学习单词，点击AI生成补充")
        返回
    }
    
    // 3. 初始化答题队列
    答题队列 = 随机打乱(新单词列表)
    加载下一题(答题队列)
    跳转到答题页()
}
```

### 3.3 Git同步模块
#### 3.3.1 全量同步流程
```plaintext
函数 fullSyncWithGit() {
    // 前置检查
    if (UserConfig.gitRepoUrl.isEmpty() OR UserConfig.gitSshKey.isEmpty()) {
        显示提示("请先配置Git仓库信息")
        返回
    }
    
    // 1. 本地备份
    backupLocalData()
    
    // 2. 处理离线操作
    if (!handleOfflineSync()) {
        显示提示("无网络，无法同步")
        返回
    }
    
    // 3. 克隆/拉取远程仓库（处理冲突）
    if (!Git目录存在) {
        克隆Git仓库(UserConfig.gitRepoUrl, PATH_CONFIG.git_local_dir, decryptGet("gitSshKey"))
    } else {
        handleGitConflict()
        拉取远程仓库(PATH_CONFIG.git_local_dir, decryptGet("gitSshKey"))
    }
    
    // 4. 导出本地数据
    exportLocalSyncFiles()
    
    // 5. 合并文件
    遍历 SYNC_FILES 每个文件 {
        本地文件 = PATH_CONFIG.sync_local_dir/文件名
        远程文件 = PATH_CONFIG.git_local_dir/word-king/文件名
        mergeFile(本地文件, 远程文件, CONFLICT_RULES[文件前缀])
    }
    
    // 6. 数据校验
    if (!validateSyncData()) {
        记录日志("同步数据校验失败，已清理脏数据")
    }
    
    // 7. 提交推送
    添加文件到Git暂存区(远程同步目录下所有CSV)
    提交Git变更("WordKing同步：${时间戳} | 设备：${设备ID}")
    推送Git变更(PATH_CONFIG.git_local_dir, decryptGet("gitSshKey"))
    
    // 8. 导入远程数据
    importRemoteSyncFiles()
    
    // 9. 更新同步元信息
    更新SyncMeta（lastSyncTimestamp=当前时间戳，deviceId=设备ID）
    保存SyncMeta()
    
    // 10. 刷新界面
    refreshUIAfterSync()
    
    显示提示("同步完成")
}
```

#### 3.3.2 子函数：备份/合并/校验
```plaintext
函数 backupLocalData() {
    备份目录 = PATH_CONFIG.backup_dir.replace("${时间戳}", 当前时间戳)
    创建目录(备份目录)
    复制文件(所有同步CSV → 备份目录)
}

函数 handleGitConflict() {
    try {
        执行Git pull()
    } catch (冲突异常) {
        备份分支名 = "word-king-backup-${时间戳}"
        创建Git分支(备份分支名)
        提交本地变更(备份分支名)
        执行Git pull(force=true)
        合并本地新增数据()
        更新SyncMeta.conflictCount +=1
    }
}

函数 mergeFile(本地文件: 路径, 远程文件: 路径, 规则: 字符串) {
    本地数据 = 读取CSV(本地文件)
    远程数据 = 读取CSV(远程文件)
    合并后数据 = 空列表
    
    if (规则 == "远程覆盖本地（仅新增本地独有单词）") {
        远程ID集合 = 提取远程数据.id
        合并后数据 = 远程数据
        遍历本地数据 {
            if (行.id not in 远程ID集合) {
                合并后数据.add(行)
            }
        }
    } else if (规则 == "按lastReviewTime最新为准") {
        所有ID = 合并(本地数据.id, 远程数据.id)
        遍历所有ID {
            本地行 = 本地数据.get(id)
            远程行 = 远程数据.get(id)
            if (本地行.isEmpty()) 合并后数据.add(远程行)
            else if (远程行.isEmpty()) 合并后数据.add(本地行)
            else 合并后数据.add(本地行.lastReviewTime > 远程行.lastReviewTime ? 本地行 : 远程行)
        }
    } else if (规则 == "合并去重（wordId+reviewTime+isCorrect）") {
        所有记录 = 本地数据 + 远程数据
        去重标识集合 = 空
        遍历所有记录 {
            标识 = "${行.wordId}_${行.reviewTime}_${行.isCorrect}"
            if (标识 not in 去重标识集合) {
                去重标识集合.add(标识)
                合并后数据.add(行)
            }
        }
    }
    
    写入CSV(远程文件, 合并后数据)
}

函数 validateSyncData() -> 布尔型 {
    校验结果 = true
    // 进度ID必须在题库中
    进度ID = 读取CSV(word_progress.csv).id
    题库ID = 读取CSV(word_base.csv).id
    异常ID = 进度ID - 题库ID
    if (异常ID非空) {
        删除word_progress.csv中异常ID记录
        校验结果 = false
    }
    // 记录wordId必须在题库中
    记录wordId = 读取CSV(learning_records.csv).wordId
    异常wordId = 记录wordId - 题库ID
    if (异常wordId非空) {
        删除learning_records.csv中异常wordId记录
        校验结果 = false
    }
    返回 校验结果
}

函数 handleOfflineSync() -> 布尔型 {
    if (无网络) return false
    离线日志 = 读取CSV(PATH_CONFIG.offline_log)
    遍历离线日志 {
        if (!行.isSync) {
            按行.operationType执行对应操作(行.operationData)
            标记行.isSync = true
        }
    }
    保存离线日志()
    返回 true
}
```

### 3.4 AI生成题库模块
#### 3.4.1 自动/手动生成流程
```plaintext
函数 checkAndGenerateWords(languageCode: 字符串) {
    // 1. 检查词库余量
    未学习单词数 = 查询未学习单词数(languageCode)
    if (未学习单词数 >= AI_CONFIG.auto_generate_threshold) return
    
    // 2. 检查配置/网络
    if (UserConfig.aiApiUrl.isEmpty() OR UserConfig.aiApiKey.isEmpty()) return
    if (无网络) {
        写入离线日志(operationType="GENERATE_WORD", data=languageCode)
        return
    }
    
    // 3. 自动生成（默认100个）
    generateWordsByAI(languageCode, 100, "AUTO")
}

函数 generateWordsByAI(languageCode: 字符串, count: 整型, type: 字符串) {
    // 1. 构建提示词
    语言名称 = 获取语言名称(languageCode)
    提示词 = """
        生成${count}个${语言名称}日常高频单词（入门级），仅输出JSON数组：
        [{"original_word":"原词","chinese_translation":"中文翻译"}]
    """
    
    // 2. 调用AI接口
    try {
        响应 = 发送POST请求(
            url = UserConfig.aiApiUrl,
            headers = {"Authorization": "Bearer ${decryptGet('aiApiKey')}"},
            body = {"model": UserConfig.aiModelId, "messages": [{"role":"user","content":提示词}]}
        )
        AI单词列表 = 解析JSON(响应)
        
        // 3. 去重
        本地单词集合 = 查询本地单词(languageCode).originalWord
        待入库列表 = AI单词列表.filter { it.original_word not in 本地单词集合 }
        
        // 4. 入库
        遍历待入库列表 {
            单词 = new Word(
                id = generateGlobalId(),
                languageCode = languageCode,
                originalWord = it.original_word,
                chineseTranslation = it.chinese_translation,
                stage = 0,
                createTime = 当前时间戳
            )
        }
        批量插入数据库(待入库列表)
        
        // 5. 反馈结果
        if (type == "MANUAL") {
            显示提示("生成成功！新增${待入库列表.size}个单词")
        } else {
            显示Snackbar("已自动补充${待入库列表.size}个单词")
        }
        
        // 6. 刷新界面
        refreshWordBankAfterGenerate(languageCode)
    } catch (异常) {
        显示提示("生成失败：${异常信息}")
    }
}

函数 refreshWordBankAfterGenerate(languageCode: 字符串) {
    刷新单词列表(未学标签)
    更新主界面词库余量提示("未学习单词：${查询未学习单词数(languageCode)}个")
    更新学习新单词按钮状态()
}
```

### 3.5 检查更新模块
```plaintext
函数 checkUpdate() {
    // 1. 获取当前版本
    当前版本 = 获取APP版本号()
    
    // 2. 调用GitHub API
    try {
        响应 = 发送GET请求("https://api.github.com/repos/steven0lisa/word-king-app/releases/latest")
        release = 解析JSON(响应, GithubRelease)
        
        if (release.isPreRelease) {
            显示提示("暂无最新版本")
            return
        }
        
        // 3. 版本对比
        对比结果 = compareVersion(当前版本, release.tagName)
        if (对比结果 == 1) {
            显示更新弹窗(release)
        } else if (对比结果 == 0) {
            显示提示("当前已是最新版本")
        }
    } catch (异常) {
        显示提示("检查更新失败：${异常信息}")
    }
}

函数 showUpdateDialog(release: GithubRelease) {
    显示弹窗(
        标题 = "发现新版本 ${release.tagName}",
        内容 = "是否升级？",
        确认按钮 = "升级",
        取消按钮 = "取消",
        确认回调 = {
            checkInstallPermission { downloadApk(release) }
        }
    )
}

函数 downloadApk(release: GithubRelease) {
    // 1. 匹配架构
    设备架构 = 获取设备CPU架构()
    目标APK = release.assets.filter {
        (设备架构 == "arm64-v8a" && it.name.contains("arm")) 
        || (设备架构 == "x86_64" && it.name.contains("x64"))
    }.firstOrNull()
    
    if (目标APK == null) {
        显示提示("无适配当前架构的APK")
        return
    }
    
    // 2. 下载APK
    下载进度 = 0
    保存路径 = PATH_CONFIG.apk_download_dir + "/word-king-${release.tagName}.apk"
    下载文件(
        url = 目标APK.downloadUrl,
        path = 保存路径,
        进度回调 = { 进度 -> 下载进度 = 进度 },
        完成回调 = { installApk(保存路径) },
        失败回调 = { 显示提示("下载失败：${异常}") }
    )
}

函数 installApk(apkPath: 字符串) {
    安装意图 = new Intent(ACTION_VIEW)
    if (Android版本 >= 7.0) {
        apkUri = FileProvider.getUriForFile(上下文, 包名+".fileprovider", new File(apkPath))
        安装意图.addFlags(FLAG_GRANT_READ_URI_PERMISSION)
    } else {
        apkUri = Uri.fromFile(new File(apkPath))
    }
    安装意图.setDataAndType(apkUri, "application/vnd.android.package-archive")
    启动Activity(安装意图)
}

函数 checkInstallPermission(callback: 回调) {
    if (Android版本 >= 8.0 && 无安装权限) {
        显示弹窗(
            标题 = "需要安装权限",
            内容 = "请开启允许安装应用权限",
            确认回调 = { 跳转到权限设置页 }
        )
    } else {
        callback()
    }
}
```

### 3.6 学习统计模块
```plaintext
函数 generateFullLearningStats() -> 字典 {
    今日开始时间 = 获取今日0点时间戳()
    本周开始时间 = 获取本周一0点时间戳()
    
    // 基础指标
    今日新单词数 = 查询学习记录（reviewTime>=今日 AND stageBefore=0）.distinct(wordId).count()
    今日复习数 = 查询学习记录（reviewTime>=今日 AND stageBefore>0）.distinct(wordId).count()
    
    // 正确率
    总答题数 = 查询学习记录.count()
    总正确数 = 查询学习记录（isCorrect=true）.count()
    总正确率 = 总答题数 == 0 ? 0 : 总正确数/总答题数*100
    
    // 各语言掌握数
    各语言掌握数 = 空字典
    遍历SUPPORT_LANGUAGES {
        掌握数 = 查询Word（languageCode=当前语言 AND stage=12）.count()
        各语言掌握数[当前语言] = 掌握数
    }
    
    // 密度图数据
    密度数据 = 空字典
    遍历最近365天 {
        日期 = 格式化日期(当前天)
        学习数 = 查询学习记录（reviewTime在当天）.count()
        密度数据[日期] = 学习数
    }
    
    返回 {
        "今日新单词数": 今日新单词数,
        "今日复习数": 今日复习数,
        "总正确率": 总正确率,
        "各语言掌握数": 各语言掌握数,
        "密度图数据": 密度数据
    }
}

函数 drawDensityChart(密度数据: 字典) {
    图表配置 = {
        颜色梯度 = ["#ebedf0", "#9be9a8", "#40c463", "#30a14e", "#216e39"],
        X轴 = 最近365天日期,
        Y轴 = 周数,
        数据 = 密度数据,
        样式 = "GitLab热力图"
    }
    渲染图表(图表配置)
}
```

### 3.7 系统设置模块
#### 3.7.1 配置页交互
```plaintext
函数 saveGitConfig(repoUrl: 字符串, sshKey: 字符串) {
    encryptSave("gitRepoUrl", repoUrl)
    encryptSave("gitSshKey", sshKey)
    UserConfig.gitRepoUrl = repoUrl
    保存UserConfig()
    显示提示("Git配置已保存")
}

函数 saveAIConfig(apiUrl: 字符串, apiKey: 字符串, modelId: 字符串) {
    encryptSave("aiApiUrl", apiUrl)
    encryptSave("aiApiKey", apiKey)
    UserConfig.aiApiUrl = apiUrl
    UserConfig.aiModelId = modelId
    保存UserConfig()
    显示提示("AI配置已保存")
}

函数 saveGeneralConfig(dailyLimit: 整型, vibrate: 布尔型, autoGenerate: 布尔型) {
    UserConfig.dailyNewWordLimit = dailyLimit
    UserConfig.vibrateEnabled = vibrate
    UserConfig.autoGenerateWord = autoGenerate
    保存UserConfig()
    显示提示("通用设置已保存")
}
```

## 四、界面设计规范
### 4.1 页面清单与核心元素
| 页面名称         | 核心UI元素                                                                 | 交互逻辑                                                                 |
|------------------|--------------------------------------------------------------------------|--------------------------------------------------------------------------|
| 启动页           | Logo、加载进度条、版本号                                                 | 初始化完成后自动跳转（停留2秒）                                           |
| 引导页           | 滑动引导（3页）、跳过/下一步/完成按钮                                    | 滑动切换，完成/跳过按钮跳转到主界面                                       |
| 主界面           | 语言切换Spinner、今日复习卡片、开始复习/学习新单词/AI生成按钮、底部导航（首页/统计/设置） | 点击按钮进入对应流程，底部导航切换页面                                     |
| 答题页           | 单词展示区、4个选项按钮、结果提示、下一题按钮                            | 点击选项触发答题逻辑，完成后加载下一题/返回主界面                          |
| 单词列表页       | 顶部筛选Tab（未学/学习中/已掌握）、搜索框、AI生成按钮、单词列表           | 筛选切换列表，点击AI生成触发手动生成，点击单词查看详情                     |
| 统计页           | 学习密度热力图、今日/本周/本月学习卡片、正确率进度条、各语言掌握数列表     | 进入页面自动加载统计数据，支持刷新                                         |
| 设置页           | Git配置/AI配置/通用设置/关于入口列表                                     | 点击入口进入对应配置页                                                   |
| Git配置页        | 仓库地址输入框、SSH Key输入框（打码）、测试连接/保存/同步按钮             | 保存按钮加密存储配置，同步按钮触发fullSyncWithGit                         |
| AI配置页         | API地址/API Key/模型ID输入框、生成数量选择框、保存/批量生成按钮、生成记录 | 批量生成按钮触发generateWordsByAI（手动）                                 |
| 通用设置页       | 每日上限输入框、振动/自动生成/自动更新开关、本地备份/恢复按钮              | 保存按钮更新UserConfig                                                   |
| 关于页           | 应用介绍、版本号、隐私政策链接                                           | 点击链接打开浏览器查看隐私政策                                           |

### 4.2 交互规范
1. **提示样式**：自动操作（如自动生成）用Snackbar，手动操作（如手动生成/同步）用弹窗；
2. **加载状态**：异步操作（同步/生成/下载）显示加载动画，禁止重复点击；
3. **权限引导**：权限缺失时弹窗引导用户到设置页，说明权限用途；
4. **空状态**：单词列表/复习列表为空时显示引导文案+操作按钮；
5. **错误处理**：所有异常需给出明确提示，提供重试/取消选项。

## 五、验收标准
### 5.1 功能验收
1. **基础能力**：多语言切换后界面数据实时刷新，首次启动加载内置词库；
2. **艾宾浩斯复习**：答对进阶、答错退阶，复习时间计算符合规则，振动反馈正常；
3. **Git同步**：支持全量同步，冲突自动处理，离线操作联网后同步，本地备份生成；
4. **AI生成题库**：词库不足50自动生成，手动生成支持自定义数量，生成后去重且界面刷新；
5. **检查更新**：能识别GitHub新版本，匹配架构下载APK，引导安装权限并完成安装；
6. **统计功能**：所有统计指标计算准确，密度图渲染符合GitLab风格；
7. **系统设置**：敏感信息加密存储+打码显示，设置修改后实时生效。

### 5.2 性能验收
1. 单词列表加载（2000+单词）无卡顿，支持分页；
2. Git同步/AI生成/CSV解析在子线程执行，不阻塞主线程；
3. 应用启动时间≤3秒，页面切换≤500ms；
4. 支持Android 8.0+所有机型，无崩溃。

### 5.3 兼容性验收
1. 支持arm64-v8a/x86_64架构，其他架构提示不支持；
2. 适配Android 8.0-14所有版本，权限申请符合系统规范；
3. 适配不同屏幕尺寸（手机/平板），UI无变形。

## 六、交付物要求
1. AI工具需基于本PRD的伪代码，生成可编译运行的安卓原生APP（Kotlin）；
2. 代码需遵循Jetpack最佳实践，包含完整的注释；
3. 生成的APP需通过所有验收标准，无功能缺失/崩溃；
4. 交付包含源码、APK安装包、编译说明文档。

## 总结
1. 本PRD覆盖词王APP全核心功能，所有业务逻辑以伪代码实现，可直接供AI工具解析执行；
2. 核心逻辑闭环：艾宾浩斯复习为核心，Git同步保障多端一致，AI生成补充词库，检查更新保障版本迭代；
3. 严格遵循安卓开发规范，兼顾兼容性、性能、安全性（敏感信息加密），符合产品落地要求。
