package com.example.incomeexpensetracker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.fragment.app.Fragment // Explicitly import androidx.fragment.app.Fragment

/**
 * A [DialogFragment] that serves as the container for adding new transactions or editing existing ones.
 * It uses a [TabLayout] and [ViewPager2] to switch between Income (Deposit) and Expense input forms.
 */
class AddTransactionDialogFragment : DialogFragment() {

    // Listener to communicate back to MainActivity when a transaction is added or updated
    private var transactionAddedListener: (() -> Unit)? = null
    // Holds the transaction object if the dialog is opened in edit mode
    private var transactionToEdit: Transaction? = null

    /**
     * Sets the listener for when a transaction is added or updated.
     */
    fun setOnTransactionAddedListener(listener: () -> Unit) {
        transactionAddedListener = listener
    }

    companion object {
        private const val ARG_TRANSACTION_TO_EDIT = "transaction_to_edit"

        /**
         * Factory method to create a new instance of AddTransactionDialogFragment.
         *
         * @param transactionToEdit An optional Transaction object if the dialog is for editing, null otherwise.
         * @return A new instance of AddTransactionDialogFragment.
         */
        @JvmStatic
        fun newInstance(transactionToEdit: Transaction?) =
            AddTransactionDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_TRANSACTION_TO_EDIT, transactionToEdit)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set dialog style to ensure it's a full-width dialog (optional, customize as needed)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogTheme) // Referencing a style from themes.xml

        // Retrieve the transaction object if available (for edit mode)
        arguments?.let {
            transactionToEdit = it.getSerializable(ARG_TRANSACTION_TO_EDIT) as? Transaction
            Log.d("AddTransactionDialog", "Transaction to edit: ${transactionToEdit?.id}")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout that contains the TabLayout and ViewPager2
        return inflater.inflate(R.layout.dialog_add_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("AddTransactionDialog", "onViewCreated called. Transaction to edit is: ${transactionToEdit?.id}")


        val tabLayout: TabLayout = view.findViewById(R.id.dialog_tab_layout)
        val viewPager: ViewPager2 = view.findViewById(R.id.dialog_view_pager)

        // Create instances of the transaction input fragments
        val fragmentList = arrayListOf<Fragment>( // Explicitly use androidx.fragment.app.Fragment
            AddTransactionTabFragment.newInstance(TransactionType.INCOME, transactionToEdit),
            AddTransactionTabFragment.newInstance(TransactionType.EXPENSE, transactionToEdit)
        )

        // Set up the ViewPager2 adapter
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = fragmentList.size
            override fun createFragment(position: Int): Fragment { // Explicitly use androidx.fragment.app.Fragment
                val fragment = fragmentList[position]
                // Pass the dismiss action and transaction added listener to the child fragments
                if (fragment is AddTransactionTabFragment) {
                    fragment.onDismissListener = {
                        dismiss() // Dismiss this DialogFragment
                    }
                    fragment.onTransactionAddSuccessListener = {
                        // Notify the AddTransactionDialogFragment's listener (which is in MainActivity)
                        transactionAddedListener?.invoke()
                    }
                }
                return fragment
            }
        }

        // Link the TabLayout with the ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.add_income_tab)
                1 -> getString(R.string.add_expense_tab)
                else -> ""
            }
        }.attach()

        // If in edit mode, select the correct tab based on transaction type
        transactionToEdit?.let {
            if (it.type == TransactionType.EXPENSE) {
                viewPager.setCurrentItem(1, false) // Select Expense tab (index 1)
            } else {
                viewPager.setCurrentItem(0, false) // Select Income tab (index 0)
            }
        }
    }
}
