package org.feichao.wordking.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import com.airbnb.lottie.LottieAnimationView

/**
 * 炸屎效果View - 完整实现微信炸屎效果
 * 1. 炸弹抛物线飞行 (二阶贝塞尔曲线)
 * 2. 炸弹旋转
 * 3. 爆炸动画 (Lottie)
 * 4. 7个💩炸开效果
 */
class FecesExplosionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var fecesBitmap: Bitmap? = null
    private var bombBitmap: Bitmap? = null
    private val particleCount = 20  // 增加粒子数量
    private val fecesViews = mutableListOf<ImageView>()
    private var bombView: ImageView? = null
    private var lottieView: LottieAnimationView? = null
    private var isAnimating = false

    init {
        isClickable = false
        isFocusable = false

        try {
            fecesBitmap = BitmapFactory.decodeResource(context.resources, org.feichao.wordking.R.drawable.feces)
        } catch (e: Exception) {
            android.util.Log.e("FecesExplosion", "Failed to load feces bitmap", e)
        }

        try {
            bombBitmap = BitmapFactory.decodeResource(context.resources, org.feichao.wordking.R.drawable.bomb)
        } catch (e: Exception) {
            android.util.Log.e("FecesExplosion", "Failed to load bomb bitmap", e)
        }
    }

    /**
     * 炸屎入口方法
     */
    fun explode(targetX: Float, targetY: Float) {
        if (isAnimating) {
            clearAll()
        }

        android.util.Log.d("FecesExplosion", "Explode to ($targetX, $targetY)")
        isAnimating = true
        visibility = View.VISIBLE

        // 获取屏幕尺寸
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // 炸弹从屏幕右侧上方出发
        val startX = screenWidth - 80f
        val startY = screenHeight * 0.2f

        createBomb(startX, startY)
        playParabolaAnimation(startX, startY, targetX, targetY)
    }

    private fun createBomb(x: Float, y: Float) {
        bombView = ImageView(context).apply {
            setImageBitmap(bombBitmap)
            layoutParams = FrameLayout.LayoutParams(100, 100)
            this.x = x - 50
            this.y = y - 50
        }
        addView(bombView)
    }

    /**
     * 播放抛物线动画
     */
    private fun playParabolaAnimation(startX: Float, startY: Float, endX: Float, endY: Float) {
        // 控制点在中间偏上
        val controlX = (startX + endX) / 2 + 80
        val controlY = minOf(startY, endY) - 250

        android.util.Log.d("FecesExplosion", "Parabola: start=($startX,$startY) control=($controlX,$controlY) end=($endX,$endY)")

        // 使用ValueAnimator实现抛物线
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 700
        animator.interpolator = LinearInterpolator()

        // 旋转动画
        val rotationAnimator = ObjectAnimator.ofFloat(bombView!!, "rotation", 0f, 720f)
        rotationAnimator.duration = 700

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(animator, rotationAnimator)

        // 实时更新位置
        animator.addUpdateListener { animation ->
            val t = animation.animatedValue as Float
            val point = getBezierPoint(startX, startY, controlX, controlY, endX, endY, t)
            bombView?.x = point.x - 50
            bombView?.y = point.y - 50
        }

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                showExplosionAndFeces(endX, endY)
            }
        })

        animatorSet.start()
    }

    /**
     * 二阶贝塞尔曲线计算
     */
    private fun getBezierPoint(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, t: Float): PointF {
        val point = PointF()
        val oneMinusT = 1 - t
        // B(t) = (1-t)²P0 + 2(1-t)tP1 + t²P2
        point.x = oneMinusT * oneMinusT * x0 + 2 * t * oneMinusT * x1 + t * t * x2
        point.y = oneMinusT * oneMinusT * y0 + 2 * t * oneMinusT * y1 + t * t * y2
        return point
    }

    /**
     * 显示爆炸动画和炸屎效果
     */
    private fun showExplosionAndFeces(x: Float, y: Float) {
        android.util.Log.d("FecesExplosion", "showExplosionAndFeces at ($x, $y)")

        // 移除炸弹
        bombView?.let { removeView(it) }
        bombView = null

        // 爆炸动画大小
        val explosionSize = 250
        // 爆炸位置直接在传入的坐标上（正确答案按钮中心）
        val explosionParams = FrameLayout.LayoutParams(explosionSize, explosionSize).apply {
            leftMargin = (x - explosionSize / 2).toInt()
            topMargin = (y - explosionSize / 2).toInt()
        }

        // 创建Lottie爆炸动画
        lottieView = LottieAnimationView(context).apply {
            setAnimation("animations/bomb.json")
            layoutParams = explosionParams
        }
        addView(lottieView)
        lottieView?.playAnimation()

        // 炸屎动画提前100ms开始（与爆炸同时）
        handler.postDelayed({
            playFecesAnimation(x, y)
        }, 100)

        // Lottie结束后清理
        lottieView?.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                lottieView?.let { removeView(it) }
                lottieView = null
            }
        })
    }

    /**
     * 播放💩炸开动画
     */
    private fun playFecesAnimation(centerX: Float, centerY: Float) {
        android.util.Log.d("FecesExplosion", "playFecesAnimation at ($centerX, $centerY)")

        if (fecesBitmap == null) {
            android.util.Log.w("FecesExplosion", "Feces bitmap is null!")
            finishAnimation()
            return
        }

        // 获取屏幕尺寸计算扩散范围
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        // 120%屏幕宽度作为扩散范围（飞出屏幕外）
        val spreadRange = (screenWidth * 1.2f / 2).toInt()

        val fecesSize = 120  // 增大屎粒子大小

        // 创建粒子 - 从中心点开始
        for (i in 0 until particleCount) {
            // 所有粒子从中心出发
            val params = FrameLayout.LayoutParams(fecesSize, fecesSize).apply {
                leftMargin = (centerX - fecesSize / 2).toInt()
                topMargin = (centerY - fecesSize / 2).toInt()
            }

            // 随机角度和力度（向上抛物线）
            val angle = Math.random() * Math.PI * 2  // 360度方向
            val power = spreadRange * (0.5 + Math.random() * 0.5)  // 50%-100%力度

            val fecesView = ImageView(context).apply {
                setImageBitmap(fecesBitmap)
                layoutParams = params
                rotation = (Math.random() * 60 - 30).toFloat()
                scaleX = 0f
                scaleY = 0f
                alpha = 0f  // 初始不可见
            }

            // 保存目标和角度用于动画
            fecesView.tag = Triple(
                (Math.cos(angle) * power).toFloat(),  // targetX
                (Math.sin(angle) * power - spreadRange * 1.5f).toFloat(),  // targetY (向上更多)
                power
            )

            addView(fecesView)
            fecesViews.add(fecesView)
        }

        // 播放动画 - 抛物线飞出屏幕
        fecesViews.forEach { fecesView ->
            val (targetX, targetY, power) = fecesView.tag as Triple<Float, Float, Float>

            // 计算初速度（向上抛）
            val velocityX = targetX / 1500f * 50f  // 速度
            val velocityY = targetY / 1500f * 50f

            // 阶段1：快速出现 (0 -> 1)
            val appearAni = ObjectAnimator.ofFloat(fecesView, "alpha", 0f, 1f)

            // 阶段2：缩放出现
            val scaleXAni = ObjectAnimator.ofFloat(fecesView, "scaleX", 0f, 1f)
            val scaleYAni = ObjectAnimator.ofFloat(fecesView, "scaleY", 0f, 1f)

            // 阶段3：抛物线运动 (使用ValueAnimator)
            val flyAnimator = ValueAnimator.ofFloat(0f, 1f)
            flyAnimator.duration = 1500  // 慢一点，1.5秒

            val gravity = 800f  // 重力加速度

            flyAnimator.addUpdateListener { animation ->
                val t = animation.animatedValue as Float
                val time = t * 1.5f  // 秒

                // 抛物线公式：y = v0*t + 0.5*g*t^2
                val newX = velocityX * time * 30f  // X方向匀速
                val newY = velocityY * time * 30f + 0.5f * gravity * time * time  // Y方向加速

                fecesView.translationX = newX
                fecesView.translationY = newY

                // 渐变消失
                fecesView.alpha = 1f - t
            }

            // 出现动画 (150ms)
            val appearSet = AnimatorSet()
            appearSet.playTogether(appearAni, scaleXAni, scaleYAni)
            appearSet.duration = 150

            // 先出现后飞行
            appearSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    flyAnimator.start()
                }
            })

            appearSet.start()
        }

        handler.postDelayed({ finishAnimation() }, 1800)
    }

    private fun finishAnimation() {
        clearAll()
        isAnimating = false
        visibility = View.GONE
    }

    private fun clearAll() {
        bombView?.let {
            it.clearAnimation()
            removeView(it)
        }
        bombView = null

        lottieView?.let {
            it.cancelAnimation()
            removeView(it)
        }
        lottieView = null

        fecesViews.forEach { view ->
            view.clearAnimation()
            removeView(view)
        }
        fecesViews.clear()
    }

    companion object {
        fun attachToWindow(activity: Activity): FecesExplosionView {
            val rootView = activity.findViewById<ViewGroup>(Window.ID_ANDROID_CONTENT)
            val explosionView = FecesExplosionView(activity)
            explosionView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            explosionView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            rootView.addView(explosionView)
            return explosionView
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    fun onDestroy() {
        clearAll()
        handler.removeCallbacksAndMessages(null)
    }
}
