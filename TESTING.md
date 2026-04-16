# Hoopla — Test Suite Documentation

## Overview

The project has three layers of tests matching the three-module architecture.

| Layer | Module | Framework | Runs on | Count |
|-------|--------|-----------|---------|-------|
| Unit — core logic | `:domain` | JUnit 4 + MockK + Robolectric | JVM | **67** |
| Unit — API / models | `:data` | JUnit 4 + MockWebServer | JVM | **70** |
| Unit — ViewModels + UI | `:app` | JUnit 4 + MockK + Turbine + Robolectric | JVM | **47** |
| UI Automation | `:app` | UI Automator | Device / Emulator | **23** |
| **Total JVM** | | | | **184** |

---

## Running the Tests

### All JVM unit tests (no device required)

```bash
./gradlew :domain:testDebugUnitTest :data:testDebugUnitTest :app:testDebugUnitTest
```

### Single module

```bash
./gradlew :domain:testDebugUnitTest
./gradlew :data:testDebugUnitTest
./gradlew :app:testDebugUnitTest
```

### Single test class

```bash
./gradlew :app:testDebugUnitTest \
  --tests "uz.alphazet.hoopla.ui.auth.AuthVMTest"
```

### UI Automator (requires connected device or running emulator)

```bash
./gradlew :app:connectedDebugAndroidTest
```

