# Arduino Water Monitor - 사용 설명서

## 📋 개요
이 아두이노 코드는 로드셀과 HX711 모듈을 사용하여 물의 무게를 측정하고, 블루투스(HC-05/06)를 통해 안드로이드 앱으로 데이터를 전송합니다.

---

## 🔧 하드웨어 연결

### HX711 → Arduino Uno
| HX711 핀 | Arduino 핀 |
|---------|-----------|
| VCC     | 5V        |
| GND     | GND       |
| DT      | D3        |
| SCK     | D2        |

### HC-05/06 → Arduino Uno
| HC-05/06 핀 | Arduino 핀 |
|------------|-----------|
| VCC        | 5V (또는 3.3V) |
| GND        | GND       |
| TXD        | D10       |
| RXD        | D11 (전압 분배기 권장) |

### 로드셀 → HX711
| 로드셀 선 | HX711 핀 |
|----------|---------|
| RED (E+) | E+      |
| BLACK (E-) | E-    |
| WHITE (A-) | A-    |
| GREEN (A+) | A+    |

**주의:** HC-05/06의 RXD는 3.3V입니다. Arduino의 5V 출력을 연결하려면 전압 분배기(1kΩ + 2kΩ 저항)를 사용하세요.

```
Arduino D11 ---[1kΩ]--- HC-05 RXD
                  |
                [2kΩ]
                  |
                 GND
```

---

## 📦 필요한 라이브러리

### HX711 라이브러리 설치
Arduino IDE를 열고:

1. **Sketch → Include Library → Manage Libraries** 선택
2. 검색창에 "HX711" 입력
3. "HX711 Arduino Library" by Bogdan Necula 설치

또는 수동 설치:
```bash
git clone https://github.com/bogde/HX711.git
```
Arduino IDE의 `libraries` 폴더에 복사

---

## ⚙️ 설정 및 캘리브레이션

### 1. config.h 설정

#### 디버그 모드 활성화
처음 테스트할 때는 디버그 모드를 켜세요:
```cpp
#define DEBUG_MODE  true
```

#### 핀 설정 확인
하드웨어 연결에 맞게 핀 번호를 조정하세요:
```cpp
#define HX711_DOUT_PIN  3
#define HX711_SCK_PIN   2
#define BT_RX_PIN  10
#define BT_TX_PIN  11
```

### 2. 로드셀 캘리브레이션

**중요:** `CALIBRATION_FACTOR` 값은 사용하는 로드셀에 따라 다릅니다!

#### 캘리브레이션 절차:

1. **영점 조정**
   - 로드셀에 아무것도 올려놓지 않은 상태
   - 아두이노 업로드 및 실행
   - 시리얼 모니터에서 "Taring..." 메시지 확인

2. **캘리브레이션 인자 찾기**
   ```cpp
   // config.h에서 임시로 1.0으로 설정
   #define CALIBRATION_FACTOR  1.0
   ```

3. **테스트 무게 올리기**
   - 알려진 무게(예: 1000g)를 로드셀에 올림
   - 시리얼 모니터에서 출력값 확인 (예: 142100)

4. **인자 계산**
   ```
   CALIBRATION_FACTOR = 출력값 / 실제 무게
   예: 142100 / 1000 = -142.1
   ```

   음수가 나올 수 있습니다. 이는 정상입니다.

5. **config.h 업데이트**
   ```cpp
   #define CALIBRATION_FACTOR  -142.1
   ```

6. **재업로드 및 확인**
   - 코드를 다시 업로드
   - 1000g을 올렸을 때 1000에 가까운 값이 나오는지 확인

---

## 🚀 업로드 및 실행

### 1. Arduino IDE에서 열기
```
File → Open → water_monitor.ino
```

### 2. 보드 및 포트 선택
```
Tools → Board → Arduino Uno
Tools → Port → (Arduino가 연결된 COM 포트 선택)
```

### 3. 컴파일 및 업로드
```
Sketch → Upload
또는 Ctrl+U
```

### 4. 시리얼 모니터 열기
```
Tools → Serial Monitor
또는 Ctrl+Shift+M

Baud Rate: 9600 선택
```

---

