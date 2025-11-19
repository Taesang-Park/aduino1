/*
 * Water Monitor System - Arduino Code
 *
 * 로드셀을 사용하여 물 섭취량을 모니터링하고
 * 블루투스로 안드로이드 앱에 데이터를 전송합니다.
 *
 * 필요한 라이브러리:
 * - HX711 (by Bogdan Necula) - Arduino Library Manager에서 설치
 * - SoftwareSerial (기본 포함)
 *
 * 작성자: Arduino Water Monitor Project
 * 날짜: 2024
 */

#include <HX711.h>
#include <SoftwareSerial.h>
#include "config.h"

// ========== 전역 객체 ==========
HX711 scale;
SoftwareSerial bluetooth(BT_RX_PIN, BT_TX_PIN);

// ========== 전역 변수 ==========
float currentWeight = 0.0;      // 현재 무게 (g)
float previousWeight = 0.0;     // 이전 무게 (g)
float baselineWeight = 0.0;     // 기준 무게 (컵 + 물)
unsigned long lastReadTime = 0; // 마지막 읽기 시간
int stableCount = 0;            // 안정화 카운터

// 이동 평균 필터용
float weightBuffer[AVERAGE_SAMPLES];
int bufferIndex = 0;
bool bufferFilled = false;

// 상태 플래그
bool isCalibrated = false;
bool cupDetected = false;

// ========== 함수 선언 ==========
void setupScale();
void setupBluetooth();
float readWeight();
float getAverageWeight();
void processBluetothCommand();
void sendWeight(float weight);
void sendDrinkAmount(float amount);
void sendStatus(const char* message);
void tare();
void resetSystem();
void debugPrint(const char* message);
void debugPrintValue(const char* label, float value);

// ========== setup() ==========
void setup() {
  // 시리얼 통신 초기화 (디버그용)
  Serial.begin(SERIAL_BAUD_RATE);
  debugPrint("=== Water Monitor System ===");
  debugPrint("Initializing...");

  // HX711 초기화
  setupScale();

  // 블루투스 초기화
  setupBluetooth();

  // 시스템 준비 완료
  debugPrint("System ready!");
  sendStatus("READY");

  delay(1000);
}

// ========== loop() ==========
void loop() {
  unsigned long currentTime = millis();

  // 블루투스 명령 처리
  processBluetothCommand();

  // 정해진 간격마다 무게 읽기
  if (currentTime - lastReadTime >= READ_INTERVAL) {
    lastReadTime = currentTime;

    // 무게 읽기
    currentWeight = readWeight();

    // 디버그 출력
    debugPrintValue("Weight", currentWeight);

    // 컵 감지 (10g 이상이면 컵이 올려진 것으로 간주)
    if (currentWeight > 10.0 && !cupDetected) {
      cupDetected = true;
      baselineWeight = currentWeight;
      debugPrint("Cup detected!");
      sendStatus("CUP_ON");
    } else if (currentWeight < 5.0 && cupDetected) {
      cupDetected = false;
      debugPrint("Cup removed!");
      sendStatus("CUP_OFF");
    }

    // 무게 전송
    if (cupDetected) {
      sendWeight(currentWeight);
    }

    // 마신 양 감지 (무게가 줄어든 경우)
    if (cupDetected && previousWeight > 0) {
      float weightDiff = previousWeight - currentWeight;

      // 임계값 이상의 변화가 있을 때만 처리
      if (weightDiff > WEIGHT_THRESHOLD) {
        stableCount++;

        // 안정화 확인
        if (stableCount >= STABLE_READINGS) {
          // 마신 양 계산 (g ≈ ml for water)
          float drinkAmount = weightDiff;

          debugPrintValue("Drink detected", drinkAmount);
          sendDrinkAmount(drinkAmount);

          // 새로운 기준점 설정
          baselineWeight = currentWeight;
          stableCount = 0;
        }
      } else {
        // 무게가 안정적이면 카운터 리셋
        stableCount = 0;
      }
    }

    previousWeight = currentWeight;
  }
}

// ========== HX711 초기화 ==========
void setupScale() {
  debugPrint("Initializing HX711...");

  scale.begin(HX711_DOUT_PIN, HX711_SCK_PIN);

  // 스케일이 준비될 때까지 대기
  int timeout = 0;
  while (!scale.is_ready() && timeout < 50) {
    delay(100);
    timeout++;
  }

  if (!scale.is_ready()) {
    debugPrint("ERROR: HX711 not found!");
    sendStatus("ERROR_HX711");
    return;
  }

  // 캘리브레이션 인자 설정
  scale.set_scale(CALIBRATION_FACTOR);

  // 영점 조정
  debugPrint("Taring... Please remove all weight.");
  delay(2000);
  scale.tare();

  isCalibrated = true;
  debugPrint("HX711 ready!");
}

