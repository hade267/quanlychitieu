package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.TransactionViewModel
import java.text.DecimalFormat
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.clickable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CategoryEntity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.update.UpdateState
import com.example.data.update.UpdateManager

@Composable
fun ProfileTab(viewModel: TransactionViewModel) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("velocity_ledger_prefs", Context.MODE_PRIVATE) }
    val ledgerName = sharedPrefs.getString("ledger_name", "Quản lý chi tiêu") ?: "Quản lý chi tiêu"

    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    
    var showResetConfirm by remember { mutableStateOf(false) }
    var showCategoryManager by remember { mutableStateOf(false) }

    // Computations
    val totalRecords = allTransactions.size
    val maxIncome = remember(allTransactions) {
        allTransactions.filter { it.type == "INCOME" }.maxOfOrNull { it.amount } ?: 0.0
    }
    val maxExpense = remember(allTransactions) {
        allTransactions.filter { it.type == "EXPENSE" }.maxOfOrNull { it.amount } ?: 0.0
    }
    val totalValueSaved = remember(allTransactions) {
        allTransactions.filter { it.type == "INCOME" }.sumOf { it.amount } - allTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(text = "Phục hồi cấu hình gốc?", color = Color.White) },
            text = { 
                Text(
                    text = "Hành động này sẽ xóa tất cả giao dịch hiện tại của bạn và khôi phục cấu hình danh mục mặc định ban đầu. Bạn vẫn muốn tiếp tục?",
                    color = WhiteOpacity70
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        viewModel.resetDatabaseToDefault()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                ) {
                    Text(text = "ĐỒNG Ý XÓA", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirm = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text(text = "HỦY")
                }
            },
            containerColor = SolidCardBg,
            textContentColor = Color.White
        )
    }

    if (showCategoryManager) {
        CategoryManagerDialog(
            categories = allCategories,
            onDismiss = { showCategoryManager = false },
            onAdd = { name, emoji, colorHex, type ->
                viewModel.insertCategory(name, emoji, colorHex, type)
            },
            onUpdate = { category ->
                viewModel.updateCategory(category)
            },
            onDelete = { category ->
                viewModel.deleteCategory(category)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 120.dp, top = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mock Avatar and Title Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = GlassCardBg),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile avatar with glowing orange border
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(SoftOrangeContainer, CircleShape)
                        .border(1.5.dp, DeepOrangePrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🏍️", fontSize = 40.sp) // Speed Delivery rider avatar!
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = ledgerName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Chuyên Gia Quản Lý Chi Tiêu",
                    color = OrangeHighlight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "make by nn_hade",
                    color = WhiteOpacity50,
                    fontSize = 12.sp
                )
            }
        }

        // Metrics Grid Bento Style
        Text(
            text = "THỐNG KÊ LỊCH SỬ SỬ DỤNG",
            color = WhiteOpacity50,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )

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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "📊", fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "TỔNG GIAO DỊCH",
                        color = WhiteOpacity50,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$totalRecords",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🪙", fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SỐ TIỀN TÍCH LŨY",
                        color = WhiteOpacity50,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatFullAmount(totalValueSaved),
                        color = if (totalValueSaved >= 0) SuccessGreen else ErrorRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    )
                }
            }
        }

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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "💰", fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "THU NHẬP LỚN NHẤT",
                        color = WhiteOpacity50,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatFullAmount(maxIncome),
                        color = SuccessGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "💸", fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "KHOẢN CHI LỚN NHẤT",
                        color = WhiteOpacity50,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatFullAmount(maxExpense),
                        color = ErrorRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Category Configuration Tool Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = GlassCardBg),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DANH MỤC GIAO DỊCH",
                    color = WhiteOpacity50,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showCategoryManager = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftOrangeContainer,
                        contentColor = OrangeHighlight
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Quản lý danh mục (Thêm, Xóa, Sửa) 🛠️",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Action Settings
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = GlassCardBg),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "BẢO TRÌ HỆ THỐNG",
                    color = WhiteOpacity50,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showResetConfirm = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x1ABA1A1A),
                        contentColor = ErrorRed
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Xóa toàn bộ giao dịch hiện tại",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- IN-APP UPDATE FOR NON-PLAY STORE APPS ---
        val updateState by viewModel.updateState.collectAsStateWithLifecycle()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = GlassCardBg),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CẬP NHẬT ỨNG DỤNG",
                    color = WhiteOpacity50,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Phiên bản hiện tại: 1.0 (Build #1)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { viewModel.checkForUpdates(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftOrangeContainer,
                        contentColor = OrangeHighlight
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Kiểm tra cập nhật 🚀",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

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
                text = { Text("Bạn đang sử dụng phiên bản Quản lý chi tiêu mới nhất (v1.0). Không cần cập nhật thêm vào lúc này.", color = WhiteOpacity70) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onAdd: (name: String, emoji: String, colorHex: String, type: String) -> Unit,
    onUpdate: (CategoryEntity) -> Unit,
    onDelete: (CategoryEntity) -> Unit
) {
    var isExpenseTab by remember { mutableStateOf(true) }
    
    // Form fields
    var nameField by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🍜") }
    var selectedColorHex by remember { mutableStateOf("#F87171") }
    
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    val filteredCategories = remember(categories, isExpenseTab) {
        categories
            .filter { if (isExpenseTab) it.type == "EXPENSE" else it.type == "INCOME" }
            .distinctBy { it.name.trim().lowercase() }
    }

    val presetColors = listOf(
        "#F87171", "#FB923C", "#FBBF24", "#34D399",
        "#60A5FA", "#A78BFA", "#F472B6", "#2DD4BF"
    )

    val presetEmojisByTab = if (isExpenseTab) {
        listOf("🍜", "☕", "🛍️", "🛒", "🏠", "⛽", "🚌", "🎮", "🍿", "📦", "📚", "⚙️")
    } else {
        listOf("💼", "💵", "💰", "💳", "📈", "🎁", "🏍️", "🌀", "❤️", "⚽", "✈️", "🌟")
    }

    // When editing state changes
    LaunchedEffect(editingCategory) {
        if (editingCategory != null) {
            nameField = editingCategory!!.name
            selectedEmoji = editingCategory!!.emoji
            selectedColorHex = editingCategory!!.colorHex
        } else {
            nameField = ""
            selectedEmoji = if (isExpenseTab) "🍜" else "💼"
            selectedColorHex = "#F87171"
        }
    }

    // Reset emoji when tab changes
    LaunchedEffect(isExpenseTab) {
        if (editingCategory == null) {
            selectedEmoji = if (isExpenseTab) "🍜" else "💼"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6080A0F))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color(0x99FF8A00), Color(0x1AFF8A00))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141720))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Danh Mục Cá Nhân",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Column wrapper to support scrollable content but keep split layout beautiful
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Segmented Control Chi phi/Thu nhap
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
                                    .background(if (isExpenseTab) Color(0xFFBA1A1A) else Color.Transparent)
                                    .clickable { 
                                        isExpenseTab = true 
                                        editingCategory = null
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "CHI PHÍ",
                                    color = if (isExpenseTab) Color.White else WhiteOpacity70,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (!isExpenseTab) Color(0xFF0058BC) else Color.Transparent)
                                    .clickable { 
                                        isExpenseTab = false 
                                        editingCategory = null
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "THU NHẬP",
                                    color = if (!isExpenseTab) Color.White else WhiteOpacity70,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Category Form Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0E12)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (editingCategory == null) "THÊM DANH MỤC MỚI" else "SỬA DANH MỤC",
                                    color = OrangeHighlight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Name Input
                                OutlinedTextField(
                                    value = nameField,
                                    onValueChange = { nameField = it },
                                    placeholder = { Text("Tên danh mục...", color = WhiteOpacity50) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DeepOrangePrimary,
                                        unfocusedBorderColor = WhiteOpacity10,
                                        focusedContainerColor = Color(0xFF141720),
                                        unfocusedContainerColor = Color(0xFF141720),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Emojis grid
                                Text("BIỂU TƯỢNG (EMOJI)", color = WhiteOpacity50, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    presetEmojisByTab.take(6).forEach { emoji ->
                                        val isEmojiSelected = selectedEmoji == emoji
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isEmojiSelected) Color(0x33FF8A00) else Color.Transparent)
                                                .border(1.dp, if (isEmojiSelected) DeepOrangePrimary else Color.Transparent, RoundedCornerShape(8.dp))
                                                .clickable { selectedEmoji = emoji },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(emoji, fontSize = 18.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    presetEmojisByTab.drop(6).take(6).forEach { emoji ->
                                        val isEmojiSelected = selectedEmoji == emoji
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isEmojiSelected) Color(0x33FF8A00) else Color.Transparent)
                                                .border(1.dp, if (isEmojiSelected) DeepOrangePrimary else Color.Transparent, RoundedCornerShape(8.dp))
                                                .clickable { selectedEmoji = emoji },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(emoji, fontSize = 18.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Colors row
                                Text("MÀU SẮC CHỦ ĐẠO", color = WhiteOpacity50, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    presetColors.forEach { hex ->
                                        val isColorSelected = selectedColorHex.equals(hex, ignoreCase = true)
                                        val parsedColor = Color(android.graphics.Color.parseColor(hex))
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(parsedColor)
                                                .border(
                                                    width = 2.dp,
                                                    color = if (isColorSelected) Color.White else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable { selectedColorHex = hex }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (editingCategory != null) {
                                        Button(
                                            onClick = { editingCategory = null },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("HỦY", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (nameField.isNotBlank()) {
                                                if (editingCategory == null) {
                                                    onAdd(nameField.trim(), selectedEmoji, selectedColorHex, if (isExpenseTab) "EXPENSE" else "INCOME")
                                                } else {
                                                    onUpdate(editingCategory!!.copy(
                                                        name = nameField.trim(),
                                                        emoji = selectedEmoji,
                                                        colorHex = selectedColorHex
                                                    ))
                                                }
                                                // Clear form
                                                nameField = ""
                                                editingCategory = null
                                            }
                                        },
                                        enabled = nameField.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = DeepOrangePrimary, contentColor = Color.Black),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = if (editingCategory == null) "THÊM" else "CẬP NHẬT",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Category List Header
                        Text(
                            text = "DANH SÁCH DANH MỤC HIỆN TẠI",
                            color = WhiteOpacity50,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // List of categories
                        if (filteredCategories.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Chưa có danh mục nào", color = WhiteOpacity50, fontSize = 13.sp)
                            }
                        } else {
                            filteredCategories.forEach { cat ->
                                val catColor = Color(android.graphics.Color.parseColor(cat.colorHex))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .border(1.dp, GlassBorder.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101319)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(catColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                    .border(1.dp, catColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = cat.emoji, fontSize = 16.sp)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(text = cat.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            // Edit Button
                                            IconButton(
                                                onClick = { editingCategory = cat },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Sửa",
                                                    tint = OrangeHighlight,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            // Delete Button (Disable if the list gets too small to ensure at least one remains, or just allow deletion)
                                            IconButton(
                                                onClick = { onDelete(cat) },
                                                modifier = Modifier.size(32.dp),
                                                enabled = categories.size > 1
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Xóa",
                                                    tint = ErrorRed,
                                                    modifier = Modifier.size(16.dp)
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
}

private fun formatFullAmount(amount: Double): String {
    val df = DecimalFormat("#,###")
    val formatted = df.format(amount).replace(',', '.')
    return "${formatted}đ"
}
