package com.example.ui

import android.app.Application
import android.content.Context
import android.widget.Toast
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
import com.example.telecom.IvrSequenceBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MeterRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MeterRepository(database.meterDao(), database.configDao())
    }

    // Active bottom navigation tab (0: Transmit, 1: History & Charts, 2: PDF, 3: Settings)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Currently active utility in Transmit screen
    private val _activeUtility = MutableStateFlow(UtilityType.GAS)
    val activeUtility: StateFlow<UtilityType> = _activeUtility.asStateFlow()

    // Meter Index Input
    private val _indexInput = MutableStateFlow("")
    val indexInput: StateFlow<String> = _indexInput.asStateFlow()

    // Notes input for current transmission
    private val _notesInput = MutableStateFlow("")
    val notesInput: StateFlow<String> = _notesInput.asStateFlow()

    // User message / snackbar
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Generated PDF reports list
    private val _generatedPdf = MutableStateFlow<File?>(null)
    val generatedPdf: StateFlow<File?> = _generatedPdf.asStateFlow()

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
        _notesInput.value = ""
    }

    fun updateIndexInput(input: String) {
        // Allow digits and at most one decimal point
        val filtered = input.filter { it.isDigit() || it == '.' }
        _indexInput.value = filtered
    }

    fun updateNotesInput(notes: String) {
        _notesInput.value = notes
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
            // Usually meters transmit integer digits, or if decimal strip dot
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
            _userMessage.value = "Atenție: Indexul nou (${indexVal.toInt()}) este mai mic decât indexul anterior (${prevVal.toInt()})!"
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
                isCallExecuted = true,
                notes = _notesInput.value.ifBlank { "Transmitere automată prin IVR" }
            )

            repository.insertReading(reading)

            if (directCall) {
                val callStarted = CallHelper.makeDirectCall(context, dialSequence)
                if (callStarted) {
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
            _notesInput.value = ""
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
        val consumption = if (prevVal != null) (indexVal - prevVal).coerceAtLeast(0.0) else 0.0
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
                notes = _notesInput.value.ifBlank { "Salvare manuală" }
            )

            repository.insertReading(reading)
            _userMessage.value = "Indexul a fost salvat cu succes în istoric!"
            _indexInput.value = ""
            _notesInput.value = ""
        }
    }

    fun deleteReading(reading: MeterReading) {
        viewModelScope.launch {
            repository.deleteReading(reading)
            _userMessage.value = "Înregistrarea a fost ștearsă."
        }
    }

    fun updateConfig(config: UtilityConfig) {
        viewModelScope.launch {
            repository.saveConfig(config)
            _userMessage.value = "Setările pentru ${config.utilityType.title} au fost actualizate!"
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

    fun exportPdfReport(context: Context, periodLabel: String) {
        viewModelScope.launch {
            val readings = allReadings.value
            val pdfFile = PdfReportGenerator.generateMonthlyReport(
                context = context,
                periodName = periodLabel,
                readings = readings,
                gasConfig = gasConfig.value,
                electricityConfig = electricityConfig.value
            )

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
