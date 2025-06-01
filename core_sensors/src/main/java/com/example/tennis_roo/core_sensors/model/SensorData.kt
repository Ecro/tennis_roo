package com.example.tennis_roo.core_sensors.model

/**
 * Data class representing a single sensor reading with timestamp.
 */
data class SensorData(
    val timestamp: Long,
    val sensorType: SensorType,
    val values: FloatArray,
    val accuracy: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorData

        if (timestamp != other.timestamp) return false
        if (sensorType != other.sensorType) return false
        if (!values.contentEquals(other.values)) return false
        if (accuracy != other.accuracy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + sensorType.hashCode()
        result = 31 * result + values.contentHashCode()
        result = 31 * result + accuracy
        return result
    }
}

/**
 * Enum representing the types of sensors used in the application.
 */
enum class SensorType {
    ACCELEROMETER,
    GYROSCOPE,
    HEART_RATE,
    UNKNOWN
}

/**
 * Extension function to convert Android sensor type to our SensorType enum.
 */
fun Int.toSensorType(): SensorType {
    return when (this) {
        android.hardware.Sensor.TYPE_ACCELEROMETER -> SensorType.ACCELEROMETER
        android.hardware.Sensor.TYPE_GYROSCOPE -> SensorType.GYROSCOPE
        android.hardware.Sensor.TYPE_HEART_RATE -> SensorType.HEART_RATE
        else -> SensorType.UNKNOWN
    }
}

/**
 * Data class representing a window of sensor data for analysis.
 */
data class SensorWindow(
    val accelerometerData: List<SensorData>,
    val gyroscopeData: List<SensorData>,
    val heartRateData: List<SensorData>,
    val windowStartTime: Long,
    val windowEndTime: Long
)
