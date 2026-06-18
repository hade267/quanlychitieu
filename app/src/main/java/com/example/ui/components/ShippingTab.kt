package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ShippingOrder
import com.example.ui.theme.*
import com.example.ui.viewmodel.TransactionViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShippingTab(viewModel: TransactionViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val allOrders by viewModel.allShippingOrders.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterStatus by remember { mutableStateOf("ALL") }

    var showAddDialog by remember { mutableStateOf(false) }
    var orderToEdit by remember { mutableStateOf<ShippingOrder?>(null) }
    var orderToDelete by remember { mutableStateOf<ShippingOrder?>(null) }

    // Computations for delivery dashboard summary
    val completedOrders = allOrders.filter { it.status == "DA_GIAO" }
    val totalShippingFeeEarned = completedOrders.sumOf { it.shippingFee }
    val totalActiveCod = allOrders.filter { it.status == "DANG_GIAO" || it.status == "CHO_GIAO" }.sumOf { it.orderAmount }
    val activeOrdersCount = allOrders.count { it.status == "DANG_GIAO" || it.status == "CHO_GIAO" }

    val currencyFormatter = remember { DecimalFormat("#,###") }

    // Filtered orders list
    val filteredOrders = remember(allOrders, searchQuery, selectedFilterStatus) {
        allOrders.filter { order ->
            val matchStatus = if (selectedFilterStatus == "ALL") true else order.status == selectedFilterStatus
            val matchSearch = searchQuery.isBlank() || 
                    order.address.contains(searchQuery, ignoreCase = true) ||
                    order.phoneNumber.contains(searchQuery) ||
                    order.note.contains(searchQuery, ignoreCase = true)
            matchStatus && matchSearch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
    ) {
        // 1. STATS BANNER
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "QUẢN LÝ ĐƠN SHIP 🏍️",
                        color = OrangeHighlight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Earnings box
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Phí Ship Thu Về 💰", color = WhiteOpacity50, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${currencyFormatter.format(totalShippingFeeEarned)}đ",
                                color = SuccessGreen,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // COD box
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tổng Tiền COD Thu Hộ", color = WhiteOpacity50, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${currencyFormatter.format(totalActiveCod)}đ",
                                color = IncomeBlue,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = WhiteOpacity10)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Đang hoạt động: $activeOrdersCount đơn hàng",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SoftOrangeContainer,
                                contentColor = OrangeHighlight
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tạo đơn", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tạo Đơn Mới", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. SEARCH & CONTROLS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm địa chỉ, SĐT, ghi chú...", color = WhiteOpacity50) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Tìm kiếm", tint = WhiteOpacity70) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = WhiteOpacity70)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepOrangePrimary,
                        unfocusedBorderColor = WhiteOpacity20,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Scrollable Row status Filter Chips
                val statusOptions = listOf(
                    "ALL" to "Tất cả",
                    "CHO_GIAO" to "Chờ giao",
                    "DANG_GIAO" to "Đang giao",
                    "DA_GIAO" to "Đã giao",
                    "DA_HUY" to "Đã hủy"
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    statusOptions.forEach { (code, name) ->
                        val isSelected = selectedFilterStatus == code
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) DeepOrangePrimary.copy(alpha = 0.25f) else Color(0x1F1F2430)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) DeepOrangePrimary else GlassBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedFilterStatus = code }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = name,
                                color = if (isSelected) OrangeHighlight else Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // 3. ORDERS LIST
        if (filteredOrders.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Empty",
                        tint = WhiteOpacity20,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Không tìm thấy đơn ship nào",
                        color = WhiteOpacity50,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bấm 'Tạo Đơn Mới' để bắt đầu quản lý đơn ship và tính cước tự động.",
                        color = WhiteOpacity20,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            items(filteredOrders, key = { it.id }) { order ->
                ShippingOrderCard(
                    order = order,
                    onStatusChange = { newStatus ->
                        viewModel.updateShippingOrder(order.copy(status = newStatus))
                    },
                    onEdit = { orderToEdit = order },
                    onDelete = { orderToDelete = order },
                    onDialPhone = { phone ->
                        try {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$phone")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Không thể mở ứng dụng gọi điện", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    // --- POPUPS & DIALOGS ---

    if (showAddDialog) {
        AddEditShippingOrderDialog(
            order = null,
            onDismiss = { showAddDialog = false },
            onSave = { newOrder ->
                viewModel.insertShippingOrder(newOrder)
                showAddDialog = false
            }
        )
    }

    if (orderToEdit != null) {
        AddEditShippingOrderDialog(
            order = orderToEdit,
            onDismiss = { orderToEdit = null },
            onSave = { updatedOrder ->
                viewModel.updateShippingOrder(updatedOrder)
                orderToEdit = null
            }
        )
    }

    if (orderToDelete != null) {
        AlertDialog(
            onDismissRequest = { orderToDelete = null },
            title = { Text("Xác nhận xoá đơn ⚠️", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xoá đơn ship giao đến: ${orderToDelete?.address}? Hành động này không thể hoàn tác.", color = WhiteOpacity70) },
            confirmButton = {
                Button(
                    onClick = {
                        orderToDelete?.let { viewModel.deleteShippingOrder(it) }
                        orderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("XOÁ ĐƠN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToDelete = null }) {
                    Text("HỦY", color = Color.White)
                }
            },
            containerColor = SolidCardBg,
            textContentColor = Color.White
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShippingOrderCard(
    order: ShippingOrder,
    onStatusChange: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDialPhone: (String) -> Unit
) {
    val currencyFormatter = remember { DecimalFormat("#,###") }
    val timeFormatter = remember { SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault()) }

    val statusBadgeColor = when (order.status) {
        "CHO_GIAO" -> Color(0xFFFFB300) // Rich Amber
        "DANG_GIAO" -> Color(0xFF29B6F6) // Sky Blue
        "DA_GIAO" -> SuccessGreen
        "DA_HUY" -> ErrorRed
        else -> Color.Gray
    }

    val statusText = when (order.status) {
        "CHO_GIAO" -> "Chờ giao 🕒"
        "DANG_GIAO" -> "Đang giao 🏍️"
        "DA_GIAO" -> "Đã giao ✅"
        "DA_HUY" -> "Đã huỷ ❌"
        else -> "Không rõ"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = GlassCardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // CARD HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time
                Text(
                    text = timeFormatter.format(Date(order.timestamp)),
                    color = WhiteOpacity50,
                    fontSize = 11.sp
                )

                // Status Badge
                Box(
                    modifier = Modifier
                        .background(statusBadgeColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, statusBadgeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusBadgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // MAIN SPECS
            Text(
                text = order.address,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onDialPhone(order.phoneNumber) }
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Phone", tint = OrangeHighlight, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = order.phoneNumber,
                        color = OrangeHighlight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(Bấm để gọi 📞)",
                        color = WhiteOpacity50,
                        fontSize = 10.sp
                    )
                }

                val currentContext = LocalContext.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0068FF).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF0068FF).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable {
                            try {
                                val cleanedPhone = order.phoneNumber.filter { it.isDigit() }
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://zalo.me/$cleanedPhone")
                                }
                                currentContext.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(currentContext, "Không thể mở Zalo", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Mở Zalo 💬",
                        color = Color(0xFF80B3FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (order.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "📝 Ghi chú: ",
                        color = WhiteOpacity50,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = order.note,
                        color = WhiteOpacity70,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // COD, SHIP & TOTAL COLLECTIBLE PANEL
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TIỀN THU HỘ (COD)", color = WhiteOpacity50, fontSize = 9.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${currencyFormatter.format(order.orderAmount)} đ",
                            color = IncomeBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(WhiteOpacity10))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("PHÍ SHIP (${order.distance}km)", color = WhiteOpacity50, fontSize = 9.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${currencyFormatter.format(order.shippingFee)} đ",
                                color = OrangeHighlight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Divider(color = WhiteOpacity10)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💵 TỔNG CẦN THU TỪ KHÁCH:",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val totalCollect = order.orderAmount + order.shippingFee
                    Text(
                        text = "${currencyFormatter.format(totalCollect)} đ",
                        color = SuccessGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Surcharges pills quick look
            val surchargesList = remember(order) {
                mutableListOf<String>().apply {
                    if (order.surchargeNightSummer) add("🌃 Sau 22h hè")
                    if (order.surchargeNightWinter) add("❄️ Sau 21h30 đông")
                    if (order.surchargeHeavyRain) add("🌧️ Mưa to")
                    if (order.surchargeCake) add("🎂 Bánh SN")
                    if (order.surchargeDoorToDoor) add("🚪 Tận cửa")
                    if (order.surchargeBuyOnBehalf) add("🛍️ Mua hộ")
                    if (order.surchargeBusStation) add("🚌 Bến xe/HL")
                    when (order.weightGroup) {
                        1 -> add("⚖️ Nặng 10-25kg")
                        2 -> add("⚖️ Nặng 26-40kg")
                        3 -> add("⚖️ Nặng 41-50kg")
                    }
                }
            }

            if (surchargesList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    surchargesList.forEach { p ->
                        Box(
                            modifier = Modifier
                                .background(Color(0x11FFFFFF), RoundedCornerShape(4.dp))
                                .border(0.5.dp, WhiteOpacity10, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(p, color = WhiteOpacity70, fontSize = 9.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = WhiteOpacity10)
            Spacer(modifier = Modifier.height(10.dp))

            // ACTIONS BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick edit & delete icons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = WhiteOpacity50,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ErrorRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Workflow status changes quick buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (order.status == "CHO_GIAO") {
                        TextButton(
                            onClick = { onStatusChange("DA_HUY") },
                            colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed),
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Huỷ Đơn ❌", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onStatusChange("DANG_GIAO") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29B6F6), contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text("Giao Đơn 🏍️", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    } else if (order.status == "DANG_GIAO") {
                        TextButton(
                            onClick = { onStatusChange("DA_HUY") },
                            colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed),
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Huỷ Đơn ❌", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onStatusChange("DA_GIAO") },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text("Hoàn Thành ✅", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    } else if (order.status == "DA_GIAO" || order.status == "DA_HUY") {
                        // Let users reactivate order if needed
                        TextButton(
                            onClick = { onStatusChange("CHO_GIAO") },
                            colors = ButtonDefaults.textButtonColors(contentColor = WhiteOpacity70),
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Kích hoạt lại 🔄", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// Dialog to Add or Edit Order
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditShippingOrderDialog(
    order: ShippingOrder?,
    onDismiss: () -> Unit,
    onSave: (ShippingOrder) -> Unit
) {
    val context = LocalContext.current
    var address by remember { mutableStateOf(order?.address ?: "") }
    var phoneNumber by remember { mutableStateOf(order?.phoneNumber ?: "") }
    var orderAmountText by remember { mutableStateOf(order?.orderAmount?.toInt()?.toString() ?: "") }
    var distanceText by remember { mutableStateOf(order?.distance?.toString() ?: "") }
    var note by remember { mutableStateOf(order?.note ?: "") }
    var status by remember { mutableStateOf(order?.status ?: "DANG_GIAO") }

    // Surcharges Checked States
    var surchargeNightSummer by remember { mutableStateOf(order?.surchargeNightSummer ?: false) }
    var surchargeNightWinter by remember { mutableStateOf(order?.surchargeNightWinter ?: false) }
    var surchargeHeavyRain by remember { mutableStateOf(order?.surchargeHeavyRain ?: false) }
    var surchargeCake by remember { mutableStateOf(order?.surchargeCake ?: false) }
    var surchargeDoorToDoor by remember { mutableStateOf(order?.surchargeDoorToDoor ?: false) }
    var surchargeBuyOnBehalf by remember { mutableStateOf(order?.surchargeBuyOnBehalf ?: false) }
    var surchargeBusStation by remember { mutableStateOf(order?.surchargeBusStation ?: false) }
    // Weight Group: 0: <10, 1: 10-25, 2: 26-40, 3: 41-50
    var weightGroup by remember { mutableStateOf(order?.weightGroup ?: 0) }

    // Override mode for shipping fee
    var isManualShippingFee by remember { mutableStateOf(false) }
    var manualShippingFeeText by remember { mutableStateOf("") }

    // Calculate Dynamic Live Shipping Fee representation
    val distanceVal = distanceText.toDoubleOrNull() ?: 0.0

    val calculatedBaseFee = remember(distanceVal) {
        when {
            distanceVal <= 0.0 -> 0.0
            distanceVal <= 2.0 -> 10000.0
            distanceVal <= 4.0 -> 15000.0
            distanceVal <= 6.0 -> 20000.0
            distanceVal <= 8.0 -> 25000.0
            distanceVal <= 10.0 -> 30000.0
            distanceVal <= 15.0 -> distanceVal * 4000.0
            else -> distanceVal * 5000.0
        }
    }

    val calculatedSurchargeTotal = remember(
        surchargeNightSummer, surchargeNightWinter, surchargeHeavyRain,
        surchargeCake, surchargeDoorToDoor, surchargeBuyOnBehalf, surchargeBusStation, weightGroup
    ) {
        var sum = 0.0
        if (surchargeNightSummer) sum += 5000.0
        if (surchargeNightWinter) sum += 5000.0
        if (surchargeHeavyRain) sum += 5000.0
        if (surchargeCake) sum += 5000.0
        if (surchargeDoorToDoor) sum += 5000.0
        if (surchargeBuyOnBehalf) sum += 5000.0
        if (surchargeBusStation) sum += 5000.0
        
        when (weightGroup) {
            1 -> sum += 5000.0   // 10-25kg
            2 -> sum += 10000.0  // 26-40kg
            3 -> sum += 15000.0  // 41-50kg
        }
        sum
    }

    val totalCalculatedFee = calculatedBaseFee + calculatedSurchargeTotal

    val finalShippingFee = if (isManualShippingFee) {
        manualShippingFeeText.toDoubleOrNull() ?: 0.0
    } else {
        totalCalculatedFee
    }

    val currencyFormatter = remember { DecimalFormat("#,###") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = SolidCardBg),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Title
                    Text(
                        text = if (order == null) "📝 TẠO ĐƠN SHIP MỚI" else "✏️ SỬA ĐƠN SHIP",
                        color = OrangeHighlight,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Scrollable form body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Địa chỉ
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Địa chỉ giao hàng", color = OrangeHighlight) },
                            placeholder = { Text("Số nhà, Tên đường, Phường...", color = WhiteOpacity50) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DeepOrangePrimary,
                                unfocusedBorderColor = WhiteOpacity20,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 2. Số điện thoại & COD Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("SĐT Khách hàng", color = OrangeHighlight) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DeepOrangePrimary,
                                    unfocusedBorderColor = WhiteOpacity20,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.1f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = orderAmountText,
                                onValueChange = { orderAmountText = it },
                                label = { Text("Tiền COD (đ)", color = OrangeHighlight) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DeepOrangePrimary,
                                    unfocusedBorderColor = WhiteOpacity20,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(0.9f),
                                singleLine = true
                            )
                        }

                        // 3. Khoảng cách (Km)
                        OutlinedTextField(
                            value = distanceText,
                            onValueChange = { distanceText = it },
                            label = { Text("Khoảng cách (Km)", color = OrangeHighlight) },
                            placeholder = { Text("Ví dụ: 3.5, 12", color = WhiteOpacity50) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DeepOrangePrimary,
                                unfocusedBorderColor = WhiteOpacity20,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Chọn nhanh khoảng cách (Quick distance selection)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Chọn nhanh khoảng cách:",
                                color = WhiteOpacity50,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val distancePresets = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0, 12.0, 15.0, 20.0)
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(distancePresets) { d ->
                                    val isSelected = distanceText == d.toString() || 
                                            (distanceText.toDoubleOrNull() == d)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) DeepOrangePrimary.copy(alpha = 0.25f) else Color(0x0AFFFFFF)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) DeepOrangePrimary else WhiteOpacity10,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { 
                                                distanceText = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString() 
                                            }
                                            .padding(horizontal = 12.dp, vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (d % 1.0 == 0.0) "${d.toInt()} km" else "$d km",
                                            color = if (isSelected) OrangeHighlight else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Phụ phí Toggles Panel
                        Text(
                            text = "PHỤ PHÍ (BẢNG GIÁ CUỐC):",
                            color = WhiteOpacity50,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                            modifier = Modifier.border(0.5.dp, WhiteOpacity10, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SurchargeCheckboxRow(
                                    label = "🌃 Sau 22h hè (+5k)",
                                    checked = surchargeNightSummer,
                                    onCheckedChange = { surchargeNightSummer = it }
                                )
                                SurchargeCheckboxRow(
                                    label = "❄️ Sau 21h30 đông (+5k)",
                                    checked = surchargeNightWinter,
                                    onCheckedChange = { surchargeNightWinter = it }
                                )
                                SurchargeCheckboxRow(
                                    label = "🌧️ Mưa to (+5k)",
                                    checked = surchargeHeavyRain,
                                    onCheckedChange = { surchargeHeavyRain = it }
                                )
                                SurchargeCheckboxRow(
                                    label = "🎂 Bánh sinh nhật (+5k)",
                                    checked = surchargeCake,
                                    onCheckedChange = { surchargeCake = it }
                                )
                                SurchargeCheckboxRow(
                                    label = "🚪 Giao tận cửa (+5k)",
                                    checked = surchargeDoorToDoor,
                                    onCheckedChange = { surchargeDoorToDoor = it }
                                )
                                SurchargeCheckboxRow(
                                    label = "🛍️ Mua đồ hộ (+5k)",
                                    checked = surchargeBuyOnBehalf,
                                    onCheckedChange = { surchargeBuyOnBehalf = it }
                                )
                                SurchargeCheckboxRow(
                                    label = "🚌 Bến xe / HL (+5k)",
                                    checked = surchargeBusStation,
                                    onCheckedChange = { surchargeBusStation = it }
                                )
                            }
                        }

                        // 5. Weight Group selection
                        Text(
                            text = "TRỌNG LƯỢNG HÀNG HOÁ:",
                            color = WhiteOpacity50,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val weights = listOf(
                                0 to "Nhỏ (<10kg)\n+0đ",
                                1 to "Vừa (10-25kg)\n+5k",
                                2 to "Nặng (26-40kg)\n+10k",
                                3 to "Rất nặng (41-50kg)\n+15k"
                            )
                            weights.forEach { (wg, lbl) ->
                                val isWgSelected = weightGroup == wg
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isWgSelected) DeepOrangePrimary.copy(alpha = 0.25f) else Color(0x0AFFFFFF)
                                        )
                                        .border(
                                            1.dp,
                                            if (isWgSelected) DeepOrangePrimary else WhiteOpacity10,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { weightGroup = wg }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = lbl,
                                        color = if (isWgSelected) OrangeHighlight else Color.White,
                                        fontSize = 8.sp,
                                        lineHeight = 11.sp,
                                        fontWeight = if (isWgSelected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // 6. Manual custom override for ship fee
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isManualShippingFee,
                                onCheckedChange = { isManualShippingFee = it },
                                colors = CheckboxDefaults.colors(checkedColor = DeepOrangePrimary)
                            )
                            Text(
                                "Tự sửa tiền ship thủ công",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (isManualShippingFee) {
                            OutlinedTextField(
                                value = manualShippingFeeText,
                                onValueChange = { manualShippingFeeText = it },
                                label = { Text("Nhập tiền ship tuỳ chỉnh (đ)", color = OrangeHighlight) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DeepOrangePrimary,
                                    unfocusedBorderColor = WhiteOpacity20,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        // 7. Ghi chú & Status dialog panel
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Ghi chú đơn hàng / Tên món hàng", color = OrangeHighlight) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DeepOrangePrimary,
                                unfocusedBorderColor = WhiteOpacity20,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 8. Trạng thái đơn
                        Text(
                            text = "TRẠNG THÁI ĐƠN HÀNG:",
                            color = WhiteOpacity50,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val statusOpts = listOf(
                                "CHO_GIAO" to "Chờ giao",
                                "DANG_GIAO" to "Đang giao",
                                "DA_GIAO" to "Đã giao",
                                "DA_HUY" to "Đã huỷ"
                            )
                            statusOpts.forEach { (st, label) ->
                                val isSelected = status == st
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) DeepOrangePrimary.copy(alpha = 0.2f) else Color(0x06FFFFFF)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) DeepOrangePrimary else WhiteOpacity10,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { status = st }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) OrangeHighlight else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // LIVE PRICING CALCULATION BANNER
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DeepOrangePrimary.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Cước cơ bản (${distanceText.ifEmpty { "0" }} km):", color = WhiteOpacity50, fontSize = 11.sp)
                                Text("${currencyFormatter.format(calculatedBaseFee)} đ", color = Color.White, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tổng phụ phí cộng thêm:", color = WhiteOpacity50, fontSize = 11.sp)
                                Text("+ ${currencyFormatter.format(calculatedSurchargeTotal)} đ", color = Color.White, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Divider(color = WhiteOpacity10)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("TỔNG TIỀN SHIP:", color = OrangeHighlight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${currencyFormatter.format(finalShippingFee)} đ",
                                    color = OrangeHighlight,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ACTION BUTTONS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x1BFFFFFF),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Bỏ qua", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (address.isBlank()) {
                                    Toast.makeText(context, "Vui lòng nhập địa chỉ giao hàng", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val finalOrder = ShippingOrder(
                                    id = order?.id ?: 0,
                                    address = address.trim(),
                                    phoneNumber = phoneNumber.trim(),
                                    orderAmount = orderAmountText.toDoubleOrNull() ?: 0.0,
                                    distance = distanceVal,
                                    shippingFee = finalShippingFee,
                                    status = status,
                                    timestamp = order?.timestamp ?: System.currentTimeMillis(),
                                    note = note.trim(),
                                    surchargeNightSummer = surchargeNightSummer,
                                    surchargeNightWinter = surchargeNightWinter,
                                    surchargeHeavyRain = surchargeHeavyRain,
                                    surchargeCake = surchargeCake,
                                    surchargeDoorToDoor = surchargeDoorToDoor,
                                    surchargeBuyOnBehalf = surchargeBuyOnBehalf,
                                    surchargeBusStation = surchargeBusStation,
                                    weightGroup = weightGroup
                                )
                                onSave(finalOrder)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepOrangePrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(
                                text = if (order == null) "Tạo Đơn 🚀" else "Lưu Lại 💾",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SurchargeCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = DeepOrangePrimary,
                checkmarkColor = Color.Black
            ),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (checked) Color.White else WhiteOpacity70,
            fontSize = 12.sp,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal
        )
    }
}
