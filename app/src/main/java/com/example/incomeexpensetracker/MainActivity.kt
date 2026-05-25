package com.example.incomeexpensetracker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log // Import Log for debugging
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AlertDialog // Keep this import for other dialogs if needed

/**
 * MainActivity is the primary screen of the income/expense tracker application.
 * It now uses a TabLayout and ViewPager2 to manage different sections (Total Accounts, All Accounts).
 */
class MainActivity : AppCompatActivity() {

    //region UI Element Declarations
    private lateinit var mainTabLayout: TabLayout
    private lateinit var mainViewPager: ViewPager2
    private lateinit var menuButton: ImageButton
    private lateinit var fabAddTransaction: com.google.android.material.floatingactionbutton.FloatingActionButton // Fully qualify
    //endregion

    // Calendar instance to hold the selected date (kept for dialog use)
    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.US) // Consistent date formatter

    // Speech recognition related variables
    private val RECORD_AUDIO_PERMISSION_CODE = 100
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent
    private var isListeningForDescription = false
    private var isListeningForAmount = false

    // Global list of transactions - shared with fragments
    val allTransactions = mutableListOf<Transaction>()

    private val GSON = Gson()
    private val TRANSACTIONS_FILE_NAME = "transactions.json"

    private val CREATE_CSV_FILE = 200
    private val CREATE_PDF_FILE = 201

    private val PREFS_FILE_NAME = "app_prefs"
    private val LANGUAGE_KEY = "app_language"
    private val EXPENSE_THRESHOLD_KEY = "expense_threshold"
    private val DEFAULT_EXPENSE_THRESHOLD = 5000.0

    // Store fragment instances directly
    private lateinit var totalAccountsFragment: TotalAccountsFragment
    private lateinit var allAccountsFragment: AllAccountsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        val languageCode = getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).getString(LANGUAGE_KEY, "en")
        languageCode?.let { setLocale(this, it) }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("MainActivity", "onCreate called.")

        initUiElements()
        loadTransactions() // Load transactions when activity is created
        setupViewPagerWithTabs()
        setupSpeechRecognizer()
        setupListeners()

        // --- IMPORTANT FIX: Removed the premature call to updateFragmentsUI() here ---
        // The fragments will handle their initial update via their onViewCreated/onResume methods.
        // Log.d("MainActivity", "Initial updateFragmentsUI triggered with ${allTransactions.size} transactions.")
    }

    override fun onStop() {
        super.onStop()
        saveTransactions() // Save transactions when activity is stopped
        Log.d("MainActivity", "onStop called, transactions saved.")
    }

    override fun onResume() {
        super.onResume()
        // This is crucial: when MainActivity resumes (e.g., after returning from AddTransactionDialog),
        // we want to ensure all visible fragments are up to date.
        updateFragmentsUI() // Keep this call, it happens when fragments' views are ready
        Log.d("MainActivity", "MainActivity onResume called, re-triggering UI update.")
    }

    /**
     * Initializes all the UI components by finding them by their respective IDs in the layout.
     */
    private fun initUiElements() {
        mainTabLayout = findViewById(R.id.main_tab_layout)
        mainViewPager = findViewById(R.id.main_view_pager)
        menuButton = findViewById(R.id.menu_button)
        fabAddTransaction = findViewById(R.id.fab_add_transaction)
        Log.d("MainActivity", "UI elements initialized.")
    }

    /**
     * Sets up the ViewPager2 with a FragmentStateAdapter and links it to the TabLayout.
     */
    private fun setupViewPagerWithTabs() {
        // Instantiate fragments here and store references
        totalAccountsFragment = TotalAccountsFragment()
        allAccountsFragment = AllAccountsFragment()

        val fragmentList = arrayListOf(
            totalAccountsFragment,
            allAccountsFragment
        )

        mainViewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = fragmentList.size
            override fun createFragment(position: Int): Fragment = fragmentList[position]
            // Override getItemId and containsItem to help FragmentStateAdapter manage state correctly
            // For fixed number of fragments, a simple position-based stable ID is recommended.
            override fun getItemId(position: Int): Long {
                return when (position) {
                    0 -> 0L // ID for TotalAccountsFragment
                    1 -> 1L // ID for AllAccountsFragment
                    else -> position.toLong() // Fallback, though not expected for 2 fragments
                }
            }
            override fun containsItem(itemId: Long): Boolean {
                return itemId == 0L || itemId == 1L
            }
        }

        TabLayoutMediator(mainTabLayout, mainViewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_total_accounts)
                1 -> getString(R.string.tab_all_accounts)
                else -> ""
            }
        }.attach()
        Log.d("MainActivity", "ViewPager and TabLayout set up.")
    }

    /**
     * Consolidates all main activity listeners.
     */
    private fun setupListeners() {
        menuButton.setOnClickListener { view -> showPopupMenu(view) }
        fabAddTransaction.setOnClickListener {
            showAddTransactionDialog()
        }
        Log.d("MainActivity", "Listeners set up.")
    }

    private fun setupSpeechRecognizer() {
        val currentLanguageCode = getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).getString(LANGUAGE_KEY, "en") ?: "en"

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { showSnackbar(getString(R.string.speech_listening)) }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { showSnackbar(getString(R.string.speech_processing)) }
            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> getString(R.string.speech_error_audio)
                    SpeechRecognizer.ERROR_CLIENT -> getString(R.string.speech_error_client)
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> getString(R.string.speech_error_insufficient_permissions)
                    SpeechRecognizer.ERROR_NETWORK -> getString(R.string.speech_error_network)
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> getString(R.string.speech_error_network_timeout)
                    SpeechRecognizer.ERROR_NO_MATCH -> getString(R.string.speech_error_no_match)
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> getString(R.string.speech_error_recognizer_busy)
                    SpeechRecognizer.ERROR_SERVER -> getString(R.string.speech_error_server)
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> getString(R.string.speech_error_speech_timeout)
                    else -> getString(R.string.speech_error_unknown)
                }
                showSnackbar(getString(R.string.speech_recognition_error_prefix, errorMessage))
                isListeningForDescription = false
                isListeningForAmount = false
                Log.e("SpeechRecognizer", "Error: $errorMessage (Code: $error)") // Log error
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    Log.d("SpeechRecognizer", "Recognized text: $spokenText") // Log recognized text
                    // This part is now handled within the dialog fragments
                } else {
                    Log.d("SpeechRecognizer", "No speech recognized.") // Log no match
                    showSnackbar(getString(R.string.speech_error_no_match))
                }
                isListeningForDescription = false
                isListeningForAmount = false
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguageCode)
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_listening))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        Log.d("MainActivity", "Speech recognizer set up.")
    }

    /**
     * Checks audio permission and starts speech recognition.
     * This function is now called from the dialog for specific EditTexts.
     */
    fun checkAudioPermissionAndStartSpeechRecognition(forDescription: Boolean, targetEditText: EditText) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_CODE)
            Log.d("MainActivity", "Requesting RECORD_AUDIO permission.")
        } else {
            startSpeechRecognition(forDescription, targetEditText)
            Log.d("MainActivity", "RECORD_AUDIO permission already granted, starting speech recognition.")
        }
    }

    private fun startSpeechRecognition(forDescription: Boolean, targetEditText: EditText) {
        isListeningForDescription = forDescription
        isListeningForAmount = !forDescription

        // Set a new listener specific to this speech recognition instance
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { showSnackbar(getString(R.string.speech_listening)) }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { showSnackbar(getString(R.string.speech_processing)) }
            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> getString(R.string.speech_error_audio)
                    SpeechRecognizer.ERROR_CLIENT -> getString(R.string.speech_error_client)
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> getString(R.string.speech_error_insufficient_permissions)
                    SpeechRecognizer.ERROR_NETWORK -> getString(R.string.speech_error_network)
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> getString(R.string.speech_error_network_timeout)
                    SpeechRecognizer.ERROR_NO_MATCH -> getString(R.string.speech_error_no_match)
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> getString(R.string.speech_error_recognizer_busy)
                    SpeechRecognizer.ERROR_SERVER -> getString(R.string.speech_error_server)
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> getString(R.string.speech_error_speech_timeout)
                    else -> getString(R.string.speech_error_unknown)
                }
                showSnackbar(getString(R.string.speech_recognition_error_prefix, errorMessage))
                Log.e("SpeechRecognizer", "Error: $errorMessage (Code: $error)") // Log error
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    Log.d("SpeechRecognizer", "Recognized text: $spokenText") // Log recognized text
                    if (isListeningForDescription) {
                        targetEditText.setText(spokenText)
                    } else if (isListeningForAmount) {
                        val number = spokenText.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
                        if (number != null) {
                            targetEditText.setText(number.toString())
                        } else {
                            showSnackbar(getString(R.string.error_valid_positive_amount))
                        }
                    }
                } else {
                    showSnackbar(getString(R.string.speech_error_no_match))
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer.startListening(speechRecognizerIntent)
        Log.d("MainActivity", "Speech recognition started for targetEditText: ${targetEditText.id}")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSnackbar("Audio recording permission granted. Please try voice input again.")
                Log.d("MainActivity", "RECORD_AUDIO permission granted.")
            } else {
                showSnackbar(getString(R.string.permission_denied_voice_input))
                Log.d("MainActivity", "RECORD_AUDIO permission denied.")
            }
        }
    }


    /**
     * Displays a dialog for adding a new transaction (income or expense) with tabs.
     * Now uses a DialogFragment for better management.
     */
    private fun showAddTransactionDialog(transactionToEdit: Transaction? = null) {
        val dialogFragment = AddTransactionDialogFragment.newInstance(transactionToEdit)
        dialogFragment.setOnTransactionAddedListener {
            // This callback is triggered when a transaction is successfully added (or updated) and the dialog dismisses.
            updateFragmentsUI() // Now this call is after a transaction is added/updated
            Log.d("MainActivity", "Transaction added/updated via dialog, triggering fragment UI update.")
        }
        dialogFragment.show(supportFragmentManager, "AddTransactionDialog")
        Log.d("MainActivity", "Add Transaction dialog shown.")
    }

    /**
     * Public method to display the Add/Edit Transaction dialog for editing.
     * Called from AllAccountsFragment when an item is clicked.
     */
    fun showEditTransactionDialog(transaction: Transaction) {
        Log.d("MainActivity", "Showing Edit Transaction dialog for ID: ${transaction.id}")
        showAddTransactionDialog(transaction)
    }

    /**
     * Handles adding a transaction from the dialog.
     * This function is now called from AddTransactionTabFragment.
     */
    fun addTransaction(transaction: Transaction) {
        allTransactions.add(0, transaction) // Add to the beginning for latest first
        saveTransactions() // Save after adding a transaction
        Log.d("MainActivity", "Transaction added: ${transaction.description}, Amount: ${transaction.amount}, Total transactions: ${allTransactions.size}")

        // The updateFragmentsUI() call is now handled by the AddTransactionDialogFragment's listener.
        showSnackbar(getString(R.string.transaction_added_success))
        checkHighExpenseAlert(transaction)
    }

    /**
     * Updates an existing transaction.
     * This is called from AddTransactionTabFragment when editing.
     */
    fun updateTransaction(updatedTransaction: Transaction) {
        val index = allTransactions.indexOfFirst { it.id == updatedTransaction.id }
        if (index != -1) {
            allTransactions[index] = updatedTransaction
            saveTransactions()
            Log.d("MainActivity", "Transaction updated: ${updatedTransaction.id}, Total transactions: ${allTransactions.size}")
            updateFragmentsUI() // Trigger UI update after modifying data
            showSnackbar(getString(R.string.transaction_updated_success)) // Use a string resource for this
        } else {
            Log.w("MainActivity", "Attempted to update non-existent transaction with ID: ${updatedTransaction.id}")
        }
    }

    /**
     * Deletes a transaction by its ID.
     * This function is called from AllAccountsFragment.
     */
    fun deleteTransaction(transactionId: String) {
        val transactionToDelete = allTransactions.find { it.id == transactionId }
        if (transactionToDelete != null) {
            allTransactions.remove(transactionToDelete)
            saveTransactions()
            Log.d("MainActivity", "Transaction deleted: $transactionId, Remaining transactions: ${allTransactions.size}")

            // Notify active fragments to update their UI
            updateFragmentsUI()

            showSnackbar(getString(R.string.transaction_deleted))
        } else {
            Log.w("MainActivity", "Attempted to delete non-existent transaction with ID: $transactionId")
        }
    }

    /**
     * Helper to update all currently visible fragments.
     * This method directly calls the updateFragmentUI method on the stored fragment instances.
     */
    fun updateFragmentsUI() { // Made public so it can be called from the dialog fragment
        Log.d("MainActivity", "updateFragmentsUI called. Current total transactions: ${allTransactions.size}")

        // These calls are now safe because updateFragmentsUI will only be called from onResume
        // (when fragments are guaranteed to have their views created)
        // or from the dialog's callback (after a transaction is added/updated, which also ensures views are ready).
        totalAccountsFragment.updateFragmentUI(allTransactions)
        allAccountsFragment.updateFragmentUI(allTransactions)
    }


    private fun saveTransactions() {
        try {
            val jsonString = GSON.toJson(allTransactions)
            val file = File(filesDir, TRANSACTIONS_FILE_NAME)
            FileWriter(file).use { writer ->
                writer.write(jsonString)
            }
            Log.d("MainActivity", "Transactions saved successfully. Count: ${allTransactions.size} to ${file.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Error saving transactions: ${e.message}")
            showSnackbar(getString(R.string.error_saving_transactions, e.message))
        }
    }

    private fun loadTransactions() {
        try {
            val file = File(filesDir, TRANSACTIONS_FILE_NAME)
            if (file.exists()) {
                FileReader(file).use { reader ->
                    val listType = object : TypeToken<MutableList<Transaction>>() {}.type
                    val loadedList: MutableList<Transaction> = GSON.fromJson(reader, listType)
                    allTransactions.clear()
                    allTransactions.addAll(loadedList)
                    Log.d("MainActivity", "Transactions loaded successfully. Count: ${allTransactions.size} from ${file.absolutePath}")
                }
            } else {
                Log.d("MainActivity", "Transactions file does not exist at ${file.absolutePath}. Starting with empty list.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Error loading transactions: ${e.message}")
            showSnackbar(getString(R.string.error_loading_transactions, e.message))
            allTransactions.clear() // Clear in case of corrupted data
        }
    }

    private fun checkHighExpenseAlert(newExpense: Transaction) {
        if (newExpense.type == TransactionType.EXPENSE) {
            val sharedPrefs = getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
            val expenseThreshold = sharedPrefs.getFloat(EXPENSE_THRESHOLD_KEY, DEFAULT_EXPENSE_THRESHOLD.toFloat()).toDouble()

            if (newExpense.amount > expenseThreshold) {
                val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("bn", "BD"))
                val formattedExpense = currencyFormatter.format(newExpense.amount)
                val formattedThreshold = currencyFormatter.format(expenseThreshold)

                showSnackbar(getString(R.string.high_expense_alert, formattedExpense, newExpense.category, formattedThreshold))
                Log.d("MainActivity", "High expense alert triggered for ${newExpense.amount} in ${newExpense.category}.")
            }
        }
    }

    private fun exportTransactionsToCsv() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "transactions_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.csv")
        }
        startActivityForResult(intent, CREATE_CSV_FILE)
        Log.d("MainActivity", "Export CSV initiated.")
    }

    private fun exportTransactionsToPdf() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "transactions_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf")
        }
        startActivityForResult(intent, CREATE_PDF_FILE)
        Log.d("MainActivity", "Export PDF initiated.")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                when (requestCode) {
                    CREATE_CSV_FILE -> writeCsvToFile(uri)
                    CREATE_PDF_FILE -> writePdfToFile(uri)
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            showSnackbar(getString(R.string.file_export_cancelled))
            Log.d("MainActivity", "File export cancelled by user.")
        }
    }

    private fun writeCsvToFile(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val writer = outputStream.writer()
                writer.append("ID,Description,Amount,Type,Category,Date,Timestamp\n")
                allTransactions.forEach { transaction ->
                    writer.append("${transaction.id},")
                    writer.append("${escapeCsv(transaction.description)},")
                    writer.append("${transaction.amount},")
                    writer.append("${transaction.type},")
                    writer.append("${escapeCsv(transaction.category)},")
                    writer.append("${transaction.date},")
                    writer.append("${transaction.timestamp}\n")
                }
                writer.flush()
                showSnackbar(getString(R.string.csv_export_success))
                Log.d("MainActivity", "CSV exported successfully to $uri.")
            }
        }
        catch (e: IOException) {
            e.printStackTrace()
            Log.e("MainActivity", "Error writing CSV to file: ${e.message}")
            showSnackbar(getString(R.string.error_exporting_csv, e.message))
        }
    }

    private fun escapeCsv(data: String): String {
        return if (data.contains(",") || data.contains("\"")) {
            "\"${data.replace("\"", "\"\"")}\""
        } else {
            data
        }
    }

    private fun writePdfToFile(uri: Uri) {
        try {
            PdfGenerator.generatePdf(applicationContext, uri, allTransactions)
            showSnackbar(getString(R.string.pdf_export_success))
            Log.d("MainActivity", "PDF exported successfully to $uri.")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Error exporting PDF: ${e.message}")
            showSnackbar(getString(R.string.error_exporting_pdf, e.message))
        }
    }

    /**
     * Displays a short message (Snackbar) to the user.
     */
    fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * This method is called when the menu button is clicked.
     */
    fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view, Gravity.END)
        popup.menuInflater.inflate(R.menu.overflow_menu, popup.menu) // Use overflow_menu.xml

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.menu_monthly_report -> {
                    navigateToReportsActivity("Monthly")
                    true
                }
                R.id.menu_weekly_report -> {
                    navigateToReportsActivity("Weekly")
                    true
                }
                R.id.menu_debt_receivables -> { // Handle new Debt & Receivables menu item
                    val intent = Intent(this, DebtReceivablesActivity::class.java)
                    startActivity(intent)
                    Log.d("MainActivity", "Navigating to DebtReceivablesActivity.")
                    true
                }
                R.id.menu_change_pin -> {
                    navigateToChangePin()
                    true
                }
                R.id.menu_export_csv -> {
                    exportTransactionsToCsv()
                    true
                }
                R.id.menu_export_pdf -> {
                    exportTransactionsToPdf()
                    true
                }
                R.id.menu_settings -> {
                    navigateToSettingsActivity()
                    true
                }
                R.id.menu_change_language -> {
                    navigateToSettingsActivity()
                    true
                }
                R.id.menu_reset_transactions -> { // Handle new reset option
                    showResetTransactionsConfirmationDialog()
                    true
                }
                R.id.menu_about -> {
                    showSnackbar(getString(R.string.about_clicked_message))
                    true
                }
                else -> false
            }
        }
        popup.show()
        Log.d("MainActivity", "Popup menu shown.")
    }

    /**
     * Shows a confirmation dialog before resetting all transactions.
     */
    private fun showResetTransactionsConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_reset_title))
            .setMessage(getString(R.string.dialog_reset_message))
            .setPositiveButton(getString(R.string.dialog_reset_confirm)) { dialog, _ ->
                resetAllTransactions()
                dialog.dismiss()
            }
            .setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Resets (deletes) all transactions, saves the empty list, and updates the UI.
     */
    private fun resetAllTransactions() {
        allTransactions.clear()
        saveTransactions() // Save the now empty list
        updateFragmentsUI() // Update UI to reflect empty state
        showSnackbar(getString(R.string.transactions_reset_success))
        Log.d("MainActivity", "All transactions have been reset.")
    }

    private fun navigateToReportsActivity(reportType: String) {
        if (allTransactions.isEmpty()) {
            showSnackbar(getString(R.string.report_no_transactions))
            Log.d("MainActivity", "Attempted to navigate to reports, but no transactions found.")
            return
        }
        val intent = Intent(this, ReportsActivity::class.java).apply {
            putExtra(ReportsActivity.EXTRA_TRANSACTIONS, ArrayList(allTransactions))
            putExtra(ReportsActivity.EXTRA_REPORT_TYPE, reportType)
        }
        startActivity(intent)
        Log.d("MainActivity", "Navigating to ReportsActivity for $reportType report.")
    }

    private fun navigateToChangePin() {
        val intent = Intent(this, PinEntryActivity::class.java).apply {
            putExtra("CHANGE_PIN_MODE", true)
        }
        startActivity(intent)
        Log.d("MainActivity", "Navigating to PinEntryActivity for PIN change.")
    }

    private fun navigateToSettingsActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        Log.d("MainActivity", "Navigating to SettingsActivity.")
    }

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
        Log.d("MainActivity", "Locale set to $languageCode")
    }
}