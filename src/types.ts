export interface MotionData {
  forwardAcceleration: number;   // m/s^2 (gravity removed, world frame)
  lateralAcceleration: number;   // m/s^2 (perpendicular)
  verticalAcceleration: number;  // m/s^2 (up/down)
  yawRate: number;               // deg/s (rotation around vertical axis)
  pitch: number;                 // device pitch (degrees)
  roll: number;                  // device roll (degrees)
  timestamp: number;             // ms
}

export interface MotionSensorConfig {
  updateInterval: number;  // ms. Default: 33 (~30Hz)
}