### Single UI Automator class

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=uz.alphazet.hoopla.ui.auth.AuthFlowTest
```

### HTML reports

After running, reports are at:

```
app/build/reports/tests/testDebugUnitTest/index.html
domain/build/reports/tests/testDebugUnitTest/index.html
data/build/reports/tests/testDebugUnitTest/index.html
```

---

## Test Dependencies

All versions live in `gradle/libs.versions.toml`.

| Library | Version | Used in |
|---------|---------|---------|
| `io.mockk:mockk` | 1.13.13 | `:domain`, `:app` |
| `app.cash.turbine:turbine` | 1.1.0 | `:domain`, `:app` |
| `kotlinx-coroutines-test` | 1.9.0 | `:domain`, `:data`, `:app` |
| `org.robolectric:robolectric` | 4.13 | `:domain`, `:app` |
| `androidx.test:core` | 1.6.1 | `:domain`, `:app` |
| `okhttp3:mockwebserver` | 4.11.0 | `:data` |
| `androidx.fragment:fragment-testing` | 1.8.5 | `:app` testImplementation |
| `androidx.fragment:fragment-testing-manifest` | 1.8.5 | `:app` debugImplementation |
| `io.insert-koin:koin-test-junit4` | 3.4.3 | `:app` |
| `androidx.test.uiautomator:uiautomator` | 2.3.0 | `:app` androidTestImplementation |

### `app/build.gradle.kts` test block

```kotlin
testOptions {
    unitTests.isIncludeAndroidResources = true  // needed for Robolectric
    unitTests.isReturnDefaultValues = true
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.fragment.testing)
    testImplementation(libs.koin.test.junit4)
    debugImplementation(libs.fragment.testing.manifest)   // provides EmptyFragmentActivity
    androidTestImplementation(libs.uiautomator)
}
```

---

## Module 1 — `:domain` Unit Tests

**Location:** `domain/src/test/java/uz/alphazet/domain/`

Tests the framework that every feature builds on: HTTP error mapping, coroutine
flow wrappers, SharedPreferences delegates, and formatting utilities.

### Test files

#### `network/BaseRepoTest.kt` — 20 tests

Tests `BaseRepo`, the abstract class that wraps every Retrofit call in `UIResource`.

| Test | What it verifies |
|------|-----------------|
| `handle_200_returns_success` | 200 → `UIResource.Success` |
| `handle_400_throws_BadRequestException` | 400 → `BadRequestException` |
| `handle_401_throws_UnauthorizedException` | 401 → `UnauthorizedException` |
| `handle_402_throws_PaymentException` | 402 → `PaymentException` |
| `handle_403_throws_ForbiddenException` | 403 → `ForbiddenException` |
| `handle_404_throws_NotFoundException` | 404 → `NotFoundException` |
| `handle_409_throws_ConflictException` | 409 → `ConflictException` |
| `handle_422_throws_ValidationException` | 422 → `ValidationException` |
| `handle_428_throws_PreconditionRequiredException` | 428 → `PreconditionRequiredException` |
| `handle_429_throws_TooManyRequestException` | 429 → `TooManyRequestException` |
| `handle_500_throws_ServerErrorException` | 5xx → `ServerErrorException` |
| `handle_IOException_throws_ConnectionErrorException` | No network → `ConnectionErrorException` |
| `handleFlow_emits_loading_then_success` | Flow wrapper emits Loading → Success |
| `handleFlow_emits_loading_then_error` | Flow wrapper emits Loading → Error |
| `handleX_success_returns_data` | Suspend variant returns data directly |
| `handleX_error_wraps_in_UIResource_Error` | Suspend variant wraps exceptions |
| + 4 more edge cases | |

#### `network/RemoteExceptionsTest.kt` — 14 tests

Verifies the custom exception hierarchy: each `RemoteException` subclass stores
the correct HTTP status code, message, and optional `errorData` payload.

#### `data/UIResourceTest.kt` — 5 tests

Verifies `UIResource` sealed class behavior: `Success`, `Error`, `Loading`
identity comparisons and data extraction.

#### `cache/PrefDelegatesTest.kt` — 12 tests (Robolectric)

Tests all SharedPreferences delegate factories (`stringPref`, `boolPref`,
`intPref`, `longPref`, `floatPref`, `nullableStringPref`) using a real
`ApplicationContext` via Robolectric. Two-instance reads confirm persistence
across `AppCache` instances.

> **Known limitation documented in test:** The `any<T>()` delegate uses
> `TypeToken<T>()` in a non-`reified` context, causing type erasure that
> prevents Gson from deserializing back to the concrete type. This is a
> latent production bug but `any<T>()` is currently unused in `AppCacheImpl`.

#### `utils/UtilsTest.kt` — 14 tests

Pure JVM tests for extension functions in `domain/utils/`. Locale and timezone
are pinned to `Locale.ENGLISH` / `UTC` in `@Before`/`@After`.

| Utility | Tests |
|---------|-------|
| `Long.formatToPrice()` | Thousand separators, zero, large numbers |
| `String.formatPhoneNumber()` | 12-digit → `+998 XX XXX-XX-XX` format |
| `Date.getDateDMMMMYYYY()` | Specific date → Russian month name |
| `Double.formatDistance()` | `< 1 km` → metres, `≥ 1 km` → kilometres |

#### `utils/FormatDistanceTest.kt` — 2 tests

Focused boundary tests for `Double.formatDistance()`.

---

## Module 2 — `:data` Unit Tests

**Location:** `data/src/test/java/uz/alphazet/data/`

Tests data models (Gson deserialization) and all 10 Retrofit service interfaces
against a real `MockWebServer`.

### Shared helper — `services/ServiceTestSupport.kt`

```kotlin
inline fun <reified T> retrofitFor(server: MockWebServer): T
fun mockOk(body: String): MockResponse          // 200 + body
fun mockError(code: Int, body: String): MockResponse
```

Every service test creates a `MockWebServer`, enqueues a response, calls the
suspend function, and asserts the request path, method, and parsed response.

### Test files

#### `ModelDeserializationTest.kt` — 11 tests

Verifies Gson deserialization of every major data model from raw JSON strings.
Covers nested objects, nullable fields, and `@SerializedName` mappings.

> **Known limitation documented in test:** `NotificationItemData.uniqueId`
> returns `null` when parsed via Gson because it is a constructor-time
> initializer (`val uniqueId = notificationId.toString()`). Gson uses
> `Unsafe.allocateInstance` and bypasses the constructor. A separate test
> locks in this behaviour as a regression guard. Compare with
> `ShopItemData.uniqueId` which uses a property getter
> (`get() = shopId.toString()`) and works correctly.

#### `BaseResponseDataTest.kt` — 7 tests

Tests `BaseResponseData<T>` wrapper: `status`, `message`, `data` extraction,
and null-safety for optional fields.

#### `rv/BaseItemTest.kt` — 6 tests

Tests `BaseItem` DiffUtil contract: `isSameItem`, `isContentTheSame`, and
stable `uniqueId` behaviour.

#### Service tests (10 files, 46 tests total)

Each service test file follows the same pattern:
1. Start `MockWebServer`
2. Build a Retrofit instance pointed at the mock URL
3. Enqueue a `MockResponse` with a JSON fixture
4. Call the `suspend` service function via `runTest`
5. Assert the outgoing request (path, method, query params, request body)
6. Assert the parsed response model fields

| File | Service | Tests |
|------|---------|-------|
| `AuthServiceTest.kt` | `AuthService` | 5 |
| `HomeServiceTest.kt` | `HomeService` | 6 |
| `ShopServiceTest.kt` | `ShopService` | 4 |
| `OrderServiceTest.kt` | `OrderService` | 5 |
| `ProfileServiceTest.kt` | `ProfileService` | 6 |
| `NotificationServiceTest.kt` | `NotificationService` | 4 |
| `CategoryServiceTest.kt` | `CategoryService` | 3 |
| `PaymentServiceTest.kt` | `PaymentServiceTest` | 4 |
| `QrCodeServiceTest.kt` | `QrCodeService` | 5 |
| `SubscriptionServiceTest.kt` | `SubscriptionService` | 4 |

---

## Module 3 — `:app` Unit Tests

**Location:** `app/src/test/java/uz/alphazet/hoopla/`

Tests ViewModels and Fragment UI state using MockK, Turbine, Robolectric, and
Koin test rules. No production Koin graph is loaded — every test wires up its
own mocks directly.

### Shared helpers

#### `rules/MainDispatcherRule.kt`

Required by every ViewModel test. Overrides `Dispatchers.Main` with
`UnconfinedTestDispatcher` so `viewModelScope.launch {}` runs eagerly without
needing `advanceUntilIdle()`.

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(d: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(d: Description) = Dispatchers.resetMain()
}
```

