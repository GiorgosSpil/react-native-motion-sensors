package com.motionsensors

import android.hardware.SensorManager
import kotlin.math.atan2
import kotlin.math.sqrt

data class MotionResult(
    val forwardAcceleration: Float,
    val lateralAcceleration: Float,
    val verticalAcceleration: Float,
    val yawRate: Float,
    val pitch: Float,
    val roll: Float,
)

class WorldFrameTransformer {

    // 3x3 rotation matrix stored in row-major order (9 elements)
    private val rotationMatrix = FloatArray(9)
    private var lastAzimuth: Float = 0f
    private var lastAzimuthTimestamp: Long = 0L
    private var hasRotation: Boolean = false

    /**
     * Update the rotation matrix from a TYPE_GAME_ROTATION_VECTOR sensor event.
     *
     * @param rotationVector The sensor values array from the event (at least 4 elements)
     * @param timestamp      The event timestamp in nanoseconds
     */
    fun updateRotation(rotationVector: FloatArray, timestamp: Long) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)
        hasRotation = true
        // Update last azimuth timestamp so yaw rate computation has a reference point
        if (lastAzimuthTimestamp == 0L) {
            lastAzimuth = extractAzimuth()
            lastAzimuthTimestamp = timestamp
        }
    }

    /**
     * Transform device-frame linear acceleration into world-frame motion data.
     *
     * @param deviceAccel FloatArray of [x, y, z] in device frame (gravity already removed)
     * @param timestamp   The sensor event timestamp in nanoseconds
     * @return A [MotionResult] with all 6 motion fields, or null if no rotation is available yet
     */
    fun transform(deviceAccel: FloatArray, timestamp: Long): MotionResult? {
        if (!hasRotation) return null

        val R = rotationMatrix

        // Rotate device-frame acceleration into world frame.
        // R is the device-to-world rotation so: worldAccel = R * deviceAccel
        // R layout (row-major):
        //   [ R[0]  R[1]  R[2] ]
        //   [ R[3]  R[4]  R[5] ]
        //   [ R[6]  R[7]  R[8] ]
        // worldX = East, worldY = North, worldZ = Up
        val worldX = R[0] * deviceAccel[0] + R[1] * deviceAccel[1] + R[2] * deviceAccel[2]
        val worldY = R[3] * deviceAccel[0] + R[4] * deviceAccel[1] + R[5] * deviceAccel[2]
        val worldZ = R[6] * deviceAccel[0] + R[7] * deviceAccel[1] + R[8] * deviceAccel[2]

        // Extract azimuth (heading) from rotation matrix: angle around Z axis (Up)
        // atan2(R[1], R[4]) gives heading measured from North, clockwise
        val azimuth = extractAzimuth()

        // Decompose horizontal world acceleration into forward/lateral relative to heading
        val sinAz = Math.sin(azimuth.toDouble()).toFloat()
        val cosAz = Math.cos(azimuth.toDouble()).toFloat()
        val forward = worldX * sinAz + worldY * cosAz
        val lateral = worldX * cosAz - worldY * sinAz

        // Compute yaw rate from azimuth delta between this sample and the last
        val yawRate = computeYawRate(azimuth, timestamp)

        // Extract pitch: rotation around the lateral (East) axis
        // pitch = atan2(-R[6], sqrt(R[7]^2 + R[8]^2))
        val pitch = Math.toDegrees(
            atan2(-R[6], sqrt(R[7] * R[7] + R[8] * R[8])).toDouble()
        ).toFloat()

        // Extract roll: rotation around the forward (North) axis
        // roll = atan2(R[7], R[8])
        val roll = Math.toDegrees(
            atan2(R[7], R[8]).toDouble()
        ).toFloat()

        return MotionResult(
            forwardAcceleration = forward,
            lateralAcceleration = lateral,
            verticalAcceleration = worldZ,
            yawRate = yawRate,
            pitch = pitch,
            roll = roll,
        )
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    private fun extractAzimuth(): Float {
        // Azimuth = atan2(R[1], R[4]) — heading measured clockwise from North
        return atan2(rotationMatrix[1], rotationMatrix[4])
    }

    private fun computeYawRate(currentAzimuth: Float, timestamp: Long): Float {
        if (lastAzimuthTimestamp == 0L) {
            lastAzimuth = currentAzimuth
            lastAzimuthTimestamp = timestamp
            return 0f
        }

        val dtSeconds = (timestamp - lastAzimuthTimestamp) / 1_000_000_000.0f
        if (dtSeconds <= 0f) return 0f

        var deltaAzimuth = Math.toDegrees(
            (currentAzimuth - lastAzimuth).toDouble()
        ).toFloat()

        // Normalize delta to [-180, 180]
        while (deltaAzimuth > 180f) deltaAzimuth -= 360f
        while (deltaAzimuth < -180f) deltaAzimuth += 360f

        val yawRate = deltaAzimuth / dtSeconds

        lastAzimuth = currentAzimuth
        lastAzimuthTimestamp = timestamp

        return yawRate
    }
}
