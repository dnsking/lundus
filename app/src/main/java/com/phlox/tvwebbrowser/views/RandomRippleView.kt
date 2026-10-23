package com.phlox.tvwebbrowser.views
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.animation.AccelerateDecelerateInterpolator

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator

import android.graphics.*
import android.view.animation.AccelerateInterpolator
import kotlin.math.*
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.content.ContextCompat
import com.phlox.tvwebbrowser.service.ai.SearchResult
import kotlin.math.max
import kotlin.random.Random
import android.animation.AnimatorSet
import android.view.animation.DecelerateInterpolator


class RandomRippleView @JvmOverloads constructor(

    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var imageBitmap: Bitmap? = null
        set(value) {
            field = value
            updateScaledBitmap()
            invalidate()
        }

    var searchResults: List<SearchResult> = emptyList()
        set(value) {
            field = value
            resultsReceived = true
            isLoading = false
            invalidate()
        }

    // Which dot is currently selected via the ViewPager
    var selectedIndex: Int = -1
        set(value) {
            field = value
            invalidate()
        }

    private var scaledBitmap: Bitmap? = null
    private val density = resources.displayMetrics.density

    // Screenshot-style animation state
    private var imageScale = 1f
    private var imageAlpha = 255
    private var animationFinished = false

    // Loading state
    private var isLoading = false
    private var resultsReceived = false
    private var spinnerRotation = 0f
    private var lastSpinnerFrame = 0L

    // Paints
    private val bitmapPaint = Paint(
        Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG
    )

    private val dotNormalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }

    private val dotSelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        setShadowLayer(8f * density, 0f, 0f, Color.RED)
    }

    private val spinnerTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 55
        style = Paint.Style.STROKE
        strokeWidth = 4f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val spinnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val spinnerBounds = RectF()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int
    ) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateScaledBitmap()
    }

    private fun updateScaledBitmap() {
        val src = imageBitmap ?: run {
            scaledBitmap = null
            return
        }

        if (width <= 0 || height <= 0) return

        scaledBitmap = Bitmap.createScaledBitmap(
            src,
            width,
            height,
            true
        )
    }

    /**
     * Plays a screenshot-style pop-in animation.
     * When the animation finishes, the centered spinner appears
     * until searchResults is assigned.
     */
    fun startScanAndScaleAnimation(onComplete: () -> Unit) {
        imageScale = 0.92f
        imageAlpha = 0
        animationFinished = false
        isLoading = false
        resultsReceived = false
        spinnerRotation = 0f

        invalidate()

        val screenshotAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1450L
            interpolator = DecelerateInterpolator()

            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float

                // Quick screenshot-style zoom and fade-in
                imageScale = 0.92f + (0.08f * fraction)
                imageAlpha = (255 * fraction).toInt()

                invalidate()
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animationFinished = true

                    // Results might already have arrived during the animation.
                    isLoading = !resultsReceived

                    invalidate()
                    onComplete()
                }
            })
        }

        screenshotAnimator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val bitmap = scaledBitmap

        if (bitmap != null) {
            canvas.save()

            // Animate around the center, like a screenshot appearing.
            canvas.scale(
                imageScale,
                imageScale,
                width / 2f,
                height / 2f
            )

            bitmapPaint.alpha = imageAlpha
            canvas.drawBitmap(bitmap, 0f, 0f, bitmapPaint)

            canvas.restore()
        }

        // Show result markers only after the screenshot animation.
        if (animationFinished && !isLoading && imageBitmap != null) {
            val origBmp = imageBitmap!!

            val scaleX = width.toFloat() / origBmp.width
            val scaleY = height.toFloat() / origBmp.height

            searchResults.forEachIndexed { index, result ->
                val sx = result.x * scaleX
                val sy = result.y * scaleY

                if (index == selectedIndex) {
                    canvas.drawCircle(
                        sx,
                        sy,
                        12f * density,
                        dotSelectedPaint
                    )

                    canvas.drawCircle(
                        sx,
                        sy,
                        6f * density,
                        dotNormalPaint
                    )
                } else {
                    canvas.drawCircle(
                        sx,
                        sy,
                        8f * density,
                        dotNormalPaint
                    )
                }
            }
        }

        // Display the spinner in the center while waiting for results.
        if (animationFinished && isLoading) {
            drawSpinner(canvas)
            postInvalidateOnAnimation()
        }
    }

    private fun drawSpinner(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = 20f * density

        spinnerBounds.set(
            cx - radius,
            cy - radius,
            cx + radius,
            cy + radius
        )

        // Background ring
        canvas.drawArc(
            spinnerBounds,
            0f,
            360f,
            false,
            spinnerTrackPaint
        )

        // Rotating arc
        val now = SystemClock.uptimeMillis()

        if (lastSpinnerFrame == 0L) {
            lastSpinnerFrame = now
        }

        val delta = (now - lastSpinnerFrame).coerceAtMost(50L)
        lastSpinnerFrame = now

        spinnerRotation = (spinnerRotation + delta * 0.36f) % 360f

        canvas.drawArc(
            spinnerBounds,
            spinnerRotation,
            270f,
            false,
            spinnerPaint
        )
    }
}