#### `TestApp.kt`

Minimal `Application` subclass used in Robolectric Fragment tests via
`@Config(application = TestApp::class)`. Prevents `App.onCreate` from
initialising MapKit or the production Koin graph.

### ViewModel flow patterns

Three patterns exist across the codebase:

**Pattern A — `MutableSharedFlow(replay=0)` (AuthVM, PaymentVM)**
```kotlin
// Collect BEFORE calling the method — SharedFlow drops events with no collectors
vm.sendSmsFlow.test {
    vm.sendSms(phone)
    assertEquals(UIResource.Loading, awaitItem())
    assertTrue(awaitItem() is UIResource.Success)
    cancelAndIgnoreRemainingEvents()
}
```

**Pattern B — `MutableStateFlow(UIResource.Loading)` (HomeVM, ProfileVM, OrderVM, QRCodeVM, NotificationVM)**
```kotlin
// StateFlow already holds Loading as initial value.
// distinctUntilChanged suppresses duplicate Loading emissions.
vm.userDataFlow.test {
    assertEquals(UIResource.Loading, awaitItem())  // initial value
    vm.getUser()
    assertTrue(awaitItem() is UIResource.Success)  // goes straight to Success
    cancelAndIgnoreRemainingEvents()
}
```

**Pattern C — `suspend fun method(): SharedFlow` (ShopVM, HomeVM.getLoyaltyCard, etc.)**
```kotlin
// Mock repo to return flowOf(...); shareIn(Lazily) starts on first collector
coEvery { repo.getShopDetail(1, "ru") } returns flowOf(UIResource.Success(shop))
val flow = vm.getShopDetail(1)
flow.test {
    assertTrue(awaitItem() is UIResource.Success)
    cancelAndIgnoreRemainingEvents()
}
```

### ViewModel test files

| File | VM | Pattern(s) | Tests |
|------|----|-----------|-------|
| `auth/AuthVMTest.kt` | `AuthVM` | A | 5 |
| `home/HomeVMTest.kt` | `HomeVM` | B, C | 8 |
| `home/NotificationVMTest.kt` | `NotificationVM` | B | 2 |
| `profile/ProfileVMTest.kt` | `ProfileVM` | B, C | 5 |
| `profile/payment/PaymentVMTest.kt` | `PaymentVM` | A, C | 3 |
| `profile/subscriptions/SubscriptionVMTest.kt` | `SubscriptionVM` | C | 3 |
| `order/OrderVMTest.kt` | `OrderVM` | B, C | 5 |
| `shop_details/ShopVMTest.kt` | `ShopVM` | C | 2 |
| `qr_code/QRCodeVMTest.kt` | `QRCodeVM` | B, C | 5 |

> **`OrderVM` special case:** The `init` block calls `getUser()` on
> construction. Tests use a `lazy` delegate to set up `profileRepo` mocks
> before the VM is instantiated.

