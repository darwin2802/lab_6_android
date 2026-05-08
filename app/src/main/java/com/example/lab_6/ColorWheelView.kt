package com.example.lab_6

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class ColorWheelView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var radius = 0f
    private var centerX = 0f
    private var centerY = 0f
    private var markerX = 0f
    private var markerY = 0f
    private val markerRadius = 35f

    private var oldColor = Color.BLACK
    private var newColor = Color.RED

    private var currentHSV = floatArrayOf(0f, 1f, 1f)

    var onColorSelected: ((Int) -> Unit)? = null

    init {
        markerPaint.style = Paint.Style.STROKE
        markerPaint.color = Color.WHITE
        markerPaint.strokeWidth = 8f
        markerPaint.setShadowLayer(12f, 0f, 0f, Color.BLACK)

        textPaint.color = Color.parseColor("#EEEEEE")
        textPaint.textSize = 42f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.CENTER

        borderPaint.style = Paint.Style.STROKE
        borderPaint.color = Color.WHITE
        borderPaint.strokeWidth = 4f

        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setOldColor(color: Int) {
        oldColor = color
        newColor = color
        Color.colorToHSV(color, currentHSV)

        post {
            updateMarkerPositionByColor(color)
            invalidate()
        }
    }

    fun getSelectedColor(): Int = newColor

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        centerX = w / 2f
        centerY = h * 0.6f
        radius = (min(w, h) / 2.6f).coerceAtLeast(150f)

        updateMarkerPositionByColor(newColor)
    }

    private fun updateMarkerPositionByColor(color: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        val angle = Math.toRadians(hsv[0].toDouble())

        val r = if (hsv[2] < 1f) {
            radius * (0.7f + (1f - hsv[2]) * 0.3f)
        } else {
            radius * 0.7f * hsv[1]
        }

        markerX = centerX + (r * cos(angle)).toFloat()
        markerY = centerY + (r * sin(angle)).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        val rectHeight = 140f
        val rectWidth = 220f
        val topOffset = 100f

        rectPaint.color = oldColor
        canvas.drawRect(
            centerX - rectWidth - 30,
            topOffset,
            centerX - 30,
            topOffset + rectHeight,
            rectPaint
        )

        canvas.drawRect(
            centerX - rectWidth - 30,
            topOffset,
            centerX - 30,
            topOffset + rectHeight,
            borderPaint
        )

        canvas.drawText(
            "WAS",
            centerX - rectWidth / 2 - 30,
            topOffset - 30,
            textPaint
        )

        rectPaint.color = newColor

        canvas.drawRect(
            centerX + 30,
            topOffset,
            centerX + rectWidth + 30,
            topOffset + rectHeight,
            rectPaint
        )

        canvas.drawRect(
            centerX + 30,
            topOffset,
            centerX + rectWidth + 30,
            topOffset + rectHeight,
            borderPaint
        )

        canvas.drawText(
            "NEW",
            centerX + rectWidth / 2 + 30,
            topOffset - 30,
            textPaint
        )

        canvas.save()
        canvas.translate(centerX, centerY)

        val hueShader = SweepGradient(
            0f,
            0f,
            intArrayOf(
                Color.RED,
                Color.YELLOW,
                Color.GREEN,
                Color.CYAN,
                Color.BLUE,
                Color.MAGENTA,
                Color.RED
            ),
            null
        )

        val satShader = RadialGradient(
            0f,
            0f,
            radius * 0.7f,
            Color.WHITE,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )

        val valShader = RadialGradient(
            0f,
            0f,
            radius,
            intArrayOf(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                Color.BLACK
            ),
            floatOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )

        val combined = ComposeShader(hueShader, satShader, PorterDuff.Mode.SRC_OVER)
        val finalShader = ComposeShader(combined, valShader, PorterDuff.Mode.SRC_OVER)

        paint.shader = finalShader
        canvas.drawCircle(0f, 0f, radius, paint)

        canvas.restore()

        canvas.drawCircle(markerX, markerY, markerRadius, markerPaint)
    }

    private fun floatOf(vararg values: Float) = values

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val dx = event.x - centerX
        val dy = event.y - centerY
        val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val angleRad = atan2(dy.toDouble(), dx.toDouble())
            var hue = Math.toDegrees(angleRad).toFloat()

            if (hue < 0) hue += 360f

            if (distance <= radius * 0.7f) {
                currentHSV[0] = hue
                currentHSV[1] = (distance / (radius * 0.7f)).coerceIn(0f, 1f)
                currentHSV[2] = 1f
            } else {
                currentHSV[0] = hue
                currentHSV[1] = 1f

                val v = 1f - ((distance - radius * 0.7f) / (radius * 0.3f))
                    .coerceIn(0f, 1f)

                currentHSV[2] = v
            }

            val limitedDist = distance.coerceAtMost(radius)

            markerX = centerX + (cos(angleRad) * limitedDist).toFloat()
            markerY = centerY + (sin(angleRad) * limitedDist).toFloat()

            newColor = Color.HSVToColor(currentHSV)

            onColorSelected?.invoke(newColor)

            invalidate()
            return true
        }

        return true
    }
}