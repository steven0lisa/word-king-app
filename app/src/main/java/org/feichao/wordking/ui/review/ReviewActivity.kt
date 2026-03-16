package org.feichao.wordking.ui.review

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.feichao.wordking.WordKingApplication
import org.feichao.wordking.data.entity.LearningRecord
import org.feichao.wordking.data.entity.Word
import org.feichao.wordking.data.repository.LearningRecordRepository
import org.feichao.wordking.data.repository.UserConfigRepository
import org.feichao.wordking.data.repository.WordRepository
import org.feichao.wordking.databinding.ActivityReviewBinding
import org.feichao.wordking.service.VibrateService
import org.feichao.wordking.util.EbbinghausUtils
import org.feichao.wordking.util.IdGenerator
import org.feichao.wordking.widget.ExplosionField
import org.feichao.wordking.widget.FecesExplosionView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewBinding
    private lateinit var wordRepository: WordRepository
    private lateinit var userConfigRepository: UserConfigRepository
    private lateinit var learningRecordRepository: LearningRecordRepository
    private lateinit var vibrateService: VibrateService
    private lateinit var explosionField: ExplosionField
    private lateinit var fecesExplosionView: FecesExplosionView

    private var wordList = mutableListOf<Word>()
    private var currentIndex = 0
    private var options = listOf<String>()
    private var currentWord: Word? = null
    private var mode = "review" // review, learn, or learning
    private var answered = false
    private var language = "en"
    private var questionCount = 0
    private val maxQuestions = 100  // 学习模式最大题数

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化烟花效果
        explosionField = ExplosionField.attach2Window(this)
        // 初始化炸屎效果
        fecesExplosionView = FecesExplosionView.attachToWindow(this)

        val database = WordKingApplication.instance.database
        wordRepository = WordRepository(database.wordDao())
        userConfigRepository = UserConfigRepository(database.userConfigDao())
        learningRecordRepository = LearningRecordRepository(database.learningRecordDao())
        vibrateService = VibrateService(this)

        mode = intent.getStringExtra("mode") ?: "review"

        setupButtons()
        loadWords()
    }

    private fun setupButtons() {
        binding.btnOption1.setOnClickListener { handleAnswer(0) }
        binding.btnOption2.setOnClickListener { handleAnswer(1) }
        binding.btnOption3.setOnClickListener { handleAnswer(2) }
        binding.btnOption4.setOnClickListener { handleAnswer(3) }

        binding.btnNext.setOnClickListener { nextQuestion() }
    }

    /**
     * 获取今天0点的时间戳
     */
    private fun getTodayStartTime(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun loadWords() {
        lifecycleScope.launch {
            val config = userConfigRepository.getUserConfigSync()
            language = config?.currentLanguage ?: "en"

            when (mode) {
                "review" -> {
                    // 复习模式：获取需要复习的单词，排除今天已做对的
                    val todayStart = getTodayStartTime()
                    val todayCorrectIds = learningRecordRepository.getTodayCorrectWordIds(todayStart)
                    val todayWrongIds = learningRecordRepository.getTodayWrongWordIds(todayStart)
                    val todayPracticedIds = todayCorrectIds + todayWrongIds

                    val reviewWords = wordRepository.getWordsNeedingReview(language)
                        .filter { it.id !in todayPracticedIds || it.id in todayWrongIds }

                    wordList = reviewWords.toMutableList()
                    if (wordList.isEmpty()) {
                        Toast.makeText(
                            this@ReviewActivity,
                            "今日无待复习单词",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                        return@launch
                    }
                    wordList.shuffle()
                }
                "learn" -> {
                    // 学习模式（旧）：获取未学习的单词
                    val limit = config?.dailyNewWordLimit ?: 300
                    val todayStart = getTodayStartTime()
                    val todayPracticedIds = learningRecordRepository.getTodayPracticedWordIds(todayStart)

                    wordList = wordRepository.getUnlearnedWords(language, limit)
                        .filter { it.id !in todayPracticedIds }
                        .toMutableList()

                    if (wordList.isEmpty()) {
                        Toast.makeText(
                            this@ReviewActivity,
                            "暂无未学习单词",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                        return@launch
                    }
                    wordList.shuffle()
                }
                "learning" -> {
                    // 学习模式（新）：按概率选词，不预设列表
                    val nextWord = selectWordByProbability()
                    if (nextWord != null) {
                        wordList.add(nextWord)
                    } else {
                        Toast.makeText(
                            this@ReviewActivity,
                            "今天已学完，明天再来吧！",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                        return@launch
                    }
                }
            }

            showQuestion()
        }
    }

    /**
     * 智能选词算法
     * 每次都从数据库实时查询今天做过的词，确保不会重复
     *
     * 优先级：
     * 1. 未学过的词（stage=0，今天没做过）：50%
     * 2. 答错的词（今天答错）：37%
     * 3. 少量见过且做对的词（正确次数1-5，stage 1-11，今天没做过）：10%
     * 4. 已掌握的词（stage=12，今天没做过）：3%
     */
    private suspend fun selectWordByProbability(): Word? {
        // 实时从数据库获取今天做过的词
        val todayStart = getTodayStartTime()
        val todayPracticedIds = learningRecordRepository.getTodayPracticedWordIds(todayStart).toSet()
        val todayWrongIds = learningRecordRepository.getTodayWrongWordIds(todayStart).toSet()

        // 获取所有类别的单词
        val allUnlearned = wordRepository.getUnlearnedWords(language, 100)  // stage=0
        val allLearning = wordRepository.getLearningWords(language)           // stage 1-11
        val allMastered = wordRepository.getMasteredWordsSync(language)       // stage=12

        // 过滤掉今天已做过的单词（使用实时查询的数据）
        val unlearned = allUnlearned.filter { it.id !in todayPracticedIds }
        val learning = allLearning.filter { it.id !in todayPracticedIds }
        val mastered = allMastered.filter { it.id !in todayPracticedIds }

        // 答错的词池（优先今天答错的，如果没有则选正确次数少的）
        val wrongPool = allLearning
            .filter { it.id in todayWrongIds }
            .ifEmpty {
                // 今天没有答错的词，从学习中选正确次数少的（1-3次）
                learning.filter { it.correctStreak in 1..3 }
            }

        // 少量见过且做对的词（正确次数1-5，今天没做过，且不在错误池中）
        val fewCorrectWords = learning.filter {
            it.correctStreak in 1..5 &&
            it.id !in todayWrongIds &&
            it.id !in wrongPool.map { w -> w.id }
        }

        // 统计可用词池
        val pools = mapOf(
            "unlearned" to unlearned,
            "wrong" to wrongPool,
            "fewCorrect" to fewCorrectWords,
            "mastered" to mastered
        )

        val totalAvailable = pools.values.sumOf { it.size }
        if (totalAvailable == 0) {
            return null
        }

        // 根据概率选择词池
        val random = Math.random()

        val selectedPool = when {
            // 50% 概率选择未学过的词
            random < 0.5 && unlearned.isNotEmpty() -> "unlearned"
            // 累计 87% (50% + 37%) 选择答错的词
            random < 0.87 && wrongPool.isNotEmpty() -> "wrong"
            // 累计 97% (87% + 10%) 选择少量见过的词
            random < 0.97 && fewCorrectWords.isNotEmpty() -> "fewCorrect"
            // 3% 概率选择已掌握的词
            mastered.isNotEmpty() -> "mastered"
            // 备选方案
            fewCorrectWords.isNotEmpty() -> "fewCorrect"
            wrongPool.isNotEmpty() -> "wrong"
            unlearned.isNotEmpty() -> "unlearned"
            else -> null
        }

        return selectedPool?.let { pools[it]?.random() }
    }

    private fun showQuestion() {
        if (currentIndex >= wordList.size) {
            if (mode == "learning") {
                // 学习模式下，动态选择下一个单词
                lifecycleScope.launch {
                    val nextWord = selectWordByProbability()
                    if (nextWord != null) {
                        wordList.clear()
                        wordList.add(nextWord)
                        currentIndex = 0
                        showQuestion()
                    } else {
                        showCompletion()
                    }
                }
                return
            }
            showCompletion()
            return
        }

        answered = false
        currentWord = wordList[currentIndex]
        questionCount++

        // 更新进度
        binding.tvProgress.text = if (mode == "learning") "第 $questionCount 题" else "${currentIndex + 1} / ${wordList.size}"

        // 显示单词
        binding.tvWord.text = currentWord?.originalWord ?: ""
        binding.tvResult.visibility = View.GONE
        binding.layoutOptions.visibility = View.VISIBLE
        binding.btnNext.visibility = View.GONE

        // 生成选项（异步）
        lifecycleScope.launch {
            generateOptionsSync()
        }
    }

    private fun showCompletion() {
        binding.tvResult.text = if (mode == "learning") "今天已学完！" else "复习完成！"
        binding.tvResult.visibility = View.VISIBLE
        binding.layoutOptions.visibility = View.GONE
        binding.btnNext.text = "返回"
        binding.btnNext.visibility = View.VISIBLE
    }

    /**
     * 同步生成选项（在协程内调用）
     */
    private suspend fun generateOptionsSync() {
        val correct = currentWord?.chineseTranslation ?: ""

        // 获取干扰项
        val lang = currentWord?.languageCode ?: "en"
        val allWords = wordRepository.getAllWordsSync().filter {
            it.languageCode == lang && it.id != currentWord?.id
        }.toMutableList().apply { shuffle() }.take(3)

        val wrongOptions = allWords.map { it.chineseTranslation }

        // 合并并打乱
        options = (listOf(correct) + wrongOptions).shuffled()

        // 显示选项
        binding.btnOption1.text = options.getOrElse(0) { "" }
        binding.btnOption2.text = options.getOrElse(1) { "" }
        binding.btnOption3.text = options.getOrElse(2) { "" }
        binding.btnOption4.text = options.getOrElse(3) { "" }

        // 重置按钮状态
        resetButtonStyles()
    }

    /**
     * 左右摇动动画
     */
    private fun shakeButton(button: com.google.android.material.button.MaterialButton) {
        val originalTranslationX = button.translationX
        button.animate()
            .translationX(originalTranslationX + 20f)
            .setDuration(100)
            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
            .withEndAction {
                button.animate()
                    .translationX(originalTranslationX - 20f)
                    .setDuration(100)
                    .withEndAction {
                        button.animate()
                            .translationX(originalTranslationX + 10f)
                            .setDuration(50)
                            .withEndAction {
                                button.animate()
                                    .translationX(originalTranslationX)
                                    .setDuration(50)
                                    .start()
                            }
                            .start()
                    }
                    .start()
            }
            .start()
    }

    private fun resetButtonStyles() {
        val buttons = listOf(binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4)
        buttons.forEach {
            it.isEnabled = true
            it.alpha = 1.0f
            it.setBackgroundColor(getColor(org.feichao.wordking.R.color.background))
            it.setTextColor(getColor(org.feichao.wordking.R.color.charcoal_blue))
            it.strokeColor = android.content.res.ColorStateList.valueOf(getColor(org.feichao.wordking.R.color.divider))
            it.strokeWidth = 2
        }
    }

    private fun handleAnswer(selectedIndex: Int) {
        if (answered || selectedIndex >= options.size) return

        answered = true
        val selectedAnswer = options[selectedIndex]
        val correctAnswer = currentWord?.chineseTranslation ?: ""
        val isCorrect = selectedAnswer == correctAnswer

        // 振动反馈
        vibrateService.vibrateForAnswer(isCorrect)

        // 获取所有按钮
        val buttons = listOf(binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4)
        val selectedButton = buttons.getOrNull(selectedIndex)

        // 找到正确答案的按钮
        val correctButtonIndex = options.indexOf(correctAnswer)
        val correctButton = buttons.getOrNull(correctButtonIndex)

        // 更新选项按钮样式
        buttons.forEachIndexed { index, button ->
            val optionText = options.getOrNull(index) ?: return@forEachIndexed

            when {
                // 正确答案 - 绿色背景，白色文字
                optionText == correctAnswer -> {
                    button.setBackgroundColor(getColor(org.feichao.wordking.R.color.correct_green))
                    button.setTextColor(getColor(android.R.color.white))
                    button.strokeColor = android.content.res.ColorStateList.valueOf(getColor(org.feichao.wordking.R.color.correct_green))
                    button.isEnabled = false
                }
                // 选错的选项 - 红色背景，白色文字
                index == selectedIndex && !isCorrect -> {
                    button.setBackgroundColor(getColor(org.feichao.wordking.R.color.incorrect_red))
                    button.setTextColor(getColor(android.R.color.white))
                    button.strokeColor = android.content.res.ColorStateList.valueOf(getColor(org.feichao.wordking.R.color.incorrect_red))
                    button.isEnabled = false
                }
                // 其他选项 - 变暗
                else -> {
                    button.alpha = 0.5f
                    button.isEnabled = false
                }
            }
        }

        // 答对时从选中的选项处显示烟花效果
        if (isCorrect && selectedButton != null) {
            explosionField.explodeWithoutHide(selectedButton)
        }

        // 答错时正确答案左右摇动
        if (!isCorrect && correctButton != null) {
            shakeButton(correctButton)
            correctButton?.let {
                val location = IntArray(2)
                it.getLocationOnScreen(location)
                val targetX = location[0] + it.width / 2f
                val targetY = location[1] + it.height / 2f
                fecesExplosionView.explode(targetX, targetY)
            }
        }

        // 更新单词阶段
        lifecycleScope.launch {
            currentWord?.let { word ->
                val newStage = EbbinghausUtils.calculateNewStage(word.stage, isCorrect)
                val nextReviewTime = EbbinghausUtils.calculateNextReviewTime(newStage)

                val updatedWord = word.copy(
                    stage = newStage,
                    nextReviewTime = nextReviewTime,
                    correctStreak = if (isCorrect) word.correctStreak + 1 else 0,
                    lastReviewTime = System.currentTimeMillis()
                )

                wordRepository.updateWord(updatedWord)

                // 记录学习记录
                val record = LearningRecord(
                    id = IdGenerator.generateGlobalId(),
                    languageCode = word.languageCode,
                    wordId = word.id,
                    reviewTime = System.currentTimeMillis(),
                    isCorrect = isCorrect,
                    stageBefore = word.stage,
                    stageAfter = newStage
                )

                val database = WordKingApplication.instance.database
                database.learningRecordDao().insertRecord(record)
            }
        }

        // 显示结果
        if (isCorrect) {
            binding.tvResult.text = "正确"
            binding.tvResult.setTextColor(getColor(org.feichao.wordking.R.color.correct_green))
        } else {
            binding.tvResult.text = "错误"
            binding.tvResult.setTextColor(getColor(org.feichao.wordking.R.color.incorrect_red))
        }
        binding.tvResult.visibility = View.VISIBLE
        binding.btnNext.visibility = View.VISIBLE
    }

    private fun nextQuestion() {
        if (mode == "learning") {
            // 学习模式下，动态选择下一个单词
            lifecycleScope.launch {
                val nextWord = selectWordByProbability()
                if (nextWord != null) {
                    // 重置状态
                    answered = false
                    currentWord = nextWord
                    currentIndex = 0
                    questionCount++

                    // 更新进度
                    binding.tvProgress.text = "第 $questionCount 题"
                    binding.progressBar.visibility = View.GONE

                    // 显示单词
                    binding.tvWord.text = currentWord?.originalWord ?: ""
                    binding.tvResult.visibility = View.GONE
                    binding.layoutOptions.visibility = View.VISIBLE
                    binding.btnNext.visibility = View.GONE

                    // 生成选项（在协程中）
                    generateOptionsSync()
                } else {
                    // 没有更多单词可学
                    showCompletion()
                }
            }
            return
        }

        if (currentIndex >= wordList.size) {
            finish()
            return
        }

        currentIndex++
        answered = false
        showQuestion()
    }
}
