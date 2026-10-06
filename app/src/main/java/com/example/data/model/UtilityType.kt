package com.example.data.model

enum class UtilityType(
    val title: String,
    val unit: String,
    val defaultDay: Int,
    val defaultPhone: String,
    val defaultPrice: Double,
    val description: String
) {
    GAS(
        title = "Gaze Naturale",
        unit = "m³",
        defaultDay = 16,
        defaultPhone = "0800800200",
        defaultPrice = 3.02,
        description = "Transmitere index lunar gaz (recomandat pe 16 ale lunii)"
    ),
    ELECTRICITY(
        title = "Curent Electric",
        unit = "kWh",
        defaultDay = 24,
        defaultPhone = "0800070701",
        defaultPrice = 1.64,
        description = "Transmitere index lunar energie electrică (recomandat pe 24 ale lunii)"
    );

    // The client code is personal: the user enters it in Settings, it is never shipped in the app
    val defaultClientCode: String get() = ""

    /**
     * Dial string with a XXXX placeholder where the meter index is typed.
     * Commas are 2-3 second pauses, # confirms an entry, the other digits are menu choices.
     */
    fun ivrTemplateFor(phone: String, clientCode: String): String = when (this) {
        GAS -> "$phone,$clientCode#,,,XXXX#,,1"
        ELECTRICITY -> "$phone,1,,$clientCode#,,,1,,,,,XXXX#,,1"
    }

    val defaultIvrTemplate: String get() = ivrTemplateFor(defaultPhone, defaultClientCode)
}
