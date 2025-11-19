# 물 섭취량 모니터링 안드로이드 앱

## 📱 개요
이 앱은 아두이노와 로드셀을 사용하여 실시간으로 물 섭취량을 모니터링하고 기록하는 안드로이드 애플리케이션입니다.

---

## 🏗️ 프로젝트 구조

```
app/src/main/java/com/example/aduino1/
├── data/
│   ├── local/
│   │   ├── database/
│   │   │   ├── WaterDao.kt              # 데이터베이스 DAO
│   │   │   └── WaterDatabase.kt         # Room Database
│   │   └── entity/
│   │       └── WaterRecord.kt           # 물 섭취 기록 Entity
│   └── repository/
│       └── WaterRepository.kt           # Repository
├── domain/
│   └── model/
│       └── DailyWaterIntake.kt          # 도메인 모델
├── presentation/
│   ├── bluetooth/
│   │   └── BluetoothManager.kt          # 블루투스 통신 관리
│   └── home/
│       ├── HomeScreen.kt                # 홈 화면 (Compose)
│       └── HomeViewModel.kt             # 홈 화면 ViewModel
├── ui/
│   └── theme/
│       ├── Theme.kt                     # Material3 테마
│       └── Type.kt                      # 타이포그래피
└── MainActivity.kt                      # 메인 액티비티
```

---

## 🔧 기술 스택

### 아키텍처
- **MVVM (Model-View-ViewModel)** 패턴
- **Repository** 패턴
- **Clean Architecture** 기반 레이어 분리

### 주요 라이브러리
- **Jetpack Compose** - 선언형 UI
- **Room Database** - 로컬 데이터 저장
- **Coroutines & Flow** - 비동기 처리
- **ViewModel & LiveData** - 생명주기 인식 데이터 관리
- **Material3** - 최신 디자인 시스템

---

## 🚀 빌드 및 실행

### 1. 요구사항
- Android Studio Hedgehog (2023.1.1) 이상
- Android SDK 24 이상
- Kotlin 1.9.0 이상
- Gradle 8.0 이상

### 2. 프로젝트 빌드
```bash
# 1. 프로젝트 열기
Android Studio에서 프로젝트 폴더 열기

# 2. Gradle Sync
File → Sync Project with Gradle Files

# 3. 빌드
Build → Make Project (Ctrl+F9)

# 4. 실행
Run → Run 'app' (Shift+F10)
```

### 3. APK 생성
```bash
# Debug APK
Build → Build Bundle(s) / APK(s) → Build APK(s)

# Release APK
Build → Generate Signed Bundle / APK
```

---

## 📱 앱 사용 방법

### 1. 초기 설정

#### 블루투스 활성화
1. 휴대폰의 블루투스를 켜세요
2. 아두이노 HC-05/06 모듈과 페어링하세요
   - 기본 PIN: 1234 또는 0000

#### 권한 허용
앱 실행 시 다음 권한이 필요합니다:
- **블루투스 연결** (Android 12 이상: BLUETOOTH_SCAN, BLUETOOTH_CONNECT)
- **알림** (Android 13 이상: POST_NOTIFICATIONS)

### 2. 기본 사용

#### 블루투스 연결
1. "디바이스 연결" 버튼 클릭
2. 페어링된 디바이스 목록에서 HC-05/06 선택
3. 연결 상태가 "연결됨"으로 변경되는지 확인

#### 영점 조정
1. 컵을 로드셀에서 제거
2. "영점 조정" 버튼 클릭
3. 아두이노에서 영점 조정 완료 메시지 수신 대기

#### 물 마시기
1. 물이 담긴 컵을 로드셀에 올림
2. 물을 마심
3. 앱에서 자동으로 마신 양 감지 및 기록
4. 일일 섭취량 자동 업데이트

#### 수동 기록 추가
1. "+" 버튼 클릭
2. 마신 양(ml) 입력
3. "추가" 클릭

---

## 🔍 주요 기능

### 1. 실시간 모니터링
- 현재 컵의 무게 실시간 표시
- 블루투스 연결 상태 모니터링

### 2. 자동 기록
- 물을 마실 때 무게 변화 자동 감지
- 마신 양 자동 계산 및 저장

### 3. 일일 통계
- 오늘 마신 총량 표시
- 목표 달성률 표시
- 남은 양 안내

### 4. 데이터 관리
- Room Database에 영구 저장
- 날짜별 기록 관리

---

## 🔌 통신 프로토콜

### 아두이노 → 앱

