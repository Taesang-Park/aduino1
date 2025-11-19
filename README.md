# 물 섭취량 모니터링 시스템 (Water Intake Monitor)

로드셀과 아두이노를 사용하여 실시간으로 물 섭취량을 측정하고, 안드로이드 앱에서 모니터링하는 IoT 프로젝트입니다.

---

## 📋 프로젝트 개요

### 시스템 구성
```
로드셀 (Load Cell)
    ↓
HX711 모듈 (ADC 증폭기)
    ↓
Arduino Uno (제어 및 연산)
    ↓
HC-05/06 (블루투스 모듈)
    ↓
안드로이드 앱 (Kotlin + Jetpack Compose)
    ↓
사용자
```

### 주요 기능
- ✅ 실시간 무게 측정 및 전송
- ✅ 자동 물 섭취량 감지
- ✅ 블루투스 무선 통신
- ✅ 일일 섭취량 자동 기록
- ✅ 목표 달성률 표시
- ✅ Room Database 영구 저장

---

## 📦 프로젝트 구조

```
aduino1/
├── arduino/                    # 아두이노 코드
│   ├── water_monitor.ino       # 메인 아두이노 스케치
│   ├── config.h                # 설정 파일
│   └── README.md               # 아두이노 사용 설명서
├── app/                        # 안드로이드 앱
│   ├── src/main/java/com/example/aduino1/
│   │   ├── data/               # 데이터 계층
│   │   ├── domain/             # 도메인 모델
│   │   ├── presentation/       # UI 계층
│   │   ├── ui/                 # 테마
│   │   └── MainActivity.kt
│   └── README.md               # 앱 사용 설명서
├── project_plan.md             # 개발 계획서
├── aduino1.txt                 # 프로젝트 요구사항
└── README.md                   # 이 파일
```

---

## 🚀 빠른 시작

### 1단계: 하드웨어 준비

#### 필요한 부품
- Arduino Uno × 1
- 로드셀 (5kg 또는 10kg) × 1
- HX711 모듈 × 1
- HC-05 또는 HC-06 블루투스 모듈 × 1
- 점퍼 와이어, 브레드보드
- USB 케이블

#### 하드웨어 연결
자세한 연결 방법은 `arduino/README.md` 참조

```
HX711 → Arduino
- VCC → 5V
- GND → GND
- DT  → Pin 3
- SCK → Pin 2

HC-05/06 → Arduino
- VCC → 5V
- GND → GND
- TXD → Pin 10
- RXD → Pin 11 (전압 분배기 권장)
```

### 2단계: 아두이노 설정

```bash
# 1. Arduino IDE 열기
# 2. HX711 라이브러리 설치
#    Sketch → Include Library → Manage Libraries → "HX711" 검색 및 설치

# 3. 파일 열기
#    arduino/water_monitor.ino 열기

# 4. 캘리브레이션
#    config.h에서 CALIBRATION_FACTOR 조정

# 5. 업로드
#    Tools → Board → Arduino Uno
#    Tools → Port → (적절한 포트 선택)
#    Sketch → Upload
```

자세한 설정 방법: `arduino/README.md`

### 3단계: 안드로이드 앱 실행

```bash
# 1. Android Studio에서 프로젝트 열기
# 2. Gradle Sync
# 3. 앱 빌드 및 실행
#    Run → Run 'app' (Shift+F10)

# 4. 블루투스 페어링
#    - 휴대폰 설정에서 HC-05/06과 페어링 (PIN: 1234 또는 0000)

# 5. 앱에서 디바이스 연결
#    - "디바이스 연결" 버튼 클릭
#    - HC-05/06 선택
```

자세한 사용 방법: `app/README.md`

---

## 🔧 기술 스택

### 하드웨어
- **Arduino Uno** - 메인 제어 보드
- **로드셀** - 무게 센서
- **HX711** - ADC 증폭기
- **HC-05/06** - 블루투스 모듈

### 아두이노 (C++)
- HX711 라이브러리
- SoftwareSerial
- 이동 평균 필터
- 실시간 센서 데이터 처리

