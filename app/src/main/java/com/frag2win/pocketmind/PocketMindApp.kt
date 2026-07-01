package com.frag2win.pocketmind

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PocketMindApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
