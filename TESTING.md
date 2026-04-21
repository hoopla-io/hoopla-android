# Hoopla — Test Suite Documentation

## Overview

The project has four test layers spread across the three-module architecture.

| Layer | Module | Framework | Runs on | Count |
|-------|--------|-----------|---------|------:|
| Unit — core logic + repositories | `:domain` | JUnit 4 + MockK + Turbine + Robolectric | JVM | **141** |
| Unit — services + models | `:data` | JUnit 4 + MockWebServer + Gson | JVM | **70** |
| Unit — ViewModels + Fragments | `:app` | JUnit 4 + MockK + Turbine + Robolectric + Koin | JVM | **61** |
| UI Automation | `:app` | UI Automator 2 | Device / Emulator | **66** |
| **Total JVM** | | | | **272** |
| **Grand total (incl. on-device)** | | | | **338** |

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

```
domain/build/reports/tests/testDebugUnitTest/index.html
data/build/reports/tests/testDebugUnitTest/index.html
app/build/reports/tests/testDebugUnitTest/index.html
app/build/reports/androidTests/connected/index.html
```

---

## Test Dependencies

Versions live in `gradle/libs.versions.toml`.

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
    unitTests.isIncludeAndroidResources = true   // required for Robolectric
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
    debugImplementation(libs.fragment.testing.manifest)   // EmptyFragmentActivity host
    androidTestImplementation(libs.uiautomator)
}
```

---

## Module 1 — `:domain` Unit Tests (141 tests)

**Location:** `domain/src/test/java/uz/alphazet/domain/`

Covers the framework every feature builds on: HTTP error mapping, coroutine
flow wrappers, SharedPreferences delegates, utilities — and every repository
in the app.

### Core framework — 67 tests

| File | Tests | Scope |
|------|------:|-------|
| `network/BaseRepoTest.kt` | 20 | `handle` / `handleFlow` / `handleX`; HTTP 200/400/401/402/403/404/409/422/428/429/5xx → custom exceptions; `IOException → ConnectionErrorException` |
| `network/RemoteExceptionsTest.kt` | 14 | Each `RemoteException` subclass carries the correct status, message and optional `errorData` |
| `data/UIResourceTest.kt` | 5 | `Success` / `Error` / `Loading` identity + data extraction |
| `cache/PrefDelegatesTest.kt` | 12 | Robolectric — every `*Pref` delegate factory against a real `ApplicationContext`; two-instance reads confirm persistence |
| `utils/UtilsTest.kt` | 14 | `Long.formatToPrice`, `String.formatPhoneNumber`, `Date.getDateDMMMMYYYY`, `Double.formatDistance`; locale/TZ pinned to `ENGLISH`/`UTC` |
| `utils/FormatDistanceTest.kt` | 2 | Boundary values (`< 1 km` → m, `≥ 1 km` → km) |

> **Known limitation** — `any<T>()` delegate uses `TypeToken<T>()` in a
> non-reified context. The resulting type erasure prevents Gson round-tripping
> to the concrete type. `any<T>()` is currently unused in `AppCacheImpl`; a
> regression test locks in the bug.

### Repositories — 74 tests

One test file per `BaseRepo` subclass. Each file mocks the relevant Retrofit
service with MockK, exercises every public method, and asserts both the
request (args forwarded correctly, JSON body shape) and the outgoing
`UIResource` via Turbine. Files that construct `JSONObject` bodies
(`AuthRepo`, `OrderRepo`, `ProfileRepo`, `SubscriptionRepo`) use Robolectric
because `org.json.JSONObject` is an Android stub on pure JVM.

| File | Tests | JVM runner | Notable coverage |
|------|------:|------------|------------------|
| `repositories/AuthRepoTest.kt` | 10 | Robolectric | `sendSMS`, `resendSMS`, `confirmSMS` — JSON body slot-captured and parsed |
| `repositories/CategoryRepoTest.kt` | 5 | Pure JVM | `getCategories()` success/empty/400/401/offline |
| `repositories/HomeRepoTest.kt` | 11 | Pure JVM | `getLoyaltyCard`, `getNearShops(lat,lng,name,catId)`, `getPendingFeedbacks`, `submitFeedback` — `buildMap` omits blank comment |
| `repositories/NotificationRepoTest.kt` | 6 | Pure JVM | `getNotificationDetail` defaults language to `"ru"`; `markRead()` via flow |
| `repositories/OrderRepoTest.kt` | 11 | Robolectric | `validateOrder`, `createOrder` (modifiers array), `createOrderRahmat` (`use_cashback` + `cashback_amount`) |
| `repositories/PaymentServiceRepoTest.kt` | 5 | Pure JVM | `getPaymentServices`, `topUpViaPayService` including 402 PaymentException |
| `repositories/ProfileRepoTest.kt` | 10 | Robolectric | `getMe`, `editMe`, `updateMe` (null/empty keys omitted), `logout`, `deactivate` |
| `repositories/QRCodeRepoTest.kt` | 7 | Pure JVM | `generateQRCode`, `getOrderInfo(id)`, `cancelOrder(id)`, `getDrinksStat` |
| `repositories/ShopRepoTest.kt` | 4 | Pure JVM | `getShopDetail(shopId)` forwards id; 404/500 branches |
| `repositories/SubscriptionRepoTest.kt` | 5 | Robolectric | `getSubscriptions`, `buySubscription(id)` with 402 PaymentException |

---

## Module 2 — `:data` Unit Tests (70 tests)

**Location:** `data/src/test/java/uz/alphazet/data/`

Covers data models (Gson deserialization) and all 10 Retrofit service
interfaces against a real `MockWebServer`.

### Shared helper — `services/ServiceTestSupport.kt`

```kotlin
inline fun <reified T> retrofitFor(server: MockWebServer): T
fun mockOk(body: String): MockResponse            // 200 + body
fun mockError(code: Int, body: String): MockResponse
```

Every service test starts `MockWebServer`, enqueues a response, calls the
suspend function via `runTest`, and asserts the outgoing request (path,
method, query params, request body) plus the parsed response model.

### Models — 24 tests

| File | Tests | Scope |
|------|------:|-------|
| `ModelDeserializationTest.kt` | 11 | Every major data model from raw JSON; nested objects, nullable fields, `@SerializedName` mappings |
| `BaseResponseDataTest.kt` | 7 | `BaseResponseData<T>` wrapper: `status`, `message`, `data`, null-safety |
| `rv/BaseItemTest.kt` | 6 | `BaseItem` DiffUtil contract — `isSameItem`, `isContentTheSame`, stable `uniqueId` |

> **Known limitation** — `NotificationItemData.uniqueId` returns `null` via
> Gson because it is a constructor-time initializer (`val uniqueId =
> notificationId.toString()`). Gson uses `Unsafe.allocateInstance` and
> bypasses the constructor. `ShopItemData.uniqueId` uses a property getter
> (`get() = shopId.toString()`) and works correctly. A regression test locks
> in the difference.

### Services — 46 tests

| File | Service | Tests |
|------|---------|------:|
| `services/AuthServiceTest.kt` | `AuthService` | 5 |
| `services/CategoryServiceTest.kt` | `CategoryService` | 3 |
| `services/HomeServiceTest.kt` | `HomeService` | 6 |
| `services/NotificationServiceTest.kt` | `NotificationService` | 4 |
| `services/OrderServiceTest.kt` | `OrderService` | 5 |
| `services/PaymentServiceTest.kt` | `PaymentService` | 4 |
| `services/ProfileServiceTest.kt` | `ProfileService` | 6 |
| `services/QrCodeServiceTest.kt` | `QrCodeService` | 5 |
| `services/ShopServiceTest.kt` | `ShopService` | 4 |
| `services/SubscriptionServiceTest.kt` | `SubscriptionService` | 4 |

---

## Module 3 — `:app` JVM Unit Tests (61 tests)

**Location:** `app/src/test/java/uz/alphazet/hoopla/`

Covers ViewModels and Fragment UI state with MockK + Turbine + Robolectric +
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
// Collect BEFORE calling the method — SharedFlow drops events with no collectors.
vm.sendSmsFlow.test {
    vm.sendSms(phone)
    assertEquals(UIResource.Loading, awaitItem())
    assertTrue(awaitItem() is UIResource.Success)
    cancelAndIgnoreRemainingEvents()
}
```

