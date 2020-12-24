package ru.arvrlab.vps.network

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import ru.arvrlab.vps.network.dto.RequestDto
import ru.arvrlab.vps.network.dto.ResponseDto
import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface VpsApiService {

    @Multipart
    @POST("job")
    suspend fun process(
        @Part("json") json: RequestDto,
        @Part image: MultipartBody.Part
    ): ResponseDto

}

object VpsApi {

    var BASE_URL = "https://api.bootcamp.vps.arvr.sberlabs.com/eeb38592-4a3c-4d4b-b4c6-38fd68331521/vps/api/v1/"
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    var retrofitService: VpsApiService? = null

    fun getApiService(baseUrl: String): VpsApiService {
        val service = retrofitService ?: getClient(baseUrl).create(VpsApiService::class.java)
        if(retrofitService == null) {
            retrofitService = service
        }

        return service
    }

    private fun getClient(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .baseUrl(baseUrl)
            .build()
    }
}