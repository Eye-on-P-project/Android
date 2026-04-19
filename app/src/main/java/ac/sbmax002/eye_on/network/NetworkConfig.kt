package ac.sbmax002.eye_on.network

import ac.sbmax002.eye_on.repository.AppStateRepository
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
    const val BASE_URL = "https://api.eyeon.company"

    // 모든 요청에 공통 헤더를 추가하는 인터셉터
    private val commonHeaderInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("X-Client-Type", "APP")
            
        // 로그인/토큰 갱신 시 발급받은 토큰이 메모리에 있다면 Header에 주입
        AppStateRepository.accessToken?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
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

    val monitoringApiService: MonitoringApiService by lazy {
        retrofit.create(MonitoringApiService::class.java)
    }
}
