package com.example.incomeexpensetracker

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.NumberFormat
import java.util.*

// ডেটা মডেল
data class DebtEntry(
    val id: String = UUID.randomUUID().toString(),
    val personName: String,
    val amount: Double,
    val note: String,
    val type: DebtType  // IOwe = আমি দিতে হবে, TheyOwe = তারা দেবে
)

enum class DebtType { I_OWE, THEY_OWE }

class DebtReceivablesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var totalOweText: TextView
    private lateinit var totalOwedText: TextView
    private lateinit var backButton: ImageButton
    private lateinit var fabAdd: FloatingActionButton

    private val entries = mutableListOf<DebtEntry>()
    private lateinit var adapter: DebtAdapter
    private val gson = Gson()
    private val FILE_NAME = "debt_entries.json"
    private lateinit var currencyFormatter: NumberFormat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debt_receivables)

        recyclerView = findViewById(R.id.debt_recycler_view)
        emptyText = findViewById(R.id.debt_empty_text)
        totalOweText = findViewById(R.id.total_owe_text)
        totalOwedText = findViewById(R.id.total_owed_text)
        backButton = findViewById(R.id.debt_back_button)
        fabAdd = findViewById(R.id.fab_add_debt)

        adapter = DebtAdapter(entries,
            onDelete = { entry ->
                entries.remove(entry)
                saveEntries()
                updateUI()
                Toast.makeText(this, getString(R.string.debt_deleted), Toast.LENGTH_SHORT).show()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        backButton.setOnClickListener { finish() }
        fabAdd.setOnClickListener { showAddDialog() }

        val langCode = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getString("app_language", "en") ?: "en"
        currencyFormatter = NumberFormat.getCurrencyInstance(Locale(langCode, "BD"))

        loadEntries()
        updateUI()
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_debt, null)
        val nameEdit = dialogView.findViewById<EditText>(R.id.debt_name_edit)
        val amountEdit = dialogView.findViewById<EditText>(R.id.debt_amount_edit)
        val noteEdit = dialogView.findViewById<EditText>(R.id.debt_note_edit)
        val typeSpinner = dialogView.findViewById<Spinner>(R.id.debt_type_spinner)

        val typeOptions = arrayOf(getString(R.string.debt_type_owe), getString(R.string.debt_type_owed))
        typeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeOptions).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.debt_add_dialog_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.debt_save_button)) { _, _ ->
                val name = nameEdit.text.toString().trim()
                val amount = amountEdit.text.toString().toDoubleOrNull()
                val note = noteEdit.text.toString().trim()
                val type = if (typeSpinner.selectedItemPosition == 0) DebtType.I_OWE else DebtType.THEY_OWE

                if (name.isEmpty()) {
                    Toast.makeText(this, getString(R.string.debt_error_name), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (amount == null || amount <= 0) {
                    Toast.makeText(this, getString(R.string.debt_error_amount), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                entries.add(0, DebtEntry(personName = name, amount = amount, note = note, type = type))
                saveEntries()
                updateUI()
                Toast.makeText(this, getString(R.string.debt_added), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.debt_cancel_button), null)
            .show()
    }

    private fun updateUI() {
        val totalOwe = entries.filter { it.type == DebtType.I_OWE }.sumOf { it.amount }
        val totalOwed = entries.filter { it.type == DebtType.THEY_OWE }.sumOf { it.amount }

        totalOweText.text = currencyFormatter.format(totalOwe)
        totalOwedText.text = currencyFormatter.format(totalOwed)

        if (entries.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
        adapter.notifyDataSetChanged()
    }

    private fun saveEntries() {
        try {
            File(filesDir, FILE_NAME).writeText(gson.toJson(entries))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadEntries() {
        try {
            val file = File(filesDir, FILE_NAME)
            if (file.exists()) {
                val type = object : TypeToken<MutableList<DebtEntry>>() {}.type
                val loaded: MutableList<DebtEntry> = gson.fromJson(file.readText(), type)
                entries.clear()
                entries.addAll(loaded)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// RecyclerView Adapter
class DebtAdapter(
    private val entries: List<DebtEntry>,
    private val onDelete: (DebtEntry) -> Unit
) : RecyclerView.Adapter<DebtAdapter.ViewHolder>() {

    private fun getCurrencyFormatter(context: android.content.Context): NumberFormat {
        val langCode = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .getString("app_language", "en") ?: "en"
        return NumberFormat.getCurrencyInstance(Locale(langCode, "BD"))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.debt_item_name)
        val amountText: TextView = view.findViewById(R.id.debt_item_amount)
        val noteText: TextView = view.findViewById(R.id.debt_item_note)
        val typeTag: TextView = view.findViewById(R.id.debt_item_type)
        val deleteBtn: ImageButton = view.findViewById(R.id.debt_item_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_debt, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.nameText.text = entry.personName
        holder.amountText.text = getCurrencyFormatter(holder.itemView.context).format(entry.amount)
        holder.noteText.text = entry.note.ifEmpty { "-" }

        if (entry.type == DebtType.I_OWE) {
            holder.typeTag.text = holder.itemView.context.getString(R.string.debt_type_owe)
            holder.typeTag.setBackgroundColor(0xFFE53935.toInt())
            holder.amountText.setTextColor(0xFFE53935.toInt())
        } else {
            holder.typeTag.text = holder.itemView.context.getString(R.string.debt_type_owed)
            holder.typeTag.setBackgroundColor(0xFF43A047.toInt())
            holder.amountText.setTextColor(0xFF43A047.toInt())
        }

        holder.deleteBtn.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setMessage("Delete this entry?")
                .setPositiveButton("Delete") { _, _ -> onDelete(entry) }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun getItemCount() = entries.size
}
