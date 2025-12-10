package ac.sbmax002.eye_on.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ac.sbmax002.eye_on.model.statistics.DrivingSession
import ac.sbmax002.eye_on.model.statistics.SessionEvent
import ac.sbmax002.eye_on.model.utils.Converters

// entities: 사용할 테이블 목록, version: DB 구조가 바뀔 때 올려야 함
@Database(entities = [DrivingSession::class, SessionEvent::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun statisticsDao(): StatisticsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "eye_on_database" // 실제 디바이스에 만들어질 DB 파일 이름
                )
                    // 개발 중에는 스키마 바꾸면 기존 데이터 날리고 재생성
                    // (배포용에서는 Migration 전략 필요)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
//                INSTANCE = instance
//                instance
            }
        }
    }
}