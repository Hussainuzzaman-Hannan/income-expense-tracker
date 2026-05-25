package com.example.incomeexpensetracker

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

/**
 * A [Fragment] subclass used within the "Add Transaction" dialog to handle
 * input fields for either Income or Expense. Now supports editing existing transactions.
 */
class AddTransactionTabFragment : Fragment() {

    private lateinit var descriptionEditText: EditText
    private lateinit var amountEditText: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var dateTextView: TextView
    private lateinit var datePickerButton: ImageButton
    private lateinit var voiceDescriptionButton: ImageButton
    private lateinit var voiceAmountButton: ImageButton
    private lateinit var cancelButton: Button
    private lateinit var saveButton: Button // This will act as "Save" or "Update"

    private lateinit var transactionType: TransactionType // To know if it's income or expense tab
    private var transactionToEdit: Transaction? = null // Holds the transaction if in edit mode
    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.US)

    // Callbacks to communicate with the parent DialogFragment
    var onDismissListener: (() -> Unit)? = null
    var onTransactionAddSuccessListener: (() -> Unit)? = null

    // Speech recognition constants
    private val SPEECH_REQUEST_CODE_DESCRIPTION = 101
    private val SPEECH_REQUEST_CODE_AMOUNT = 102
    private val RECORD_AUDIO_PERMISSION_CODE = 200


    companion object {
        private const val ARG_TRANSACTION_TYPE = "transaction_type"
        private const val ARG_TRANSACTION_TO_EDIT = "transaction_to_edit"

        @JvmStatic
        fun newInstance(type: TransactionType, transactionToEdit: Transaction? = null) =
            AddTransactionTabFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_TRANSACTION_TYPE, type)
                    putSerializable(ARG_TRANSACTION_TO_EDIT, transactionToEdit)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            transactionType = it.getSerializable(ARG_TRANSACTION_TYPE) as TransactionType
            transactionToEdit = it.getSerializable(ARG_TRANSACTION_TO_EDIT) as? Transaction
            Log.d("AddTransactionTab", "Fragment created for type: $transactionType, with transaction to edit: ${transactionToEdit?.id}")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_add_transaction_tab_content, container, false)

        descriptionEditText = view.findViewById(R.id.dialog_description_edit_text)
        amountEditText = view.findViewById(R.id.dialog_amount_edit_text)
        categorySpinner = view.findViewById(R.id.dialog_category_spinner)
        dateTextView = view.findViewById(R.id.dialog_date_text_view)
        datePickerButton = view.findViewById(R.id.dialog_date_picker_button)
        voiceDescriptionButton = view.findViewById(R.id.dialog_voice_description_button)
        voiceAmountButton = view.findViewById(R.id.dialog_voice_amount_button)
        cancelButton = view.findViewById(R.id.dialog_cancel_button)
        saveButton = view.findViewById(R.id.dialog_save_button)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("AddTransactionTab", "onViewCreated called for type: $transactionType")

        setupCategorySpinner()
        setupListeners() // Set up listeners before pre-populating to ensure they are ready

        // Pre-populate fields if in edit mode and this tab matches the transaction's type
        if (transactionToEdit != null && transactionToEdit?.type == transactionType) {
            prepopulateFields(transactionToEdit!!)
            saveButton.text = getString(R.string.save_button) // Or a specific "Update" string if you have one
        } else {
            updateDateInView() // Only update date if not editing or not the correct tab
            saveButton.text = getString(R.string.save_button) // Default "Save" for new transaction
        }

        // Request focus for the description field to try and bring up the keyboard
        descriptionEditText.requestFocus()
        // Show keyboard explicitly
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    private fun setupCategorySpinner() {
        val categoriesArrayId = when (transactionType) {
            TransactionType.INCOME -> R.array.income_categories
            TransactionType.EXPENSE -> R.array.expense_categories
        }
        val categories = resources.getStringArray(categoriesArrayId).toList()
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, categories)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }

    /**
     * Pre-populates the input fields with the provided transaction data.
     */
    private fun prepopulateFields(transaction: Transaction) {
        descriptionEditText.setText(transaction.description)
        amountEditText.setText(transaction.amount.toString())
        dateTextView.text = transaction.date

        // Set the spinner selection
        val categoriesArrayId = when (transactionType) {
            TransactionType.INCOME -> R.array.income_categories
            TransactionType.EXPENSE -> R.array.expense_categories
        }
        val categories = resources.getStringArray(categoriesArrayId).toList()
        val categoryIndex = categories.indexOf(transaction.category)
        if (categoryIndex != -1) {
            categorySpinner.setSelection(categoryIndex)
        }

        // Set the calendar to the transaction's date for DatePickerDialog
        try {
            val date = dateFormatter.parse(transaction.date)
            date?.let { calendar.time = it }
        } catch (e: Exception) {
            Log.e("AddTransactionTab", "Error parsing date for pre-population: ${transaction.date}", e)
        }
        Log.d("AddTransactionTab", "Fields pre-populated for transaction ID: ${transaction.id}")
    }

    private fun updateDateInView() {
        dateTextView.text = dateFormatter.format(calendar.time)
    }

    private fun setupListeners() {
        dateTextView.setOnClickListener { showDatePickerDialog() }
        datePickerButton.setOnClickListener { showDatePickerDialog() }

        voiceDescriptionButton.setOnClickListener {
            checkAudioPermissionAndStartSpeechRecognition(SPEECH_REQUEST_CODE_DESCRIPTION)
        }
        voiceAmountButton.setOnClickListener {
            checkAudioPermissionAndStartSpeechRecognition(SPEECH_REQUEST_CODE_AMOUNT)
        }

        cancelButton.setOnClickListener {
            (activity as? MainActivity)?.showSnackbar(getString(R.string.transaction_add_cancelled))
            onDismissListener?.invoke() // Call the dismiss listener to close the dialog
            hideKeyboard()
        }

        saveButton.setOnClickListener {
            saveOrUpdateTransaction()
            hideKeyboard()
        }
    }

    private fun showDatePickerDialog() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }

        DatePickerDialog(
            requireContext(),
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveOrUpdateTransaction() {
        val description = descriptionEditText.text.toString().trim()
        val amountString = amountEditText.text.toString()
        val date = dateTextView.text.toString()
        val category = categorySpinner.selectedItem.toString()

        if (description.isEmpty()) {
            (activity as? MainActivity)?.showSnackbar(getString(R.string.error_enter_description))
            return
        }

        val amount = amountString.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            (activity as? MainActivity)?.showSnackbar(getString(R.string.error_valid_positive_amount))
            return
        }

        if (transactionToEdit != null) {
            // This is an update operation
            val updatedTransaction = transactionToEdit!!.copy(
                description = description,
                amount = amount,
                date = date,
                category = category
            )
            (activity as? MainActivity)?.updateTransaction(updatedTransaction)
            Log.d("AddTransactionTab", "Updating transaction: ${updatedTransaction.id}")
        } else {
            // This is a new transaction addition
            val newTransaction = Transaction(
                description = description,
                amount = amount,
                type = transactionType,
                date = date,
                category = category
            )
            (activity as? MainActivity)?.addTransaction(newTransaction)
            Log.d("AddTransactionTab", "Adding new transaction: ${newTransaction.id}")
        }

        onTransactionAddSuccessListener?.invoke() // Notify success listener
        onDismissListener?.invoke() // Call the dismiss listener to close the dialog
    }

    /**
     * Checks audio permission and starts speech recognition.
     */
    private fun checkAudioPermissionAndStartSpeechRecognition(requestCode: Int) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Request permission from the fragment
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_CODE)
        } else {
            startSpeechRecognition(requestCode)
        }
    }

    /**
     * Starts the speech recognition intent.
     */
    private fun startSpeechRecognition(requestCode: Int) {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            (activity as? MainActivity)?.showSnackbar("Speech recognition not available on this device.")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()) // Use device's current locale
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_listening))
        }
        try {
            startActivityForResult(intent, requestCode)
        } catch (e: Exception) {
            e.printStackTrace()
            (activity as? MainActivity)?.showSnackbar(getString(R.string.speech_error_unknown))
        }
    }

    /**
     * Handles the result of permission requests.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, now start speech recognition based on which button was clicked
                // This requires a way to know which text field was active.
                // A simpler approach is to trigger it immediately after permission grant
                // For now, let's just acknowledge
                (activity as? MainActivity)?.showSnackbar("Audio recording permission granted. Please try voice input again.")
            } else {
                (activity as? MainActivity)?.showSnackbar(getString(R.string.permission_denied_voice_input))
            }
        }
    }

    /**
     * Handles the result of activities launched for results (e.g., speech-to-text).
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val spokenText: String? = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            spokenText?.let {
                when (requestCode) {
                    SPEECH_REQUEST_CODE_DESCRIPTION -> descriptionEditText.setText(it)
                    SPEECH_REQUEST_CODE_AMOUNT -> {
                        val number = it.filter { char -> char.isDigit() || char == '.' }.toDoubleOrNull()
                        if (number != null) {
                            amountEditText.setText(number.toString())
                        } else {
                            (activity as? MainActivity)?.showSnackbar(getString(R.string.speech_error_no_match))
                        }
                    }
                    else -> {
                        // Handle any other request codes that might come back to this fragment
                        // For now, we'll just show a generic snackbar for unhandled codes.
                        (activity as? MainActivity)?.showSnackbar("Unhandled request code: $requestCode")
                    }
                }
            } ?: (activity as? MainActivity)?.showSnackbar(getString(R.string.speech_error_no_match))
        } else if (resultCode == Activity.RESULT_CANCELED) {
            (activity as? MainActivity)?.showSnackbar(getString(R.string.speech_error_no_match))
        }
    }

    /**
     * Hides the software keyboard.
     */
    private fun hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}
