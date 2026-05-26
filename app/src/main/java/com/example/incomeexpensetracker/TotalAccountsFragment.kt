// TotalAccountsFragment.kt
package com.example.incomeexpensetracker

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import java.text.NumberFormat
import java.util.*

class TotalAccountsFragment : Fragment() {

    private lateinit var totalIncomeTextView: TextView
    private lateinit var totalExpenseTextView: TextView
    private lateinit var netBalanceTextView: TextView
    private lateinit var pieChart: PieChart
    private lateinit var currencyFormatter: NumberFormat

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_total_accounts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        totalIncomeTextView = view.findViewById(R.id.total_income_text_view)
        totalExpenseTextView = view.findViewById(R.id.total_expenses_text_view)
        netBalanceTextView = view.findViewById(R.id.balance_text_view)
        pieChart = view.findViewById(R.id.total_accounts_pie_chart)

        val currentLocale = Locale(
            requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getString("app_language", "en") ?: "en",
            "BD"
        )
        currencyFormatter = NumberFormat.getCurrencyInstance(currentLocale)

        setupPieChart()

        (activity as? MainActivity)?.let { mainActivity ->
            updateFragmentUI(mainActivity.allTransactions)
        }
        Log.d("TotalAccountsFragment", "onViewCreated: UI elements initialized.")
    }

    private fun chartTextColor(): Int {
        val nightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return if (nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) Color.WHITE else Color.BLACK
    }

    private fun setupPieChart() {
        val textColor = chartTextColor()
        pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleColor(Color.TRANSPARENT)
            setTransparentCircleAlpha(110)
            holeRadius = 45f
            transparentCircleRadius = 50f
            setDrawCenterText(true)
            centerText = "Expense\nBreakdown"
            setCenterTextSize(13f)
            setCenterTextColor(textColor)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            legend.isEnabled = true
            legend.textColor = textColor
            legend.textSize = 11f
            setEntryLabelColor(textColor)
            setEntryLabelTextSize(11f)
            setNoDataText("No expense data yet")
            setNoDataTextColor(textColor)
        }
    }

    fun updateFragmentUI(transactions: List<Transaction>) {
        if (!::totalIncomeTextView.isInitialized || !::totalExpenseTextView.isInitialized
            || !::netBalanceTextView.isInitialized || !::pieChart.isInitialized) {
            Log.e("TotalAccountsFragment", "updateFragmentUI called but views are not initialized!")
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

        updatePieChart(transactions)

        Log.d("TotalAccountsFragment", "UI updated. Income: $totalIncome, Expense: $totalExpense")
    }

    private fun updatePieChart(transactions: List<Transaction>) {
        // শুধু Expense গুলো category অনুযায়ী ভাগ করি
        val categoryMap = mutableMapOf<String, Float>()
        for (t in transactions) {
            if (t.type == TransactionType.EXPENSE) {
                categoryMap[t.category] = (categoryMap[t.category] ?: 0f) + t.amount.toFloat()
            }
        }

        if (categoryMap.isEmpty()) {
            pieChart.clear()
            pieChart.invalidate()
            return
        }

        val entries = categoryMap.map { (category, amount) ->
            PieEntry(amount, category)
        }

        val colors = listOf(
            Color.parseColor("#EF5350"), // red
            Color.parseColor("#42A5F5"), // blue
            Color.parseColor("#66BB6A"), // green
            Color.parseColor("#FFA726"), // orange
            Color.parseColor("#AB47BC"), // purple
            Color.parseColor("#26C6DA"), // cyan
            Color.parseColor("#FF7043"), // deep orange
            Color.parseColor("#D4E157"), // lime
            Color.parseColor("#EC407A"), // pink
            Color.parseColor("#26A69A"), // teal
        )

        val textColor = chartTextColor()
        val dataSet = PieDataSet(entries, "").apply {
            setColors(colors)
            sliceSpace = 3f
            selectionShift = 5f
            valueLinePart1OffsetPercentage = 80f
            valueLinePart1Length = 0.4f
            valueLinePart2Length = 0.4f
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLineColor = textColor
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(pieChart))
            setValueTextSize(11f)
            setValueTextColor(textColor)
        }

        pieChart.data = data
        pieChart.highlightValues(null)
        pieChart.invalidate()
        pieChart.animateY(800)
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.let { mainActivity ->
            updateFragmentUI(mainActivity.allTransactions)
        }
        Log.d("TotalAccountsFragment", "onResume called, updating UI.")
    }
}