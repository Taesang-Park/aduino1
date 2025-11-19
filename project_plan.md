# 물 섭취량 모니터링 시스템 개발 계획

## 📋 프로젝트 개요
로드셀과 아두이노를 사용하여 컵의 물 무게를 측정하고, 블루투스로 안드로이드 앱에 전송하여 하루 섭취량을 추적하는 시스템

---

## 시스템 구성도

```
로드셀 (Load Cell)
    ↓
HX711 모듈 (ADC 증폭기)
    ↓
Arduino Uno (제어 및 연산)
    ↓
HC-05/06 (블루투스 모듈)
    ↓
안드로이드 앱 (Kotlin)
```

---

## 1️⃣ 시스템 아키텍처 및 데이터 프로토콜

### 하드웨어 구성
- 로드셀 → HX711 모듈 → Arduino Uno → HC-05/06 → 안드로이드 앱

### 통신 프로토콜 설계
```
아두이노 → 앱: "W:250\n"  (현재 무게 250g)
아두이노 → 앱: "D:50\n"   (마신 양 50ml)
앱 → 아두이노: "T\n"      (영점 조정 - Tare)
앱 → 아두이노: "R\n"      (리셋)
```

---

## 2️⃣ 아두이노 개발 (C++)

### 필요 라이브러리
- `HX711.h` (로드셀 데이터 읽기)
- `SoftwareSerial.h` (블루투스 통신)

### 주요 기능
- 로드셀 캘리브레이션
- 무게 변화 감지 (마신 양 계산)
- 블루투스 데이터 전송
- 영점 조정 기능

### 파일 구조
```
arduino/
├── water_monitor.ino
└── config.h
```

---

## 3️⃣ 안드로이드 앱 개발 (Kotlin)

### 기술 스택
- Kotlin
- Jetpack Compose (UI)
- Coroutines + Flow (비동기 처리)
- Room Database (로컬 데이터 저장)
- DataStore (설정 저장)
- MPAndroidChart (그래프)
- WorkManager (백그라운드 알림)

### 앱 구조 (MVVM 패턴)
```
app/
├── data/
│   ├── local/
│   │   ├── database/
│   │   │   ├── WaterDatabase.kt
│   │   │   └── WaterDao.kt
│   │   └── entity/
│   │       └── WaterRecord.kt
│   └── repository/
│       └── WaterRepository.kt
├── domain/
│   └── model/
│       └── DailyWaterIntake.kt
├── presentation/
│   ├── MainActivity.kt
│   ├── bluetooth/
│   │   ├── BluetoothManager.kt
│   │   └── BluetoothViewModel.kt
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   └── statistics/
│       ├── StatisticsScreen.kt
│       └── StatisticsViewModel.kt
└── util/
    └── NotificationHelper.kt
```

---

## 4️⃣ 블루투스 통신 모듈

### 주요 기능
- 블루투스 기기 스캔 및 페어링
- HC-05/06과 연결
- 실시간 데이터 수신
- 연결 상태 모니터링

---

## 5️⃣ 데이터 저장 및 관리

### Room Database 스키마
```kotlin
@Entity(tableName = "water_records")
data class WaterRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val amount: Int,  // ml
    val date: String  // yyyy-MM-dd
)
```

### 저장할 데이터
- 매번 마신 양과 시간
- 일별 누적량
- 사용자 목표 설정

---

## 6️⃣ UI/UX 구현

### 화면 구성

#### 1. 홈 화면
- 실시간 컵 무게 표시
- 오늘 마신 양 / 목표량 (진행률 표시)
- 블루투스 연결 상태
- 영점 조정 버튼

#### 2. 통계 화면
- 일별/주별/월별 그래프
- 평균 섭취량
- 달성률

#### 3. 설정 화면
- 목표 섭취량 설정
- 알림 시간 설정
- 블루투스 기기 관리

### 알림 기능
- 일정 시간마다 물 마시기 알림
- 목표 달성 시 축하 알림

---

## 7️⃣ 테스트 및 디버깅

### 테스트 항목
- [ ] 로드셀 정확도 테스트 (캘리브레이션)
- [ ] 블루투스 연결 안정성
- [ ] 데이터 송수신 정확성
- [ ] 앱 백그라운드 동작
- [ ] 배터리 최적화
- [ ] 예외 상황 처리 (연결 끊김, 센서 오류)

---

## 📦 필요한 하드웨어 및 도구

### 하드웨어
- Arduino Uno
- 로드셀 (5kg 또는 10kg)
- HX711 모듈
- HC-05 또는 HC-06 블루투스 모듈
- 점퍼 와이어, 브레드보드
- 컵 거치대

### 소프트웨어
- Arduino IDE
- Android Studio (최신 버전)
- USB 케이블 (아두이노 업로드용)

---

## ⏱️ 예상 개발 일정

1. **1-2일**: 아두이노 개발 및 하드웨어 테스트
2. **3-5일**: 안드로이드 앱 기본 구조 및 블루투스 통신
3. **2-3일**: UI/UX 구현
4. **1-2일**: 데이터 저장 및 알림 기능
5. **1-2일**: 통합 테스트 및 디버깅

**총 예상 기간: 약 7-14일**

---

## 🔌 하드웨어 연결도

### HX711 → Arduino
- VCC → 5V
- GND → GND
- DT (Data) → Pin 3
- SCK (Clock) → Pin 2

### HC-05/06 → Arduino
- VCC → 5V (또는 3.3V, 모듈에 따라)
- GND → GND
- TXD → Pin 10 (Arduino RX)
- RXD → Pin 11 (Arduino TX) - 전압 분배기 필요할 수 있음

### 로드셀 → HX711
- RED (E+) → E+
- BLACK (E-) → E-
- WHITE (A-) → A-
- GREEN (A+) → A+

---

## 📝 개발 진행 상태

- [x] 프로젝트 계획 수립
- [ ] 아두이노 코드 개발
- [ ] 안드로이드 앱 개발
- [ ] 통합 테스트
- [ ] 최종 배포