**Pattern B — `MutableStateFlow(UIResource.Loading)` (HomeVM, ProfileVM, OrderVM, QRCodeVM, NotificationVM)**
```kotlin
// StateFlow already holds Loading; distinctUntilChanged suppresses duplicates.
vm.userDataFlow.test {
    assertEquals(UIResource.Loading, awaitItem())  // initial value
    vm.getUser()
    assertTrue(awaitItem() is UIResource.Success)  // straight to Success
    cancelAndIgnoreRemainingEvents()
}
```

**Pattern C — `suspend fun method(): SharedFlow` (ShopVM, HomeVM.getLoyaltyCard, SubscriptionVM, …)**
```kotlin
coEvery { repo.getShopDetail(1, "ru") } returns flowOf(UIResource.Success(shop))
val flow = vm.getShopDetail(1)
flow.test {
    assertTrue(awaitItem() is UIResource.Success)
    cancelAndIgnoreRemainingEvents()
}
```

### ViewModel tests — 38 tests

| File | VM | Pattern(s) | Tests |
|------|----|-----------|------:|
| `ui/auth/AuthVMTest.kt` | `AuthVM` | A | 5 |
| `ui/home/HomeVMTest.kt` | `HomeVM` | B, C | 8 |
| `ui/home/NotificationVMTest.kt` | `NotificationVM` | B | 2 |
| `ui/profile/ProfileVMTest.kt` | `ProfileVM` | B, C | 5 |
| `ui/profile/payment/PaymentVMTest.kt` | `PaymentVM` | A, C | 3 |
| `ui/profile/subscriptions/SubscriptionVMTest.kt` | `SubscriptionVM` | C | 3 |
| `ui/order/OrderVMTest.kt` | `OrderVM` | B, C | 5 |
| `ui/qr_code/QRCodeVMTest.kt` | `QRCodeVM` | B, C | 5 |
| `ui/shop_details/ShopVMTest.kt` | `ShopVM` | C | 2 |

