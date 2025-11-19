# 앱 업그레이드 구현 가이드

**작성일**: 2025-01-08
**목적**: 시간 간격 기반 수분 섭취 시스템 완성을 위한 나머지 구현 가이드

---

## ✅ 완료된 작업

### 1. Arduino 코드
- ✅ `config.h`: RGB LED 핀 정의 추가
- ✅ `water_monitor.ino`: C:x 명령 파싱 및 setRgbLed() 함수 추가

### 2. Domain Layer
- ✅ `LedColorCommand.kt`: LED 색상 Enum
- ✅ `HydrationSettings.kt`: 설정 모델
- ✅ `HydrationInterval.kt`: 시간 구간 모델
- ✅ `IntervalStatus.kt`: 구간 상태 Enum
- ✅ `IntervalCalculator.kt`: 구간 계산 로직

### 3. Data Layer
- ✅ `HydrationSettingsEntity.kt`: 설정 Entity
- ✅ `SettingsDao.kt`: 설정 DAO
- ✅ `WaterDatabase.kt`: 버전 2로 업그레이드
- ✅ `SettingsRepository.kt`: 설정 Repository

### 4. Bluetooth
- ✅ `BluetoothManager.kt`: sendColorCommand() 함수 추가

---

## 📝 남은 작업 (구현 필요)

### 1. WaterRepository 기능 추가

**파일**: `app/src/main/java/com/example/aduino1/data/repository/WaterRepository.kt`

```kotlin
package com.example.aduino1.data.repository

// 기존 import...
import com.example.aduino1.domain.calculator.IntervalCalculator

class WaterRepository(private val waterDao: WaterDao) {

    // 기존 함수들 유지...

    /**
     * 기록 삭제
     */
    suspend fun deleteRecord(record: WaterRecord) {
        waterDao.delete(record)
    }

    /**
     * ID로 기록 삭제
     */
    suspend fun deleteRecordById(id: Long) {
        waterDao.deleteById(id)
    }

    /**
     * 구간 번호별 섭취량 조회
     * @param date 날짜 (yyyy-MM-dd)
     * @param settings 설정
     * @return Map<구간번호, Pair<섭취량, 기록횟수>>
     */
    suspend fun getIntakeByInterval(
        date: String,
        settings: HydrationSettings
    ): Map<Int, Pair<Int, Int>> {
        val records = waterDao.getRecordsByDateOnce(date)
        val intakeMap = mutableMapOf<Int, MutableList<Int>>()

        records.forEach { record ->
            val intervalNumber = IntervalCalculator.getIntervalNumberForTimestamp(
                record.timestamp,
                settings
            )
            if (intervalNumber != null) {
                intakeMap.getOrPut(intervalNumber) { mutableListOf() }.add(record.amount)
            }
        }

        return intakeMap.mapValues { (_, amounts) ->
            Pair(amounts.sum(), amounts.size)
        }
    }

    /**
     * 시간대별 섭취량 (0-23시)
     */
    suspend fun getHourlyIntake(date: String): Map<Int, Int> {
        val records = waterDao.getRecordsByDateOnce(date)
        val hourlyMap = mutableMapOf<Int, Int>()

        records.forEach { record ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = record.timestamp
            }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hourlyMap[hour] = (hourlyMap[hour] ?: 0) + record.amount
        }

        return hourlyMap
    }
}
```

**WaterDao에 추가 필요:**

