package ac.sbmax002.eye_on.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 구독 시스템 Hilt DI 모듈
 *
 * SubscriptionRepository는 @Singleton @Inject constructor로 직접 주입되므로
 * 별도 @Provides가 필요 없습니다.
 *
 * 추후 API 연결 시 SubscriptionApiService 등의 의존성을 여기에 추가합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object SubscriptionModule {
    // 추후 API 서비스 주입 포인트
    // @Provides @Singleton
    // fun provideSubscriptionApiService(): SubscriptionApiService { ... }
}
