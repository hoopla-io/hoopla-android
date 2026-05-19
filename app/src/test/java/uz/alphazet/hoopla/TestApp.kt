package uz.alphazet.hoopla

import android.app.Application

/**
 * Minimal Application for Robolectric tests.
 * Replaces [App] so that production Koin setup is skipped.
 * Each test that needs Koin starts it via [KoinTestRule].
 */
class TestApp : Application()
