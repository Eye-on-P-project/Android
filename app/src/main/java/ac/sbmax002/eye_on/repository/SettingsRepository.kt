package ac.sbmax002.eye_on.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ac.sbmax002.eye_on.ui.settings.AlarmSound
import ac.sbmax002.eye_on.ui.settings.DrowsinessSensitivity
import ac.sbmax002.eye_on.ui.settings.FloatingIconSize
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// DataStore는 애플리케이션 전체에서 싱글톤으로 관리되어야 하므로
// 파일 최상위에 Context 확장 프로퍼티로 정의한다.
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 설정값을 DataStore에 저장하고 불러오는 Repository
 * DataStore (Preferences)를 사용하여 설정값을 영구적으로 저장
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // ApplicationContext 기반 싱글톤 DataStore 사용
    private val dataStore = context.settingsDataStore
    
    // Preference 키 정의
    private object Keys {
        val LEVEL1_ALARM_SOUND = stringPreferencesKey("level1_alarm_sound")
        val LEVEL1_VOLUME = intPreferencesKey("level1_volume")
        val DROWSINESS_SENSITIVITY = stringPreferencesKey("drowsiness_sensitivity")
        val LEVEL2_ALARM_SOUND = stringPreferencesKey("level2_alarm_sound")
        val LEVEL2_VOLUME = intPreferencesKey("level2_volume")
        val FLOATING_ICON_SIZE = stringPreferencesKey("floating_icon_size")
        val VIBRATION_ENABLED = intPreferencesKey("vibration_enabled") // Boolean을 Int로 저장 (0=false, 1=true)
        val DARK_MODE_ENABLED = intPreferencesKey("dark_mode_enabled")
    }
    
    // 졸음 경고 1단계 알림음
    val level1AlarmSound: Flow<AlarmSound> = dataStore.data.map { preferences ->
        val soundName = preferences[Keys.LEVEL1_ALARM_SOUND] ?: AlarmSound.BELL_NOTIFICATION.name
        try {
            AlarmSound.valueOf(soundName)
        } catch (e: IllegalArgumentException) {
            AlarmSound.BELL_NOTIFICATION
        }
    }
    
    suspend fun saveLevel1AlarmSound(sound: AlarmSound) {
        dataStore.edit { preferences ->
            preferences[Keys.LEVEL1_ALARM_SOUND] = sound.name
        }
    }
    
    // 졸음 경고 1단계 음량
    val level1Volume: Flow<Int> = dataStore.data.map { preferences ->
        preferences[Keys.LEVEL1_VOLUME] ?: 70
    }
    
    suspend fun saveLevel1Volume(volume: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.LEVEL1_VOLUME] = volume.coerceIn(0, 100)
        }
    }

    // 졸음 감지 민감도
    val drowsinessSensitivity: Flow<DrowsinessSensitivity> = dataStore.data.map { preferences ->
        val name = preferences[Keys.DROWSINESS_SENSITIVITY] ?: DrowsinessSensitivity.LOW.name
        try {
            DrowsinessSensitivity.valueOf(name)
        } catch (_: IllegalArgumentException) {
            DrowsinessSensitivity.LOW
        }
    }

    suspend fun saveDrowsinessSensitivity(sensitivity: DrowsinessSensitivity) {
        dataStore.edit { preferences ->
            preferences[Keys.DROWSINESS_SENSITIVITY] = sensitivity.name
        }
    }
    
    // 수면 경고 2단계 알림음
    val level2AlarmSound: Flow<AlarmSound> = dataStore.data.map { preferences ->
        val soundName = preferences[Keys.LEVEL2_ALARM_SOUND] ?: AlarmSound.SIREN.name
        try {
            AlarmSound.valueOf(soundName)
        } catch (e: IllegalArgumentException) {
            AlarmSound.SIREN
        }
    }
    
    suspend fun saveLevel2AlarmSound(sound: AlarmSound) {
        dataStore.edit { preferences ->
            preferences[Keys.LEVEL2_ALARM_SOUND] = sound.name
        }
    }
    
    // 수면 경고 2단계 음량
    val level2Volume: Flow<Int> = dataStore.data.map { preferences ->
        preferences[Keys.LEVEL2_VOLUME] ?: 100
    }
    
    suspend fun saveLevel2Volume(volume: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.LEVEL2_VOLUME] = volume.coerceIn(0, 100)
        }
    }
    
    // 플로팅 아이콘 크기
    val floatingIconSize: Flow<FloatingIconSize> = dataStore.data.map { preferences ->
        val sizeName = preferences[Keys.FLOATING_ICON_SIZE] ?: FloatingIconSize.MEDIUM.name
        try {
            FloatingIconSize.valueOf(sizeName)
        } catch (e: IllegalArgumentException) {
            FloatingIconSize.MEDIUM
        }
    }
    
    suspend fun saveFloatingIconSize(size: FloatingIconSize) {
        dataStore.edit { preferences ->
            preferences[Keys.FLOATING_ICON_SIZE] = size.name
        }
    }
    
    // 진동 알림
    val vibrationEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        (preferences[Keys.VIBRATION_ENABLED] ?: 1) == 1
    }
    
    suspend fun saveVibrationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.VIBRATION_ENABLED] = if (enabled) 1 else 0
        }
    }
    
    // 다크 모드
    val darkModeEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        (preferences[Keys.DARK_MODE_ENABLED] ?: 1) == 1
    }
    
    suspend fun saveDarkModeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.DARK_MODE_ENABLED] = if (enabled) 1 else 0
        }
    }
}