> **`OrderVM` special case** — the `init` block calls `getUser()` on
> construction. Tests use a `lazy` delegate to set up `profileRepo` mocks
> *before* the VM is instantiated.

### Fragment (Robolectric) tests — 23 tests

Fragment tests use `launchFragmentInContainer<T>` with
`@Config(sdk = [33], application = TestApp::class)` and inject mocks via
`KoinTestRule`.

| File | Fragment | Tests |
|------|----------|------:|
| `ui/auth/AuthScreenTest.kt` | `AuthScreen` | 5 |
| `ui/home/HomeScreenTest.kt` | `HomeScreen` | 8 |
| `ui/profile/ProfileScreenTest.kt` | `ProfileScreen` | 10 |

**What Fragment tests cover:**

- Fragment inflation without crash
- Initial view state (button enabled/disabled, group visibility)
- Loading transitions (`showLoading` / `hideLoading` toggle input
  clickability, swipe-refresh state)
- ViewModel methods called with correct arguments (`verify`)
- Success/error flow responses mapped into views (pre-seed the mocked
  `StateFlow`, assert `authGroup`/`unAuthGroup` visibility, text fields,
  etc.)
- Pull-to-refresh triggers the repository a second time

**What Fragment tests do NOT cover** (requires Espresso / device):

- Navigation between screens (Cicerone router)
- `MaskedEditText` input behaviour
- `CircularProgressButton` animation
- Location permission flows, QR camera, MapKit rendering

---

## Module 3 — `:app` UI Automator Tests (66 tests)

**Location:** `app/src/androidTest/java/uz/alphazet/hoopla/`

UI Automator tests run on a real device or emulator against the installed
debug APK. They cover end-to-end flows that cannot be verified on the JVM.

### Base class — `ui/BaseUiTest.kt`

```kotlin
abstract class BaseUiTest {
    protected lateinit var device: UiDevice

    @Before open fun setUp() {
        device = UiDevice.getInstance(...)
        // Seeds both language SharedPrefs (app_cache + localehelper) to match
        // the OS locale, seeds isFirstTime=false so Splash skips onboarding,
        // wakes the screen and dismisses the keyguard, then presses Home.
    }

    protected fun launchApp()
    protected fun waitForId(resourceId: String, timeout: Long = 5_000): UiObject2
    protected fun waitForText(text: String, timeout: Long = 5_000): UiObject2
    protected fun waitForTextContains(substring: String, timeout: Long = 5_000): UiObject2

    // Shared helpers:
    protected fun clearAuthTokens()                    // removes accessToken + refreshToken
    protected fun prefs(name: String): SharedPreferences
    protected fun resolveDeviceLang(): String          // OS locale → "uz" / "en" / "ru"
    protected fun toSupportedLang(lang: String): String
    protected fun buildLocalizedContext(lang: String): Context
    protected fun currentPrefLang(): String
    protected fun localizedString(@StringRes id: Int): String

    companion object {
        const val APP_PACKAGE     = "uz.alphazet.hoopla"
        const val TIMEOUT_MS      = 5_000L
        const val IDLE_TIMEOUT_MS = 1_000L
        const val LAUNCH_READY_MS = 10_000L
    }
}
```

### Shared patterns

- **Logged-out baseline** — tests that must hit the unauthenticated branch
  call `clearAuthTokens()` in `@Before` before `super.setUp()` + `launchApp()`.
