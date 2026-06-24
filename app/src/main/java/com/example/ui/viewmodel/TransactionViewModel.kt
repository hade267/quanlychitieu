package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Transaction
import com.example.data.model.CategoryEntity
import com.example.data.model.ShippingOrder
import com.example.data.repository.TransactionRepository
import com.example.data.repository.ShippingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    val allTransactions: StateFlow<List<Transaction>>
    val allCategories: StateFlow<List<CategoryEntity>>

    private val shippingRepository: ShippingRepository
    val allShippingOrders: StateFlow<List<ShippingOrder>>
    
    private val seedMutex = Mutex()

    // Calendar Selection State (Selected Year and Month)
    private val _selectedYear = MutableStateFlow(2023)
    val selectedYear = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(9) // 0-indexed, 9 = October
    val selectedMonth = _selectedMonth.asStateFlow()

    // Selected day for transaction list filter
    private val _selectedDay = MutableStateFlow(12) // Default to 12
    val selectedDay = _selectedDay.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TransactionRepository(database.transactionDao(), database.categoryDao())
        
        val shippingDao = database.shippingOrderDao()
        shippingRepository = ShippingRepository(shippingDao)
        allShippingOrders = shippingRepository.allShippingOrders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        allTransactions = repository.allTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allCategories = repository.allCategories.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed default categories if empty (using first() to get actual db state, bypassing StateFlow placeholder)
        viewModelScope.launch {
            val list = repository.allCategories.first()
            if (list.isEmpty()) {
                seedDefaultCategories()
            }
        }

        // Set view date to current month/day
        val now = Calendar.getInstance()
        _selectedYear.value = now.get(Calendar.YEAR)
        _selectedMonth.value = now.get(Calendar.MONTH)
        _selectedDay.value = now.get(Calendar.DAY_OF_MONTH)
    }

    private suspend fun seedDefaultCategories() {
        seedMutex.withLock {
            val check = repository.allCategories.first()
            if (check.isNotEmpty()) return

            val defaultExpenses = listOf(
                CategoryEntity(name = "Ăn uống", emoji = "🍜", colorHex = "#F87171", type = "EXPENSE"),
                CategoryEntity(name = "Nhiên liệu", emoji = "⛽", colorHex = "#FB923C", type = "EXPENSE"),
                CategoryEntity(name = "Mua sắm", emoji = "🛍️", colorHex = "#FBBF24", type = "EXPENSE"),
                CategoryEntity(name = "Giải trí", emoji = "🎮", colorHex = "#34D399", type = "EXPENSE"),
                CategoryEntity(name = "Di chuyển", emoji = "🚌", colorHex = "#60A5FA", type = "EXPENSE"),
                CategoryEntity(name = "Khác", emoji = "📦", colorHex = "#A78BFA", type = "EXPENSE")
            )
            val defaultIncomes = listOf(
                CategoryEntity(name = "Lương", emoji = "💼", colorHex = "#34D399", type = "INCOME"),
                CategoryEntity(name = "Thu nhập phụ", emoji = "💵", colorHex = "#60A5FA", type = "INCOME"),
                CategoryEntity(name = "Thu nhập giao hàng", emoji = "🏍️", colorHex = "#FF9E2B", type = "INCOME"),
                CategoryEntity(name = "Quà tặng", emoji = "🎁", colorHex = "#F472B6", type = "INCOME"),
                CategoryEntity(name = "Khác", emoji = "🌀", colorHex = "#A78BFA", type = "INCOME")
            )
            for (cat in defaultExpenses + defaultIncomes) {
                repository.insertCategory(cat)
            }
        }
    }

    private fun createTimeInMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month, day, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun selectMonth(year: Int, month: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
        // Default to day 1, or today if same month
        val cal = Calendar.getInstance()
        if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month) {
            _selectedDay.value = cal.get(Calendar.DAY_OF_MONTH)
        } else {
            _selectedDay.value = 1
        }
    }

    fun selectDay(day: Int) {
        _selectedDay.value = day
    }

    fun insertTransaction(title: String, amount: Double, dateMillis: Long, category: String, type: String, note: String = "") {
        viewModelScope.launch {
            val finalTitle = title.trim().ifEmpty { category }
            repository.insert(Transaction(
                title = finalTitle,
                amount = amount,
                dateMillis = dateMillis,
                category = category,
                type = type,
                note = note
            ))
            triggerAutoSync()
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val finalTitle = transaction.title.trim().ifEmpty { transaction.category }
            repository.update(transaction.copy(title = finalTitle))
            triggerAutoSync()
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)
            triggerAutoSync()
        }
    }

    fun deleteTransactionById(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
            triggerAutoSync()
        }
    }

    // Category Operations
    fun insertCategory(name: String, emoji: String, colorHex: String, type: String) {
        viewModelScope.launch {
            repository.insertCategory(CategoryEntity(name = name, emoji = emoji, colorHex = colorHex, type = type))
            triggerAutoSync()
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.updateCategory(category)
            triggerAutoSync()
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            triggerAutoSync()
        }
    }

    fun resetDatabaseToDefault() {
        viewModelScope.launch {
            // Delete all transactions sequentially
            val txs = allTransactions.value
            for (t in txs) {
                repository.delete(t)
            }
            // Delete all categories sequentially
            val cats = allCategories.value
            for (c in cats) {
                repository.deleteCategory(c)
            }
            // Reseed default categories
            seedDefaultCategories()
            triggerAutoSync()
        }
    }

    // Navigation and Calendar helper flows
    val monthlyTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions, selectedYear, selectedMonth
    ) { txs, year, month ->
        txs.filter {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.dateMillis
            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDayTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions, selectedYear, selectedMonth, selectedDay
    ) { txs, year, month, day ->
        txs.filter {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.dateMillis
            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month && cal.get(Calendar.DAY_OF_MONTH) == day
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Update Checking States for In-App Updates
    private val _updateState = MutableStateFlow<com.example.data.update.UpdateState>(com.example.data.update.UpdateState.Idle)
    val updateState: StateFlow<com.example.data.update.UpdateState> = _updateState.asStateFlow()

    fun checkForUpdatesOnLaunch(context: android.content.Context) {
        viewModelScope.launch {
            try {
                val info = com.example.data.update.UpdateManager.checkUpdate(context)
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    pInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    pInfo.versionCode
                }

                if (info.versionCode > currentVersionCode) {
                    _updateState.value = com.example.data.update.UpdateState.UpdateAvailable(info)
                }
            } catch (e: Exception) {
                // Ignore silent update errors on app launch
            }
        }
    }

    fun checkForUpdates(context: android.content.Context) {
        viewModelScope.launch {
            _updateState.value = com.example.data.update.UpdateState.Checking
            try {
                val info = com.example.data.update.UpdateManager.checkUpdate(context)
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    pInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    pInfo.versionCode
                }

                if (info.versionCode > currentVersionCode) {
                    _updateState.value = com.example.data.update.UpdateState.UpdateAvailable(info)
                } else {
                    _updateState.value = com.example.data.update.UpdateState.UpToDate
                }
            } catch (e: Exception) {
                _updateState.value = com.example.data.update.UpdateState.Error(
                    e.localizedMessage ?: "Không thể kết nối đến máy chủ cập nhật."
                )
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = com.example.data.update.UpdateState.Idle
    }

    fun downloadAndInstallUpdate(context: android.content.Context, apkUrl: String) {
        viewModelScope.launch {
            try {
                _updateState.value = com.example.data.update.UpdateState.Downloading(0)
                val apkFile = com.example.data.update.UpdateManager.downloadApk(context, apkUrl) { progress ->
                    _updateState.value = com.example.data.update.UpdateState.Downloading(progress)
                }
                _updateState.value = com.example.data.update.UpdateState.ReadyToInstall(apkFile)
                com.example.data.update.UpdateManager.installApk(context, apkFile)
            } catch (e: java.lang.Exception) {
                _updateState.value = com.example.data.update.UpdateState.Error(
                    "Lỗi khi tải bản cập nhật: ${e.localizedMessage ?: "Lỗi không xác định"}"
                )
            }
        }
    }

    fun insertShippingOrder(order: ShippingOrder) {
        viewModelScope.launch {
            val insertedId = shippingRepository.insert(order)
            val updatedOrder = order.copy(id = insertedId.toInt())
            syncShippingOrderTransaction(updatedOrder)
            triggerAutoSync()
        }
    }

    fun updateShippingOrder(order: ShippingOrder) {
        viewModelScope.launch {
            shippingRepository.update(order)
            syncShippingOrderTransaction(order)
            triggerAutoSync()
        }
    }

    fun deleteShippingOrder(order: ShippingOrder) {
        viewModelScope.launch {
            shippingRepository.delete(order)
            removeShippingOrderTransaction(order.id)
            triggerAutoSync()
        }
    }

    private suspend fun syncShippingOrderTransaction(order: ShippingOrder) {
        val txs = repository.allTransactions.first()
        val matchingTx = txs.find { it.note == "SHIPPING_ORDER_${order.id}" }
        
        if (order.status == "DA_GIAO") {
            val shortAddress = if (order.address.length > 25) {
                order.address.take(22) + "..."
            } else {
                order.address
            }
            if (matchingTx == null) {
                repository.insert(Transaction(
                    title = "Phí ship: $shortAddress",
                    amount = order.shippingFee,
                    dateMillis = order.timestamp,
                    category = "Thu nhập giao hàng",
                    type = "INCOME",
                    note = "SHIPPING_ORDER_${order.id}"
                ))
            } else {
                repository.update(matchingTx.copy(
                    title = "Phí ship: $shortAddress",
                    amount = order.shippingFee,
                    dateMillis = order.timestamp
                ))
            }
        } else {
            if (matchingTx != null) {
                repository.delete(matchingTx)
            }
        }
    }

    private suspend fun removeShippingOrderTransaction(orderId: Int) {
        val txs = repository.allTransactions.first()
        val matchingTx = txs.find { it.note == "SHIPPING_ORDER_${orderId}" }
        if (matchingTx != null) {
            repository.delete(matchingTx)
        }
    }

    private fun triggerAutoSync() {
        com.example.data.firebase.FirebaseManager.autoSyncToCloud(
            context = getApplication(),
            transactionRepository = repository,
            shippingRepository = shippingRepository
        )
    }
}

class TransactionViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
