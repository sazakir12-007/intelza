package com.ht.intelza.scan

import java.nio.ByteBuffer

/** Raw JNI entry points implemented in `src/main/cpp/intelza_jni.c`. */
internal object AprilTagNative {
    const val FLOATS_PER_DETECTION = 13

    init {
        System.loadLibrary("intelza_native")
    }

    @JvmStatic
    external fun nativeCreate(threads: Int, decimate: Float, sigma: Float, maxHamming: Int): Long

    /** [buffer] must be a direct buffer holding an 8-bit grayscale image. */
    @JvmStatic
    external fun nativeDetect(
        handle: Long,
        buffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
    ): FloatArray?

    @JvmStatic
    external fun nativeDestroy(handle: Long)

    @JvmStatic
    external fun nativeCodeCount(): Int

    @JvmStatic
    external fun nativeTagImage(id: Int): ByteArray?
}