- **Direct-launch activities** — sub-screens like `SubscriptionActivity`,
  `OrderActivity`, `EditProfileScreen` and `NotificationDetailScreen` are
  launched via `Intent().setClassName(APP_PACKAGE, "...")` instead of
  navigating from the bottom nav. Extras are seeded inline when needed
  (order id, notification id, shop id).
- **Per-character typing** — phone / search / name fields use
  `device.executeShellCommand("input text ...")` so the `MaskedEditText` /
  `PinView` TextWatchers fire exactly as in production.
- **Locale-resilient back arrow** — toolbar back is located via
  `By.desc("Orqaga")` with a `device.pressBack()` fallback for non-UZ
  locales.
- **Seeded fake auth** — `OrdersScreenTest` writes a non-empty `accessToken`
  to `app_cache` so MainActivity's orders-tab guard lets the tap through;
  the backend 401s the fake token but the fragment chrome is still
  assertable.
- **Onboarding override** — `OnBoardingScreenTest` writes
  `isFirstTime=true` *after* `super.setUp()` runs (which seeds `false` by
  default), then launches the app manually.

### Test files

#### Activity-level flows

| File | Tests | Target |
|------|------:|--------|
| `ui/SplashActivityTest.kt` | 3 | Splash branches: onboarding vs. MainActivity based on `isFirstTime`; auth state does not affect routing |
| `ui/MainActivityTest.kt` | 5 | Bottom-nav startup, profile/map/home tabs, orders-tab guard dialog when logged out |
| `ui/on_boarding/OnBoardingScreenTest.kt` | 4 | Page 1 chrome, `next` advances to page 2 (still on onboarding), `next` on page 2 → MainActivity, `skip` → MainActivity |

#### Auth

| File | Tests | Target |
|------|------:|--------|
| `ui/auth/AuthFlowTest.kt` | 6 | `AuthActivity` launch, `btSend` disabled/enabled gating, navigation to `ConfirmPhoneNumberScreen`, invalid-code error surfacing, back navigation. Uses `+998 90 047-24-00` / code `12345` |
| `ui/auth/ConfirmPhoneNumberScreenTest.kt` | 5 | Static chrome (`confirmation_code`, `code_send_to`, `phoneNumber`, `timer`, `privacy_policy`), `mm:ss` timer format, `send_again` disabled while counting down, `errorTextCode` hidden on fresh screen, `backImg` returns to phone input |

#### Home + Notifications

| File | Tests | Target |
|------|------:|--------|
| `ui/home/HomeScreenTest.kt` | 5 | Home logo, `search`/`notification` cards, `categoriesRv`, bottom-nav persistence |
| `ui/home/NotificationsScreenTest.kt` | 3 | Toolbar + `notification_rv` render, toolbar back → Home, system back → Home |
| `ui/home/NotificationDetailScreenTest.kt` | 1 | Layout chrome (toolbar, image, name, desc) with `id = -1` |

#### Shop / Map / Search

| File | Tests | Target |
|------|------:|--------|
| `ui/shop_details/ShopDetailFlowTest.kt` | 4 | Navigate via Home nearby-shops; assert `ShopDetailActivity` chrome and back-arrow → Home |
| `ui/map/MapScreenTest.kt` | 3 | Yandex `map_view` + `header_layout` + `logo`, bottom-nav persistence, Map → Home round-trip |
| `ui/search/SearchScreenTest.kt` | 3 | Toolbar + `inputSearch` + `items_rv`, typing survives (TextWatcher fires), system back → Home |

#### Profile + sub-screens

| File | Tests | Target |
|------|------:|--------|
| `ui/profile/ProfileScreenTest.kt` | 5 | Profile header, swipe-refresh, unauth group visible after 401, language bottom-sheet, login CTA → `AuthActivity` |
| `ui/profile/EditProfileScreenTest.kt` | 3 | Form chrome (name/gender/birth/CTA/delete-account), `btSend` disabled on initial render, toolbar back |
| `ui/profile/subscriptions/SubscriptionActivityTest.kt` | 3 | Toolbar + `subscription_rv`, `swipe_refresh_layout` present, toolbar back |
| `ui/profile/payment/PaymentServicesActivityTest.kt` | 2 | Toolbar + `subscription_rv` grid, toolbar back |

#### Orders / Order flow

| File | Tests | Target |
|------|------:|--------|
| `ui/qr_code/OrdersScreenTest.kt` | 3 | Logged-out guard blocks the tab, logged-in chrome (header/`order_rv`/swipe/title), bottom-nav persistence |
| `ui/qr_code/OrderInfoScreenTest.kt` | 2 | Layout chrome (toolbar/image/name/status/info_rv), toolbar back |
| `ui/order/OrderActivityTest.kt` | 2 | Drink customisation chrome (size/sugar/milk/syrup RVs + `order` CTA), toolbar back |
| `ui/order/OrderActivity2Test.kt` | 2 | Alt grouped-modifier flow (`modifications_rv` + `size_selector` + `order`), toolbar back |
| `ui/order/CheckoutActivityTest.kt` | 2 | Review-and-pay chrome (image/name/total_summa/info_rv/order), toolbar back |

