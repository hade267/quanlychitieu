package com.example.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object UpdateManager {
    private const val PREFS_NAME = "app_update_prefs"
    private const val KEY_UPDATE_URL = "update_url"
    
    // Default URL pointing to a raw configuration repository schema
    const val DEFAULT_UPDATE_URL = "https://raw.githubusercontent.com/hade267/quanlychitieu/main/update.json"

    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://placeholder.api/") // Dynamic @Url overrides this
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val updateService: UpdateService = retrofit.create(UpdateService::class.java)

    fun getUpdateUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_UPDATE_URL, DEFAULT_UPDATE_URL) ?: DEFAULT_UPDATE_URL
    }

    fun saveUpdateUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_UPDATE_URL, url.trim()).apply()
    }

    suspend fun checkUpdate(context: Context): UpdateInfo {
        val url = getUpdateUrl(context)
        return updateService.checkForUpdates(url)
    }

    suspend fun downloadApk(context: Context, apkUrl: String, onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val request = okhttp3.Request.Builder().url(apkUrl).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Lỗi kết nối tải: ${response.code}")
        val body = response.body ?: throw IOException("Nội dung tải rỗng")
        val contentLength = body.contentLength()
        
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        val apkFile = File(cacheDir, "quan_ly_chi_tieu_update.apk")
        if (apkFile.exists()) {
            apkFile.delete()
        }
        
        body.byteStream().use { inputStream ->
            FileOutputStream(apkFile).use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0) {
                        val progress = ((totalBytesRead * 100) / contentLength).toInt()
                        onProgress(progress)
                    }
                }
            }
        }
        apkFile
    }

    fun installApk(context: Context, apkFile: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
