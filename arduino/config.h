/*
 * Water Monitor System - Configuration File
 *
 * 이 파일에서 핀 설정 및 캘리브레이션 값을 조정하세요.
 */

#ifndef CONFIG_H
#define CONFIG_H

// ========== HX711 핀 설정 ==========
#define HX711_DOUT_PIN  3  // HX711 DT 핀
#define HX711_SCK_PIN   2  // HX711 SCK 핀

// ========== 블루투스 핀 설정 ==========
#define BT_RX_PIN  10  // HC-05/06 TXD 연결
#define BT_TX_PIN  11  // HC-05/06 RXD 연결

// ========== 캘리브레이션 설정 ==========
// 주의: 이 값은 사용하는 로드셀에 따라 다릅니다!
// 캘리브레이션 방법:
// 1. 빈 로드셀에서 영점 조정
// 2. 알려진 무게(예: 1000g)를 올림
// 3. 출력값을 확인하고 CALIBRATION_FACTOR 조정
#define CALIBRATION_FACTOR  -7050.0  // 실제 로드셀에 맞게 조정 필요

// ========== 센서 설정 ==========
#define WEIGHT_THRESHOLD  5.0     // 무게 변화 감지 임계값 (g)
#define STABLE_READINGS   5       // 안정화를 위한 연속 읽기 횟수
#define READ_INTERVAL     500     // 센서 읽기 간격 (ms)

// ========== 필터 설정 ==========
#define USE_AVERAGING     true    // 이동 평균 필터 사용
#define AVERAGE_SAMPLES   10      // 평균 계산 샘플 수

// ========== 디버그 설정 ==========
#define DEBUG_MODE        false   // true로 설정하면 시리얼 모니터에 디버그 정보 출력
#define SERIAL_BAUD_RATE  9600    // 시리얼 통신 속도 (HC-05/06 기본값)

// ========== 프로토콜 정의 ==========
// 아두이노 -> 앱
#define CMD_WEIGHT        "W:"    // 현재 무게 전송
#define CMD_DRINK         "D:"    // 마신 양 전송
#define CMD_STATUS        "S:"    // 상태 메시지

// 앱 -> 아두이노
#define CMD_TARE          "T"     // 영점 조정
#define CMD_RESET         "R"     // 리셋
#define CMD_CALIBRATE     "C"     // 캘리브레이션 모드

#endif