cmake_minimum_required(VERSION 3.22.1)
project("s25_agent_native")

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_library(LOG_LIB log)
find_library(ANDROID_LIB android)
find_library(AAUDIO_LIB aaudio)

# Include directories
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# Link directory for prebuilt llama.cpp libraries
link_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}
)

# Unified native library
add_library(s25_agent_native SHARED
    llama_jni.cpp
    sherpa_indic_jni.cpp
    audio_router.cpp
    sqlite_vec_sqlcipher.c
)

target_link_libraries(s25_agent_native
    ${LOG_LIB}
    ${ANDROID_LIB}
    ${AAUDIO_LIB}
    llama
    ggml
    ggml-base
    ggml-cpu
)
