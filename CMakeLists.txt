cmake_minimum_required(VERSION 3.12)
project(CounterJava LANGUAGES CXX)

if (MSVC)
    add_compile_options("/utf-8")
    add_compile_options("$<$<C_COMPILER_ID:MSVC>:/utf-8>")
    add_compile_options("$<$<CXX_COMPILER_ID:MSVC>:/utf-8>")
elseif (ANDROID)
    add_compile_options(-finput-charset=UTF-8 -fexec-charset=UTF-8)
else()
    add_compile_options(-finput-charset=UTF-8 -fexec-charset=UTF-8)
endif()

set(CMAKE_CXX_STANDARD 11)

# 查找必要的包
find_package(SWIG REQUIRED)
include(${SWIG_USE_FILE})

find_package(Java COMPONENTS Development REQUIRED)
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

# 设置SWIG输出目录
set(SWIG_OUTPUT_DIR "${CMAKE_CURRENT_BINARY_DIR}/generated")
file(MAKE_DIRECTORY ${SWIG_OUTPUT_DIR})

set(JAVA_PACKAGE "com.example.counter")
string(REPLACE "." "/" JAVA_PACKAGE_DIR ${JAVA_PACKAGE})

set(CMAKE_SWIG_OUTDIR ${SWIG_OUTPUT_DIR}/java/${JAVA_PACKAGE_DIR})
set(SWIG_OUTFILE_DIR ${SWIG_OUTPUT_DIR}/cpp)

set(CMAKE_SWIG_FLAGS -package ${JAVA_PACKAGE})

# C++ lib
add_library(counter_lib STATIC
    Counter.cpp
    Counter.hpp
)
target_include_directories(counter_lib PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

set_source_files_properties(counter.i PROPERTIES CPLUSPLUS ON)

swig_add_library(counter_jni
    TYPE SHARED
    LANGUAGE java
    SOURCES counter.i
)

swig_link_libraries(counter_jni PRIVATE 
    counter_lib
    ${JNI_LIBRARIES}
)

find_program(JAVA_COMPILER javac)
if (JAVA_COMPILER)
    set(JAVA_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/java)
    set(JAVA_CLASSES_DIR ${CMAKE_CURRENT_SOURCE_DIR}/java_classes)
    file(MAKE_DIRECTORY ${JAVA_CLASSES_DIR})

    set(JAVA_CLASS_PACKAGE_DIR ${JAVA_CLASSES_DIR}/${JAVA_PACKAGE_DIR})
    file(MAKE_DIRECTORY ${JAVA_CLASS_PACKAGE_DIR})

    add_custom_target(compile_java ALL
        COMMAND ${CMAKE_COMMAND} -E echo "Running: ${JAVA_COMPILER} -cp \"${SWIG_OUTPUT_DIR}/java\" -d \"${JAVA_CLASSES_DIR}\" \"${JAVA_SOURCE_DIR}/Main.java\""
        COMMAND ${JAVA_COMPILER} -cp "${SWIG_OUTPUT_DIR}/java" -d "${JAVA_CLASSES_DIR}" "${JAVA_SOURCE_DIR}/Main.java"
        DEPENDS counter_jni
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        COMMENT "Compiling Java main program"
    )

    add_custom_target(run_java
        COMMAND ${CMAKE_COMMAND} -E echo "Running: ${Java_JAVA_EXECUTABLE} -cp \"${JAVA_CLASSES_DIR}\" -Djava.library.path=\"$<TARGET_FILE_DIR:counter_jni>\" Main"
        COMMAND ${Java_JAVA_EXECUTABLE} -cp "${JAVA_CLASSES_DIR}" -D"java.library.path=$<TARGET_FILE_DIR:counter_jni>" Main
        DEPENDS compile_java
        WORKING_DIRECTORY ${JAVA_CLASSES_DIR}
        COMMENT "Running Java program"
    )
else()
    message(WARNING "java compiler not found, skipping Java compilation")
endif()