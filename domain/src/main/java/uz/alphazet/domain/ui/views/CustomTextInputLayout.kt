package uz.alphazet.domain.ui.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.material.textfield.TextInputLayout
import uz.alphazet.domain.R
import uz.alphazet.domain.utils.getColorCompat
import uz.alphazet.domain.utils.setTextColorRes

class CustomTextInputLayout(context: Context, attrs: AttributeSet) :
    TextInputLayout(context, attrs) {

    init {
        initAttrs(attrs)
        initEditText()
    }


    private fun initAttrs(attrs: AttributeSet) {
        val inputCorner = dp2px(context, 12f)
        setBoxCornerRadii(inputCorner, inputCorner, inputCorner, inputCorner)

        boxBackgroundColor = ContextCompat.getColor(context, R.color.grey_200)
        boxStrokeWidth = dp2px(context, 0f).toInt()
        boxStrokeErrorColor =
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.error_75))
    }

    private fun initEditText() {


        addOnEditTextAttachedListener {
            editText?.apply {
                setOnFocusChangeListener { _, hasFocus ->
                    if (isErrorEnabled) return@setOnFocusChangeListener
                    updateDrawableTint(hasFocus)
                }


                doOnTextChanged { _, _, _, _ ->

                    isErrorEnabled = false


                }

                this.post {
                    updateDrawableTint(false)
                }
                isCursorVisible = true

            }

        }
    }

    private fun EditText.updateDrawableTint(hasFocus: Boolean) {
        val drawableTintColor = if (hasFocus || text.toString().isNotEmpty()) {
            context.getColorCompat(R.color.black_200)
        } else {
            context.getColorCompat(R.color.white_400)
        }
        for (drawable in compoundDrawables) {
            drawable?.colorFilter =
                PorterDuffColorFilter(drawableTintColor, PorterDuff.Mode.SRC_IN)
        }
    }

    private var onErrorEnableChangeListener: ((enabled: Boolean) -> Unit)? = null
    fun setOnErrorEnableChangeListener(l: (enabled: Boolean) -> Unit) {
        onErrorEnableChangeListener = l
    }

    override fun setErrorEnabled(enabled: Boolean) {
        super.setErrorEnabled(enabled)
        onErrorEnableChangeListener?.invoke(enabled)

        boxStrokeWidth = dp2px(context, 0f).toInt()
        val drawableTintColor = if (enabled) {
            boxBackgroundColor = ContextCompat.getColor(context, R.color.error_75)
            editText?.setHintTextColor(ContextCompat.getColorStateList(context, R.color.error_300))
            editText?.setTextColorRes(R.color.error_300)
            titleTextView?.setTextColorRes(R.color.error_300)
            context.getColorCompat(R.color.error_300)
        } else {
            boxBackgroundColor = ContextCompat.getColor(context, R.color.grey_200)
            editText?.setHintTextColor(ContextCompat.getColorStateList(context, R.color.grey_400))
            editText?.setTextColorRes(R.color.black_300)
            titleTextView?.setTextColorRes(R.color.black_300)
            context.getColorCompat(R.color.black_200)
        }

        editText?.let {
            for (drawable in it.compoundDrawables) {
                drawable?.colorFilter =
                    PorterDuffColorFilter(drawableTintColor, PorterDuff.Mode.SRC_IN)
            }

        }


        if (!enabled) {
            return
        }

        try {
            val layout = this;
            val errorView: TextView =
                ((this.getChildAt(1) as ViewGroup).getChildAt(0) as ViewGroup).getChildAt(0) as TextView

            (layout.getChildAt(1) as ViewGroup).layoutParams.width = LayoutParams.MATCH_PARENT
            (layout.getChildAt(1) as ViewGroup).getChildAt(0).layoutParams.width =
                FrameLayout.LayoutParams.MATCH_PARENT

            errorView.gravity = Gravity.START

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dp2px(
        context: Context,
        dp: Float
    ): Float {
        return (context.resources.displayMetrics.density * dp + 0.5f)
    }

    private var titleTextView: TextView? = null

}

