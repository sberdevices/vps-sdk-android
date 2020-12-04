package lab.ar.network

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Deferred
import lab.ar.network.dto.RequestDto
import lab.ar.network.dto.ResponseDto
import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface RestApiService {

    @Multipart
    @POST("job")
    fun process(
        @Part("json") json: RequestDto,
        @Part image: MultipartBody.Part
    ): Deferred<ResponseDto>

}

object RestApi {

    private var BASE_URL = "https://api.polytech.vps.arvr.sberlabs.com/polytech/vps/api/v1/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    var retrofitService: RestApiService? = null

    fun getApiService(baseUrl: String?): RestApiService {
        val service = retrofitService ?: getClient(baseUrl ?: BASE_URL).create(RestApiService::class.java)
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