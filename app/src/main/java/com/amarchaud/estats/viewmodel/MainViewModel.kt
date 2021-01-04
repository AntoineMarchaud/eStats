package com.amarchaud.estats.viewmodel

import android.app.Application
import android.content.Intent
import com.amarchaud.estats.base.BaseViewModel
import com.amarchaud.estats.service.PositionService

class MainViewModel(val app: Application) : BaseViewModel(app) {
    init {
        // start service if needed
        Intent(app, PositionService::class.java).also { intent ->
            app.startService(intent)
        }
    }
}