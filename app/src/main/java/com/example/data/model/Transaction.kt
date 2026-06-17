package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val dateMillis: Long,
    val category: String,
    val type: String, // "INCOME" or "EXPENSE"
    val note: String = ""
)
