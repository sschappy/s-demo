cmake_minimum_required(VERSION 3.22)

project(ispc_demo LANGUAGES CXX)

include(CheckLanguage)
check_language(ISPC)

if(CMAKE_ISPC_COMPILER)
    enable_language(ISPC)
    message(STATUS "Using native ISPC language support: ${CMAKE_ISPC_COMPILER}")

    add_library(ispc_kernels STATIC simple.ispc)
else()
    message(STATUS "Native ISPC language not available, fallback to custom command")

    find_program(ISPC_EXECUTABLE NAMES ispc REQUIRED)

    set(ISPC_SOURCE "${CMAKE_CURRENT_SOURCE_DIR}/simple.ispc")
    set(ISPC_OBJECT "${CMAKE_CURRENT_BINARY_DIR}/simple_ispc.obj")

    add_custom_command(
        OUTPUT "${ISPC_OBJECT}"
        COMMAND "${ISPC_EXECUTABLE}" "${ISPC_SOURCE}" -o "${ISPC_OBJECT}"
        DEPENDS "${ISPC_SOURCE}"
        COMMENT "Compiling ISPC source: ${ISPC_SOURCE}"
        VERBATIM
    )

    add_library(ispc_kernels STATIC "${ISPC_OBJECT}")
    set_source_files_properties("${ISPC_OBJECT}" PROPERTIES
        EXTERNAL_OBJECT TRUE
        GENERATED TRUE
    )
    set_target_properties(ispc_kernels PROPERTIES LINKER_LANGUAGE CXX)
endif()

add_executable(ispc_app main.cpp)
target_compile_features(ispc_app PRIVATE cxx_std_17)
target_link_libraries(ispc_app PRIVATE ispc_kernels)