### Fragment (Robolectric) test files

Fragment tests use `launchFragmentInContainer<T>` with `@Config(sdk = [33], application = TestApp::class)` and inject mocks via `KoinTestRule`.

| File | Fragment | Tests |
|------|---------|-------|
| `auth/AuthScreenTest.kt` | `AuthScreen` | 4 |
| `profile/ProfileScreenTest.kt` | `ProfileScreen` | 5 |

**What Fragment tests cover:**

| Test | Assertion |
|------|-----------|
| `fragment_inflates_without_crash` | `launchFragmentInContainer` succeeds |
| `send_button_is_disabled_initially` | `btSend.isEnabled == false` |
| `showLoading_disables_phone_input_clickability` | `inputPhone.isClickable == false` |
| `hideLoading_re_enables_phone_input_clickability` | `inputPhone.isClickable == true` |
| `getUser_is_called_once_on_view_created` | `verify(exactly = 1) { profileVM.getUser() }` |
| `success_state_shows_auth_group_and_user_name` | Pre-seed StateFlow → `authGroup` VISIBLE, name set |
| `unauthorized_exception_shows_unauth_group` | `onUnauthorizedException()` → `unAuthGroup` VISIBLE |
| `pull_to_refresh_calls_getUser_a_second_time` | `verify(exactly = 2) { profileVM.getUser() }` |

**What Fragment tests do NOT cover (requires Espresso / device):**

- Navigation between screens (Cicerone router)
- Third-party `MaskedEditText` input behaviour
- `CircularProgressButton` animation
- Location permission flows
- QR camera, MapKit map rendering

---

## Module 3 — `:app` UI Automator Tests

**Location:** `app/src/androidTest/java/uz/alphazet/hoopla/`

UI Automator tests run on a real device or emulator against the installed debug
APK. They test end-to-end user flows that cannot be verified with JVM tests.

### Base class — `ui/BaseUiTest.kt`

```kotlin
abstract class BaseUiTest {
    protected lateinit var device: UiDevice

    @Before open fun setUp() {
        device = UiDevice.getInstance(...)
        // Seeds both language SharedPrefs (app_cache + localehelper) to match
        // the device locale, bypasses first-launch onboarding, wakes screen.
        device.pressHome()
    }

    protected fun launchApp()
    protected fun waitForId(resourceId: String, timeout: Long = 5_000): UiObject2
    protected fun waitForText(text: String, timeout: Long = 5_000): UiObject2
    protected fun waitForTextContains(substring: String, timeout: Long = 5_000): UiObject2

    // Shared helpers available to all subclasses:
    protected fun clearAuthTokens()               // removes accessToken + refreshToken from app_cache
    protected fun prefs(name: String): SharedPreferences  // opens any named prefs file
    protected fun resolveDeviceLang(): String      // OS locale → "uz" / "en" / "ru"
    protected fun toSupportedLang(lang: String): String   // maps any tag to supported code
    protected fun buildLocalizedContext(lang: String): Context  // Context locked to lang
}
```

Default timeout: **5 000 ms** per element lookup.

### Robot classes

The Robot Pattern separates test intent from `UiDevice` boilerplate. Every
robot method returns `this` so calls chain naturally.

| Class | Location | Used by |
|-------|----------|---------|
| `AuthRobot` | `ui/auth/AuthRobot.kt` | `AuthFlowTest` |
| `ProfileRobot` | `ui/profile/ProfileRobot.kt` | `ProfileScreenTest` |

**`AuthRobot` API:**

| Method | Action |
|--------|--------|
| `typePhone(digits)` | Click `inputPhone`, type 9-digit suffix |
| `clearPhone()` | Clear `inputPhone` |
| `assertAuthScreenVisible()` | "hoopla" title + `inputPhone` visible |
| `assertContinueEnabled()` | `btSend.isEnabled == true` |
| `assertContinueDisabled()` | `btSend.isEnabled == false` |

**`ProfileRobot` API:**

| Method | Action |
|--------|--------|
| `navigateToProfile()` | Tap profile tab, wait for `swipe_refresh_layout` |
| `readHeaderText()` | Return `header_title` text |
| `assertHeaderText(expected)` | Assert `header_title == expected` |
| `assertLoginButtonEnabledIfPresent()` | If `login` button exists, assert it is enabled |
| `assertLanguagesRowVisible()` | `languages` view found |
| `assertPrivacyPolicyRowVisible()` | `privacyPolicy` view found |
| `assertOrdersTabVisible()` | `orders` nav item found |

