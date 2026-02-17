package ru.netology.nework

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.addListener

class DrawTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val textToDraw = "NeWork"

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = Color.parseColor("#2196F3")
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#2196F3")
        alpha = 0
    }

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 120f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        style = Paint.Style.FILL
    }

    private data class LetterPath(
        val path: Path,
        val measure: PathMeasure,
        val length: Float,
        val positionX: Float
    )

    private val letterPaths = mutableListOf<LetterPath>()
    private val fullPath = Path()
    private val letterPositions = FloatArray(textToDraw.length)
    private var animationProgress = 0f
    private var fillProgress = 0f
    private var isDrawingComplete = false
    var onAnimationComplete: (() -> Unit)? = null
    private var textOffsetX = 0f
    private var textOffsetY = 0f

    init {
        createTextPaths()
        startDrawingAnimation()
    }

    private fun createTextPaths() {
        letterPaths.clear()
        fullPath.reset()

        val widths = FloatArray(textToDraw.length)
        textPaint.getTextWidths(textToDraw, widths)

        var currentX = 0f
        for (i in textToDraw.indices) {
            letterPositions[i] = currentX
            currentX += widths[i]
        }

        for (i in textToDraw.indices) {
            val tempPath = Path()
            textPaint.getTextPath(textToDraw, i, i + 1, 0f, 0f, tempPath)

            val matrix = Matrix()
            matrix.setTranslate(letterPositions[i], 0f)
            tempPath.transform(matrix)

            fullPath.addPath(tempPath)

            val measure = PathMeasure(tempPath, false)

            letterPaths.add(
                LetterPath(
                    path = Path(tempPath),
                    measure = measure,
                    length = measure.length,
                    positionX = letterPositions[i]
                )
            )
        }

        val totalWidth = if (letterPositions.isNotEmpty()) {
            val lastIndex = letterPositions.size - 1
            letterPositions[lastIndex] +
                    textPaint.measureText(textToDraw, lastIndex, lastIndex + 1)
        } else {
            0f
        }

        val bounds = Rect()
        textPaint.getTextBounds(textToDraw, 0, textToDraw.length, bounds)
        textOffsetX = (width - totalWidth) / 2f
        textOffsetY = (height + bounds.height()) / 2f
    }

    private fun startDrawingAnimation() {
        val drawAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3000
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animation ->
                animationProgress = animation.animatedValue as Float
                invalidate()
            }

            addListener(
                onEnd = {
                    isDrawingComplete = true
                    startFillAnimation()
                }
            )

            start()
        }
    }

    private fun startFillAnimation() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animation ->
                fillProgress = animation.animatedValue as Float
                fillPaint.alpha = (fillProgress * 255).toInt()
                invalidate()
            }

            addListener(
                onEnd = {
                    onAnimationComplete?.invoke()
                }
            )

            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val bounds = Rect()
        textPaint.getTextBounds(textToDraw, 0, textToDraw.length, bounds)

        val totalWidth = if (letterPositions.isNotEmpty()) {
            val lastIndex = letterPositions.size - 1
            letterPositions[lastIndex] +
                    textPaint.measureText(textToDraw, lastIndex, lastIndex + 1)
        } else {
            bounds.width().toFloat()
        }

        textOffsetX = (w - totalWidth) / 2f
        textOffsetY = (h + bounds.height()) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.translate(textOffsetX, textOffsetY)

        if (!isDrawingComplete) {
            for (i in letterPaths.indices) {
                val letter = letterPaths[i]
                val totalLetters = letterPaths.size
                val startProgress = i.toFloat() / totalLetters
                val endProgress = (i + 1).toFloat() / totalLetters
                val letterProgress = when {
                    animationProgress < startProgress -> 0f
                    animationProgress > endProgress -> 1f
                    else -> (animationProgress - startProgress) / (endProgress - startProgress)
                }

                if (letterProgress > 0) {
                    val drawPath = Path()
                    letter.measure.getSegment(0f, letterProgress * letter.length, drawPath, true)
                    canvas.drawPath(drawPath, strokePaint)
                }
            }
        } else {
            canvas.drawPath(fullPath, strokePaint)
        }

        if (fillProgress > 0) {
            val oldAlpha = fillPaint.alpha
            fillPaint.alpha = (fillProgress * oldAlpha).toInt()

            for (letter in letterPaths) {
                canvas.drawPath(letter.path, fillPaint)
            }

            fillPaint.alpha = oldAlpha
        }

        canvas.restore()
    }

    fun startAnimation() {
        animationProgress = 0f
        fillProgress = 0f
        isDrawingComplete = false
        startDrawingAnimation()
    }

    fun setStrokeColor(color: Int) {
        strokePaint.color = color
        invalidate()
    }

    fun setFillColor(color: Int) {
        fillPaint.color = color
        invalidate()
    }

    fun setStrokeWidth(width: Float) {
        strokePaint.strokeWidth = width
        invalidate()
    }

    fun setTextSize(size: Float) {
        textPaint.textSize = size
        createTextPaths()
        invalidate()
    }
}