package com.ht.intelza.scan

import android.content.Context
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.SystemClock
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.LifecycleOwner
import com.ht.intelza.domain.AnswerOption
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.Closeable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** A card seen in the latest frame, positioned in preview (view) coordinates. */
data class ScannedCard(
    val cardNumber: Int,
    /** Answer read from this frame, or null if the card is held diagonally. */
    val reading: AnswerOption?,
    /** Answer confirmed over several frames, if any. */
    val confirmed: AnswerOption?,
    /** Outline corners in view pixels (bottom-left, bottom-right, top-right, top-left of the card). */
    val outline: List<Vec2>,
    val center: Vec2,
)

data class ScanFrame(
    val cards: List<ScannedCard>,
    val processingMillis: Long,
    val framesPerSecond: Float,
    val imageSize: Size,
)

/**
 * Runs the back camera, finds answer cards in every frame and confirms each student's
 * answer over several frames.
 *
 * Bind it to a lifecycle with [bind], show [controller] in a `PreviewView`, observe
 * [frames] for the live overlay and collect [confirmations] for answers to record.
 */
class TagScanner(private val context: Context) : Closeable {

    val controller: LifecycleCameraController = LifecycleCameraController(context).apply {
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        previewResolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .build()
        // Higher resolution lets the detector read cards further away.
        imageAnalysisResolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(1920, 1080),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                ),
            )
            .build()
        imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        isPinchToZoomEnabled = true
        isTapToFocusEnabled = true
    }

    private val gravity = GravityTracker(context)
    private val sensorRotationDegrees = backCameraSensorRotation(context)
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val stabilizer = AnswerStabilizer()

    // Only touched on the analysis thread.
    private var detector: AprilTagDetector? = null
    private var lastFrameAt = 0L
    private var fps = 0f

    @Volatile
    private var sensorToView: Matrix? = null

    /** Cards outside this set (e.g. from another class) are ignored. Null accepts all. */
    @Volatile
    var acceptedCards: Set<Int>? = null

    private val _frames = MutableStateFlow<ScanFrame?>(null)
    val frames: StateFlow<ScanFrame?> = _frames.asStateFlow()

    private val _confirmations = MutableSharedFlow<Map<Int, AnswerOption>>(extraBufferCapacity = 64)

    /** Emits card number to answer for every answer that became confirmed or changed. */
    val confirmations: SharedFlow<Map<Int, AnswerOption>> = _confirmations.asSharedFlow()

    private val analyzer = object : ImageAnalysis.Analyzer {
        override fun getTargetCoordinateSystem(): Int = ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED

        override fun updateTransform(matrix: Matrix?) {
            sensorToView = matrix?.let(::Matrix)
        }

        override fun analyze(image: ImageProxy) {
            try {
                analyzeFrame(image)
            } catch (e: RuntimeException) {
                Log.e(TAG, "Frame analysis failed", e)
            } finally {
                image.close()
            }
        }
    }

    fun bind(lifecycleOwner: LifecycleOwner) {
        gravity.start()
        controller.setImageAnalysisAnalyzer(analysisExecutor, analyzer)
        controller.bindToLifecycle(lifecycleOwner)
    }

    fun unbind() {
        controller.unbind()
        controller.clearImageAnalysisAnalyzer()
        gravity.stop()
    }

    /** Starts a fresh scan, treating [answers] as already recorded. */
    fun reset(answers: Map<Int, AnswerOption> = emptyMap()) {
        analysisExecutor.execute {
            stabilizer.seed(answers)
            _frames.value = null
        }
    }

    fun setZoomRatio(ratio: Float) {
        controller.setZoomRatio(ratio)
    }

    override fun close() {
        unbind()
        analysisExecutor.execute {
            detector?.close()
            detector = null
        }
        analysisExecutor.shutdown()
    }

    private fun analyzeFrame(image: ImageProxy) {
        val started = SystemClock.elapsedRealtime()
        val plane = image.planes[0]
        val activeDetector = detector ?: AprilTagDetector().also { detector = it }
        val detections = activeDetector.detect(plane.buffer, image.width, image.height, plane.rowStride)

        val accepted = acceptedCards
        val worldUp = gravity.worldUp
        val toView = bufferToView(image)
        val readings = HashMap<Int, AnswerOption>()
        val visible = detections.filter { it.id > 0 && (accepted == null || it.id in accepted) }
        for (detection in visible) {
            CardOrientation.decode(detection, sensorRotationDegrees, worldUp)?.let {
                readings[detection.id] = it
            }
        }

        val now = SystemClock.elapsedRealtime()
        val changes = stabilizer.update(readings, now)
        if (changes.isNotEmpty()) _confirmations.tryEmit(changes)
        val confirmed = stabilizer.confirmed()

        if (lastFrameAt > 0) {
            val instant = 1000f / (now - lastFrameAt).coerceAtLeast(1)
            fps = if (fps == 0f) instant else fps * 0.8f + instant * 0.2f
        }
        lastFrameAt = now

        _frames.value = ScanFrame(
            cards = visible.map { detection ->
                ScannedCard(
                    cardNumber = detection.id,
                    reading = readings[detection.id],
                    confirmed = confirmed[detection.id],
                    outline = detection.corners.map { toView.map(it) },
                    center = toView.map(detection.center),
                )
            },
            processingMillis = now - started,
            framesPerSecond = fps,
            imageSize = Size(image.width, image.height),
        )
    }

    /** Maps camera-buffer pixels to preview pixels; identity until the preview is laid out. */
    private fun bufferToView(image: ImageProxy): Matrix {
        val result = Matrix()
        val toView = sensorToView ?: return result
        if (image.imageInfo.sensorToBufferTransformMatrix.invert(result)) {
            result.postConcat(toView)
        }
        return result
    }

    private fun Matrix.map(point: Vec2): Vec2 {
        val values = floatArrayOf(point.x, point.y)
        mapPoints(values)
        return Vec2(values[0], values[1])
    }

    private companion object {
        const val TAG = "TagScanner"

        /** Clockwise rotation that makes the back camera's frames upright in portrait. */
        fun backCameraSensorRotation(context: Context): Int {
            val manager = context.getSystemService(CameraManager::class.java) ?: return 90
            return try {
                manager.cameraIdList.asSequence()
                    .map { manager.getCameraCharacteristics(it) }
                    .firstOrNull {
                        it.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                    }
                    ?.get(CameraCharacteristics.SENSOR_ORIENTATION)
                    ?: 90
            } catch (e: Exception) {
                Log.w(TAG, "Could not read the camera orientation", e)
                90
            }
        }
    }
}
