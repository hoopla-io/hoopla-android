# Hoopla (Qahvazor) — Claude Rules

## Project Overview

Android app for a coffee loyalty/ordering platform. Package: `uz.alphazet.hoopla`. Min SDK 26, Target SDK 36, Kotlin 2.2.0, Java 11.

---

## Module Structure

| Module | Namespace | Purpose |
|--------|-----------|---------|
| `:app` | `uz.alphazet.hoopla` | UI, Activities, Fragments, ViewModels, DI |
| `:domain` | `uz.alphazet.domain` | Base classes, Repositories, Network, Cache, Utilities |
| `:data` | `uz.alphazet.data` | Retrofit services, data models (DTOs), UIResource |

---

## Architecture

**Clean Architecture + MVVM + Repository Pattern**

```
Fragment/Activity → ViewModel → Repository → Service (Retrofit) → API
```

- State: `UIResource<T>` sealed class — `Success<T>`, `Error(Throwable)`, `Loading`
- Async: Kotlin Coroutines + Flow/SharedFlow/StateFlow
- DI: Koin (modules in `domain/di/Modules.kt` and `app/di/AppModule.kt`)
- Navigation: Cicerone library + AndroidX Navigation Component

---

## Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| ViewModel | `{Feature}VM` | `HomeVM`, `AuthVM` |
| Fragment | `{Feature}Screen` | `HomeScreen`, `ProfileScreen` |
| Activity | `{Feature}Activity` | `ShopDetailActivity`, `CheckoutActivity` |
| Repository | `{Domain}Repo` | `HomeRepo`, `OrderRepo` |
| Service | `{Domain}Service` | `HomeService`, `OrderService` |
| Data model | `{Entity}Data` or `{Entity}ItemData` | `UserData`, `ShopItemData` |
| Adapter | `{Item}Adapter` | `NearShopAdapter`, `OrderAdapter` |
| BottomSheet dialog | `{Feature}BD` | `SelectCashbackSummaBD` |
| Custom exceptions | `{Type}Exception` | `UnauthorizedException` |

---

## Base Classes — Always Extend These

- **Activity:** `BaseActivity` (domain) — locale-aware, error listener
- **Fragment:** `BaseFragment` (domain) — error handling, viewBinding delegate
- **ViewModel:** `BaseVM` (domain) — coroutine scope, UIResource emission
- **Adapter:** `BaseAdapter<Item, VH>` (domain) — DiffUtil, generic
- **Paging Adapter:** `BasePagingAdapter` (domain)
- **Dialog:** `BaseDialog` / `BaseBottomSheetDF` (domain)

---

## Dependency Injection (Koin)

- **`utilsModule`** — AppCache, PermissionManager
- **`apiModule`** — Retrofit, OkHttpClient, all Services
- **`repositoryModule`** — all Repo instances
- **`viewModelModule`** — all ViewModels (in `:app`)

When adding a new feature:
1. Add Service to `apiModule`
2. Add Repository to `repositoryModule`
3. Add ViewModel to `viewModelModule` in `AppModule.kt`

---

## Network

- **Base URL:** `https://api.hoopla.uz/api/`
- **Interceptor chain:** Chucker (debug) → NetworkConnectionInterceptor → NetworkInterceptor (412→401) → HttpLoggingInterceptor → TokenAuthenticator
- Token refresh is automatic via `TokenAuthenticator`; 401 triggers refresh, repeated 401 = logout
- Wrap all API calls in `UIResource` via `BaseRepo` helpers

---

## Error Handling

Custom exception hierarchy in `domain/network/RemoteExceptions.kt`:
- `RemoteException` → base
  - `UnauthorizedException` (401)
  - `BadRequestException` (400)
  - `ValidationException` (422)
  - `ForbiddenException` (403)
  - `NotFoundException` (404)
  - `PaymentException` (402)
  - `ConflictException` (409)
  - `TooManyRequestException` (429)
  - `PreconditionRequiredException` (428)
  - `ServerErrorException` (5xx)
  - `ConnectionErrorException` (offline)

Handle errors through `RemoteErrorListener` — implemented in BaseActivity/BaseFragment.

