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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "eye_on_database" // 폰에 저장될 실제 파일 이름
                )
                    // 괄호안 true로 설정시 개발 중 사용하는 기존 데이터 날리고 재생성 코드
                    // 실제 배포시에는 삭제하거나 Migration 전략을 세워야 함
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}