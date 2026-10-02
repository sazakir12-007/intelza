package com.ht.intelza.scan

import java.io.Closeable
import java.nio.ByteBuffer
import kotlin.math.sqrt

/**
 * A tag found in a camera frame, in that frame's pixel coordinates.
 *
 * [corners] are ordered bottom-left, bottom-right, top-right, top-left relative to the
 * tag as printed, so they rotate together with the card.
 */
data class TagDetection(
    val id: Int,
    val hamming: Int,
    val decisionMargin: Float,
    val center: Vec2,
    val corners: List<Vec2>,
) {
    /** Direction from the centre of the card towards its printed top edge (the "A" side). */
    val cardUp: Vec2
        get() = Vec2.midpoint(corners[2], corners[3]) - center

    /** Approximate side length in pixels, useful for judging scan distance. */
    val sizePx: Float
        get() {
            val dx = corners[1].x - corners[0].x
            val dy = corners[1].y - corners[0].y
            return sqrt(dx * dx + dy * dy)
        }
}

/**
 * Finds tag36h11 AprilTags in grayscale images. Wraps a native detector, so it must be
 * [close]d. Calls are serialised; use one instance per analysis thread.
 */
class AprilTagDetector(config: Config = Config()) : Closeable {

    data class Config(
        val threads: Int = Runtime.getRuntime().availableProcessors().coerceIn(1, 4),
        /** Downscale factor for the quad search; higher is faster but sees less far. */
        val decimate: Float = 2f,
        val sigma: Float = 0f,
        /** Bit errors to correct. tag36h11 has a minimum Hamming distance of 11, so 2 is safe. */
        val maxHamming: Int = 2,
        /** Detections below this decoding confidence are dropped as likely false positives. */
        val minDecisionMargin: Float = 15f,
    )

    private val minDecisionMargin = config.minDecisionMargin
    private var handle: Long = AprilTagNative.nativeCreate(
        config.threads,
        config.decimate,
        config.sigma,
        config.maxHamming,
    )

    init {
        check(handle != 0L) { "Could not create the AprilTag detector" }
    }

    @Synchronized
    fun detect(gray: ByteBuffer, width: Int, height: Int, rowStride: Int): List<TagDetection> {
        check(handle != 0L) { "Detector is closed" }
        val raw = AprilTagNative.nativeDetect(handle, gray, width, height, rowStride)
            ?: return emptyList()
        val count = raw.size / AprilTagNative.FLOATS_PER_DETECTION
        return (0 until count).mapNotNull { i ->
            val o = i * AprilTagNative.FLOATS_PER_DETECTION
            val margin = raw[o + 2]
            if (margin < minDecisionMargin) return@mapNotNull null
            TagDetection(
                id = raw[o].toInt(),
                hamming = raw[o + 1].toInt(),
                decisionMargin = margin,
                center = Vec2(raw[o + 3], raw[o + 4]),
                corners = List(4) { k -> Vec2(raw[o + 5 + 2 * k], raw[o + 6 + 2 * k]) },
            )
        }
    }

    @Synchronized
    override fun close() {
        if (handle != 0L) {
            AprilTagNative.nativeDestroy(handle)
            handle = 0L
        }
    }

    companion object {
        /** Number of distinct tags in the family; card numbers run from 1 to this minus one. */
        val codeCount: Int by lazy { AprilTagNative.nativeCodeCount() }

        /**
         * The tag's cells in upright orientation, row by row: `true` means black.
         * Includes the outer white border, so the grid is 10 x 10 for tag36h11.
         */
        fun tagCells(id: Int): Array<BooleanArray> {
            val pixels = requireNotNull(AprilTagNative.nativeTagImage(id)) { "No tag with id $id" }
            val size = sqrt(pixels.size.toDouble()).toInt()
            return Array(size) { y -> BooleanArray(size) { x -> pixels[y * size + x].toInt() == 0 } }
        }
    }
}
