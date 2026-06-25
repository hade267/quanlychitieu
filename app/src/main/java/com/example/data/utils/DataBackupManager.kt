package com.example.data.utils

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.ShippingOrder
import com.example.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object DataBackupManager {

    /**
     * Serializes Room database collections into a formatted JSON string.
     */
    fun exportToJson(
        transactions: List<Transaction>,
        categories: List<CategoryEntity>,
        shippingOrders: List<ShippingOrder>
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("backup_time", System.currentTimeMillis())

        val txArray = JSONArray()
        for (tx in transactions) {
            val txObj = JSONObject().apply {
                put("id", tx.id)
                put("title", tx.title)
                put("amount", tx.amount)
                put("dateMillis", tx.dateMillis)
                put("category", tx.category)
                put("type", tx.type)
                put("note", tx.note)
            }
            txArray.put(txObj)
        }
        root.put("transactions", txArray)

        val catArray = JSONArray()
        for (cat in categories) {
            val catObj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("emoji", cat.emoji)
                put("colorHex", cat.colorHex)
                put("type", cat.type)
            }
            catArray.put(catObj)
        }
        root.put("categories", catArray)

        val shipArray = JSONArray()
        for (order in shippingOrders) {
            val orderObj = JSONObject().apply {
                put("id", order.id)
                put("address", order.address)
                put("phoneNumber", order.phoneNumber)
                put("orderAmount", order.orderAmount)
                put("distance", order.distance)
                put("shippingFee", order.shippingFee)
                put("status", order.status)
                put("timestamp", order.timestamp)
                put("note", order.note)
                put("surchargeNightSummer", order.surchargeNightSummer)
                put("surchargeNightWinter", order.surchargeNightWinter)
                put("surchargeHeavyRain", order.surchargeHeavyRain)
                put("surchargeCake", order.surchargeCake)
                put("surchargeDoorToDoor", order.surchargeDoorToDoor)
                put("surchargeBuyOnBehalf", order.surchargeBuyOnBehalf)
                put("surchargeBusStation", order.surchargeBusStation)
                put("weightGroup", order.weightGroup)
                put("customerPrepaid", order.customerPrepaid)
                put("shopPaysShipping", order.shopPaysShipping)
            }
            shipArray.put(orderObj)
        }
        root.put("shipping_orders", shipArray)

        return root.toString(4)
    }

    /**
     * Reads a backup JSON string from a Document Uri and applies it to the AppDatabase using a safe Room transaction.
     */
    suspend fun importFromJson(
        context: Context,
        uri: Uri,
        db: AppDatabase
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) 
                ?: return@withContext Result.failure(Exception("Không thể mở tệp lưu trữ."))
            
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
            inputStream.close()

            val jsonString = stringBuilder.toString()
            val root = JSONObject(jsonString)

            val txList = mutableListOf<Transaction>()
            val catList = mutableListOf<CategoryEntity>()
            val shipList = mutableListOf<ShippingOrder>()

            // 1. Parse Transactions
            if (root.has("transactions")) {
                val txArray = root.getJSONArray("transactions")
                for (i in 0 until txArray.length()) {
                    val txObj = txArray.getJSONObject(i)
                    txList.add(
                        Transaction(
                            id = 0, // Generate new IDs to prevent room constraints if clean fails
                            title = txObj.optString("title", ""),
                            amount = txObj.optDouble("amount", 0.0),
                            dateMillis = txObj.optLong("dateMillis", System.currentTimeMillis()),
                            category = txObj.optString("category", ""),
                            type = txObj.optString("type", "EXPENSE"),
                            note = txObj.optString("note", "")
                        )
                    )
                }
            }

            // 2. Parse Categories
            if (root.has("categories")) {
                val catArray = root.getJSONArray("categories")
                for (i in 0 until catArray.length()) {
                    val catObj = catArray.getJSONObject(i)
                    catList.add(
                        CategoryEntity(
                            id = 0, // Auto-generate database keys
                            name = catObj.optString("name", ""),
                            emoji = catObj.optString("emoji", "📝"),
                            colorHex = catObj.optString("colorHex", "#FFFFFF"),
                            type = catObj.optString("type", "EXPENSE")
                        )
                    )
                }
            }

            // 3. Parse Shipping Orders
            if (root.has("shipping_orders")) {
                val shipArray = root.getJSONArray("shipping_orders")
                for (i in 0 until shipArray.length()) {
                    val orderObj = shipArray.getJSONObject(i)
                    shipList.add(
                        ShippingOrder(
                            id = 0, // Auto-generate database keys
                            address = orderObj.optString("address", ""),
                            phoneNumber = orderObj.optString("phoneNumber", ""),
                            orderAmount = orderObj.optDouble("orderAmount", 0.0),
                            distance = orderObj.optDouble("distance", 0.0),
                            shippingFee = orderObj.optDouble("shippingFee", 0.0),
                            status = orderObj.optString("status", "DANG_GIAO"),
                            timestamp = orderObj.optLong("timestamp", System.currentTimeMillis()),
                            note = orderObj.optString("note", ""),
                            surchargeNightSummer = orderObj.optBoolean("surchargeNightSummer", false),
                            surchargeNightWinter = orderObj.optBoolean("surchargeNightWinter", false),
                            surchargeHeavyRain = orderObj.optBoolean("surchargeHeavyRain", false),
                            surchargeCake = orderObj.optBoolean("surchargeCake", false),
                            surchargeDoorToDoor = orderObj.optBoolean("surchargeDoorToDoor", false),
                            surchargeBuyOnBehalf = orderObj.optBoolean("surchargeBuyOnBehalf", false),
                            surchargeBusStation = orderObj.optBoolean("surchargeBusStation", false),
                            weightGroup = orderObj.optInt("weightGroup", 0),
                            customerPrepaid = orderObj.optBoolean("customerPrepaid", false),
                            shopPaysShipping = orderObj.optBoolean("shopPaysShipping", false)
                        )
                    )
                }
            }

            // Perform DB overwrite transaction
            db.withTransaction {
                db.transactionDao().clearAllTransactions()
                db.categoryDao().clearAllCategories()
                db.shippingOrderDao().clearAllShippingOrders()

                for (cat in catList) {
                    db.categoryDao().insertCategory(cat)
                }
                for (tx in txList) {
                    db.transactionDao().insertTransaction(tx)
                }
                for (order in shipList) {
                    db.shippingOrderDao().insertShippingOrder(order)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
