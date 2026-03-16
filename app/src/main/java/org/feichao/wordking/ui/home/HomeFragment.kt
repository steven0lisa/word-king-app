package org.feichao.wordking.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.feichao.wordking.R
import org.feichao.wordking.WordKingApplication
import org.feichao.wordking.data.repository.UserConfigRepository
import org.feichao.wordking.data.repository.WordRepository
import org.feichao.wordking.databinding.FragmentHomeBinding
import org.feichao.wordking.ui.review.ReviewActivity
import org.feichao.wordking.ui.stats.ContributionHeatmapView
import org.feichao.wordking.util.Constants
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var wordRepository: WordRepository
    private lateinit var userConfigRepository: UserConfigRepository

    private var currentLanguage = Constants.Defaults.DEFAULT_LANGUAGE
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化Repository
        val database = WordKingApplication.instance.database
        wordRepository = WordRepository(database.wordDao())
        userConfigRepository = UserConfigRepository(database.userConfigDao())

        setupButtons()
        observeData()
    }

    private fun setupButtons() {
        // 点击统计卡片跳转到单词列表
        binding.cardStats.setOnClickListener {
            // 切换到底部导航的单词tab
            activity?.let { activity ->
                val bottomNav = activity.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                bottomNav?.selectedItemId = R.id.wordListFragment
            }
        }

        // 开始学习按钮
        binding.btnStartLearning.setOnClickListener {
            startActivity(Intent(requireContext(), ReviewActivity::class.java).apply {
                putExtra("mode", "learning")
            })
        }
    }

    private fun observeData() {
        // 观察用户配置
        viewLifecycleOwner.lifecycleScope.launch {
            userConfigRepository.getUserConfig().collect { config ->
                config?.let {
                    currentLanguage = it.currentLanguage

                    // 检查并自动生成单词
                    if (it.autoGenerateWord) {
                        checkAndGenerateWords()
                    }
                }
            }
        }

        // 加载统计数据
        loadStats()
    }

    private fun loadStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            // 待复习数量
            val reviewWords = wordRepository.getWordsNeedingReview(currentLanguage)
            binding.tvReviewCount.text = reviewWords.size.toString()

            // 已学习数量
            val learningCount = wordRepository.getLearningWordCount(currentLanguage)
            binding.tvLearnedCount.text = learningCount.toString()

            // 已掌握数量
            val masteredCount = wordRepository.getMasteredWordCount(currentLanguage)
            binding.tvMasteredCount.text = masteredCount.toString()

            // 词库总量
            val totalCount = wordRepository.getTotalWordCount(currentLanguage)
            binding.tvTotalCount.text = totalCount.toString()

            // 加载最近7天正确率
            loadAccuracy()

            // 加载热力图数据
            loadHeatmapData()
        }
    }

    private fun loadAccuracy() {
        viewLifecycleOwner.lifecycleScope.launch {
            val database = WordKingApplication.instance.database
            val learningRecordDao = database.learningRecordDao()

            // 最近7天
            val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000

            val correctCount = learningRecordDao.getCorrectCountSince(sevenDaysAgo)
            val totalCount = learningRecordDao.getTotalCountSince(sevenDaysAgo)

            val accuracy = if (totalCount > 0) (correctCount * 100 / totalCount) else 0

            binding.tvAccuracy.text = "$accuracy%"
            binding.progressAccuracy.progress = accuracy
        }
    }

    private fun loadHeatmapData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val database = WordKingApplication.instance.database
            val learningRecordDao = database.learningRecordDao()

            // 过去365天
            val oneYearAgo = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000
            val records = learningRecordDao.getRecordsBetween(oneYearAgo, System.currentTimeMillis())

            // 按日期统计学习次数
            val heatmapData = mutableMapOf<String, Int>()
            records.forEach { record ->
                val dateKey = dateFormat.format(record.reviewTime)
                heatmapData[dateKey] = (heatmapData[dateKey] ?: 0) + 1
            }

            binding.heatmapView.setData(heatmapData)
        }
    }

    private fun checkAndGenerateWords() {
        viewLifecycleOwner.lifecycleScope.launch {
            val unlearnedCount = wordRepository.getUnlearnedWordCount(currentLanguage)
            if (unlearnedCount < Constants.AiConfig.AUTO_GENERATE_THRESHOLD) {
                // 自动生成
                val result = org.feichao.wordking.service.AiGenerateService(requireContext()).generateWords(
                    currentLanguage,
                    Constants.AiConfig.DEFAULT_AUTO_GENERATE_COUNT,
                    "AUTO"
                )
                result.onSuccess { words ->
                    if (words.isNotEmpty()) {
                        wordRepository.insertWords(words)
                        loadStats()
                        Toast.makeText(
                            requireContext(),
                            "已自动补充${words.size}个单词",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