### 안드로이드 (Kotlin)
- **Jetpack Compose** - 선언형 UI
- **MVVM Architecture** - 아키텍처 패턴
- **Room Database** - 로컬 저장소
- **Coroutines & Flow** - 비동기 처리
- **Material3** - 디자인 시스템

---

## 📡 통신 프로토콜

### 아두이노 → 앱
```
W:250.5\n    # 현재 무게 (g)
D:50.0\n     # 마신 양 (ml)
S:READY\n    # 상태 메시지
```

### 앱 → 아두이노
```
T\n          # 영점 조정 (Tare)
R\n          # 시스템 리셋
```

---

## 📱 주요 화면

### 홈 화면
<table>
<tr>
<td width="50%">

**블루투스 연결 카드**
- 연결 상태 표시
- 디바이스 선택 및 연결
- 영점 조정 버튼

</td>
<td width="50%">

**현재 무게 카드**
- 실시간 무게 표시
- 대형 글씨로 가독성 향상

</td>
</tr>
<tr>
<td colspan="2">

**오늘의 섭취량 카드**
- 총 섭취량 / 목표량
- 진행률 바 표시
- 달성률 퍼센트
- 수동 기록 추가 버튼

</td>
</tr>
</table>

---

## 🎯 사용 예시

### 일반적인 사용 흐름

1. **아침**
   - 앱 실행 및 블루투스 연결
   - 빈 컵을 로드셀에 올리고 영점 조정

2. **물 마시기**
   - 컵에 물을 채움 (앱에서 무게 표시)
   - 물을 마심
   - 앱에서 자동으로 마신 양 감지 및 기록

3. **하루 종일**
   - 물을 마실 때마다 자동 기록
   - 실시간으로 일일 섭취량 확인
   - 목표 달성 여부 확인

4. **밤**
   - 일일 통계 확인
   - 데이터 자동 저장

---

## ⚙️ 설정 및 커스터마이징

### 아두이노 설정 (config.h)

```cpp
// 감지 민감도 조정
#define WEIGHT_THRESHOLD  5.0      // 낮을수록 민감

// 안정화 설정
#define STABLE_READINGS   5        // 높을수록 안정적

// 샘플링 속도
#define READ_INTERVAL     500      // ms
```

### 앱 설정

```kotlin
// HomeViewModel.kt
private val _goalAmount = MutableStateFlow(2000) // 기본 2L

// 목표량 변경
viewModel.setGoalAmount(2500) // 2.5L로 변경
```

---

## 🐛 문제 해결

### 일반적인 문제

| 문제 | 해결 방법 |
|-----|---------|
| 블루투스 연결 안 됨 | 1. 페어링 확인<br>2. 권한 확인<br>3. HC-05/06 전원 확인 |
| 무게 측정 안 됨 | 1. HX711 연결 확인<br>2. 캘리브레이션 재조정<br>3. 로드셀 연결 확인 |
| 무게 값 불안정 | 1. AVERAGE_SAMPLES 증가<br>2. 로드셀을 안정적인 곳에 설치 |
| 앱 크래시 | 1. 권한 확인<br>2. Logcat 에러 확인<br>3. 앱 재설치 |

자세한 문제 해결: `arduino/README.md` 및 `app/README.md`

---

## 📊 데이터 흐름

```
[로드셀] 무게 감지
    ↓
[HX711] 신호 증폭 및 디지털 변환
    ↓
[Arduino] 데이터 읽기 및 처리
    ↓
[Arduino] 무게 변화 감지 (마신 양 계산)
    ↓
[HC-05/06] 블루투스로 전송 (W:xxx, D:xxx)
    ↓
[Android] BluetoothManager에서 수신
    ↓
[Android] 자동으로 Room DB에 저장
    ↓
[Android] UI에 실시간 업데이트
```

---

## 🔐 보안 및 권한

### 안드로이드 권한
- `BLUETOOTH_SCAN` (Android 12+)
- `BLUETOOTH_CONNECT` (Android 12+)
- `BLUETOOTH` (Android 11 이하)
- `BLUETOOTH_ADMIN` (Android 11 이하)
- `POST_NOTIFICATIONS` (Android 13+, 향후 구현)