```kotlin
@Dao
interface WaterDao {
    // 기존 함수들...

    @Query("SELECT * FROM water_records WHERE date = :date ORDER BY timestamp ASC")
    suspend fun getRecordsByDateOnce(date: String): List<WaterRecord>

    @Query("DELETE FROM water_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

---

### 2. HomeViewModel 업그레이드

**파일**: `app/src/main/java/com/example/aduino1/presentation/home/HomeViewModel.kt`

**추가할 핵심 로직:**

```kotlin
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // 기존 코드 유지...

    private val settingsRepository: SettingsRepository

    // 설정 Flow
    val settings: StateFlow<HydrationSettings> = settingsRepository.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HydrationSettings.default())

    // 현재 구간 정보
    private val _currentInterval = MutableStateFlow<HydrationInterval?>(null)
    val currentInterval: StateFlow<HydrationInterval?> = _currentInterval.asStateFlow()

    // 오늘의 모든 구간
    private val _todayIntervals = MutableStateFlow<List<HydrationInterval>>(emptyList())
    val todayIntervals: StateFlow<List<HydrationInterval>> = _todayIntervals.asStateFlow()

    init {
        // 기존 초기화...

        // 구간 계산 및 업데이트
        viewModelScope.launch {
            combine(
                settings,
                repository.getTodayIntake()
            ) { settings, _ ->
                updateIntervals(settings)
            }.collect()
        }

        // 1분마다 구간 업데이트
        viewModelScope.launch {
            while (true) {
                delay(60000) // 1분
                updateIntervals(settings.value)
            }
        }

        // LED 색상 자동 업데이트
        viewModelScope.launch {
            currentInterval.collect { interval ->
                if (interval != null && connectionState.value == BluetoothConnectionState.CONNECTED) {
                    bluetoothManager.sendColorCommand(interval.ledColor)
                }
            }
        }
    }

    private suspend fun updateIntervals(settings: HydrationSettings) {
        val intakeByInterval = repository.getIntakeByInterval(
            getCurrentDate(),
            settings
        )

        val intervals = IntervalCalculator.calculateTodayIntervals(
            settings,
            intakeByInterval
        )

        _todayIntervals.value = intervals
        _currentInterval.value = IntervalCalculator.getCurrentInterval(intervals)
    }

    fun updateSettings(newSettings: HydrationSettings) {
        viewModelScope.launch {
            settingsRepository.saveSettings(newSettings)
        }
    }
}
```

---

### 3. SettingsScreen 구현

**파일**: `app/src/main/java/com/example/aduino1/presentation/settings/SettingsScreen.kt`

```kotlin
package com.example.aduino1.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    var dailyGoal by remember { mutableStateOf(settings.dailyGoal.toString()) }
    var selectedInterval by remember { mutableStateOf(settings.intervalHours) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 하루 목표량
            OutlinedTextField(
                value = dailyGoal,
                onValueChange = { dailyGoal = it },
                label = { Text("하루 목표량 (ml)") },
                modifier = Modifier.fillMaxWidth()
            )

            // 시간 간격
            Text("시간 간격", style = MaterialTheme.typography.titleMedium)

            HydrationSettings.INTERVAL_OPTIONS.forEach { interval ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedInterval == interval,
                        onClick = { selectedInterval = interval }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("${interval}시간마다")
                }
            }

            // 요약 정보
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("요약", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("하루 ${dailyGoal}ml")
                    Text("${selectedInterval}시간 간격")
                    val times = (16 / selectedInterval).toInt()
                    Text("총 ${times}회")
                    Text("회당 ${dailyGoal.toIntOrNull()?.div(times) ?: 0}ml")
                }
            }

            Spacer(Modifier.weight(1f))

            // 저장 버튼
            Button(
                onClick = {
                    val newSettings = settings.copy(
                        dailyGoal = dailyGoal.toIntOrNull() ?: 2000,
                        intervalHours = selectedInterval
                    )
                    viewModel.saveSettings(newSettings)
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("저장")
            }
        }
    }
}
```

**SettingsViewModel:**

```kotlin
package com.example.aduino1.presentation.settings

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<HydrationSettings> = settingsRepository.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HydrationSettings.default())

    fun saveSettings(newSettings: HydrationSettings) {
        viewModelScope.launch {
            settingsRepository.saveSettings(newSettings)
        }
    }
}
```

---

### 4. HistoryScreen 구현

**파일**: `app/src/main/java/com/example/aduino1/presentation/history/HistoryScreen.kt`

```kotlin
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit
) {
    val records by viewModel.todayRecords.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("히스토리") },
                navigationIcon = { /* Back button */ }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 날짜 선택 (추가 가능)
            // ...

            // 기록 리스트
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(records) { record ->
                    WaterRecordItem(
                        record = record,
                        onDelete = { viewModel.deleteRecord(record) }
                    )
                }
            }

            // 수동 추가 버튼
            Button(
                onClick = { viewModel.showAddDialog() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, "추가")
                Spacer(Modifier.width(8.dp))
                Text("수동 추가")
            }
        }
    }

    // 추가 다이얼로그
    if (viewModel.showAddDialog.value) {
        AddWaterDialog(
            onConfirm = { amount ->
                viewModel.addManualRecord(amount)
                viewModel.hideAddDialog()
            },
            onDismiss = { viewModel.hideAddDialog() }
        )
    }
}

