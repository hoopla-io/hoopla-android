package uz.i_tv.domain.utils

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.Window
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.view.animation.ScaleAnimation
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AnimRes
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

fun View.scaleAndAlphaView(
    startScale: Float, endScale: Float, fromAlpha: Float, toAlpha: Float, time: Long
): Animation {
    val animationScale: Animation = ScaleAnimation(
        startScale, endScale,
        startScale, endScale,
        Animation.RELATIVE_TO_SELF, 0.6f,
        Animation.RELATIVE_TO_SELF, 0.6f
    )
    animationScale.fillAfter = true
    animationScale.duration = time
    animationScale.interpolator = OvershootInterpolator()
    startAnimation(animationScale)
    val animationAlpha: Animation = AlphaAnimation(fromAlpha, toAlpha)
    animationAlpha.fillAfter = true
    animationAlpha.duration = time
    startAnimation(animationAlpha)
    return animationScale
}


fun Animation.animationListener(
    onStart: (() -> Unit)? = null,
    onRepeat: (() -> Unit)? = null,
    onEnd: (() -> Unit)? = null,
) {
    this.setAnimationListener(object : AnimationListener {
        override fun onAnimationStart(p0: Animation?) {
            onStart?.invoke()
        }

        override fun onAnimationEnd(p0: Animation?) {
            onEnd?.invoke()
        }

        override fun onAnimationRepeat(p0: Animation?) {
            onRepeat?.invoke()
        }

    })
}

fun View.startAnim(
    @AnimRes id: Int,
    duration: Long? = null,
    onStart: (() -> Unit)? = null,
    onEnd: (() -> Unit)? = null,
    onRepeat: (() -> Unit)? = null
) {
    val animation = AnimationUtils.loadAnimation(this.context, id).apply {
        if (duration != null)
            setDuration(duration)
        setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                onStart?.invoke()
            }

            override fun onAnimationEnd(p0: Animation?) {
                onEnd?.invoke()
            }

            override fun onAnimationRepeat(p0: Animation?) {
                onRepeat?.invoke()
            }

        })
    }
    this.startAnimation(animation)
}



