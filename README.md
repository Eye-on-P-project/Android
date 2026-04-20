# Eye:on Android

Eye:on(아이온) Android 앱은 전면 카메라 + 온디바이스 규칙 기반 분석으로 졸음/피로 상태를 실시간 감지하고, 경고/통계/설정/백그라운드 모니터링을 제공하는 클라이언트입니다.

- 개인 사용자 중심 실시간 탐지(운전/스터디/조직 모드 공통 파이프라인)
- 포그라운드 서비스 + 플로팅 윈도우 기반 백그라운드 모니터링
- 로컬 통계 저장(Room) 및 통계 화면
- 인증/모니터링 서버 연동(로그인, 세션 시작/이벤트/종료)

## 1) 기획/기능명세 대비 구현 현황

| 기능명세 대분류 | 구현 상태 | 현재 Android 실제 구현 |
|---|---|---|
| 1. 홈 화면 | 부분 구현 | 시작/종료 토글, 모드 선택(운전/스터디/조직), 카메라 프리뷰, 통계/설정 이동 구현. |
| 2. 탐지 로직 | 부분 구현 | MediaPipe Face Landmarker + Eye ROI + EAR 기반 상태판정 구현. `PERCLOS`, 입/하품, 시선 이탈, 집중도 모델은 미구현 |
| 3. 경고 | 부분 구현 | 단계별 음성 경고(L1/L2), 단계 승격, 알람 해제(깨어났어요), 플로팅 상태 색상/아이콘 반영 구현.  |
| 4. 설정 | 대부분 구현 | 1단계/2단계 알림음 선택, 음량, 민감도, 플로팅 아이콘 크기, 진동 토글 구현(DataStore 저장). 저조도 설정 UI는 미구현(대신 자동 노출 보정 로직 존재) |
| 5. 통계 | 구현 | 세션/이벤트 로컬 저장, 필터(주간/월간/전체), 요약/상세 타임라인, 배터리 사용량 표시 구현 |
| 6. 시스템 | 구현 | Foreground Service + CAMERA 권한 + SYSTEM_ALERT_WINDOW 권한 + 플로팅 아이콘 + 백그라운드 모니터링 구현 |

## 2) 현재 앱 동작 흐름

```text
Login/SignUp
  -> Home(모드 선택, 시작)
    -> MonitoringService(Foreground)
      -> ServiceCameraManager(CameraX)
      -> FaceProcessingPipeline(MediaPipe -> ROI -> EAR -> 상태판정)
      -> 단계별 Alarm + FloatingWindow 갱신
      -> 서버 이벤트 전송(start/event/end)
      -> HomeViewModel/StatisticsRepository로 세션 이벤트 축적
  -> Statistics/Detail
  -> Settings(알림/민감도/아이콘)
```

## 3) 실제 구현 상세

### 3.1 홈 화면/세션 제어

- `HomeScreen`에서 `모니터링 시작`, `모니터링 종료`, `플로팅 모드 전환` 지원
- 모드: `DRIVING`, `STUDY`, `ORGANIZATION`
- 시작 시:
  - Room 세션 생성
  - 배터리 시작값 기록
  - Foreground Service 시작
- 종료 시:
  - 세션 종료 처리(운행/학습 시간, 배터리 사용량 계산)
  - 알람/플로팅/카메라 해제

### 3.2 카메라/탐지 파이프라인 (현재 규칙 기반)

- 입력: CameraX `ImageAnalysis` 프레임
- 얼굴 추출: MediaPipe `FaceLandmarker`
- 눈 영역 추출: 좌/우 6개 랜드마크 인덱스 기반 ROI
- 지표 계산: EAR
- 상태 판정: `NORMAL`, `DROWSY`, `SLEEPING`

현재 판정 핵심 파라미터(코드 기준):

- 기본 임계: `baseEarThreshold = 0.12`
- 적응 임계: `baselineEar * closedRatio(0.7)`
- 워밍업: `2초` (baseline 학습 구간)
- 졸음 진입: 기본 `1.0초` 연속 감김
- 수면 진입: `3.0초` 연속 감김
- 정상 복귀: `1.0초` 연속 뜸
- 민감도 설정:
  - `HIGH` = 500ms
  - `LOW` = 700ms

즉, 이번 학기 목표 중 딥러닝 시퀀스 모델(경량 backbone + temporal model)은 아직 미적용입니다. 중간 평가 이후 도입 예정입니다.

### 3.3 경고 시스템

- 경고 단계:
  - `LEVEL1`: 졸음
  - `LEVEL2`: 수면
- 단계별 음원/볼륨 분리
- 상태 전이:
  - `NORMAL -> LEVEL1/LEVEL2` 이벤트 서버 기록
  - `LEVEL1 -> LEVEL2` 즉시 승격
  - `LEVEL1/2 -> NORMAL` 복귀 이벤트 기록
