package uz.alphazet.hoopla.ui.profile

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uz.alphazet.hoopla.ui.BaseUiTest

/**
 * UI Automator tests for the Profile screen.
 *
 * Four independent language sources are tracked and verified against each other:
 *
 *  | Source       | How it is read                                         |
 *  |--------------|--------------------------------------------------------|
 *  | [deviceLang] | OS locale → "ru" / "en" / "uz"                        |
 *  | [prefLang]   | "app_cache" SharedPrefs → key "lang"                   |
 *  | [localeLang] | localehelper SharedPrefs → key "Locale.Helper.Selected.Language" |
 *  | [uiLang]     | reverse-lookup of the actual [header_title] text       |
 *
 * UI interactions are delegated to [ProfileRobot]. Language-source reads and
 * comparison logic stay here since they are not UI interactions.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ProfileScreenTest : BaseUiTest() {

    private val robot by lazy { ProfileRobot(device) }

    /** Device system locale mapped to a supported code. */
    private lateinit var deviceLang: String

    /** Language stored in AppCache SharedPreferences ("app_cache" / key "lang"). */
    private lateinit var prefLang: String

    /**
     * Language stored by localehelper.
     * File: "com.zeugmasolutions.localehelper.LocaleHelper"
     * Key:  "Locale.Helper.Selected.Language"
     */
    private lateinit var localeLang: String

    /** Language detected from the rendered [header_title] text. */
    private lateinit var uiLang: String

    /** Context locked to [prefLang] for resolving expected string values. */
    private lateinit var localizedContext: Context

    @Before
    override fun setUp() {
        super.setUp()

        deviceLang = resolveDeviceLang()
        prefLang   = readCachePrefLang()
        localeLang = readLocaleHelperLang()
        localizedContext = buildLocalizedContext(prefLang)

        launchApp()
        robot.navigateToProfile()

        // Derive uiLang AFTER navigation so the header is on screen.
        uiLang = readUiLang()
    }

    // -----------------------------------------------------------------------
    // Language consistency tests
    // -----------------------------------------------------------------------

    @Test
    fun cache_lang_matches_localehelper_lang() {
        assertEquals(
            "AppCache lang '$prefLang' ≠ localehelper lang '$localeLang'. " +
            "SplashActivity should call LocaleHelper.setLocale() on first install.",
            prefLang, localeLang
        )
    }

    @Test
    fun device_language_matches_sharedpref_language() {
        assertEquals(
            "Device lang '$deviceLang' ≠ pref lang '$prefLang'. " +
            "SplashActivity should have written the device locale on first install.",
            deviceLang, prefLang
        )
    }

    @Test
    fun ui_language_matches_sharedpref_language() {
        val expected = buildLocalizedContext(prefLang)
            .getString(uz.alphazet.domain.R.string.profile)
        assertEquals(
            "header_title shows '$uiLang' language but prefLang='$prefLang'. " +
            "localehelper='$localeLang', device='$deviceLang'.",
            prefLang, uiLang
        )
        assertEquals(
            "header_title text should be '$expected' for lang='$prefLang'",
            expected,
            robot.readHeaderText()
        )
    }

    @Test
    fun all_language_sources_are_in_sync() {
        val langs = mapOf(
            "device"       to deviceLang,
            "pref"         to prefLang,
            "localehelper" to localeLang,
            "ui"           to uiLang
        )
        assertTrue(
            "Language sources are out of sync: $langs",
            langs.values.distinct().size == 1
        )
    }

    // -----------------------------------------------------------------------
    // Screen content tests
    // -----------------------------------------------------------------------

    @Test
    fun profile_screen_shows_header_in_current_language() {
        val expected = localizedContext.getString(uz.alphazet.domain.R.string.profile)
        robot.assertHeaderText(expected)
    }

    @Test
    fun login_button_visible_when_logged_out() {
        robot.assertLoginButtonEnabledIfPresent()
    }

    @Test
    fun languages_row_is_visible() {
        robot.assertLanguagesRowVisible()
    }

    @Test
    fun privacy_policy_row_is_visible() {
        robot.assertPrivacyPolicyRowVisible()
    }

    @Test
    fun orders_tab_label_matches_current_language() {
        robot.assertOrdersTabVisible()
    }

    // -----------------------------------------------------------------------
    // Helpers — language-source reads (not UI interactions; stay in test class)
    // -----------------------------------------------------------------------

    /**
     * Language stored in AppCache SharedPreferences.
     * File: "app_cache"  Key: "lang"  Default: "ru"
     */
    private fun readCachePrefLang(): String =
        prefs("app_cache").getString("lang", "ru") ?: "ru"

    /**
     * Language stored by the localehelper library.
     * File: "com.zeugmasolutions.localehelper.LocaleHelper"
     * Key:  "Locale.Helper.Selected.Language"
     * Default: device locale mapped to supported code.
     */
    private fun readLocaleHelperLang(): String {
        val stored = prefs("com.zeugmasolutions.localehelper.LocaleHelper")
            .getString("Locale.Helper.Selected.Language", null)
        return if (stored != null) toSupportedLang(stored) else deviceLang
    }

    /**
     * Reverse-maps the [header_title] text to a supported language code.
     * "Профиль" → "ru", "Profile" → "en", "Profil" → "uz".
     */
    private fun readUiLang(): String {
        val text = robot.readHeaderText()
        return HEADER_TRANSLATIONS.entries
            .firstOrNull { it.value == text }
            ?.key ?: "ru"
    }

    companion object {
        /** All known translations of R.string.profile, keyed by language code. */
        private val HEADER_TRANSLATIONS = mapOf(
            "ru" to "Профиль",
            "en" to "Profile",
            "uz" to "Profil"
        )
    }
}