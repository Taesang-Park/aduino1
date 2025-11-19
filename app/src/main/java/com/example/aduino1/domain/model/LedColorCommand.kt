package com.example.aduino1.domain.model

/**
 * RGB LED 색상 명령
 *
 * Arduino로 전송할 LED 색상 코드를 정의합니다.
 */
enum class LedColorCommand(val code: Int, val displayName: String) {
    RED(0, "빨강"),      // 섭취량 부족 (0-50%)
    YELLOW(1, "노랑"),   // 보통 (50-100%)
    BLUE(2, "파랑");     // 충분 (100%↑)

    /**
     * Arduino로 전송할 명령 문자열 생성
     * @return "C:0", "C:1", "C:2" 형식
     */
    fun toCommand(): String = "C:$code"

    companion object {
        /**
         * 달성률에 따라 적절한 LED 색상을 반환
         * @param achievementPercentage 달성률 (0-100+)
         * @return 해당하는 LED 색상 명령
         */
        fun fromAchievementPercentage(achievementPercentage: Float): LedColorCommand {
            return when {
                achievementPercentage < 50f -> RED
                achievementPercentage < 100f -> YELLOW
                else -> BLUE
            }
        }

        /**
         * 색상 코드로부터 LED 명령 가져오기
         * @param code 0, 1, 2
         * @return 해당하는 LED 색상 명령
         */
        fun fromCode(code: Int): LedColorCommand? {
            return values().find { it.code == code }
        }
    }
}
