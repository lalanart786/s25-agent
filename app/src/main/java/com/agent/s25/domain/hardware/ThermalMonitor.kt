package com.agent.s25.domain.hardware

import android.content.Context
import android.os.Build
import android.os.HardwarePropertiesManager
import android.util.Log

class ThermalMonitor(private val context: Context) {
    companion object {
        private const val TAG = "ThermalMonitor"
        private const val THROTTLE_TEMP_CELSIUS = 41.0f
    }

    private var isThrottled = false

    fun checkDeviceTemperature() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val hwManager = context.getSystemService(Context.HARDWARE_PROPERTIES_SERVICE) as? HardwarePropertiesManager
                    ?: return
                val cpuTemps = hwManager.getDeviceTemperatures(
                    HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU,
                    HardwarePropertiesManager.TEMPERATURE_CURRENT
                )

                val maxTemp = cpuTemps?.maxOrNull() ?: 35.0f
                Log.d(TAG, "Current S25 Ultra SoC Temperature: $maxTemp °C")

                if (maxTemp >= THROTTLE_TEMP_CELSIUS && !isThrottled) {
                    Log.w(TAG, "High temperature detected ($maxTemp °C). ThermalGuard flagging throttle.")
                    isThrottled = true
                    // Native thread adjustment deferred until model is loaded
                } else if (maxTemp < THROTTLE_TEMP_CELSIUS - 3.0f && isThrottled) {
                    Log.i(TAG, "Device cooled down ($maxTemp °C). Throttle flag cleared.")
                    isThrottled = false
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Temperature monitoring not available (requires system permission): ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Temperature check skipped: ${e.message}")
        }
    }

    fun isCurrentlyThrottled(): Boolean = isThrottled
}
