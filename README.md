# Eye_on

Android 애플리케이션 프로젝트입니다.

## 📋 필수 요구사항

프로젝트를 시작하기 전에 다음 항목들이 설치되어 있어야 합니다:

### 필수 설치 항목

- **Java Development Kit (JDK) 17 이상**
  - 확인 방법:
    ```bash
    java -version
    ```
    - 출력 예: `openjdk version "17.x.x"` 이상이어야 함
  - 다운로드: [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) 또는 [OpenJDK](https://adoptium.net/)

- **Android Studio**
  - 최신 버전 권장
  - 다운로드: [Android Studio 공식 사이트](https://developer.android.com/studio)

### 자동 설치되는 항목

- ✅ **Gradle**: 프로젝트에 포함되어 있음 (`gradle-wrapper.jar`)
- ✅ **Android SDK**: Android Studio 설치 시 자동 포함

## 🚀 시작하기

### 1. 프로젝트 클론

```bash
git clone <저장소-URL>
```

### 2. Android Studio에서 열기

1. Android Studio 실행
2. `File` → `Open` → 프로젝트 폴더 선택
3. Android Studio가 자동으로 Gradle 동기화를 시작합니다

### 3. 첫 빌드

Android Studio에서:
- 프로젝트가 열리면 자동으로 Gradle 동기화가 실행됩니다
- 완료되면 `Build` → `Make Project` 또는 `Ctrl+F9` (Windows) / `Cmd+F9` (Mac)

또는 터미널에서:
```bash
# macOS/Linux
./gradlew build

# Windows
gradlew.bat build
```

### 4. 에뮬레이터 또는 실제 기기에서 실행

- **에뮬레이터**: `Tools` → `Device Manager`에서 AVD 생성
- **실제 기기**: USB 디버깅 활성화 후 연결

## 🛠 개발 환경 정보

- **Gradle**: 8.11.1 (자동 설치됨)
- **JDK**: 17 이상 필요
- **Kotlin**: 최신 버전 (build.gradle.kts에서 관리)
- **compileSdk**: 35
- **minSdk**: 26
- **targetSdk**: 35
- **빌드 도구**: Gradle (Kotlin DSL)

## 📁 프로젝트 구조

```
Eye_on/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/          # Kotlin 소스 코드
│   │   │   ├── res/           # 리소스 (레이아웃, 이미지 등)
│   │   │   └── AndroidManifest.xml
│   │   ├── androidTest/       # UI 테스트
│   │   └── test/              # 유닛 테스트
│   └── build.gradle.kts       # 앱 빌드 설정
├── gradle/
│   └── wrapper/               # Gradle Wrapper 파일
├── build.gradle.kts           # 프로젝트 빌드 설정
├── settings.gradle.kts        # 프로젝트 구조 설정
└── gradle.properties          # Gradle 전역 설정
```

## 🔧 문제 해결

### Gradle 동기화 실패

1. **네트워크 문제**: 인터넷 연결 확인 (Gradle이 의존성 다운로드)
2. **JDK 버전 문제**: Java 17 이상인지 확인
   ```bash
   java -version
   ```
3. **캐시 삭제 후 재시도**:
   ```bash
   ./gradlew clean
   ```

### 빌드 에러

- `SDK location not found`: Android Studio에서 SDK 경로 설정 확인
  - `File` → `Project Structure` → `SDK Location`
- `Failed to resolve dependency`: 네트워크 연결 확인 후 재시도

### Windows/macOS 호환성

- 줄 바꿈 문자 문제는 `.gitattributes`로 자동 해결됩니다
- 파일명 대소문자 주의 (Windows는 대소문자 구분 안 함)

## 🤝 팀 협업 가이드

### 커밋 전 확인사항 ********중요*********

- ✅ 코드가 정상적으로 빌드되는지 확인
- ✅ `local.properties` 파일을 커밋하지 않았는지 확인 (자동으로 무시됨 그럼에도 체크해야함)
- ✅ 빌드 결과물 (`/build`, `*.apk`)을 커밋하지 않았는지 확인


## 📝 추가 정보

- **패키지명**: `ac.sbmax002.eye_on`
- **앱 ID**: `ac.sbmax002.eye_on`