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

                    actions = {
                        IconButton(onClick = { selectedTab = 2 }) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Tài khoản",
                                tint = if (selectedTab == 2) DeepOrangePrimary else WhiteOpacity70
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
                    2 -> ProfileTab(viewModel = viewModel)
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
                onDismiss = { showAddDialog = false },
                onCreateCategory = { name, emoji, colorHex, type ->
                    viewModel.insertCategory(name, emoji, colorHex, type)
                },
                onSave = { title, amount, isIncome, category, note ->
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
                }
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
            .padding(horizontal = 16.dp)
            .fillMaxWidth(0.92f)
            .height(72.dp)
            .background(
                color = Color(0xF2101319), // High transclucent slate backer
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = GlassBorder,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 8.dp),
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

        // Add Floating Action Overlay Trigger
        Box(
            modifier = Modifier
                .size(56.dp)
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
                modifier = Modifier.size(36.dp)
            )
        }

        // Tab 1: Thống kê / Stats
        DockItem(
            isSelected = selectedTab == 1,
            icon = Icons.Default.Star,
            label = "Báo cáo",
            onClick = { onTabSelected(1) }
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
