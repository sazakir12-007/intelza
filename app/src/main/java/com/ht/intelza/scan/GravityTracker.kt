package com.ht.intelza.scan

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.hypot

/**
 * Tracks which way is physically up, expressed in the device's natural (portrait)
 * screen coordinates with y pointing down. Falls back to "top of the phone is up" when
 * no motion sensor is available.
 */
class GravityTracker(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val gravitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val sensor = gravitySensor ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val needsFiltering = gravitySensor == null

    private var filteredX = 0f
    private var filteredY = SensorManager.GRAVITY_EARTH

    @Volatile
    var worldUp: Vec2 = Vec2.UP
        private set

    fun start() {
        val s = sensor ?: return
        sensorManager?.registerListener(this, s, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        var gx = event.values[0]
        var gy = event.values[1]
        if (needsFiltering) {
            // Low-pass filter isolates gravity from hand movement.
            filteredX = ALPHA * filteredX + (1 - ALPHA) * gx
            filteredY = ALPHA * filteredY + (1 - ALPHA) * gy
            gx = filteredX
            gy = filteredY
        }
        // Readings point away from the earth: +y is towards the top of the phone.
        // When the phone is nearly flat the in-screen component is too weak to trust,
        // so keep the last good direction.
        if (hypot(gx, gy) > MIN_IN_PLANE_GRAVITY) {
            worldUp = Vec2(gx, -gy)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val ALPHA = 0.8f
        const val MIN_IN_PLANE_GRAVITY = 3f
    }
}
