package uz.alphazet.hoopla

import android.app.Application

/**
 * Minimal Application for Robolectric tests.
 * Replaces [App] so that MapKit initialization and production Koin setup are skipped.
 * Each test that needs Koin starts it via [KoinTestRule].
 */
class TestApp : Application()
