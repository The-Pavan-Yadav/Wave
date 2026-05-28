package com.example

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.os.Build

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class MainActivityTest {
    @Test
    fun `test activity launches without crashing`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        println("FIREBASE APPS SIZE: " + com.google.firebase.FirebaseApp.getApps(context).size)
        // Wait for coroutines to execute, including the splash screen delay
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }
}
