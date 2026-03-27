package com.motionsensors

import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

class MotionSensorsPackage : BaseReactPackage() {
    override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? =
        if (name == MotionSensorsModule.NAME) {
            MotionSensorsModule(reactContext)
        } else {
            null
        }

    override fun getReactModuleInfoProvider() = ReactModuleInfoProvider {
        mapOf(
            MotionSensorsModule.NAME to ReactModuleInfo(
                MotionSensorsModule.NAME,
                MotionSensorsModule.NAME,
                false,
                false,
                false,
                true
            )
        )
    }
}
