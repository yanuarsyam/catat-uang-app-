package com.catat.uang.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.catat.uang.data.local.AppDatabase
import com.catat.uang.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).financeDao()

    val accounts: StateFlow<List<Account>> = dao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionItem>> = dao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenseSummary: StateFlow<List<CategorySummary>> = dao.getExpenseSummaryByCategory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance: StateFlow<Long> = accounts.map { list ->
        list.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalExpense: StateFlow<Long> = expenseSummary.map { list ->
        list.sumOf { it.totalAmount }
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

    fun updateAccountBalance(accountId: Long, newBalance: Long) {
        viewModelScope.launch {
            dao.setAccountBalanceDirectly(accountId, newBalance)
        }
    }

    fun addNewAccount(name: String, initialBalance: Long, type: String) {
        viewModelScope.launch {
            val color = when (type) {
                "Bank" -> "#1565C0"
                "E-Wallet" -> "#6A1B9A"
                else -> "#2E7D32"
            }
            dao.insertAccount(Account(name = name, balance = initialBalance, colorHex = color))
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

    fun getCategories(): Flow<List<Category>> = dao.getAllCategories()
}
