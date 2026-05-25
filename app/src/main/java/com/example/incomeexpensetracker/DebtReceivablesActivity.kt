package com.example.incomeexpensetracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity for managing Debts and Receivables.
 * Currently a placeholder, will be expanded with full functionality.
 */
class DebtReceivablesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debt_receivables)

        // Set up action bar for back navigation and title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.menu_debt_receivables)
    }

    /**
     * Handles the back button in the action bar.
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
