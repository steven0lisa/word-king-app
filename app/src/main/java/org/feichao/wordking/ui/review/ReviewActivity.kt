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
import org.feichao.wordking.data.repository.UserConfigRepository
import org.feichao.wordking.data.repository.WordRepository
import org.feichao.wordking.databinding.ActivityReviewBinding
import org.feichao.wordking.service.VibrateService
import org.feichao.wordking.util.EbbinghausUtils
import org.feichao.wordking.util.IdGenerator
import org.feichao.wordking.widget.ExplosionField
import org.feichao.wordking.widget.FecesExplosionView

class ReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewBinding
    private lateinit var wordRepository: WordRepository
    private lateinit var userConfigRepository: UserConfigRepository
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

    private fun loadWords() {
        lifecycleScope.launch {
            val config = userConfigRepository.getUserConfigSync()
            language = config?.currentLanguage ?: "en"

            when (mode) {
                "review" -> {
                    // 复习模式：获取需要复习的单词
                    wordList = wordRepository.getWordsNeedingReview(language).toMutableList()
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
                    wordList = wordRepository.getUnlearnedWords(language, limit).toMutableList()
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
                    // 初始加载一轮单词
                    loadWordsByProbability()
                    if (wordList.isEmpty()) {
                        Toast.makeText(
                            this@ReviewActivity,
                            "词库为空，请先添加单词",
                            Toast.LENGTH_SHORT
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
     * 按概率加载单词：50%新词 + 30%学习中 + 20%已掌握
     */
    private suspend fun loadWordsByProbability() {
        val unlearned = wordRepository.getUnlearnedWords(language, 100)  // stage=0
        val learning = wordRepository.getLearningWords(language)        // stage 1-11
        val mastered = wordRepository.getMasteredWordsSync(language)    // stage=12

        val newWordCount = (maxQuestions * 0.5).toInt().coerceAtLeast(1)
        val learningWordCount = (maxQuestions * 0.3).toInt().coerceAtLeast(1)
        val masteredWordCount = (maxQuestions * 0.2).toInt().coerceAtLeast(1)

        wordList.clear()

        // 按概率比例添加各类单词
        repeat(minOf(newWordCount, unlearned.size)) { wordList.add(unlearned[it % unlearned.size]) }
        repeat(minOf(learningWordCount, learning.size)) { wordList.add(learning[it % learning.size]) }
        repeat(minOf(masteredWordCount, mastered.size)) { wordList.add(mastered[it % mastered.size]) }

        wordList.shuffle()
    }

    /**
     * 根据概率选择一个单词
     */
    private suspend fun selectWordByProbability(): Word? {
        val unlearned = wordRepository.getUnlearnedWords(language, 50)  // stage=0
        val learning = wordRepository.getLearningWords(language)        // stage 1-11
        val mastered = wordRepository.getMasteredWordsSync(language)    // stage=12

        // 如果所有类别都为空，返回null
        if (unlearned.isEmpty() && learning.isEmpty() && mastered.isEmpty()) {
            return null
        }

        val random = Math.random()

        return when {
            // 50% 概率选新词
            random < 0.5 && unlearned.isNotEmpty() -> unlearned.random()
            // 30% 概率选学习中（累计80%）
            random < 0.8 && learning.isNotEmpty() -> learning.random()
            // 20% 概率选已掌握
            mastered.isNotEmpty() -> mastered.random()
            // 备选：学习中
            learning.isNotEmpty() -> learning.random()
            // 备选：新词
            unlearned.isNotEmpty() -> unlearned.random()
            else -> null
        }
    }

    private fun showQuestion() {
        if (currentIndex >= wordList.size) {
            if (mode == "learning") {
                // 学习模式下，动态加载更多单词
                lifecycleScope.launch {
                    loadWordsByProbability()
                    currentIndex = 0
                    if (wordList.isEmpty()) {
                        showCompletion()
                    } else {
                        showQuestion()
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

        // 更新进度（进度条已隐藏）
        binding.tvProgress.text = if (mode == "learning") "第 $questionCount 题" else "${currentIndex + 1} / ${wordList.size}"

        // 显示单词
        binding.tvWord.text = currentWord?.originalWord ?: ""
        binding.tvResult.visibility = View.GONE
        binding.layoutOptions.visibility = View.VISIBLE
        binding.btnNext.visibility = View.GONE

        // 生成选项
        generateOptions()
    }

    private fun showCompletion() {
        binding.tvResult.text = "复习完成！"
        binding.tvResult.visibility = View.VISIBLE
        binding.layoutOptions.visibility = View.GONE
        binding.btnNext.text = "返回"
        binding.btnNext.visibility = View.VISIBLE
    }

    private fun generateOptions() {
        lifecycleScope.launch {
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
    }

    /**
     * 左右摇动动画
     */
    private fun shakeButton(button: com.google.android.material.button.MaterialButton) {
        val originalTranslationX = button.translationX
        // 使用非线性动画（加速减速）
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
            // 答错时显示炸屎效果
            correctButton?.let {
                // 获取按钮中心位置
                val location = IntArray(2)
                it.getLocationOnScreen(location)
                val targetX = location[0] + it.width / 2f
                val targetY = location[1] + it.height / 2f
                android.util.Log.d("ReviewActivity", "Feces explosion to: ($targetX, $targetY)")
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
                    currentWord = nextWord
                    currentIndex = 0
                    questionCount++
                    answered = false  // 重置回答状态

                    // 更新进度（隐藏进度条）
                    binding.tvProgress.text = "第 $questionCount 题"
                    binding.progressBar.visibility = View.GONE

                    // 显示单词
                    binding.tvWord.text = currentWord?.originalWord ?: ""
                    binding.tvResult.visibility = View.GONE
                    binding.layoutOptions.visibility = View.VISIBLE
                    binding.btnNext.visibility = View.GONE

                    generateOptions()
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
        answered = false  // 重置回答状态
        showQuestion()
    }
}
