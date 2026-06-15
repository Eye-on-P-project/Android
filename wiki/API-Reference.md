# API Reference

이 문서는 Android 앱이 사용하는 백엔드 API 계약을 정리합니다. 기준 코드는 `app/src/main/java/ac/sbmax002/eye_on/network/*.kt`입니다.

## Common Rules

### Base URL

```kotlin
const val BASE_URL = "https://api.eyeon.company"
```

개발 환경에서는 `NetworkConfig.kt`의 값을 바꿔 사용합니다.

```kotlin
// Android Emulator에서 로컬 Spring Boot 접근
const val BASE_URL = "http://10.0.2.2:8080"
```

### Common Headers

`NetworkConfig.commonHeaderInterceptor`가 모든 요청에 추가합니다.

```http
Accept: application/json
Content-Type: application/json
X-Client-Type: APP
Authorization: Bearer <accessToken>
```

`Authorization`은 `AppStateRepository.accessToken`이 있을 때만 추가됩니다.

### Token Refresh

401 응답을 받으면 OkHttp `Authenticator`가 다음 절차를 수행합니다.

1. `AuthRepository`에서 refresh token 조회
2. `POST /api/auth/refresh` 호출
3. 새 access/refresh token을 `EncryptedSharedPreferences`에 저장
4. `AppStateRepository`의 access token/userId 갱신
5. 실패했던 원 요청을 새 access token으로 재시도

refresh 실패 시 로컬 token을 삭제합니다.

### Time Format

앱은 모니터링 API에 KST 기준 local date-time 문자열을 보냅니다.

```text
yyyy-MM-dd'T'HH:mm:ss
```

예시:

```text
2026-06-15T14:30:00
```

### Long ID Format

백엔드는 JavaScript number precision 문제를 줄이기 위해 `userId`, `sessionId`, `eventId` 같은 큰 `Long` ID를 JSON 문자열로 직렬화할 수 있습니다.

```json
{
  "sessionId": "987654321012345678"
}
```

Android DTO는 현재 `Long` 타입을 사용합니다. Gson은 숫자 문자열을 `Long`으로 파싱할 수 있지만, 새 JSON parser를 도입하거나 DTO를 수정할 때는 이 정책을 함께 확인해야 합니다.

## Enums

### App Mode

```kotlin
enum class AppMode {
    DRIVING,
    STUDY,
    ORGANIZATION
}
```

### Monitoring Event Type

```text
DROWSY
SLEEP
NORMAL
```

### Agent Driving State

```text
NORMAL
DROWSY
SLEEP
```

백엔드는 `AWAKE`, `SLEEPING` alias도 처리할 수 있지만 Android 앱은 현재 `NORMAL`, `DROWSY`, `SLEEP`을 보냅니다.

## Auth API

### Login

```http
POST /api/auth/login
```

Request:

```json
{
  "email": "driver@example.com",
  "password": "password1234"
}
```

Android DTO:

```kotlin
data class LoginRequest(
    val email: String,
    val password: String
)
```

Response:

```json
{
  "userId": "123456789012345678",
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "role": "USER"
}
```

Android behavior:

- `accessToken`과 `userId`를 `AppStateRepository`에 저장
- access/refresh/userId를 `AuthRepository`에 암호화 저장
- 성공 후 `home` 화면으로 이동

### Signup

```http
POST /api/auth/signup
```

Request:

```json
{
  "email": "driver@example.com",
  "password": "password1234",
  "organizationCode": "",
  "name": "홍길동",
  "nickname": "길동",
  "age": 25,
  "gender": "MALE"
}
```

Android DTO:

```kotlin
data class SignupRequest(
    val email: String,
    val password: String,
    val organizationCode: String = "",
    val name: String,
    val nickname: String,
    val age: Int,
    val gender: String
)
```

`gender` 값:

- `MALE`
- `FEMALE`

Response는 login과 같은 `AuthTokenResponse`입니다.

### Refresh

```http
POST /api/auth/refresh
```

Request:

```json
{
  "refreshToken": "eyJhbGciOi..."
}
```

Response:

```json
{
  "userId": "123456789012345678",
  "accessToken": "new-access-token",
  "refreshToken": "new-refresh-token",
  "role": "USER"
}
```

Android에서는 일반적으로 직접 호출하지 않고 `NetworkConfig`의 Authenticator가 자동 호출합니다.

### Logout

```http
POST /api/auth/logout
```

Request:

```json
{
  "refreshToken": "eyJhbGciOi..."
}
```

Response:

```json
{
  "success": true
}
```

Android behavior:

- 서버 호출 실패와 관계없이 로컬 token 삭제
- `AppStateRepository.accessToken`, `userId` 초기화
- 로그인 화면으로 이동

### Delete Account

```http
DELETE /api/auth/account
```

Request:

```json
{
  "password": "password1234"
}
```

Response:

```json
{
  "success": true
}
```

Android behavior:

- 서버 성공 시 token 삭제
- 설정 DataStore 초기화
- Room 세션/이벤트 전체 삭제
- 로그인 화면으로 이동

## Monitoring API

모니터링 API는 인증이 필요합니다.

