# Usage Examples

이 문서는 Android 앱 코드를 활용하거나 확장할 때 참고할 수 있는 예제입니다.

## 1. 로그인 후 토큰 저장

```kotlin
val response = NetworkConfig.authApiService.login(
    LoginRequest(
        email = "driver@example.com",
        password = "password1234"
    )
)

if (response.isSuccessful) {
    val body = response.body() ?: return

    AppStateRepository.accessToken = body.accessToken
    AppStateRepository.userId = body.userId

    AuthRepository(context).saveAuthTokens(
        access = body.accessToken,
        refresh = body.refreshToken,
        uid = body.userId.toString()
    )
}
```

실제 구현 위치:

- `ui/login/LoginScreen.kt`
- `ui/login/SignUpScreen.kt`

## 2. 모니터링 시작/종료

UI에서는 로컬 통계 세션과 Foreground Service를 함께 시작합니다.

```kotlin
// HomeScreen에서 시작 버튼 클릭 시
viewModel.startMonitoring()
MonitoringService.startMonitoring(context)
(context as? Activity)?.moveTaskToBack(true)
```

종료:

```kotlin
viewModel.stopMonitoring()
MonitoringService.stopMonitoring(context)
```

알림 해제:

```kotlin
MonitoringService.acknowledgeWake(context)
```

주의할 점:

- `HomeViewModel.startMonitoring()`은 Room 로컬 세션을 만듭니다.
- `MonitoringService.startMonitoring()`은 카메라, 모델, 서버 세션, 플로팅 아이콘을 시작합니다.
- 둘 중 하나만 호출하면 UI/로컬통계와 실제 서비스 상태가 어긋날 수 있습니다.

## 3. 모니터링 이벤트 직접 전송

```kotlin
val sessionId: Long = 987654321012345678L
val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))

val response = NetworkConfig.monitoringApiService.recordEvent(
    sessionId = sessionId,
    request = MonitoringEventRequest(
        eventType = "DROWSY",
        occurredAtApp = now
    )
)

if (!response.isSuccessful) {
    Log.e("Monitoring", "event failed: ${response.code()}")
}
```

`MonitoringService.recordServerEvent()`가 이 패턴을 사용합니다.

## 4. 새 API 추가 패턴

예를 들어 서버에 `GET /api/users/me`를 추가로 호출하려면 DTO와 Retrofit interface를 추가합니다.

```kotlin
data class MeResponse(
    val userId: Long,
    val email: String,
    val role: String,
    val name: String,
    val nickname: String?,
    val age: Int?,
    val gender: String?
)

interface UserApiService {
    @GET("/api/users/me")
    suspend fun getMe(): Response<MeResponse>
}
```

`NetworkConfig`에 service lazy property를 추가합니다.

```kotlin
val userApiService: UserApiService by lazy {
    retrofit.create(UserApiService::class.java)
}
```

호출:

```kotlin
val me = NetworkConfig.userApiService.getMe()
if (me.isSuccessful) {
    Log.d("User", "email=${me.body()?.email}")
}
```

공통 헤더와 token refresh는 기존 OkHttp client가 그대로 처리합니다.

## 5. 설정값 저장 및 구독

```kotlin
val repository = SettingsRepository(context)

viewModelScope.launch {
    repository.saveLevel1AlarmSound(AlarmSound.BELL_NOTIFICATION)
    repository.saveLevel1Volume(70)
    repository.saveLevel2AlarmSound(AlarmSound.SIREN)
    repository.saveLevel2Volume(100)
    repository.saveFloatingIconSize(FloatingIconSize.MEDIUM)
}
```

Flow 구독:

```kotlin
viewModelScope.launch {
    repository.level1Volume.collect { volume ->
        Log.d("Settings", "level1 volume=$volume")
    }
}
```

`MonitoringService`는 설정 Flow를 combine해서 경고음, 음량을 실시간 반영합니다.

## 6. Room에 통계 이벤트 저장

