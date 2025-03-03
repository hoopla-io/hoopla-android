package uz.i_tv.qahvazor.ui.on_boarding

import kotlinx.coroutines.delay
import uz.i_tv.domain.ui.BaseFragment
import uz.i_tv.domain.utils.startAnim
import uz.i_tv.domain.viewbinding.viewBinding
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ScreenOnBoardingBinding
import uz.i_tv.qahvazor.ui.Screens
import uz.i_tv.domain.R as domainR

class OnBoardingScreen : BaseFragment(R.layout.screen_on_boarding) {

    private val binding by viewBinding(ScreenOnBoardingBinding::bind)

    private val title1 = "Добро пожаловать в Hoopla"
    private val title2 = "Ваш кофе ждёт вас"

    private val desc1 =
        "Наслаждайтесь свежим и ароматным кофе каждый день. Удобно и без лишних усилий"
    private val desc2 = "Покажите QR-код и заберите свой напиток. Быстро, удобно и без очередей"

    override fun initialize() {

        binding.desc.animateText(desc1)

        binding.next.setOnClickListener {
            when {
                binding.desc.text.toString() == desc1 -> {
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
                }

                binding.desc.text.toString() == desc2 -> {
                    replaceScreen(Screens.bottomNav())
                }
            }
        }

        binding.skip.setOnClickListener {
            replaceScreen(Screens.bottomNav())
        }

    }

}