- 사용자 해제:
  - 홈 화면 버튼 또는 플로팅 아이콘 클릭으로 `acknowledgeWake`
  - 즉시 알람 중지 + 상태 NORMAL 반영

LLM을 이용한 대화형 졸음 예방 시스템은 아직 미적용입니다. 이후 구현 예정입니다.

### 3.4 플로팅 모드/백그라운드 모니터링

- `MonitoringService`가 foreground로 카메라와 파이프라인을 유지
- 앱이 백그라운드로 가면 플로팅 아이콘 표시
- 아이콘 상태:
  - 얼굴 미검출: `none_detect`
  - 정상: `open`
  - 졸음: `half_close`
  - 수면: `close`
- 아이콘 드래그 가능, 탭 시 앱 복귀 + 알람 해제

### 3.5 설정(DataStore 영속화)

- 1단계/2단계 알림음 선택
- 1단계/2단계 볼륨(0~100)
- 민감도(HIGH/LOW)
- 플로팅 아이콘 크기(S/M/L)
- 진동 토글

### 3.6 통계(Room)

- 엔티티:
  - `driving_sessions`
  - `session_events`
- 저장 항목:
  - 세션 시작/종료 시간, duration, 모드, L1/L2 횟수
  - 이벤트 시간, 메시지, 지속시간, 레벨
  - 배터리 시작/종료/사용량
- 통계 화면:
  - 필터(주간/월간/전체)
  - 총 시간/총 세션/단계별 감지 횟수
  - 시간대 분포(오전/오후/저녁/새벽)
  - 세션 리스트 + 상세 타임라인

### 3.7 서버 연동

- 인증:
  - `POST /api/auth/login`
  - `POST /api/auth/signup`
  - `POST /api/auth/refresh`
  - `POST /api/auth/logout`
- 모니터링:
  - `POST /api/monitoring/sessions/start`
  - `POST /api/monitoring/sessions/{sessionId}/events`
  - `POST /api/monitoring/sessions/{sessionId}/end`
- 인증 토큰 처리:
  - 메모리(`AppStateRepository`) + DataStore 동시 저장
  - 모니터링 API 401 시 refresh 후 재시도


## 4) 기술 스택

- Kotlin, Coroutines, StateFlow
- Jetpack Compose + Navigation Compose
- CameraX
- MediaPipe Tasks Vision (`face_landmarker.task`)
- Foreground Service + Overlay Window
- Room
- DataStore Preferences
- Retrofit + OkHttp
- Hilt(일부 ViewModel/Repository 주입)

## 5) 주요 코드 위치

```text
app/src/main/java/ac/sbmax002/eye_on/
  MainActivity.kt                      # 앱 엔트리, 서비스 바인딩, 세션 복원
  navigation/                          # 로그인/홈/통계/설정 라우팅
  ui/home/                             # 홈/모드선택/권한/카메라 프리뷰
  service/MonitoringService.kt         # 핵심 모니터링 서비스
  service/FloatingWindowManager.kt     # 플로팅 아이콘 관리
  model/pipeline/                      # Face->ROI->EAR->상태 판정
  model/vision/FaceLandmarkerHelper.kt # MediaPipe 래퍼
  ui/settings/                         # 알림/민감도/아이콘 설정
  ui/statistics/                       # 통계 대시보드/상세
  repository/StatisticsRepository.kt   # 세션/이벤트 저장 로직
  repository/SettingsRepository.kt     # DataStore 설정 영속화
  database/                            # Room DB/DAO
  network/                             # Auth/Monitoring API DTO/Service
```

## 6) 실행 방법

### 요구 사항

- JDK 17+
- Android Studio (최신 권장)
- Android SDK (compileSdk 35)

### 터미널 빌드

```bash
cd android
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```

### 실행

- Android Studio에서 `android` 프로젝트 열기
- Gradle Sync 완료 후 에뮬레이터/실기기 실행
- 첫 실행 시 카메라 권한 및 오버레이 권한 허용 필요

## 7) 현재 한계 및 다음 단계

기획서/기능명세 기준으로 아직 남은 항목:

1. 딥러닝 기반 시퀀스 모델(teacher-student, distillation) 온디바이스 추론 적용
2. 눈 외 feature(입/하품, 시선 이탈, 집중도) 반영

## 8) 빌드 확인 기록

- 2026-04-20 기준 `./gradlew :app:compileDebugKotlin` 성공
- 결과: `BUILD SUCCESSFUL`

---

패키지명: `ac.sbmax002.eye_on`  
앱 ID: `ac.sbmax002.eye_on`
