package com.example.purrytify

import android.app.Application
import com.example.purrytify.network.RetrofitClient

class PurrytifyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.initialize(this)
    }
}