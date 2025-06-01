package com.example.tennis_roo.watch_app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.tennis_roo.core_sensors.TennisRooSensorManager
import com.example.tennis_roo.core_sensors.model.SensorData
import com.example.tennis_roo.core_sensors.model.SensorWindow
import com.example.tennis_roo.ml_gesture.GestureClassifier
import com.example.tennis_roo.ml_gesture.GestureClassifierFactory
import com.example.tennis_roo.ml_gesture.ClassifierType
import com.example.tennis_roo.ml_gesture.PointEvent
import com.example.tennis_roo.watch_app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Foreground service for continuous sensor monitoring.
 * This service will be responsible for collecting sensor data and detecting tennis point events.
 */
class SensorService : Service() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _sensorState = MutableStateFlow<SensorState>(SensorState.Idle)
    val sensorState: StateFlow<SensorState> = _sensorState.asStateFlow()
    
    private val _lastPointEvent = MutableStateFlow<PointEvent?>(null)
    val lastPointEvent: StateFlow<PointEvent?> = _lastPointEvent.asStateFlow()
    
    private val _debugInfo = MutableStateFlow<String>("")
    val debugInfo: StateFlow<String> = _debugInfo.asStateFlow()
    
    private lateinit var sensorManager: TennisRooSensorManager
    private lateinit var gestureClassifier: GestureClassifier
    
    private var isTestMode = false
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "tennis_roo_sensor_channel"
        private const val TAG = "SensorService"
        
        // Intent actions
        const val ACTION_START_SERVICE = "com.example.tennis_roo.START_SENSOR_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.tennis_roo.STOP_SENSOR_SERVICE"
        const val ACTION_TOGGLE_TEST_MODE = "com.example.tennis_roo.TOGGLE_TEST_MODE"
        const val ACTION_SIMULATE_POINT = "com.example.tennis_roo.SIMULATE_POINT"
        
        // Intent extras
        const val EXTRA_TEST_MODE = "test_mode"
        const val EXTRA_POINT_TYPE = "point_type"
        const val EXTRA_PLAYER = "player"
        
        // Foreground service types
        const val FOREGROUND_SERVICE_TYPE_DATA_SYNC = 1 // dataSync=1
        const val FOREGROUND_SERVICE_TYPE_LOCATION = 8 // location=8
        const val FOREGROUND_SERVICE_TYPE_HEALTH = 256 // health=256
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Initialize sensor manager
        sensorManager = TennisRooSensorManager(this)
        
        // Initialize gesture classifier
        gestureClassifier = GestureClassifierFactory.create(ClassifierType.MOCK)
        
        Log.d(TAG, "Service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                isTestMode = intent.getBooleanExtra(EXTRA_TEST_MODE, false)
                
                // Try to start as foreground service with different types
                if (!tryStartForeground()) {
                    // If all foreground service attempts fail, run in background mode
                    Log.d(TAG, "Running in background mode")
                    updateDebugInfo("Running in background mode")
                }
                
                // Start sensor monitoring regardless of foreground service status
                startSensorMonitoring()
            }
            ACTION_STOP_SERVICE -> {
                stopSensorMonitoring()
                stopSelf()
            }
            ACTION_TOGGLE_TEST_MODE -> {
                isTestMode = intent.getBooleanExtra(EXTRA_TEST_MODE, false)
                updateDebugInfo("Test mode: $isTestMode")
            }
            ACTION_SIMULATE_POINT -> {
                if (isTestMode) {
                    val pointType = intent.getStringExtra(EXTRA_POINT_TYPE)
                    val player = intent.getStringExtra(EXTRA_PLAYER)
                    simulatePointEvent(pointType, player)
                }
            }
        }
        
        return START_STICKY
    }
    
    /**
     * Binder class for clients to access the service.
     */
    inner class LocalBinder : android.os.Binder() {
        fun getService(): SensorService = this@SensorService
    }
    
    private val binder = LocalBinder()
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopSensorMonitoring()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }
    
    private fun startSensorMonitoring() {
        // Check for required permissions
        val bodySensorPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.BODY_SENSORS
        ) == PackageManager.PERMISSION_GRANTED
        
        val activityRecognitionPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
        
        val highSamplingRatePermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.HIGH_SAMPLING_RATE_SENSORS
        ) == PackageManager.PERMISSION_GRANTED
        
        val locationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || 
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!bodySensorPermission || !activityRecognitionPermission || 
            !highSamplingRatePermission || !locationPermission) {
            val missingPermissions = mutableListOf<String>().apply {
                if (!bodySensorPermission) add("BODY_SENSORS")
                if (!activityRecognitionPermission) add("ACTIVITY_RECOGNITION")
                if (!highSamplingRatePermission) add("HIGH_SAMPLING_RATE_SENSORS")
                if (!locationPermission) add("LOCATION")
            }
            
            val errorMessage = "Missing permissions: ${missingPermissions.joinToString()}"
            _sensorState.value = SensorState.Error(errorMessage)
            Log.e(TAG, errorMessage)
            
            // If in test mode, we can still proceed with simulated data
            if (isTestMode) {
                updateDebugInfo("Running in test mode with missing permissions. Simulated data only.")
                _sensorState.value = SensorState.Running
                return
            } else {
                return
            }
        }
        
        _sensorState.value = SensorState.Running
        
        // Register sensor listeners
        val success = sensorManager.registerListeners(
            callback = object : TennisRooSensorManager.SensorCallback {
                override fun onAccelerometerData(data: SensorData) {
                    // Update debug info with latest accelerometer data
                    if (isTestMode) {
                        updateDebugInfo("Accel: ${data.values.joinToString()}")
                    }
                }
                
                override fun onGyroscopeData(data: SensorData) {
                    // Update debug info with latest gyroscope data
                    if (isTestMode) {
                        updateDebugInfo("Gyro: ${data.values.joinToString()}")
                    }
                }
                
                override fun onHeartRateData(data: SensorData) {
                    // Update debug info with latest heart rate data
                    if (isTestMode) {
                        updateDebugInfo("HR: ${data.values.joinToString()}")
                    }
                }
                
                override fun onSensorWindowReady(window: SensorWindow) {
                    // Process sensor window for gesture classification
                    processSensorWindow(window)
                }
            }
        )
        
        if (!success) {
            _sensorState.value = SensorState.Error("Failed to register sensor listeners")
            Log.e(TAG, "Failed to register sensor listeners")
        } else {
            Log.d(TAG, "Sensor monitoring started")
        }
    }
    
    private fun stopSensorMonitoring() {
        sensorManager.unregisterListeners()
        sensorManager.clearBuffers()
        _sensorState.value = SensorState.Idle
        Log.d(TAG, "Sensor monitoring stopped")
    }
    
    private fun processSensorWindow(window: SensorWindow) {
        if (isTestMode) {
            updateDebugInfo("Processing window: ${window.accelerometerData.size} accel, ${window.gyroscopeData.size} gyro")
        }
        
        serviceScope.launch {
            try {
                val pointEvent = gestureClassifier.classify(window)
                if (pointEvent != null) {
                    _lastPointEvent.value = pointEvent
                    Log.d(TAG, "Point detected: $pointEvent")
                    updateDebugInfo("Point detected: ${pointEvent.type} by ${pointEvent.player}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error classifying gesture", e)
                _sensorState.value = SensorState.Error("Classification error: ${e.message}")
            }
        }
    }
    
    private fun simulatePointEvent(pointTypeStr: String?, playerStr: String?) {
        try {
            val pointType = pointTypeStr?.let { 
                com.example.tennis_roo.ml_gesture.PointType.valueOf(it) 
            } ?: com.example.tennis_roo.ml_gesture.PointType.FOREHAND
            
            val player = playerStr?.let { 
                com.example.tennis_roo.ml_gesture.Player.valueOf(it) 
            } ?: com.example.tennis_roo.ml_gesture.Player.PLAYER_1
            
            val simulatedEvent = PointEvent(
                timestamp = System.currentTimeMillis(),
                type = pointType,
                confidence = 0.95f,
                player = player
            )
            
            _lastPointEvent.value = simulatedEvent
            Log.d(TAG, "Simulated point: $simulatedEvent")
            updateDebugInfo("Simulated point: ${simulatedEvent.type} by ${simulatedEvent.player}")
        } catch (e: Exception) {
            Log.e(TAG, "Error simulating point", e)
        }
    }
    
    private fun updateDebugInfo(info: String) {
        _debugInfo.value = info
    }
    
    private fun createNotificationChannel() {
        val name = getString(R.string.sensor_service_notification_title)
        val descriptionText = getString(R.string.sensor_service_notification_text)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.sensor_service_notification_title))
            .setContentText(getString(R.string.sensor_service_notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    /**
     * Tries to start the service as a foreground service with different types.
     * 
     * @return true if successful, false otherwise
     */
    private fun tryStartForeground(): Boolean {
        val notification = createNotification()
        
        // Try with all types combined
        try {
            val allTypes = FOREGROUND_SERVICE_TYPE_DATA_SYNC or 
                           FOREGROUND_SERVICE_TYPE_LOCATION or 
                           FOREGROUND_SERVICE_TYPE_HEALTH
            startForeground(NOTIFICATION_ID, notification, allTypes)
            Log.d(TAG, "Started as foreground service with all types")
            updateDebugInfo("Running as foreground service (all types)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start as foreground service with all types: ${e.message}")
        }
        
        // Try with just data sync type
        try {
            startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            Log.d(TAG, "Started as foreground service with DATA_SYNC type")
            updateDebugInfo("Running as foreground service (data sync)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start as foreground service with DATA_SYNC type: ${e.message}")
        }
        
        // Try with health type
        try {
            startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_HEALTH)
            Log.d(TAG, "Started as foreground service with HEALTH type")
            updateDebugInfo("Running as foreground service (health)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start as foreground service with HEALTH type: ${e.message}")
        }
        
        // Try with no specific type
        try {
            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "Started as foreground service with no specific type")
            updateDebugInfo("Running as foreground service (generic)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start as foreground service with no specific type: ${e.message}")
        }
        
        return false
    }
}

sealed class SensorState {
    object Idle : SensorState()
    object Running : SensorState()
    data class Error(val message: String) : SensorState()
}
