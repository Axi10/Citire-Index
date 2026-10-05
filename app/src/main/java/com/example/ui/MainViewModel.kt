package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.MeterReading
import com.example.data.model.UtilityConfig
import com.example.data.model.UtilityType
import com.example.data.repository.MeterRepository
import com.example.notifications.ReminderScheduler
import com.example.pdf.PdfReportGenerator
import com.example.telecom.CallHelper
import com.example.telecom.CallLogHelper
import com.example.telecom.CallStatusEvaluation
import com.example.telecom.CallVerificationResult
import com.example.telecom.IvrSequenceBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        // How long we keep waiting for the call to show up in the call log
        const val PENDING_CALL_TIMEOUT_MS = 10 * 60 * 1000L
        const val MAX_INDEX_DIGITS = 9
    }

    private val repository: MeterRepository = AppDatabase.getDatabase(application).let { database ->
        MeterRepository(database.meterDao(), database.configDao())
    }

    // Active bottom navigation tab (0: Transmit, 1: History & Charts, 2: PDF, 3: Settings)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Currently active utility in Transmit screen
    private val _activeUtility = MutableStateFlow(UtilityType.ELECTRICITY)
    val activeUtility: StateFlow<UtilityType> = _activeUtility.asStateFlow()

    // Meter Index Input
    private val _indexInput = MutableStateFlow("")
    val indexInput: StateFlow<String> = _indexInput.asStateFlow()

    // User message / snackbar
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Generated PDF reports list
    private val _generatedPdf = MutableStateFlow<File?>(null)
    val generatedPdf: StateFlow<File?> = _generatedPdf.asStateFlow()

    // The reading created by the last transmit, so it can be undone
    private var lastInitiatedReadingId: Long? = null
    private var lastInsertedType: UtilityType? = null

    // Call verification: set when a call is requested, cleared once the call log was read
    private var callInitiatedTimeMs: Long = 0L
    private var lastInitiatedType: UtilityType? = null
    private var verifying = false

    private val _lastCallEvaluation = MutableStateFlow<CallVerificationResult?>(null)
    val lastCallEvaluation: StateFlow<CallVerificationResult?> = _lastCallEvaluation.asStateFlow()

    fun clearLastCallEvaluation() {
        _lastCallEvaluation.value = null
    }

    // Database flows
    val allReadings: StateFlow<List<MeterReading>> = repository.allReadings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gasConfig: StateFlow<UtilityConfig?> = repository.getConfig(UtilityType.GAS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val electricityConfig: StateFlow<UtilityConfig?> = repository.getConfig(UtilityType.ELECTRICITY)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val latestGasReading: StateFlow<MeterReading?> = repository.getLatestReading(UtilityType.GAS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val latestElectricityReading: StateFlow<MeterReading?> = repository.getLatestReading(UtilityType.ELECTRICITY)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current active config
    val currentConfig: StateFlow<UtilityConfig?> = combine(
        _activeUtility, gasConfig, electricityConfig
    ) { type, gas, elec ->
        if (type == UtilityType.GAS) gas else elec
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current latest reading for the selected utility
    val currentLatestReading: StateFlow<MeterReading?> = combine(
        _activeUtility, latestGasReading, latestElectricityReading
    ) { type, gas, elec ->
        if (type == UtilityType.GAS) gas else elec
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    fun selectUtility(type: UtilityType) {
        _activeUtility.value = type
        _indexInput.value = ""
    }

    fun updateIndexInput(input: String) {
        // Meter indexes are sent to the IVR as whole numbers: digits only
        _indexInput.value = input.filter { it.isDigit() }.take(MAX_INDEX_DIGITS)
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    /**
     * Calculates the estimated consumption delta:
     * Consumption = Current Index - Previous Index
     */
    fun calculateConsumption(currentIndex: Double, previousReading: MeterReading?): Double {
        val prev = previousReading?.indexValue ?: return 0.0
        return (currentIndex - prev).coerceAtLeast(0.0)
    }

    /**
     * Calculates estimated cost in LEI
     */
    fun calculateEstimatedCost(consumption: Double, unitPrice: Double): Double {
        return consumption * unitPrice
    }

    /**
     * Generates full dial string
     */
    fun getFullDialString(type: UtilityType, config: UtilityConfig?, indexString: String): String {
        val template = config?.ivrTemplate ?: type.defaultIvrTemplate
        val indexForDial = if (indexString.isBlank()) "XXXX" else {
            // Meters transmit whole numbers; strip any separator just in case
            indexString.replace(".", "")
        }
        return IvrSequenceBuilder.buildDialString(template, indexForDial)
    }

    /**
     * Transmit and call: Saves reading to database and places call (direct or dialer)
     */
    fun transmitAndCall(context: Context, directCall: Boolean) {
        val indexVal = _indexInput.value.toDoubleOrNull()
        if (indexVal == null || indexVal <= 0) {
            _userMessage.value = "Te rugăm să introduci un index valid de pe contor!"
            return
        }

        val type = _activeUtility.value
        val config = if (type == UtilityType.GAS) gasConfig.value else electricityConfig.value
        val prevReading = if (type == UtilityType.GAS) latestGasReading.value else latestElectricityReading.value

        val prevVal = prevReading?.indexValue
        if (prevVal != null && indexVal < prevVal) {
            _userMessage.value = "Atenție: Indexul nou (${indexVal.toLong()}) este mai mic decât indexul anterior (${prevVal.toLong()})!"
            return
        }

        val consumption = if (prevVal != null) indexVal - prevVal else 0.0
        val price = config?.unitPrice ?: type.defaultPrice
        val estimatedCost = consumption * price
        val dialSequence = getFullDialString(type, config, _indexInput.value)

        viewModelScope.launch {
            // Marked as called only once the call really starts (or the call log confirms it)
            val reading = MeterReading(
                utilityType = type,
                indexValue = indexVal,
                previousIndexValue = prevVal,
                consumption = consumption,
                unitPrice = price,
                estimatedCost = estimatedCost,
                timestamp = System.currentTimeMillis(),
                callSequenceUsed = dialSequence,
                isCallExecuted = false,
                notes = ""
            )

            val insertedId = repository.insertReading(reading)
            lastInitiatedReadingId = insertedId
            lastInsertedType = type
            callInitiatedTimeMs = System.currentTimeMillis()
            lastInitiatedType = type
            _lastCallEvaluation.value = null

            if (directCall) {
                val callStarted = CallHelper.makeDirectCall(context, dialSequence)
                if (callStarted) {
                    repository.updateReading(reading.copy(id = insertedId, isCallExecuted = true))
                    _userMessage.value = "Apelul automat a fost inițiat! Robotul preia secvența."
                } else {
                    _userMessage.value = "Deschidere în tastatură (permisiune apel necesară pentru apel direct)."
                }
            } else {
                CallHelper.openInDialer(context, dialSequence)
                _userMessage.value = "Secvența a fost încărcată în apeluri."
            }

            // Clear inputs
            _indexInput.value = ""
        }
    }

    /**
     * Recomputes previous index, consumption and cost for every reading of a utility,
     * so that editing or deleting one reading never leaves its neighbours stale.
     */
    private suspend fun recalculateChain(type: UtilityType) {
        var previous: Double? = null
        for (item in repository.getReadingsByTypeAsc(type)) {
            val consumption = if (previous != null) (item.indexValue - previous).coerceAtLeast(0.0) else 0.0
            val fixed = item.copy(
                previousIndexValue = previous,
                consumption = consumption,
                estimatedCost = consumption * item.unitPrice
            )
            if (fixed != item) repository.updateReading(fixed)
            previous = item.indexValue
        }
    }

    fun updateReading(reading: MeterReading) {
        viewModelScope.launch {
            repository.updateReading(reading)
            recalculateChain(reading.utilityType)
            _userMessage.value = "Înregistrarea a fost actualizată!"
        }
    }

    fun undoLastReading() {
        val id = lastInitiatedReadingId ?: return
        val type = lastInsertedType
        viewModelScope.launch {
            repository.deleteById(id)
            if (type != null) recalculateChain(type)
            lastInitiatedReadingId = null
            lastInsertedType = null
            _lastCallEvaluation.value = null
            _userMessage.value = "Ultimul index a fost anulat și șters din istoric."
        }
    }

    private suspend fun markLastReadingAsCalled(type: UtilityType) {
        val id = lastInitiatedReadingId ?: return
        val latest = repository.getLatestReadingSync(type) ?: return
        if (latest.id == id && !latest.isCallExecuted) {
            repository.updateReading(latest.copy(isCallExecuted = true))
        }
    }

    /**
     * Verifies the call outcome by inspecting the Android call log duration.
     * The call log is written when the call ends, so until then there is nothing to read
     * and we simply keep waiting. Once a call that started after our request is found it is
     * evaluated exactly once. The log only tells us how long the call lasted, not whether the
     * robot accepted the index, so the messages are deliberately cautious.
     */
    fun verifyCallOutcome(context: Context) {
        val startedAt = callInitiatedTimeMs
        val type = lastInitiatedType
        if (startedAt == 0L || type == null || verifying) return

        if (!CallLogHelper.hasCallLogPermission(context)) {
            // Without the permission the outcome can never be read: stop waiting
            callInitiatedTimeMs = 0L
            lastInitiatedType = null
            return
        }

        val config = if (type == UtilityType.GAS) gasConfig.value else electricityConfig.value
        val expectedPhone = config?.phoneNumber ?: type.defaultPhone

        verifying = true
        viewModelScope.launch {
            try {
                delay(1200)
                val entry = CallLogHelper.checkLatestOutgoingCall(context, startedAt, expectedPhone)
                    ?.takeIf { it.date >= startedAt - 5_000L }

                if (entry == null) {
                    if (System.currentTimeMillis() - startedAt > PENDING_CALL_TIMEOUT_MS) {
                        callInitiatedTimeMs = 0L
                        lastInitiatedType = null
                    }
                    return@launch
                }

                callInitiatedTimeMs = 0L
                lastInitiatedType = null

                val result = CallLogHelper.evaluateCall(entry, type)
                _lastCallEvaluation.value = result

                when (result.status) {
                    CallStatusEvaluation.CANCELLED_OR_MISSED ->
                        _userMessage.value = "Apel anulat imediat (0s). Indexul nu a fost transmis!"
                    CallStatusEvaluation.CONFIRMED_SUCCESS -> {
                        markLastReadingAsCalled(type)
                        _userMessage.value = "Apel de ${result.durationSeconds}s. Probabil transmis; robotul nu poate confirma automat."
                    }
                    else -> Unit
                }
            } finally {
                verifying = false
            }
        }
    }

    /**
     * Save reading to history without calling
     */
    fun saveReadingOnly() {
        val indexVal = _indexInput.value.toDoubleOrNull()
        if (indexVal == null || indexVal <= 0) {
            _userMessage.value = "Te rugăm să introduci un index valid!"
            return
        }

        val type = _activeUtility.value
        val config = if (type == UtilityType.GAS) gasConfig.value else electricityConfig.value
        val prevReading = if (type == UtilityType.GAS) latestGasReading.value else latestElectricityReading.value

        val prevVal = prevReading?.indexValue
        if (prevVal != null && indexVal < prevVal) {
            _userMessage.value = "Indexul nou (${indexVal.toLong()}) este mai mic decât cel anterior (${prevVal.toLong()}). Verifică contorul."
            return
        }

        val consumption = if (prevVal != null) indexVal - prevVal else 0.0
        val price = config?.unitPrice ?: type.defaultPrice
        val estimatedCost = consumption * price
        val dialSequence = getFullDialString(type, config, _indexInput.value)

        viewModelScope.launch {
            val reading = MeterReading(
                utilityType = type,
                indexValue = indexVal,
                previousIndexValue = prevVal,
                consumption = consumption,
                unitPrice = price,
                estimatedCost = estimatedCost,
                timestamp = System.currentTimeMillis(),
                callSequenceUsed = dialSequence,
                isCallExecuted = false,
                notes = ""
            )

            repository.insertReading(reading)
            _userMessage.value = "Indexul a fost salvat cu succes în istoric!"
            _indexInput.value = ""
        }
    }

    fun deleteReading(reading: MeterReading) {
        viewModelScope.launch {
            repository.deleteReading(reading)
            recalculateChain(reading.utilityType)
            _userMessage.value = "Înregistrarea a fost ștearsă."
        }
    }

    fun updateConfig(config: UtilityConfig) {
        viewModelScope.launch {
            val previous = if (config.utilityType == UtilityType.GAS) gasConfig.value else electricityConfig.value

            // The dial template embeds the client code and the phone number, so keep it in
            // sync when the user edits them in Settings.
            var template = config.ivrTemplate
            if (previous != null) {
                if (previous.clientCode.isNotBlank() && previous.clientCode != config.clientCode) {
                    template = template.replace(previous.clientCode, config.clientCode)
                }
                if (previous.phoneNumber.isNotBlank() && previous.phoneNumber != config.phoneNumber) {
                    template = template.replace(previous.phoneNumber, config.phoneNumber)
                }
            }
            val updated = config.copy(ivrTemplate = template)

            repository.saveConfig(updated)

            // Apply the chosen reminder day / hour right away
            val context = getApplication<Application>()
            if (updated.isReminderEnabled) {
                ReminderScheduler.scheduleNext(
                    context,
                    updated.utilityType,
                    updated.reminderDayOfMonth,
                    updated.reminderHour,
                    updated.reminderMinute
                )
            } else {
                ReminderScheduler.cancel(context, updated.utilityType)
            }

            _userMessage.value = "Setările pentru ${updated.utilityType.title} au fost actualizate!"
        }
    }

    fun triggerTestNotification(context: Context, type: UtilityType) {
        ReminderScheduler.sendImmediateTestNotification(context, type)
        _userMessage.value = "Notificarea de test a fost trimisă pentru ${type.title}!"
    }

    /**
     * Triggers a test call up to the step of entering the meter index.
     * The DTMF sequence stops right before XXXX, allowing the user to verify
     * that the robot receives the client code and asks for the index.
     */
    fun triggerTestCall(context: Context, type: UtilityType, directCall: Boolean = true) {
        val config = if (type == UtilityType.GAS) gasConfig.value else electricityConfig.value
        val template = config?.ivrTemplate ?: type.defaultIvrTemplate
        val testSequence = IvrSequenceBuilder.buildTestSequenceUntilIndex(template)

        if (directCall) {
            val callStarted = CallHelper.makeDirectCall(context, testSequence)
            if (callStarted) {
                _userMessage.value = "Apel test pornit (${type.title})! Secvența se oprește la cererea indexului: $testSequence"
            } else {
                _userMessage.value = "Secvența de test (${type.title}) a fost deschisă în apeluri."
            }
        } else {
            CallHelper.openInDialer(context, testSequence)
            _userMessage.value = "Secvența de test (${type.title}) a fost încărcată în apeluri."
        }
    }

    /**
     * Generates the PDF for the readings made since [sinceMs] (all of them when null).
     */
    fun exportPdfReport(context: Context, periodLabel: String, sinceMs: Long?) {
        viewModelScope.launch {
            val readings = allReadings.value.let { all ->
                if (sinceMs == null) all else all.filter { it.timestamp >= sinceMs }
            }

            val pdfFile = withContext(Dispatchers.IO) {
                PdfReportGenerator.generateMonthlyReport(
                    context = context,
                    periodName = periodLabel,
                    readings = readings,
                    gasConfig = gasConfig.value,
                    electricityConfig = electricityConfig.value
                )
            }

            if (pdfFile != null && pdfFile.exists()) {
                _generatedPdf.value = pdfFile
                _userMessage.value = "Raportul PDF a fost generat cu succes!"
                PdfReportGenerator.sharePdf(context, pdfFile)
            } else {
                _userMessage.value = "Eroare la generarea fișierului PDF."
            }
        }
    }
}
