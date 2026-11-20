package com.eeseka.nimbat

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.eeseka.nimbat.core.domain.settings.AppTheme
import com.eeseka.nimbat.testing.collectInBackground
import com.eeseka.nimbat.testing.data.FakeAppPreferences
import com.eeseka.nimbat.testing.data.FakeOnboardingStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var appPreferences: FakeAppPreferences
    private lateinit var onboardingStorage: FakeOnboardingStorage

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        appPreferences = FakeAppPreferences()
        onboardingStorage = FakeOnboardingStorage()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a first launch starts with onboarding`() = runTest {
        val viewModel = createViewModel()

        assertThat(viewModel.state.value.isCheckingOnboarding).isFalse()
        assertThat(viewModel.state.value.hasSeenOnboarding).isFalse()
    }

    @Test
    fun `a finished onboarding is skipped next launch`() = runTest {
        onboardingStorage.setOnboardingCompleted()

        val viewModel = createViewModel()

        assertThat(viewModel.state.value.hasSeenOnboarding).isTrue()
    }

    @Test
    fun `the theme comes from the saved settings`() = runTest {
        appPreferences.setTheme(AppTheme.DARK)

        val viewModel = createViewModel()

        assertThat(viewModel.state.value.theme).isEqualTo(AppTheme.DARK)
    }

    @Test
    fun `pure black comes from the saved settings`() = runTest {
        appPreferences.setPureBlackEnabled(true)

        val viewModel = createViewModel()

        assertThat(viewModel.state.value.isPureBlackEnabled).isTrue()
    }

    @Test
    fun `haptics come from the saved settings`() = runTest {
        appPreferences.setHapticsEnabled(false)

        val viewModel = createViewModel()

        assertThat(viewModel.state.value.areHapticsEnabled).isFalse()
    }

    private fun TestScope.createViewModel(): MainViewModel {
        val viewModel = MainViewModel(
            appPreferences = appPreferences,
            onboardingStorage = onboardingStorage
        )
        collectInBackground(viewModel.state)
        advanceUntilIdle()
        return viewModel
    }
}
