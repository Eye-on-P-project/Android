package ac.sbmax002.eye_on.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: AccountViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val email by viewModel.email.collectAsState()
    val organization by viewModel.organization.collectAsState()
    val name by viewModel.name.collectAsState()
    val nickname by viewModel.nickname.collectAsState()
    val birthYear by viewModel.birthYear.collectAsState()
    val gender by viewModel.gender.collectAsState()
    val isDeletingAccount by viewModel.isDeletingAccount.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }

    // Snackbar 이벤트 수신
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is AccountViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is AccountViewModel.UiEvent.NavigateBack -> { /* handled elsewhere */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "계정 정보",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF1A1A1A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // 정보 표시 리스트 디자인 (Read-Only)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF2A2A2A)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    AccountInfoItem(label = "이메일", value = email)
                    if (organization.isNotBlank()) {
                        AccountInfoItem(label = "조직", value = organization)
                    }
                    AccountInfoItem(label = "이름", value = name)
                    AccountInfoItem(label = "닉네임", value = nickname)
                    AccountInfoItem(label = "출생 연도", value = birthYear)
                    AccountInfoItem(label = "성별", value = gender, showDivider = false)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 정보 수정 버튼
            Button(
                onClick = onNavigateToEditProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF)
                )
            ) {
                Text("회원 정보 수정", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // 로그아웃 버튼
            Button(
                onClick = { viewModel.logout(onNavigateToLogin) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.8f)
                )
            ) {
                Text("로그아웃", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // 회원 탈퇴 버튼
            TextButton(
                onClick = {
                    deletePassword = ""
                    showDeleteDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "회원 탈퇴",
                    color = Color(0xFF99A1AF),
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }

    // 탈퇴 확인 다이얼로그 (비밀번호 재입력)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeletingAccount) {
                    showDeleteDialog = false
                }
            },
            title = {
                Text(
                    "회원 탈퇴",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "정말로 탈퇴하시겠습니까?\n\n" +
                                "탈퇴 시 모든 데이터가 영구적으로 삭제되며 복구할 수 없습니다.",
                        color = Color(0xFFCCCCCC),
                        fontSize = 14.sp
                    )

                    // 비밀번호 입력 필드
                    OutlinedTextField(
                        value = deletePassword,
                        onValueChange = { deletePassword = it },
                        label = { Text("비밀번호 확인", color = Color(0xFF99A1AF)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !isDeletingAccount,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Red,
                            unfocusedBorderColor = Color(0xFF555555),
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount(deletePassword) {
                            showDeleteDialog = false
                            onNavigateToLogin()
                        }
                    },
                    enabled = !isDeletingAccount && deletePassword.isNotBlank()
                ) {
                    if (isDeletingAccount) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.Red,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("탈퇴하기", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !isDeletingAccount
                ) {
                    Text("취소", color = Color(0xFF99A1AF))
                }
            },
            containerColor = Color(0xFF2A2A2A),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun AccountInfoItem(
    label: String,
    value: String,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color(0xFF99A1AF),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = Color(0xFF3A3A3A),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}
