package com.example.incomeexpensetracker

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log // Keep this import if you use Log for debugging
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import java.text.NumberFormat
import android.content.res.Resources

/**
 * ReportsActivity displays financial reports using PieChart and BarChart from MPAndroidChart.
 * It receives a list of transactions from MainActivity and filters them to generate monthly or weekly reports.
 */
class ReportsActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private var allTransactions: List<Transaction> = listOf()
    private lateinit var reportType: String // "Monthly" or "Weekly"

    private val PREFS_FILE_NAME = "app_prefs"
    private val LANGUAGE_KEY = "app_language"

    companion object {
        const val EXTRA_TRANSACTIONS = "extra_transactions"
        const val EXTRA_REPORT_TYPE = "extra_report_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved language before super.onCreate()
        val languageCode = getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).getString(LANGUAGE_KEY, "en")
        languageCode?.let { setLocale(this, it) }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports) // Set the layout for this activity

        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)

        // Retrieve transactions and report type from the Intent
        allTransactions = intent.getSerializableExtra(EXTRA_TRANSACTIONS) as? List<Transaction> ?: listOf()
        reportType = intent.getStringExtra(EXTRA_REPORT_TYPE) ?: "Monthly"

        if (allTransactions.isEmpty()) {
            showSnackbar(getString(R.string.report_no_transactions))
            pieChart.visibility = View.GONE
            barChart.visibility = View.GONE
            return
        }

        // Setup charts based on the report type
        if (reportType == "Monthly") {
            setupMonthlyReportCharts()
        } else { // Weekly Report
            setupWeeklyReportCharts()
        }
    }

    /**
     * Filters transactions for the current month and sets up pie and bar charts.
     */
    private fun setupMonthlyReportCharts() {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        val monthlyTransactions = allTransactions.filter {
            val transactionCalendar = Calendar.getInstance().apply {
                time = SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(it.date) ?: Date()
            }
            transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                    transactionCalendar.get(Calendar.YEAR) == currentYear
        }

        if (monthlyTransactions.isEmpty()) {
            showSnackbar(getString(R.string.report_no_monthly_transactions))
            pieChart.visibility = View.GONE
            barChart.visibility = View.GONE
            return
        }

        populatePieChart(monthlyTransactions, getString(R.string.report_monthly_income_expense_breakdown))
        populateBarChart(monthlyTransactions, getString(R.string.report_monthly_income_expense))
    }

    /**
     * Filters transactions for the last 7 days and sets up pie and bar charts.
     */
    private fun setupWeeklyReportCharts() {
        val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }

        val weeklyTransactions = allTransactions.filter {
            val transactionDate = SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(it.date) ?: Date()
            // Include transactions from 7 days ago up to and including today
            !transactionDate.before(sevenDaysAgo.time)
        }

        if (weeklyTransactions.isEmpty()) {
            showSnackbar(getString(R.string.report_no_weekly_transactions))
            pieChart.visibility = View.GONE
            barChart.visibility = View.GONE
            return
        }

        populatePieChart(weeklyTransactions, getString(R.string.report_weekly_income_expense_breakdown))
        populateBarChart(weeklyTransactions, getString(R.string.report_weekly_income_expense))
    }

    /**
     * Populates the Pie Chart with income vs expense breakdown.
     * @param transactions List of transactions to analyze.
     * @param chartTitle Title for the pie chart.
     */
    private fun populatePieChart(transactions: List<Transaction>, chartTitle: String) {
        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        val pieEntries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        if (totalIncome > 0) {
            pieEntries.add(PieEntry(totalIncome.toFloat(), getString(R.string.report_income_label)))
            colors.add(resources.getColor(R.color.green_income, theme))
        }
        if (totalExpense > 0) {
            pieEntries.add(PieEntry(totalExpense.toFloat(), getString(R.string.report_expense_label)))
            colors.add(resources.getColor(R.color.red_expense, theme))
        }

        if (pieEntries.isEmpty()) {
            pieChart.visibility = View.GONE
            return
        }

        val pieDataSet = PieDataSet(pieEntries, "")
        pieDataSet.colors = colors
        pieDataSet.valueTextSize = 12f
        pieDataSet.valueTextColor = Color.BLACK
        // Formatter for BDT
        pieDataSet.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return NumberFormat.getCurrencyInstance(Locale("bn", "BD")).format(value)
            }
        }

        val pieData = PieData(pieDataSet)
        pieChart.data = pieData
        pieChart.description.text = chartTitle
        pieChart.description.textSize = 14f
        pieChart.description.textColor = Color.BLACK
        pieChart.centerText = getString(R.string.report_income_vs_expense)
        pieChart.setCenterTextSize(16f)
        pieChart.setCenterTextColor(Color.BLACK)
        // If you want percentages along with values on slices, enable this line and add PercentFormatter
        // pieChart.setUsePercentValues(true)
        // pieDataSet.valueFormatter = PercentFormatter(pieChart)
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setEntryLabelTextSize(10f)


        val legend = pieChart.legend
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = Legend.LegendOrientation.VERTICAL
        legend.setDrawInside(false)
        legend.xEntrySpace = 7f
        legend.yEntrySpace = 0f
        legend.yOffset = 0f
        legend.textColor = Color.BLACK
        legend.textSize = 10f

        pieChart.animateY(1000)
        pieChart.invalidate()
        pieChart.visibility = View.VISIBLE
    }

    /**
     * Populates the Bar Chart with daily/weekly income and expense totals.
     * @param transactions List of transactions to analyze.
     * @param chartTitle Title for the bar chart.
     */
    private fun populateBarChart(transactions: List<Transaction>, chartTitle: String) {
        val incomeEntries = ArrayList<BarEntry>()
        val expenseEntries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        // Group transactions by date for daily totals
        val incomeByDate = transactions
            .filter { it.type == TransactionType.INCOME }
            .groupBy { it.date }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val expenseByDate = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.date }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        // Get unique sorted dates
        val allDates = (incomeByDate.keys + expenseByDate.keys).distinct().sortedBy {
            SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(it)
        }

        if (allDates.isEmpty()) {
            barChart.visibility = View.GONE
            return
        }

        // Prepare data for bar chart
        for ((index, date) in allDates.withIndex()) {
            incomeEntries.add(BarEntry(index.toFloat(), incomeByDate[date]?.toFloat() ?: 0f))
            expenseEntries.add(BarEntry(index.toFloat(), expenseByDate[date]?.toFloat() ?: 0f))
            labels.add(date) // Add date as label
        }

        val incomeDataSet = BarDataSet(incomeEntries, getString(R.string.report_income_label))
        incomeDataSet.color = resources.getColor(R.color.green_income, theme)
        incomeDataSet.valueTextColor = Color.BLACK
        incomeDataSet.valueTextSize = 10f
        incomeDataSet.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return NumberFormat.getCurrencyInstance(Locale("bn", "BD")).format(value)
            }
        }


        val expenseDataSet = BarDataSet(expenseEntries, getString(R.string.report_expense_label))
        expenseDataSet.color = resources.getColor(R.color.red_expense, theme)
        expenseDataSet.valueTextColor = Color.BLACK
        expenseDataSet.valueTextSize = 10f
        expenseDataSet.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return NumberFormat.getCurrencyInstance(Locale("bn", "BD")).format(value)
            }
        }

        val barData = BarData(incomeDataSet, expenseDataSet)
        barChart.data = barData
        barChart.description.text = chartTitle
        barChart.description.textSize = 14f
        barChart.description.textColor = Color.BLACK
        barChart.setDrawValueAboveBar(true)
        barChart.setFitBars(true)

        // X-axis styling
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f
        xAxis.textColor = Color.BLACK
        xAxis.textSize = 10f

        // Y-axis styling
        val leftAxis = barChart.axisLeft
        leftAxis.textColor = Color.BLACK
        leftAxis.textSize = 10f
        val rightAxis = barChart.axisRight
        rightAxis.isEnabled = false

        // Legend styling
        val legend = barChart.legend
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = Legend.LegendOrientation.VERTICAL
        legend.setDrawInside(false)
        legend.xEntrySpace = 7f
        legend.yEntrySpace = 0f
        legend.yOffset = 0f
        legend.textColor = Color.BLACK
        legend.textSize = 10f

        // Grouped Bar Chart settings
        val barSpace = 0.02f
        val groupSpace = 0.3f
        val groupCount = labels.size.toFloat()
        barData.barWidth = 0.33f

        barData.groupBars(0f, groupSpace, barSpace)
        barChart.xAxis.axisMinimum = 0f
        barChart.xAxis.axisMaximum = 0f + barData.getGroupWidth(groupSpace, barSpace) * groupCount

        barChart.animateY(1000)
        barChart.invalidate()
        barChart.visibility = View.VISIBLE
    }

    /**
     * Helper function to display Snackbar messages.
     */
    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }

    /**
     * Sets the application's locale.
     * This method is duplicated here for consistency, but consider making it a utility function
     * if used in many activities.
     */
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
    }
}
