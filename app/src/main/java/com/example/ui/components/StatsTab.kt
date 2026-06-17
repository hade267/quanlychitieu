package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Transaction
import com.example.data.model.CategoryEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.TransactionViewModel
import java.text.DecimalFormat

data class CategoryStat(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val color: Color,
    val emoji: String
)

@Composable
fun StatsTab(viewModel: TransactionViewModel) {
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val monthlyTransactions by viewModel.monthlyTransactions.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    val monthNames = listOf(
        "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
        "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
    )

    var isExpenseView by remember { mutableStateOf(true) }

    // Aggregate category calculations
    val filteredTransactions = remember(monthlyTransactions, isExpenseView) {
        val targetType = if (isExpenseView) "EXPENSE" else "INCOME"
        monthlyTransactions.filter { it.type == targetType }
    }

    val totalAmount = remember(filteredTransactions) {
        filteredTransactions.sumOf { it.amount }
    }

    val stats = remember(filteredTransactions, totalAmount, allCategories) {
        if (totalAmount == 0.0) {
            emptyList()
        } else {
            filteredTransactions
                .groupBy { it.category }
                .map { (catName, txs) ->
                    val sum = txs.sumOf { it.amount }
                    CategoryStat(
                        category = catName,
                        amount = sum,
                        percentage = (sum / totalAmount).toFloat(),
                        color = CategoryConfig.getColor(catName, allCategories),
                        emoji = CategoryConfig.getEmoji(catName, allCategories)
                    )
                }
                .sortedByDescending { it.amount }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
    ) {
        // Month Indicator
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Báo Cáo ${monthNames[selectedMonth]}, $selectedYear",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // View Toggles: CHI PHÍ vs THU NHẬP
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF090B0F))
                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isExpenseView) Color(0xFFBA1A1A).copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isExpenseView) Color(0xFFBA1A1A) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { isExpenseView = true }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PHÂN TÍCH CHI PHÍ",
                        color = if (isExpenseView) ErrorRed else WhiteOpacity70,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!isExpenseView) Color(0xFF0058BC).copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (!isExpenseView) Color(0xFF0058BC) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { isExpenseView = false }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PHÂN TÍCH THU NHẬP",
                        color = if (!isExpenseView) IncomeBlue else WhiteOpacity70,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Main Chart Bento Widget
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (isExpenseView) "TỔNG SỐ CHI TIÊU" else "TỔNG SỐ THU NHẬP",
                        color = WhiteOpacity50,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatFullAmount(totalAmount),
                        color = if (isExpenseView) ErrorRed else SuccessGreen,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (stats.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Không có số liệu báo cáo trong tháng này.",
                                color = WhiteOpacity50,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Custom stacked line chart
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                        ) {
                            stats.forEach { stat ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(stat.percentage.coerceAtLeast(0.01f))
                                        .background(stat.color)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Category break-downs with details progress bar
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            stats.forEach { stat ->
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(stat.color.copy(alpha = 0.15f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = stat.emoji, fontSize = 13.sp)
                                            }
                                            Text(
                                                text = stat.category,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = formatFullAmount(stat.amount),
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = String.format("%.0f%%", stat.percentage * 100),
                                                color = WhiteOpacity50,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Background track
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(WhiteOpacity10)
                                    ) {
                                        // Colored portion
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(stat.percentage)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(stat.color)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // AI Intelligence Insights Card
        if (stats.isNotEmpty() && isExpenseView) {
            item {
                val highestCategory = stats.firstOrNull()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💡", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "GỢI Ý TÀI CHÍNH",
                                color = OrangeHighlight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (highestCategory != null) {
                                "Bạn dành nhiều chi phí nhất cho danh mục " +
                                        "\"${highestCategory.category}\" (${String.format("%.0f%%", highestCategory.percentage * 100)} tổng thẻ chi tiêu). " +
                                        "Hãy cân nhắc lên kế hoạch cắt bớt các khoản mua sắm nhỏ không cần thiết để tối ưu tích lũY."
                            } else {
                                "Chi tiêu của bạn trong tháng này ở mức tương đối cân bằng. Tiếp tục duy trì thói quen ghi chép tốt!"
                            },
                            color = WhiteOpacity70,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatFullAmount(amount: Double): String {
    val df = DecimalFormat("#,###")
    val formatted = df.format(amount).replace(',', '.')
    return "${formatted}đ"
}