### Start Monitoring Session

```http
POST /api/monitoring/sessions/start
```

Request:

```json
{
  "mode": "DRIVING",
  "startedAtApp": "2026-06-15T14:30:00"
}
```

Android DTO:

```kotlin
data class MonitoringStartRequest(
    val mode: String,
    val startedAtApp: String
)
```

Response:

```json
{
  "sessionId": "987654321012345678",
  "userId": "123456789012345678",
  "mode": "DRIVING",
  "startedAtApp": "2026-06-15T14:30:00",
  "startedAtServer": "2026-06-15T14:30:01",
  "drowsyCount": 0,
  "sleepCount": 0
}
```

Android behavior:

- `MonitoringService.serverSessionId`에 `sessionId` 저장
- 이후 event/end API path parameter로 사용

### Record Monitoring Event

```http
POST /api/monitoring/sessions/{sessionId}/events
```

Request:

```json
{
  "eventType": "DROWSY",
  "occurredAtApp": "2026-06-15T14:31:10"
}
```

Android DTO:

```kotlin
data class MonitoringEventRequest(
    val eventType: String,
    val occurredAtApp: String
)
```

Response:

```json
{
  "eventId": "111222333444",
  "sessionId": "987654321012345678",
  "eventType": "DROWSY",
  "occurredAtApp": "2026-06-15T14:31:10",
  "occurredAtServer": "2026-06-15T14:31:11",
  "drowsyCount": 1,
  "sleepCount": 0
}
```

Android event rules:

| 앱 상태 전이 | 전송 eventType |
| --- | --- |
| `NONE -> LEVEL1` | `DROWSY` |
| `NONE -> LEVEL2` | `SLEEP` |
| `LEVEL1 -> LEVEL2` | `SLEEP` |
| `LEVEL1/LEVEL2 -> NONE` | `NORMAL` |

### End Monitoring Session

```http
POST /api/monitoring/sessions/{sessionId}/end
```

Request:

```json
{
  "endedAtApp": "2026-06-15T15:10:00"
}
```

Android DTO:

```kotlin
data class MonitoringEndRequest(
    val endedAtApp: String
)
```

Response:

```json
{
  "sessionId": "987654321012345678",
  "userId": "123456789012345678",
  "mode": "DRIVING",
  "startedAtApp": "2026-06-15T14:30:00",
  "startedAtServer": "2026-06-15T14:30:01",
  "endedAtApp": "2026-06-15T15:10:00",
  "endedAtServer": "2026-06-15T15:10:01",
  "durationMinutes": 40,
  "drowsyCount": 2,
  "sleepCount": 1
}
```

Android behavior:

- `serverSessionId`를 null로 초기화
- 카메라, 파이프라인, 알림음, TTS, 플로팅 아이콘 해제

## Agent API

AI 동승자 API는 인증이 필요합니다.

### Get Agent Config

```http
GET /api/agent/config
```

Response:

```json
{
  "enabled": true,
  "mode": "PROACTIVE",
  "cooldownSeconds": 30
}
```

Android DTO:

```kotlin
data class AgentConfigResponse(
    val enabled: Boolean,
    val mode: String,
    val cooldownSeconds: Int
)
```

Android behavior:

- `enabled=false`이면 서버 기반 agent 기능을 제한
- `mode=PROACTIVE`이면 DROWSY/SLEEP recovery 상황에서 로컬 prompt TTS 가능
- `cooldownSeconds`로 prompt 중복 출력 제한

### Chat With Agent

```http
POST /api/agent/chat
```

Request:

```json
{
  "message": "조금 졸린 것 같아",
  "drivingState": "DROWSY"
}
```

Android DTO:

```kotlin
data class AgentChatRequest(
    val message: String,
    val drivingState: String
)
```

Response:

```json
{
  "reply": "가까운 곳에서 잠깐 쉬어가는 게 좋아요.",
  "source": "GEMINI"
}
```

Android behavior:

- 사용자의 음성 입력을 SpeechRecognizer로 텍스트화
- 최근 대화 맥락을 최대 6 turn까지 포함
- 서버 응답이 비어 있거나 실패하면 앱 내부 fallback 문장 사용
- `SLEEPING` 또는 2단계 알림 중에는 agent 응답보다 안전 알림을 우선
- 403이면 "AI 동승자 기능은 구독 사용자만 사용할 수 있어요." 메시지 사용

## Android DTO 참고

현재 Android의 `MonitoringSessionResponse`는 start/end 응답을 하나의 DTO로 받도록 되어 있습니다.

```kotlin
data class MonitoringSessionResponse(
    val sessionId: Long,
    val userId: Long,
    val mode: String,
    val startedAtApp: String,
    val startedAtServer: String,
    val endedAtApp: String?,
    val endedAtServer: String?,
    val durationMinutes: Int,
    val drowsyCount: Int,
    val sleepCount: Int
)
```

백엔드 start 응답에는 종료 관련 필드가 없을 수 있으므로, 서버 계약이 바뀌거나 Gson/Kotlin null 처리 정책을 강화할 때는 start/end DTO를 분리하는 것을 권장합니다.
