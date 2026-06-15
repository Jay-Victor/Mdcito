package com.mdcito.app.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticHelper {

    fun lightClick(context: Context) {
        performVibration(context, 10L)
    }

    fun mediumClick(context: Context) {
        performVibration(context, 20L)
    }

    fun heavyClick(context: Context) {
        performVibration(context, 40L)
    }

    fun dragStart(context: Context) {
        performVibration(context, 15L)
    }

    fun dragDrop(context: Context) {
        performVibration(context, 25L)
    }

    fun longPress(context: Context) {
        performVibration(context, 30L)
    }

    fun delete(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            performVibrationEffect(context, VibrationEffect.createOneShot(50L, 200))
        } else {
            performVibration(context, 50L)
        }
    }

    private fun performVibration(context: Context, duration: Long) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } catch (_: Exception) {}
    }

    private fun performVibrationEffect(context: Context, effect: VibrationEffect) {
        try {
            val vibrator = getVibrator(context) ?: return
            vibrator.vibrate(effect)
        } catch (_: Exception) {}
    }

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }
}
