// TotalAccountsFragment.kt
package com.example.incomeexpensetracker

import android.content.Context
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
        return inflater.inflate(R.layout.fragment_total_accounts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // সঠিক XML ID দিয়ে views initialize করা হচ্ছে
        totalIncomeTextView = view.findViewById(R.id.total_income_text_view)
        totalExpenseTextView = view.findViewById(R.id.total_expenses_text_view)
        netBalanceTextView = view.findViewById(R.id.balance_text_view)

        val currentLocale = Locale(
            requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getString("app_language", "en") ?: "en",
            "BD"
        )
        currencyFormatter = NumberFormat.getCurrencyInstance(currentLocale)

        (activity as? MainActivity)?.let { mainActivity ->
            updateFragmentUI(mainActivity.allTransactions)
        }
        Log.d("TotalAccountsFragment", "onViewCreated: UI elements initialized and initial update attempted.")
    }

    fun updateFragmentUI(transactions: List<Transaction>) {
        if (!::totalIncomeTextView.isInitialized || !::totalExpenseTextView.isInitialized || !::netBalanceTextView.isInitialized) {
            Log.e("TotalAccountsFragment", "updateFragmentUI called but TextViews are not initialized!")
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

        totalIncomeTextView.text = currencyFormatter.format(totalIncome)
        totalExpenseTextView.text = currencyFormatter.format(totalExpense)
        netBalanceTextView.text = currencyFormatter.format(netBalance)

        Log.d("TotalAccountsFragment", "UI updated. Income: $totalIncome, Expense: $totalExpense, Balance: $netBalance")
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.let { mainActivity ->
            updateFragmentUI(mainActivity.allTransactions)
        }
        Log.d("TotalAccountsFragment", "onResume called, updating UI.")
    }
}