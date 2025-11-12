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
find_program(JAVA_TOOL jar)
if (JAVA_COMPILER AND JAVA_TOOL)
    # 工具类 JAR 相关设置
    set(UTILITY_JAR_FILE ${CMAKE_CURRENT_BINARY_DIR}/counter-utils.jar)
    set(UTILITY_CLASSES_DIR ${CMAKE_CURRENT_BINARY_DIR}/utility_classes)
    file(MAKE_DIRECTORY ${UTILITY_CLASSES_DIR})

    # 编译 SWIG 生成的 Java 文件（工具类）
    add_custom_target(compile_utility_java ALL
        COMMAND ${CMAKE_COMMAND} -E echo "Compiling SWIG utility Java files: ${JAVA_COMPILER} -d \"${UTILITY_CLASSES_DIR}\" \"${CMAKE_SWIG_OUTDIR}/*.java\""
        COMMAND ${JAVA_COMPILER} -d "${UTILITY_CLASSES_DIR}" "${CMAKE_SWIG_OUTDIR}/*.java"
        DEPENDS counter_jni
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        COMMENT "Compiling SWIG-generated utility Java files"
    )

    # 创建工具类 JAR 包（不包含 Main.java）
    add_custom_target(create_utility_jar ALL
        COMMAND ${CMAKE_COMMAND} -E echo "Creating utility JAR: ${JAVA_TOOL} cf \"${UTILITY_JAR_FILE}\" -C \"${UTILITY_CLASSES_DIR}\" ."
        COMMAND ${JAVA_TOOL} cf "${UTILITY_JAR_FILE}" -C "${UTILITY_CLASSES_DIR}" .
        DEPENDS compile_utility_java
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        COMMENT "Creating utility JAR with SWIG classes only"
    )

    # 主程序相关设置（与工具类 JAR 分离）
    set(JAVA_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/java)
    set(MAIN_CLASSES_DIR ${CMAKE_CURRENT_BINARY_DIR}/main_classes)
    # set(SWIG_JAR_FILE ${CMAKE_CURRENT_BINARY_DIR})
    file(MAKE_DIRECTORY ${MAIN_CLASSES_DIR})

    # 编译 Main.java，使用工具类 JAR 作为依赖
    add_custom_target(compile_main_java ALL
        COMMAND ${CMAKE_COMMAND} -E echo "Compiling Main.java with utility JAR: ${JAVA_COMPILER} -cp \"${UTILITY_JAR_FILE}\" -d \"${MAIN_CLASSES_DIR}\" \"${JAVA_SOURCE_DIR}/Main.java\""
        COMMAND ${JAVA_COMPILER} -cp "${UTILITY_JAR_FILE}" -d "${MAIN_CLASSES_DIR}" "${JAVA_SOURCE_DIR}/Main.java"
        DEPENDS create_utility_jar
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        COMMENT "Compiling Main.java using utility JAR"
    )

    # 运行主程序（使用工具类 JAR 和本地库）
    add_custom_target(run_java
        COMMAND ${CMAKE_COMMAND} -E echo "Running Java program: ${Java_JAVA_EXECUTABLE} -cp \"${MAIN_CLASSES_DIR};${UTILITY_JAR_FILE}\" -Djava.library.path=\"$<TARGET_FILE_DIR:counter_jni>\" Main"
        COMMAND ${Java_JAVA_EXECUTABLE} -cp "${MAIN_CLASSES_DIR};${UTILITY_JAR_FILE}" -D"java.library.path=$<TARGET_FILE_DIR:counter_jni>" Main
        DEPENDS compile_main_java
        WORKING_DIRECTORY ${MAIN_CLASSES_DIR}
        COMMENT "Running Java program"
    )
else()
    message(WARNING "java compiler not found, skipping Java compilation")
endif()