import { NativeEventEmitter, Platform } from 'react-native';
import NativeMotionSensors from './NativeMotionSensors';
import type { MotionData, MotionSensorConfig } from './types';

export type { MotionData, MotionSensorConfig } from './types';

type MotionDataListener = (data: MotionData) => void;

const eventEmitter = new NativeEventEmitter(NativeMotionSensors as any);

let listenerCount = 0;

export function start(config: MotionSensorConfig = { updateInterval: 33 }): void {
  NativeMotionSensors.start(config.updateInterval);
}

export function stop(): void {
  NativeMotionSensors.stop();
}

export function isAvailable(): Promise<boolean> {
  return NativeMotionSensors.isAvailable();
}

export function subscribe(listener: MotionDataListener): () => void {
  listenerCount++;
  const subscription = eventEmitter.addListener('onMotionData', listener);

  return () => {
    subscription.remove();
    listenerCount--;
  };
}
