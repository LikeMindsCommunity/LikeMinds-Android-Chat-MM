package com.likeminds.chatmm.media.customviews

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.drawable.Drawable

class WrappedDrawable(private val drawable: Drawable) : Drawable() {

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        //update bounds to get correctly
        super.setBounds(left, top, right, bottom)
        val drawable: Drawable = drawable
        drawable.setBounds(left, top, right, bottom)
    }

    override fun setAlpha(alpha: Int) {
        val drawable: Drawable = drawable
        drawable.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        val drawable: Drawable = drawable
        drawable.colorFilter = colorFilter
    }

    override fun getOpacity(): Int {
        val drawable: Drawable = drawable
        return drawable.opacity
    }

    override fun draw(canvas: Canvas) {
        val drawable: Drawable = drawable
        drawable.draw(canvas)
    }

    override fun getIntrinsicWidth(): Int {
        val drawable: Drawable = drawable
        return drawable.bounds.width() ?: 0
    }

    override fun getIntrinsicHeight(): Int {
        val drawable: Drawable = drawable
        return drawable.bounds.height() ?: 0
    }
}