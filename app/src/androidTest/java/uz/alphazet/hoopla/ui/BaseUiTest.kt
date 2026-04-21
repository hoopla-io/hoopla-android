package uz.alphazet.hoopla.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Build
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Before
import java.util.Locale

/**
 * Base class for all UI Automator tests.
 *
 * Provides:
 *  - [device] — the singleton [UiDevice]
 *  - [launchApp] — presses Home, then launches the app from scratch and waits
 *    for the main window to appear
 *  - [waitForId] / [waitForText] — concise helpers that fail the test with a
 *    clear message when an element is not found within [TIMEOUT_MS]
 *
 * IMPORTANT: These tests require a connected device or running emulator with the
 * app installed.  Run them via:
 *   ./gradlew :app:connectedDebugAndroidTest
 */
abstract class BaseUiTest {

    protected lateinit var device: UiDevice
        private set

    @Before
    open fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Determine the true system locale (unaffected by localehelper overrides).
        val appLang = resolveDeviceLang()

        val ctx = InstrumentationRegistry.getInstrumentation().targetContext

        // Bypass the first-launch onboarding so tests always land on MainActivity,
        // and seed BOTH language stores so they are consistent with the device locale.
        ctx.getSharedPreferences("app_cache", Context.MODE_PRIVATE).edit()
            .putBoolean("isFirstTime", false)
            .putString("lang", appLang)
            .commit()

        // localehelper reads Locale.getDefault() after its one-time initialization.
        // Write the same value to its prefs AND update the JVM default so that
        // LocaleHelper.onAttach(context) returns the right locale for new Activities.
        ctx.getSharedPreferences("com.zeugmasolutions.localehelper.LocaleHelper", Context.MODE_PRIVATE)
            .edit()
            .putString("Locale.Helper.Selected.Language", appLang)
            .putString("Locale.Helper.Selected.Country", "")
            .commit()
        Locale.setDefault(Locale.forLanguageTag(appLang))

        // Wake the screen if it is off and dismiss the lock screen.
        if (!device.isScreenOn) {
            device.wakeUp()
            device.waitForIdle(IDLE_TIMEOUT_MS)
        }
        // Swipe up to dismiss keyguard (no-PIN lock screen).
        device.swipe(
            device.displayWidth / 2, device.displayHeight * 3 / 4,
            device.displayWidth / 2, device.displayHeight / 4,
            20
        )
        device.waitForIdle(IDLE_TIMEOUT_MS)

        // Dismiss any system dialogs (e.g. "App crashed") that could block tests.
        device.pressHome()
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    protected fun launchApp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val intent = context.packageManager
            .getLaunchIntentForPackage(APP_PACKAGE)!!
            .apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK) }

        context.startActivity(intent)

        // 1. Wait until the package window is in the foreground.
        val launched = device.wait(Until.hasObject(By.pkg(APP_PACKAGE).depth(0)), TIMEOUT_MS)
        assertNotNull("App did not launch within ${TIMEOUT_MS}ms", launched)

        // 2. Wait for the bottom navigation to be fully drawn — specifically wait for
        //    the "profile" menu item so navigateToProfile() can tap it immediately.
        //    MainActivity inflates bottom_nav after Koin + layout setup; on some
        //    devices the container appears before the items are in the a11y tree.
        val navReady = device.wait(Until.hasObject(By.res(APP_PACKAGE, "profile")), LAUNCH_READY_MS)
        assertNotNull(
            "profile nav item not visible within ${LAUNCH_READY_MS}ms — MainActivity may not have finished rendering",
            navReady
        )
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    /** Returns the object or fails the test if it is not visible within [TIMEOUT_MS]. */
    protected fun waitForId(resourceId: String, timeout: Long = TIMEOUT_MS): UiObject2 {
        val selector = By.res(APP_PACKAGE, resourceId)
        val found = device.wait(Until.findObject(selector), timeout)
        assertNotNull("View with id '$resourceId' not found within ${timeout}ms", found)
        return found!!
    }

    /** Returns the object or fails the test if it is not visible within [TIMEOUT_MS]. */
    protected fun waitForText(text: String, timeout: Long = TIMEOUT_MS): UiObject2 {
        val found = device.wait(Until.findObject(By.text(text)), timeout)
        assertNotNull("View with text '$text' not found within ${timeout}ms", found)
        return found!!
    }

    /** Returns the object or fails if text is not found as a substring. */
    protected fun waitForTextContains(substring: String, timeout: Long = TIMEOUT_MS): UiObject2 {
        val found = device.wait(Until.findObject(By.textContains(substring)), timeout)
        assertNotNull("No view containing text '$substring' found within ${timeout}ms", found)
        return found!!
    }

    // -----------------------------------------------------------------------
    // Shared helpers available to all subclasses
    // -----------------------------------------------------------------------

    /** Clears access + refresh tokens so the app starts in a logged-out state. */
    protected fun clearAuthTokens() {
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("app_cache", Context.MODE_PRIVATE)
            .edit()
            .remove("accessToken")
            .remove("refreshToken")
            .commit()
    }

    /** Opens a named SharedPreferences file in the target app context. */
    protected fun prefs(name: String): SharedPreferences =
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getSharedPreferences(name, Context.MODE_PRIVATE)

    /** Maps any language tag to a supported code: "uz", "en", or "ru" (default). */
    protected fun toSupportedLang(lang: String): String = when (lang) {
        "uz" -> "uz"
        "en" -> "en"
        else -> "ru"
    }

    /** Reads the true OS locale (unaffected by localehelper overrides). */
    protected fun resolveDeviceLang(): String {
        val systemLang = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Resources.getSystem().configuration.locales[0].language
        } else {
            @Suppress("DEPRECATION")
            Resources.getSystem().configuration.locale.language
        }
        return toSupportedLang(systemLang)
    }

    /** Creates a Context whose resources are locked to [lang]. */
    protected fun buildLocalizedContext(lang: String): Context {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val config = base.resources.configuration.apply { setLocale(Locale.forLanguageTag(lang)) }
        return base.createConfigurationContext(config)
    }

    /**
     * The language code currently stored in "app_cache" / "lang". Falls back to
     * the device locale then to "ru". Use this (not a hard-coded literal) when
     * your test needs to reference user-visible strings — otherwise the test
     * fails on any device whose locale differs from the test author's.
     */
    protected fun currentPrefLang(): String =
        prefs("app_cache").getString("lang", null) ?: resolveDeviceLang()

    /**
     * Returns a user-visible string in the active [currentPrefLang]. Use this
     * instead of hard-coded Russian/English snippets when asserting dialog
     * messages, headers, or other localized UI text.
     */
    protected fun localizedString(@StringRes id: Int): String =
        buildLocalizedContext(currentPrefLang()).getString(id)

    companion object {
        const val APP_PACKAGE = "uz.alphazet.hoopla"
        const val TIMEOUT_MS = 5_000L
        const val IDLE_TIMEOUT_MS = 1_000L

        /** Extra budget given to MainActivity to finish its first render after launch. */
        const val LAUNCH_READY_MS = 10_000L
    }
}