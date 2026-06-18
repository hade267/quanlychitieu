package com.example.data.update

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

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
}
