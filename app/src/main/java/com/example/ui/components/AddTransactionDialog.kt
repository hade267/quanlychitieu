package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import java.util.Calendar

import com.example.data.model.CategoryEntity
import com.example.data.model.ShippingOrder
import android.widget.Toast
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    initialDateMillis: Long,
    dynamicCategories: List<CategoryEntity> = emptyList(),
    shippingOrders: List<ShippingOrder> = emptyList(),
    onDismiss: () -> Unit,
    onCreateCategory: (name: String, emoji: String, colorHex: String, type: String) -> Unit = { _, _, _, _ -> },
    onSave: (
        title: String,
        amount: Double,
        isIncome: Boolean,
        category: String,
        note: String,
        createShippingOrder: Boolean,
        shipAddress: String,
        shipPhone: String,
        shipDistance: Double,
        shipFee: Double
    ) -> Unit
) {
    var isIncome by remember { mutableStateOf(false) }
    var amountString by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showQuickAdd by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var createShippingOrder by remember { mutableStateOf(false) }
    var shipAddress by remember { mutableStateOf("") }
    var shipPhone by remember { mutableStateOf("") }
    var shipDistanceText by remember { mutableStateOf("") }
    val shipDistanceVal = shipDistanceText.toDoubleOrNull() ?: 0.0

    // Auto calculated shipping fee based on distance
    val baseShipFee = remember(shipDistanceVal) {
        when {
            shipDistanceVal <= 0.0 -> 0.0
            shipDistanceVal <= 2.0 -> 10000.0
            shipDistanceVal <= 4.0 -> 15000.0
            shipDistanceVal <= 6.0 -> 20000.0
            shipDistanceVal <= 8.0 -> 25000.0
            shipDistanceVal <= 10.0 -> 30000.0
            shipDistanceVal <= 15.0 -> shipDistanceVal * 4000.0
            else -> shipDistanceVal * 5000.0
        }
    }

    var shipFeeText by remember { mutableStateOf("") }
    var isManualShipFee by remember { mutableStateOf(false) }

    LaunchedEffect(baseShipFee) {
        if (!isManualShipFee) {
            shipFeeText = if (baseShipFee == 0.0) "" else baseShipFee.toInt().toString()
        }
    }

    val suggestedOrders = remember(shipAddress, shipPhone) {
        if (shipAddress.isBlank() && shipPhone.isBlank()) {
            emptyList()
        } else {
            shippingOrders.filter { order ->
                (shipAddress.isNotBlank() && order.address.lowercase().contains(shipAddress.lowercase())) ||
                (shipPhone.isNotBlank() && order.phoneNumber.contains(shipPhone))
            }.distinctBy { "${it.address.trim().lowercase()}_${it.phoneNumber.trim()}" }.take(3)
        }
    }
    
    val categoriesList = if (dynamicCategories.isNotEmpty()) {
        dynamicCategories.filter { cat ->
            if (isIncome) cat.type == "INCOME" else cat.type == "EXPENSE"
        }.map { cat ->
            CategoryData(cat.name, cat.emoji, try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch(e: Exception) { Color(0xFFFF8A00) })
        }.distinctBy { it.name.trim().lowercase() }
    } else {
        if (isIncome) {
            CategoryConfig.incomeCategories
        } else {
            CategoryConfig.expenseCategories
        }
    }
    
    var selectedCategory by remember(isIncome) { 
        mutableStateOf(categoriesList.firstOrNull()?.name ?: "Khác") 
    }

    if (showQuickAdd) {
        var newCatName by remember { mutableStateOf("") }
        var newCatEmoji by remember { mutableStateOf("⭐") }
        var selectedColorHex by remember { mutableStateOf("#FF8A00") }
        
        val quickEmojiList = listOf("🍔", "🛒", "🩺", "🏫", "📽️", "⚡", "🎁", "💵", "📈", "⭐", "🏋️", "✈️", "🐱", "☕")
        val quickColorHexList = listOf("#F87171", "#FB923C", "#FBBF24", "#34D399", "#60A5FA", "#A78BFA", "#F472B6", "#FF8A00")

        AlertDialog(
            onDismissRequest = { showQuickAdd = false },
            title = {
                Text(
                    text = "Thêm danh mục mới 🏷️",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Danh mục này sẽ thuộc loại: ${if (isIncome) "THU NHẬP" else "CHI PHÍ"}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                    
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Tên danh mục", color = DeepOrangePrimary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepOrangePrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column {
                        Text(
                            text = "CHỌN BIỂU TƯỢNG (EMOJI)",
                            color = OrangeHighlight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newCatEmoji,
                                onValueChange = { if (it.length <= 4) newCatEmoji = it },
                                label = { Text("Emoji", color = DeepOrangePrimary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DeepOrangePrimary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                modifier = Modifier.width(70.dp)
                            )
                            
                            Box(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val firstFew = quickEmojiList.take(6)
                                    for (emoji in firstFew) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (newCatEmoji == emoji) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                                                .clickable { newCatEmoji = emoji },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = emoji, fontSize = 20.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Column {
                        Text(
                            text = "CHỌN MÀU SẮC",
                            color = OrangeHighlight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (hex in quickColorHexList) {
                                val col = Color(android.graphics.Color.parseColor(hex))
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(col)
                                        .border(
                                            width = if (selectedColorHex == hex) 2.dp else 0.dp,
                                            color = Color.White,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { selectedColorHex = hex }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newCatName.trim()
                        if (name.isNotEmpty()) {
                            onCreateCategory(
                                name,
                                newCatEmoji.trim().ifEmpty { "⭐" },
                                selectedColorHex,
                                if (isIncome) "INCOME" else "EXPENSE"
                            )
                            // Auto select the newly created category
                            selectedCategory = name
                            showQuickAdd = false
                        }
                    },
                    enabled = newCatName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepOrangePrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Tạo mới", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showQuickAdd = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6080A0F)) // Translucent overlay
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color(0x99FF8A00), Color(0x1AFF8A00))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF141720)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Thêm Giao Dịch",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .background(WhiteOpacity10, RoundedCornerShape(12.dp))
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Income / Expense Segmented Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF090B0F))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (!isIncome) Color(0xFFBA1A1A) else Color.Transparent)
                                .clickable { isIncome = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CHI PHÍ",
                                color = if (!isIncome) Color.White else WhiteOpacity70,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isIncome) Color(0xFF0058BC) else Color.Transparent)
                                .clickable { isIncome = true }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "THU NHẬP",
                                color = if (isIncome) Color.White else WhiteOpacity70,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Amount Input
                    Text(
                        text = "SỐ TIỀN",
                        color = OrangeHighlight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountString,
                        onValueChange = { input ->
                            val cleanInput = input.filter { it.isDigit() }
                            if (cleanInput.isEmpty()) {
                                amountString = ""
                            } else {
                                val parsed = cleanInput.toDoubleOrNull() ?: 0.0
                                val df = java.text.DecimalFormat("#,###")
                                amountString = df.format(parsed)
                            }
                        },
                        textStyle = LocalTextStyle.current.copy(
                            color = if (isIncome) Color(0xFF60A5FA) else Color(0xFFF87171),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        placeholder = {
                            Text(
                                text = "0đ",
                                color = WhiteOpacity20,
                                fontSize = 24.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isIncome) Color(0xFF60A5FA) else Color(0xFFF87171),
                            unfocusedBorderColor = WhiteOpacity10,
                            focusedContainerColor = Color(0xFF0C0E12),
                            unfocusedContainerColor = Color(0xFF0C0E12)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Buttons to quickly append zeros (VND optimized)
                    val hasBaseNumber = amountString.filter { it.isDigit() }.isNotEmpty()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val appendZeros: (String) -> Unit = { zeroString ->
                            val clean = amountString.filter { it.isDigit() }
                            if (clean.isNotEmpty()) {
                                val newStr = clean + zeroString
                                val parsed = newStr.toDoubleOrNull() ?: 0.0
                                val df = java.text.DecimalFormat("#,###")
                                amountString = df.format(parsed)
                            }
                        }

                        // Button +000
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF161920).copy(alpha = if (hasBaseNumber) 1f else 0.5f))
                                .border(
                                    width = 1.dp,
                                    color = if (hasBaseNumber) DeepOrangePrimary.copy(alpha = 0.4f) else WhiteOpacity10,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = hasBaseNumber) { appendZeros("000") }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+000",
                                color = if (hasBaseNumber) DeepOrangePrimary else Color.White.copy(alpha = 0.3f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Button +000.000
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF161920).copy(alpha = if (hasBaseNumber) 1f else 0.5f))
                                .border(
                                    width = 1.dp,
                                    color = if (hasBaseNumber) DeepOrangePrimary.copy(alpha = 0.4f) else WhiteOpacity10,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = hasBaseNumber) { appendZeros("000000") }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+000.000",
                                color = if (hasBaseNumber) DeepOrangePrimary else Color.White.copy(alpha = 0.3f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Title Input
                    Text(
                        text = "TIÊU ĐỀ GIAO DỊCH",
                        color = OrangeHighlight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Ví dụ: Đổ xăng xe máy, Ăn phở", color = WhiteOpacity50) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepOrangePrimary,
                            unfocusedBorderColor = WhiteOpacity10,
                            focusedContainerColor = Color(0xFF0C0E12),
                            unfocusedContainerColor = Color(0xFF0C0E12)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Category Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DANH MỤC",
                            color = OrangeHighlight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showQuickAdd = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "+ Thêm nhanh",
                                color = DeepOrangePrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Simple flow list of categories
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val chunks = categoriesList.chunked(3)
                        for (rowItems in chunks) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (cat in rowItems) {
                                    val isSelected = selectedCategory == cat.name
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) cat.color.copy(alpha = 0.25f) 
                                                else Color(0xFF0C0E12)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) cat.color else WhiteOpacity10,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { selectedCategory = cat.name }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(text = cat.emoji, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = cat.name,
                                                color = if (isSelected) Color.White else WhiteOpacity70,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                                // Fill missing items to avoid uneven rows
                                if (rowItems.size < 3) {
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Note Input
                    Text(
                        text = "GHI CHÚ (TÙY CHỌN)",
                        color = OrangeHighlight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text("Ghi chú thêm...", color = WhiteOpacity50) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepOrangePrimary,
                            unfocusedBorderColor = WhiteOpacity10,
                            focusedContainerColor = Color(0xFF0C0E12),
                            unfocusedContainerColor = Color(0xFF0C0E12)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Quick Shipping toggle row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0C0E12))
                            .border(
                                width = 1.dp,
                                color = if (createShippingOrder) DeepOrangePrimary.copy(alpha = 0.5f) else WhiteOpacity10,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { createShippingOrder = !createShippingOrder }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Đồng thời tạo đơn ship nhanh",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Từ thông tin giao dịch này",
                                    color = WhiteOpacity50,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Checkbox(
                            checked = createShippingOrder,
                            onCheckedChange = { createShippingOrder = it },
                            colors = CheckboxDefaults.colors(checkedColor = DeepOrangePrimary)
                        )
                    }

                    AnimatedVisibility(
                        visible = createShippingOrder,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF090B0F))
                                .border(1.dp, DeepOrangePrimary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "THÔNG TIN ĐƠN SHIP NHANH",
                                color = OrangeHighlight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            // SUGGESTIONS PANEL
                            if (suggestedOrders.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Gợi ý từ lịch sử ship (Bấm để điền nhanh):",
                                        color = SuccessGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    suggestedOrders.forEach { order ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White.copy(alpha = 0.05f))
                                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    shipAddress = order.address
                                                    shipPhone = order.phoneNumber
                                                    shipDistanceText = if (order.distance % 1.0 == 0.0) order.distance.toInt().toString() else order.distance.toString()
                                                    shipFeeText = order.shippingFee.toInt().toString()
                                                    isManualShipFee = true
                                                }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "📍 ${order.address} (${order.phoneNumber})",
                                                color = Color.White.copy(alpha = 0.9f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${order.distance}km",
                                                color = OrangeHighlight,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // 1. Địa chỉ giao
                            OutlinedTextField(
                                value = shipAddress,
                                onValueChange = { shipAddress = it },
                                label = { Text("Địa chỉ nhận hàng (Ví dụ: 123 Nguyễn Trãi)", color = WhiteOpacity70) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DeepOrangePrimary,
                                    unfocusedBorderColor = WhiteOpacity10,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // 2. Số điện thoại
                            OutlinedTextField(
                                value = shipPhone,
                                onValueChange = { shipPhone = it },
                                label = { Text("Số điện thoại khách hàng", color = WhiteOpacity70) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DeepOrangePrimary,
                                    unfocusedBorderColor = WhiteOpacity10,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // 3. Khoảng cách & Quick presets
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Khoảng cách (km):",
                                    color = WhiteOpacity50,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedTextField(
                                    value = shipDistanceText,
                                    onValueChange = { input ->
                                        // Allow digits and single dot
                                        val filtered = input.filter { it.isDigit() || it == '.' }
                                        shipDistanceText = filtered
                                    },
                                    placeholder = { Text("Nhập km (Ví dụ: 3.5)", color = Color.White.copy(alpha = 0.3f)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DeepOrangePrimary,
                                        unfocusedBorderColor = WhiteOpacity10,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                val distancePresets = listOf(1.0, 2.0, 3.0, 5.0, 8.0, 10.0, 15.0, 20.0)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    distancePresets.forEach { d ->
                                        val isSelected = shipDistanceText == d.toString() || shipDistanceText.toDoubleOrNull() == d
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isSelected) DeepOrangePrimary.copy(alpha = 0.25f) else Color(0x0AFFFFFF)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) DeepOrangePrimary else WhiteOpacity10,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable {
                                                    shipDistanceText = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (d % 1.0 == 0.0) "${d.toInt()}k" else "${d}k",
                                                color = if (isSelected) OrangeHighlight else Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // 4. Phí Ship
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Phí ship dự tính (đ):",
                                        color = WhiteOpacity50,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            isManualShipFee = !isManualShipFee
                                            if (!isManualShipFee) {
                                                shipFeeText = if (baseShipFee == 0.0) "" else baseShipFee.toInt().toString()
                                            }
                                        }
                                    ) {
                                        Checkbox(
                                            checked = isManualShipFee,
                                            onCheckedChange = {
                                                isManualShipFee = it
                                                if (!isManualShipFee) {
                                                    shipFeeText = if (baseShipFee == 0.0) "" else baseShipFee.toInt().toString()
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = DeepOrangePrimary),
                                            modifier = Modifier.scale(0.8f)
                                        )
                                        Text("Sửa thủ công", color = WhiteOpacity70, fontSize = 10.sp)
                                    }
                                }
                                OutlinedTextField(
                                    value = shipFeeText,
                                    onValueChange = { 
                                        shipFeeText = it.filter { c -> c.isDigit() }
                                        isManualShipFee = true
                                    },
                                    placeholder = { Text("Ví dụ: 15000", color = Color.White.copy(alpha = 0.3f)) },
                                    enabled = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DeepOrangePrimary,
                                        unfocusedBorderColor = WhiteOpacity10,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            val cleanInput = amountString.filter { it.isDigit() }
                            val amount = cleanInput.toDoubleOrNull() ?: 0.0
                            if (amount > 0) {
                                if (createShippingOrder) {
                                    if (shipAddress.isBlank()) {
                                        Toast.makeText(context, "Vui lòng nhập địa chỉ giao hàng", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                }
                                onSave(
                                    title,
                                    amount,
                                    isIncome,
                                    selectedCategory,
                                    note,
                                    createShippingOrder,
                                    shipAddress,
                                    shipPhone,
                                    shipDistanceVal,
                                    shipFeeText.toDoubleOrNull() ?: 0.0
                                )
                            }
                        },
                        enabled = amountString.isNotBlank() && (amountString.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0) > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepOrangePrimary,
                            contentColor = Color.Black,
                            disabledContainerColor = WhiteOpacity10,
                            disabledContentColor = WhiteOpacity50
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "LƯU GIAO DỊCH",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}
