package uz.alphazet.hoopla.ui.home

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class CubePageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        when {
            position <= -1f || position >= 1f -> {
                page.alpha = 0f
            }

            else -> {
                // No partial alpha here: a translucent full-screen page forces an offscreen
                // layer every frame of the swipe, which is what makes the cube stutter.
                page.alpha = 1f
                page.pivotY = page.height / 2f
                page.pivotX = if (position < 0f) page.width.toFloat() else 0f
                // Positive sign → convex cube: bodies tilt forward on Z, hinge
                // recedes between them. Negative would be a concave book-fold.
                page.rotationY = 90f * position
            }
        }
    }
}
