cmake_minimum_required(VERSION 3.22.1)
project(nfsmw_android LANGUAGES C CXX)

# SDL's Android Activity loads libSDL3.so and then libmain.so. Keep both native
# libraries and the ReXGlue app in this Gradle externalNativeBuild graph.
get_filename_component(NFSMW_REXSDK_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../../../sdk" ABSOLUTE)
set(REXSDK_DIR "${NFSMW_REXSDK_DIR}" CACHE PATH "ReXGlue SDK source tree")
set(REXGLUE_USE_VULKAN ON CACHE BOOL "Build ReXGlue's Vulkan backend" FORCE)
set(REXGLUE_USE_D3D12 OFF CACHE BOOL "Android uses Vulkan" FORCE)
set(REXGLUE_ENABLE_TRACY OFF CACHE BOOL "Disable Tracy on Android" FORCE)
set(REXGLUE_VERSION_OVERRIDE "0.10.0-dev.android" CACHE STRING "ReXGlue SDK version for Android")
set(REXGLUE_RECOMP_DEBUG_INFO "none" CACHE STRING "Omit debug tables for the large generated guest code")
set(SDL_SHARED ON CACHE BOOL "SDL shared library loaded by SDLActivity" FORCE)
set(SDL_STATIC OFF CACHE BOOL "Use SDL's shared Android library" FORCE)

add_subdirectory("${CMAKE_CURRENT_LIST_DIR}/../../../../../app" "nfsmw-app")

if(TARGET SDL3-shared)
    # SDLActivity calls System.loadLibrary("SDL3") even in debug builds.
    set_target_properties(SDL3-shared PROPERTIES DEBUG_POSTFIX "")
endif()

# The game never needs to access the extracted disc through a SAF URI. Java
# imports it into app-private POSIX storage before GameActivity starts.
target_link_options(nfsmw PRIVATE "-Wl,-z,max-page-size=16384")
