package com.example.purrytify

import android.app.Application
import com.example.purrytify.network.RetrofitClient
import com.example.purrytify.util.NetworkConnectionObserver
import com.example.purrytify.util.TokenManager

class PurrytifyApp : Application() {
    lateinit var tokenManager: TokenManager
    lateinit var networkConnectionObserver: NetworkConnectionObserver

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager(this)
        RetrofitClient.initialize(this)

        // Initialize NetworkConnectionObserver
        networkConnectionObserver = NetworkConnectionObserver(this)
        networkConnectionObserver.start()
    }

    override fun onTerminate() {
        super.onTerminate()
        networkConnectionObserver.stop()
    }
}