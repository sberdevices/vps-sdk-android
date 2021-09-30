package com.arvrlab.vps_sdk

import android.app.Application
import com.arvrlab.vps_sdk.di.Module
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

internal class App : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)
            modules(
                Module.repository,
                Module.domain,
                Module.presentation
            )
        }
    }
}