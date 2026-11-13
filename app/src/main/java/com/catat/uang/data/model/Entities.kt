package com.catat.uang.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val balance: Long,
    val colorHex: String
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val categoryId: Long,
    val amount: Long,
    val type: String,
    val timestamp: Long,
    val note: String
)

data class TransactionItem(
    val id: Long,
    val accountId: Long,
    val amount: Long,
    val type: String,
    val timestamp: Long,
    val note: String,
    val accountName: String,
    val categoryName: String,
    val categoryColor: String
)

data class CategorySummary(
    val categoryName: String,
    val colorHex: String,
    val totalAmount: Long
)