---

## Architecture of the Test Stack

```
┌──────────────────────────────────────────────────────────────┐
│  UI Automator (androidTest)             real device/emulator │
│  20 files · 66 tests                                         │
│  — full app installed, real Koin graph, network optional     │
├──────────────────────────────────────────────────────────────┤
│  Robolectric Fragment tests (test)                 JVM only  │
│  AuthScreen · HomeScreen · ProfileScreen  — 23 tests         │
│  — inflation, view state transitions, VM verify              │
├──────────────────────────────────────────────────────────────┤
│  ViewModel unit tests (test)                       JVM only  │
│  9 *VMTest.kt files  — 38 tests                              │
│  — UIResource flow patterns A/B/C, MockK + Turbine           │
├──────────────────────────────────────────────────────────────┤
│  Repository unit tests (test — :domain)            JVM only  │
│  10 *RepoTest.kt files  — 74 tests                           │
│  — MockK service + request/body capture + UIResource flow    │
├──────────────────────────────────────────────────────────────┤
│  Service tests (test — :data)                      JVM only  │
│  10 *ServiceTest.kt files  — 46 tests                        │
│  — MockWebServer: request path/method/body, response JSON    │
├──────────────────────────────────────────────────────────────┤
│  Model + core (test — :domain, :data)              JVM only  │
│  BaseRepo · RemoteExceptions · UIResource ·                  │
│  PrefDelegates · Utils · ModelDeserialization · BaseItem     │
│  — 91 tests, pure logic (Robolectric where Android ctx needed)│
└──────────────────────────────────────────────────────────────┘
```

---

## What Is Not Tested

| Area | Reason | Path forward |
|------|--------|--------------|
| Paging flows (`getNotificationsPager`, `getOrderHistoryPager`) | Need `androidx.paging:paging-testing` | Add as a separate task |
| `HomeScreen` integrations (FusedLocationClient, QR scan, LocationManager) | Native dependencies cannot run headless | Extract location logic into a wrapper class |
| `MapScreen` map rendering | Yandex MapKit initialises a native SDK | UI Automator only (already present) |
| Full authenticated flows (buy subscription, create order, cancel order, top-up) | Need real access token matching backend | Test account + OTP injection, or staging env with pre-provisioned tokens |
| `any<T>()` PrefDelegate Gson round-trip | Type erasure — `TypeToken<T>()` in non-reified context | Fix delegate to use `reified` or remove |
| Bottom-sheet dialogs (`FeedbackBD`, `SelectCashbackSummaBD`, `TopUpViaPaymentServiceBD`) | Hosted dialogs — tests need their host activity state | Dialog is covered indirectly via the host activity's UI test |

---

## Recommended Improvements

Identified during an android-testing skill review. These require
build.gradle / infrastructure changes and have not been applied yet.

| Improvement | Effort | Details |
|-------------|--------|---------|
| **JUnit4 → JUnit5** | High | Add `junit-jupiter`; replace `@Rule` / `TestWatcher` with `@BeforeEach` / `@AfterEach`; `MainDispatcherRule` becomes a plain setup/teardown pair |
| **MockK → Fakes** | High | Hand-rolled `FakeAuthRepo`, `FakeProfileRepo`, etc. catch integration-level bugs (double-emit, state leaks) that mocks miss |
| **JUnit Assert → AssertK** | Medium | Add `assertk`; replace `assertEquals(x, y)` with `assertThat(y).isEqualTo(x)` for better failure messages and fluent chaining |
| **Robot pattern for UI Automator** | Medium | Re-introduce per-screen robots (previously used for Auth/Profile) as the UI suite grew past 20 files — reduces duplicated `device.wait(Until.findObject(...))` boilerplate |

---

## CI Recommendation

```yaml
# Fast gate — runs on every push (no device needed)
- run: ./gradlew :domain:testDebugUnitTest :data:testDebugUnitTest :app:testDebugUnitTest

# Slow gate — runs on PRs to main (requires emulator)
- run: ./gradlew :app:connectedDebugAndroidTest
```

The UI Automator suite assumes a device on which network is reachable and
Yandex MapKit has a valid API key wired into the debug build. Flaky runs are
usually attributable to one of those two prerequisites.