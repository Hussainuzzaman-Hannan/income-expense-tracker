package com.example.incomeexpensetracker

import java.io.Serializable

/**
 * Data class representing a single income or expense transaction.
 *
 * @param id A unique identifier for the transaction.
 * @param description A brief text description of the transaction.
 * @param amount The monetary amount of the transaction.
 * @param type The type of transaction (Income or Expense).
 * @param category The category of the transaction (e.g., Food, Transport, Salary). Default is "Uncategorized".
 * @param date The date of the transaction in "dd/MM/yyyy" format.
 * @param timestamp The timestamp when the transaction was recorded, in milliseconds.
 */
data class Transaction(
    val id: String = java.util.UUID.randomUUID().toString(), // Unique ID for each transaction
    val description: String,
    val amount: Double,
    val type: TransactionType, // Enum to differentiate income/expense
    val category: String, // Added category field
    val date: String,
    val timestamp: Long = System.currentTimeMillis() // When the transaction was added
) : Serializable

/**
 * Enum class to define the type of transaction.
 */
enum class TransactionType {
    INCOME,
    EXPENSE
}
