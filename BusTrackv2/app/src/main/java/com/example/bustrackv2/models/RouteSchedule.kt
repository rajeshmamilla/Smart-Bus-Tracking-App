package com.example.bustrackv2.models

data class RouteSchedule(
    val bus_type: String = "",
    val coordinates: Map<String, String> = emptyMap(),
    val date: String = "",
    val departure_time: String = "",
    val from: String = "",
    val is_active: Boolean = false,
    val platform_number: String = "",
    val service_id: String = "",
    val to: String = "",
    val vehicle_number: String = ""
) 