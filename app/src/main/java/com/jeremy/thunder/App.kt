package com.jeremy.thunder

import android.app.Application
import android.content.Context

class App: Application() {

    init{
        instance = this
    }

    companion object {
        var instance: App? = null
        fun context() : Context {
            return instance!!.applicationContext
        }
    }

}