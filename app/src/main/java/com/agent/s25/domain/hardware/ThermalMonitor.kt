package com.agent.s25.domain.hardware

import android.content.Context
import android.os.Build
import android.os.HardwarePropertiesManager
import android.util.Log
import com.agent.s25.domain.engine.LlamaBridge

class ThermalMonitor(private val context: Context) {
    companion object {
        private const val TAG = "ThermalMonitor"
        private const val THROTTLE_TEMP_CELSIUS = 41.0f
    }

    private var isThrottled = false

    fun checkDeviceTemperature() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val hwManager = context.getSystemService(Context.HARDWARE_PROPERTIES_SERVICE) as? HardwarePropertiesManager
            val cpuTemps = hwManager?.getDeviceTemperatures(
                HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU,
                HardwarePropertiesManager.TEMPERATURE_CURRENT
            )

            val maxTemp = cpuTemps?.maxOrNull() ?: 35.0f
            Log.d(TAG, "Current S25 Ultra SoC Temperature: $maxTemp °C")

            if (maxTemp >= THROTTLE_TEMP_CELSIUS && !isThrottled) {
                Log.w(TAG, "High temperature detected ($maxTemp °C). ThermalGuard down-shifting Oryon threads to 4.")
                LlamaBridge.nativeSetThreadCount(4)
                isThrottled = true
            } else if (maxTemp < THROTTLE_TEMP_CELSIUS - 3.0f && isThrottled) {
                Log.i(TAG, "Device cooled down ($maxTemp °C). Restoring full 6 performance threads.")
                LlamaBridge.nativeSetThreadCount(6)
                isThrottled = false
            }
        }
    }
}
