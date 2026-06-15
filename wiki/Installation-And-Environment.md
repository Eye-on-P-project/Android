# Installation And Environment

## 개발 환경

| 항목 | 값 |
| --- | --- |
| Android Gradle Plugin | `8.9.0` |
| Kotlin | `2.1.21` |
| Java target | `17` |
| Gradle wrapper | 저장소 포함 |
| compileSdk | `35` |
| targetSdk | `35` |
| minSdk | `26` |
| Application ID | `ac.sbmax002.eye_on` |
| Namespace | `ac.sbmax002.eye_on` |

권장 환경:

- Android Studio 최신 안정 버전
- JDK 17 이상
- 전면 카메라가 있는 Android 8.0 이상 실기기

## 설치 및 실행

```bash
cd android
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```

Android Studio에서는 `android` 디렉터리를 프로젝트로 열고 Gradle Sync 후 `app`을 실행합니다.

## 권한

`AndroidManifest.xml`에 선언된 권한은 다음과 같습니다.

| 권한 | 목적 |
| --- | --- |
| `CAMERA` | 전면 카메라 프레임 분석 |
| `INTERNET` | 백엔드 API 호출 |
| `RECORD_AUDIO` | AI 동승자 음성 입력 |
| `SYSTEM_ALERT_WINDOW` | 백그라운드 플로팅 아이콘 |
| `FOREGROUND_SERVICE` | 백그라운드 모니터링 서비스 |
| `FOREGROUND_SERVICE_CAMERA` | Android 14 이상 카메라 foreground service |

사용자가 처음 실행할 때 허용해야 하는 권한:

1. 카메라 권한
2. 다른 앱 위에 표시 권한
3. AI 동승자 음성 입력을 사용할 경우 마이크 권한

## 서버 주소 설정

현재 서버 주소는 `NetworkConfig.kt`의 상수입니다.

```kotlin
object NetworkConfig {
    const val BASE_URL = "https://api.eyeon.company"
}
```

개발 환경별 예시:

| 환경 | `BASE_URL` |
| --- | --- |
| 운영 서버 | `https://api.eyeon.company` |
| Android Emulator에서 로컬 백엔드 접근 | `http://10.0.2.2:8080` |
| 실기기에서 같은 Wi-Fi 개발 PC 접근 | `http://192.168.0.10:8080` |

Retrofit의 `baseUrl`은 반드시 `/`로 끝나는 URL이 안전합니다. 현재 상수에는 trailing slash가 없지만 Retrofit은 endpoint path가 `/api/...`로 시작하므로 동작합니다. 새 API base URL을 구성할 때는 `https://api.example.com/` 형태를 권장합니다.

## 주요 라이브러리

| 라이브러리 | 버전 | 사용처 |
| --- | --- | --- |
| AndroidX Core KTX | `1.16.0` | Android Kotlin 확장 |
| AppCompat | `1.7.0` | 호환성 |
| Material | `1.12.0` | Material component |
| Compose BOM | `2024.10.00` | Compose 버전 통합 |
| Activity Compose | `1.9.3` | Compose Activity |
| Navigation Compose | `2.8.3` | 화면 라우팅 |
| Lifecycle Compose | `2.8.6` | lifecycle aware state |
| CameraX | `1.5.1` | 카메라 preview/image analysis |
| MediaPipe Tasks | `0.10.26.1` | Face Landmarker |
| Hilt | `2.51.1` | 의존성 주입 |
| Room | `2.7.0-rc01` | 로컬 통계 DB |
| DataStore Preferences | `1.1.1` | 설정 저장 |
| Retrofit | `2.11.0` | REST API |
| Security Crypto | `1.1.0-alpha06` | 토큰 암호화 저장 |
| TensorFlow Lite | `2.16.1` | 온디바이스 모델 추론 |

## 모델 및 리소스

모델 파일은 `app/src/main/assets`에 포함됩니다.

| 파일 | 역할 |
| --- | --- |
| `face_landmarker.task` | MediaPipe 얼굴 랜드마크 모델 |
| `eye.tflite` | 눈 crop 입력 기반 open/closed 확률 추론 |
| `eye_fp32.tflite` | eye model fp32 variant |
| `gru_fp32.tflite` | temporal 졸음 상태 분류 모델 |

알림음은 `app/src/main/res/raw`에 있습니다.

| 리소스 | 설정 enum |
| --- | --- |
| `bell_notification.mp3` | `BELL_NOTIFICATION` |
| `fire_alarm_test.mp3` | `FIRE_ALARM` |
| `mega_horn.mp3` | `MEGA_HORN` |
| `school_bell.mp3` | `SCHOOL_BELL` |
| `security_alarm.mp3` | `SECURITY_ALARM` |
| `siren_alarm.mp3` | `SIREN` |

## 로컬 저장소

| 저장소 | 이름 | 저장 내용                                |
| --- | --- |--------------------------------------|
| EncryptedSharedPreferences | `auth_prefs` | access token, refresh token, user id |
| DataStore Preferences | `settings` | 알림음, 음량, 민감도, 아이콘 크기, 진동, 다크 모드      |
| DataStore Preferences | `subscription` | 임시 구독 tier, 시작/만료일, 자동 갱신 여부         |
| Room | `eye_on_database` | monitoring session, session event    |

Room은 현재 `fallbackToDestructiveMigration()`을 사용합니다. 개발 중에는 편하지만 운영 배포에서는 명시적 migration 전략으로 바꾸는 것이 좋습니다.

## 빌드 명령

```bash
# Kotlin compile
./gradlew :app:compileDebugKotlin

# Debug APK
./gradlew :app:assembleDebug

# Unit test
./gradlew test

# Instrumented test
./gradlew connectedAndroidTest
```

## 실기기 검증 체크리스트

| 체크 | 기대 결과 |
| --- | --- |
| 로그인/회원가입 | 토큰 저장 후 홈 화면 이동 |
| 모니터링 시작 | 앱이 백그라운드로 이동하고 foreground notification 표시 |
| 플로팅 아이콘 | 다른 앱 위에 표시되고 드래그 가능 |
| 카메라 프리뷰 | 홈 화면 복귀 시 preview가 연결됨 |
| 졸음 감지 | `DROWSY`에서 1단계 경고 |
| 수면 감지 | `SLEEPING`에서 2단계 경고 |
| 알림 해제 | 버튼 또는 플로팅 아이콘 탭으로 알림 중지 |
| 모니터링 종료 | 백엔드 session end 호출, Room 세션 종료 |
| 통계 화면 | 세션과 이벤트 타임라인 표시 |

## 운영 환경에서 분리하면 좋은 항목

- `BASE_URL`: build flavor, Gradle property, `BuildConfig` 기반 분리
- `usesCleartextTraffic`: debug와 release 분리
- API DTO nullable 정책: 백엔드 응답 차이에 맞게 명확화
- Room migration: destructive migration 제거
- 모델 버전: asset 파일명 또는 metadata로 버전 관리

