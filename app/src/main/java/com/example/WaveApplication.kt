package com.example

import android.app.Application
import android.util.Log
import com.example.services.FirebaseManager

class WaveApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Set up clean global Uncaught Exception handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("WaveApplication", "FATAL UNCAUGHT EXCEPTION on thread ${thread.name}: ${throwable.message}", throwable)
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            }
        }

        try {
            // Gaurantee early manual initialization of Firebase matching context
            FirebaseManager.initialize(this)
            Log.d("WaveApplication", "Firebase manually initialized during application startup.")
        } catch (e: Throwable) {
            Log.e("WaveApplication", "Failed to initialize Firebase in Application.onCreate", e)
        }
    }
}
