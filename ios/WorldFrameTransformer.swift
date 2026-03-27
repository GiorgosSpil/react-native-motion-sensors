import CoreMotion
import Foundation

// MARK: - MotionResult

@objc public class MotionResult: NSObject {
    @objc public let forwardAcceleration: Double
    @objc public let lateralAcceleration: Double
    @objc public let verticalAcceleration: Double
    @objc public let yawRate: Double
    @objc public let pitch: Double
    @objc public let roll: Double

    init(
        forwardAcceleration: Double,
        lateralAcceleration: Double,
        verticalAcceleration: Double,
        yawRate: Double,
        pitch: Double,
        roll: Double
    ) {
        self.forwardAcceleration = forwardAcceleration
        self.lateralAcceleration = lateralAcceleration
        self.verticalAcceleration = verticalAcceleration
        self.yawRate = yawRate
        self.pitch = pitch
        self.roll = roll
    }
}

// MARK: - WorldFrameTransformer

@objc public class WorldFrameTransformer: NSObject {

    // Conversion factor: CoreMotion userAcceleration is in G's
    private static let kGravity: Double = 9.80665

    private var prevYaw: Double = 0
    private var prevTimestamp: TimeInterval = 0

    /// Transform a CMDeviceMotion sample into world-frame MotionResult.
    @objc public func transform(_ motion: CMDeviceMotion) -> MotionResult {
        let R = motion.attitude.rotationMatrix
        let ua = motion.userAcceleration

        // Convert userAcceleration from G's to m/s^2
        let uaX = ua.x * WorldFrameTransformer.kGravity
        let uaY = ua.y * WorldFrameTransformer.kGravity
        let uaZ = ua.z * WorldFrameTransformer.kGravity

        // Rotate device-frame acceleration into world frame.
        // R is the device-to-world rotation, so: worldAccel = R * deviceAccel
        // worldX = East, worldY = North, worldZ = Up
        let worldX = R.m11 * uaX + R.m12 * uaY + R.m13 * uaZ
        let worldY = R.m21 * uaX + R.m22 * uaY + R.m23 * uaZ
        let worldZ = R.m31 * uaX + R.m32 * uaY + R.m33 * uaZ

        // Azimuth: yaw reported by CMAttitude (radians, measured around the Z/Up axis)
        let azimuth = motion.attitude.yaw

        // Decompose horizontal world acceleration relative to current heading
        // forward = component along heading direction
        // lateral = component perpendicular to heading (positive = right)
        let sinAz = sin(azimuth)
        let cosAz = cos(azimuth)
        let forward = worldX * sinAz + worldY * cosAz
        let lateral = worldX * cosAz - worldY * sinAz

        // Compute yaw rate from yaw delta between consecutive samples (deg/s)
        let yawRate = computeYawRate(currentYaw: azimuth, timestamp: motion.timestamp)

        // Pitch and roll from CMAttitude, converted to degrees
        let pitchDeg = motion.attitude.pitch * (180.0 / .pi)
        let rollDeg  = motion.attitude.roll  * (180.0 / .pi)

        return MotionResult(
            forwardAcceleration: forward,
            lateralAcceleration: lateral,
            verticalAcceleration: worldZ,
            yawRate: yawRate,
            pitch: pitchDeg,
            roll: rollDeg
        )
    }

    // MARK: - Private helpers

    /// Returns yaw rate in deg/s, normalized to [-PI, PI] range before dividing.
    private func computeYawRate(currentYaw: Double, timestamp: TimeInterval) -> Double {
        defer {
            prevYaw = currentYaw
            prevTimestamp = timestamp
        }

        guard prevTimestamp > 0 else {
            return 0
        }

        let dt = timestamp - prevTimestamp
        guard dt > 0 else { return 0 }

        var deltaYaw = currentYaw - prevYaw

        // Normalize delta to [-PI, PI]
        while deltaYaw > .pi  { deltaYaw -= 2 * .pi }
        while deltaYaw < -.pi { deltaYaw += 2 * .pi }

        // Convert radians to degrees before dividing by dt
        return (deltaYaw * (180.0 / .pi)) / dt
    }
}
