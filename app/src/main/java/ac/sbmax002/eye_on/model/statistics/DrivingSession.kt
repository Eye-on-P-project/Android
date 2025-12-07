package ac.sbmax002.eye_on.model.statistics

import androidx.room.Entity
import androidx.room.PrimaryKey
import ac.sbmax002.eye_on.ui.home.AppMode
import java.time.LocalDateTime

@Entity(tableName = "driving_sessions")
data class DrivingSession(
    @PrimaryKey val id: String, // UUID 등을 사용하여 고유 문자열 ID 사용
    val dateStr: String,        // "2024-11-25"
    val time: String,           // "14:30"
    val location: String,
    val durationMinutes: Int,
    val durationStr: String,
    val level1Alerts: Int,
    val level2Alerts: Int,
    val rawDateTime: LocalDateTime, // ★ TypeConverter 필요 (아래 3번 참고)
    val mode: AppMode = AppMode.DRIVING // ★ TypeConverter 필요
    // val events: List<SessionEvent>는 DB 컬럼에서 제외합니다. (관계형으로 해결)
)