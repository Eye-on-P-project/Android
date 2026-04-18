package ac.sbmax002.eye_on.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ac.sbmax002.eye_on.network.NetworkConfig
import ac.sbmax002.eye_on.network.SignupRequest
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onNavigateToHome: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    // 나이(년도) 드롭다운 상태
    var expandedYear by remember { mutableStateOf(false) }
    var selectedYear by remember { mutableStateOf("") }
    val years = (1950..2024).map { it.toString() }.reversed()

    // 성별 상태
    val genderOptions = listOf("남", "여")
    var selectedGender by remember { mutableStateOf(genderOptions[0]) }

    val scrollState = rememberScrollState()

    // 어두운 배경색 적용
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF1A1A1A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "회원가입",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            CustomTextField(
                value = email,
                onValueChange = { email = it },
                label = "이메일",
                keyboardType = KeyboardType.Email
            )
            
            CustomTextField(
                value = password,
                onValueChange = { password = it },
                label = "비밀번호",
                isPassword = true
            )
            
            CustomTextField(
                value = name,
                onValueChange = { name = it },
                label = "이름"
            )
            
            CustomTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = "닉네임"
            )
            
            // 년도 드롭다운
            ExposedDropdownMenuBox(
                expanded = expandedYear,
                onExpandedChange = { expandedYear = !expandedYear },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = selectedYear,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("출생 년도", color = Color(0xFF9E9E9E)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedYear) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = Color(0xFF424242),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = expandedYear,
                    onDismissRequest = { expandedYear = false },
                    modifier = Modifier.background(Color(0xFF2A2A2A))
                ) {
                    years.forEach { year ->
                        DropdownMenuItem(
                            text = { Text(text = year, color = Color.White) },
                            onClick = {
                                selectedYear = year
                                expandedYear = false
                            }
                        )
                    }
                }
            }
            
            // 성별 선택 (Radio Buttons)
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "성별",
                    fontSize = 14.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    genderOptions.forEach { text ->
                        Row(
                            Modifier
                                .height(56.dp)
                                .selectable(
                                    selected = (text == selectedGender),
                                    onClick = { selectedGender = text },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (text == selectedGender),
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF007AFF),
                                    unselectedColor = Color(0xFF757575)
                                )
                            )
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp),
                                color = Color.White
                            )
                        }
                    }
                }
            }
            
            AnimatedButtonLogin(
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank() && name.isNotBlank()) {
                        scope.launch {
                            try {
                                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                                val ageValue = if (selectedYear.isNotEmpty()) currentYear - selectedYear.toInt() + 1 else 20
                                val response = NetworkConfig.authApiService.signUp(
                                    SignupRequest(
                                        email = email,
                                        password = password,
                                        organizationCode = "", // 서버 규격 호환
                                        name = name,
                                        nickname = nickname,
                                        age = ageValue,
                                        gender = if (selectedGender == "남") "MALE" else "FEMALE"
                                    )
                                )
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                                    onNavigateToHome()
                                } else {
                                    val msg = when(response.code()) {
                                        409 -> "이미 사용 중인 이메일입니다."
                                        400 -> "입력 양식을 확인해주세요."
                                        else -> "회원가입 실패: ${response.code()}"
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                text = "가입 완료",
                backgroundColor = Color(0xFF007AFF)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = onNavigateBack) {
                Text(
                    text = "이미 계정이 있으신가요? 로그인",
                    color = Color(0xFF007AFF),
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color(0xFF9E9E9E)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF007AFF),
            unfocusedBorderColor = Color(0xFF424242),
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true
    )
}