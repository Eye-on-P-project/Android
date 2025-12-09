package ac.sbmax002.eye_on.model.statistics

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_events",
    foreignKeys = [
        ForeignKey(
            entity = DrivingSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE // 세션 삭제 시 이벤트도 같이 삭제
        )
    ]
)
data class SessionEvent(
    @PrimaryKey(autoGenerate = true) val eventId: Long = 0, // 고유 ID 자동 생성
    val sessionId: String, // ★ DrivingSession의 id와 연결될 외래키
    val time: String,      // "14:22"
    val message: String,
    val duration: String,
    val level: Int
)