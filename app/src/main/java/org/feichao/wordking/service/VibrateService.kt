package org.feichao.wordking.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import org.feichao.wordking.util.Constants

/**
 * 振动反馈服务
 * 对应需求文档中的 vibrateForAnswer() 函数
 */
class VibrateService(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * 振动反馈
     * @param isCorrect 是否答对
     */
    fun vibrateForAnswer(isCorrect: Boolean) {
        if (!isVibrateEnabled()) return

        val duration = if (isCorrect) {
            Constants.Vibrate.CORRECT
        } else {
            Constants.Vibrate.INCORRECT
        }

        vibrate(duration)
    }

    /**
     * 执行振动
     */
    private fun vibrate(duration: Long) {
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(duration)
            }
        }
    }

    /**
     * 检查振动是否可用
     */
    private fun isVibrateEnabled(): Boolean {
        return vibrator?.hasVibrator() == true
    }
}