@Composable
fun WaterRecordItem(
    record: WaterRecord,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(Date(record.timestamp))
                )
                Text(
                    "${record.amount}ml",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "삭제")
            }
        }
    }
}
```

---

### 5. StatisticsScreen 구현

**파일**: `app/src/main/java/com/example/aduino1/presentation/statistics/StatisticsScreen.kt`

```kotlin
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    onNavigateBack: () -> Unit
) {
    val intervals by viewModel.todayIntervals.collectAsState()
    val hourlyIntake by viewModel.hourlyIntake.collectAsState()
    val statistics by viewModel.statistics.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("통계") },
                navigationIcon = { /* Back button */ }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 전체 통계 카드
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("오늘 총 섭취량", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${statistics.totalAmount} / ${statistics.totalGoal} ml",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        LinearProgressIndicator(
                            progress = { statistics.overallAchievementRate },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("${(statistics.overallAchievementRate * 100).toInt()}%")
                    }
                }
            }

            // 시간대별 바 차트
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("시간대별 섭취량", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))

                        // 간단한 바 차트 (Canvas 사용 또는 라이브러리)
                        SimpleBarChart(
                            data = hourlyIntake,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
            }

            // 구간별 달성률
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("구간별 달성률", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))

                        intervals.chunked(2).forEach { rowIntervals ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowIntervals.forEach { interval ->
                                    IntervalAchievementCard(
                                        interval = interval,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IntervalAchievementCard(
    interval: HydrationInterval,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = when (interval.ledColor) {
                LedColorCommand.RED -> Color.Red.copy(alpha = 0.1f)
                LedColorCommand.YELLOW -> Color.Yellow.copy(alpha = 0.1f)
                LedColorCommand.BLUE -> Color.Blue.copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("${interval.intervalNumber}회", style = MaterialTheme.typography.labelMedium)
            Text(
                "${interval.achievementPercent.toInt()}%",
                style = MaterialTheme.typography.titleLarge
            )
            Icon(
                imageVector = if (interval.isGoalAchieved)
                    Icons.Default.CheckCircle
                else
                    Icons.Default.Circle,
                contentDescription = null,
                tint = when (interval.ledColor) {
                    LedColorCommand.RED -> Color.Red
                    LedColorCommand.YELLOW -> Color.Yellow.copy(red = 0.8f)
                    LedColorCommand.BLUE -> Color.Blue
                }
            )
        }
    }
}
```

---

### 6. Navigation 설정

**파일**: `app/src/main/java/com/example/aduino1/presentation/navigation/NavGraph.kt`

```kotlin
package com.example.aduino1.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Settings : Screen("settings")
    object History : Screen("history")
    object Statistics : Screen("statistics")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel(),
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToStatistics = { navController.navigate(Screen.Statistics.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = viewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen(
                viewModel = viewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
```

**MainActivity 업데이트:**

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Aduino1Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
```

---

### 7. HomeScreen 업데이트 (구간 정보 추가)

**HomeScreen.kt에 추가:**

```kotlin
// 현재 구간 카드 (기존 카드들 위에 추가)
currentInterval?.let { interval ->
    IntervalInfoCard(
        interval = interval,
        onNextIntervalTime = IntervalCalculator.getTimeUntilNextInterval(interval),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun IntervalInfoCard(
    interval: HydrationInterval,
    onNextIntervalTime: Long,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${interval.intervalNumber}번째 구간",
                     style = MaterialTheme.typography.titleMedium)
                Text(
                    interval.ledColor.displayName,
                    color = when (interval.ledColor) {
                        LedColorCommand.RED -> Color.Red
                        LedColorCommand.YELLOW -> Color.Yellow.copy(red = 0.8f)
                        LedColorCommand.BLUE -> Color.Blue
                    }
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(interval.getTimeRangeString())

            Spacer(Modifier.height(16.dp))

            // 진행률
            LinearProgressIndicator(
                progress = { interval.achievementRate.coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("목표: ${interval.goalAmount}ml")
                Text("현재: ${interval.currentAmount}ml (${interval.achievementPercent.toInt()}%)")
            }

            if (!interval.isGoalAchieved) {
                Text("남은 양: ${interval.remainingAmount}ml",
                     style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))

            Text("남은 시간: ${interval.getRemainingTimeString()}",
                 style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

---

## 🔧 빌드 설정

**build.gradle.kts (app)에 추가 (필요시):**

```kotlin
dependencies {
    // 그래프 라이브러리 (선택사항)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // 기존 의존성 유지...
}
```

---

## 🎯 구현 우선순위

1. **우선 높음**: WaterRepository 기능 추가, HomeViewModel 업그레이드
2. **우선 중간**: SettingsScreen, Navigation
3. **우선 낮음**: HistoryScreen, StatisticsScreen (그래프는 나중에)

---

## 🧪 테스트 시나리오

### 기본 흐름 테스트

1. **설정 변경**
   - 앱 실행 → 설정 화면
   - 목표량 2500ml, 간격 1시간으로 변경
   - 저장 → 홈 화면에서 구간 정보 확인

2. **자동 기록 + LED 업데이트**
   - Arduino 연결
   - 물 마시기 (50ml)
   - D:50.0 수신 확인
   - 구간 섭취량 업데이트 확인
   - LED 색상 변경 확인 (C:x 전송)

3. **구간 전환**
   - 현재 구간 종료 시간까지 대기
   - 다음 구간으로 자동 전환 확인
   - 이전 구간 데이터 유지 확인

4. **수동 추가**
   - 히스토리 화면
   - 수동 추가 버튼
   - 100ml 입력 → 저장
   - 리스트에 표시 확인

5. **삭제**
   - 히스토리에서 기록 선택
   - 삭제 버튼
   - 리스트에서 제거 확인

6. **통계 조회**
   - 통계 화면
   - 구간별 달성률 표시 확인
   - 시간대별 그래프 확인

---

## 📚 참고 자료

### Jetpack Compose
- [Compose Navigation](https://developer.android.com/jetpack/compose/navigation)
- [State Management](https://developer.android.com/jetpack/compose/state)

### Room Database
- [Room Migration](https://developer.android.com/training/data-storage/room/migrating-db-versions)

### 그래프 라이브러리
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
- [Vico Charts](https://github.com/patrykandpatrick/vico) (Compose Native)

---

## 💡 추가 개선 아이디어

1. **알림 기능**
   - WorkManager로 주기적 알림
   - 각 구간 시작 시 알림

2. **위젯**
   - 홈 화면 위젯으로 현재 구간 정보 표시

3. **데이터 내보내기**
   - CSV 파일로 내보내기
   - 주간/월간 리포트

4. **클라우드 동기화**
   - Firebase Firestore
   - 여러 기기 간 동기화

---

**다음 단계**: 위 가이드를 참고하여 나머지 파일들을 구현하시면 됩니다!