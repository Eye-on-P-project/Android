package ac.sbmax002.eye_on.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
            .height(56.dp)
            .background(
                color = Color(0xFF2A2A2A),
                shape = RoundedCornerShape(14.dp)
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
    val selectedGradient = when (mode) {
        AppMode.DRIVING -> listOf(Color(0xFF007AFF), Color(0xFF0051D5))
        AppMode.STUDY -> listOf(Color(0xFFFF9800), Color(0xFFF57C00))
        AppMode.ORGANIZATION -> listOf(Color(0xFF00A86B), Color(0xFF007A4D))
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = if (isSelected) {
                    Brush.linearGradient(
                        colors = selectedGradient
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent
                        )
                    )
                }
            )
            .clickable {
                android.util.Log.d("ModeSelector", "Mode button clicked: ${mode.name}")
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = if (isSelected) Color.White else Color(0xFF99A1AF),
            letterSpacing = (-0.31).sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
