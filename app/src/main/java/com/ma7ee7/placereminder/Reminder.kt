package com.ma7ee7.placereminder

data class Reminder(
    val id: String,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val enabled: Boolean = true,
    val triggered: Boolean = false
) {
    companion object {
        const val RADIUS_METERS: Float = 100f
    }
}
