package com.arvrlab.vps_sdk

import android.content.Context
import com.arvrlab.vps_sdk.di.Module
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

object VpsSdk {

    fun init(context: Context) {
        if (GlobalContext.getOrNull() != null) return

        startKoin {
            androidContext(context)
            modules(
                Module.repository,
                Module.domain,
                Module.presentation
            )
        }
    }

}

class VpsSdkInitializationException : RuntimeException("Must be called VpsSdk.init(context)")