---

## State Management in Fragments/Activities

```kotlin
viewModel.someFlow.collectLatest { resource ->
    when (resource) {
        is UIResource.Loading -> showLoading()
        is UIResource.Success -> handleData(resource.data)
        is UIResource.Error -> handleError(resource.throwable)
    }
}
```

---

## View Binding

- Fragments: use `viewBinding()` delegate from `VBFactory.kt` — lifecycle-aware
- Activities: standard `ActivityXxxBinding.inflate(layoutInflater)`

---

## Common Utility Extensions (domain/utils/)

- `view.gone()` / `view.visible()` — visibility helpers
- `String.formatPhoneNumber()` — phone formatting
- `Long.formatToPrice()` — currency display
- `Date.getDateDMMMMYYYY()` — date formatting
- `Double.formatDistance()` — distance display
- Any object: `.log(tag)` — custom logging

---

## Localization

- Supported via `com.akexorcist:localization` + `LocaleHelper`
- Default language: Russian (`ru`)
- User preference stored in `AppCache.lang`
- Language switching via activity recreation — already handled in `BaseActivity`
- String resources: `values/` (Russian default), `values-en/`, `values-uz/`

---

## SharedPreferences (AppCache)

Interface in `domain/cache/AppCache.kt`, implemented via `PrefDelegates`:
- `accessToken`, `refreshToken`, `tokenExpireAt`
- `fcmToken`
- `lang` — language code
- `isFirstTime` — onboarding flag

---

## Key Libraries

| Library | Usage |
|---------|-------|
| Retrofit 2.9.0 + Gson | REST API |
| OkHttp 4.11.0 | HTTP client |
| Koin | Dependency injection |
| Cicerone 7.1 | Navigation router |
| Coil 3.0.4 | Image loading |
| Lottie 5.0.3 | Animations |
| Yandex MapKit 4.24.0 | Maps |
| AndroidX Paging 3 | Pagination |
| Firebase (Analytics + Crashlytics) | Analytics/crash reporting |
| Dexter 6.2.2 | Runtime permissions |
| Quickie | QR code scanning |

---

## Code Style Rules

1. **No LiveData** — use StateFlow/SharedFlow consistently
2. **No direct SharedPreferences** — always go through `AppCache`
3. **No raw Retrofit calls in ViewModels** — only via Repositories
4. **No hardcoded strings in code** — use string resources
5. **Adapters** must extend `BaseAdapter` or `BasePagingAdapter`
6. **New screens** must extend `BaseFragment`/`BaseActivity`
7. **New ViewModels** must extend `BaseVM`
8. ViewHolders are inner classes named `VH` inside their Adapter class
9. BottomSheet dialogs extend `BaseBottomSheetDF`, named with `BD` suffix

---

## Package Layout (`:app`)

```
ui/
├── auth/          — phone login, OTP confirmation
├── home/          — home screen, nearby shops, notifications, loyalty cards
├── shop_details/  — shop info, products, images, working hours
├── order/         — order creation, checkout, modifications, cashback
├── qr_code/       — order history, QR code display, order info
├── profile/       — user profile, edit profile
│   ├── payment/   — top-up via payment services
│   └── subscriptions/ — subscription plans
├── search/        — shop/product search
├── map/           — map view with Yandex MapKit
└── on_boarding/   — first-launch onboarding
```

---

## Adding a New Feature Checklist

- [ ] Create data model in `data/models/`
- [ ] Create Retrofit service in `data/services/`
- [ ] Add service to `apiModule` in `Modules.kt`
- [ ] Create repository in `domain/repositories/`
- [ ] Add repository to `repositoryModule` in `Modules.kt`
- [ ] Create ViewModel extending `BaseVM` in `app/ui/{feature}/`
- [ ] Add ViewModel to `viewModelModule` in `AppModule.kt`
- [ ] Create Fragment extending `BaseFragment` or Activity extending `BaseActivity`
- [ ] Add layout XML in `app/src/main/res/layout/`
- [ ] Add string resources in all three `values*/strings.xml` files
- [ ] Register Activity in `AndroidManifest.xml` if needed
