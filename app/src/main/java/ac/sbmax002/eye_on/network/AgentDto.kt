package ac.sbmax002.eye_on.network

data class AgentConfigResponse(
    val enabled: Boolean,
    val mode: String,
    val cooldownSeconds: Int
)

data class AgentChatRequest(
    val message: String,
    val drivingState: String
)

data class AgentChatResponse(
    val reply: String,
    val source: String? = null
)
