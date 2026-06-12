package ac.sbmax002.eye_on.network

import android.content.Context
import ac.sbmax002.eye_on.repository.AppStateRepository
import ac.sbmax002.eye_on.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response as OkHttpResponse
import okhttp3.Route
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

    private var settingsRepository: SettingsRepository? = null
    private var authRepository: ac.sbmax002.eye_on.repository.AuthRepository? = null

    fun initialize(context: Context) {
        settingsRepository = SettingsRepository(context)
        authRepository = ac.sbmax002.eye_on.repository.AuthRepository(context)
    }

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

    private val tokenAuthenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: OkHttpResponse): Request? {
            // 401 에러 발생 시 토큰 갱신 시도
            if (response.code() == 401) {
                return runBlocking {
                    val repo = authRepository ?: return@runBlocking null
                    val refreshToken = repo.getRefreshToken() ?: return@runBlocking null
                    
                    try {
                        val authApiService = Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                            .create(AuthApiService::class.java)

                        val refreshResponse = authApiService.refresh(TokenRequest(refreshToken))
                        
                        if (refreshResponse.isSuccessful) {
                            val newTokens = refreshResponse.body()
                            if (newTokens != null) {
                                // 새 토큰 저장
                                repo.saveAuthTokens(
                                    access = newTokens.accessToken,
                                    refresh = newTokens.refreshToken,
                                    uid = newTokens.userId.toString()
                                )
                                AppStateRepository.accessToken = newTokens.accessToken
                                AppStateRepository.userId = newTokens.userId
                                
                                // 기존 요청 재시도
                                return@runBlocking response.request().newBuilder()
                                    .removeHeader("Authorization")
                                    .addHeader("Authorization", "Bearer ${newTokens.accessToken}")
                                    .build()
                            }
                        } else {
                            // 토큰 갱신 실패 시 로그아웃 처리
                            repo.clearAuthTokens()
                            AppStateRepository.accessToken = null
                            AppStateRepository.userId = null
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    null
                }
            }
            return null
        }
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(commonHeaderInterceptor)
        .authenticator(tokenAuthenticator)
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

    val agentApiService: AgentApiService by lazy {
        retrofit.create(AgentApiService::class.java)
    }
}
