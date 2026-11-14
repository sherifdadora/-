package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.MainViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class MainViewModelStartupTest {

    @Test
    fun mainViewModel_initializesWithoutInitOrderNullPointerException() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)
        ShadowLooper.idleMainLooper()

        assertNotNull(viewModel.syncStatus.value)
        assertFalse(viewModel.isSyncing.value)
    }
}
