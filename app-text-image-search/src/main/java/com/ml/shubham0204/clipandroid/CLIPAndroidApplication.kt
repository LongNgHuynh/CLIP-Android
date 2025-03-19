package com.ml.shubham0204.clipandroid

import android.app.Application
import com.ml.shubham0204.clipandroid.data.ObjectBoxStore

class CLIPAndroidApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ObjectBoxStore.init(this)
    }
}
