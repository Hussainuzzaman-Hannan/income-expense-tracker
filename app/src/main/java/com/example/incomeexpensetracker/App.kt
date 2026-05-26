package com.example.incomeexpensetracker

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Custom Application class.
 * এটি app চালু হওয়ার সাথে সাথেই theme apply করে,
 * যাতে PinEntryActivity সহ সব Activity-তে dark mode কাজ করে।
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // SharedPreferences থেকে saved theme পড়ো।
        // Default: MODE_NIGHT_YES (Dark Mode)
        val savedTheme = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getInt("app_theme", AppCompatDelegate.MODE_NIGHT_YES)

        AppCompatDelegate.setDefaultNightMode(savedTheme)
    }
}
