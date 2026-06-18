package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TransactionViewModel
import com.example.ui.viewmodel.TransactionViewModelFactory
import com.example.data.model.ShippingOrder
import com.example.data.update.UpdateState
import com.example.data.update.UpdateManager
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import java.util.Calendar

import androidx.compose.ui.platform.LocalContext
import android.content.Context

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val app = application
                val factory = remember { TransactionViewModelFactory(app) }
                val txViewModel: TransactionViewModel = viewModel(factory = factory)
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SpaceSlateDark
                ) {
                    VelocityLedgerApp(viewModel = txViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VelocityLedgerApp(viewModel: TransactionViewModel) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Calendar, 1 = Stats, 2 = Profile
    var showAddDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("velocity_ledger_prefs", Context.MODE_PRIVATE) }
    
    var ledgerName by remember { 
        mutableStateOf(sharedPrefs.getString("ledger_name", "Velocity Ledger") ?: "Velocity Ledger") 
    }
    var showWelcomeDialog by remember { 
        mutableStateOf(sharedPrefs.getBoolean("is_first_launch", true)) 
    }
    var showEditDialog by remember { mutableStateOf(false) }

    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val allShippingOrders by viewModel.allShippingOrders.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.checkForUpdatesOnLaunch(context)
    }

    if (showWelcomeDialog) {
        var tempName by remember { mutableStateOf("Velocity Ledger") }
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = "Welcome to your Ledger! 🎯",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Vui lòng đặt tên cho sổ ghi chép tài chính của bạn để cá nhân hóa tài chính cá nhân ngay từ lần đầu truy cập:",
                        color = WhiteOpacity70,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Tên sổ ghi chép", color = DeepOrangePrimary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepOrangePrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalName = tempName.trim().ifEmpty { "Velocity Ledger" }
                        ledgerName = finalName
                        sharedPrefs.edit()
                            .putString("ledger_name", finalName)
                            .putBoolean("is_first_launch", false)
                            .apply()
                        showWelcomeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepOrangePrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Bắt đầu trải nghiệm ✨", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showEditDialog) {
        var tempName by remember { mutableStateOf(ledgerName) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Đổi tên sổ ghi chép ✏️",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Tên mới", color = DeepOrangePrimary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepOrangePrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalName = tempName.trim().ifEmpty { "Velocity Ledger" }
                        ledgerName = finalName
                        sharedPrefs.edit().putString("ledger_name", finalName).apply()
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepOrangePrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Lưu thay đổi", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceSlateDark)
    ) {
        // Mesh background blurs (glowing ambient orange lights simulated via gradients)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehindMeshBackground()
        )

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showEditDialog = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = ledgerName,
                                color = DeepOrangePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "✏️",
                                fontSize = 12.sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Tab Selection Logic
                when (selectedTab) {
                    0 -> DashboardTab(
                        viewModel = viewModel,
                        onShowAddDialog = { showAddDialog = true }
                    )
                    1 -> StatsTab(viewModel = viewModel)
                    2 -> ShippingTab(viewModel = viewModel)
                    3 -> ProfileTab(viewModel = viewModel)
                }

                // Floating bottom glassmorphic dock menu overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding() // Protect against device navigation handles
                        .padding(bottom = 12.dp)
                ) {
                    FloatingBottomDock(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        onAddClicked = { showAddDialog = true }
                    )
                }
            }
        }

        // Overlay dialog
        if (showAddDialog) {
            val calendar = remember(selectedYear, selectedMonth, selectedDay) {
                Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                }
            }
            AddTransactionDialog(
                initialDateMillis = calendar.timeInMillis,
                dynamicCategories = allCategories,
                shippingOrders = allShippingOrders,
                onDismiss = { showAddDialog = false },
                onCreateCategory = { name, emoji, colorHex, type ->
                    viewModel.insertCategory(name, emoji, colorHex, type)
                },
                onSave = { title, amount, isIncome, category, note, createShip, shipAddress, shipPhone, shipDistance, shipFee ->
                    showAddDialog = false
                    val sign = if (isIncome) 1.0 else -1.0
                    val finalAmount = amount * sign
                    
                    val saveCalendar = Calendar.getInstance().apply {
                        set(Calendar.YEAR, selectedYear)
                        set(Calendar.MONTH, selectedMonth)
                        set(Calendar.DAY_OF_MONTH, selectedDay)
                    }
                    
                    viewModel.insertTransaction(
                        title = title,
                        amount = amount,
                        dateMillis = saveCalendar.timeInMillis,
                        category = category,
                        type = if (isIncome) "INCOME" else "EXPENSE",
                        note = note
                    )

                    if (createShip) {
                        viewModel.insertShippingOrder(
                            ShippingOrder(
                                id = 0,
                                address = shipAddress,
                                phoneNumber = shipPhone,
                                orderAmount = amount, // Using transaction amount as COD orderAmount
                                distance = shipDistance,
                                shippingFee = shipFee,
                                status = "DANG_GIAO", // Default is "Đang giao"
                                timestamp = saveCalendar.timeInMillis,
                                note = note
                            )
                        )
                    }
                }
            )
        }

        // Global In-App Update Dialog Overlays
        // 1. UPDATE STATE: CHECKING DIALOG
        if (updateState is UpdateState.Checking) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = SolidCardBg),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = DeepOrangePrimary)
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Đang kiểm tra cập nhật...",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Vui lòng đợi giây lát",
                            color = WhiteOpacity50,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 1.5. UPDATE STATE: DOWNLOADING DIALOG WITH PROGRESS BAR
        if (updateState is UpdateState.Downloading) {
            val progress = (updateState as UpdateState.Downloading).progress
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = SolidCardBg),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = progress / 100f,
                            color = DeepOrangePrimary,
                            trackColor = WhiteOpacity20
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Đang tải bản cập nhật... $progress%",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        LinearProgressIndicator(
                            progress = progress / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = DeepOrangePrimary,
                            trackColor = WhiteOpacity10
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Vui lòng giữ ứng dụng mở",
                            color = WhiteOpacity50,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 1.8. UPDATE STATE: READY TO INSTALL DIALOG
        if (updateState is UpdateState.ReadyToInstall) {
            val apkFile = (updateState as UpdateState.ReadyToInstall).apkFile
            AlertDialog(
                onDismissRequest = { viewModel.resetUpdateState() },
                title = { Text("Tải về hoàn tất 📦", color = Color.White) },
                text = { Text("Cài đặt bản cập nhật mới đã sẵn sàng. Hãy bấm tiếp tục để cài đặt.", color = WhiteOpacity70) },
                confirmButton = {
                    Button(
                        onClick = {
                            UpdateManager.installApk(context, apkFile)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("BẮT ĐẦU CÀI ĐẶT", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.resetUpdateState() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text("HỦY")
                    }
                },
                containerColor = SolidCardBg,
                textContentColor = Color.White
            )
        }

        // 2. UPDATE STATE: UPDATE AVAILABLE DIALOG
        if (updateState is UpdateState.UpdateAvailable) {
            val info = (updateState as UpdateState.UpdateAvailable).info
            Dialog(
                onDismissRequest = { if (!info.forceUpdate) viewModel.resetUpdateState() },
                properties = DialogProperties(
                    dismissOnBackPress = !info.forceUpdate,
                    dismissOnClickOutside = !info.forceUpdate,
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC000000))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DeepOrangePrimary.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141720)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = "✨ BẢN CẬP NHẬT MỚI!",
                                color = OrangeHighlight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Text(
                                text = "Phiên bản mới: v${info.versionName}",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (info.forceUpdate) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(ErrorRed.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("BẮT BUỘC CẬP NHẬT ⚠️", color = ErrorRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "NỘI DUNG CẬP NHẬT:",
                                color = WhiteOpacity50,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF090B0F)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = info.updateLog.ifEmpty { "Không có ghi chú phiên bản nào được cung cấp." },
                                        color = WhiteOpacity70,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (!info.forceUpdate) {
                                    Button(
                                        onClick = { viewModel.resetUpdateState() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0x1BFFFFFF),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Hủy", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }

                                Button(
                                    onClick = {
                                        viewModel.downloadAndInstallUpdate(context, info.apkUrl)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DeepOrangePrimary,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Text("Tải & Cập Nhật 🚀", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. UPDATE STATE: UP TO DATE DIALOG
        if (updateState is UpdateState.UpToDate) {
            AlertDialog(
                onDismissRequest = { viewModel.resetUpdateState() },
                title = { Text("Ứng dụng mới nhất! 🎉", color = Color.White) },
                text = { Text("Bạn đang sử dụng phiên bản Quản lý chi tiêu mới nhất. Không cần cập nhật thêm vào lúc này.", color = WhiteOpacity70) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.resetUpdateState() },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("XÁC NHẬN", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = SolidCardBg,
                textContentColor = Color.White
            )
        }

        // 4. UPDATE STATE: ERROR DIALOG
        if (updateState is UpdateState.Error) {
            val errorMsg = (updateState as UpdateState.Error).message
            AlertDialog(
                onDismissRequest = { viewModel.resetUpdateState() },
                title = { Text("Kiểm Tra Cập Nhật Lỗi ⚠️", color = Color.White) },
                text = { 
                    Column {
                        Text("Không thể hoàn tất kiểm tra hoặc tải cập nhật ứng dụng. Vui lòng thử lại sau.", color = WhiteOpacity70)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Chi tiết lỗi:", color = OrangeHighlight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(errorMsg, color = ErrorRed, fontSize = 11.sp, maxLines = 3)
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { 
                                viewModel.resetUpdateState()
                                viewModel.checkForUpdates(context)
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = OrangeHighlight)
                        ) {
                            Text("THỬ LẠI", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.resetUpdateState() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("ĐÓNG")
                        }
                    }
                },
                containerColor = SolidCardBg,
                textContentColor = Color.White
            )
        }
    }
}

@Composable
fun FloatingBottomDock(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onAddClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth(0.95f)
            .height(72.dp)
            .background(
                color = Color(0xF2101319), // High translucent slate backer
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = GlassBorder,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tab 0: Lịch / Calendar
        DockItem(
            isSelected = selectedTab == 0,
            icon = Icons.Default.DateRange,
            label = "Lịch",
            onClick = { onTabSelected(0) }
        )

        // Tab 1: Thống kê / Stats
        DockItem(
            isSelected = selectedTab == 1,
            icon = Icons.Default.Star,
            label = "Báo cáo",
            onClick = { onTabSelected(1) }
        )

        // Add Floating Action Overlay Trigger
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(DeepOrangePrimary, OrangeHighlight)
                    )
                )
                .clickable { onAddClicked() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = "Thêm giao dịch",
                tint = Color.Black,
                modifier = Modifier.size(32.dp)
            )
        }

        // Tab 2: Đơn Ship / Shipping orders
        DockItem(
            isSelected = selectedTab == 2,
            icon = Icons.Default.ShoppingCart,
            label = "Đơn Ship",
            onClick = { onTabSelected(2) }
        )

        // Tab 3: Cài đặt / Cấu hình
        DockItem(
            isSelected = selectedTab == 3,
            icon = Icons.Default.AccountCircle,
            label = "Cài đặt",
            onClick = { onTabSelected(3) }
        )
    }
}

@Composable
fun RowScope.DockItem(
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) DeepOrangePrimary else WhiteOpacity50,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = if (isSelected) DeepOrangePrimary else WhiteOpacity50,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// Extension to draw custom back blurs using Canvas
fun Modifier.drawBehindMeshBackground(): Modifier = this.drawBehind {
    val centerLeft = androidx.compose.ui.geometry.Offset(-50.dp.toPx(), 100.dp.toPx())
    val bottomRight = androidx.compose.ui.geometry.Offset(size.width + 50.dp.toPx(), size.height + 50.dp.toPx())
    
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x22FF8A00), Color.Transparent),
            center = centerLeft,
            radius = size.width * 0.75f
        ),
        radius = size.width * 0.75f,
        center = centerLeft
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x33FF8A00), Color.Transparent),
            center = bottomRight,
            radius = size.width * 0.85f
        ),
        radius = size.width * 0.85f,
        center = bottomRight
    )
}
