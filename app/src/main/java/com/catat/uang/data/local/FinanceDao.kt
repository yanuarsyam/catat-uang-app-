package com.catat.uang.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.catat.uang.data.model.Account
import com.catat.uang.data.model.Category
import com.catat.uang.data.model.CategorySummary
import com.catat.uang.data.model.TransactionEntity
import com.catat.uang.data.model.TransactionItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Query("UPDATE accounts SET balance = :newBalance WHERE id = :accountId")
    suspend fun setAccountBalanceDirectly(accountId: Long, newBalance: Long)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Query("""
        SELECT 
            t.id, t.amount, t.type, t.timestamp, t.note,
            a.name AS accountName,
            c.name AS categoryName,
            c.colorHex AS categoryColor
        FROM transactions t
        INNER JOIN accounts a ON t.accountId = a.id
        INNER JOIN categories c ON t.categoryId = c.id
        ORDER BY t.timestamp DESC
    """)
    fun getAllTransactions(): Flow<List<TransactionItem>>

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("UPDATE accounts SET balance = balance + :delta WHERE id = :accountId")
    suspend fun updateAccountBalance(accountId: Long, delta: Long)

    @Query("""
        SELECT c.name AS categoryName, c.colorHex AS colorHex, COALESCE(SUM(t.amount), 0) AS totalAmount
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
