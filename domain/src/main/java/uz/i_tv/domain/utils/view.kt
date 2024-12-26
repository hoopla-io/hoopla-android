package uz.i_tv.domain.utils

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

fun View.gone(): View {
    visibility = View.GONE
    return this
}

fun View.invisible(): View {
    visibility = View.INVISIBLE
    return this
}

fun View.visible(): View {
    visibility = View.VISIBLE
    return this
}

fun View.toggle(): Boolean {
    return if (visibility == View.VISIBLE) {
        visibility = View.INVISIBLE
        false
    } else {
        visibility = View.VISIBLE
        true
    }
}

fun View.enable(): View {
    isEnabled = true
    alpha = 1f
    isClickable = true
    return this
}

fun View.disable(): View {
    alpha = 0.3f
    isEnabled = false
    isClickable = false
    return this
}

fun View.makeNotClickable() {
    isClickable = false
    isFocusable = false
}

fun View.makeClickable() {
    isClickable = true
    isFocusable = true
}

fun View.disableAndClickable(): View {
    alpha = 0.3f
    return this
}

fun View.inDevelopment() {
    setOnClickListener {
        Toast.makeText(context, "In Development", Toast.LENGTH_SHORT).show()
    }
}

fun TextView.setTextColorRes(@ColorRes color: Int) =
    setTextColor(ContextCompat.getColor(context, color))

fun Context.getColorCompat(@ColorRes color: Int) = ContextCompat.getColor(this, color)

fun View.getYInRoot(): Int {
    return if (parent == rootView) top
    else top + (parent as View).getYInRoot()
}

fun View.getYInRootWithoutStatusBar(window: Window): Int {

    return if (parent == rootView) {
        val rectangle = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rectangle)
        top - rectangle.top
    } else top + (parent as View).getYInRoot()
}



