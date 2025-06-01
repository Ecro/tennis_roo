package com.example.tennis_roo.core_sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import com.example.tennis_roo.core_sensors.buffer.CircularBuffer
import com.example.tennis_roo.core_sensors.model.SensorData
import com.example.tennis_roo.core_sensors.model.SensorType
import com.example.tennis_roo.core_sensors.model.SensorWindow
import com.example.tennis_roo.core_sensors.model.toSensorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

/**
 * Manager class for handling sensor registration, data collection, and processing.
 * This class provides a high-level API for the sensor functionality.
 */
class TennisRooSensorManager(private val context: Context) {
    
    private val androidSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Sensor instances
    private val accelerometer = androidSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = androidSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val heartRateSensor = androidSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    
    // Circular buffers for sensor data
    private val accelerometerBuffer = CircularBuffer<SensorData>(BUFFER_SIZE)
    private val gyroscopeBuffer = CircularBuffer<SensorData>(BUFFER_SIZE)
    private val heartRateBuffer = CircularBuffer<SensorData>(BUFFER_SIZE)
    
    // State flows for sensor availability
    private val _accelerometerAvailable = MutableStateFlow(accelerometer != null)
    val accelerometerAvailable: StateFlow<Boolean> = _accelerometerAvailable.asStateFlow()
    
    private val _gyroscopeAvailable = MutableStateFlow(gyroscope != null)
    val gyroscopeAvailable: StateFlow<Boolean> = _gyroscopeAvailable.asStateFlow()
    
    private val _heartRateAvailable = MutableStateFlow(heartRateSensor != null)
    val heartRateAvailable: StateFlow<Boolean> = _heartRateAvailable.asStateFlow()
    
    // Sensor event listeners
    private val accelerometerListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val sensorData = SensorData(
                timestamp = event.timestamp,
                sensorType = event.sensor.type.toSensorType(),
                values = event.values.clone(),
                accuracy = event.accuracy
            )
            accelerometerBuffer.add(sensorData)
            sensorCallback?.onAccelerometerData(sensorData)
        }
        
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // Not used in this implementation
        }
    }
    
    private val gyroscopeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val sensorData = SensorData(
                timestamp = event.timestamp,
                sensorType = event.sensor.type.toSensorType(),
                values = event.values.clone(),
                accuracy = event.accuracy
            )
            gyroscopeBuffer.add(sensorData)
            sensorCallback?.onGyroscopeData(sensorData)
        }
        
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // Not used in this implementation
        }
    }
    
    private val heartRateListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val sensorData = SensorData(
                timestamp = event.timestamp,
                sensorType = event.sensor.type.toSensorType(),
                values = event.values.clone(),
                accuracy = event.accuracy
            )
            heartRateBuffer.add(sensorData)
            sensorCallback?.onHeartRateData(sensorData)
        }
        
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // Not used in this implementation
        }
    }
    
    // Callback interface for sensor data
    interface SensorCallback {
        fun onAccelerometerData(data: SensorData) {}
        fun onGyroscopeData(data: SensorData) {}
        fun onHeartRateData(data: SensorData) {}
        fun onSensorWindowReady(window: SensorWindow) {}
    }
    
    private var sensorCallback: SensorCallback? = null
    private var isRegistered = false
    
    /**
     * Registers the sensor listeners and starts collecting data.
     *
     * @param samplingRateUs The sampling rate in microseconds
     * @param callback The callback to receive sensor data
     * @return true if registration was successful, false otherwise
     */
    fun registerListeners(samplingRateUs: Int = DEFAULT_SAMPLING_RATE_US, callback: SensorCallback): Boolean {
        if (isRegistered) {
            return false
        }
        
        sensorCallback = callback
        
        var success = true
        
        if (accelerometer != null) {
            success = success && androidSensorManager.registerListener(
                accelerometerListener,
                accelerometer,
                samplingRateUs
            )
        }
        
        if (gyroscope != null) {
            success = success && androidSensorManager.registerListener(
                gyroscopeListener,
                gyroscope,
                samplingRateUs
            )
        }
        
        if (heartRateSensor != null) {
            success = success && androidSensorManager.registerListener(
                heartRateListener,
                heartRateSensor,
                samplingRateUs
            )
        }
        
        isRegistered = success
        
        // Start window processing
        if (success) {
            startWindowProcessing()
        }
        
        return success
    }
    
    /**
     * Unregisters the sensor listeners and stops collecting data.
     */
    fun unregisterListeners() {
        if (!isRegistered) {
            return
        }
        
        androidSensorManager.unregisterListener(accelerometerListener)
        androidSensorManager.unregisterListener(gyroscopeListener)
        androidSensorManager.unregisterListener(heartRateListener)
        
        stopWindowProcessing()
        
        isRegistered = false
        sensorCallback = null
    }
    
    /**
     * Gets the current sensor window containing recent data from all sensors.
     *
     * @return A SensorWindow object containing the recent sensor data
     */
    fun getCurrentWindow(): SensorWindow {
        val accelData = accelerometerBuffer.getAll()
        val gyroData = gyroscopeBuffer.getAll()
        val heartData = heartRateBuffer.getAll()
        
        val startTime = minOf(
            accelData.minOfOrNull { it.timestamp } ?: Long.MAX_VALUE,
            gyroData.minOfOrNull { it.timestamp } ?: Long.MAX_VALUE,
            heartData.minOfOrNull { it.timestamp } ?: Long.MAX_VALUE
        )
        
        val endTime = maxOf(
            accelData.maxOfOrNull { it.timestamp } ?: Long.MIN_VALUE,
            gyroData.maxOfOrNull { it.timestamp } ?: Long.MIN_VALUE,
            heartData.maxOfOrNull { it.timestamp } ?: Long.MIN_VALUE
        )
        
        return SensorWindow(
            accelerometerData = accelData,
            gyroscopeData = gyroData,
            heartRateData = heartData,
            windowStartTime = if (startTime == Long.MAX_VALUE) 0 else startTime,
            windowEndTime = if (endTime == Long.MIN_VALUE) 0 else endTime
        )
    }
    
    /**
     * Clears all sensor data buffers.
     */
    fun clearBuffers() {
        accelerometerBuffer.clear()
        gyroscopeBuffer.clear()
        heartRateBuffer.clear()
    }
    
    private val windowRunnable = object : Runnable {
        override fun run() {
            val window = getCurrentWindow()
            sensorCallback?.onSensorWindowReady(window)
            mainHandler.postDelayed(this, WINDOW_PROCESSING_INTERVAL_MS)
        }
    }
    
    private fun startWindowProcessing() {
        mainHandler.postDelayed(windowRunnable, WINDOW_PROCESSING_INTERVAL_MS)
    }
    
    private fun stopWindowProcessing() {
        mainHandler.removeCallbacks(windowRunnable)
    }
    
    companion object {
        // Default sampling rate: 50Hz (20,000 microseconds)
        const val DEFAULT_SAMPLING_RATE_US = 20_000
        
        // Buffer size for 10 seconds of data at 50Hz
        const val BUFFER_SIZE = 500
        
        // Window processing interval: 300ms (as per requirements)
        const val WINDOW_PROCESSING_INTERVAL_MS = 300L
    }
}
