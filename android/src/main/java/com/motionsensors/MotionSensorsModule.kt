package com.motionsensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule

class MotionSensorsModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), SensorEventListener {

    companion object {
        private const val MODULE_NAME = "MotionSensors"
        private const val EVENT_NAME = "onMotionData"
        private const val DEFAULT_INTERVAL_MS = 33L
    }

    private val sensorManager: SensorManager =
        reactContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val linearAccelSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private val gameRotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

    private val transformer = WorldFrameTransformer()

    // Latest linear acceleration values from the sensor
    private var latestAccel: FloatArray = FloatArray(3)
    private var latestAccelTimestamp: Long = 0L

    // Throttle: timestamp of the last emission to JS (in milliseconds)
    private var lastEmitMs: Long = 0L
    private var throttleIntervalMs: Long = DEFAULT_INTERVAL_MS

    // ---------------------------------------------------------------------------
    // ReactContextBaseJavaModule
    // ---------------------------------------------------------------------------

    override fun getName(): String = MODULE_NAME

    // ---------------------------------------------------------------------------
    // Public API (called from JS)
    // ---------------------------------------------------------------------------

    @ReactMethod
    fun start(updateIntervalMs: Double) {
        throttleIntervalMs = if (updateIntervalMs > 0) updateIntervalMs.toLong() else DEFAULT_INTERVAL_MS
        lastEmitMs = 0L

        linearAccelSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gameRotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    @ReactMethod
    fun stop() {
        sensorManager.unregisterListener(this)
    }

    @ReactMethod
    fun isAvailable(promise: Promise) {
        val available = linearAccelSensor != null && gameRotationSensor != null
        promise.resolve(available)
    }

    /** Required by NativeEventEmitter on the JS side — no-op. */
    @ReactMethod
    fun addListener(eventName: String) {
        // no-op
    }

    /** Required by NativeEventEmitter on the JS side — no-op. */
    @ReactMethod
    fun removeListeners(count: Double) {
        // no-op
    }

    // ---------------------------------------------------------------------------
    // SensorEventListener
    // ---------------------------------------------------------------------------

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                transformer.updateRotation(event.values, event.timestamp)
            }

            Sensor.TYPE_LINEAR_ACCELERATION -> {
                latestAccel[0] = event.values[0]
                latestAccel[1] = event.values[1]
                latestAccel[2] = event.values[2]
                latestAccelTimestamp = event.timestamp

                maybeEmit()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // not used
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    private fun maybeEmit() {
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastEmitMs < throttleIntervalMs) return

        val result = transformer.transform(latestAccel, latestAccelTimestamp) ?: return

        lastEmitMs = nowMs

        val params = Arguments.createMap().apply {
            putDouble("forwardAcceleration", result.forwardAcceleration.toDouble())
            putDouble("lateralAcceleration", result.lateralAcceleration.toDouble())
            putDouble("verticalAcceleration", result.verticalAcceleration.toDouble())
            putDouble("yawRate", result.yawRate.toDouble())
            putDouble("pitch", result.pitch.toDouble())
            putDouble("roll", result.roll.toDouble())
            putDouble("timestamp", latestAccelTimestamp.toDouble())
        }

        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(EVENT_NAME, params)
    }
}
