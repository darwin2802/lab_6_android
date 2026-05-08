package com.example.lab_6

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import java.util.*

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    data class DrawPath(
        val path: Path,
        val paint: Paint,
        val isEraser: Boolean = false
    )

    data class TextObject(
        var content: String,
        var x: Float,
        var y: Float,
        var color: Int,
        var size: Float,
        val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    )

    data class Layer(
        var bitmap: Bitmap,
        var canvas: Canvas,
        var isVisible: Boolean = true,
        var name: String = "",
        val textObjects: MutableList<TextObject> = mutableListOf(),
        var isBackground: Boolean = false,
        val pathHistory: MutableList<DrawPath> = mutableListOf()
    )

    private var canvasW = 0
    private var canvasH = 0

    val layers = mutableListOf<Layer>()
    var currentLayerIndex = -1

    private var drawPath = Path()

    var drawPaint = Paint()
    private val canvasPaint = Paint(Paint.DITHER_FLAG)

    private val appBackgroundPaint = Paint().apply {
        color = Color.parseColor("#333333")
    }

    private val canvasShadowPaint = Paint().apply {
        color = Color.BLACK
        alpha = 60
    }

    private var currentColor = Color.BLACK
    private var currentAlpha = 255
    private var brushSize = 20f

    private var isEraser = false
    private var isFillMode = false
    private var isTextMode = false

    private var selectedText: TextObject? = null
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private var viewMatrix = Matrix()
    private var inverseMatrix = Matrix()
    private val scaleDetector: ScaleGestureDetector

    private var lastPanX = 0f
    private var lastPanY = 0f
    private var isMultiTouching = false

    init {
        setupPaint()

        scaleDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val scaleFactor = detector.scaleFactor

                    viewMatrix.postScale(
                        scaleFactor,
                        scaleFactor,
                        detector.focusX,
                        detector.focusY
                    )

                    invalidate()
                    return true
                }
            }
        )
    }

    private fun setupPaint() {
        drawPaint.isAntiAlias = true
        drawPaint.color = currentColor
        drawPaint.alpha = currentAlpha
        drawPaint.strokeWidth = brushSize
        drawPaint.style = Paint.Style.STROKE
        drawPaint.strokeJoin = Paint.Join.ROUND
        drawPaint.strokeCap = Paint.Cap.ROUND
    }

    fun initCanvas(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return

        canvasW = w
        canvasH = h

        layers.clear()

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.WHITE)

        val backgroundLayer = Layer(bitmap, canvas, true, "Background")
        backgroundLayer.isBackground = true

        layers.add(backgroundLayer)
        currentLayerIndex = 0

        viewMatrix.reset()

        post {
            val scale =
                (width.toFloat() / w).coerceAtMost(height.toFloat() / h) * 0.75f

            viewMatrix.postTranslate(
                (width - w * scale) / 2f,
                (height - h * scale) / 2f
            )

            viewMatrix.postScale(
                scale,
                scale,
                width / 2f,
                height / 2f
            )

            invalidate()
        }
    }

    fun addLayer(w: Int, h: Int, name: String? = null) {
        val bitmap =
            Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)

        val layerName = name ?: "Layer ${layers.size + 1}"

        layers.add(0, Layer(bitmap, canvas, true, layerName))

        currentLayerIndex = 0

        invalidate()
    }

    fun mergeLayersDown(index: Int) {
        if (index < 0 || index >= layers.size - 1) return

        val topLayer = layers[index]
        val bottomLayer = layers[index + 1]

        bottomLayer.canvas.drawBitmap(topLayer.bitmap, 0f, 0f, null)

        bottomLayer.textObjects.addAll(topLayer.textObjects)

        bottomLayer.pathHistory.addAll(topLayer.pathHistory)

        layers.removeAt(index)

        currentLayerIndex =
            index.coerceAtMost(layers.size - 1)

        invalidate()
    }

    fun addImageToCanvas(bitmap: Bitmap) {
        if (currentLayerIndex == -1) return

        val currentLayer = layers[currentLayerIndex]

        currentLayer.canvas.drawBitmap(
            bitmap,
            0f,
            0f,
            Paint(Paint.FILTER_BITMAP_FLAG)
        )

        invalidate()
    }

    fun setBrushType(type: String) {
        isFillMode = false
        isEraser = false
        isTextMode = false

        drawPaint.xfermode = null
        drawPaint.maskFilter = null
        drawPaint.pathEffect = null
        drawPaint.shader = null
        drawPaint.strokeCap = Paint.Cap.ROUND

        when (type) {

            "MARKER" -> {
                drawPaint.strokeCap = Paint.Cap.SQUARE
                drawPaint.alpha = 130
            }

            "AIRBRUSH" -> {
                drawPaint.maskFilter =
                    BlurMaskFilter(
                        brushSize / 2f,
                        BlurMaskFilter.Blur.NORMAL
                    )

                drawPaint.alpha = 80
            }

            "WATERCOLOR" -> {
                drawPaint.maskFilter =
                    BlurMaskFilter(
                        brushSize / 1.5f,
                        BlurMaskFilter.Blur.NORMAL
                    )

                drawPaint.alpha = 60
            }

            "PENCIL" -> {
                drawPaint.pathEffect =
                    DiscretePathEffect(5f, 2f)

                drawPaint.alpha = 180
            }

            "PASTEL" -> {
                val textureSize = 30

                val textureBmp =
                    Bitmap.createBitmap(
                        textureSize,
                        textureSize,
                        Bitmap.Config.ARGB_8888
                    )

                val c = Canvas(textureBmp)

                val p = Paint().apply {
                    color = currentColor
                    alpha = 40
                }

                val random = Random()

                for (i in 0..20) {
                    c.drawCircle(
                        random.nextFloat() * 30,
                        random.nextFloat() * 30,
                        2f,
                        p
                    )
                }

                drawPaint.shader =
                    BitmapShader(
                        textureBmp,
                        Shader.TileMode.REPEAT,
                        Shader.TileMode.REPEAT
                    )
            }

            else -> {
                drawPaint.alpha = currentAlpha
                drawPaint.strokeWidth = brushSize
            }
        }
    }

    fun setEraserMode(e: Boolean) {
        isEraser = e
        isFillMode = false
        isTextMode = false

        if (e) {
            val layer =
                if (currentLayerIndex != -1)
                    layers[currentLayerIndex]
                else null

            if (layer?.isBackground == true) {
                drawPaint.xfermode = null
                drawPaint.color = Color.WHITE
            } else {
                drawPaint.xfermode =
                    PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }

        } else {
            drawPaint.xfermode = null
            drawPaint.color = currentColor
            drawPaint.alpha = currentAlpha
        }
    }

    fun undo() {
        if (currentLayerIndex != -1) {
            val layer = layers[currentLayerIndex]

            if (layer.pathHistory.isNotEmpty()) {
                layer.pathHistory.removeAt(
                    layer.pathHistory.size - 1
                )

                redrawLayer(layer)

                invalidate()
            }
        }
    }

    private fun redrawLayer(layer: Layer) {
        layer.bitmap.eraseColor(
            if (layer.isBackground)
                Color.WHITE
            else
                Color.TRANSPARENT
        )

        for (dp in layer.pathHistory) {
            layer.canvas.drawPath(dp.path, dp.paint)
        }
    }

    override fun onDraw(canvas: Canvas) {

        canvas.drawRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            appBackgroundPaint
        )

        canvas.save()

        canvas.concat(viewMatrix)

        canvas.drawRect(
            10f,
            10f,
            canvasW.toFloat() + 10f,
            canvasH.toFloat() + 10f,
            canvasShadowPaint
        )

        canvas.clipRect(
            0f,
            0f,
            canvasW.toFloat(),
            canvasH.toFloat()
        )

        canvas.drawColor(Color.WHITE)

        for (i in layers.indices.reversed()) {

            val layer = layers[i]

            if (layer.isVisible) {

                canvas.drawBitmap(
                    layer.bitmap,
                    0f,
                    0f,
                    canvasPaint
                )

                for (textObj in layer.textObjects) {

                    textObj.paint.color = textObj.color
                    textObj.paint.textSize = textObj.size

                    if (textObj == selectedText && isTextMode) {

                        val highlightPaint = Paint().apply {
                            color = Color.parseColor("#4000BFFF")
                            style = Paint.Style.FILL
                        }

                        canvas.drawRect(
                            textObj.x,
                            textObj.y - textObj.size,
                            textObj.x + (
                                    textObj.content.length *
                                            textObj.size *
                                            0.6f
                                    ),
                            textObj.y + 10f,
                            highlightPaint
                        )
                    }

                    canvas.drawText(
                        textObj.content,
                        textObj.x,
                        textObj.y,
                        textObj.paint
                    )
                }
            }
        }

        if (
            !isFillMode &&
            !isTextMode &&
            currentLayerIndex != -1 &&
            !isMultiTouching
        ) {
            canvas.drawPath(drawPath, drawPaint)
        }

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        scaleDetector.onTouchEvent(event)

        val pointerCount = event.pointerCount

        if (pointerCount >= 2) {

            isMultiTouching = true

            val centerX =
                (event.getX(0) + event.getX(1)) / 2

            val centerY =
                (event.getY(0) + event.getY(1)) / 2

            if (event.actionMasked == MotionEvent.ACTION_MOVE) {

                val dx = centerX - lastPanX
                val dy = centerY - lastPanY

                if (!scaleDetector.isInProgress) {
                    viewMatrix.postTranslate(dx, dy)
                }
            }

            lastPanX = centerX
            lastPanY = centerY

            invalidate()

            return true
        }

        if (event.action == MotionEvent.ACTION_UP) {
            isMultiTouching = false
        }

        if (isMultiTouching) return true

        viewMatrix.invert(inverseMatrix)

        val pts = floatArrayOf(event.x, event.y)

        inverseMatrix.mapPoints(pts)

        val mX = pts[0]
        val mY = pts[1]

        val isInsideCanvas =
            mX in 0f..canvasW.toFloat() &&
                    mY in 0f..canvasH.toFloat()

        when (event.action) {

            MotionEvent.ACTION_DOWN -> {

                if (isInsideCanvas) {

                    if (isTextMode) {

                        selectedText = findTextAt(mX, mY)

                        lastTouchX = mX
                        lastTouchY = mY

                    } else if (isFillMode) {

                        performFloodFill(
                            mX.toInt(),
                            mY.toInt()
                        )

                    } else {

                        drawPath.moveTo(mX, mY)
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {

                if (isTextMode && selectedText != null) {

                    selectedText!!.x += (mX - lastTouchX)

                    selectedText!!.y += (mY - lastTouchY)

                    lastTouchX = mX
                    lastTouchY = mY

                } else if (!isTextMode && !isFillMode) {

                    drawPath.lineTo(mX, mY)
                }
            }

            MotionEvent.ACTION_UP -> {

                if (
                    !isTextMode &&
                    !isFillMode &&
                    currentLayerIndex != -1
                ) {

                    val layer = layers[currentLayerIndex]

                    val pathCopy = Path(drawPath)

                    val paintCopy = Paint(drawPaint)

                    layer.pathHistory.add(
                        DrawPath(
                            pathCopy,
                            paintCopy,
                            isEraser
                        )
                    )

                    layer.canvas.drawPath(
                        drawPath,
                        drawPaint
                    )

                    drawPath.reset()
                }
            }
        }

        invalidate()

        return true
    }

    private fun findTextAt(
        x: Float,
        y: Float
    ): TextObject? {

        if (currentLayerIndex == -1)
            return null

        val layer = layers[currentLayerIndex]

        for (textObj in layer.textObjects.reversed()) {

            val rect = RectF(
                textObj.x,
                textObj.y - textObj.size,
                textObj.x + (
                        textObj.content.length *
                                textObj.size *
                                0.6f
                        ),
                textObj.y
            )

            if (rect.contains(x, y))
                return textObj
        }

        return null
    }

    private fun performFloodFill(
        x: Int,
        y: Int
    ) {

        val bmp = layers[currentLayerIndex].bitmap

        if (
            x !in 0 until bmp.width ||
            y !in 0 until bmp.height
        ) return

        val targetColor = bmp.getPixel(x, y)

        if (targetColor == currentColor)
            return

        val pixels =
            IntArray(bmp.width * bmp.height)

        bmp.getPixels(
            pixels,
            0,
            bmp.width,
            0,
            0,
            bmp.width,
            bmp.height
        )

        val queue: Queue<Point> = LinkedList()

        queue.add(Point(x, y))

        while (queue.isNotEmpty()) {

            val p = queue.poll()!!

            val idx = p.y * bmp.width + p.x

            if (pixels[idx] == targetColor) {

                pixels[idx] = currentColor

                if (p.x > 0)
                    queue.add(Point(p.x - 1, p.y))

                if (p.x < bmp.width - 1)
                    queue.add(Point(p.x + 1, p.y))

                if (p.y > 0)
                    queue.add(Point(p.x, p.y - 1))

                if (p.y < bmp.height - 1)
                    queue.add(Point(p.x, p.y + 1))
            }
        }

        bmp.setPixels(
            pixels,
            0,
            bmp.width,
            0,
            0,
            bmp.width,
            bmp.height
        )

        invalidate()
    }

    fun getBitmap(): Bitmap {

        if (canvasW <= 0 || canvasH <= 0) {

            return Bitmap.createBitmap(
                1,
                1,
                Bitmap.Config.ARGB_8888
            )
        }

        val result =
            Bitmap.createBitmap(
                canvasW,
                canvasH,
                Bitmap.Config.ARGB_8888
            )

        val canvas = Canvas(result)

        for (i in layers.indices.reversed()) {

            val layer = layers[i]

            if (layer.isVisible) {

                canvas.drawBitmap(
                    layer.bitmap,
                    0f,
                    0f,
                    null
                )

                for (t in layer.textObjects) {

                    val p =
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = t.color
                            textSize = t.size
                        }

                    canvas.drawText(
                        t.content,
                        t.x,
                        t.y,
                        p
                    )
                }
            }
        }

        return result
    }

    fun clearAll() {
        layers.clear()

        currentLayerIndex = -1

        viewMatrix.reset()

        invalidate()
    }

    fun setDimensions(w: Int, h: Int) {
        canvasW = w
        canvasH = h
    }

    fun loadLayer(
        bmp: Bitmap,
        name: String,
        visible: Boolean,
        isBg: Boolean
    ) {

        val mutableBmp =
            bmp.copy(Bitmap.Config.ARGB_8888, true)

        val canvas = Canvas(mutableBmp)

        val layer =
            Layer(
                mutableBmp,
                canvas,
                visible,
                name
            )

        layer.isBackground = isBg

        layers.add(layer)

        if (currentLayerIndex == -1)
            currentLayerIndex = 0

        invalidate()
    }

    fun setFillMode(f: Boolean) {
        isFillMode = f
        isEraser = false
        isTextMode = false
    }

    fun setTextMode(t: Boolean) {
        isTextMode = t
        isEraser = false
        isFillMode = false
    }

    fun setColor(c: Int) {
        currentColor = c

        drawPaint.color = c

        if (isEraser)
            setEraserMode(true)
    }

    fun setBrushSize(s: Float) {
        brushSize = s
        drawPaint.strokeWidth = s
    }

    fun setBrushAlpha(a: Int) {
        currentAlpha = a
        drawPaint.alpha = a
    }

    fun addTextObject(content: String) {

        if (currentLayerIndex != -1) {

            layers[currentLayerIndex]
                .textObjects
                .add(
                    TextObject(
                        content,
                        canvasW / 4f,
                        canvasH / 4f,
                        currentColor,
                        brushSize * 3
                    )
                )

            invalidate()
        }
    }

    fun getSelectedText() = selectedText

    fun removeSelectedText() {

        if (currentLayerIndex != -1) {

            layers[currentLayerIndex]
                .textObjects
                .remove(selectedText)
        }

        selectedText = null

        invalidate()
    }
}