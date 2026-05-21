package uz.alphazet.hoopla.ui.home

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class CubePageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        when {
            position <= -1f || position >= 1f -> {
                page.alpha = 0f
            }

            else -> {
                page.alpha = 1f - abs(position) * 0.25f
                page.pivotY = page.height / 2f
                page.pivotX = if (position < 0f) page.width.toFloat() else 0f
                // Positive sign → convex cube: bodies tilt forward on Z, hinge
                // recedes between them. Negative would be a concave book-fold.
                page.rotationY = 90f * position
            }
        }
    }
}
