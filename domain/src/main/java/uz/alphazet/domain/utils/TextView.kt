package uz.alphazet.domain.utils

import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat


fun TextView.setTextColorRes(@ColorRes color: Int) =
    setTextColor(ContextCompat.getColor(context, color))

fun TextView.setTextStringRes(@StringRes stringRes: Int) =
    setText(stringRes)

fun TextView.setDrawableStart(@DrawableRes drawable: Int) =
    setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0)