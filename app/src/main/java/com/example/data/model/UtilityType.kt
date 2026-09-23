package com.example.data.model

enum class UtilityType(
    val title: String,
    val unit: String,
    val defaultDay: Int,
    val defaultPhone: String,
    val defaultClientCode: String,
    val defaultIvrTemplate: String,
    val defaultPrice: Double,
    val description: String
) {
    GAS(
        title = "Gaze Naturale",
        unit = "m³",
        defaultDay = 16,
        defaultPhone = "0800800200",
        defaultClientCode = "3730081",
        defaultIvrTemplate = "0800800200,3730081#,,,XXXX#,,1",
        defaultPrice = 3.02,
        description = "Transmitere index lunar gaz (recomandat pe 16 ale lunii)"
    ),
    ELECTRICITY(
        title = "Curent Electric",
        unit = "kWh",
        defaultDay = 24,
        defaultPhone = "0800070701",
        defaultClientCode = "111192991",
        defaultIvrTemplate = "0800070701,1,,111192991#,,,1,,,,,XXXX#,,1",
        defaultPrice = 1.64,
        description = "Transmitere index lunar energie electrică (recomandat pe 24 ale lunii)"
    )
}
