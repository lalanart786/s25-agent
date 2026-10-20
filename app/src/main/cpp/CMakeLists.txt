cmake_minimum_required(VERSION 3.22.1)
project("s25_agent_native")

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Enable OpenMP for Oryon 8-core multi-threading
find_package(OpenMP)
if(OpenMP_CXX_FOUND)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${OpenMP_C_FLAGS}")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${OpenMP_CXX_FLAGS}")
endif()

# Vulkan Graphics & Compute for Adreno 830 GPU
find_library(VULKAN_LIB vulkan)
find_library(LOG_LIB log)
find_library(ANDROID_LIB android)
find_library(AAUDIO_LIB aaudio)

# Include directories
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
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
    ${VULKAN_LIB}
    ${AAUDIO_LIB}
)
