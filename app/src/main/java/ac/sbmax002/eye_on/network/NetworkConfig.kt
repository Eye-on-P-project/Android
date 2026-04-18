package ac.sbmax002.eye_on.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * 네트워크 관련 설정을 관리하는 객체
 */
object NetworkConfig {
    /**
     * 백엔드 서버의 기본 URL
     */
    const val BASE_URL = "http://localhost:8080"

    // 모든 요청에 공통 헤더를 추가하는 인터셉터
    private val commonHeaderInterceptor = Interceptor { chain ->
        val original = chain.request()
        val request = original.newBuilder()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .build()
        chain.proceed(request)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(commonHeaderInterceptor)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }
}
