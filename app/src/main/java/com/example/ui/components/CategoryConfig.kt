package com.example.ui.components

import androidx.compose.ui.graphics.Color
import com.example.data.model.CategoryEntity

data class CategoryData(
    val name: String,
    val emoji: String,
    val color: Color
)

object CategoryConfig {
    val expenseCategories = listOf(
        CategoryData("Ăn uống", "🍜", Color(0xFFF87171)),
        CategoryData("Nhiên liệu", "⛽", Color(0xFFFB923C)),
        CategoryData("Mua sắm", "🛍️", Color(0xFFFBBF24)),
        CategoryData("Giải trí", "🎮", Color(0xFF34D399)),
        CategoryData("Di chuyển", "🚌", Color(0xFF60A5FA)),
        CategoryData("Khác", "📦", Color(0xFFA78BFA))
    )

    val incomeCategories = listOf(
        CategoryData("Lương", "💼", Color(0xFF34D399)),
        CategoryData("Thu nhập phụ", "💵", Color(0xFF60A5FA)),
        CategoryData("Thu nhập giao hàng", "🏍️", Color(0xFFFF9E2B)),
        CategoryData("Quà tặng", "🎁", Color(0xFFF472B6)),
        CategoryData("Khác", "🌀", Color(0xFFA78BFA))
    )

    fun getEmoji(categoryName: String, dynamicCategories: List<CategoryEntity> = emptyList()): String {
        val foundDynamic = dynamicCategories.find { it.name.equals(categoryName, ignoreCase = true) }
        if (foundDynamic != null) return foundDynamic.emoji
        return (expenseCategories + incomeCategories).find { it.name == categoryName }?.emoji ?: "💰"
    }

    fun getColor(categoryName: String, dynamicCategories: List<CategoryEntity> = emptyList()): Color {
        val foundDynamic = dynamicCategories.find { it.name.equals(categoryName, ignoreCase = true) }
        if (foundDynamic != null) {
            return try {
                Color(android.graphics.Color.parseColor(foundDynamic.colorHex))
            } catch (e: Exception) {
                Color(0xFFFF8A00)
            }
        }
        return (expenseCategories + incomeCategories).find { it.name == categoryName }?.color ?: Color(0xFFFF8A00)
    }
}
