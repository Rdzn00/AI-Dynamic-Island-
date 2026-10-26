package com.aiisland.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.io.File
import java.util.Locale

data class Stats(
    val battery: Int = 0,
    val charging: Boolean = false,
    val tempC: Float = 0f,
    val ramUsed: Float = 0f,
    val ramTotal: Float = 0f,
    val cpu: Int = -1,
    val ghz: Float = 0f,
    val gpu: Int = -1
)

fun fmt(v: Float): String = String.format(Locale.US, "%.1f", v)

private fun rd(path: String): String? = try {
    File(path).readText().trim()
} catch (e: Exception) {
    null
}

private fun cpuLoad(): Pair<Int, Float> {
    var sum = 0f
    var ghz = 0f
    var n = 0
    for (i in 0 until Runtime.getRuntime().availableProcessors()) {
        val d = "/sys/devices/system/cpu/cpu$i/cpufreq/"
        val cur = rd(d + "scaling_cur_freq")?.toLongOrNull()
        val max = rd(d + "cpuinfo_max_freq")?.toLongOrNull()
        val min = rd(d + "cpuinfo_min_freq")?.toLongOrNull() ?: 0L
        if (cur != null && max != null && max > min) {
            sum += (cur - min).toFloat() / (max - min).toFloat()
            ghz += cur / 1_000_000f
            n++
        }
    }
    return if (n == 0) Pair(-1, 0f) else Pair((sum / n * 100f).toInt().coerceIn(0, 100), ghz / n)
}

private fun gpuLoad(): Int {
    val a = rd("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage")?.substringBefore('%')?.trim()?.toIntOrNull()
    if (a != null) return a.coerceIn(0, 100)
    val b = rd("/sys/class/kgsl/kgsl-3d0/gpubusy")
        ?.split(" ")
        ?.filter { it.isNotBlank() }
        ?.mapNotNull { it.toLongOrNull() }
    if (b != null && b.size >= 2 && b[1] > 0L) return (b[0] * 100L / b[1]).toInt().coerceIn(0, 100)
    val c = rd("/sys/kernel/gpu/gpu_busy")?.substringBefore('%')?.trim()?.toIntOrNull()
    if (c != null) return c.coerceIn(0, 100)
    val d = rd("/sys/class/misc/mali0/device/utilization")?.trim()?.toIntOrNull()
    if (d != null) return d.coerceIn(0, 100)
    return -1
}

fun readStats(c: Context): Stats {
    val b = c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = b?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val status = b?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val temp = (b?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
    val mi = ActivityManager.MemoryInfo()
    c.getSystemService(ActivityManager::class.java).getMemoryInfo(mi)
    val gb = 1024f * 1024f * 1024f
    val cpu = cpuLoad()
    return Stats(
        battery = if (level >= 0 && scale > 0) level * 100 / scale else 0,
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL,
        tempC = temp,
        ramUsed = (mi.totalMem - mi.availMem) / gb,
        ramTotal = mi.totalMem / gb,
        cpu = cpu.first,
        ghz = cpu.second,
        gpu = gpuLoad()
    )
}
