package ac.sbmax002.eye_on.database

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Room Database 및 DAO를 Hilt DI로 제공하기 위한 Module
 *
 * StatisticsDao는 Room이 자동 생성하는 인터페이스이므로
 * @Inject constructor를 사용할 수 없어 @Provides로 제공해야 합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideStatisticsDao(database: AppDatabase): StatisticsDao {
        return database.statisticsDao()
    }
}
