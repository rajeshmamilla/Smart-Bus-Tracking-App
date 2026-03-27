package com.example.bustrackv2.models

data class Coordinates(
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class VehicleLocation(
    val vehicle_number: String = "",
    val from: String = "",
    val to: String = "",
    val date: String = "",
    val is_active: Boolean = false,
    val coordinates: Coordinates = Coordinates(),
    val last_updated: Long = System.currentTimeMillis(),
    val next_depot: String = "",
    val expected_platform: String = "",
    val speed: Double = 0.0
) 