package com.example.data.repository

import com.example.data.db.ShippingOrderDao
import com.example.data.model.ShippingOrder
import kotlinx.coroutines.flow.Flow

class ShippingRepository(private val shippingOrderDao: ShippingOrderDao) {
    val allShippingOrders: Flow<List<ShippingOrder>> = shippingOrderDao.getAllShippingOrders()

    suspend fun insert(order: ShippingOrder) {
        shippingOrderDao.insertShippingOrder(order)
    }

    suspend fun update(order: ShippingOrder) {
        shippingOrderDao.updateShippingOrder(order)
    }

    suspend fun delete(order: ShippingOrder) {
        shippingOrderDao.deleteShippingOrder(order)
    }
}
