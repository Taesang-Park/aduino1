package com.example.aduino1.presentation.home

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aduino1.domain.model.BluetoothConnectionState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * Home 화면 (메인 화면)
 * 블루투스 연결, 실시간 무게 표시, 일일 섭취량 표시
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val currentWeight by viewModel.currentWeight.collectAsState()
    val todayIntake by viewModel.todayIntake.collectAsState()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsState()

    var showDeviceDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // 블루투스 권한 요청
    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        listOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }

    val permissionState = rememberMultiplePermissionsState(bluetoothPermissions)

    // UI 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is HomeViewModel.UiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is HomeViewModel.UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.error)
                }
                is HomeViewModel.UiEvent.RecordAdded -> {
                    snackbarHostState.showSnackbar("${event.amount}ml 기록됨")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("물 섭취량 모니터") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 블루투스 연결 카드
            BluetoothConnectionCard(
                connectionState = connectionState,
                connectedDeviceName = connectedDeviceName,
                onConnectClick = {
                    if (permissionState.allPermissionsGranted) {
                        showDeviceDialog = true
                    } else {
                        permissionState.launchMultiplePermissionRequest()
                    }
                },
                onDisconnectClick = { viewModel.disconnect() },
                onTareClick = { viewModel.tare() }
            )

            // 현재 무게 카드
            CurrentWeightCard(currentWeight = currentWeight)

            // 오늘의 섭취량 카드
            TodayIntakeCard(
                intake = todayIntake,
                onAddClick = { showAddDialog = true }
            )
        }
    }

    // 디바이스 선택 다이얼로그
    if (showDeviceDialog) {
        DeviceSelectionDialog(
            devices = viewModel.getPairedDevices(),
            onDeviceSelected = { device ->
                viewModel.connectToDevice(device)
                showDeviceDialog = false
            },
            onDismiss = { showDeviceDialog = false }
        )
    }

    // 수동 기록 추가 다이얼로그
    if (showAddDialog) {
        AddWaterDialog(
            onConfirm = { amount ->
                viewModel.addWaterRecord(amount)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

/**
 * 블루투스 연결 카드
 */
@Composable
fun BluetoothConnectionCard(
    connectionState: BluetoothConnectionState,
    connectedDeviceName: String?,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onTareClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "블루투스 연결",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // 연결 상태 표시
                ConnectionStatusChip(connectionState)
            }

            if (connectedDeviceName != null) {
                Text(
                    text = "연결됨: $connectedDeviceName",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (connectionState == BluetoothConnectionState.CONNECTED) {
                    Button(
                        onClick = onDisconnectClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("연결 해제")
                    }

                    Button(
                        onClick = onTareClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("영점 조정")
                    }
                } else {
                    Button(
                        onClick = onConnectClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = connectionState != BluetoothConnectionState.CONNECTING
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            when (connectionState) {
                                BluetoothConnectionState.CONNECTING -> "연결 중..."
                                else -> "디바이스 연결"
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 연결 상태 칩
 */
@Composable
fun ConnectionStatusChip(state: BluetoothConnectionState) {
    val (text, color) = when (state) {
        BluetoothConnectionState.CONNECTED -> "연결됨" to MaterialTheme.colorScheme.primary
        BluetoothConnectionState.CONNECTING -> "연결 중" to MaterialTheme.colorScheme.secondary
        BluetoothConnectionState.DISCONNECTED -> "연결 안 됨" to MaterialTheme.colorScheme.outline
        BluetoothConnectionState.ERROR -> "오류" to MaterialTheme.colorScheme.error
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

/**
 * 현재 무게 카드
 */
@Composable
fun CurrentWeightCard(currentWeight: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "현재 무게",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "%.1f g".format(currentWeight),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 오늘의 섭취량 카드
 */
@Composable
fun TodayIntakeCard(
    intake: com.example.aduino1.domain.model.DailyWaterIntake,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "오늘의 섭취량",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = "수동 추가")
                }
            }

            // 진행률 표시
            Column {
                LinearProgressIndicator(
                    progress = { intake.achievementRate },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${intake.totalAmount} / ${intake.goalAmount} ml",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "${intake.achievementPercent}%",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 남은 양
            if (!intake.isGoalAchieved) {
                Text(
                    text = "목표까지 ${intake.remainingAmount}ml 남았습니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "오늘의 목표를 달성했습니다!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 디바이스 선택 다이얼로그
 */
@Composable
fun DeviceSelectionDialog(
    devices: List<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("블루투스 디바이스 선택") },
        text = {
            if (devices.isEmpty()) {
                Text("페어링된 디바이스가 없습니다")
            } else {
                LazyColumn {
                    items(devices) { device ->
                        TextButton(
                            onClick = { onDeviceSelected(device) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = device.name ?: device.address,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

/**
 * 수동 기록 추가 다이얼로그
 */
@Composable
fun AddWaterDialog(
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("물 섭취 기록 추가") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("양 (ml)") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    amount.toIntOrNull()?.let { onConfirm(it) }
                },
                enabled = amount.toIntOrNull() != null && amount.toInt() > 0
            ) {
                Text("추가")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}