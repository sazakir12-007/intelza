cmake_minimum_required(VERSION 3.22.1)
project(intelza_native C)

# AprilTag 3 (vendored from https://github.com/AprilRobotics/apriltag, BSD 2-Clause).
# Only the detector core and the tag36h11 family are compiled.
set(APRILTAG_DIR ${CMAKE_CURRENT_SOURCE_DIR}/apriltag)
file(GLOB APRILTAG_COMMON_SOURCES ${APRILTAG_DIR}/common/*.c)

add_library(apriltag STATIC
        ${APRILTAG_DIR}/apriltag.c
        ${APRILTAG_DIR}/apriltag_quad_thresh.c
        ${APRILTAG_DIR}/tag36h11.c
        ${APRILTAG_COMMON_SOURCES})
target_include_directories(apriltag PUBLIC ${APRILTAG_DIR})
set_target_properties(apriltag PROPERTIES C_STANDARD 99 POSITION_INDEPENDENT_CODE ON)
# Always optimise the detector, even in debug builds, so scanning stays real-time.
target_compile_options(apriltag PRIVATE -O3 -w)

add_library(intelza_native SHARED intelza_jni.c)
target_compile_options(intelza_native PRIVATE -O2 -Wall)
target_link_libraries(intelza_native PRIVATE apriltag log m)
