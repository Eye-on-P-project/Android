package ac.sbmax002.eye_on.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ac.sbmax002.eye_on.ui.theme.EyeOnTheme

/**
 * 스터디 집중 모드 중에 차단된 앱에 접속할 경우 띄워주는 전체 화면 차단 액티비티
 */
class BlockedAppActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 차단된 앱의 패키지명 수신
        val blockedPackage = intent.getStringExtra("blocked_package") ?: ""
        
        var appName = "지정된 앱"
        var appIcon: Drawable? = null
        
        if (blockedPackage.isNotEmpty()) {
            try {
                val pm = packageManager
                val appInfo = pm.getApplicationInfo(blockedPackage, 0)
                appName = pm.getApplicationLabel(appInfo).toString()
                appIcon = pm.getApplicationIcon(appInfo)
            } catch (e: PackageManager.NameNotFoundException) {
                // 패키지 정보를 가져올 수 없는 경우 기본값 유지
            }
        }

        setContent {
            EyeOnTheme {
                // 뒤로가기 입력 시 홈 화면으로 강제 리다이렉트
                BackHandler {
                    navigateToHomeScreen()
                }
                
                BlockedAppScreenContent(
                    appName = appName,
                    appIcon = appIcon,
                    onGoHomeClick = { navigateToHomeScreen() }
                )
            }
        }
    }

    /**
     * 스마트폰의 기본 홈 화면(Launcher)으로 강제 이동
     */
    private fun navigateToHomeScreen() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish() // 현재 차단 화면 종료
    }
}

/**
 * 차단 화면 UI 구성 컴포저블
 */
@Composable
fun BlockedAppScreenContent(
    appName: String,
    appIcon: Drawable?,
    onGoHomeClick: () -> Unit
) {
    val bitmap = remember(appIcon) { drawableToImageBitmap(appIcon) }
    
    // 원형 펄스(맥박) 애니메이션 구현
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF121212),
                        Color(0xFF1E1510), // 은은한 주황색 톤의 그라데이션 적용
                        Color(0xFF2E1A0C)
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 중앙 잠금 로고 및 애니메이션 영역
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(scale),
            contentAlignment = Alignment.Center
        ) {
            // 외부 반투명 원형 맥박선
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFFFF9800).copy(alpha = 0.1f))
            )
            // 내부 원형 배경
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFB74D),
                                Color(0xFFFF9800)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "차단됨",
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // 경고 메시지 타이틀
        Text(
            text = "지금은 공부에 집중할 시간! 📚",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // 설명 문구
        Text(
            text = "스터디 모드가 활성화되어 설정하신\n[$appName] 사용이 제한되었습니다.",
            color = Color(0xFFE0E0E0),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 대상 앱 아이콘 미니어처 표시
        if (bitmap != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Text(
                    text = appName,
                    color = Color(0xFFFFB74D),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        // 홈 화면 이동 버튼 (주황색 테마 적용)
        Button(
            onClick = onGoHomeClick,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9800),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp,
                pressedElevation = 2.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "홈화면으로 돌아가기",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
