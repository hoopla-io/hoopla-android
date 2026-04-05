package uz.alphazet.hoopla.ui.on_boarding

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import kotlinx.coroutines.delay
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenOnBoardingBinding
import uz.alphazet.hoopla.ui.MainActivity
import uz.alphazet.domain.R as domainR

class OnBoardingScreen : BaseFragment(R.layout.screen_on_boarding) {

    private val binding by viewBinding(ScreenOnBoardingBinding::bind)
    private var currentPage = 1

    override fun initialize() {

        val title2 = getString(domainR.string.your_coffee_is_waiting_for_you)

        val desc1 = getString(domainR.string.on_boarding_desc1)
        val desc2 = getString(domainR.string.on_boarding_desc2)

        animateEntrance()
        binding.desc.animateText(desc1)

        binding.next.setOnClickListener {
            when (currentPage) {
                1 -> {
                    animatePageTransition {
                        binding.image.setImageResource(domainR.drawable.intro_2)
                    }
                    animateIndicatorToPage2()
                    launch {
                        binding.title.animateText(title2)
                        delay(300)
                        binding.desc.animateText(desc2)
                    }
                    currentPage++
                }

                2 -> {
                    animateExit {
                        val intent = Intent(requireActivity(), MainActivity::class.java)
                        startActivity(intent)
                        @Suppress("DEPRECATION")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            requireActivity().overrideActivityTransition(
                                Activity.OVERRIDE_TRANSITION_OPEN,
                                R.anim.slide_in_right,
                                R.anim.slide_out_left
                            )
                        } else {
                            requireActivity().overridePendingTransition(
                                R.anim.slide_in_right,
                                R.anim.slide_out_left
                            )
                        }
                        requireActivity().finish()
                    }
                }
            }
        }

        binding.skip.setOnClickListener {
            animateExit {
                val intent = Intent(requireActivity(), MainActivity::class.java)
                startActivity(intent)
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    requireActivity().overrideActivityTransition(
                        Activity.OVERRIDE_TRANSITION_OPEN,
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                } else {
                    requireActivity().overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                }
                requireActivity().finish()
            }
        }

    }

    private fun animateEntrance() {
        val interpolator = DecelerateInterpolator(1.8f)
        val duration = 500L

        binding.skip.alpha = 0f
        binding.skip.translationX = 40f
        binding.skip.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(duration)
            .setStartDelay(200)
            .setInterpolator(interpolator)
            .start()

        binding.image.alpha = 0f
        binding.image.scaleX = 0.85f
        binding.image.scaleY = 0.85f
        binding.image.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setStartDelay(100)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        binding.contentContainer.alpha = 0f
        binding.contentContainer.translationY = 40f
        binding.contentContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(duration)
            .setStartDelay(350)
            .setInterpolator(interpolator)
            .start()

        binding.bottomContainer.alpha = 0f
        binding.bottomContainer.translationY = 30f
        binding.bottomContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(duration)
            .setStartDelay(500)
            .setInterpolator(interpolator)
            .start()
    }

    private fun animatePageTransition(onSwap: () -> Unit) {
        val interpolator = DecelerateInterpolator(1.5f)

        binding.image.animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .translationX(-80f)
            .setDuration(250)
            .setInterpolator(interpolator)
            .withEndAction {
                onSwap()
                binding.image.translationX = 80f
                binding.image.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationX(0f)
                    .setDuration(350)
                    .setInterpolator(OvershootInterpolator(1.0f))
                    .start()
            }
            .start()

        binding.contentContainer.animate()
            .alpha(0f)
            .translationY(20f)
            .setDuration(200)
            .withEndAction {
                binding.contentContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(350)
                    .setStartDelay(150)
                    .setInterpolator(interpolator)
                    .start()
            }
            .start()
    }

    private fun animateIndicatorToPage2() {
        val indicator1 = binding.indicator1
        val indicator2 = binding.indicator2

        val shrink1 = ValueAnimator.ofInt(
            indicator1.layoutParams.width,
            resources.getDimensionPixelSize(R.dimen.indicator_dot)
        ).apply {
            addUpdateListener {
                indicator1.layoutParams.width = it.animatedValue as Int
                indicator1.requestLayout()
            }
        }

        val grow2 = ValueAnimator.ofInt(
            indicator2.layoutParams.width,
            resources.getDimensionPixelSize(R.dimen.indicator_bar)
        ).apply {
            addUpdateListener {
                indicator2.layoutParams.width = it.animatedValue as Int
                indicator2.requestLayout()
            }
        }

        indicator1.setBackgroundResource(R.drawable.bg_indicator_inactive)
        indicator2.setBackgroundResource(R.drawable.bg_indicator_active)

        AnimatorSet().apply {
            playTogether(shrink1, grow2)
            duration = 300
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun animateExit(onComplete: () -> Unit) {
        val interpolator = DecelerateInterpolator(1.5f)
        val duration = 300L

        binding.image.animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .start()

        binding.contentContainer.animate()
            .alpha(0f)
            .translationY(30f)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .start()

        binding.bottomContainer.animate()
            .alpha(0f)
            .translationY(30f)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .start()

        binding.skip.animate()
            .alpha(0f)
            .setDuration(200)
            .start()

        binding.root.postDelayed({ onComplete() }, duration + 50)
    }

}