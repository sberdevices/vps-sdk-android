package lab.ar.network

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import lab.ar.network.dto.RequestDto
import lab.ar.network.dto.ResponseDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Deferred
import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

private const val BASE_URL = "https://api.polytech.vps.arvr.sberlabs.com/polytech/vps/api/v1/"

interface RestApiService {

    @Multipart
    @POST("job")
    fun process(
        @Part("json") json: RequestDto,
        @Part image: MultipartBody.Part
    ): Deferred<ResponseDto>

}

object RestApi {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .baseUrl(BASE_URL)
        .build()

    val retrofitService: RestApiService by lazy {
        retrofit.create(RestApiService::class.java)
    }
}