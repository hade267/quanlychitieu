package com.example.data.update

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateInfo(
    @Json(name = "versionCode") val versionCode: Int,
    @Json(name = "versionName") val versionName: String,
    @Json(name = "updateLog") val updateLog: String,
    @Json(name = "apkUrl") val apkUrl: String,
    @Json(name = "forceUpdate") val forceUpdate: Boolean = false
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data class ReadyToInstall(val apkFile: java.io.File) : UpdateState()
    object UpToDate : UpdateState()
    data class Error(val message: String) : UpdateState()
}

