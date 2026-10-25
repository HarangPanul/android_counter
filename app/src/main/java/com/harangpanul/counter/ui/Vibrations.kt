package com.harangpanul.counter.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

private const val INCREMENT_MS = 50L
private const val RESET_MS = 500L

/** 최대 진폭의 진동을 낸다. 진폭 제어를 지원하지 않는 기기는 기본 세기로 울린다. */
class Vibrations(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /** +1 시: 짧고 강한 진동 */
    fun increment() = vibrate(INCREMENT_MS)

    /** 초기화 시: 긴 진동 */
    fun reset() = vibrate(RESET_MS)

    private fun vibrate(durationMs: Long) {
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(durationMs, 255))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(durationMs)
        }
    }
}
