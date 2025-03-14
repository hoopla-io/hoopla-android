package uz.alphazet.hoopla.ui.on_boarding

import kotlinx.coroutines.delay
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.utils.startAnim
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenOnBoardingBinding
import uz.alphazet.hoopla.ui.Screens
import uz.alphazet.domain.R as domainR

class OnBoardingScreen : BaseFragment(R.layout.screen_on_boarding) {

    private val binding by viewBinding(ScreenOnBoardingBinding::bind)
    private var currentPage = 1

    override fun initialize() {

        val title1 = getString(uz.alphazet.domain.R.string.welcome_to_hoopla)
        val title2 = getString(uz.alphazet.domain.R.string.your_coffee_is_waiting_for_you)

        val desc1 = getString(uz.alphazet.domain.R.string.on_boarding_desc1)
        val desc2 = getString(uz.alphazet.domain.R.string.on_boarding_desc2)

        binding.desc.animateText(desc1)

        binding.next.setOnClickListener {
            when (currentPage) {
                1 -> {
                    launch {
                        binding.image.startAnim(domainR.anim.alpha1_0, 200, onEnd = {
                            binding.image.startAnim(domainR.anim.alpha0_1, 200, onStart = {
                                binding.image.setImageResource(domainR.drawable.intro_2)
                            }, onEnd = { })
                        })
                        binding.title.animateText(title2)
                        delay(300)
                        binding.desc.animateText(desc2)
                    }
                    currentPage++
                }

                2 -> {
                    replaceScreen(Screens.bottomNav())
                }
            }
//            when {
//                binding.desc.text.toString() == desc1 -> {
//                    launch {
//                        binding.image.startAnim(domainR.anim.alpha1_0, 200, onEnd = {
//                            binding.image.startAnim(domainR.anim.alpha0_1, 200, onStart = {
//                                binding.image.setImageResource(domainR.drawable.intro_2)
//                            }, onEnd = { })
//                        })
//                        binding.title.animateText(title2)
//                        delay(300)
//                        binding.desc.animateText(desc2)
//                    }
//                }
//
//                binding.desc.text.toString() == desc2 -> {
//                    replaceScreen(Screens.bottomNav())
//                }
//            }
        }

        binding.skip.setOnClickListener {
            replaceScreen(Screens.bottomNav())
        }

    }

}