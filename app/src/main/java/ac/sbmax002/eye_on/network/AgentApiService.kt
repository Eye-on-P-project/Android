package ac.sbmax002.eye_on.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AgentApiService {

    @GET("/api/agent/config")
    suspend fun getConfig(): Response<AgentConfigResponse>

    @POST("/api/agent/chat")
    suspend fun chat(@Body request: AgentChatRequest): Response<AgentChatResponse>
}
