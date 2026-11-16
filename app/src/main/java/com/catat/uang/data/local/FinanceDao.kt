package com.catat.uang.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.catat.uang.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Query("UPDATE accounts SET name = :newName, balance = :newBalance WHERE id = :accountId")
    suspend fun updateAccountDetails(accountId: Long, newName: String, newBalance: Long)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT id FROM categories ORDER BY id ASC LIMIT 1")
    suspend fun getFirstCategoryId(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Query("""
        SELECT 
            t.id, t.accountId, t.targetAccountId, t.amount, t.type, t.timestamp, t.note, t.receiptPath,
            a.name AS accountName,
            a2.name AS targetAccountName,
            COALESCE(c.name, 'Transfer') AS categoryName,
            COALESCE(c.colorHex, '#1565C0') AS categoryColor
        FROM transactions t
        INNER JOIN accounts a ON t.accountId = a.id
        LEFT JOIN accounts a2 ON t.targetAccountId = a2.id
        LEFT JOIN categories c ON t.categoryId = c.id
        ORDER BY t.timestamp DESC
    """)
    fun getAllTransactions(): Flow<List<TransactionItem>>

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: Long)

    @Query("UPDATE accounts SET balance = balance + :delta WHERE id = :accountId")
    suspend fun updateAccountBalance(accountId: Long, delta: Long)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int

    @Transaction
    suspend fun executeTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        categoryId: Long,
        amount: Long,
        timestamp: Long,
        note: String
    ) {
        updateAccountBalance(fromAccountId, -amount)
        updateAccountBalance(toAccountId, amount)
        insertTransaction(
            TransactionEntity(
                accountId = fromAccountId,
                targetAccountId = toAccountId,
                categoryId = categoryId,
                amount = amount,
                type = "TRANSFER",
                timestamp = timestamp,
                note = note
            )
        )
    }
}
