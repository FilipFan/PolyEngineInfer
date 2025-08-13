cmake_minimum_required(VERSION 3.22.1)

project("llama-android")

# Build llama.cpp, build output directory is located in `.cxx`.
add_subdirectory(../../../../external/llama.cpp/ build-llama)

add_library(${CMAKE_PROJECT_NAME} SHARED
        llama-android.cpp)

target_link_libraries(${CMAKE_PROJECT_NAME}
        llama
        common
        android
        log)
