package ac.sbmax002.eye_on.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 인증 관련 API 인터페이스 (Swagger v1.0.0 반영)
 * 헤더는 NetworkConfig의 Interceptor에서 공통 처리됨
 */
interface AuthApiService {
    
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthTokenResponse>

    @POST("/api/auth/signup")
    suspend fun signUp(@Body request: SignupRequest): Response<AuthTokenResponse>

    @POST("/api/auth/refresh")
    suspend fun refresh(@Body request: TokenRequest): Response<AuthTokenResponse>

    @POST("/api/auth/logout")
    suspend fun logout(@Body request: TokenRequest): Response<SimpleResponse>
}
