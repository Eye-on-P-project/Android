package ac.sbmax002.eye_on.network

/**
 * 로그인/회원가입/토큰갱신 공통 응답 (AuthTokenResponse)
 */
data class AuthTokenResponse(
    val userId: Long,
    val accessToken: String,
    val refreshToken: String,
    val role: String // "ADMIN" | "USER"
)

/**
 * 로그인 요청 (LoginRequest)
 */
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * 회원가입 요청 (SignupRequest)
 * organizationCode는 서버 DTO 호환을 위해 유지 (앱에서는 "" 전송)
 */
data class SignupRequest(
    val email: String,
    val password: String,
    val organizationCode: String = "",
    val name: String,
    val nickname: String,
    val age: Int,
    val gender: String // "MALE" | "FEMALE"
)

/**
 * 로그아웃/토큰갱신 요청 (LogoutRequest, RefreshRequest)
 */
data class TokenRequest(
    val refreshToken: String
)

/**
 * 일반 성공 여부 응답
 */
data class SimpleResponse(
    val success: Boolean
)
