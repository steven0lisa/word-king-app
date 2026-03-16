package org.feichao.wordking.ui.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import org.feichao.wordking.R
import java.util.Calendar

/**
 * GitLab风格的学习热力图
 * 显示最近一年的学习记录
 * 横向：列=周（从左到右），纵向：行=天（从上到下是周一到周日）
 */
class ContributionHeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 热力图颜色等级（从浅到深）
    private val colorLevels = listOf(
        0xFFEBEDF0.toInt(),                                    // 0次 - 浅灰
        ContextCompat.getColor(context, R.color.heat_level_1), // 1-5次
        ContextCompat.getColor(context, R.color.heat_level_2), // 6-10次
        ContextCompat.getColor(context, R.color.heat_level_3), // 11-20次
        ContextCompat.getColor(context, R.color.heat_level_4)  // 20+次
    )

    private val cellSize = 14.dpToPx()
    private val cellGap = 3.dpToPx()
    private val weeksToShow = 18  // 显示约4个月
    private val daysInWeek = 7
    private val weekdayLabelWidth = 16   // 星期标签宽度
    private val weekdayGap = 6           // 星期与网格之间的间距
    private val leftMargin = weekdayLabelWidth + weekdayGap  // 左侧总宽度
    private val topMargin = 28           // 顶部月份标签高度（增加1个字高的间距）

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = 9.dpToPx().toFloat()
    }

    // 存储每日学习次数：key是"yyyy-MM-dd"，value是次数
    private var data: Map<String, Int> = emptyMap()

    // 每周的起始日期（从过去约6个月开始，周一为起始）
    private val weekStartDates: List<Long> by lazy {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // 今天是这周的第几天（周一=2，周日=1）
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else (dayOfWeek - Calendar.MONDAY)

        // 往前推到最近一个周一
        val thisWeekMonday = calendar.timeInMillis - daysFromMonday * 24 * 60 * 60 * 1000L

        val result = mutableListOf<Long>()
        // 从约6个月前的周一开始
        var currentDate = thisWeekMonday - (weeksToShow - 1) * 7 * 24 * 60 * 60 * 1000L

        for (i in 0 until weeksToShow) {
            result.add(currentDate)
            currentDate += 7 * 24 * 60 * 60 * 1000L
        }
        result
    }

    fun setData(learningData: Map<String, Int>) {
        data = learningData
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val totalWidth = (cellSize + cellGap) * weeksToShow + cellGap + leftMargin + weekdayGap
        val totalHeight = (cellSize + cellGap) * daysInWeek + cellGap + topMargin
        setMeasuredDimension(totalWidth, totalHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val calendar = Calendar.getInstance()
        val monthLabels = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")

        var lastDrewMonth = -1

        // 绘制每一天的格子
        for (week in 0 until weeksToShow) {
            val weekStart = weekStartDates[week]

            // 绘制月份标签（每个月第一周）
            calendar.timeInMillis = weekStart
            val month = calendar.get(Calendar.MONTH)
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

            // 每月1日或第一周显示月份
            if (month != lastDrewMonth && dayOfMonth <= 7 && weekStart <= System.currentTimeMillis()) {
                lastDrewMonth = month
                val x = leftMargin + cellGap + week * (cellSize + cellGap)
                // 月份在顶部，与下方网格有margin间隔
                canvas.drawText(monthLabels[month], x.toFloat(), (topMargin - 4).toFloat(), textPaint)
            }

            for (day in 0 until daysInWeek) {
                val dateMillis = weekStart + day * 24 * 60 * 60 * 1000L

                // 跳过未来的日期
                if (dateMillis > System.currentTimeMillis()) continue

                calendar.timeInMillis = dateMillis
                val dateKey = String.format(
                    "%04d-%02d-%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH)
                )

                val count = data[dateKey] ?: 0
                val colorIndex = when {
                    count == 0 -> 0
                    count <= 5 -> 1
                    count <= 10 -> 2
                    count <= 20 -> 3
                    else -> 4
                }

                cellPaint.color = colorLevels[colorIndex]

                val x = leftMargin + cellGap + week * (cellSize + cellGap)
                val y = topMargin + cellGap + day * (cellSize + cellGap)

                // 绘制方格
                canvas.drawRect(
                    x.toFloat(), y.toFloat(),
                    (x + cellSize).toFloat(), (y + cellSize).toFloat(),
                    cellPaint
                )
            }
        }

        // 绘制星期标签（左对齐）- 与网格对齐，中间有gap隔开
        val dayLabels = listOf("一" to 0, "三" to 2, "五" to 4, "日" to 6)
        for ((label, dayIndex) in dayLabels) {
            val y = topMargin + cellGap + dayIndex * (cellSize + cellGap) + cellSize / 2 + 4
            // 星期标签在网格左边，有gap隔开
            canvas.drawText(label, 2f, y.toFloat(), textPaint)
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}
