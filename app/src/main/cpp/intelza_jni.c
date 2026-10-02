// JNI bridge between Kotlin (com.ht.intelza.scan.AprilTagNative) and the AprilTag detector.

#include <jni.h>
#include <stdint.h>
#include <stdlib.h>

#include "apriltag.h"
#include "tag36h11.h"
#include "common/image_u8.h"
#include "common/zarray.h"

// Layout of one detection in the float array returned by nativeDetect:
// id, hamming, decisionMargin, centerX, centerY, then the four corners (x, y).
// Corners follow the detector's convention: bottom-left, bottom-right, top-right,
// top-left of the tag as printed (i.e. counter-clockwise starting at tag (-1, 1)).
#define FLOATS_PER_DETECTION 13

typedef struct {
    apriltag_detector_t *detector;
    apriltag_family_t *family;
} detector_handle;

JNIEXPORT jlong JNICALL
Java_com_ht_intelza_scan_AprilTagNative_nativeCreate(JNIEnv *env, jclass clazz, jint threads,
                                                     jfloat decimate, jfloat sigma,
                                                     jint max_hamming) {
    detector_handle *handle = calloc(1, sizeof(detector_handle));
    if (handle == NULL) {
        return 0;
    }
    handle->family = tag36h11_create();
    handle->detector = apriltag_detector_create();
    apriltag_detector_add_family_bits(handle->detector, handle->family, max_hamming);
    handle->detector->nthreads = threads;
    handle->detector->quad_decimate = decimate;
    handle->detector->quad_sigma = sigma;
    handle->detector->refine_edges = true;
    handle->detector->decode_sharpening = 0.25;
    return (jlong) (intptr_t) handle;
}

JNIEXPORT jfloatArray JNICALL
Java_com_ht_intelza_scan_AprilTagNative_nativeDetect(JNIEnv *env, jclass clazz, jlong handle_ptr,
                                                     jobject buffer, jint width, jint height,
                                                     jint row_stride) {
    detector_handle *handle = (detector_handle *) (intptr_t) handle_ptr;
    if (handle == NULL) {
        return NULL;
    }
    uint8_t *pixels = (*env)->GetDirectBufferAddress(env, buffer);
    jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
    if (pixels == NULL || width <= 0 || height <= 0 || row_stride < width ||
        capacity < (jlong) row_stride * (height - 1) + width) {
        return NULL;
    }

    image_u8_t image = {.width = width, .height = height, .stride = row_stride, .buf = pixels};
    zarray_t *detections = apriltag_detector_detect(handle->detector, &image);

    int count = zarray_size(detections);
    jfloatArray result = (*env)->NewFloatArray(env, count * FLOATS_PER_DETECTION);
    if (result != NULL && count > 0) {
        jfloat *values = malloc(sizeof(jfloat) * count * FLOATS_PER_DETECTION);
        if (values != NULL) {
            for (int i = 0; i < count; i++) {
                apriltag_detection_t *det;
                zarray_get(detections, i, &det);
                jfloat *out = values + i * FLOATS_PER_DETECTION;
                out[0] = (jfloat) det->id;
                out[1] = (jfloat) det->hamming;
                out[2] = det->decision_margin;
                out[3] = (jfloat) det->c[0];
                out[4] = (jfloat) det->c[1];
                for (int k = 0; k < 4; k++) {
                    out[5 + 2 * k] = (jfloat) det->p[k][0];
                    out[6 + 2 * k] = (jfloat) det->p[k][1];
                }
            }
            (*env)->SetFloatArrayRegion(env, result, 0, count * FLOATS_PER_DETECTION, values);
            free(values);
        }
    }
    apriltag_detections_destroy(detections);
    return result;
}

JNIEXPORT void JNICALL
Java_com_ht_intelza_scan_AprilTagNative_nativeDestroy(JNIEnv *env, jclass clazz, jlong handle_ptr) {
    detector_handle *handle = (detector_handle *) (intptr_t) handle_ptr;
    if (handle == NULL) {
        return;
    }
    apriltag_detector_destroy(handle->detector);
    tag36h11_destroy(handle->family);
    free(handle);
}

JNIEXPORT jint JNICALL
Java_com_ht_intelza_scan_AprilTagNative_nativeCodeCount(JNIEnv *env, jclass clazz) {
    apriltag_family_t *family = tag36h11_create();
    jint count = (jint) family->ncodes;
    tag36h11_destroy(family);
    return count;
}

// Renders tag `id` in its canonical (upright) orientation as a square grid of
// 0 (black) / 255 (white) cells, including the outer white border.
JNIEXPORT jbyteArray JNICALL
Java_com_ht_intelza_scan_AprilTagNative_nativeTagImage(JNIEnv *env, jclass clazz, jint id) {
    apriltag_family_t *family = tag36h11_create();
    if (id < 0 || (uint32_t) id >= family->ncodes) {
        tag36h11_destroy(family);
        return NULL;
    }
    image_u8_t *image = apriltag_to_image(family, (uint32_t) id);
    int size = image->width;
    jbyteArray result = (*env)->NewByteArray(env, size * size);
    if (result != NULL) {
        for (int y = 0; y < size; y++) {
            (*env)->SetByteArrayRegion(env, result, y * size, size,
                                       (const jbyte *) (image->buf + y * image->stride));
        }
    }
    image_u8_destroy(image);
    tag36h11_destroy(family);
    return result;
}
