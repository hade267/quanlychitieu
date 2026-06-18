package com.example.data.update

import retrofit2.http.GET
import retrofit2.http.Url

interface UpdateService {
    @GET
    suspend fun checkForUpdates(@Url url: String): UpdateInfo
}