```kotlin
val database = AppDatabase.getDatabase(context)
val repository = StatisticsRepository(database.statisticsDao())

val sessionId = repository.startDrivingSession(
    currentMode = AppMode.DRIVING,
    startBatteryPercent = 83
)

repository.saveEvent(
    sessionId = sessionId,
    message = "Drowsiness Detected",
    level = 1,
    duration = "2.3s"
)

repository.endDrivingSession(
    sessionId = sessionId,
    batterySnapshot = BatteryUsageTracker.Snapshot(
        startBatteryPercent = 83,
        endBatteryPercent = 80,
        usagePercent = 3
    )
)
```

통계 화면은 `StatisticsDao.getAllSessions()`의 `Flow<List<DrivingSession>>`를 구독해 DB 변경을 자동 반영합니다.

## 7. PipelineResult를 직접 처리하는 패턴

`MonitoringService`는 pipeline result listener를 통해 `HomeViewModel`에 결과를 넘깁니다.

```kotlin
monitoringService?.setOnPipelineResultListener { result ->
    homeViewModel.onPipelineResult(result)
}
```

`PipelineResult` 구조:

```kotlin
data class PipelineResult(
    val frameTimestampMs: Long,
    val isFaceDetected: Boolean,
    val leftEye: EyeState?,
    val rightEye: EyeState?,
    val drowsinessState: DrowsinessState
)
```

상태별 처리 예시:

```kotlin
when (result.drowsinessState) {
    DrowsinessState.NORMAL -> {
        // 정상 상태 UI 표시
    }
    DrowsinessState.DROWSY -> {
        // 1단계 경고 표시
    }
    DrowsinessState.SLEEPING -> {
        // 2단계 경고 표시
    }
}
```

## 8. AI 동승자에게 음성 답변 보내기

`HomeScreen`은 Android SpeechRecognizer를 사용해 사용자의 음성을 텍스트로 변환한 뒤 서비스에 전달합니다.

```kotlin
monitoringService?.askAgent("조금 졸린 것 같아")
```

`MonitoringService.askAgent()`는 내부에서 다음 요청을 만듭니다.

```kotlin
val request = AgentChatRequest(
    message = createAgentMessage(userMessage),
    drivingState = currentAgentDrivingState()
)

val response = NetworkConfig.agentApiService.chat(request)
```

서버 실패 시 앱 내부 fallback 답변을 TTS로 읽습니다.

## 9. 새로운 알림음 추가

1. `app/src/main/res/raw`에 음원 파일 추가

```text
soft_alarm.mp3
```

2. `AlarmSound` enum에 항목 추가

```kotlin
enum class AlarmSound(
    val displayName: String,
    val fileName: String
) {
    SOFT_ALARM("부드러운 알림음", "soft_alarm")
}
```

3. 설정 화면의 목록은 enum 기반이면 자동 반영됩니다.

`AlarmPlayer`는 `fileName`으로 raw resource id를 찾아 재생합니다.

## 10. 로컬 백엔드로 연결하기

에뮬레이터에서 로컬 Spring Boot 서버를 사용하려면:

```kotlin
const val BASE_URL = "http://10.0.2.2:8080"
```

실기기에서는 같은 Wi-Fi에 있는 개발 PC의 IP를 사용합니다.

```kotlin
const val BASE_URL = "http://192.168.0.10:8080"
```

백엔드가 HTTP라면 debug 환경에서 `usesCleartextTraffic=true`가 필요합니다. 현재 manifest에는 이미 설정되어 있습니다.

## 11. 자주 겪는 문제

| 증상 | 확인할 것 |
| --- | --- |
| 플로팅 아이콘이 안 보임 | 시스템 설정에서 다른 앱 위에 표시 권한 허용 |
| 모니터링 시작 후 카메라가 안 켜짐 | 카메라 권한, foreground service notification 확인 |
| 로컬 API 연결 실패 | emulator는 `localhost` 대신 `10.0.2.2` 사용 |
| 401이 반복됨 | refresh token 저장 여부, 서버 refresh token 유효성 확인 |
| AI 동승자 응답이 fallback만 나옴 | `/api/agent/config`, 구독 권한, Gemini 설정 확인 |
| 통계가 사라짐 | Room destructive migration 또는 앱 데이터 삭제 여부 확인 |

