package ac.sbmax002.eye_on.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ac.sbmax002.eye_on.model.statistics.DrivingSession
import ac.sbmax002.eye_on.model.statistics.SessionEvent

@Dao
interface StatisticsDao {
    // 1. 운전 세션 시작 (이미 ID가 생성된 상태로 들어옴)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: DrivingSession): Long

    // 세션 여러 건 삽입(MockDataSource 초기 데이터 적재용)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<DrivingSession>)

    // 2. 운전 세션 업데이트 (종료 시간, 알림 횟수 등 갱신)
    @Update
    suspend fun updateSession(session: DrivingSession): Int

    // 3. 졸음 이벤트 저장
    @Insert
    suspend fun insertEvent(event: SessionEvent): Long

    // 이벤트 여러 건 삽입 (MockDataSource 초기 데이터 적재용)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<SessionEvent>)

    // 4. 통계 화면용: 모든 세션을 날짜 내림차순(최신순)으로 가져오기
    // Flow를 반환하면 DB가 변경될 때마다 화면이 자동으로 갱신됩니다.
    @Query("SELECT * FROM driving_sessions ORDER BY rawDateTime DESC")
    fun getAllSessions(): Flow<List<DrivingSession>>

    // 5. 상세 화면용: 특정 세션의 모든 이벤트 가져오기
    @Query("SELECT * FROM session_events WHERE sessionId = :sessionId ORDER BY eventId ASC")
    suspend fun getEventsForSession(sessionId: String): List<SessionEvent>

    // 6. 특정 세션 정보 하나만 가져오기
    @Query("SELECT * FROM driving_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): DrivingSession?

    // (선택) 특정 세션 삭제
    @Query("DELETE FROM driving_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String): Int

    // 전체 삭제 (초기화용)
    @Query("DELETE FROM session_events")
    suspend fun clearAllEvents()

    @Query("DELETE FROM driving_sessions")
    suspend fun clearAllSessions()

    // 세션/이벤트 통째로 갈아끼우기 (MockDataSource.generateData() 초기 적재용)
    @Transaction
    suspend fun replaceAllData(
        sessions: List<DrivingSession>,
        events: List<SessionEvent>
    ) {
        clearAllEvents()
        clearAllSessions()
        insertSessions(sessions)
        insertEvents(events)
    }
}