package uz.alphazet.domain.utils

object Constants {
    const val APP_CACHE = "app_cache"

    /**
     * Hidden hot-key: tapping the Profile header title this many times toggles
     * test mode on/off (drives the `x-hoopla-test` request header).
     */
    const val TEST_MODE_TAP_COUNT = 5

    const val NOTIFICATION_CHANNEL_ID = "music notification channel id 78"
    const val NOTIFICATION_CHANNEL_NAME = "music notification channel 78"
    const val NOTIFICATION_ID = 178

    const val ID = "id"
    const val DATA = "data"
    const val MODIFIERS = "modifiers"
    const val COMMENT = "comment"

    /**
     * The shop's weekly schedule, carried from the shop detail screen down to checkout so the
     * pickup-time picker can be constrained without a second shop request.
     */
    const val WORKING_HOURS = "working_hours"
}