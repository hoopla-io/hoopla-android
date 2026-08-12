package uz.alphazet.hoopla.ui

import uz.alphazet.domain.ui.BaseFragment

/** The hosting [MainActivity], or null while detached or hosted elsewhere (story viewer, auth). */
val BaseFragment.mainActivity: MainActivity?
    get() = activity as? MainActivity

/** Pushes [screen] onto the detail back stack; the bottom nav stays visible. */
fun BaseFragment.navigateTo(screen: BaseFragment) {
    mainActivity?.navigateTo(screen)
}

/**
 * The fragment replacement for finish(): routes through the back dispatcher so a screen's own
 * [BaseFragment.onBackPressed] logic runs for toolbar-back exactly as it does for system back.
 */
fun BaseFragment.popScreen() {
    activity?.onBackPressedDispatcher?.onBackPressed()
}
