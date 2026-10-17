package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
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
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
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
import androidx.compose.ui.geometry.Rect as UiRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text as VisionText
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.min

// How many of the latest frames are kept, and in how many of them a number must appear before it
// is offered. With a previous index to compare against, a wrong number is rare, so 2 is enough.
private const val RECENT_FRAMES = 8
private const val MIN_VOTES_WITH_PREVIOUS = 2
private const val MIN_VOTES_WITHOUT_PREVIOUS = 3

// Used until the layout of the screen is known (fractions of the visible camera picture)
private val DEFAULT_ROI = floatArrayOf(0.05f, 0.30f, 0.95f, 0.55f)

// Zoom buttons offered when the camera supports them (a Galaxy S25 Ultra has telephoto lenses)
private val ZOOM_STEPS = listOf(1f, 2f, 3f, 5f)

@Composable
fun MeterScannerDialog(
    onDismiss: () -> Unit,
    onIndexDetected: (String) -> Unit,
    // The last saved index of this meter: only numbers that can follow it are offered
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
                            Text("Permite accesul")
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

    // The number in the editable field. It follows the best suggestion until the user touches it.
    var fieldText by remember { mutableStateOf("") }
    var userEdited by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // Where the preview and the framing box are on screen: the analysed image is cropped to
    // exactly the box, so what the user aims at is what is read
    val previewBounds = remember { AtomicReference<UiRect?>(null) }
    val boxBounds = remember { AtomicReference<UiRect?>(null) }

    // What the latest frames contained; touched from the analyzer thread only
    val recentFrames = remember { mutableListOf<List<Detection>>() }
    // Kept so the camera can be released when the dialog is closed
    val cameraProviderHolder = remember { arrayOfNulls<ProcessCameraProvider>(1) }

    val best = suggestions.firstOrNull()

    // Until the user touches the field, it shows the best suggestion
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
        // Camera preview
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { previewBounds.set(it.boundsInWindow()) },
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
                        val minVotes = if (previousIndex != null) MIN_VOTES_WITH_PREVIOUS else MIN_VOTES_WITHOUT_PREVIOUS
                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            try {
                                val detections = readFrame(
                                    proxy = imageProxy,
                                    recognizer = textRecognizer,
                                    roi = regionOfInterest(previewBounds.get(), boxBounds.get())
                                )
                                val ranked = synchronized(recentFrames) {
                                    recentFrames.add(detections)
                                    if (recentFrames.size > RECENT_FRAMES) recentFrames.removeAt(0)
                                    IndexCandidatePicker.suggest(
                                        frames = recentFrames.toList(),
                                        minVotes = minVotes,
                                        previousIndex = previousIndex,
                                        maxJump = maxJump
                                    )
                                }
                                // Compose state is updated on the main thread; once the user has
                                // touched the result it is frozen so it does not change under them
                                mainExecutor.execute { if (!userEdited) suggestions = ranked }
                            } catch (e: Exception) {
                                // A bad frame (or a recognizer closed meanwhile) is simply skipped
                            } finally {
                                imageProxy.close()
                            }
                        }

                        // The preview and the analysis must show the same part of the picture,
                        // otherwise the box on screen does not match what is read
                        previewView.post {
                            try {
                                val group = UseCaseGroup.Builder()
                                    .addUseCase(preview)
                                    .addUseCase(imageAnalysis)
                                    .apply { previewView.viewPort?.let { setViewPort(it) } }
                                    .build()
                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    group
                                )
                                cameraControl = camera.cameraControl
                                maxZoom = camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
                            } catch (e: Exception) {
                                e.printStackTrace()
                                cameraError = true
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        cameraError = true
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Top bar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Închide", tint = Color.White)
            }

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

        // Framing box and zoom: the position is fixed, whatever the result card below does
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-90).dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(96.dp)
                    .onGloballyPositioned { boxBounds.set(it.boundsInWindow()) }
                    .border(BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (best != null) {
                    Text(
                        text = best.display,
                        color = Color(0xFF69F0AE),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                                text = "${step.toInt()}×",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        // Result card at the bottom
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color.Black.copy(alpha = 0.8f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (cameraError) {
                    Text(
                        text = "Camera nu poate fi deschisă acum. Închide scanerul și scrie indexul.",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                        Text("Închide")
                    }
                } else if (best != null) {
                    OutlinedTextField(
                        value = fieldText,
                        onValueChange = { input ->
                            fieldText = input.filter { it.isDigit() }.take(9)
                            userEdited = true
                        },
                        singleLine = true,
                        label = { Text("Index detectat") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedLabelColor = Color.White.copy(alpha = 0.8f),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                            cursorColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // The user may type anything; a number below the last index is flagged
                    val typed = fieldText.toLongOrNull()
                    if (previousIndex != null && typed != null && typed < previousIndex) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mai mic decât ultimul index ($previousIndex)",
                                color = Color(0xFFFFB74D),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Other numbers the camera considered, on one line
                    if (suggestions.size > 1) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (suggestion in suggestions) {
                                FilterChip(
                                    selected = fieldText == suggestion.display,
                                    onClick = {
                                        fieldText = suggestion.display
                                        userEdited = true
                                    },
                                    label = {
                                        Text(
                                            text = suggestion.display,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
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
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Renunță", color = Color.White, maxLines = 1)
                        }

                        Button(
                            onClick = { if (fieldText.isNotEmpty()) onIndexSelected(fieldText) },
                            enabled = fieldText.isNotEmpty(),
                            modifier = Modifier.weight(1.4f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Folosește", maxLines = 1, fontWeight = FontWeight.Bold)
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
                            text = if (previousIndex != null) {
                                "Pune cifrele negre în chenar · ultimul: $previousIndex"
                            } else {
                                "Pune cifrele negre în chenar"
                            },
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                    if (showTips) {
                        Text(
                            text = "Apropie-te, atinge cifrele ca să focalizeze, folosește zoom 2×–3× sau lanterna. " +
                                "Dacă nu merge, scrie indexul manual.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/** The framing box as fractions (left, top, right, bottom) of the visible camera picture. */
private fun regionOfInterest(preview: UiRect?, box: UiRect?): FloatArray {
    if (preview == null || box == null || preview.width <= 0f || preview.height <= 0f) return DEFAULT_ROI
    val left = ((box.left - preview.left) / preview.width).coerceIn(0f, 1f)
    val top = ((box.top - preview.top) / preview.height).coerceIn(0f, 1f)
    val right = ((box.right - preview.left) / preview.width).coerceIn(0f, 1f)
    val bottom = ((box.bottom - preview.top) / preview.height).coerceIn(0f, 1f)
    return if (right - left < 0.05f || bottom - top < 0.02f) DEFAULT_ROI else floatArrayOf(left, top, right, bottom)
}

/**
 * Reads one camera frame: crops it to the framing box, cuts off the red decimal drums, and reads
 * two enhanced versions of the crop (contrast, and inverted for light digits on dark drums).
 * Runs on the analyzer thread, so it may wait for the recognizer.
 */
@SuppressLint("UnsafeOptInUsageError")
private fun readFrame(proxy: ImageProxy, recognizer: TextRecognizer, roi: FloatArray): List<Detection> {
    var visible = proxy.toBitmap()

    // With a view port the frame carries the crop that matches the preview; apply it unless the
    // conversion has already done so
    val crop = proxy.cropRect
    val coversAll = crop.width() >= proxy.width && crop.height() >= proxy.height
    val alreadyCropped = visible.width == crop.width() && visible.height == crop.height()
    if (!coversAll && !alreadyCropped) {
        val l = crop.left.coerceIn(0, visible.width - 2)
        val t = crop.top.coerceIn(0, visible.height - 2)
        val w = min(crop.width(), visible.width - l).coerceAtLeast(2)
        val h = min(crop.height(), visible.height - t).coerceAtLeast(2)
        visible = Bitmap.createBitmap(visible, l, t, w, h)
    }

    val rotation = proxy.imageInfo.rotationDegrees
    val upright = if (rotation == 0) {
        visible
    } else {
        Bitmap.createBitmap(
            visible, 0, 0, visible.width, visible.height,
            Matrix().apply { postRotate(rotation.toFloat()) }, true
        )
    }

    val left = (roi[0] * upright.width).toInt().coerceIn(0, upright.width - 2)
    val top = (roi[1] * upright.height).toInt().coerceIn(0, upright.height - 2)
    val width = ((roi[2] - roi[0]) * upright.width).toInt().coerceIn(2, upright.width - left)
    val height = ((roi[3] - roi[1]) * upright.height).toInt().coerceIn(2, upright.height - top)
    var region = Bitmap.createBitmap(upright, left, top, width, height)

    region = cutRedDecimals(region)

    // Small crops are enlarged: the recognizer reads small characters badly
    if (region.width in 2..899) {
        val scaledHeight = max(2, (region.height * 900f / region.width).toInt())
        region = Bitmap.createScaledBitmap(region, 900, scaledHeight, true)
    }

    val detections = ArrayList<Detection>()
    for (negative in listOf(false, true)) {
        val enhanced = enhance(region, negative)
        val text = Tasks.await(recognizer.process(InputImage.fromBitmap(enhanced, 0)))
        detections.addAll(detectionsFrom(text, enhanced.height))
    }
    return detections
}

/**
 * Drum meters show the red decimals after the index. Their drums are red, so the crop is cut at
 * the first wide band of red columns and only the black drums (the index) are read.
 */
private fun cutRedDecimals(src: Bitmap): Bitmap {
    val smallWidth = 160
    val smallHeight = max(1, (smallWidth.toFloat() * src.height / src.width).toInt())
    val small = Bitmap.createScaledBitmap(src, smallWidth, smallHeight, true)
    val pixels = IntArray(smallWidth * smallHeight)
    small.getPixels(pixels, 0, smallWidth, 0, 0, smallWidth, smallHeight)

    val redColumn = BooleanArray(smallWidth)
    for (x in 0 until smallWidth) {
        var red = 0
        for (y in 0 until smallHeight) {
            val c = pixels[y * smallWidth + x]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            if (r > 110 && r > g * 1.5f && r > b * 1.5f) red++
        }
        redColumn[x] = red > smallHeight * 0.2f
    }

    // The red drums are on the right: look for a run of red columns, at least 6% of the width
    val minRun = (smallWidth * 0.06f).toInt()
    var run = 0
    var runStart = -1
    for (x in (smallWidth * 0.3f).toInt() until smallWidth) {
        if (redColumn[x]) {
            if (run == 0) runStart = x
            run++
            if (run >= minRun) {
                val cutX = (((runStart - 1).coerceAtLeast(0)).toFloat() / smallWidth * src.width).toInt()
                // Keep at least 15% of the crop: a tiny remainder means the red was a false alarm
                if (cutX < src.width * 0.15f) return src
                return Bitmap.createBitmap(src, 0, 0, cutX, src.height)
            }
        } else {
            run = 0
        }
    }
    return src
}

/** Grey, stronger contrast, optionally inverted (the recognizer prefers dark text on light). */
private fun enhance(src: Bitmap, negative: Boolean): Bitmap {
    val contrast = 1.6f
    val sign = if (negative) -contrast else contrast
    val translate = if (negative) 128f * contrast + 127f else 128f * (1f - contrast)
    val matrix = ColorMatrix().apply { setSaturation(0f) }
    matrix.postConcat(
        ColorMatrix(
            floatArrayOf(
                sign, 0f, 0f, 0f, translate,
                0f, sign, 0f, 0f, translate,
                0f, 0f, sign, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
    Canvas(out).drawBitmap(src, 0f, 0f, paint)
    return out
}

private data class Piece(val digits: String, val box: Rect)

/**
 * The numbers found in a crop. A drum meter is often read digit by digit, so digits that are on
 * the same row and close to each other are glued together, left to right.
 */
private fun detectionsFrom(text: VisionText, cropHeight: Int): List<Detection> {
    val pieces = ArrayList<Piece>()
    for (block in text.textBlocks) {
        for (line in block.lines) {
            for (element in line.elements) {
                val box = element.boundingBox ?: continue
                val digits = cleanToken(element.text) ?: continue
                pieces.add(Piece(digits, box))
            }
        }
    }
    if (pieces.isEmpty()) return emptyList()

    // Group into rows by vertical position
    val rows = ArrayList<MutableList<Piece>>()
    for (piece in pieces.sortedBy { it.box.centerY() }) {
        val row = rows.lastOrNull()
        if (row != null && kotlin.math.abs(piece.box.centerY() - row.map { it.box.centerY() }.average()) < piece.box.height() * 0.6) {
            row.add(piece)
        } else {
            rows.add(mutableListOf(piece))
        }
    }

    val result = ArrayList<Detection>()
    for (row in rows) {
        val ordered = row.sortedBy { it.box.left }
        val digitHeight = ordered.map { it.box.height() }.average().toFloat()
        val ratio = (digitHeight / cropHeight).coerceAtMost(1f)

        // Split the row into runs where the gap is small compared with the digit height
        val runs = ArrayList<MutableList<Piece>>()
        for (piece in ordered) {
            val run = runs.lastOrNull()
            if (run != null && piece.box.left - run.last().box.right <= digitHeight * 1.5f) {
                run.add(piece)
            } else {
                runs.add(mutableListOf(piece))
            }
        }

        for (run in runs) {
            val glued = run.joinToString("") { it.digits }
            if (glued.length in IndexCandidatePicker.MIN_DIGITS..8) result.add(Detection(glued, ratio))
            if (run.size > 1) {
                for (piece in run) {
                    if (piece.digits.length in IndexCandidatePicker.MIN_DIGITS..8) {
                        result.add(Detection(piece.digits, ratio))
                    }
                }
            }
        }
    }
    return result
}

/**
 * Turns a recognised word into digits, fixing the usual misreads (O for 0, l for 1, S for 5, ...).
 * A word that is mostly letters or symbols is not a number and gives null.
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
