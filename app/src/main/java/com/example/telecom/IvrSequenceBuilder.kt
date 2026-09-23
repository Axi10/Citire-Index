package com.example.telecom

import com.example.data.model.UtilityConfig
import com.example.data.model.UtilityType

data class IvrStep(
    val stepNumber: Int,
    val title: String,
    val dtmfSequence: String,
    val pauseSeconds: Int,
    val explanation: String
)

object IvrSequenceBuilder {

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
     * Breaks down the sequence into human-readable steps for the user UI.
     */
    fun explainSequence(type: UtilityType, rawSequence: String, indexInput: String): List<IvrStep> {
        val steps = mutableListOf<IvrStep>()
        var stepNum = 1

        val parts = rawSequence.split(",")
        var accumulatedPauses = 0

        if (type == UtilityType.ELECTRICITY) {
            steps.add(
                IvrStep(
                    stepNumber = stepNum++,
                    title = "Inițiere apel TelVerde Curent",
                    dtmfSequence = "0800070701",
                    pauseSeconds = 2,
                    explanation = "Apelează automat numărul gratuit de relații clienți / autocitire."
                )
            )
            steps.add(
                IvrStep(
                    stepNumber = stepNum++,
                    title = "Selectare meniu Transmitere Index",
                    dtmfSequence = "Tasta 1",
                    pauseSeconds = 4,
                    explanation = "Trimite tasta 1 pentru secțiunea de transmitere index contor."
                )
            )
            steps.add(
                IvrStep(
                    stepNumber = stepNum++,
                    title = "Introducere Cod Client / NLC",
                    dtmfSequence = "Cod client urmat de #",
                    pauseSeconds = 6,
                    explanation = "Transmite automat codul de client înregistrat și confirmă cu tasta #."
                )
            )
            steps.add(
                IvrStep(
                    stepNumber = stepNum++,
                    title = "Confirmare date client",
                    dtmfSequence = "Tasta 1",
                    pauseSeconds = 10,
                    explanation = "Confirmă identificatorul locului de consum și așteaptă solicitarea indexului."
                )
            )
            steps.add(
                IvrStep(
                    stepNumber = stepNum++,
                    title = "Transmitere Index Contor",
                    dtmfSequence = "${indexInput.ifBlank { "XXXX" }}#",
                    pauseSeconds = 4,
                    explanation = "Introduce cifrele indexului citit de pe contor urmate de diez (#)."
                )
            )
            steps.add(
                IvrStep(
                    stepNumber = stepNum++,
                    title = "Confirmare finală și încheiere",
                    dtmfSequence = "Tasta 1",
                    pauseSeconds = 2,
                    explanation = "Apasă tasta 1 pentru salvarea definitivă a indexului în baza distribuitorului."
                )
            )
        } else {
            steps.add(
                IvrStep(
                    stepNumber = stepNum++,
                    title = "Inițiere apel TelVerde Gaz",
                    dtmfSequence = "0800800200",
                    pauseSeconds = 2,
                    explanation = "Apelează numărul gratuit TelVerde furnizor/distribuitor gaze naturale."
                )
            )
            steps.add(
                IvrStep(
                    stepNumber = stepNum++,
                    title = "Transmitere Cod Client / NLC",
                    dtmfSequence = "Cod client urmat de #",
                    pauseSeconds = 6,
                    explanation = "Robotul preia codul clientului și identifică automat contractul."
                )
            )
            steps.add(
                IvrStep(
                    stepNumber = stepNum++,
                    title = "Transmitere Index Contor Gaz",
                    dtmfSequence = "${indexInput.ifBlank { "XXXX" }}#",
                    pauseSeconds = 4,
                    explanation = "Transmite valoarea indexului în m³ urmată de tasta diez (#)."
                )
            )
            steps.add(
                IvrStep(
                    stepNumber = stepNum++,
                    title = "Confirmare finală salvare",
                    dtmfSequence = "Tasta 1",
                    pauseSeconds = 2,
                    explanation = "Confirmă că indexul transmis este corect și înregistrează autocitirea."
                )
            )
        }

        return steps
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