// ========== 블루투스 초기화 ==========
void setupBluetooth() {
  debugPrint("Initializing Bluetooth...");
  bluetooth.begin(SERIAL_BAUD_RATE);
  debugPrint("Bluetooth ready!");
}

// ========== 무게 읽기 ==========
float readWeight() {
  if (!scale.is_ready()) {
    debugPrint("ERROR: Scale not ready");
    return 0.0;
  }

  float weight = 0.0;

  if (USE_AVERAGING) {
    // 이동 평균 필터 사용
    weightBuffer[bufferIndex] = scale.get_units(1);
    bufferIndex = (bufferIndex + 1) % AVERAGE_SAMPLES;

    if (bufferIndex == 0) {
      bufferFilled = true;
    }

    weight = getAverageWeight();
  } else {
    // 단순 읽기
    weight = scale.get_units(3);  // 3개 샘플 평균
  }

  // 음수 무게는 0으로 처리
  if (weight < 0) {
    weight = 0.0;
  }

  return weight;
}

// ========== 이동 평균 계산 ==========
float getAverageWeight() {
  float sum = 0.0;
  int count = bufferFilled ? AVERAGE_SAMPLES : bufferIndex;

  if (count == 0) return 0.0;

  for (int i = 0; i < count; i++) {
    sum += weightBuffer[i];
  }

  return sum / count;
}

// ========== 블루투스 명령 처리 ==========
void processBluetothCommand() {
  if (bluetooth.available() > 0) {
    String command = bluetooth.readStringUntil('\n');
    command.trim();

    debugPrint("Received command: ");
    debugPrint(command.c_str());

    if (command == CMD_TARE) {
      // 영점 조정
      tare();
    } else if (command == CMD_RESET) {
      // 시스템 리셋
      resetSystem();
    } else if (command == CMD_CALIBRATE) {
      // 캘리브레이션 모드 (추가 기능)
      sendStatus("CALIBRATE_MODE");
      debugPrint("Calibration mode not implemented yet");
    } else {
      sendStatus("UNKNOWN_CMD");
    }
  }
}

// ========== 무게 전송 ==========
void sendWeight(float weight) {
  // 형식: "W:250.5\n"
  bluetooth.print(CMD_WEIGHT);
  bluetooth.println(weight, 1);  // 소수점 1자리
}

// ========== 마신 양 전송 ==========
void sendDrinkAmount(float amount) {
  // 형식: "D:50.0\n"
  bluetooth.print(CMD_DRINK);
  bluetooth.println(amount, 1);  // 소수점 1자리
}

// ========== 상태 메시지 전송 ==========
void sendStatus(const char* message) {
  // 형식: "S:READY\n"
  bluetooth.print(CMD_STATUS);
  bluetooth.println(message);
}

// ========== 영점 조정 ==========
void tare() {
  debugPrint("Taring scale...");
  sendStatus("TARING");

  scale.tare();

  // 버퍼 초기화
  bufferIndex = 0;
  bufferFilled = false;
  for (int i = 0; i < AVERAGE_SAMPLES; i++) {
    weightBuffer[i] = 0.0;
  }

  currentWeight = 0.0;
  previousWeight = 0.0;
  baselineWeight = 0.0;
  stableCount = 0;
  cupDetected = false;

  debugPrint("Tare complete!");
  sendStatus("TARE_OK");
}

// ========== 시스템 리셋 ==========
void resetSystem() {
  debugPrint("Resetting system...");
  sendStatus("RESETTING");

  // 변수 초기화
  currentWeight = 0.0;
  previousWeight = 0.0;
  baselineWeight = 0.0;
  stableCount = 0;
  cupDetected = false;

  // 버퍼 초기화
  bufferIndex = 0;
  bufferFilled = false;
  for (int i = 0; i < AVERAGE_SAMPLES; i++) {
    weightBuffer[i] = 0.0;
  }

  debugPrint("Reset complete!");
  sendStatus("RESET_OK");
}

// ========== 디버그 출력 (메시지) ==========
void debugPrint(const char* message) {
  if (DEBUG_MODE) {
    Serial.println(message);
  }
}

// ========== 디버그 출력 (값) ==========
void debugPrintValue(const char* label, float value) {
  if (DEBUG_MODE) {
    Serial.print(label);
    Serial.print(": ");
    Serial.println(value, 2);
  }
}