plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ac.sbmax002.eye_on"
    compileSdk = 35

    defaultConfig {
        applicationId = "ac.sbmax002.eye_on"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // 기존
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Compose BOM - 모든 Compose 라이브러리의 버전을 통합 관리
    implementation(platform(libs.androidx.compose.bom))

    // Compose UI 기본 라이브러리들 (BOM 사용으로 버전 명시 불필요)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Activity에서 setContent { } 사용하기 위해 필요 (BOM에 포함 안 됨)
    implementation(libs.androidx.activity.compose)

    // Navigation Compose - 화면 간 이동을 위한 네비게이션 (BOM에 포함 안 됨)
    implementation(libs.androidx.navigation.compose)

    // ViewModel과 Compose 연결 (BOM에 포함 안 됨 - 버전 명시 필요)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // 통계용 아이콘 확장
    implementation(libs.androidx.compose.material.icons.extended)

    //camerax - 카메라 기능
    implementation(libs.androidx.camera.camera2) // 실제 하드웨어 제어
    implementation(libs.androidx.camera.lifecycle) //lifecycle 연동
    implementation(libs.androidx.camera.view) //화면에 카메라 영상 표시할 때 필요
    implementation(libs.androidx.camera.core) //기본 엔진 미디어 파이프가 카메라 프레임을 처리할 때 필요

    // MediaPipe Tasks (Vision) - 16KB 페이지 크기 호환 버전
    // 0.10.26.1 이상 버전은 16KB 페이지 크기를 지원합니다
    implementation(libs.mediapipe.tasks.core) // 모든 tasks 기능의 기반 로직 제공
    implementation(libs.mediapipe.tasks.vision) //안면, 눈, 포즈, 제스처 인식 포함

    // Hilt - 의존성 주입
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // DataStore - 설정값 저장
    implementation(libs.androidx.datastore.preferences)

}