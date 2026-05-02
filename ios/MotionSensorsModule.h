#import <Foundation/Foundation.h>
#import <React/RCTEventEmitter.h>
#import <MotionSensorsSpec/MotionSensorsSpec.h>

NS_ASSUME_NONNULL_BEGIN

@interface MotionSensorsModule : RCTEventEmitter <NativeMotionSensorsSpec>
@end

NS_ASSUME_NONNULL_END
