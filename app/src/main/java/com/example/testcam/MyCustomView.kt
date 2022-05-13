package com.example.testcam

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat

public class MyCustomView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val boundingBox: Rect = Rect()
    private val exclusions = listOf(boundingBox)

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        boundingBox.set(left, top, right, bottom)
        this.systemGestureExclusionRects = exclusions
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onDraw(canvas: Canvas) {
        this.systemGestureExclusionRects = exclusions
    }
}