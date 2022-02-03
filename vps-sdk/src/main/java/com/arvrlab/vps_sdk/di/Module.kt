package com.arvrlab.vps_sdk.di

import android.content.Context
import android.hardware.SensorManager
import android.location.LocationManager
import com.arvrlab.vps_sdk.BuildConfig
import com.arvrlab.vps_sdk.common.CompassManager
import com.arvrlab.vps_sdk.common.CoordinateConverter
import com.arvrlab.vps_sdk.data.api.IVpsApiManager
import com.arvrlab.vps_sdk.data.api.NeuroApi
import com.arvrlab.vps_sdk.data.api.VpsApiManager
import com.arvrlab.vps_sdk.data.model.request.RequestVpsModel
import com.arvrlab.vps_sdk.data.repository.*
import com.arvrlab.vps_sdk.domain.interactor.INeuroInteractor
import com.arvrlab.vps_sdk.domain.interactor.IVpsInteractor
import com.arvrlab.vps_sdk.domain.interactor.NeuroInteractor
import com.arvrlab.vps_sdk.domain.interactor.VpsInteractor
import com.arvrlab.vps_sdk.ui.ArManager
import com.arvrlab.vps_sdk.ui.VpsArViewModel
import com.arvrlab.vps_sdk.ui.VpsService
import com.arvrlab.vps_sdk.ui.VpsServiceImpl
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import org.koin.core.module.Module
import org.koin.dsl.module
import retrofit2.Retrofit

internal object Module {

    private const val HOST_MOCK = "http://mock/"

    val repository: Module = module {
        single {
            HttpLoggingInterceptor()
                .also { it.level = if (BuildConfig.DEBUG) Level.BODY else Level.NONE }
        }
        single {
            OkHttpClient.Builder()
                .addInterceptor(get<HttpLoggingInterceptor>())
                .build()
        }
        single<Moshi> {
            Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        }
        single<JsonAdapter<RequestVpsModel>> { get<Moshi>().adapter(RequestVpsModel::class.java) }
        single<IVpsApiManager> { VpsApiManager(get(), get()) }
        factory<IVpsRepository> { VpsRepository(get(), get()) }

        single<NeuroApi> {
            Retrofit.Builder()
                .baseUrl(HOST_MOCK)
                .client(get())
                .build()
                .create(NeuroApi::class.java)
        }
        single<INeuroRepository> { NeuroRepository(get(), get()) }
        single<IPrefsRepository> { PrefsRepository(get()) }
    }

    val domain: Module = module {
        factory<INeuroInteractor> { NeuroInteractor(get()) }
        factory<IVpsInteractor> { VpsInteractor(get(), get(), get()) }
    }

    val presentation: Module = module {
        factory { ArManager() }
        factory { get<Context>().getSystemService(LocationManager::class.java) }
        factory { get<Context>().getSystemService(SensorManager::class.java) }
        factory { CompassManager(get()) }
        single { CoordinateConverter() }
        factory<VpsService> { VpsServiceImpl(get(), get(), get(), get(), get()) }
        factory { VpsArViewModel(get(), get(), get()) }
    }

}