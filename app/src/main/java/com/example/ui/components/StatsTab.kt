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
import com.example.data.model.ShippingOrder
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

data class SurchargeStat(
    val name: String,
    val count: Int,
    val amount: Double
)

@Composable
fun StatsTab(viewModel: TransactionViewModel) {
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val monthlyTransactions by viewModel.monthlyTransactions.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val allShippingOrders by viewModel.allShippingOrders.collectAsStateWithLifecycle()

    val monthNames = listOf(
        "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
        "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
    )

    // Three tabs: "EXPENSE", "INCOME", "SHIPPING"
    var selectedTab by remember { mutableStateOf("EXPENSE") }

    // --- TRANSACTIONS CALCULATIONS ---
    val filteredTransactions = remember(monthlyTransactions, selectedTab) {
        val targetType = if (selectedTab == "EXPENSE") "EXPENSE" else "INCOME"
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

    // --- SHIPPING ORDERS CALCULATIONS ---
    val monthlyShippingOrders = remember(allShippingOrders, selectedYear, selectedMonth) {
        allShippingOrders.filter { order ->
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = order.timestamp }
            cal.get(java.util.Calendar.YEAR) == selectedYear && cal.get(java.util.Calendar.MONTH) == selectedMonth
        }
    }

    val totalShippingOrders = monthlyShippingOrders.size
    val deliveredOrdersCount = monthlyShippingOrders.count { it.status == "DA_GIAO" }
    val deliveringOrdersCount = monthlyShippingOrders.count { it.status == "DANG_GIAO" }
    val cancelledOrdersCount = monthlyShippingOrders.count { it.status == "DA_HUY" }

    val deliveredOrders = monthlyShippingOrders.filter { it.status == "DA_GIAO" }
    val totalShippingFeeEarned = deliveredOrders.sumOf { it.shippingFee }
    val totalCodCollected = deliveredOrders.filter { !it.customerPrepaid }.sumOf { it.orderAmount }
    val totalDistanceTravelled = deliveredOrders.sumOf { it.distance }
    val avgFeePerKm = if (totalDistanceTravelled > 0.0) totalShippingFeeEarned / totalDistanceTravelled else 0.0

    val pendingCod = monthlyShippingOrders.filter { it.status == "DANG_GIAO" && !it.customerPrepaid }.sumOf { it.orderAmount }

    val surchargeStats = remember(deliveredOrders) {
        val countNightSummer = deliveredOrders.count { it.surchargeNightSummer }
        val countNightWinter = deliveredOrders.count { it.surchargeNightWinter }
        val countHeavyRain = deliveredOrders.count { it.surchargeHeavyRain }
        val countCake = deliveredOrders.count { it.surchargeCake }
        val countDoorToDoor = deliveredOrders.count { it.surchargeDoorToDoor }
        val countBuyOnBehalf = deliveredOrders.count { it.surchargeBuyOnBehalf }
        val countBusStation = deliveredOrders.count { it.surchargeBusStation }

        listOf(
            SurchargeStat("Sau 22h hè 🌃", countNightSummer, countNightSummer * 5000.0),
            SurchargeStat("Sau 21h30 đông ❄️", countNightWinter, countNightWinter * 5000.0),
            SurchargeStat("Mưa to 🌧️", countHeavyRain, countHeavyRain * 5000.0),
            SurchargeStat("Bánh SN 🎂", countCake, countCake * 5000.0),
            SurchargeStat("Tận cửa 🚪", countDoorToDoor, countDoorToDoor * 5000.0),
            SurchargeStat("Mua hộ 🛍️", countBuyOnBehalf, countBuyOnBehalf * 5000.0),
            SurchargeStat("Bến xe/HL 🚌", countBusStation, countBusStation * 5000.0)
        ).filter { it.count > 0 }.sortedByDescending { it.amount }
    }

    val totalSurchargeEarned = surchargeStats.sumOf { it.amount }

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

        // View Toggles: CHI PHÍ vs THU NHẬP vs ĐƠN SHIP
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF090B0F))
                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab Chi Phi
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedTab == "EXPENSE") Color(0xFFBA1A1A).copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (selectedTab == "EXPENSE") Color(0xFFBA1A1A) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedTab = "EXPENSE" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CHI PHÍ",
                        color = if (selectedTab == "EXPENSE") ErrorRed else WhiteOpacity70,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // Tab Thu Nhap
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedTab == "INCOME") Color(0xFF0058BC).copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (selectedTab == "INCOME") Color(0xFF0058BC) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedTab = "INCOME" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "THU NHẬP",
                        color = if (selectedTab == "INCOME") IncomeBlue else WhiteOpacity70,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // Tab Don Ship
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedTab == "SHIPPING") OrangeHighlight.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (selectedTab == "SHIPPING") OrangeHighlight else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedTab = "SHIPPING" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ĐƠN SHIP 🏍️",
                        color = if (selectedTab == "SHIPPING") OrangeHighlight else WhiteOpacity70,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // CONDITIONAL RENDERING OF CONTENT
        if (selectedTab == "EXPENSE" || selectedTab == "INCOME") {
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
                            text = if (selectedTab == "EXPENSE") "TỔNG SỐ CHI TIÊU" else "TỔNG SỐ THU NHẬP",
                            color = WhiteOpacity50,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatFullAmount(totalAmount),
                            color = if (selectedTab == "EXPENSE") ErrorRed else SuccessGreen,
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
            if (stats.isNotEmpty() && selectedTab == "EXPENSE") {
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
        } else {
            // --- SHIPPING REPORTS VIEW ---
            
            // 1. Total Delivery Earnings Bento Card
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
                            text = "TỔNG THU NHẬP PHÍ SHIP",
                            color = OrangeHighlight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatFullAmount(totalShippingFeeEarned),
                            color = SuccessGreen,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = WhiteOpacity10)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Quãng đường đã đi", color = WhiteOpacity50, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${String.format("%.1f", totalDistanceTravelled)} km",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Phí ship trung bình", color = WhiteOpacity50, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${formatFullAmount(avgFeePerKm)}/km",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 2. Orders Status Distribution Card
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
                            text = "TRẠNG THÁI ĐƠN HÀNG (Tổng: $totalShippingOrders đơn)",
                            color = WhiteOpacity50,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (totalShippingOrders == 0) {
                            Text(
                                text = "Không có đơn ship nào trong tháng này.",
                                color = WhiteOpacity50,
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            // Stacked progress bar
                            val pDelivered = deliveredOrdersCount.toFloat() / totalShippingOrders
                            val pDelivering = deliveringOrdersCount.toFloat() / totalShippingOrders
                            val pCancelled = cancelledOrdersCount.toFloat() / totalShippingOrders

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                            ) {
                                if (deliveredOrdersCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(pDelivered.coerceAtLeast(0.01f))
                                            .background(SuccessGreen)
                                    )
                                }
                                if (deliveringOrdersCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(pDelivering.coerceAtLeast(0.01f))
                                            .background(Color(0xFF29B6F6))
                                    )
                                }
                                if (cancelledOrdersCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(pCancelled.coerceAtLeast(0.01f))
                                            .background(ErrorRed)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Legend and counts
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.size(10.dp).background(SuccessGreen, CircleShape))
                                        Text("Đã giao thành công", color = Color.White, fontSize = 13.sp)
                                    }
                                    Text("$deliveredOrdersCount đơn (${String.format("%.0f%%", pDelivered * 100)})", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.size(10.dp).background(Color(0xFF29B6F6), CircleShape))
                                        Text("Đang giao hàng", color = Color.White, fontSize = 13.sp)
                                    }
                                    Text("$deliveringOrdersCount đơn (${String.format("%.0f%%", pDelivering * 100)})", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.size(10.dp).background(ErrorRed, CircleShape))
                                        Text("Đã hủy đơn", color = Color.White, fontSize = 13.sp)
                                    }
                                    Text("$cancelledOrdersCount đơn (${String.format("%.0f%%", pCancelled * 100)})", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 3. COD Collected Card
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
                            text = "TIỀN COD THU HỘ",
                            color = IncomeBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatFullAmount(totalCodCollected),
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Đang vận chuyển (Chưa thu):", color = WhiteOpacity50, fontSize = 12.sp)
                            Text(
                                text = formatFullAmount(pendingCod),
                                color = Color(0xFF29B6F6),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 4. Surcharges breakdown card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BÁO CÁO PHỤ PHÍ",
                                color = WhiteOpacity50,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "+ ${formatFullAmount(totalSurchargeEarned)}",
                                color = SuccessGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (surchargeStats.isEmpty()) {
                            Text(
                                text = "Chưa phát sinh phụ phí nào được ghi nhận trong tháng này.",
                                color = WhiteOpacity50,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                surchargeStats.forEach { stat ->
                                    val percent = if (totalSurchargeEarned > 0) stat.amount / totalSurchargeEarned else 0.0
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stat.name,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${stat.count} lần",
                                                    color = WhiteOpacity70,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = formatFullAmount(stat.amount),
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Surcharge progress bar
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(WhiteOpacity10)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(percent.toFloat())
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(OrangeHighlight)
                                            )
                                        }
                                    }
                                }
                            }
                        }
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
