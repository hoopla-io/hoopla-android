package uz.alphazet.hoopla.ui.shop_details

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uz.alphazet.hoopla.ui.BaseUiTest

/**
 * UI Automator tests for [ShopDetailActivity], reached **from** the Home
 * screen by tapping the first nearby shop in `partners_rv`. This is the
 * production navigation path — Home → ShopDetail.
 *
 * Tested flows:
 *  1. Tapping the first item in `partners_rv` opens [ShopDetailActivity]
 *     (asserted by the shop-detail toolbar).
 *  2. Once on the detail screen the core chrome is rendered — `app_bar`,
 *     `shop_name`, call FAB, direction FAB, `nested_scroll`,
 *     `drinks_container`.
 *  3. The `shop_name` TextView is populated with the title returned by the
 *     shop-detail endpoint.
 *  4. The toolbar back arrow finishes the activity and returns the user to
 *     the Home screen (asserted by the home `logo`).
 *
 * Prerequisites:
 *  - Device / emulator has the debug build installed.
 *  - Network is reachable.
 *  - GPS is enabled and the device's mock/real location resolves to at
 *     least one nearby shop on the dev backend. If none exist near you,
 *     `partners_rv` will stay empty and these tests will fail with a clear
 *     message.
 *  - Location permissions are pre-granted via `pm grant` in [setUp] so the
 *     runtime-permission dialog does not block tests.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ShopDetailFlowTest : BaseUiTest() {

    @Before
    override fun setUp() {
        clearAuthTokens()
        super.setUp()
        // Grant location permissions before the app starts so HomeScreen's
        // permission check passes and `runLocationListener()` is invoked
        // immediately. The androidx.test.rules artifact (GrantPermissionRule)
        // is not on this project's classpath, so we use `pm grant` instead.
        grantLocationPermissionsViaShell()
        launchApp()

        // Be explicit about the start tab — Home is default but tapping makes
        // the test resilient to any restored state.
        waitForId("home").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    // -----------------------------------------------------------------------
    // 1. Tapping a nearby shop opens ShopDetailActivity
    // -----------------------------------------------------------------------

    @Test
    fun tapping_first_nearby_shop_opens_shop_detail() {
        navigateFromHomeToFirstNearbyShop()

        assertNotNull(
            "shop-detail toolbar not visible after tapping a nearby shop",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "toolbar")), LAUNCH_READY_MS)
        )
    }

    // -----------------------------------------------------------------------
    // 2. Core chrome of ShopDetailActivity renders
    // -----------------------------------------------------------------------

    @Test
    fun shop_detail_chrome_renders_after_navigation_from_home() {
        navigateFromHomeToFirstNearbyShop()

        // Wait once for the activity, then assert structural elements.
        assertNotNull(
            "shop-detail toolbar not visible",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "toolbar")), LAUNCH_READY_MS)
        )
        assertNotNull(
            "app_bar missing on shop-detail",
            device.findObject(By.res(APP_PACKAGE, "app_bar"))
        )
        assertNotNull(
            "shop_name TextView missing in header",
            device.findObject(By.res(APP_PACKAGE, "shop_name"))
        )
        assertNotNull(
            "btn_call FAB missing",
            device.findObject(By.res(APP_PACKAGE, "btn_call"))
        )
        assertNotNull(
            "btn_direction FAB missing",
            device.findObject(By.res(APP_PACKAGE, "btn_direction"))
        )
        assertNotNull(
            "nested_scroll missing — drinks list cannot be hosted",
            device.findObject(By.res(APP_PACKAGE, "nested_scroll"))
        )
        assertNotNull(
            "drinks_container missing — drink sections have no host",
            device.findObject(By.res(APP_PACKAGE, "drinks_container"))
        )
    }

    // -----------------------------------------------------------------------
    // 3. shop_name populates after the backend returns
    // -----------------------------------------------------------------------

    @Test
    fun shop_name_populates_after_navigation_from_home() {
        navigateFromHomeToFirstNearbyShop()

        // Poll until shop_name has any non-empty text — collectData() sets it
        // from the ShopData payload after a network round-trip.
        val deadline = System.currentTimeMillis() + LAUNCH_READY_MS
        var loadedTitle: String? = null
        while (System.currentTimeMillis() < deadline) {
            val text = device.findObject(By.res(APP_PACKAGE, "shop_name"))?.text.orEmpty()
            if (text.isNotBlank()) {
                loadedTitle = text
                break
            }
            device.waitForIdle(IDLE_TIMEOUT_MS)
        }

        assertNotNull(
            "shop_name stayed empty — shop-detail endpoint did not return data",
            loadedTitle
        )
        assertTrue(
            "shop_name resolved to a blank string",
            loadedTitle!!.isNotBlank()
        )
    }

    // -----------------------------------------------------------------------
    // 4. Back arrow returns to Home
    // -----------------------------------------------------------------------

    @Test
    fun tapping_back_arrow_returns_to_home() {
        navigateFromHomeToFirstNearbyShop()

        // The toolbar nav icon's content description is @string/back (localized).
        val backArrow = device.wait(Until.findObject(By.desc(localizedString(uz.alphazet.domain.R.string.back))), TIMEOUT_MS)
        assertNotNull("toolbar back arrow (desc=@string/back) not found", backArrow)

        backArrow!!.click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        assertNotNull(
            "Home logo not visible after back from shop detail — did not return to Home",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "logo")), TIMEOUT_MS)
        )
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Grants the two location runtime permissions to the app under test. */
    private fun grantLocationPermissionsViaShell() {
        device.executeShellCommand("pm grant $APP_PACKAGE android.permission.ACCESS_FINE_LOCATION")
        device.executeShellCommand("pm grant $APP_PACKAGE android.permission.ACCESS_COARSE_LOCATION")
    }

    /**
     * Waits for `partners_rv` to populate with at least one nearby shop, then
     * taps the first item. Bubbles a clear failure message if either step
     * times out — both depend on GPS being on AND the backend returning a
     * non-empty list for the device's location.
     */
    private fun navigateFromHomeToFirstNearbyShop() {
        val rv = device.wait(
            Until.findObject(By.res(APP_PACKAGE, "partners_rv")),
            LAUNCH_READY_MS
        )
        assertNotNull(
            "partners_rv not visible — GPS may be off or the location-permission " +
                    "dialog was not auto-granted",
            rv
        )

        val firstItem: UiObject2? = waitForFirstNearbyShopItem(rv!!)
        assertNotNull(
            "No nearby shop items found inside partners_rv within ${LAUNCH_READY_MS}ms — " +
                    "backend may have returned an empty list for this location",
            firstItem
        )

        firstItem!!.click()
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    /**
     * Scoped lookup inside [partnersRv] for the first item's `image` view —
     * `image` is the most prominent child of an `item_partner` row and the
     * click bubbles up to the RecyclerView item handler that launches
     * [ShopDetailActivity].
     */
    private fun waitForFirstNearbyShopItem(partnersRv: UiObject2): UiObject2? {
        val deadline = System.currentTimeMillis() + LAUNCH_READY_MS
        while (System.currentTimeMillis() < deadline) {
            val firstImage = partnersRv.findObject(By.res(APP_PACKAGE, "image"))
            if (firstImage != null) return firstImage
            device.waitForIdle(IDLE_TIMEOUT_MS)
        }
        return null
    }
}