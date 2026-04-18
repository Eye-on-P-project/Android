package ac.sbmax002.eye_on.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 모니터링 세션 및 이벤트 관련 API 인터페이스 (Swagger v1.0.0 반영)
 */
interface MonitoringApiService {
    
    // 1. 모니터링 세션 시작
    @POST("/api/monitoring/sessions/start")
    suspend fun startMonitoring(@Body request: MonitoringStartRequest): Response<MonitoringSessionResponse>

    // 2. 모니터링 이벤트 발생 (졸음/수면 시작 및 정상 복귀)
    @POST("/api/monitoring/sessions/{sessionId}/events")
    suspend fun recordEvent(
        @Path("sessionId") sessionId: Long,
        @Body request: MonitoringEventRequest
    ): Response<MonitoringEventResponse>

    // 3. 모니터링 세션 종료
    @POST("/api/monitoring/sessions/{sessionId}/end")
    suspend fun endMonitoring(
        @Path("sessionId") sessionId: Long,
        @Body request: MonitoringEndRequest
    ): Response<MonitoringSessionResponse>
}
