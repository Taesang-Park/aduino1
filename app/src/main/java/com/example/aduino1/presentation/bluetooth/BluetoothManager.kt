package com.example.aduino1.presentation.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.aduino1.domain.model.BluetoothConnectionState
import com.example.aduino1.domain.model.LedColorCommand
import com.example.aduino1.domain.model.SensorData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * 블루투스 통신 매니저
 * HC-05/06 모듈과의 블루투스 통신을 담당
 */
class BluetoothManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var readJob: Job? = null

    // 연결 상태
    private val _connectionState = MutableStateFlow(BluetoothConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    // 현재 무게
    private val _currentWeight = MutableStateFlow(0f)
    val currentWeight: StateFlow<Float> = _currentWeight.asStateFlow()

    // 마신 양 (이벤트성)
    private val _drinkAmount = MutableStateFlow<Float?>(null)
    val drinkAmount: StateFlow<Float?> = _drinkAmount.asStateFlow()

    // 상태 메시지
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // 연결된 디바이스 이름
    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    companion object {
        private const val TAG = "BluetoothManager"
        private val UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID

        // 통신 프로토콜
        private const val CMD_WEIGHT = "W:"
        private const val CMD_DRINK = "D:"
        private const val CMD_STATUS = "S:"
    }

    /**
     * 블루투스 활성화 여부 확인
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * 블루투스 권한 확인
     */
    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 페어링된 디바이스 목록 가져오기
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermission()) {
            Log.e(TAG, "Bluetooth permission not granted")
            return emptyList()
        }

        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    /**
     * 디바이스에 연결
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice) = withContext(Dispatchers.IO) {
        if (!hasBluetoothPermission()) {
            _connectionState.value = BluetoothConnectionState.ERROR
            return@withContext
        }

        try {
            _connectionState.value = BluetoothConnectionState.CONNECTING
            Log.d(TAG, "Connecting to ${device.name}...")

            // 기존 연결 해제
            disconnect()

            // 소켓 생성
            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID_SPP)

            // 연결
            bluetoothSocket?.connect()

            // 스트림 초기화
            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream

            _connectionState.value = BluetoothConnectionState.CONNECTED
            _connectedDeviceName.value = device.name
            Log.d(TAG, "Connected to ${device.name}")

            // 데이터 수신 시작
            startReading()

        } catch (e: IOException) {
            Log.e(TAG, "Connection failed", e)
            _connectionState.value = BluetoothConnectionState.ERROR
            disconnect()
        }
    }

    /**
     * 연결 해제
     */
    fun disconnect() {
        try {
            readJob?.cancel()
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()

            bluetoothSocket = null
            inputStream = null
            outputStream = null

            _connectionState.value = BluetoothConnectionState.DISCONNECTED
            _connectedDeviceName.value = null
            _currentWeight.value = 0f
            _drinkAmount.value = null

            Log.d(TAG, "Disconnected")
        } catch (e: IOException) {
            Log.e(TAG, "Error during disconnect", e)
        }
    }

    /**
     * 데이터 읽기 시작
     */
    private fun startReading() {
        readJob = scope.launch {
            val buffer = ByteArray(1024)
            val stringBuilder = StringBuilder()

            try {
                while (isActive && _connectionState.value == BluetoothConnectionState.CONNECTED) {
                    val bytes = inputStream?.read(buffer)
                    if (bytes != null && bytes > 0) {
                        val data = String(buffer, 0, bytes)
                        stringBuilder.append(data)

                        // 줄바꿈 문자로 메시지 분리
                        val messages = stringBuilder.toString().split("\n")
                        stringBuilder.clear()

                        // 마지막 불완전한 메시지는 다시 버퍼에 저장
                        if (!data.endsWith("\n")) {
                            stringBuilder.append(messages.last())
                        }

                        // 완전한 메시지들 처리
                        val completeMessages = if (data.endsWith("\n")) {
                            messages
                        } else {
                            messages.dropLast(1)
                        }

                        completeMessages.forEach { message ->
                            if (message.isNotBlank()) {
                                parseMessage(message.trim())
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error reading data", e)
                withContext(Dispatchers.Main) {
                    if (_connectionState.value == BluetoothConnectionState.CONNECTED) {
                        _connectionState.value = BluetoothConnectionState.ERROR
                        disconnect()
                    }
                }
            }
        }
    }

    /**
     * 수신 메시지 파싱
     */
    private fun parseMessage(message: String) {
        Log.d(TAG, "Received: $message")

        when {
            message.startsWith(CMD_WEIGHT) -> {
                // 무게 데이터: "W:250.5"
                val weight = message.substring(CMD_WEIGHT.length).toFloatOrNull()
                if (weight != null) {
                    _currentWeight.value = weight
                    Log.d(TAG, "Weight: $weight g")
                }
            }

            message.startsWith(CMD_DRINK) -> {
                // 마신 양: "D:50.0"
                val amount = message.substring(CMD_DRINK.length).toFloatOrNull()
                if (amount != null) {
                    _drinkAmount.value = amount
                    Log.d(TAG, "Drink amount: $amount ml")

                    // 이벤트 처리 후 리셋 (다음 이벤트를 위해)
                    scope.launch {
                        delay(1000)
                        _drinkAmount.value = null
                    }
                }
            }

            message.startsWith(CMD_STATUS) -> {
                // 상태 메시지: "S:READY"
                val status = message.substring(CMD_STATUS.length)
                _statusMessage.value = status
                Log.d(TAG, "Status: $status")
            }

            else -> {
                Log.w(TAG, "Unknown message format: $message")
            }
        }
    }

    /**
     * 명령 전송
     */
    private suspend fun sendCommand(command: String) = withContext(Dispatchers.IO) {
        try {
            outputStream?.write("$command\n".toByteArray())
            outputStream?.flush()
            Log.d(TAG, "Sent: $command")
        } catch (e: IOException) {
            Log.e(TAG, "Error sending command", e)
            _connectionState.value = BluetoothConnectionState.ERROR
        }
    }

    /**
     * 영점 조정 명령 전송
     */
    suspend fun sendTareCommand() {
        sendCommand("T")
    }

    /**
     * 리셋 명령 전송
     */
    suspend fun sendResetCommand() {
        sendCommand("R")
    }

    /**
     * RGB LED 색상 명령 전송
     * @param color LED 색상 (RED, YELLOW, BLUE)
     */
    suspend fun sendColorCommand(color: LedColorCommand) {
        val command = color.toCommand()  // "C:0", "C:1", "C:2"
        sendCommand(command)
        Log.d(TAG, "Sent LED color command: $command (${color.displayName})")
    }

    /**
     * RGB LED 색상 명령 전송 (코드로)
     * @param colorCode 0 (RED), 1 (YELLOW), 2 (BLUE)
     */
    suspend fun sendColorCommandByCode(colorCode: Int) {
        val color = LedColorCommand.fromCode(colorCode)
        if (color != null) {
            sendColorCommand(color)
        } else {
            Log.w(TAG, "Invalid color code: $colorCode")
        }
    }

    /**
     * 리소스 정리
     */
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
}