package com.phlox.tvwebbrowser.service.ai
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface ApiGatewayService {
    // Replace with your API Gateway stage route (e.g., "prod/analyze")
    @POST("photoscan")
    suspend fun analyzeImage(@Body request: LambdaRequest): Response<ResponseBody>
}

object ApiClient {
    // Replace with your actual API Gateway Base URL

    private const val BASE_URL = ""

    val service: ApiGatewayService by lazy {
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiGatewayService::class.java)
    }
}