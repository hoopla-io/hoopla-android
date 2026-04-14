package uz.alphazet.hoopla.rules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit 4 rule that replaces Dispatchers.Main with a test dispatcher for the
 * duration of each test. All ViewModels use viewModelScope, which ties to
 * Dispatchers.Main.immediate — this rule makes coroutines launched via
 * `BaseVM.launch {}` run on the test dispatcher instead.
 *
 * UnconfinedTestDispatcher is chosen over StandardTestDispatcher so that
 * `launch {}` blocks execute eagerly without needing `advanceUntilIdle()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
