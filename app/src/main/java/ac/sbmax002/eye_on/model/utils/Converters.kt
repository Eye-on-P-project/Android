package ac.sbmax002.eye_on.model.utils // 또는 적절한 패키지

import androidx.room.TypeConverter
import ac.sbmax002.eye_on.ui.home.AppMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    // LocalDateTime <-> String 변환
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.toString() // ISO-8601 형식으로 저장
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }

    // AppMode <-> String 변환
    @TypeConverter
    fun fromAppMode(mode: AppMode): String {
        return mode.name
    }

    @TypeConverter
    fun toAppMode(value: String): AppMode {
        return runCatching { AppMode.valueOf(value) }.getOrDefault(AppMode.DRIVING)
    }
}