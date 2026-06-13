package ac.sbmax002.eye_on.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ModeSelector(
    selectedMode: AppMode,
    onModeSelected: (AppMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ModeButton(
            mode = AppMode.DRIVING,
            label = "운전 모드",
            isSelected = selectedMode == AppMode.DRIVING,
            onClick = { onModeSelected(AppMode.DRIVING) },
            modifier = Modifier.weight(1f)
        )

        ModeButton(
            mode = AppMode.STUDY,
            label = "스터디 모드",
            isSelected = selectedMode == AppMode.STUDY,
            onClick = { onModeSelected(AppMode.STUDY) },
            modifier = Modifier.weight(1f)
        )

        ModeButton(
            mode = AppMode.ORGANIZATION,
            label = "조직 모드",
            isSelected = selectedMode == AppMode.ORGANIZATION,
            onClick = { onModeSelected(AppMode.ORGANIZATION) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModeButton(
    mode: AppMode,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) modeAccentColor(mode) else Color.Transparent)
            .clickable {
                android.util.Log.d("ModeSelector", "Mode button clicked: ${mode.name}")
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
        )
    }
}

private fun modeAccentColor(mode: AppMode): Color = when (mode) {
    AppMode.DRIVING -> Color(0xFF2477F2)
    AppMode.STUDY -> Color(0xFFFF9F0A)
    AppMode.ORGANIZATION -> Color(0xFF1EB980)
}
