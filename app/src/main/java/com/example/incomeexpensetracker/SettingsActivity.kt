package com.example.incomeexpensetracker

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.snackbar.Snackbar
import java.text.NumberFormat
import java.util.*

/**
 * SettingsActivity provides options for the user to configure app settings,
 * such as changing the PIN, changing the language, and setting expense thresholds.
 */
class SettingsActivity : AppCompatActivity() {

    private val PREFS_FILE_NAME = "app_prefs"
    private val LANGUAGE_KEY = "app_language"
    private val EXPENSE_THRESHOLD_KEY = "expense_threshold"
    private val THEME_KEY = "app_theme" // New preference key for theme
    private val DEFAULT_EXPENSE_THRESHOLD = 5000.0 // Default value for the threshold

    private lateinit var currentThresholdTextView: TextView
    private lateinit var newThresholdEditText: EditText
    private lateinit var saveThresholdButton: Button
    private lateinit var toggleThemeButton: Button // Assuming a button for theme toggle for now

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved language and theme before super.onCreate()
        val languageCode = getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).getString(LANGUAGE_KEY, "en")
        languageCode?.let { setLocale(this, it) }

        val savedThemeMode = getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).getInt(THEME_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedThemeMode)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize UI elements and set click listeners
        val changePinButton: Button = findViewById(R.id.btn_change_pin_setting)
        val changeLanguageButton: Button = findViewById(R.id.btn_change_language_setting)
        currentThresholdTextView = findViewById(R.id.current_threshold_text_view)
        newThresholdEditText = findViewById(R.id.new_threshold_edit_text)
        saveThresholdButton = findViewById(R.id.save_threshold_button)
        toggleThemeButton = findViewById(R.id.btn_toggle_theme) // Initialize theme toggle button

        changePinButton.setOnClickListener {
            navigateToChangePin()
        }

        changeLanguageButton.setOnClickListener {
            showLanguageSelectionDialog()
        }

        saveThresholdButton.setOnClickListener {
            saveExpenseThreshold()
        }

        toggleThemeButton.setOnClickListener {
            toggleAppTheme()
        }

        updateCurrentThresholdDisplay()
        updateThemeButtonText() // Update the text of the theme toggle button
    }

    /**
     * Updates the displayed current expense threshold.
     */
    private fun updateCurrentThresholdDisplay() {
        val sharedPrefs = getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
        val currentThreshold = sharedPrefs.getFloat(EXPENSE_THRESHOLD_KEY, DEFAULT_EXPENSE_THRESHOLD.toFloat()).toDouble()
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("bn", "BD"))
        currentThresholdTextView.text = getString(R.string.current_threshold_label) + " " + currencyFormatter.format(currentThreshold)
    }

    /**
     * Saves the new expense threshold entered by the user.
     */
    private fun saveExpenseThreshold() {
        val newThresholdString = newThresholdEditText.text.toString()
        val newThreshold = newThresholdString.toDoubleOrNull()

        if (newThreshold == null || newThreshold <= 0) {
            showSnackbar(getString(R.string.error_invalid_threshold_amount))
            return
        }

        val sharedPrefs = getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putFloat(EXPENSE_THRESHOLD_KEY, newThreshold.toFloat())
            apply()
        }

        updateCurrentThresholdDisplay() // Update the display immediately
        newThresholdEditText.text.clear() // Clear input field

        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("bn", "BD"))
        showSnackbar(getString(R.string.threshold_saved_success, currencyFormatter.format(newThreshold)))
    }

    /**
     * Toggles between light and dark theme and saves the preference.
     */
    private fun toggleAppTheme() {
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        val newNightMode = when (currentNightMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_NO // Currently Dark, switch to Light
            else -> AppCompatDelegate.MODE_NIGHT_YES // Currently Light or System, switch to Dark
        }

        // Save the new theme preference
        getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).edit()
            .putInt(THEME_KEY, newNightMode)
            .apply()

        // Apply the new theme
        AppCompatDelegate.setDefaultNightMode(newNightMode)

        // Recreate the activity to apply theme changes immediately
        recreate()

        // Also, finish and restart MainActivity to ensure theme is applied there
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    /**
     * Updates the text of the theme toggle button based on the current theme mode.
     */
    private fun updateThemeButtonText() {
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            toggleThemeButton.text = getString(R.string.switch_to_light_theme)
        } else {
            toggleThemeButton.text = getString(R.string.switch_to_dark_theme)
        }
    }

    /**
     * Navigates to the PinEntryActivity in "change PIN" mode.
     */
    private fun navigateToChangePin() {
        val intent = Intent(this, PinEntryActivity::class.java).apply {
            putExtra("CHANGE_PIN_MODE", true)
        }
        startActivity(intent)
    }

    /**
     * Shows a dialog for language selection, similar to the one in MainActivity.
     */
    private fun showLanguageSelectionDialog() {
        val languages = arrayOf(getString(R.string.language_english), getString(R.string.language_bengali))
        val languageCodes = arrayOf("en", "bn")

        val currentLanguageCode = getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).getString(LANGUAGE_KEY, "en")
        val checkedItem = languageCodes.indexOf(currentLanguageCode)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_language_title))
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val selectedLanguageCode = languageCodes[which]
                if (selectedLanguageCode != currentLanguageCode) {
                    saveLanguagePreference(selectedLanguageCode)
                    setLocale(this, selectedLanguageCode)
                    dialog.dismiss()
                    // Recreate this activity and also finish and restart MainActivity to apply language change across the app
                    recreate()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish() // Finish SettingsActivity
                } else {
                    dialog.dismiss()
                }
            }
            .setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Saves the selected language preference to SharedPreferences.
     */
    private fun saveLanguagePreference(languageCode: String) {
        getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).edit()
            .putString(LANGUAGE_KEY, languageCode)
            .apply()
    }

    /**
     * Sets the application's locale. This method is crucial for changing the language.
     * It needs to be called before `super.onCreate()` in activities.
     */
    private fun setLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources: Resources = context.resources
        val config: Configuration = resources.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    /**
     * Helper function to display Snackbar messages.
     */
    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }
}
