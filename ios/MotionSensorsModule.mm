#import "MotionSensorsModule.h"
#import <CoreMotion/CoreMotion.h>

// Generated Swift header -- supports both framework and non-framework builds.
#if __has_include("react_native_motion_sensors-Swift.h")
#import "react_native_motion_sensors-Swift.h"
#else
#import <react_native_motion_sensors/react_native_motion_sensors-Swift.h>
#endif

@implementation MotionSensorsModule {
    CMMotionManager *_motionManager;
    WorldFrameTransformer *_transformer;
    NSTimeInterval _throttleIntervalSec;
    NSTimeInterval _lastEmitTime;
}

RCT_EXPORT_MODULE(MotionSensors)

// ---------------------------------------------------------------------------
// TurboModule
// ---------------------------------------------------------------------------

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:(const facebook::react::ObjCTurboModule::InitParams &)params {
    return std::make_shared<facebook::react::NativeMotionSensorsSpecJSI>(params);
}

+ (BOOL)requiresMainQueueSetup {
    return NO;
}

// ---------------------------------------------------------------------------
// Lifecycle
// ---------------------------------------------------------------------------

- (instancetype)init {
    if (self = [super init]) {
        _motionManager = [[CMMotionManager alloc] init];
        _transformer   = [[WorldFrameTransformer alloc] init];
        _throttleIntervalSec = 0.033; // ~30 Hz default
        _lastEmitTime        = 0.0;
    }
    return self;
}

- (void)dealloc {
    [_motionManager stopDeviceMotionUpdates];
}

// ---------------------------------------------------------------------------
// JS API
// ---------------------------------------------------------------------------

- (void)start:(double)updateIntervalMs {
    if (updateIntervalMs > 0) {
        _throttleIntervalSec = updateIntervalMs / 1000.0;
    } else {
        _throttleIntervalSec = 0.033;
    }
    _lastEmitTime = 0.0;

    if (!_motionManager.isDeviceMotionAvailable) {
        return;
    }

    // Stop any previous updates before reconfiguring.
    [_motionManager stopDeviceMotionUpdates];

    // Use xArbitraryCorrectedZVertical for a stable, drift-corrected heading.
    CMAttitudeReferenceFrame referenceFrame = CMAttitudeReferenceFrameXArbitraryCorrectedZVertical;

    __weak __typeof(self) weakSelf = self;
    NSOperationQueue *queue = [[NSOperationQueue alloc] init];
    queue.maxConcurrentOperationCount = 1;
    queue.qualityOfService = NSQualityOfServiceUserInteractive;

    [_motionManager startDeviceMotionUpdatesUsingReferenceFrame:referenceFrame
                                                        toQueue:queue
                                                    withHandler:^(CMDeviceMotion *motion, NSError *error) {
        if (!motion || error) return;

        __typeof(self) strongSelf = weakSelf;
        if (!strongSelf) return;

        // Throttle emissions to the requested interval.
        NSTimeInterval now = motion.timestamp;
        if (now - strongSelf->_lastEmitTime < strongSelf->_throttleIntervalSec) {
            return;
        }
        strongSelf->_lastEmitTime = now;

        MotionResult *result = [strongSelf->_transformer transform:motion];

        // timestamp in milliseconds (motion.timestamp is seconds since device boot)
        double timestampMs = now * 1000.0;

        [strongSelf sendEventWithName:@"onMotionData" body:@{
            @"forwardAcceleration"  : @(result.forwardAcceleration),
            @"lateralAcceleration"  : @(result.lateralAcceleration),
            @"verticalAcceleration" : @(result.verticalAcceleration),
            @"yawRate"              : @(result.yawRate),
            @"pitch"                : @(result.pitch),
            @"roll"                 : @(result.roll),
            @"timestamp"            : @(timestampMs),
        }];
    }];
}

- (void)stop {
    [_motionManager stopDeviceMotionUpdates];
    _lastEmitTime = 0.0;
}

- (void)isAvailable:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
    resolve(@(_motionManager.isDeviceMotionAvailable));
}

// ---------------------------------------------------------------------------
// RCTEventEmitter
// ---------------------------------------------------------------------------

- (NSArray<NSString *> *)supportedEvents {
    return @[@"onMotionData"];
}

// Forward TurboModule-spec listener calls to RCTEventEmitter's bookkeeping.
- (void)addListener:(NSString *)eventName {
    [super addListener:eventName];
}

- (void)removeListeners:(double)count {
    [super removeListeners:count];
}

@end
