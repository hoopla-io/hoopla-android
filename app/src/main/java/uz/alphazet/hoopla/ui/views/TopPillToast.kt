package uz.alphazet.hoopla.ui.views

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.hoopla.databinding.ViewTopPillToastBinding

/**
 * A short confirmation that drops in over the top of the screen.
 *
 * Neither of the usual options fits: a `Toast` cannot be moved to the top from API 30, where a
 * custom view is ignored outright, and a `Snackbar` belongs to the bottom of a CoordinatorLayout.
 * So the pill is added straight to the activity's content frame and removed again when it is done.
 */
fun BaseFragment.showTopPill(@StringRes message: Int) = showTopPill(getString(message))

fun BaseFragment.showTopPill(message: CharSequence) {
    val content = activity?.findViewById<FrameLayout>(android.R.id.content) ?: return

    // A second confirmation restarts the one already showing rather than stacking on top of it.
    val existing = content.findViewWithTag<View>(TAG)
    val binding = if (existing != null) {
        ViewTopPillToastBinding.bind(existing)
    } else {
        ViewTopPillToastBinding.inflate(LayoutInflater.from(content.context), content, false).also {
            it.root.tag = TAG
            content.addView(it.root, topCenterParams(content))
        }
    }

    binding.message.text = message

    val pill = binding.root
    pill.removeCallbacks(pill.hideRunnable())

    if (existing == null) {
        pill.alpha = 0f
        pill.translationY = -pill.slideDistance()
    }
    pill.animate()
        .alpha(1f)
        .translationY(0f)
        .setDuration(ENTER_MS)
        .setInterpolator(DecelerateInterpolator())
        .start()

    pill.postDelayed(pill.hideRunnable(), HOLD_MS)
}

private fun topCenterParams(content: FrameLayout): FrameLayout.LayoutParams =
    FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        Gravity.TOP or Gravity.CENTER_HORIZONTAL
    ).apply {
        // The content frame reaches under the status bar, so the pill has to clear it itself.
        val statusBar = ViewCompat.getRootWindowInsets(content)
            ?.getInsets(WindowInsetsCompat.Type.systemBars())?.top ?: 0
        topMargin = statusBar + (MARGIN_DP * content.resources.displayMetrics.density).toInt()
        leftMargin = topMargin
        rightMargin = topMargin
    }

private fun View.slideDistance(): Float = SLIDE_DP * resources.displayMetrics.density

/**
 * One runnable per pill, cached on the view, so a re-show cancels the pending hide instead of
 * leaving an older one to pull the new message off screen early.
 */
private fun View.hideRunnable(): Runnable {
    (getTag(RUNNABLE_TAG) as? Runnable)?.let { return it }
    val runnable = Runnable {
        animate()
            .alpha(0f)
            .translationY(-slideDistance())
            .setDuration(EXIT_MS)
            .withEndAction { (parent as? ViewGroup)?.removeView(this) }
            .start()
    }
    setTag(RUNNABLE_TAG, runnable)
    return runnable
}

private const val TAG = "top_pill_toast"
private val RUNNABLE_TAG = uz.alphazet.hoopla.R.id.top_pill_hide_runnable
private const val ENTER_MS = 180L
private const val EXIT_MS = 160L
private const val HOLD_MS = 1600L
private const val SLIDE_DP = 32f
private const val MARGIN_DP = 12f
