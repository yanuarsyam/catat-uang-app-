package com.catat.uang.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.catat.uang.data.local.AppDatabase
import com.catat.uang.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).financeDao()

    val accounts: StateFlow<List<Account>> = dao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allTransactions: StateFlow<List<TransactionItem>> = dao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMonth = MutableStateFlow<YearMonth?>(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth?> = _selectedMonth.asStateFlow()

    val filteredTransactions: StateFlow<List<TransactionItem>> = combine(allTransactions, _selectedMonth) { list, ym ->
        if (ym == null) list
        else list.filter { item ->
            val date = Instant.ofEpochMilli(item.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            YearMonth.from(date) == ym
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyIncome: StateFlow<Long> = filteredTransactions.map { list ->
        list.filter { it.type == "INCOME" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val monthlyExpense: StateFlow<Long> = filteredTransactions.map { list ->
        list.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val monthlyExpenseSummary: StateFlow<List<CategorySummary>> = filteredTransactions.map { list ->
        list.filter { it.type == "EXPENSE" }
            .groupBy { it.categoryName }
            .map { (catName, items) ->
                CategorySummary(
                    categoryName = catName,
                    colorHex = items.first().categoryColor,
                    totalAmount = items.sumOf { it.amount }
                )
            }
            .sortedByDescending { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance: StateFlow<Long> = accounts.map { list ->
        list.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    init {
        seedInitialDataIfEmpty()
    }

    private fun seedInitialDataIfEmpty() {
        viewModelScope.launch {
            if (dao.getAccountCount() == 0) {
                dao.insertAccount(Account(name = "Dompet Tunai", balance = 0L, colorHex = "#2E7D32"))
                dao.insertAccount(Account(name = "Rekening Bank", balance = 0L, colorHex = "#1565C0"))
                dao.insertAccount(Account(name = "E-Wallet", balance = 0L, colorHex = "#6A1B9A"))

                dao.insertCategory(Category(name = "Dapur & Sembako", colorHex = "#D32F2F"))
                dao.insertCategory(Category(name = "Kendaraan & BBM", colorHex = "#F57C00"))
                dao.insertCategory(Category(name = "Listrik & Utilitas", colorHex = "#0288D1"))
                dao.insertCategory(Category(name = "Jajan & Kopi", colorHex = "#7B1FA2"))
                dao.insertCategory(Category(name = "Gaji / Pemasukan", colorHex = "#388E3C"))
            }
        }
    }

    fun previousMonth() {
        val current = _selectedMonth.value ?: YearMonth.now()
        _selectedMonth.value = current.minusMonths(1)
    }

    fun nextMonth() {
        val current = _selectedMonth.value ?: YearMonth.now()
        _selectedMonth.value = current.plusMonths(1)
    }

    fun toggleAllTime() {
        _selectedMonth.value = if (_selectedMonth.value == null) YearMonth.now() else null
    }

    fun addAccount(name: String, initialBalance: Long, colorHex: String = "#1565C0") {
        viewModelScope.launch {
            dao.insertAccount(Account(name = name, balance = initialBalance, colorHex = colorHex))
        }
    }

    fun updateAccountDetails(accountId: Long, newName: String, newBalance: Long) {
        viewModelScope.launch {
            dao.updateAccountDetails(accountId, newName.trim(), newBalance)
        }
    }

    fun addTransaction(accountId: Long, categoryId: Long, amount: Long, type: String, note: String) {
        viewModelScope.launch {
            dao.insertTransaction(
                TransactionEntity(
                    accountId = accountId,
                    categoryId = categoryId,
                    amount = amount,
                    type = type,
                    timestamp = System.currentTimeMillis(),
                    note = note
                )
            )
            val delta = if (type == "INCOME") amount else -amount
            dao.updateAccountBalance(accountId, delta)
        }
    }

    fun deleteTransaction(item: TransactionItem) {
        viewModelScope.launch {
            dao.deleteTransactionById(item.id)
            val refundDelta = if (item.type == "EXPENSE") item.amount else -item.amount
            dao.updateAccountBalance(item.accountId, refundDelta)
        }
    }

    fun getCategories(): Flow<List<Category>> = dao.getAllCategories()
}
