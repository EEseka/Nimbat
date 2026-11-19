package com.eeseka.nimbat

import android.app.Application
import com.eeseka.nimbat.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class NimbatApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin(config = {
            androidContext(this@NimbatApplication)
            androidLogger()
        })
    }
}
