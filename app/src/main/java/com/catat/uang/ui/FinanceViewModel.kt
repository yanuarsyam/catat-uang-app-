package com.catat.uang.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.catat.uang.data.local.AppDatabase
import com.catat.uang.data.model.*
import com.catat.uang.util.CsvImporter
import com.catat.uang.util.ReceiptStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val monthTransactions: StateFlow<List<TransactionItem>> = combine(allTransactions, _selectedMonth) { list, ym ->
        if (ym == null) list
        else list.filter { item ->
            val date = Instant.ofEpochMilli(item.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            YearMonth.from(date) == ym
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTransactions: StateFlow<List<TransactionItem>> = combine(monthTransactions, _searchQuery) { list, query ->
        val q = query.trim().lowercase()
        if (q.isEmpty()) list
        else list.filter { item ->
            item.note.lowercase().contains(q) ||
                item.categoryName.lowercase().contains(q) ||
                item.accountName.lowercase().contains(q) ||
                (item.targetAccountName?.lowercase()?.contains(q) == true) ||
                item.amount.toString().contains(q)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyIncome: StateFlow<Long> = monthTransactions.map { list ->
        list.filter { it.type == "INCOME" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val monthlyExpense: StateFlow<Long> = monthTransactions.map { list ->
        list.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val monthlyExpenseSummary: StateFlow<List<CategorySummary>> = monthTransactions.map { list ->
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

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
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
            dao.insertAccount(Account(name = name.trim(), balance = initialBalance, colorHex = colorHex))
        }
    }

    fun updateAccountDetails(accountId: Long, newName: String, newBalance: Long) {
        viewModelScope.launch {
            dao.updateAccountDetails(accountId, newName.trim(), newBalance)
        }
    }

    fun addCategory(name: String, colorHex: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                dao.insertCategory(Category(name = name.trim(), colorHex = colorHex))
            }
        }
    }

    fun updateCategory(id: Long, name: String, colorHex: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                dao.updateCategory(id, name.trim(), colorHex)
            }
        }
    }

    fun deleteCategory(categoryId: Long, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val allCats = dao.getAllCategoriesList()
            if (allCats.size <= 1) {
                onResult(false, "Minimal harus tersisa 1 kategori.")
                return@launch
            }
            val usageCount = dao.countTransactionsByCategory(categoryId)
            if (usageCount > 0) {
                onResult(false, "Kategori ini dipakai oleh $usageCount transaksi.")
                return@launch
            }
            dao.deleteCategoryById(categoryId)
            onResult(true, "Kategori dihapus.")
        }
    }

    fun addTransaction(
        accountId: Long,
        categoryId: Long,
        amount: Long,
        type: String,
        note: String,
        timestamp: Long = System.currentTimeMillis(),
        receiptPath: String? = null
    ) {
        viewModelScope.launch {
            dao.insertTransaction(
                TransactionEntity(
                    accountId = accountId,
                    categoryId = categoryId,
                    amount = amount,
                    type = type,
                    timestamp = timestamp,
                    note = note,
                    receiptPath = receiptPath
                )
            )
            val delta = if (type == "INCOME") amount else -amount
            dao.updateAccountBalance(accountId, delta)
        }
    }

    fun updateTransaction(
        oldItem: TransactionItem,
        newAccountId: Long,
        newCategoryId: Long,
        newAmount: Long,
        newType: String,
        newTimestamp: Long,
        newNote: String,
        newReceiptPath: String?
    ) {
        viewModelScope.launch {
            val revertDelta = if (oldItem.type == "EXPENSE") oldItem.amount else -oldItem.amount
            dao.updateAccountBalance(oldItem.accountId, revertDelta)

            val applyDelta = if (newType == "INCOME") newAmount else -newAmount
            dao.updateAccountBalance(newAccountId, applyDelta)

            dao.updateTransaction(
                id = oldItem.id,
                accountId = newAccountId,
                categoryId = newCategoryId,
                amount = newAmount,
                type = newType,
                timestamp = newTimestamp,
                note = newNote,
                receiptPath = newReceiptPath
            )

            if (oldItem.receiptPath != null && oldItem.receiptPath != newReceiptPath) {
                if (dao.countTransactionsByReceiptPath(oldItem.receiptPath) == 0) {
                    ReceiptStorage.deleteReceiptFile(oldItem.receiptPath)
                }
            }
        }
    }

    fun transferFunds(fromAccountId: Long, toAccountId: Long, amount: Long, note: String, timestamp: Long = System.currentTimeMillis()) {
        if (fromAccountId == toAccountId || amount <= 0L) return
        viewModelScope.launch {
            val defaultCatId = dao.getFirstCategoryId() ?: 1L
            dao.executeTransfer(
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                categoryId = defaultCatId,
                amount = amount,
                timestamp = timestamp,
                note = note.trim()
            )
        }
    }

    fun deleteTransaction(item: TransactionItem) {
        viewModelScope.launch {
            dao.deleteTransactionById(item.id)
            if (item.type == "TRANSFER") {
                dao.updateAccountBalance(item.accountId, item.amount)
                item.targetAccountId?.let { targetId ->
                    dao.updateAccountBalance(targetId, -item.amount)
                }
            } else {
                val refundDelta = if (item.type == "EXPENSE") item.amount else -item.amount
                dao.updateAccountBalance(item.accountId, refundDelta)
            }
            item.receiptPath?.let { path ->
                if (dao.countTransactionsByReceiptPath(path) == 0) {
                    ReceiptStorage.deleteReceiptFile(path)
                }
            }
        }
    }

    fun importFromCsv(uri: Uri, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                CsvImporter.importCsvFromUri(getApplication(), uri, dao)
            }
            onComplete(count)
        }
    }

    fun getCategories(): Flow<List<Category>> = dao.getAllCategories()
}