### Test files

#### `ui/MainActivityTest.kt` — 5 tests

`setUp()` calls `clearAuthTokens()` before launching so tests always start
in the logged-out state.

| Test | Flow |
|------|------|
| `app_launches_and_home_screen_is_visible` | Launch → `bottom_nav` + `home` tab present |
| `tapping_profile_tab_opens_profile_screen` | Tap `profile` tab → `header_title` view visible |
| `tapping_orders_tab_when_logged_out_shows_sign_in_dialog` | Tap `orders` while logged out → dialog with "авторизованы" text appears (unconditional assert); dismisses via "Отмена" |
| `tapping_map_tab_shows_map_screen` | Tap `map` tab → `bottom_nav` still present |
| `bottom_nav_is_visible_on_all_tabs` | Cycle home → map → profile; assert `bottom_nav` visible each time |

#### `ui/auth/AuthFlowTest.kt` — 5 tests

Launches `AuthActivity` directly (isolated from `MainActivity`). `launchAuthActivity()` waits for `inputPhone` to be drawn (not just the package window) before returning.

All UI interactions are delegated to **`AuthRobot`** — test bodies are single-line chains.

| Test | Robot call |
|------|------------|
| `auth_screen_shows_title_and_phone_input` | `robot.assertAuthScreenVisible()` |
| `continue_button_is_disabled_before_phone_entry` | `robot.assertContinueDisabled()` |
| `typing_valid_phone_enables_continue_button` | `robot.typePhone("901234567").assertContinueEnabled()` |
| `clearing_phone_disables_continue_button` | `robot.typePhone("901234567").clearPhone().assertContinueDisabled()` |
| `incomplete_phone_number_keeps_continue_button_disabled` | `robot.typePhone("9012").assertContinueDisabled()` |

#### `ui/profile/ProfileScreenTest.kt` — 9 tests

Navigates to Profile via bottom nav. Verifies four independent language
sources simultaneously. UI interactions are delegated to **`ProfileRobot`**;
language-source reads (SharedPrefs comparisons) stay in the test class.

**Language sources tracked:**

| Source | How it is read |
|--------|----------------|
| `deviceLang` | OS locale via `resolveDeviceLang()` (BaseUiTest) |
| `prefLang` | `app_cache` SharedPrefs → key `lang` |
| `localeLang` | localehelper SharedPrefs → key `Locale.Helper.Selected.Language` |
| `uiLang` | reverse-mapped from `robot.readHeaderText()` |

| Test | What it checks |
|------|----------------|
| `cache_lang_matches_localehelper_lang` | `prefLang == localeLang` |
| `device_language_matches_sharedpref_language` | `deviceLang == prefLang` |
| `ui_language_matches_sharedpref_language` | rendered header matches expected string for `prefLang` |
| `all_language_sources_are_in_sync` | all four sources agree |
| `profile_screen_shows_header_in_current_language` | `robot.assertHeaderText(expected)` |
| `login_button_visible_when_logged_out` | `robot.assertLoginButtonEnabledIfPresent()` |
| `languages_row_is_visible` | `robot.assertLanguagesRowVisible()` |
| `privacy_policy_row_is_visible` | `robot.assertPrivacyPolicyRowVisible()` |
| `orders_tab_label_matches_current_language` | `robot.assertOrdersTabVisible()` |

#### `ui/auth/LoginE2ETest.kt` — 4 tests

End-to-end login flow using a real test account and SMS API.

**Requires:**
- Real internet connection (SMS API call)
- Physical/virtual SIM reachable at `+998 90 047-24-00`
- `OTP_CODE` constant replaced with the actual received SMS code

> `full_login_flow_with_real_phone_and_sms_code` is **automatically skipped**
> (`Assume.assumeTrue`) when `OTP_CODE` equals the placeholder `"12345"`.
> Replace the constant before running this test manually; do not run it in CI
> without a real OTP injection mechanism.