### 데이터 프라이버시
- 모든 데이터는 로컬 디바이스에만 저장
- 외부 서버로 전송하지 않음
- 사용자 동의 없이 데이터 수집하지 않음

---

## 🚧 향후 개발 계획

### Phase 2 (우선순위 높음)
- [ ] 통계 화면 (주간/월간 그래프)
- [ ] 알림 기능 (WorkManager)
- [ ] 설정 화면 (DataStore)
- [ ] 다크 모드 완벽 지원

### Phase 3 (추가 기능)
- [ ] 위젯 지원
- [ ] 데이터 내보내기 (CSV)
- [ ] 여러 사용자 프로필
- [ ] 클라우드 백업

### Phase 4 (고급 기능)
- [ ] 머신러닝 기반 섭취 패턴 분석
- [ ] 건강 앱 연동
- [ ] 웨어러블 디바이스 지원

---

## 📚 문서

- **프로젝트 계획**: `project_plan.md`
- **아두이노 설명서**: `arduino/README.md`
- **앱 설명서**: `app/README.md`
- **요구사항**: `aduino1.txt`

---

## 🧪 테스트

### 하드웨어 테스트
```bash
# 아두이노 시리얼 모니터에서 확인
1. HX711 초기화 확인
2. 무게 읽기 테스트
3. 블루투스 데이터 전송 확인
```

### 앱 테스트
```bash
# 수동 테스트 체크리스트
□ 블루투스 연결/해제
□ 실시간 무게 표시
□ 자동 섭취 감지
□ 수동 기록 추가
□ 데이터 영구 저장
□ 앱 재시작 후 데이터 유지
```

---

## 💡 개발 팁

### 디버깅

**아두이노**
```cpp
// config.h에서 디버그 모드 활성화
#define DEBUG_MODE  true

// 시리얼 모니터에서 로그 확인
```

**안드로이드**
```bash
# Logcat 필터
adb logcat | grep "BluetoothManager"
adb logcat | grep "HomeViewModel"
```

### 성능 최적화

**아두이노**
- 이동 평균 필터 사용으로 노이즈 감소
- 적절한 READ_INTERVAL 설정

**안드로이드**
- Flow를 사용한 효율적인 데이터 스트림
- Room Database의 인덱싱 활용

---

## 🤝 기여

이 프로젝트는 학습 목적으로 제작되었습니다. 개선 사항이나 버그를 발견하시면:

1. Issue 등록
2. Pull Request 생성
3. 피드백 제공

---

## 📝 라이선스

이 프로젝트는 개인 학습 목적으로 제작되었습니다.

---

## 📞 지원 및 문의

### 문제 발생 시
1. 해당 README 파일 확인 (`arduino/` 또는 `app/`)
2. 문제 해결 섹션 참조
3. Logcat/시리얼 모니터 로그 확인

### 개발 환경
- Arduino IDE 2.x
- Android Studio Hedgehog (2023.1.1)
- Kotlin 1.9.0
- Gradle 8.0

---

## 🎓 학습 자료

### 참고한 기술
- [Arduino HX711 라이브러리](https://github.com/bogde/HX711)
- [Android Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- [Bluetooth Classic (SPP)](https://developer.android.com/guide/topics/connectivity/bluetooth)

---

## ✨ 주요 특징

### 하드웨어
- ⚡ 실시간 센서 데이터 처리
- 🔄 자동 영점 조정
- 📡 안정적인 블루투스 통신
- 🎯 정확한 무게 측정 (캘리브레이션)

### 소프트웨어
- 🎨 Modern Material3 디자인
- 🏗️ Clean Architecture
- 📱 Jetpack Compose 선언형 UI
- 💾 Room Database 영구 저장
- 🔄 Reactive Programming (Flow)

---

**프로젝트 시작일**: 2025-01-08
**현재 버전**: 1.0.0
**상태**: ✅ 기본 기능 완성 (Phase 1)