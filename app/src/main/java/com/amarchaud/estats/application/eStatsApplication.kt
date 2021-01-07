package com.amarchaud.estats.application

import android.app.Application
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class eStatsApplication  : Application() {

    override fun onCreate() {
        super.onCreate()
    }

}