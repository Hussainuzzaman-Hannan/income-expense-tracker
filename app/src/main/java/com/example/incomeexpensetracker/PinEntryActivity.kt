package com.example.incomeexpensetracker

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import java.util.*

/**
 * PinEntryActivity handles the PIN creation and verification for app access.
 * It's the first activity launched to ensure privacy.
 */
class PinEntryActivity : AppCompatActivity() {

    private lateinit var pinEditText: EditText
    private lateinit var confirmPinButton: Button
    private lateinit var pinInstructionText: TextView
    private lateinit var setPinText: TextView // This TextView is for the "No PIN set" message
    private lateinit var setNewPinButton: Button // This button is for initiating a PIN change

    private var currentPin: String? = null // Stores the currently saved PIN
    private var isSettingNewPin = false // Flag to track if the user is in the process of setting/changing a PIN

    private val PREFS_FILE_NAME = "app_prefs"
    private val PIN_KEY = "app_pin"
    private val LANGUAGE_KEY = "app_language" // For consistent language loading

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved language before super.onCreate()
        val languageCode = getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).getString(LANGUAGE_KEY, "en")
        languageCode?.let { setLocale(this, it) }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_entry)

        initUiElements()
        loadPin()
        setupListeners()

        // Check if the activity was launched specifically for changing PIN
        isSettingNewPin = intent.getBooleanExtra("CHANGE_PIN_MODE", false)

        updateUiForPinState() // Update UI based on initial PIN state
    }

    /**
     * Initializes all UI components by finding their respective IDs in the layout.
     * Uses string resources for text.
     */
    private fun initUiElements() {
        pinEditText = findViewById(R.id.pin_edit_text)
        confirmPinButton = findViewById(R.id.confirm_pin_button)
        pinInstructionText = findViewById(R.id.pin_instruction_text)
        setPinText = findViewById(R.id.set_pin_text)
        setNewPinButton = findViewById(R.id.set_new_pin_button)

        // Set hints/texts from string resources
        pinEditText.hint = getString(R.string.pin_hint)
        confirmPinButton.text = getString(R.string.confirm_button) // Default, will be updated by updateUiForPinState
        setPinText.text = getString(R.string.pin_not_set_message)
        setNewPinButton.text = getString(R.string.set_new_pin_button)
    }

    /**
     * Loads the saved PIN from SharedPreferences.
     */
    private fun loadPin() {
        val sharedPrefs = getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
        currentPin = sharedPrefs.getString(PIN_KEY, null)
    }

    /**
     * Saves the provided PIN to SharedPreferences.
     */
    private fun savePin(pin: String) {
        val sharedPrefs = getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString(PIN_KEY, pin)
            apply()
        }
        currentPin = pin // Update the in-memory currentPin as well
    }

    /**
     * Sets up click listeners for buttons.
     */
    private fun setupListeners() {
        confirmPinButton.setOnClickListener {
            if (isSettingNewPin) {
                attemptSetNewPin()
            } else {
                attemptPinVerification()
            }
        }

        setNewPinButton.setOnClickListener {
            // When "Set New PIN" button is clicked, transition to PIN setting mode
            isSettingNewPin = true
            updateUiForPinState()
        }
    }

    /**
     * Updates the UI based on whether a PIN is set or if the user is currently setting/changing a new PIN.
     */
    private fun updateUiForPinState() {
        pinEditText.text.clear() // Always clear input when state changes

        // Scenario 1: No PIN set at all (first launch or PIN cleared)
        if (currentPin == null) {
            pinInstructionText.text = getString(R.string.pin_instruction_set)
            setPinText.visibility = View.VISIBLE // Show "No PIN set. Please set a new PIN."
            setNewPinButton.visibility = View.GONE // Not applicable
            confirmPinButton.text = getString(R.string.set_pin_button)
            isSettingNewPin = true // Force setting a new PIN
        }
        // Scenario 2: PIN is set, and user is entering it to unlock
        else if (!isSettingNewPin) { // PIN exists and not in "setting new pin" mode
            pinInstructionText.text = getString(R.string.pin_instruction_enter)
            setPinText.visibility = View.GONE
            setNewPinButton.visibility = View.VISIBLE // Option to change PIN
            confirmPinButton.text = getString(R.string.unlock_button)
        }
        // Scenario 3: PIN is set, and user is in the process of changing it (triggered by setNewPinButton)
        else { // PIN exists, and isSettingNewPin is true
            pinInstructionText.text = getString(R.string.pin_instruction_confirm_new)
            setPinText.visibility = View.GONE
            setNewPinButton.visibility = View.GONE // Hide "Set New PIN" button while setting
            confirmPinButton.text = getString(R.string.confirm_new_pin_button)
        }
    }

    /**
     * Attempts to verify the entered PIN against the saved PIN.
     */
    private fun attemptPinVerification() {
        val enteredPin = pinEditText.text.toString()

        if (enteredPin.length != 4) {
            showSnackbar(getString(R.string.pin_length_error))
            return
        }

        if (enteredPin == currentPin) {
            showSnackbar(getString(R.string.pin_correct_unlocking))
            navigateToMainActivity()
        } else {
            showSnackbar(getString(R.string.pin_incorrect))
            pinEditText.text.clear()
        }
    }

    /**
     * Attempts to set a new PIN.
     * This function is used both for initial PIN setup and changing an existing PIN.
     */
    private fun attemptSetNewPin() {
        val enteredPin = pinEditText.text.toString()

        if (enteredPin.length != 4) {
            showSnackbar(getString(R.string.pin_length_error))
            return
        }

        val wasPinNullBeforeSave = (currentPin == null) // Check if PIN was null before saving
        savePin(enteredPin) // This updates currentPin internally

        showSnackbar(getString(R.string.pin_set_or_changed_success, if (wasPinNullBeforeSave) getString(R.string.pin_verb_set) else getString(R.string.pin_verb_changed)))

        // Reset state and update UI
        isSettingNewPin = false
        updateUiForPinState()

        // If it was the initial setup (PIN was null and not in change PIN mode), navigate to MainActivity.
        // If CHANGE_PIN_MODE was true, we don't navigate automatically.
        if (wasPinNullBeforeSave && !intent.getBooleanExtra("CHANGE_PIN_MODE", false)) {
            navigateToMainActivity()
        }
    }


    /**
     * Navigates to the MainActivity.
     */
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Close PinEntryActivity so user cannot go back to it
    }

    /**
     * Helper function to display Snackbar messages.
     */
    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Handles back button press to manage navigation within the PIN entry flow.
     */
    override fun onBackPressed() {
        if (isSettingNewPin && currentPin != null) {
            // If user was trying to change PIN, go back to the "Enter PIN" (unlock) state
            isSettingNewPin = false
            updateUiForPinState()
            showSnackbar(getString(R.string.pin_change_canceled))
        } else if (currentPin != null) {
            // If PIN is set and not in setting mode, prevent going back without successful authentication
            showSnackbar(getString(R.string.pin_enter_to_unlock))
        } else {
            // No PIN set (initial setup), allow exiting the app
            super.onBackPressed()
        }
    }

    /**
     * Sets the application's locale.
     * This method is duplicated here for consistency, but consider making it a utility function
     * if used in many activities.
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
}
