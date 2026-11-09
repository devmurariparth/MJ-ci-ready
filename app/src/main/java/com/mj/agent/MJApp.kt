package com.mj.agent

import android.app.Application

class MJApp : Application() {
    companion object {
        lateinit var instance: MJApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Speaker.init(this)
        MJBrain.init(this)
    }
}
