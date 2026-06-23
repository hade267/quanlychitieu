package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.ShippingOrder
import com.example.data.model.Transaction
import com.example.data.repository.ShippingRepository
import com.example.data.repository.TransactionRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Manages Firebase configuration, authentication, and data synchronization.
 * Supports manual configuration as well as env-injected options via BuildConfig.
 */
object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private const val PREFS_NAME = "firebase_configs_prefs"
    
    // Config Keys
    private const val KEY_API_KEY = "api_key"
    private const val KEY_PROJECT_ID = "project_id"
    private const val KEY_APP_ID = "app_id"
    private const val KEY_DB_URL = "db_url"
    
    // Observables
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    
    private val _syncMessage = MutableStateFlow("")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    private var firebaseApp: FirebaseApp? = null
    var auth: FirebaseAuth? = null
        private set
    var rtdb: FirebaseDatabase? = null
        private set

    /**
     * Tries to initialize Firebase automatically using google-services.json configuration,
     * with an options-based fallback on user-configured properties or BuildConfig variables.
     */
    fun initialize(context: Context) {
        if (_isInitialized.value) return

        try {
            Log.d(TAG, "Attempting standard Firebase initialization from google-services.json...")
            // When google-services.json is processed, FirebaseApp can be initialized automatically
            val app = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(context)
            
            if (app == null) {
                _isInitialized.value = false
                return
            }
            
            firebaseApp = app
            auth = FirebaseAuth.getInstance(app)
            rtdb = FirebaseDatabase.getInstance(app)
            
            // Enable offline data persistence for RTDB
            try {
                rtdb?.setPersistenceEnabled(true)
            } catch (e: Exception) {
                Log.d(TAG, "Persistence already enabled or failed to enable: ${e.message}")
            }

            _isInitialized.value = true
            Log.d(TAG, "Firebase successfully initialized using google-services.json.")
        } catch (e: Exception) {
            Log.e(TAG, "Standard Firebase initialization failed: ${e.message}. Trying manual option fallback...", e)
            try {
                val config = getEffectiveConfig(context)
                if (config.apiKey.isNotEmpty() && config.projectId.isNotEmpty() && config.appId.isNotEmpty()) {
                    val optionsBuilder = FirebaseOptions.Builder()
                        .setApiKey(config.apiKey)
                        .setProjectId(config.projectId)
                        .setApplicationId(config.appId)

                    if (config.databaseUrl.isNotEmpty()) {
                        optionsBuilder.setDatabaseUrl(config.databaseUrl)
                    }

                    val options = optionsBuilder.build()
                    val app = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(context, options)
                    
                    if (app == null) {
                        _isInitialized.value = false
                        return
                    }
                    
                    firebaseApp = app
                    auth = FirebaseAuth.getInstance(app)
                    rtdb = if (config.databaseUrl.isNotEmpty()) {
                        FirebaseDatabase.getInstance(app, config.databaseUrl)
                    } else {
                        FirebaseDatabase.getInstance(app)
                    }

                    try {
                        rtdb?.setPersistenceEnabled(true)
                    } catch (persErr: Exception) {
                        Log.d(TAG, "Persistence options enable: ${persErr.message}")
                    }

                    _isInitialized.value = true
                    Log.d(TAG, "Firebase successfully initialized with manual options fallback.")
                } else {
                    _isInitialized.value = false
                }
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Fallback initialization also failed", fallbackEx)
                _isInitialized.value = false
            }
        }
    }

    /**
     * Retrieve the effective configuration from user-entered preferences falling back on BuildConfig key definitions.
     */
    fun getEffectiveConfig(context: Context): FirebaseConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val spApiKey = prefs.getString(KEY_API_KEY, "") ?: ""
        val spProjectId = prefs.getString(KEY_PROJECT_ID, "") ?: ""
        val spAppId = prefs.getString(KEY_APP_ID, "") ?: ""
        val spDbUrl = prefs.getString(KEY_DB_URL, "") ?: ""

        // Find standard generated BuildConfig variable if exists, or fallback
        val buildApiKey = try {
            val field = com.example.BuildConfig::class.java.getField("FIREBASE_API_KEY")
            field.get(null) as? String ?: ""
        } catch (e: Exception) { "" }

        val buildProjectId = try {
            val field = com.example.BuildConfig::class.java.getField("FIREBASE_PROJECT_ID")
            field.get(null) as? String ?: ""
        } catch (e: Exception) { "" }

        val buildAppId = try {
            val field = com.example.BuildConfig::class.java.getField("FIREBASE_APP_ID")
            field.get(null) as? String ?: ""
        } catch (e: Exception) { "" }

        val buildDbUrl = try {
            val field = com.example.BuildConfig::class.java.getField("FIREBASE_DATABASE_URL")
            field.get(null) as? String ?: ""
        } catch (e: Exception) { "" }

        return FirebaseConfig(
            apiKey = spApiKey.ifEmpty { buildApiKey },
            projectId = spProjectId.ifEmpty { buildProjectId },
            appId = spAppId.ifEmpty { buildAppId },
            databaseUrl = spDbUrl.ifEmpty { buildDbUrl }
        )
    }

    /**
     * Push entire local SQLite Database to the Cloud
     */
    fun uploadDataToCloud(
        context: Context,
        transactionRepository: TransactionRepository,
        shippingRepository: ShippingRepository,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUser = auth?.currentUser
        val database = rtdb
        if (currentUser == null || database == null) {
            onFailure("Vui lòng đăng nhập trước khi đồng bộ.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            _isSyncing.value = true
            _syncMessage.value = "Đang tải dữ liệu lên Cloud..."
            try {
                val transactionsList = transactionRepository.allTransactions.first()
                val categoriesList = transactionRepository.allCategories.first()
                val shippingList = shippingRepository.allShippingOrders.first()

                val userId = currentUser.uid
                val userRef = database.getReference("users").child(userId)

                // Map clean structures with string keys for database compatibility
                val dataToSync = hashMapOf<String, Any>(
                    "transactions" to transactionsList.map { 
                        mapOf(
                            "id" to it.id,
                            "title" to it.title,
                            "amount" to it.amount,
                            "dateMillis" to it.dateMillis,
                            "category" to it.category,
                            "type" to it.type,
                            "note" to it.note
                        )
                    },
                    "categories" to categoriesList.map {
                        mapOf(
                            "id" to it.id,
                            "name" to it.name,
                            "emoji" to it.emoji,
                            "colorHex" to it.colorHex,
                            "type" to it.type
                        )
                    },
                    "shipping_orders" to shippingList.map {
                        mapOf(
                            "id" to it.id,
                            "address" to it.address,
                            "phoneNumber" to it.phoneNumber,
                            "orderAmount" to it.orderAmount,
                            "distance" to it.distance,
                            "shippingFee" to it.shippingFee,
                            "status" to it.status,
                            "timestamp" to it.timestamp,
                            "note" to it.note,
                            "surchargeNightSummer" to it.surchargeNightSummer,
                            "surchargeNightWinter" to it.surchargeNightWinter,
                            "surchargeHeavyRain" to it.surchargeHeavyRain,
                            "surchargeCake" to it.surchargeCake,
                            "surchargeDoorToDoor" to it.surchargeDoorToDoor,
                            "surchargeBuyOnBehalf" to it.surchargeBuyOnBehalf,
                            "surchargeBusStation" to it.surchargeBusStation,
                            "weightGroup" to it.weightGroup,
                            "customerPrepaid" to it.customerPrepaid,
                            "shopPaysShipping" to it.shopPaysShipping
                        )
                    },
                    "last_synced_millis" to System.currentTimeMillis()
                )

                userRef.setValue(dataToSync).await()

                withContext(Dispatchers.Main) {
                    _isSyncing.value = false
                    _lastSyncTime.value = "Hôm nay, " + java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error pushing data to Cloud: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _isSyncing.value = false
                    onFailure("Lỗi tải lên: ${e.message}")
                }
            }
        }
    }

    /**
     * Download Cloud Data and Overwrite the local SQLite tables
     */
    fun downloadDataFromCloud(
        context: Context,
        db: AppDatabase,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUser = auth?.currentUser
        val database = rtdb
        if (currentUser == null || database == null) {
            onFailure("Vui lòng đăng nhập trước khi đồng bộ.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            _isSyncing.value = true
            _syncMessage.value = "Đang tải dữ liệu từ Cloud..."
            try {
                val userId = currentUser.uid
                val userRef = database.getReference("users").child(userId)
                val snapshot = userRef.get().await()

                if (!snapshot.exists()) {
                    withContext(Dispatchers.Main) {
                        _isSyncing.value = false
                        onFailure("Không tìm thấy dữ liệu sao lưu trên Cloud.")
                    }
                    return@launch
                }

                val transactionsSnap = snapshot.child("transactions")
                val categoriesSnap = snapshot.child("categories")
                val shippingSnap = snapshot.child("shipping_orders")

                // Map back into objects securely
                val transactionsToInsert = mutableListOf<Transaction>()
                transactionsSnap.children.forEach { snap ->
                    try {
                        val id = snap.child("id").getValue(Int::class.java) ?: 0
                        val title = snap.child("title").getValue(String::class.java) ?: ""
                        val amount = snap.child("amount").getValue(Double::class.java) ?: 0.0
                        val dateMillis = snap.child("dateMillis").getValue(Long::class.java) ?: System.currentTimeMillis()
                        val category = snap.child("category").getValue(String::class.java) ?: "Khác"
                        val type = snap.child("type").getValue(String::class.java) ?: "EXPENSE"
                        val note = snap.child("note").getValue(String::class.java) ?: ""
                        
                        transactionsToInsert.add(
                            Transaction(
                                id = id,
                                title = title,
                                amount = amount,
                                dateMillis = dateMillis,
                                category = category,
                                type = type,
                                note = note
                            )
                        )
                    } catch (e: Exception) { Log.e(TAG, "Failed to parse Transaction: ${e.message}") }
                }

                val categoriesToInsert = mutableListOf<CategoryEntity>()
                categoriesSnap.children.forEach { snap ->
                    try {
                        val id = snap.child("id").getValue(Int::class.java) ?: 0
                        val name = snap.child("name").getValue(String::class.java) ?: ""
                        val emoji = snap.child("emoji").getValue(String::class.java) ?: snap.child("icon").getValue(String::class.java) ?: "❓"
                        val colorHex = snap.child("colorHex").getValue(String::class.java) ?: "#F87171"
                        val type = snap.child("type").getValue(String::class.java) ?: "EXPENSE"
                        categoriesToInsert.add(
                            CategoryEntity(id = id, name = name, emoji = emoji, colorHex = colorHex, type = type)
                        )
                    } catch (e: Exception) { Log.e(TAG, "Failed to parse Category: ${e.message}") }
                }

                val shippingToInsert = mutableListOf<ShippingOrder>()
                shippingSnap.children.forEach { snap ->
                    try {
                        val id = snap.child("id").getValue(Int::class.java) ?: 0
                        val address = snap.child("address").getValue(String::class.java) ?: snap.child("shopName").getValue(String::class.java) ?: ""
                        val phoneNumber = snap.child("phoneNumber").getValue(String::class.java) ?: ""
                        val orderAmount = snap.child("orderAmount").getValue(Double::class.java) ?: 0.0
                        val distance = snap.child("distance").getValue(Double::class.java) ?: 0.0
                        val shippingFee = snap.child("shippingFee").getValue(Double::class.java) ?: 0.0
                        val status = snap.child("status").getValue(String::class.java) ?: "DA_GIAO"
                        val timestamp = snap.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                        val note = snap.child("note").getValue(String::class.java) ?: ""
                        
                        val surchargeNightSummer = snap.child("surchargeNightSummer").getValue(Boolean::class.java) ?: false
                        val surchargeNightWinter = snap.child("surchargeNightWinter").getValue(Boolean::class.java) ?: false
                        val surchargeHeavyRain = snap.child("surchargeHeavyRain").getValue(Boolean::class.java) ?: false
                        val surchargeCake = snap.child("surchargeCake").getValue(Boolean::class.java) ?: false
                        val surchargeDoorToDoor = snap.child("surchargeDoorToDoor").getValue(Boolean::class.java) ?: false
                        val surchargeBuyOnBehalf = snap.child("surchargeBuyOnBehalf").getValue(Boolean::class.java) ?: false
                        val surchargeBusStation = snap.child("surchargeBusStation").getValue(Boolean::class.java) ?: false
                        val weightGroup = snap.child("weightGroup").getValue(Int::class.java) ?: 0
                        
                        val customerPrepaid = snap.child("customerPrepaid").getValue(Boolean::class.java) ?: false
                        val shopPaysShipping = snap.child("shopPaysShipping").getValue(Boolean::class.java) ?: false

                        shippingToInsert.add(
                            ShippingOrder(
                                id = id,
                                address = address,
                                phoneNumber = phoneNumber,
                                orderAmount = orderAmount,
                                distance = distance,
                                shippingFee = shippingFee,
                                status = status,
                                timestamp = timestamp,
                                note = note,
                                surchargeNightSummer = surchargeNightSummer,
                                surchargeNightWinter = surchargeNightWinter,
                                surchargeHeavyRain = surchargeHeavyRain,
                                surchargeCake = surchargeCake,
                                surchargeDoorToDoor = surchargeDoorToDoor,
                                surchargeBuyOnBehalf = surchargeBuyOnBehalf,
                                surchargeBusStation = surchargeBusStation,
                                weightGroup = weightGroup,
                                customerPrepaid = customerPrepaid,
                                shopPaysShipping = shopPaysShipping
                            )
                        )
                    } catch (e: Exception) { Log.e(TAG, "Failed to parse ShippingOrder: ${e.message}") }
                }

                // SQLite write synchronously on Dispatchers.IO context
                withContext(Dispatchers.IO) {
                    try {
                        db.transactionDao().clearAllTransactions()
                        db.categoryDao().clearAllCategories()
                        db.shippingOrderDao().clearAllShippingOrders()

                        // Insert collections back
                        transactionsToInsert.forEach { db.transactionDao().insertTransaction(it) }
                        categoriesToInsert.forEach { db.categoryDao().insertCategory(it) }
                        shippingToInsert.forEach { db.shippingOrderDao().insertShippingOrder(it) }
                        
                        Log.d(TAG, "Successfully wiped local database and restored from Cloud backup.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Database operation failed: ${e.message}", e)
                        throw e
                    }
                }

                withContext(Dispatchers.Main) {
                    _isSyncing.value = false
                    _lastSyncTime.value = "Hôm nay, " + java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching data from Cloud: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _isSyncing.value = false
                    onFailure("Lỗi tải xuống: ${e.message}")
                }
            }
        }
    }
}

data class FirebaseConfig(
    val apiKey: String,
    val projectId: String,
    val appId: String,
    val databaseUrl: String
)
