cmake_minimum_required(VERSION 3.22.1)
project(nfsmw_android LANGUAGES CXX)

add_library(nfsmw_android SHARED native_bootstrap.cpp)
target_compile_features(nfsmw_android PRIVATE cxx_std_23)
target_compile_options(nfsmw_android PRIVATE -Wall -Wextra -Werror=return-type)
target_link_libraries(nfsmw_android PRIVATE android log vulkan)
