// AllAccountsFragment.kt
package com.example.incomeexpensetracker

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AllAccountsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var noTransactionsTextView: TextView // New: For the "No transactions yet" message

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_all_accounts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // CORRECTED ID HERE:
        recyclerView = view.findViewById(R.id.transactions_recycler_view)
        noTransactionsTextView = view.findViewById(R.id.no_transactions_text_view) // Initialize the new TextView

        transactionAdapter = TransactionAdapter(
            mutableListOf(), // Start with an empty list, it will be updated
            onDeleteClick = { transactionId ->
                // Handle delete click
                (activity as? MainActivity)?.deleteTransaction(transactionId)
                Log.d("AllAccountsFragment", "Transaction delete requested: $transactionId")
            },
            onEditClick = { transaction ->
                // Handle item click, e.g., show edit dialog
                (activity as? MainActivity)?.showEditTransactionDialog(transaction)
                Log.d("AllAccountsFragment", "Transaction clicked: ${transaction.id}")
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionAdapter
        }

        // Initial update when the fragment's view is created.
        (activity as? MainActivity)?.let { mainActivity ->
            updateFragmentUI(mainActivity.allTransactions)
        }
        Log.d("AllAccountsFragment", "onViewCreated: RecyclerView and adapter set up.")
    }

    /**
     * Updates the UI of this fragment with the latest transaction data.
     * This method is called by MainActivity to refresh the view.
     */
    fun updateFragmentUI(transactions: List<Transaction>) {
        if (!::transactionAdapter.isInitialized || !::noTransactionsTextView.isInitialized) {
            Log.e("AllAccountsFragment", "updateFragmentUI called but transactionAdapter or noTransactionsTextView is not initialized!")
            return
        }

        if (transactions.isEmpty()) {
            recyclerView.visibility = View.GONE
            noTransactionsTextView.visibility = View.VISIBLE
            Log.d("AllAccountsFragment", "No transactions to display, showing empty message.")
        } else {
            recyclerView.visibility = View.VISIBLE
            noTransactionsTextView.visibility = View.GONE
            // Update the adapter with new data
            transactionAdapter.updateList(transactions.toMutableList())
            Log.d("AllAccountsFragment", "RecyclerView UI updated with ${transactions.size} transactions.")
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure UI is updated when fragment becomes visible/resumes
        (activity as? MainActivity)?.let { mainActivity ->
            updateFragmentUI(mainActivity.allTransactions)
        }
        Log.d("AllAccountsFragment", "onResume called, updating UI.")
    }
}