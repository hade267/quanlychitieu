package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shipping_orders")
data class ShippingOrder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val address: String,
    val phoneNumber: String,
    val orderAmount: Double, // COD
    val distance: Double,     // km
    val shippingFee: Double,  // calculated or manual
    val status: String,       // "DANG_GIAO" (Đang giao), "DA_GIAO" (Đã giao), "DA_HUY" (Đã huỷ)
    val timestamp: Long,
    val note: String = "",
    val surchargeNightSummer: Boolean = false,
    val surchargeNightWinter: Boolean = false,
    val surchargeHeavyRain: Boolean = false,
    val surchargeCake: Boolean = false,
    val surchargeDoorToDoor: Boolean = false,
    val surchargeBuyOnBehalf: Boolean = false,
    val surchargeBusStation: Boolean = false,
    val weightGroup: Int = 0, // 0: <10kg, 1: 10-25kg, 2: 26-40kg, 3: 41-50kg
    val customerPrepaid: Boolean = false,
    val shopPaysShipping: Boolean = false
)