## 📡 통신 프로토콜

### Arduino → 앱

| 명령어 | 형식 | 설명 | 예시 |
|-------|------|------|------|
| W: | `W:무게\n` | 현재 무게 전송 (g) | `W:250.5\n` |
| D: | `D:양\n` | 마신 양 전송 (ml) | `D:50.0\n` |
| S: | `S:상태\n` | 상태 메시지 | `S:READY\n` |

**상태 메시지 종류:**
- `READY` - 시스템 준비 완료
- `CUP_ON` - 컵 감지됨
- `CUP_OFF` - 컵 제거됨
- `TARING` - 영점 조정 중
- `TARE_OK` - 영점 조정 완료
- `RESETTING` - 리셋 중
- `RESET_OK` - 리셋 완료
- `ERROR_HX711` - HX711 오류

### 앱 → Arduino

| 명령어 | 설명 | 예시 |
|-------|------|------|
| T\n | 영점 조정 (Tare) | `T\n` |
| R\n | 시스템 리셋 | `R\n` |
| C\n | 캘리브레이션 모드 (미구현) | `C\n` |

---

## 🧪 테스트

### 시리얼 모니터로 테스트

1. **HX711 테스트**
   ```
   - 시리얼 모니터 열기 (9600 baud)
   - "HX711 ready!" 메시지 확인
   - 무게를 올리면 "Weight: xxx" 메시지 출력
   ```

2. **블루투스 테스트**
   ```
   - HC-05/06 LED가 깜빡이는지 확인
   - 스마트폰에서 블루투스 페어링 (PIN: 1234 또는 0000)
   - 블루투스 터미널 앱으로 데이터 수신 확인
   ```

3. **영점 조정 테스트**
   ```
   - 시리얼 모니터에서 "T" 입력 후 엔터
   - "Tare complete!" 메시지 확인
   ```

### 블루투스 터미널 앱 추천
- Serial Bluetooth Terminal (Android)
- Bluetooth Terminal HC-05 (Android)

---

## ⚠️ 문제 해결

### "ERROR: HX711 not found!"
- HX711 연결 확인 (VCC, GND, DT, SCK)
- HX711 라이브러리 설치 확인
- 케이블 접촉 불량 확인

### 무게 값이 너무 크거나 작음
- `CALIBRATION_FACTOR` 재조정
- 로드셀 연결 확인 (4개 선 모두 연결)

### 무게 값이 불안정함
- `AVERAGE_SAMPLES` 증가 (config.h)
- `STABLE_READINGS` 증가
- 로드셀을 안정적인 표면에 설치

### 블루투스 연결 안 됨
- HC-05/06 전원 확인 (LED 깜빡임)
- RX/TX 연결 확인 (교차 연결: Arduino TX → BT RX)
- Baud rate 확인 (9600)
- 전압 분배기 확인 (필요시)

### 음수 무게 표시
- 영점 조정 (T 명령)
- `CALIBRATION_FACTOR`의 부호 변경

---

## 🔍 고급 설정

### 이동 평균 필터 비활성화
```cpp
#define USE_AVERAGING  false
```

### 감지 민감도 조정
```cpp
#define WEIGHT_THRESHOLD  5.0   // 낮을수록 민감
#define STABLE_READINGS   5     // 높을수록 안정적
```

### 샘플링 속도 조정
```cpp
#define READ_INTERVAL  500  // 밀리초 (낮을수록 빠름)
```

---

## 📝 다음 단계

1. ✅ 아두이노 코드 업로드 완료
2. ⬜ 안드로이드 앱 개발
3. ⬜ 통합 테스트

---

## 💡 팁

- 처음엔 `DEBUG_MODE true`로 설정하여 시리얼 모니터로 동작 확인
- 캘리브레이션은 실제 사용할 컵과 물을 사용하여 진행
- 로드셀은 진동이 없는 안정적인 곳에 설치
- HC-05/06 모듈의 기본 PIN은 보통 1234 또는 0000

---

## 📞 지원

문제가 발생하면:
1. 시리얼 모니터에서 디버그 메시지 확인
2. 하드웨어 연결 재확인
3. 라이브러리 버전 확인