package org.feichao.wordking.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.feichao.wordking.WordKingApplication
import org.feichao.wordking.data.repository.LearningRecordRepository
import org.feichao.wordking.data.repository.UserConfigRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatsFragment : Fragment() {

    private lateinit var userConfigRepository: UserConfigRepository
    private lateinit var learningRecordRepository: LearningRecordRepository

    private lateinit var tvTodayNew: TextView
    private lateinit var tvTodayReview: TextView
    private lateinit var tvAccuracy: TextView
    private lateinit var progressAccuracy: ProgressBar
    private lateinit var heatmapView: ContributionHeatmapView

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            org.feichao.wordking.R.layout.fragment_stats,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = WordKingApplication.instance.database
        userConfigRepository = UserConfigRepository(database.userConfigDao())
        learningRecordRepository = LearningRecordRepository(database.learningRecordDao())

        tvTodayNew = view.findViewById(org.feichao.wordking.R.id.tv_today_new)
        tvTodayReview = view.findViewById(org.feichao.wordking.R.id.tv_today_review)
        tvAccuracy = view.findViewById(org.feichao.wordking.R.id.tv_accuracy)
        progressAccuracy = view.findViewById(org.feichao.wordking.R.id.progress_accuracy)
        heatmapView = view.findViewById(org.feichao.wordking.R.id.heatmap_view)

        loadStats()
    }

    private fun loadStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            val config = userConfigRepository.getUserConfigSync()
            val language = config?.currentLanguage ?: "en"

            // 今日开始时间
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val todayStart = calendar.timeInMillis

            // 今日新单词数
            val newWords = learningRecordRepository.getNewWordCountSince(todayStart)
            tvTodayNew.text = newWords.toString()

            // 今日复习数
            val reviewWords = learningRecordRepository.getReviewWordCountSince(todayStart)
            tvTodayReview.text = reviewWords.toString()

            // 正确率
            val correctCount = learningRecordRepository.getCorrectCount()
            val totalCount = learningRecordRepository.getTotalCount()
            val accuracy = if (totalCount > 0) (correctCount * 100 / totalCount) else 0

            tvAccuracy.text = "$accuracy%"
            progressAccuracy.progress = accuracy

            // 加载热力图数据（过去365天）
            val oneYearAgo = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000
            val records = learningRecordRepository.getRecordsBetween(oneYearAgo, System.currentTimeMillis())

            // 按日期统计学习次数
            val heatmapData = mutableMapOf<String, Int>()
            records.forEach { record ->
                val dateKey = dateFormat.format(record.reviewTime)
                heatmapData[dateKey] = (heatmapData[dateKey] ?: 0) + 1
            }

            heatmapView.setData(heatmapData)
        }
    }
}