| 명령어 | 형식 | 설명 | 예시 |
|-------|------|------|------|
| W: | `W:무게\n` | 현재 무게 전송 (g) | `W:250.5\n` |
| D: | `D:양\n` | 마신 양 전송 (ml) | `D:50.0\n` |
| S: | `S:상태\n` | 상태 메시지 | `S:READY\n` |

### 앱 → 아두이노

| 명령어 | 설명 |
|-------|------|
| T\n | 영점 조정 (Tare) |
| R\n | 시스템 리셋 |

---

## 🎨 UI 구성

### 홈 화면
```
┌─────────────────────────────┐
│   물 섭취량 모니터           │
├─────────────────────────────┤
│ [블루투스 연결]              │
│ • 연결 상태: 연결됨          │
│ • 디바이스: HC-06            │
│ [연결 해제] [영점 조정]      │
├─────────────────────────────┤
│ [현재 무게]                  │
│       250.5 g                │
├─────────────────────────────┤
│ [오늘의 섭취량]          [+] │
│ ▓▓▓▓▓▓░░░░ 65%              │
│ 1300 / 2000 ml               │
│ 목표까지 700ml 남았습니다    │
└─────────────────────────────┘
```

---

## 🗃️ 데이터베이스 스키마

### WaterRecord 테이블
```kotlin
@Entity(tableName = "water_records")
data class WaterRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Int,            // 마신 양 (ml)
    val timestamp: Long,        // 타임스탬프
    val date: String            // 날짜 (yyyy-MM-dd)
)
```

---

## ⚠️ 문제 해결

### 블루투스 연결 안 됨
1. 블루투스 권한 확인
   - 설정 → 앱 → 권한에서 블루투스 권한 허용
2. HC-05/06 모듈 페어링 확인
   - 휴대폰 블루투스 설정에서 페어링 상태 확인
3. 아두이노 전원 확인
   - HC-05/06 LED 깜빡임 확인

### 무게 측정 안 됨
1. 아두이노 연결 확인
2. 시리얼 모니터에서 데이터 전송 확인
3. 로드셀 연결 확인

### 앱 크래시
1. Logcat에서 에러 메시지 확인
2. 권한 허용 여부 확인
3. 앱 재설치

---

## 🔮 향후 개발 계획

### Phase 2 (계획)
- [ ] 통계 화면 구현 (주간/월간 그래프)
- [ ] 알림 기능 (일정 시간마다 물 마시기 알림)
- [ ] 설정 화면 (목표량 조정, 알림 시간 설정)
- [ ] DataStore를 통한 설정 저장
- [ ] 위젯 지원

### Phase 3 (계획)
- [ ] 다크 모드 완벽 지원
- [ ] 데이터 내보내기 (CSV, Excel)
- [ ] 클라우드 백업
- [ ] 사용자 프로필 관리

---

## 📄 라이선스
이 프로젝트는 개인 학습 목적으로 제작되었습니다.

---

## 📞 지원
문제가 발생하면 다음을 확인하세요:
1. 아두이노 README.md - 하드웨어 설정
2. Logcat - 앱 에러 로그
3. 시리얼 모니터 - 아두이노 통신 상태

---

## 🎯 핵심 클래스 설명

### BluetoothManager.kt
- HC-05/06과의 블루투스 통신 담당
- StateFlow를 통한 반응형 데이터 제공
- 자동 재연결 기능 (향후 추가 예정)

### WaterRepository.kt
- 데이터 계층 추상화
- Room Database 접근
- 비즈니스 로직 처리

### HomeViewModel.kt
- UI 상태 관리
- 블루투스 매니저와 Repository 연결
- 이벤트 처리

### HomeScreen.kt
- Jetpack Compose 기반 UI
- Material3 디자인
- 반응형 UI 구현

---

## 🧪 테스트

### 수동 테스트 체크리스트
- [ ] 블루투스 연결
- [ ] 블루투스 연결 해제
- [ ] 영점 조정
- [ ] 무게 실시간 표시
- [ ] 자동 물 섭취 감지
- [ ] 수동 기록 추가
- [ ] 일일 통계 업데이트
- [ ] 앱 재시작 후 데이터 유지

---

## 💡 개발 팁

### Compose Preview
각 Composable 함수에 @Preview 추가하여 빠른 UI 확인 가능

### 디버깅
```kotlin
// BluetoothManager에서 로그 확인
Log.d("BluetoothManager", "...")

// HomeViewModel에서 상태 확인
viewModel.connectionState.value
```

### Room Database 검사
Android Studio의 Database Inspector 사용
- View → Tool Windows → App Inspection