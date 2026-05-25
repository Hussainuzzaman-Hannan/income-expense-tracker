// TotalAccountsFragment.kt
package com.example.incomeexpensetracker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.NumberFormat
import java.util.*

class TotalAccountsFragment : Fragment() {

    private lateinit var totalIncomeTextView: TextView
    private lateinit var totalExpenseTextView: TextView
    private lateinit var netBalanceTextView: TextView
    private lateinit var currencyFormatter: NumberFormat

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_total_accounts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI elements here (Crucial Step!)
        totalIncomeTextView = view.findViewById(R.id.totalIncomeTextView)
        totalExpenseTextView = view.findViewById(R.id.totalExpenseTextView)
        netBalanceTextView = view.findViewById(R.id.netBalanceTextView)

        // Initialize currency formatter based on app's locale (assuming MainActivity sets it)
        val currentLocale = Locale(
            requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getString("app_language", "en") ?: "en",
            "BD" // Assuming Bangladesh for Taka, adjust if currency is dynamic
        )
        currencyFormatter = NumberFormat.getCurrencyInstance(currentLocale)

        // Initial update when the fragment's view is created.
        // This ensures data is displayed even if MainActivity.onResume() is slightly delayed or not called.
        // We'll still keep MainActivity.onResume()'s call for general updates too.
        (activity as? MainActivity)?.let { mainActivity ->
            updateFragmentUI(mainActivity.allTransactions)
        }
        Log.d("TotalAccountsFragment", "onViewCreated: UI elements initialized and initial update attempted.")
    }

    /**
     * Updates the UI of this fragment with the latest transaction data.
     * This method is called by MainActivity to refresh the view.
     */
    fun updateFragmentUI(transactions: List<Transaction>) {
        // Check if views are initialized before accessing them
        if (!::totalIncomeTextView.isInitialized || !::totalExpenseTextView.isInitialized || !::netBalanceTextView.isInitialized) {
            Log.e("TotalAccountsFragment", "updateFragmentUI called but TextViews are not initialized!")
            // This should ideally not happen after the fix in onViewCreated, but good for defensive programming.
            return
        }

        var totalIncome = 0.0
        var totalExpense = 0.0

        for (transaction in transactions) {
            if (transaction.type == TransactionType.INCOME) {
                totalIncome += transaction.amount
            } else {
                totalExpense += transaction.amount
            }
        }
        val netBalance = totalIncome - totalExpense

        totalIncomeTextView.text = getString(R.string.currency_format, currencyFormatter.format(totalIncome))
        totalExpenseTextView.text = getString(R.string.currency_format, currencyFormatter.format(totalExpense))
        netBalanceTextView.text = getString(R.string.currency_format, currencyFormatter.format(netBalance))

        Log.d("TotalAccountsFragment", "UI updated. Income: $totalIncome, Expense: $totalExpense, Balance: $netBalance")
    }

    override fun onResume() {
        super.onResume()
        // Ensure UI is updated when fragment becomes visible/resumes
        (activity as? MainActivity)?.let { mainActivity ->
            updateFragmentUI(mainActivity.allTransactions)
        }
        Log.d("TotalAccountsFragment", "onResume called, updating UI.")
    }

    // You might want to clear references in onDestroyView to prevent memory leaks,
    // though for simple TextViews with lateinit, it's often handled by system.
    // If you had complex views or listeners that could outlive the view, you'd do:
    // override fun onDestroyView() {
    //     super.onDestroyView()
    //     _binding = null // If using view binding
    //     // totalIncomeTextView = null // If not lateinit, could set to null
    // }
}