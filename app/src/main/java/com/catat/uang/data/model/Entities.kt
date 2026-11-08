package com.catat.uang.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

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
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val categoryId: Long,
    val amount: Long,
    val type: String, // "EXPENSE" atau "INCOME"
    val timestamp: Long,
    val note: String
)

data class TransactionDetail(
    @Embedded val transaction: Transaction,
    @Relation(parentColumn = "accountId", entityColumn = "id")
    val account: Account,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: Category
)

data class CategorySummary(
    val categoryName: String,
    val colorHex: String,
    val totalAmount: Long
)
