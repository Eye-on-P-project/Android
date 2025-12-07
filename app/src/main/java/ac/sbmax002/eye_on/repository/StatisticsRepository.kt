package ac.sbmax002.eye_on.repository

import ac.sbmax002.eye_on.model.statistics.DrivingSession

/**
 * 통계 데이터를 관리하는 Repository
 * 데이터 생성 로직은 MockDataSource로 위임함
 */
class StatisticsRepository {

    private val _allSessions = mutableListOf<DrivingSession>()

    init {
        // 분리된 파일(MockDataSource)에서 더미 데이터 로드
        _allSessions.addAll(MockDataSource.generateDrivingSessions())
    }

    fun getAllSessions(): List<DrivingSession> {
        // 항상 최신순으로 정렬해서 반환
        return _allSessions.sortedByDescending { it.rawDateTime }
    }

    fun getSessionById(id: String): DrivingSession? {
        return _allSessions.find { it.id == id }
    }
}