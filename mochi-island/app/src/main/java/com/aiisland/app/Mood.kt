package com.aiisland.app

enum class Mood(val label: String) {
    NEUTRAL("Santai"),
    HAPPY("Gembira"),
    SLEEPY("Mengantuk"),
    SLEEP("Tidur"),
    DIZZY("Pening"),
    SURPRISED("Terkejut"),
    RELAXED("Tenang"),
    LISTENING("Mendengar"),
    DANCE("Menari"),
    ENERGY("Bertenaga"),
    HEADPHONES("Fon kepala"),
    PARTY("Raikan!")
}

data class Phone(
    val battery: Int = 80,
    val charging: Boolean = false,
    val hour: Int = 12,
    val tilt: Float = 0f,
    val faceDown: Boolean = false,
    val dizzy: Boolean = false,
    val headphones: Boolean = false,
    val dnd: Boolean = false
)

fun moodOf(p: Phone): Mood = when {
    p.dizzy -> Mood.DIZZY
    p.faceDown -> Mood.SLEEP
    p.dnd -> Mood.SLEEP
    p.hour >= 23 || p.hour < 5 -> Mood.SLEEPY
    p.battery <= 15 && !p.charging -> Mood.SLEEPY
    p.headphones -> Mood.HEADPHONES
    p.charging -> Mood.ENERGY
    else -> Mood.NEUTRAL
}
