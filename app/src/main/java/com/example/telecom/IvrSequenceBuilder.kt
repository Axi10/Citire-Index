package com.example.telecom

import com.example.data.model.UtilityConfig

object IvrSequenceBuilder {

    private val TEMPLATE_CHARS = Regex("[0-9,#*+X]+")

    /**
     * Replaces XXXX with the meter index in the template.
     */
    fun buildDialString(template: String, meterIndex: String): String {
        val cleanIndex = meterIndex.trim().replace(" ", "")
        return if (template.contains("XXXX")) {
            template.replace("XXXX", cleanIndex)
        } else {
            // fallback if user removed placeholder
            "$template,$cleanIndex#,,1"
        }
    }

    /**
     * Builds dial string from a UtilityConfig and the entered index.
     */
    fun buildDialString(config: UtilityConfig, meterIndex: String): String {
        return buildDialString(config.ivrTemplate, meterIndex)
    }

    /**
     * A usable template has exactly one XXXX placeholder and only characters a phone can dial
     * (digits, comma pause, # and *, +).
     */
    fun isValidTemplate(template: String): Boolean {
        val trimmed = template.trim()
        return trimmed.matches(TEMPLATE_CHARS) && trimmed.split("XXXX").size == 2
    }

    /**
     * Builds a test dial string that stops right before entering the meter index.
     * This allows the user to test the call and verify that the IVR robot reaches
     * the "introduceti indexul" prompt without actually submitting an index.
     */
    fun buildTestSequenceUntilIndex(template: String): String {
        val indexPos = template.indexOf("XXXX")
        return if (indexPos != -1) {
            template.substring(0, indexPos)
        } else {
            template
        }
    }

    /**
     * Estimates total automated call duration in seconds based on pause commas.
     * Each comma on Android GSM standard is ~2 to 3 seconds.
     */
    fun estimateDurationSeconds(sequence: String): Int {
        val commaCount = sequence.count { it == ',' }
        return (commaCount * 2.5).toInt() + 10 // +10s base connection & tone transmission
    }
}
