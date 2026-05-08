package com.example.lab_6

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

data class Layer(
    var bitmap: Bitmap,
    var canvas: Canvas,
    var name: String,
    var isVisible: Boolean = true,
    var opacity: Int = 255,
    var isBackground: Boolean = false,
    val textObjects: MutableList<DrawingView.TextObject> = mutableListOf()
) {
    val layerPaint: Paint
        get() = Paint().apply {
            alpha = opacity
            isAntiAlias = true
        }
}