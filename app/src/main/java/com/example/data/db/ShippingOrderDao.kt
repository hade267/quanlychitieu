package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.data.model.ShippingOrder
import kotlinx.coroutines.flow.Flow

@Dao
interface ShippingOrderDao {
    @Query("SELECT * FROM shipping_orders ORDER BY timestamp DESC")
    fun getAllShippingOrders(): Flow<List<ShippingOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShippingOrder(order: ShippingOrder): Long

    @Update
    suspend fun updateShippingOrder(order: ShippingOrder)

    @Delete
    suspend fun deleteShippingOrder(order: ShippingOrder)
}
