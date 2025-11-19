# 물 섭취량 모니터링 앱 - 코드 상세 설명서

**대상**: 개발자, 코드 리뷰어, 학습자
**난이도**: 중급~고급
**작성일**: 2025-01-08

---

## 📋 목차

1. [앱 개요](#1-앱-개요)
2. [기술 스택](#2-기술-스택)
3. [프로젝트 구조](#3-프로젝트-구조)
4. [아키텍처 설명](#4-아키텍처-설명)
5. [계층별 상세 설명](#5-계층별-상세-설명)
6. [주요 컴포넌트 동작 원리](#6-주요-컴포넌트-동작-원리)
7. [데이터 흐름](#7-데이터-흐름)
8. [블루투스 통신 메커니즘](#8-블루투스-통신-메커니즘)
9. [UI/UX 구현](#9-uiux-구현)
10. [핵심 기능 구현 분석](#10-핵심-기능-구현-분석)
11. [코드 예시 및 분석](#11-코드-예시-및-분석)
12. [확장 포인트](#12-확장-포인트)

---

## 1. 앱 개요

### 1.1 프로젝트 목적

로드셀과 아두이노를 사용하여 컵의 물 무게를 실시간으로 측정하고, 블루투스를 통해 데이터를 수신하여 일일 물 섭취량을 자동으로 기록하고 관리하는 Android 앱입니다.

### 1.2 핵심 기능

```
1. 블루투스 통신
   └─ Arduino HC-05/06과 연결
   └─ 실시간 데이터 수신
   └─ 양방향 명령 전송

2. 자동 섭취 감지
   └─ 무게 변화 자동 감지
   └─ 마신 양 계산
   └─ 데이터베이스 자동 저장

3. 데이터 관리
   └─ Room Database 영구 저장
   └─ 날짜별 기록 관리
   └─ 통계 계산

4. 사용자 인터페이스
   └─ Jetpack Compose 반응형 UI
   └─ Material3 디자인
   └─ 실시간 업데이트
```

---

## 2. 기술 스택

### 2.1 언어 및 프레임워크

```kotlin
언어: Kotlin 1.9.0
최소 SDK: Android 7.0 (API 24)
타겟 SDK: Android 14 (API 36)
```

### 2.2 주요 라이브러리

| 카테고리 | 라이브러리 | 버전 | 용도 |
|---------|-----------|------|------|
| UI | Jetpack Compose | 2024.02.00 | 선언형 UI |
| UI | Material3 | Latest | 디자인 시스템 |
| 아키텍처 | ViewModel | 2.7.0 | MVVM 패턴 |
| 아키텍처 | LiveData | 2.7.0 | 생명주기 인식 데이터 |
| 데이터베이스 | Room | 2.6.1 | 로컬 저장소 |
| 비동기 | Coroutines | 1.7.3 | 비동기 처리 |
| 반응형 | Flow | - | 데이터 스트림 |
| 네비게이션 | Navigation Compose | 2.7.7 | 화면 전환 |
| 권한 | Accompanist Permissions | 0.34.0 | 권한 처리 |

### 2.3 아키텍처 패턴

```
MVVM (Model-View-ViewModel)
+ Clean Architecture (3-Layer)
+ Repository Pattern
+ Reactive Programming (Flow)
```

---

## 3. 프로젝트 구조

### 3.1 전체 디렉토리 구조

```
app/src/main/java/com/example/aduino1/
│
├── data/                           # 데이터 계층
│   ├── local/
│   │   ├── entity/
│   │   │   └── WaterRecord.kt      # Room Entity
│   │   └── database/
│   │       ├── WaterDao.kt         # Database Access Object
│   │       └── WaterDatabase.kt    # Room Database
│   └── repository/
│       └── WaterRepository.kt      # Repository (데이터 추상화)
│
├── domain/                         # 도메인 계층
│   └── model/
│       └── DailyWaterIntake.kt     # 비즈니스 모델
│
├── presentation/                   # 프레젠테이션 계층
│   ├── bluetooth/
│   │   └── BluetoothManager.kt     # 블루투스 통신 관리
│   └── home/
│       ├── HomeViewModel.kt        # ViewModel
│       └── HomeScreen.kt           # Compose UI
│
├── ui/                             # UI 테마
│   └── theme/
│       ├── Theme.kt                # Material3 테마
│       └── Type.kt                 # 타이포그래피
│
└── MainActivity.kt                 # 메인 액티비티
```

### 3.2 파일별 역할

| 파일 | 라인 수 | 주요 역할 |
|-----|--------|----------|
| WaterRecord.kt | ~50 | 데이터베이스 Entity 정의 |
| WaterDao.kt | ~100 | 데이터베이스 쿼리 정의 |
| WaterDatabase.kt | ~50 | Room 데이터베이스 설정 |
| WaterRepository.kt | ~120 | 데이터 접근 추상화 |
| DailyWaterIntake.kt | ~80 | 비즈니스 로직 모델 |
| BluetoothManager.kt | ~300 | 블루투스 통신 전체 |
| HomeViewModel.kt | ~130 | UI 상태 관리 |
| HomeScreen.kt | ~350 | Compose UI 구현 |
| MainActivity.kt | ~20 | 앱 진입점 |

---

## 4. 아키텍처 설명

### 4.1 MVVM 패턴

```
┌─────────────────────────────────────────┐
│              View (Compose)              │
│         HomeScreen.kt                    │
│  - UI 렌더링                              │
│  - 사용자 입력 처리                        │
└──────────────┬──────────────────────────┘
               │ collectAsState()
               │ 이벤트 전달
               ↓
┌─────────────────────────────────────────┐
│           ViewModel                      │
│         HomeViewModel.kt                 │
│  - UI 상태 관리 (StateFlow)               │
│  - 비즈니스 로직 호출                      │
│  - 이벤트 처리                            │
└──────────────┬──────────────────────────┘
               │ Repository 호출
               │ BluetoothManager 호출
               ↓
┌─────────────────────────────────────────┐
│            Model (Repository)            │
│  - WaterRepository.kt                    │
│  - BluetoothManager.kt                   │
│  - 데이터 소스 추상화                      │
└──────────────┬──────────────────────────┘
               │ DAO 호출
               │ Bluetooth API 호출
               ↓
┌─────────────────────────────────────────┐
│          Data Source                     │
│  - Room Database                         │
│  - Bluetooth Classic API                 │
└─────────────────────────────────────────┘
```

### 4.2 Clean Architecture (3-Layer)

#### Layer 1: Data Layer (데이터 계층)

```kotlin
역할: 데이터의 저장, 조회, 외부 통신
구성 요소:
  - Entity (WaterRecord)
  - DAO (WaterDao)
  - Database (WaterDatabase)
  - Repository (WaterRepository)

책임:
  - Room Database 접근
  - 쿼리 실행
  - 데이터 변환
```

#### Layer 2: Domain Layer (도메인 계층)

```kotlin
역할: 비즈니스 로직, 순수 Kotlin 클래스
구성 요소:
  - Model (DailyWaterIntake)
  - Enum (BluetoothConnectionState)

책임:
  - 비즈니스 규칙 정의
  - 계산 로직 (달성률, 남은 양)
  - 도메인 모델 정의
```

#### Layer 3: Presentation Layer (프레젠테이션 계층)

```kotlin
역할: UI 표시, 사용자 상호작용
구성 요소:
  - ViewModel (HomeViewModel)
  - Compose UI (HomeScreen)
  - BluetoothManager

책임:
  - UI 상태 관리
  - 사용자 입력 처리
  - 화면 렌더링
```

### 4.3 의존성 방향

```
Presentation → Domain ← Data
     ↓           ↑         ↑
  ViewModel   Model    Repository
     ↓                      ↓
     UI                 Database
```

**핵심 원칙:**
- 상위 계층은 하위 계층에만 의존
- Domain은 다른 계층에 의존하지 않음 (순수 Kotlin)
- Data와 Presentation은 서로 직접 의존하지 않음

---

## 5. 계층별 상세 설명

### 5.1 Data Layer

#### 5.1.1 WaterRecord.kt (Entity)

```kotlin
@Entity(tableName = "water_records")
data class WaterRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Int,        // 마신 양 (ml)
    val timestamp: Long,    // 타임스탬프 (밀리초)
    val date: String        // 날짜 (yyyy-MM-dd)
)
```

**역할:**
- Room Database의 테이블 구조 정의
- 한 번의 물 섭취 기록을 나타냄

**필드 설명:**
- `id`: 자동 증가 Primary Key
- `amount`: 마신 양 (ml 단위)
- `timestamp`: 기록 시간 (밀리초)
- `date`: 날짜 문자열 (집계용)

**특징:**
- `@Entity`: Room에게 테이블임을 알림
- `@PrimaryKey(autoGenerate = true)`: 자동 ID 생성
- `data class`: 자동으로 equals, hashCode, copy 생성

#### 5.1.2 WaterDao.kt (Data Access Object)

```kotlin
@Dao
interface WaterDao {
    // 삽입
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: WaterRecord): Long

    // 조회 (Flow)
    @Query("SELECT * FROM water_records WHERE date = :date ORDER BY timestamp DESC")
    fun getRecordsByDate(date: String): Flow<List<WaterRecord>>

    // 총량 조회
    @Query("SELECT COALESCE(SUM(amount), 0) FROM water_records WHERE date = :date")
    suspend fun getTotalAmountByDate(date: String): Int

    // 삭제
    @Delete
    suspend fun delete(record: WaterRecord)
}
```

**역할:**
- 데이터베이스 쿼리 정의
- CRUD 작업 추상화

**주요 함수:**

1. **insert()**: 새 기록 추가
   - `suspend`: 코루틴에서 실행
   - `onConflict`: 중복 시 덮어쓰기
   - 반환값: 삽입된 row ID

2. **getRecordsByDate()**: 날짜별 기록 조회
   - `Flow`: 반응형 데이터 스트림
   - 데이터 변경 시 자동 업데이트
   - 최신순 정렬

3. **getTotalAmountByDate()**: 날짜별 총량
   - `COALESCE`: NULL을 0으로 처리
   - `SUM`: 합계 계산

**Flow vs Suspend:**
```kotlin
// Flow: 실시간 업데이트
fun getData(): Flow<List<T>>  // 데이터 변경 시 자동 emit

// Suspend: 일회성 조회
suspend fun getData(): List<T>  // 한 번만 조회
```

#### 5.1.3 WaterDatabase.kt

```kotlin
@Database(
    entities = [WaterRecord::class],
    version = 1,
    exportSchema = false
)
abstract class WaterDatabase : RoomDatabase() {
    abstract fun waterDao(): WaterDao

    companion object {
        @Volatile
        private var INSTANCE: WaterDatabase? = null

        fun getDatabase(context: Context): WaterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WaterDatabase::class.java,
                    "water_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

**역할:**
- Room Database 설정
- Singleton 패턴으로 단일 인스턴스 보장

**주요 구성:**

1. **@Database**: Room 설정
   - `entities`: 포함할 Entity 목록
   - `version`: 데이터베이스 버전
   - `exportSchema`: 스키마 파일 내보내기 여부

2. **Singleton 패턴:**
   ```kotlin
   @Volatile  // 멀티 스레드 안전성
   private var INSTANCE: WaterDatabase? = null

   synchronized(this) {  // 동시 생성 방지
       // 인스턴스 생성
   }
   ```

3. **fallbackToDestructiveMigration():**
   - 마이그레이션 실패 시 데이터 삭제 후 재생성
   - 개발 단계에서 유용

#### 5.1.4 WaterRepository.kt

```kotlin
class WaterRepository(private val waterDao: WaterDao) {

    // 기록 추가
    suspend fun addWaterRecord(amount: Int): Long {
        val record = WaterRecord(amount = amount)
        return waterDao.insert(record)
    }

    // 오늘의 섭취량 (Flow)
    fun getTodayIntake(goalAmount: Int = 2000): Flow<DailyWaterIntake> {
        val today = getCurrentDate()
        return waterDao.getRecordsByDate(today).map { records ->
            DailyWaterIntake(
                date = today,
                totalAmount = records.sumOf { it.amount },
                goalAmount = goalAmount,
                recordCount = records.size
            )
        }
    }

    // 주간 통계
    suspend fun getWeeklyStats(goalAmount: Int = 2000): List<DailyWaterIntake> {
        // 날짜 계산 및 데이터 조회
    }
}
```

**역할:**
- DAO와 ViewModel 사이의 추상화 계층
- 비즈니스 로직 처리
- 데이터 변환

**주요 함수:**

1. **addWaterRecord():**
   - Entity 생성 및 저장
   - ID 반환

2. **getTodayIntake():**
   - Flow 변환 (List → DailyWaterIntake)
   - 실시간 업데이트
   - 비즈니스 모델로 변환

3. **getWeeklyStats():**
   - 7일 치 데이터 집계
   - 빈 날짜는 0으로 채움
   - 통계 계산

**Flow 변환 예시:**
```kotlin
// DAO에서 Flow<List<WaterRecord>> 받음
waterDao.getRecordsByDate(today)
    .map { records ->  // 변환
        DailyWaterIntake(...)
    }
// ViewModel에서 Flow<DailyWaterIntake> 사용
```

---

### 5.2 Domain Layer

#### 5.2.1 DailyWaterIntake.kt

```kotlin
data class DailyWaterIntake(
    val date: String,
    val totalAmount: Int,      // 총 섭취량 (ml)
    val goalAmount: Int = 2000, // 목표량 (ml)
    val recordCount: Int = 0    // 기록 횟수
) {
    // 계산된 속성
    val achievementRate: Float
        get() = if (goalAmount > 0)
            (totalAmount.toFloat() / goalAmount).coerceIn(0f, 1f)
            else 0f

    val achievementPercent: Int
        get() = (achievementRate * 100).toInt()

    val isGoalAchieved: Boolean
        get() = totalAmount >= goalAmount

    val remainingAmount: Int
        get() = (goalAmount - totalAmount).coerceAtLeast(0)
}
```

**역할:**
- 비즈니스 로직 캡슐화
- UI에 필요한 계산된 값 제공
- 순수 Kotlin 클래스 (Android 의존성 없음)

**계산된 속성:**

1. **achievementRate**: 달성률 (0.0 ~ 1.0)
   ```kotlin
   예: 1000ml / 2000ml = 0.5
   ```

2. **achievementPercent**: 달성률 (%)
   ```kotlin
   예: 0.5 * 100 = 50%
   ```

3. **isGoalAchieved**: 목표 달성 여부
   ```kotlin
   예: 2100ml >= 2000ml = true
   ```

4. **remainingAmount**: 남은 양
   ```kotlin
   예: 2000ml - 1300ml = 700ml
   coerceAtLeast(0): 음수 방지
   ```

**Enum 정의:**

```kotlin
enum class BluetoothConnectionState {
    DISCONNECTED,  // 연결 안 됨
    CONNECTING,    // 연결 중
    CONNECTED,     // 연결됨
    ERROR          // 오류
}
```

---

### 5.3 Presentation Layer

#### 5.3.1 BluetoothManager.kt

```kotlin
class BluetoothManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    // 상태 Flow
    private val _connectionState = MutableStateFlow(BluetoothConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    private val _currentWeight = MutableStateFlow(0f)
    val currentWeight: StateFlow<Float> = _currentWeight.asStateFlow()

    private val _drinkAmount = MutableStateFlow<Float?>(null)
    val drinkAmount: StateFlow<Float?> = _drinkAmount.asStateFlow()
}
```

**역할:**
- 블루투스 통신 전체 관리
- 데이터 수신 및 파싱
- 명령 전송

**핵심 구성 요소:**

1. **StateFlow**: 상태 관리
   ```kotlin
   MutableStateFlow: 내부에서 값 변경
   StateFlow: 외부에서 읽기만 가능
   ```

2. **BluetoothSocket**: 통신 소켓
3. **InputStream/OutputStream**: 데이터 입출력

**주요 함수 (상세):**

##### connect() - 디바이스 연결

```kotlin
@SuppressLint("MissingPermission")
suspend fun connect(device: BluetoothDevice) = withContext(Dispatchers.IO) {
    try {
        _connectionState.value = BluetoothConnectionState.CONNECTING

        // 1. 소켓 생성
        bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID_SPP)

        // 2. 연결
        bluetoothSocket?.connect()

        // 3. 스트림 초기화
        inputStream = bluetoothSocket?.inputStream
        outputStream = bluetoothSocket?.outputStream

        _connectionState.value = BluetoothConnectionState.CONNECTED

        // 4. 데이터 수신 시작
        startReading()

    } catch (e: IOException) {
        _connectionState.value = BluetoothConnectionState.ERROR
        disconnect()
    }
}
```

**동작 과정:**
1. 상태를 CONNECTING으로 변경
2. SPP(Serial Port Profile) UUID로 소켓 생성
3. 소켓 연결
4. 입출력 스트림 초기화
5. 상태를 CONNECTED로 변경
6. 데이터 읽기 시작

##### startReading() - 데이터 수신

```kotlin
private fun startReading() {
    readJob = scope.launch {
        val buffer = ByteArray(1024)
        val stringBuilder = StringBuilder()

        try {
            while (isActive && _connectionState.value == BluetoothConnectionState.CONNECTED) {
                // 1. 데이터 읽기
                val bytes = inputStream?.read(buffer)
                if (bytes != null && bytes > 0) {
                    val data = String(buffer, 0, bytes)
                    stringBuilder.append(data)

                    // 2. 줄바꿈으로 메시지 분리
                    val messages = stringBuilder.toString().split("\n")
                    stringBuilder.clear()

                    // 3. 마지막 불완전 메시지 처리
                    if (!data.endsWith("\n")) {
                        stringBuilder.append(messages.last())
                    }

                    // 4. 완전한 메시지만 파싱
                    val completeMessages = if (data.endsWith("\n")) {
                        messages
                    } else {
                        messages.dropLast(1)
                    }

                    // 5. 각 메시지 파싱
                    completeMessages.forEach { message ->
                        if (message.isNotBlank()) {
                            parseMessage(message.trim())
                        }
                    }
                }
            }
        } catch (e: IOException) {
            _connectionState.value = BluetoothConnectionState.ERROR
            disconnect()
        }
    }
}
```

**데이터 수신 프로세스:**

1. **버퍼 읽기**: InputStream에서 바이트 읽기
2. **문자열 변환**: 바이트 → String
3. **누적**: StringBuilder에 누적
4. **메시지 분리**: 줄바꿈(`\n`)으로 분리
5. **불완전 메시지 처리**: 마지막 부분 보관
6. **파싱**: 완전한 메시지만 파싱

**버퍼링 예시:**

```
수신 1: "W:250.5\nD"  → "W:250.5" 파싱, "D" 보관
수신 2: ":50.0\n"     → "D:50.0" 파싱
```

##### parseMessage() - 메시지 파싱

```kotlin
private fun parseMessage(message: String) {
    when {
        message.startsWith(CMD_WEIGHT) -> {
            // "W:250.5" → 250.5
            val weight = message.substring(CMD_WEIGHT.length).toFloatOrNull()
            if (weight != null) {
                _currentWeight.value = weight
            }
        }

        message.startsWith(CMD_DRINK) -> {
            // "D:50.0" → 50.0
            val amount = message.substring(CMD_DRINK.length).toFloatOrNull()
            if (amount != null) {
                _drinkAmount.value = amount

                // 이벤트 처리 후 리셋
                scope.launch {
                    delay(1000)
                    _drinkAmount.value = null
                }
            }
        }

        message.startsWith(CMD_STATUS) -> {
            // "S:READY" → "READY"
            val status = message.substring(CMD_STATUS.length)
            _statusMessage.value = status
        }
    }
}
```

**프로토콜 파싱:**

| 명령 | 형식 | 파싱 결과 | StateFlow 업데이트 |
|-----|------|----------|-------------------|
| W: | `W:250.5\n` | 250.5 | `_currentWeight` |
| D: | `D:50.0\n` | 50.0 | `_drinkAmount` |
| S: | `S:READY\n` | "READY" | `_statusMessage` |

**이벤트 처리:**
- `drinkAmount`는 1초 후 자동으로 null로 리셋
- 이유: 다음 이벤트 감지를 위해

##### sendCommand() - 명령 전송

```kotlin
private suspend fun sendCommand(command: String) = withContext(Dispatchers.IO) {
    try {
        outputStream?.write("$command\n".toByteArray())
        outputStream?.flush()
    } catch (e: IOException) {
        _connectionState.value = BluetoothConnectionState.ERROR
    }
}

// 영점 조정
suspend fun sendTareCommand() {
    sendCommand("T")
}

// 리셋
suspend fun sendResetCommand() {
    sendCommand("R")
}
```

**명령 전송 과정:**
1. 명령 문자열에 `\n` 추가
2. 바이트 배열로 변환
3. OutputStream에 쓰기
4. 버퍼 비우기 (flush)

---

#### 5.3.2 HomeViewModel.kt

```kotlin
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothManager = BluetoothManager(application)
    private val repository: WaterRepository

    init {
        val database = WaterDatabase.getDatabase(application)
        repository = WaterRepository(database.waterDao())
    }

    // StateFlow 노출
    val connectionState: StateFlow<BluetoothConnectionState> = bluetoothManager.connectionState
    val currentWeight: StateFlow<Float> = bluetoothManager.currentWeight
    val todayIntake: StateFlow<DailyWaterIntake> = repository.getTodayIntake(_goalAmount.value)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DailyWaterIntake.empty(...)
        )

    // 이벤트 처리
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()
}
```

**역할:**
- UI 상태 관리
- 비즈니스 로직 호출
- 이벤트 처리

**StateFlow vs SharedFlow:**

```kotlin
// StateFlow: 상태 (항상 값이 있음)
val currentWeight: StateFlow<Float>
// 구독 즉시 현재 값 전달

// SharedFlow: 이벤트 (일회성)
val uiEvent: SharedFlow<UiEvent>
// 발생 시에만 전달, 구독 전 이벤트는 놓침
```

**StateFlow 변환 (stateIn):**

```kotlin
repository.getTodayIntake(_goalAmount.value)  // Flow<DailyWaterIntake>
    .stateIn(
        scope = viewModelScope,        // ViewModel 생명주기
        started = SharingStarted.WhileSubscribed(5000),  // 구독 전략
        initialValue = DailyWaterIntake.empty(...)       // 초기값
    )  // → StateFlow<DailyWaterIntake>
```

**SharingStarted 전략:**
- `WhileSubscribed(5000)`: 구독자가 있는 동안만 활성화
- `5000ms`: 마지막 구독자가 떠난 후 5초간 유지
- 메모리 최적화

**자동 기록 로직:**

```kotlin
init {
    // BluetoothManager의 drinkAmount 감지
    viewModelScope.launch {
        bluetoothManager.drinkAmount.collect { amount ->
            if (amount != null && amount > 0) {
                // 자동으로 기록 추가
                addWaterRecord(amount.toInt())
            }
        }
    }
}
```

**동작 과정:**
1. BluetoothManager에서 `D:50.0` 수신
2. `drinkAmount` StateFlow가 50.0으로 업데이트
3. ViewModel에서 collect로 감지
4. `addWaterRecord(50)` 호출
5. Repository를 통해 데이터베이스에 저장
6. Room의 Flow가 자동으로 UI 업데이트

**함수 구현:**

##### connectToDevice()

```kotlin
fun connectToDevice(device: BluetoothDevice) {
    viewModelScope.launch {
        try {
            bluetoothManager.connect(device)
            _uiEvent.emit(UiEvent.ShowMessage("${device.name}에 연결되었습니다"))
        } catch (e: Exception) {
            _uiEvent.emit(UiEvent.ShowError("연결 실패: ${e.message}"))
        }
    }
}
```

**특징:**
- `viewModelScope.launch`: ViewModel 생명주기에 맞춰 실행
- `_uiEvent.emit()`: 일회성 이벤트 발생
- try-catch: 예외 처리

##### addWaterRecord()

```kotlin
fun addWaterRecord(amount: Int) {
    viewModelScope.launch {
        try {
            repository.addWaterRecord(amount)
            _uiEvent.emit(UiEvent.RecordAdded(amount))
        } catch (e: Exception) {
            _uiEvent.emit(UiEvent.ShowError("기록 추가 실패: ${e.message}"))
        }
    }
}
```

**자동 UI 업데이트:**
```
addWaterRecord(50)
    ↓
Repository.insert()
    ↓
Room Database 변경
    ↓
Flow 자동 emit
    ↓
StateFlow 업데이트
    ↓
Compose recomposition
    ↓
UI 자동 갱신
```

---

#### 5.3.3 HomeScreen.kt (Compose UI)

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    // State 구독
    val connectionState by viewModel.connectionState.collectAsState()
    val currentWeight by viewModel.currentWeight.collectAsState()
    val todayIntake by viewModel.todayIntake.collectAsState()

    // 로컬 State
    var showDeviceDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                // ...
            }
        }
    }

    Scaffold(
        topBar = { ... },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column { ... }
    }
}
```

**Compose 핵심 개념:**

##### collectAsState()

```kotlin
val currentWeight by viewModel.currentWeight.collectAsState()
```

**동작:**
1. StateFlow를 State로 변환
2. 값 변경 시 자동으로 recomposition 트리거
3. `by` 키워드로 자동 unwrap

**recomposition:**
```
StateFlow 값 변경
    ↓
collectAsState() 감지
    ↓
Composable 함수 재실행
    ↓
UI 업데이트
```

##### remember

```kotlin
var showDeviceDialog by remember { mutableStateOf(false) }
```

**역할:**
- recomposition 간에 값 유지
- `mutableStateOf`: 변경 가능한 상태 생성
- recomposition 트리거

**without remember:**
```kotlin
var value = false  // recomposition마다 false로 리셋됨
```

**with remember:**
```kotlin
var value by remember { mutableStateOf(false) }  // 값 유지됨
```

##### LaunchedEffect

```kotlin
LaunchedEffect(Unit) {  // key: Unit (한 번만 실행)
    viewModel.uiEvent.collect { event ->
        // 이벤트 처리
    }
}
```

**역할:**
- Composable에서 suspend 함수 실행
- key 변경 시 재실행
- Composable 제거 시 자동 취소

**key 변경:**
```kotlin
LaunchedEffect(userId) {  // userId 변경 시마다 실행
    loadUserData(userId)
}
```

##### Scaffold

```kotlin
Scaffold(
    topBar = { TopAppBar(...) },
    snackbarHost = { SnackbarHost(snackbarHostState) }
) { paddingValues ->
    // Content
}
```

**역할:**
- Material Design 레이아웃 구조
- TopBar, BottomBar, FAB, Snackbar 자동 배치
- paddingValues로 겹침 방지

**UI 컴포넌트:**

##### BluetoothConnectionCard

```kotlin
@Composable
fun BluetoothConnectionCard(
    connectionState: BluetoothConnectionState,
    connectedDeviceName: String?,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onTareClick: () -> Unit
) {
    Card {
        Column {
            // 연결 상태 표시
            ConnectionStatusChip(connectionState)

            // 조건부 렌더링
            if (connectionState == BluetoothConnectionState.CONNECTED) {
                Button(onClick = onDisconnectClick) { Text("연결 해제") }
                Button(onClick = onTareClick) { Text("영점 조정") }
            } else {
                Button(onClick = onConnectClick) { Text("디바이스 연결") }
            }
        }
    }
}
```

**조건부 렌더링:**
```kotlin
if (condition) {
    // 조건 참일 때만 렌더링
} else {
    // 조건 거짓일 때 렌더링
}
```

##### TodayIntakeCard

```kotlin
@Composable
fun TodayIntakeCard(
    intake: DailyWaterIntake,
    onAddClick: () -> Unit
) {
    Card {
        Column {
            // 진행률 바
            LinearProgressIndicator(
                progress = { intake.achievementRate }
            )

            // 섭취량 표시
            Text("${intake.totalAmount} / ${intake.goalAmount} ml")

            // 달성률
            Text("${intake.achievementPercent}%")

            // 조건부 메시지
            if (!intake.isGoalAchieved) {
                Text("목표까지 ${intake.remainingAmount}ml 남았습니다")
            } else {
                Text("오늘의 목표를 달성했습니다!")
            }
        }
    }
}
```

**반응형 UI:**
```
intake.achievementRate 변경
    ↓
LinearProgressIndicator 자동 업데이트
    ↓
애니메이션 효과
```

##### DeviceSelectionDialog

```kotlin
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
            LazyColumn {
                items(devices) { device ->
                    TextButton(
                        onClick = { onDeviceSelected(device) }
                    ) {
                        Text(device.name ?: device.address)
                    }
                }
            }
        }
    )
}
```

**LazyColumn:**
- RecyclerView의 Compose 버전
- 필요한 만큼만 렌더링 (성능 최적화)
- `items()`: 리스트 항목 렌더링

---

## 6. 주요 컴포넌트 동작 원리

### 6.1 블루투스 연결 흐름

```
[사용자]
   ↓ "디바이스 연결" 버튼 클릭
[HomeScreen]
   ↓ onConnectClick()
[HomeViewModel]
   ↓ connectToDevice(device)
[BluetoothManager]
   ↓ connect(device)
[Android Bluetooth API]
   ↓ createRfcommSocketToServiceRecord()
   ↓ socket.connect()
[HC-05/06]
   ↓ 연결 성공
[BluetoothManager]
   ↓ _connectionState.value = CONNECTED
   ↓ startReading()
[HomeViewModel]
   ↓ connectionState Flow 업데이트
[HomeScreen]
   ↓ collectAsState() 감지
   ↓ Recomposition
[UI]
   ↓ "연결됨" 표시
```

### 6.2 자동 섭취 감지 흐름

```
[Arduino]
   ↓ 무게 변화 감지 (250g → 200g)
   ↓ "D:50.0\n" 전송
[HC-05/06]
   ↓ Bluetooth 전송
[BluetoothManager]
   ↓ inputStream.read()
   ↓ parseMessage("D:50.0")
   ↓ _drinkAmount.value = 50.0
[HomeViewModel]
   ↓ drinkAmount.collect { 50.0 }
   ↓ addWaterRecord(50)
[WaterRepository]
   ↓ WaterRecord(amount=50) 생성
   ↓ waterDao.insert(record)
[Room Database]
   ↓ INSERT INTO water_records ...
   ↓ Flow 자동 emit
[WaterRepository]
   ↓ getTodayIntake() Flow 업데이트
[HomeViewModel]
   ↓ todayIntake StateFlow 업데이트
[HomeScreen]
   ↓ collectAsState() 감지
   ↓ Recomposition
[UI]
   ↓ 섭취량 자동 업데이트 (0ml → 50ml)
   ↓ 진행률 바 애니메이션
   ↓ Snackbar "50ml 기록됨"
```

### 6.3 데이터베이스 읽기 흐름

```
[HomeViewModel 초기화]
   ↓
[Repository.getTodayIntake()]
   ↓
[WaterDao.getRecordsByDate(today)]
   ↓ Flow<List<WaterRecord>>
[Repository]
   ↓ .map { records -> DailyWaterIntake(...) }
   ↓ Flow<DailyWaterIntake>
[HomeViewModel]
   ↓ .stateIn(...) → StateFlow
[HomeScreen]
   ↓ collectAsState()
   ↓ State<DailyWaterIntake>
[UI]
   ↓ 초기 렌더링

// 데이터 변경 시
[새 기록 추가]
   ↓
[Room Database 변경]
   ↓
[Flow 자동 emit]
   ↓
[Repository map 재실행]
   ↓
[StateFlow 업데이트]
   ↓
[Recomposition]
   ↓
[UI 자동 갱신]
```

---

## 7. 데이터 흐름

### 7.1 단방향 데이터 플로우 (UDF)

```
┌─────────────────────────────────────┐
│           User Action                │
│     (버튼 클릭, 입력 등)              │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│           ViewModel                  │
│   - 이벤트 처리                       │
│   - Repository 호출                  │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│          Repository                  │
│   - 데이터 저장/조회                  │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│         Database / API               │
│   - 실제 데이터 변경                  │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│            Flow                      │
│   - 자동 데이터 emit                  │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│          StateFlow                   │
│   - UI 상태 업데이트                  │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│            Compose                   │
│   - Recomposition                    │
│   - UI 렌더링                         │
└─────────────────────────────────────┘
```

**핵심 원칙:**
- 데이터는 한 방향으로만 흐름
- UI는 상태만 반영 (비즈니스 로직 없음)
- 사용자 액션은 ViewModel로 전달

### 7.2 상태 관리 계층

```
UI Layer (Compose)
  └─ State (by collectAsState)
       └─ StateFlow (읽기 전용)
            └─ MutableStateFlow (ViewModel 내부)
                 └─ Flow (Repository)
                      └─ Room Database Query
```

---

## 8. 블루투스 통신 메커니즘

### 8.1 프로토콜 상세

#### 8.1.1 Arduino → App

| 명령 | 형식 | 설명 | 빈도 |
|-----|------|------|------|
| W: | `W:250.5\n` | 현재 무게 (g) | 500ms마다 |
| D: | `D:50.0\n` | 마신 양 (ml) | 감지 시 |
| S: | `S:READY\n` | 상태 메시지 | 이벤트 시 |

**전송 예시:**
```
W:0.0
W:0.5
W:0.2
S:CUP_ON
W:253.5
W:253.2
D:50.0
W:203.1
```

#### 8.1.2 App → Arduino

| 명령 | 형식 | 설명 | 응답 |
|-----|------|------|------|
| T | `T\n` | 영점 조정 | `S:TARING\nS:TARE_OK\n` |
| R | `R\n` | 시스템 리셋 | `S:RESETTING\nS:RESET_OK\n` |

### 8.2 버퍼링 및 파싱 전략

#### 문제: 데이터 분할 수신

```
전송: "W:250.5\nD:50.0\n"

실제 수신:
수신 1: "W:25"
수신 2: "0.5\nD:"
수신 3: "50.0\n"
```

#### 해결: StringBuilder + 줄바꿈 분리

```kotlin
val stringBuilder = StringBuilder()

while (true) {
    val data = readFromStream()  // "W:25"
    stringBuilder.append(data)    // "W:25"

    if (data.contains("\n")) {
        val messages = stringBuilder.split("\n")
        // 완전한 메시지만 처리
        // 불완전한 마지막 부분은 보관
    }
}
```

#### 상태 머신

```
State 0: 대기
  ↓ "W" 수신
State 1: 명령 읽기중
  ↓ ":" 수신
State 2: 데이터 읽기중
  ↓ "\n" 수신
State 3: 파싱
  ↓
State 0: 대기
```

### 8.3 에러 처리

```kotlin
try {
    inputStream?.read(buffer)
} catch (e: IOException) {
    // 연결 끊김
    _connectionState.value = BluetoothConnectionState.ERROR
    disconnect()
}
```

**발생 가능한 에러:**
- IOException: 연결 끊김
- SecurityException: 권한 없음
- NullPointerException: 소켓/스트림 null

---

## 9. UI/UX 구현

### 9.1 Compose 구조

```
HomeScreen
├─ Scaffold
│  ├─ TopAppBar
│  │  └─ "물 섭취량 모니터"
│  ├─ SnackbarHost
│  └─ Column
│     ├─ BluetoothConnectionCard
│     │  ├─ ConnectionStatusChip
│     │  ├─ Button (연결/해제)
│     │  └─ Button (영점 조정)
│     ├─ CurrentWeightCard
│     │  └─ Text (무게 표시)
│     └─ TodayIntakeCard
│        ├─ LinearProgressIndicator
│        ├─ Text (섭취량/목표량)
│        └─ IconButton (수동 추가)
└─ Dialog (조건부)
   ├─ DeviceSelectionDialog
   └─ AddWaterDialog
```

### 9.2 Material3 디자인

#### Color Scheme

```kotlin
// Light Theme
primary = Color(0xFF1976D2)      // 파란색
secondary = Color(0xFF00ACC1)    // 청록색
tertiary = Color(0xFF388E3C)     // 초록색

// Dark Theme
primary = Color(0xFF90CAF9)      // 밝은 파란색
secondary = Color(0xFF80DEEA)    // 밝은 청록색
tertiary = Color(0xFFA5D6A7)     // 밝은 초록색
```

#### Typography

```kotlin
bodyLarge = TextStyle(
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.5.sp
)

titleLarge = TextStyle(
    fontSize = 22.sp,
    lineHeight = 28.sp
)
```

### 9.3 애니메이션

#### LinearProgressIndicator

```kotlin
LinearProgressIndicator(
    progress = { intake.achievementRate }  // 0.0 ~ 1.0
)
```

**애니메이션:**
- 진행률 변경 시 자동 애니메이션
- Material Design 기본 애니메이션 적용

#### Snackbar

```kotlin
snackbarHostState.showSnackbar("50ml 기록됨")
```

**애니메이션:**
- 하단에서 슬라이드 업
- 일정 시간 후 자동 사라짐

---

## 10. 핵심 기능 구현 분석

### 10.1 실시간 무게 표시

**코드 경로:**
```
BluetoothManager.startReading()
    → parseMessage("W:250.5")
    → _currentWeight.value = 250.5
    → HomeViewModel.currentWeight (StateFlow)
    → HomeScreen.collectAsState()
    → CurrentWeightCard(currentWeight)
```

**UI 업데이트 주기:**
- Arduino: 500ms마다 전송
- 앱: 수신 즉시 UI 업데이트
- Compose: Recomposition 최적화

**최적화:**
```kotlin
// Recomposition 범위 최소화
@Composable
fun CurrentWeightCard(currentWeight: Float) {
    // 이 Composable만 recompose
    Text("%.1f g".format(currentWeight))
}
```

### 10.2 자동 섭취 감지

**알고리즘 (Arduino):**

```cpp
1. 컵 감지 (10g 이상)
2. 기준 무게 설정 (baselineWeight)
3. 현재 무게 모니터링
4. 무게 감소 감지 (임계값 5g 이상)
5. 안정화 확인 (5회 연속)
6. 마신 양 계산 및 전송
```

**앱 처리:**

```kotlin
// 1. 데이터 수신
parseMessage("D:50.0")
_drinkAmount.value = 50.0

// 2. ViewModel에서 감지
bluetoothManager.drinkAmount.collect { amount ->
    if (amount != null && amount > 0) {
        addWaterRecord(amount.toInt())
    }
}

// 3. 데이터베이스 저장
repository.addWaterRecord(50)

// 4. Flow 자동 업데이트
todayIntake StateFlow 변경

// 5. UI 자동 갱신
TodayIntakeCard recomposition
```

**타이밍 다이어그램:**

```
t=0ms    Arduino: 무게 감소 감지
t=10ms   Arduino: "D:50.0\n" 전송
t=20ms   App: 데이터 수신
t=25ms   App: parseMessage()
t=30ms   App: StateFlow 업데이트
t=35ms   App: addWaterRecord() 시작
t=50ms   App: Database INSERT
t=55ms   App: Flow emit
t=60ms   App: UI Recomposition
t=65ms   App: Snackbar 표시
```

### 10.3 데이터 영구 저장

**저장 프로세스:**

```kotlin
// 1. ViewModel
fun addWaterRecord(amount: Int) {
    viewModelScope.launch {  // 코루틴 실행
        repository.addWaterRecord(amount)
    }
}

// 2. Repository
suspend fun addWaterRecord(amount: Int): Long {
    val record = WaterRecord(
        amount = amount,
        timestamp = System.currentTimeMillis(),
        date = getCurrentDate()
    )
    return waterDao.insert(record)
}

// 3. DAO
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insert(record: WaterRecord): Long

// 4. Room이 자동으로 SQL 생성
INSERT INTO water_records (amount, timestamp, date) VALUES (50, 1704700000000, '2025-01-08')
```

**데이터 무결성:**
```kotlin
@Transaction  // Room에서 자동 지원
suspend fun complexOperation() {
    // 여러 작업을 원자적으로 실행
    insert(...)
    update(...)
    // 하나라도 실패하면 모두 롤백
}
```

### 10.4 목표 달성률 계산

**DailyWaterIntake 모델:**

```kotlin
data class DailyWaterIntake(
    val totalAmount: Int = 1300,  // 현재 섭취량
    val goalAmount: Int = 2000     // 목표량
) {
    val achievementRate: Float
        get() = (totalAmount.toFloat() / goalAmount).coerceIn(0f, 1f)

    val achievementPercent: Int
        get() = (achievementRate * 100).toInt()

    val remainingAmount: Int
        get() = (goalAmount - totalAmount).coerceAtLeast(0)
}
```

**계산 예시:**

```kotlin
val intake = DailyWaterIntake(
    totalAmount = 1300,
    goalAmount = 2000
)

intake.achievementRate    // 0.65 (1300 / 2000)
intake.achievementPercent // 65 (0.65 * 100)
intake.remainingAmount    // 700 (2000 - 1300)
```

**UI 반영:**

```kotlin
// 진행률 바
LinearProgressIndicator(
    progress = { intake.achievementRate }  // 0.65 → 65% 채움
)

// 텍스트
Text("${intake.totalAmount} / ${intake.goalAmount} ml")  // "1300 / 2000 ml"
Text("${intake.achievementPercent}%")  // "65%"
```

---

## 11. 코드 예시 및 분석

### 11.1 StateFlow 사용 예시

```kotlin
// BluetoothManager
class BluetoothManager {
    // Private MutableStateFlow (내부에서만 수정)
    private val _connectionState = MutableStateFlow(BluetoothConnectionState.DISCONNECTED)

    // Public StateFlow (외부에서 읽기만)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    // 내부에서 값 변경
    fun connect() {
        _connectionState.value = BluetoothConnectionState.CONNECTING
        // ...
        _connectionState.value = BluetoothConnectionState.CONNECTED
    }
}

// HomeViewModel
class HomeViewModel {
    // BluetoothManager의 StateFlow를 그대로 노출
    val connectionState: StateFlow<BluetoothConnectionState> = bluetoothManager.connectionState
}

// HomeScreen (Compose)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    // StateFlow를 State로 변환
    val connectionState by viewModel.connectionState.collectAsState()

    // 값 사용
    when (connectionState) {
        BluetoothConnectionState.CONNECTED -> Text("연결됨")
        BluetoothConnectionState.CONNECTING -> Text("연결 중")
        // ...
    }
}
```

**흐름:**
```
BluetoothManager._connectionState (MutableStateFlow)
    ↓ value 변경
BluetoothManager.connectionState (StateFlow)
    ↓ 변경 전파
HomeViewModel.connectionState (StateFlow)
    ↓ collectAsState()
HomeScreen.connectionState (State)
    ↓ Recomposition
UI 업데이트
```

### 11.2 Flow 변환 예시

```kotlin
// DAO
@Query("SELECT * FROM water_records WHERE date = :date")
fun getRecordsByDate(date: String): Flow<List<WaterRecord>>

// Repository
fun getTodayIntake(goalAmount: Int): Flow<DailyWaterIntake> {
    val today = getCurrentDate()

    return waterDao.getRecordsByDate(today)  // Flow<List<WaterRecord>>
        .map { records ->  // 변환
            DailyWaterIntake(
                date = today,
                totalAmount = records.sumOf { it.amount },
                goalAmount = goalAmount,
                recordCount = records.size
            )
        }  // Flow<DailyWaterIntake>
}

// ViewModel
val todayIntake: StateFlow<DailyWaterIntake> =
    repository.getTodayIntake(2000)  // Flow<DailyWaterIntake>
        .stateIn(  // Flow → StateFlow 변환
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DailyWaterIntake.empty("2025-01-08", 2000)
        )
```

**변환 과정:**

```
Database: List<WaterRecord>
    ↓ Flow<List<WaterRecord>>
Repository: map 변환
    ↓ Flow<DailyWaterIntake>
ViewModel: stateIn
    ↓ StateFlow<DailyWaterIntake>
Compose: collectAsState
    ↓ State<DailyWaterIntake>
UI: 렌더링
```

### 11.3 Compose 상태 관리 예시

```kotlin
@Composable
fun HomeScreen() {
    // ViewModel에서 StateFlow 구독
    val todayIntake by viewModel.todayIntake.collectAsState()

    // 로컬 State (다이얼로그 표시 여부)
    var showDialog by remember { mutableStateOf(false) }

    // UI
    Column {
        // todayIntake 값 사용
        Text("${todayIntake.totalAmount} ml")

        // 버튼 클릭 시 로컬 상태 변경
        Button(onClick = { showDialog = true }) {
            Text("기록 추가")
        }
    }

    // 조건부 렌더링
    if (showDialog) {
        AddWaterDialog(
            onConfirm = { amount ->
                viewModel.addWaterRecord(amount)  // ViewModel 호출
                showDialog = false  // 다이얼로그 닫기
            },
            onDismiss = { showDialog = false }
        )
    }
}
```

**상태 변경 흐름:**

```
1. 사용자가 "기록 추가" 버튼 클릭
    ↓
2. showDialog = true
    ↓
3. Recomposition
    ↓
4. AddWaterDialog 렌더링
    ↓
5. 사용자가 100ml 입력 후 확인
    ↓
6. viewModel.addWaterRecord(100) 호출
    ↓
7. Repository → Database 저장
    ↓
8. Flow 자동 emit
    ↓
9. todayIntake StateFlow 업데이트
    ↓
10. Recomposition (자동)
    ↓
11. Text 업데이트 (0ml → 100ml)
```

### 11.4 코루틴 사용 예시

```kotlin
// ViewModel
class HomeViewModel : ViewModel() {

    // viewModelScope: ViewModel 생명주기에 맞춰 자동 취소
    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch {  // 코루틴 시작
            try {
                // suspend 함수 호출
                bluetoothManager.connect(device)

                // UI 스레드에서 실행 (Dispatchers.Main)
                _uiEvent.emit(UiEvent.ShowMessage("연결 성공"))

            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("연결 실패"))
            }
        }
    }

    // init 블록에서도 사용
    init {
        viewModelScope.launch {
            bluetoothManager.drinkAmount.collect { amount ->
                if (amount != null) {
                    addWaterRecord(amount.toInt())
                }
            }
        }
    }
}

// BluetoothManager
class BluetoothManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // suspend 함수
    suspend fun connect(device: BluetoothDevice) = withContext(Dispatchers.IO) {
        // IO 작업
        bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID_SPP)
        bluetoothSocket?.connect()

        // 백그라운드 작업 시작
        startReading()
    }

    private fun startReading() {
        scope.launch {  // IO 스레드에서 실행
            while (isActive) {
                val bytes = inputStream?.read(buffer)
                // 데이터 처리
            }
        }
    }
}
```

**Dispatchers:**

```kotlin
Dispatchers.Main    // UI 스레드
Dispatchers.IO      // 네트워크, 파일 I/O
Dispatchers.Default // CPU 집약적 작업
```

**생명주기:**

```
ViewModel 생성
    ↓
viewModelScope 생성
    ↓
launch { ... } 시작
    ↓
ViewModel onCleared()
    ↓
viewModelScope 자동 취소
    ↓
모든 코루틴 중지
```

---

## 12. 확장 포인트

### 12.1 Phase 2 구현 가이드

#### 통계 화면 추가

```kotlin
// 1. ViewModel 추가
class StatisticsViewModel : ViewModel() {
    val weeklyStats: StateFlow<List<DailyWaterIntake>>
    val averageIntake: StateFlow<Float>
}

// 2. Screen 추가
@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel) {
    // 그래프 라이브러리 사용 (MPAndroidChart)
}

// 3. Navigation 추가
NavHost {
    composable("home") { HomeScreen() }
    composable("statistics") { StatisticsScreen() }
}
```

#### 알림 기능 추가

```kotlin
// 1. WorkManager 설정
class WaterReminderWorker : Worker() {
    override fun doWork(): Result {
        showNotification("물 마실 시간입니다!")
        return Result.success()
    }
}

// 2. 스케줄링
WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "water_reminder",
    ExistingPeriodicWorkPolicy.KEEP,
    PeriodicWorkRequestBuilder<WaterReminderWorker>(1, TimeUnit.HOURS).build()
)

// 3. NotificationHelper
class NotificationHelper(context: Context) {
    fun showNotification(message: String) {
        // 알림 생성
    }
}
```

### 12.2 확장 가능한 구조

#### Repository 패턴 활용

```kotlin
// 인터페이스 정의
interface WaterDataSource {
    fun getTodayIntake(): Flow<DailyWaterIntake>
    suspend fun addRecord(amount: Int)
}

// Local 구현
class LocalWaterDataSource(private val dao: WaterDao) : WaterDataSource {
    // Room 사용
}

// Remote 구현 (향후)
class RemoteWaterDataSource(private val api: ApiService) : WaterDataSource {
    // 클라우드 동기화
}

// Repository는 추상화 사용
class WaterRepository(
    private val localDataSource: WaterDataSource,
    private val remoteDataSource: WaterDataSource? = null
) {
    // 필요에 따라 local 또는 remote 사용
}
```

#### ViewModel 확장

```kotlin
// BaseViewModel
abstract class BaseViewModel : ViewModel() {
    protected val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    protected suspend fun emitEvent(event: UiEvent) {
        _uiEvent.emit(event)
    }
}

// 상속
class HomeViewModel : BaseViewModel() {
    // 공통 기능 사용
}
```

### 12.3 테스트 작성 가이드

```kotlin
// Repository 테스트
class WaterRepositoryTest {
    private lateinit var repository: WaterRepository
    private lateinit var dao: WaterDao

    @Test
    fun `addWaterRecord should insert into database`() = runTest {
        // Given
        val amount = 100

        // When
        val id = repository.addWaterRecord(amount)

        // Then
        val record = dao.getById(id)
        assertEquals(100, record?.amount)
    }
}

// ViewModel 테스트
class HomeViewModelTest {
    @Test
    fun `connectToDevice should update connectionState`() = runTest {
        // Given
        val viewModel = HomeViewModel(application)
        val device = mockBluetoothDevice

        // When
        viewModel.connectToDevice(device)

        // Then
        assertEquals(BluetoothConnectionState.CONNECTED, viewModel.connectionState.value)
    }
}

// Compose UI 테스트
@Test
fun homeScreen_displays_todayIntake() {
    composeTestRule.setContent {
        HomeScreen(viewModel = mockViewModel)
    }

    composeTestRule
        .onNodeWithText("0 / 2000 ml")
        .assertIsDisplayed()
}
```

---

## 13. 성능 최적화

### 13.1 Compose Recomposition 최적화

```kotlin
// Bad: 전체 recompose
@Composable
fun Screen(viewModel: HomeViewModel) {
    val state by viewModel.state.collectAsState()

    Column {
        Header(state.title)  // state 변경 시마다 recompose
        Content(state.data)  // state 변경 시마다 recompose
    }
}

// Good: 필요한 부분만 recompose
@Composable
fun Screen(viewModel: HomeViewModel) {
    Column {
        Header(viewModel.title.collectAsState().value)  // title 변경 시만
        Content(viewModel.data.collectAsState().value)  // data 변경 시만
    }
}
```

### 13.2 데이터베이스 쿼리 최적화

```kotlin
// Bad: N+1 쿼리
suspend fun getDailyStats(dates: List<String>): List<DailyIntake> {
    return dates.map { date ->
        val records = dao.getRecordsByDateOnce(date)  // N번 쿼리
        // ...
    }
}

// Good: 단일 쿼리
suspend fun getDailyStats(startDate: String, endDate: String): List<DailyIntake> {
    val allRecords = dao.getRecordsByDateRange(startDate, endDate)  // 1번 쿼리
    return allRecords.groupBy { it.date }.map { (date, records) ->
        // ...
    }
}
```

### 13.3 메모리 관리

```kotlin
// ViewModel
override fun onCleared() {
    super.onCleared()
    bluetoothManager.cleanup()  // 리소스 정리
}

// BluetoothManager
fun cleanup() {
    scope.cancel()  // 코루틴 취소
    disconnect()    // 소켓 닫기
}
```

---

## 14. 보안 고려사항

### 14.1 권한 처리

```kotlin
// AndroidManifest.xml에서 선언
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

// 런타임 권한 요청
val permissionState = rememberMultiplePermissionsState(
    listOf(Manifest.permission.BLUETOOTH_CONNECT)
)

if (!permissionState.allPermissionsGranted) {
    permissionState.launchMultiplePermissionRequest()
}
```

### 14.2 데이터 보호

```kotlin
// Room Database 암호화 (선택사항)
val passphrase = SQLiteDatabase.getBytes("my-secret-key".toCharArray())
val factory = SupportFactory(passphrase)

Room.databaseBuilder(context, WaterDatabase::class.java, "water_db")
    .openHelperFactory(factory)
    .build()
```

### 14.3 블루투스 보안

```kotlin
// 페어링된 디바이스만 연결
val pairedDevices = bluetoothAdapter.bondedDevices
// 신뢰할 수 있는 디바이스만 목록에 표시
```

---

## 15. 디버깅 팁

### 15.1 로그 활용

```kotlin
private const val TAG = "HomeViewModel"

fun connectToDevice(device: BluetoothDevice) {
    Log.d(TAG, "Connecting to ${device.name}")

    viewModelScope.launch {
        try {
            bluetoothManager.connect(device)
            Log.d(TAG, "Connected successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
        }
    }
}
```

### 15.2 Database Inspector

```
Android Studio → View → Tool Windows → App Inspection
→ Database → water_database → water_records
```

### 15.3 Layout Inspector

```
Tools → Layout Inspector
→ Compose 계층 구조 확인
→ Recomposition 횟수 확인
```

---

## 16. 마무리

### 16.1 코드 구조 요약

```
아키텍처: MVVM + Clean Architecture + Repository
UI: Jetpack Compose + Material3
데이터: Room Database + Flow
통신: Bluetooth Classic (SPP)
비동기: Kotlin Coroutines + StateFlow
```

### 16.2 핵심 원칙

1. **관심사 분리**: 각 계층의 책임 명확화
2. **단방향 데이터 플로우**: UI → ViewModel → Repository
3. **반응형 프로그래밍**: Flow/StateFlow 활용
4. **생명주기 인식**: ViewModel, Compose Lifecycle
5. **테스트 가능성**: 의존성 주입, 추상화

### 16.3 학습 포인트

- **Kotlin**: Coroutines, Flow, StateFlow
- **Android**: MVVM, Room, Bluetooth
- **Compose**: 선언형 UI, State 관리
- **Architecture**: Clean Architecture, Repository 패턴

---

**이 문서를 통해 aduino1 앱의 전체 코드 구조와 동작 원리를 이해하셨기를 바랍니다!**

---

*작성: 2025-01-08*
*버전: 1.0*
*대상: 중급~고급 개발자*
