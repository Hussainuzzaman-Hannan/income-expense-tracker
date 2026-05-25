package com.example.incomeexpensetracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import android.util.TypedValue
import android.util.Log // Import Log for debugging

/**
 * RecyclerView Adapter for displaying a list of Transaction objects.
 * This adapter is responsible for creating and binding views for each transaction item.
 *
 * @param transactions The mutable list of Transaction objects to display.
 * @param onDeleteClick A lambda function to be called when the delete button of an item is clicked,
 * passing the ID of the transaction to be deleted.
 * @param onEditClick A lambda function to be called when a transaction item is clicked for editing.
 * It passes the Transaction object to be edited.
 */
class TransactionAdapter(
    private val transactions: MutableList<Transaction>,
    private val onDeleteClick: (String) -> Unit,
    private val onEditClick: (Transaction) -> Unit // Added onEditClick callback
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    /**
     * Called when RecyclerView needs a new [ViewHolder] of the given type to represent
     * an item.
     *
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        // Inflate the layout for a single transaction item.
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false) // Link to item XML layout
        return TransactionViewHolder(view)
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method updates the contents of the [ViewHolder]'s itemView to reflect the item at the given position.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the item at the given position.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.bind(transaction)
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int = transactions.size

    /**
     * Updates the list of transactions using DiffUtil for efficient updates.
     *
     * @param newList The new list of Transaction objects.
     */
    fun updateList(newList: List<Transaction>) {
        val diffResult = DiffUtil.calculateDiff(TransactionDiffCallback(transactions, newList))
        transactions.clear()
        transactions.addAll(newList)
        diffResult.dispatchUpdatesTo(this) // Dispatch updates to the RecyclerView
        Log.d("TransactionAdapter", "List updated. New size: ${transactions.size}")
    }

    /**
     * A ViewHolder describes an item view and metadata about its place within the RecyclerView.
     * It's used to cache the views for faster access.
     *
     * @param itemView The View for a single transaction item.
     */
    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // UI elements within a single transaction item view.
        private val descriptionTextView: TextView = itemView.findViewById(R.id.transaction_description_text_view)
        private val amountTextView: TextView = itemView.findViewById(R.id.transaction_amount_text_view)
        private val dateTextView: TextView = itemView.findViewById(R.id.transaction_date_text_view)
        private val categoryTextView: TextView = itemView.findViewById(R.id.transaction_category_text_view)
        private val categoryIcon: ImageView = itemView.findViewById(R.id.category_icon)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.delete_transaction_button)

        /**
         * Binds the data of a [Transaction] object to the UI elements of the ViewHolder.
         *
         * @param transaction The Transaction object to bind.
         */
        fun bind(transaction: Transaction) {
            descriptionTextView.text = transaction.description
            dateTextView.text = transaction.date
            categoryTextView.text = transaction.category

            // Get colorOnPrimary from the theme dynamically for text that should adapt
            val typedValue = TypedValue()
            itemView.context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
            // Correctly get the color value from the resolved resource ID
            val colorOnPrimary = ContextCompat.getColor(itemView.context, typedValue.resourceId)

            descriptionTextView.setTextColor(colorOnPrimary)
            categoryTextView.setTextColor(colorOnPrimary)
            dateTextView.setTextColor(colorOnPrimary) // Also apply to date text for consistency
            categoryIcon.setColorFilter(colorOnPrimary) // Tint category icon

            // Format the amount to currency, e.g., "৳100.00" or "-৳50.00".
            val formattedAmount = NumberFormat.getCurrencyInstance(Locale("bn", "BD")).format(transaction.amount)
            amountTextView.text = if (transaction.type == TransactionType.EXPENSE) "-$formattedAmount" else formattedAmount // Removed "+" for income

            // Set amount text color based on transaction type (specific colors)
            val amountColorResId = if (transaction.type == TransactionType.INCOME) R.color.green_income else R.color.red_expense
            amountTextView.setTextColor(ContextCompat.getColor(itemView.context, amountColorResId))

            // Set category icon based on category type
            categoryIcon.setImageResource(getCategoryIcon(transaction.category))

            // Delete button setup
            deleteButton.setColorFilter(ContextCompat.getColor(itemView.context, R.color.red_expense)) // Explicitly red for delete
            deleteButton.setOnClickListener {
                onDeleteClick(transaction.id) // Invoke the callback with the transaction's ID.
            }

            // Edit functionality (click on the whole item)
            itemView.setOnClickListener {
                onEditClick(transaction)
            }
        }

        /**
         * Returns the drawable resource ID for a given category.
         * You'll need to create vector drawable assets (e.g., ic_food, ic_transport)
         * in your res/drawable folder.
         */
        private fun getCategoryIcon(category: String): Int {
            return when (category) {
                itemView.context.getString(R.string.uncategorized) -> R.drawable.ic_category_placeholder
                itemView.context.getString(R.string.category_food) -> R.drawable.ic_food
                itemView.context.getString(R.string.category_transport) -> R.drawable.ic_transport
                itemView.context.getString(R.string.category_utilities) -> R.drawable.ic_utilities
                itemView.context.getString(R.string.category_salary) -> R.drawable.ic_salary
                itemView.context.getString(R.string.category_shopping) -> R.drawable.ic_shopping
                itemView.context.getString(R.string.category_entertainment) -> R.drawable.ic_entertainment
                itemView.context.getString(R.string.category_health) -> R.drawable.ic_health
                itemView.context.getString(R.string.category_education) -> R.drawable.ic_education
                itemView.context.getString(R.string.category_housing) -> R.drawable.ic_housing
                itemView.context.getString(R.string.category_investment) -> R.drawable.ic_investment
                itemView.context.getString(R.string.category_gift) -> R.drawable.ic_gift
                itemView.context.getString(R.string.category_freelance) -> R.drawable.ic_freelance
                itemView.context.getString(R.string.category_misc_expense) -> R.drawable.ic_misc_expense
                itemView.context.getString(R.string.category_misc_income) -> R.drawable.ic_misc_income
                else -> R.drawable.ic_category_placeholder // Fallback for any unhandled category
            }
        }
    }

    /**
     * DiffUtil.Callback implementation for efficient RecyclerView updates.
     * Compares two lists of Transaction objects.
     */
    class TransactionDiffCallback(
        private val oldList: List<Transaction>,
        private val newList: List<Transaction>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        // Check if items represent the same object (using a unique ID)
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        // Check if the contents of the items are the same
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
