package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Size
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.scan.Detection
import com.example.scan.IndexCandidatePicker
import com.example.scan.IndexSuggestion
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

// How many of the latest frames are kept, and in how many of them a number must appear
// before it is offered to the user (a single frame is often a misread)
private const val RECENT_FRAMES = 8
private const val MIN_FRAME_VOTES = 3

// Only the middle band of the picture is read: that is where the viewfinder is aimed. Serial
// numbers, years and plate texts around it are ignored (fractions of the frame).
private const val ROI_TOP = 0.28f
private const val ROI_BOTTOM = 0.72f
private const val ROI_LEFT = 0.03f
private const val ROI_RIGHT = 0.97f

// Zoom buttons offered when the camera supports them (a Galaxy S25 Ultra has telephoto lenses)
private val ZOOM_STEPS = listOf(1f, 2f, 3f, 5f)

@Composable
fun MeterScannerDialog(
    onDismiss: () -> Unit,
    onIndexDetected: (String) -> Unit,
    // The last saved index of this meter: lets the scanner reject numbers that cannot be the
    // next reading, and drop the red decimals of drum meters
    previousIndex: Long? = null,
    // The biggest plausible increase since the previous index
    maxJump: Long = 1000L
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            if (hasCameraPermission) {
                CameraScannerContent(
                    previousIndex = previousIndex,
                    maxJump = maxJump,
                    onDismiss = onDismiss,
                    onIndexSelected = onIndexDetected
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Permisiunea pentru cameră este necesară pentru a scana contorul.",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Permite Accesul la Cameră")
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError", "ClickableViewAccessibility")
@Composable
private fun CameraScannerContent(
    previousIndex: Long?,
    maxJump: Long,
    onDismiss: () -> Unit,
    onIndexSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var suggestions by remember { mutableStateOf<List<IndexSuggestion>>(emptyList()) }
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var maxZoom by remember { mutableFloatStateOf(1f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    // Set when the camera could not be opened (used by another app, blocked by policy, ...)
    var cameraError by remember { mutableStateOf(false) }
    var showTips by remember { mutableStateOf(false) }

    // The number in the editable field. It follows the best suggestion until the user types.
    var fieldText by remember { mutableStateOf("") }
    var userEdited by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // What the latest frames contained; touched from the analyzer thread only
    val recentFrames = remember { mutableListOf<List<Detection>>() }
    // Kept so the camera can be released when the dialog is closed
    val cameraProviderHolder = remember { arrayOfNulls<ProcessCameraProvider>(1) }

    val best = suggestions.firstOrNull()

    // Until the user types, the field shows the best suggestion
    LaunchedEffect(best?.value) {
        if (!userEdited && best != null) fieldText = best.display
    }

    // Hints appear when nothing readable was found for a while
    LaunchedEffect(Unit) {
        delay(8000)
        showTips = true
    }

    DisposableEffect(Unit) {
        onDispose {
            // The use cases are bound to the activity lifecycle: without this the camera (and
            // the torch) keep running after the dialog is closed, and the analyzer keeps
            // calling a recognizer that has been closed.
            cameraProviderHolder[0]?.unbindAll()
            cameraExecutor.shutdown()
            textRecognizer.close()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                // Tap to focus: close-up digits are often out of focus until the camera is told where to look
                previewView.setOnTouchListener { view, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        val point = previewView.meteringPointFactory.createPoint(event.x, event.y)
                        cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
                        view.performClick()
                    }
                    true
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    // get() throws when the camera service is unavailable; this runs on the main
                    // thread, so an uncaught exception here would close the whole app
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProviderHolder[0] = cameraProvider
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        // The default analysis size is about 640x480: too little for small digits
                        val analysisResolution = ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(1920, 1080),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                )
                            )
                            .build()

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setResolutionSelector(analysisResolution)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val mainExecutor = ContextCompat.getMainExecutor(ctx)
                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            processImageProxy(imageProxy, textRecognizer) { detections ->
                                val ranked = synchronized(recentFrames) {
                                    recentFrames.add(detections)
                                    if (recentFrames.size > RECENT_FRAMES) recentFrames.removeAt(0)
                                    IndexCandidatePicker.suggest(
                                        frames = recentFrames.toList(),
                                        minVotes = MIN_FRAME_VOTES,
                                        previousIndex = previousIndex,
                                        maxJump = maxJump
                                    )
                                }
                                // Compose state is updated on the main thread; an empty result keeps
                                // the last good one so the screen does not flicker
                                if (ranked.isNotEmpty()) {
                                    mainExecutor.execute { suggestions = ranked }
                                }
                            }
                        }

                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                        cameraControl = camera.cameraControl
                        maxZoom = camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
                    } catch (e: Exception) {
                        e.printStackTrace()
                        cameraError = true
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Overlay with scanning frame
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Închide", tint = Color.White)
                }

                Text(
                    text = "Scanare Contor",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                IconButton(
                    onClick = {
                        isTorchOn = !isTorchOn
                        cameraControl?.enableTorch(isTorchOn)
                    },
                    modifier = Modifier.background(
                        if (isTorchOn) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashlightOn,
                        contentDescription = "Lanternă",
                        tint = Color.White
                    )
                }
            }

            // Central Guide Viewfinder + zoom
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(110.dp)
                        .border(BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (best != null) {
                        Text(
                            text = best.display,
                            color = if (best.plausible) Color.Green else Color(0xFFFFB74D),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Text(
                            text = "Încadrează cifrele contorului aici",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val steps = ZOOM_STEPS.filter { it <= maxZoom + 0.01f }
                if (steps.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (step in steps) {
                            val selected = zoom == step
                            Surface(
                                modifier = Modifier.clickable {
                                    zoom = step
                                    cameraControl?.setZoomRatio(step)
                                },
                                shape = CircleShape,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.55f)
                            ) {
                                Text(
                                    text = if (step == step.toInt().toFloat()) "${step.toInt()}×" else "$step×",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = if (previousIndex != null) {
                        "Ultimul index: $previousIndex. Atinge ecranul ca să focalizezi."
                    } else {
                        "Atinge ecranul ca să focalizezi pe cifre."
                    },
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
            }

            // Bottom Results Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.78f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (cameraError) {
                        Text(
                            text = "Camera nu poate fi deschisă acum (poate o folosește altă aplicație). Închide scanerul și scrie indexul manual.",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                            Text("Închide")
                        }
                    } else if (best != null) {
                        Text(
                            text = "Index detectat — poți corecta cifrele",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        OutlinedTextField(
                            value = fieldText,
                            onValueChange = { input ->
                                fieldText = input.filter { it.isDigit() }.take(9)
                                userEdited = true
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                cursorColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // A number that does not follow the previous index is flagged, not hidden:
                        // the user may know better than the app
                        val typed = fieldText.toLongOrNull()
                        val fitsPrevious = previousIndex == null || typed == null || typed >= previousIndex
                        if (!fitsPrevious) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB74D),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Numărul e mai mic decât ultimul index ($previousIndex). Verifică cifrele.",
                                    color = Color(0xFFFFB74D),
                                    fontSize = 12.sp
                                )
                            }
                        } else if (!best.plausible && !userEdited) {
                            Text(
                                text = "Nu pare să urmeze după ultimul index. Mai citește o dată sau corectează.",
                                color = Color(0xFFFFB74D),
                                fontSize = 12.sp
                            )
                        }

                        best.note?.let {
                            if (!userEdited) {
                                Text(text = it, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            }
                        }

                        // The other candidates the camera considered
                        if (suggestions.size > 1) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (suggestion in suggestions.take(4)) {
                                    FilterChip(
                                        selected = fieldText == suggestion.display,
                                        onClick = {
                                            fieldText = suggestion.display
                                            userEdited = true
                                        },
                                        label = { Text(suggestion.display, fontFamily = FontFamily.Monospace) }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Renunță", color = Color.White)
                            }

                            Button(
                                onClick = { if (fieldText.isNotEmpty()) onIndexSelected(fieldText) },
                                enabled = fieldText.isNotEmpty(),
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Folosește indexul")
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Îndreaptă camera spre cadranul contorului...",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                        if (showTips) {
                            Text(
                                text = "Nu am citit nimic încă. Încearcă: apropie-te de cifre, ine telefonul drept, " +
                                    "pornește lanterna dacă e întuneric, evită reflexiile, folosește zoom 2× sau 3×, " +
                                    "atinge cifrele ca să focalizezi. Dacă nu merge, scrie indexul manual.",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Reads one camera frame and reports the numbers found in the aimed middle band of the picture. */
@SuppressLint("UnsafeOptInUsageError")
private fun processImageProxy(
    imageProxy: ImageProxy,
    recognizer: TextRecognizer,
    onResult: (List<Detection>) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }

    val rotation = imageProxy.imageInfo.rotationDegrees
    // Bounding boxes come back in the rotated picture, so width and height swap for 90/270
    val frameWidth = if (rotation % 180 == 0) imageProxy.width else imageProxy.height
    val frameHeight = if (rotation % 180 == 0) imageProxy.height else imageProxy.width

    val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
    recognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            val detections = ArrayList<Detection>()
            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    val box = line.boundingBox ?: continue
                    val centerX = box.centerX().toFloat() / frameWidth
                    val centerY = box.centerY().toFloat() / frameHeight
                    if (centerX !in ROI_LEFT..ROI_RIGHT || centerY !in ROI_TOP..ROI_BOTTOM) continue

                    // The index is printed in the biggest digits: their height ranks the candidates
                    val height = box.height().toFloat() / frameHeight
                    for (digits in digitStrings(line.text)) {
                        detections.add(Detection(digits, height))
                    }
                }
            }
            onResult(detections)
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

/**
 * The numbers in one line of recognised text. A dot or comma splits a token, so "2899.123" gives
 * the index 2899 and the red decimals 123 separately, and digits read one by one ("2 8 9 9")
 * are also offered glued together.
 */
private fun digitStrings(text: String): List<String> {
    val tokens = text.trim().split(Regex("[\\s.,:;]+")).filter { it.isNotEmpty() }
    val cleaned = tokens.map { cleanToken(it) }

    val result = ArrayList<String>()
    for (digits in cleaned) {
        if (digits != null && digits.length >= IndexCandidatePicker.MIN_DIGITS) result.add(digits)
    }

    if (tokens.size in IndexCandidatePicker.MIN_DIGITS..8 && cleaned.all { it != null && it.length == 1 }) {
        result.add(cleaned.joinToString("") { it.orEmpty() })
    }
    return result
}

/**
 * Turns a recognised token into digits, fixing the usual misreads (O for 0, l for 1, S for 5, ...).
 * A token that is mostly letters is not a number and gives null.
 */
private fun cleanToken(token: String): String? {
    val digitCount = token.count { it.isDigit() }
    // One wrong character is tolerated, more than that is text
    if (digitCount == 0 || digitCount < token.length - 1) return null

    val mapped = token.map { c ->
        when (c) {
            'O', 'o', 'D', 'Q' -> '0'
            'I', 'l', 'i', '|' -> '1'
            'Z', 'z' -> '2'
            'S', 's' -> '5'
            'B' -> '8'
            else -> c
        }
    }.joinToString("")

    return if (mapped.all { it.isDigit() } && mapped.length <= 8) mapped else null
}
