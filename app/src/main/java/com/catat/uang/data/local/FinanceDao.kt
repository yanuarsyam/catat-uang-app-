package com.catat.uang.data.local

import androidx.room.*
import com.catat.uang.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionDetail>>

    @Query("UPDATE accounts SET balance = balance + :delta WHERE id = :accountId")
    suspend fun updateAccountBalance(accountId: Long, delta: Long)

    @Insert
    suspend fun insertTransactionRaw(transaction: Transaction): Long

    @Transaction
    suspend fun insertTransaction(transaction: Transaction) {
        insertTransactionRaw(transaction)
        val delta = if (transaction.type == "INCOME") transaction.amount else -transaction.amount
        updateAccountBalance(transaction.accountId, delta)
    }

    @Query("""
        SELECT c.name AS categoryName, c.colorHex AS colorHex, SUM(t.amount) AS totalAmount
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.type = 'EXPENSE'
        GROUP BY t.categoryId
        ORDER BY totalAmount DESC
    """)
    fun getExpenseSummaryByCategory(): Flow<List<CategorySummary>>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int
}
