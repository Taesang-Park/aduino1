package com.example.aduino1.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Settings Screen
 * 수분 섭취 목표 및 시간 간격 설정 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // UI 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is SettingsViewModel.UiEvent.SettingsSaved -> {
                    snackbarHostState.showSnackbar("설정이 저장되었습니다")
                    onNavigateBack()
                }
                is SettingsViewModel.UiEvent.SettingsReset -> {
                    snackbarHostState.showSnackbar("설정이 초기화되었습니다")
                }
                is SettingsViewModel.UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 일일 목표량 설정
            DailyGoalSection(
                dailyGoal = uiState.dailyGoal,
                onDailyGoalChange = { viewModel.updateDailyGoal(it) }
            )

            Divider()

            // 시간 간격 설정
            IntervalSection(
                intervalHours = uiState.intervalHours,
                onIntervalChange = { viewModel.updateIntervalHours(it) }
            )

            Divider()

            // 활동 시간 설정
            WakingHoursSection(
                wakingHours = uiState.wakingHours,
                startTimeHour = uiState.startTimeHour,
                onWakingHoursChange = { viewModel.updateWakingHours(it) },
                onStartTimeChange = { viewModel.updateStartTimeHour(it) }
            )

            Divider()

            // 요약 정보
            SummaryCard(uiState)

            Spacer(modifier = Modifier.weight(1f))

            // 버튼들
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.resetToDefault() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("초기화")
                }

                Button(
                    onClick = {
                        if (viewModel.validateSettings()) {
                            viewModel.saveSettings()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("저장")
                }
            }
        }
    }
}

@Composable
private fun DailyGoalSection(
    dailyGoal: Int,
    onDailyGoalChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "일일 목표량",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = dailyGoal.toString(),
            onValueChange = { value ->
                value.toIntOrNull()?.let { onDailyGoalChange(it) }
            },
            label = { Text("목표량 (ml)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            suffix = { Text("ml") }
        )

        Text(
            text = "권장: 2000ml (2L)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun IntervalSection(
    intervalHours: Float,
    onIntervalChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "시간 간격",
            style = MaterialTheme.typography.titleMedium
        )

        // 프리셋 버튼들
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IntervalButton(
                label = "1시간",
                value = 1f,
                isSelected = intervalHours == 1f,
                onClick = { onIntervalChange(1f) },
                modifier = Modifier.weight(1f)
            )
            IntervalButton(
                label = "2시간",
                value = 2f,
                isSelected = intervalHours == 2f,
                onClick = { onIntervalChange(2f) },
                modifier = Modifier.weight(1f)
            )
            IntervalButton(
                label = "3시간",
                value = 3f,
                isSelected = intervalHours == 3f,
                onClick = { onIntervalChange(3f) },
                modifier = Modifier.weight(1f)
            )
        }

        // 커스텀 입력
        OutlinedTextField(
            value = intervalHours.toString(),
            onValueChange = { value ->
                value.toFloatOrNull()?.let { onIntervalChange(it) }
            },
            label = { Text("커스텀 간격 (시간)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            suffix = { Text("시간") }
        )

        Text(
            text = "물을 마시는 주기를 설정합니다 (0.5 ~ 8시간)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun IntervalButton(
    label: String,
    value: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier
    )
}

@Composable
private fun WakingHoursSection(
    wakingHours: Int,
    startTimeHour: Int,
    onWakingHoursChange: (Int) -> Unit,
    onStartTimeChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "활동 시간",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = wakingHours.toString(),
            onValueChange = { value ->
                value.toIntOrNull()?.let { onWakingHoursChange(it) }
            },
            label = { Text("활동 시간 (시간)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            suffix = { Text("시간") }
        )

        OutlinedTextField(
            value = startTimeHour.toString(),
            onValueChange = { value ->
                value.toIntOrNull()?.let { onStartTimeChange(it) }
            },
            label = { Text("시작 시간") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            suffix = { Text("시") },
            supportingText = {
                Text("예: 8 → 오전 8시부터 시작")
            }
        )

        Text(
            text = "활동 시간: 8 ~ 20시간, 시작 시간: 0 ~ 23시",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun SummaryCard(uiState: SettingsViewModel.SettingsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "설정 요약",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Divider()

            SummaryRow("일일 목표", "${uiState.dailyGoal}ml")
            SummaryRow("시간 간격", "${uiState.intervalHours}시간 (${uiState.intervalMinutes}분)")
            SummaryRow("하루 횟수", "${uiState.timesPerDay}회")
            SummaryRow("회당 목표", "${uiState.goalPerInterval}ml")
            SummaryRow(
                "활동 시간",
                "${uiState.startTimeHour}시 ~ ${(uiState.startTimeHour + uiState.wakingHours) % 24}시 (${uiState.wakingHours}시간)"
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