| Test | Flow |
|------|------|
| `full_login_flow_with_real_phone_and_sms_code` | Full flow: enter phone → Continue → wait OTP screen → enter OTP → assert `bottom_nav` (skipped when OTP is placeholder) |
| `phone_input_accepts_number_and_enables_continue` | Enter "900472400" → `btSend.isEnabled == true` |
| `sms_request_leads_to_otp_screen` | Tap Continue → `inputCode` (PinView) + `phoneNumber` label appear |
| `back_button_on_otp_screen_returns_to_phone_input` | On OTP screen → tap `backImg` → `inputPhone` reappears |

---

## Architecture of the Test Stack

```
┌──────────────────────────────────────────────────────────┐
│  UI Automator (androidTest)          real device/emulator │
│  MainActivityTest (5), AuthFlowTest (5),                  │
│  ProfileScreenTest (9), LoginE2ETest (4) = 23 tests       │
│  — full app installed, real Koin, network optional        │
├──────────────────────────────────────────────────────────┤
│  Robolectric Fragment tests (test)              JVM only  │
│  AuthScreenTest, ProfileScreenTest                        │
│  — fragment inflation, view state, ViewModel mock calls   │
├──────────────────────────────────────────────────────────┤
│  ViewModel unit tests (test)                    JVM only  │
│  *VMTest.kt × 9 files                                     │
│  — UIResource flow patterns A/B/C, MockK + Turbine        │
├──────────────────────────────────────────────────────────┤
│  Service tests (test)                           JVM only  │
│  *ServiceTest.kt × 10 files                               │
│  — MockWebServer: request path/method/body, response JSON │
├──────────────────────────────────────────────────────────┤
│  Model / BaseRepo / Utils tests (test)          JVM only  │
│  BaseRepoTest, ModelDeserializationTest, UtilsTest, …     │
│  — pure logic, no Android context (except PrefDelegates)  │
└──────────────────────────────────────────────────────────┘
```

---

## What Is Not Tested

| Area | Reason | Path forward |
|------|--------|-------------|
| Paging flows (`getNotificationsPager`, `getOrderHistoryPager`) | Need `androidx.paging:paging-testing` | Add as a separate task |
| `HomeScreen` | Heavy: FusedLocationClient, QR scan, LocationManager, Yandex MapKit | Extract location logic into a testable wrapper class |
| `MapScreen` | MapKit initialises native SDK; cannot run headless | Instrument test only |
| `ShopDetailActivity` (scroll-chip sync, working hours) | Complex View interactions | Espresso with `ActivityScenario` |
| Network integration | Token authenticator, real API responses | Mock server environment / staging config |
| `any<T>()` PrefDelegate Gson round-trip | Type erasure bug — `TypeToken<T>()` in non-reified context; `any<T>()` is currently unused in `AppCacheImpl` | Fix delegate to use `reified` or remove |

---

## Recommended Improvements

Identified during android-testing skill review. These require build.gradle / infrastructure changes and have not been applied yet.

| Improvement | Effort | Details |
|-------------|--------|---------|
| **JUnit4 → JUnit5** | High | Add `junit-jupiter` to `libs.versions.toml`; replace `@Rule`/`TestWatcher` with `@BeforeEach`/`@AfterEach`; `MainDispatcherRule` becomes a plain setup/teardown pair |
| **MockK → Fakes** | High | Create `FakeAuthRepo`, `FakeProfileRepo`, etc. as in-memory implementations; fakes catch integration-level bugs (e.g. double-emit, state leaks) that mocks miss |
| **JUnit Assert → AssertK** | Medium | Add `assertk` dependency; replace `assertEquals(x, y)` with `assertThat(y).isEqualTo(x)` for better failure messages and fluent chaining |

---

## CI Recommendation

```yaml
# Fast gate — runs on every push (no device needed)
- run: ./gradlew :domain:testDebugUnitTest :data:testDebugUnitTest :app:testDebugUnitTest

# Slow gate — runs on PRs to main (requires emulator)
- run: ./gradlew :app:connectedDebugAndroidTest
```

> **Note on `LoginE2ETest`:** The slow gate will run all four tests in this class.
> `full_login_flow_with_real_phone_and_sms_code` will be **skipped** (not failed)
> in CI because `OTP_CODE` defaults to the placeholder `"12345"`. The remaining
> three tests (`phone_input_accepts_number_and_enables_continue`,
> `sms_request_leads_to_otp_screen`, `back_button_on_otp_screen_returns_to_phone_input`)
> require a live internet connection and will make real SMS API calls.
