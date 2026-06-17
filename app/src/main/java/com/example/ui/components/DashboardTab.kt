package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Transaction
import com.example.data.model.CategoryEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.TransactionViewModel
import java.text.DecimalFormat
import java.util.Calendar

@Composable
fun DashboardTab(
    viewModel: TransactionViewModel,
    onShowAddDialog: () -> Unit
) {
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    
    val monthlyTransactions by viewModel.monthlyTransactions.collectAsStateWithLifecycle()
    val selectedDayTransactions by viewModel.selectedDayTransactions.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    // Calculate bento values
    val totalIncome = remember(monthlyTransactions) {
        monthlyTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val totalExpense = remember(monthlyTransactions) {
        monthlyTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }
    val netBalance = totalIncome - totalExpense

    // Group monthly transactions by day to render indicators
    val dailySummary = remember(monthlyTransactions) {
        monthlyTransactions.groupBy {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.dateMillis
            cal.get(Calendar.DAY_OF_MONTH)
        }.mapValues { (_, txs) ->
            val income = txs.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            Pair(income, expense)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
    ) {
        // 1. Calendar View Block
        item {
            CalendarBentoBlock(
                year = selectedYear,
                month = selectedMonth,
                selectedDay = selectedDay,
                dailySummary = dailySummary,
                onMonthChange = { newYear, newMonth ->
                    viewModel.selectMonth(newYear, newMonth)
                },
                onDaySelect = { day ->
                    viewModel.selectDay(day)
                }
            )
        }

        // 2. Bento Grid Summary Cards
        item {
            BentoGridSummary(
                netBalance = netBalance,
                totalIncome = totalIncome,
                totalExpense = totalExpense
            )
        }

        // 3. Daily Transactions List
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Giao dịch $selectedDay Th${selectedMonth + 1}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Lịch sử",
                    color = DeepOrangePrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                )
            }
        }

        if (selectedDayTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        .background(GlassCardBg)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📭", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Không có giao dịch nào trong ngày này",
                            color = WhiteOpacity50,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onShowAddDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = SoftOrangeContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "+ Thêm ngay", color = OrangeHighlight, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            items(selectedDayTransactions, key = { it.id }) { transaction ->
                TransactionListItem(
                    transaction = transaction,
                    dynamicCategories = allCategories,
                    onDelete = { viewModel.deleteTransaction(transaction) }
                )
            }
            
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onShowAddDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = DeepOrangePrimary),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .wrapContentWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Thêm",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Thêm Giao Dịch",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarBentoBlock(
    year: Int,
    month: Int,
    selectedDay: Int,
    dailySummary: Map<Int, Pair<Double, Double>>,
    onMonthChange: (Int, Int) -> Unit,
    onDaySelect: (Int) -> Unit
) {
    val monthNames = listOf(
        "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
        "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
    )

    // Calculate dates config
    val calendar = remember(year, month) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday...
    val offset = (firstDayOfWeek - 2 + 7) % 7 // Monday offset

    val prevCalendar = remember(year, month) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            add(Calendar.MONTH, -1)
        }
    }
    val prevMaxDays = prevCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = GlassCardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month Header Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${monthNames[month]}, $year",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            val newCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                add(Calendar.MONTH, -1)
                            }
                            onMonthChange(newCal.get(Calendar.YEAR), newCal.get(Calendar.MONTH))
                        },
                        modifier = Modifier
                            .background(WhiteOpacity10, RoundedCornerShape(8.dp))
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Tháng trước",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            val newCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                add(Calendar.MONTH, 1)
                            }
                            onMonthChange(newCal.get(Calendar.YEAR), newCal.get(Calendar.MONTH))
                        },
                        modifier = Modifier
                            .background(WhiteOpacity10, RoundedCornerShape(8.dp))
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Tháng sau",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Week Day Grid Header
            Row(modifier = Modifier.fillMaxWidth()) {
                val headers = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
                headers.forEachIndexed { idx, label ->
                    val isSunday = idx == 6
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = if (isSunday) ErrorRed else WhiteOpacity50,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 42 Grid Slots
            val totalSlots = 42
            var dayCounter = 1
            var nextMonthDayCounter = 1

            for (row in 0 until 6) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.9f)
                                .padding(1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cellIndex < offset) {
                                // Previous month days
                                val prevDay = prevMaxDays - offset + cellIndex + 1
                                Text(
                                    text = prevDay.toString(),
                                    color = WhiteOpacity20,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            } else if (dayCounter <= maxDays) {
                                // Current month days
                                val currentDay = dayCounter
                                val isSelected = currentDay == selectedDay
                                
                                val summary = dailySummary[currentDay]
                                val income = summary?.first ?: 0.0
                                val expense = summary?.second ?: 0.0

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) DeepOrangePrimary 
                                            else Color.Transparent
                                        )
                                        .clickable { onDaySelect(currentDay) }
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        val isSunday = col == 6
                                        Text(
                                            text = currentDay.toString(),
                                            color = if (isSelected) Color.Black 
                                                    else if (isSunday) ErrorRed 
                                                    else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                        
                                        if (income > 0) {
                                            val text = "+${formatCompactAmount(income)}"
                                            Text(
                                                text = text,
                                                color = if (isSelected) Color.Black else SuccessGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        } else if (expense > 0) {
                                            val text = "-${formatCompactAmount(expense)}"
                                            Text(
                                                text = text,
                                                color = if (isSelected) Color.Black else ErrorRed,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                dayCounter++
                            } else {
                                // Next month days
                                Text(
                                    text = nextMonthDayCounter.toString(),
                                    color = WhiteOpacity20,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                nextMonthDayCounter++
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BentoGridSummary(
    netBalance: Double,
    totalIncome: Double,
    totalExpense: Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Balance row (Full width)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = GlassCardBg),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(IncomeBlue.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, IncomeBlue.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "💳", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "SỐ DƯ TỊNH",
                        color = WhiteOpacity50,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatFullAmount(netBalance),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Row of Income and Expense bento boxes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "📈", fontSize = 16.sp)
                        Text(
                            text = "THU NHẬP",
                            color = SuccessGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatFullAmount(totalIncome),
                        color = SuccessGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "📉", fontSize = 16.sp)
                        Text(
                            text = "CHI PHÍ",
                            color = ErrorRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatFullAmount(totalExpense),
                        color = ErrorRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionListItem(
    transaction: Transaction,
    dynamicCategories: List<CategoryEntity> = emptyList(),
    onDelete: () -> Unit
) {
    val isIncome = transaction.type == "INCOME"
    val emoji = CategoryConfig.getEmoji(transaction.category, dynamicCategories)
    val catColor = CategoryConfig.getColor(transaction.category, dynamicCategories)

    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(key1 = transaction.id) {
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = alphaAnim.value)
            .border(1.dp, GlassBorder.copy(alpha = 0.12f * alphaAnim.value), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101319)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Category Icon Bubble
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(catColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, catColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = transaction.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category Pill Tag
                        Box(
                            modifier = Modifier
                                .background(WhiteOpacity10, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = transaction.category,
                                color = WhiteOpacity70,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (transaction.note.isNotBlank()) {
                            Text(
                                text = "• ${transaction.note}",
                                color = WhiteOpacity50,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // Large numeric formatted indicator
                Text(
                    text = if (isIncome) "+${formatFullAmountRaw(transaction.amount)}" 
                           else "-${formatFullAmountRaw(transaction.amount)}",
                    color = if (isIncome) SuccessGreen else ErrorRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa",
                        tint = ErrorRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Decimal format utilities
private fun formatFullAmount(amount: Double): String {
    val df = DecimalFormat("#,###")
    val formatted = df.format(Math.abs(amount)).replace(',', '.')
    return "${formatted}đ"
}

private fun formatFullAmountRaw(amount: Double): String {
    val df = DecimalFormat("#,###")
    val formatted = df.format(amount).replace(',', '.')
    return "${formatted}đ"
}

private fun formatCompactAmount(amount: Double): String {
    return if (amount >= 1000000) {
        val millions = amount / 1000000.0
        val formatted = if (millions % 1.0 == 0.0) String.format("%.0f", millions) else String.format("%.1f", millions)
        "${formatted}tr"
    } else {
        val thousands = (amount / 1000).toInt()
        "${thousands}k"
    }
}
