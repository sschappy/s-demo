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
    # 定义所有需要打包的本地库目标
    set(ALL_NATIVE_TARGETS counter_lib counter_jni)

    # 自动收集所有目标库文件
    set(NATIVE_LIB_FILES "")
    foreach(target IN LISTS ALL_NATIVE_TARGETS)
        if(TARGET ${target})
            list(APPEND NATIVE_LIB_FILES $<TARGET_FILE:${target}>)
            message(STATUS "Added native library target: ${target}")
        else()
            message(WARNING "Target ${target} not found, skipping")
        endif()
    endforeach()
    
    # java源文件设置
    set(JAVA_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/java/${JAVA_PACKAGE_DIR}/)

    # 工具类 JAR 相关设置
    set(UTILITY_JAR_FILE ${CMAKE_CURRENT_BINARY_DIR}/counter-utils.jar)
    set(UTILITY_CLASSES_DIR ${CMAKE_CURRENT_BINARY_DIR}/utility_classes)
    set(NATIVE_LIBS_DIR ${CMAKE_CURRENT_BINARY_DIR}/native_libs)
    set(NATIVE_LIBS_JAR_DIR ${CMAKE_CURRENT_BINARY_DIR}/native_libs_jar)
    file(MAKE_DIRECTORY ${UTILITY_CLASSES_DIR})
    file(MAKE_DIRECTORY ${NATIVE_LIBS_DIR})
    file(MAKE_DIRECTORY ${NATIVE_LIBS_JAR_DIR})
    file(MAKE_DIRECTORY ${NATIVE_LIBS_JAR_DIR}/nativeLib)

    # 复制所有本地库到临时目录
    add_custom_target(copy_native_libs ALL
        COMMENT "Copying all native libraries to temporary directory"
    )

    foreach(lib_file IN LISTS NATIVE_LIB_FILES)
        add_custom_command(TARGET copy_native_libs
            COMMAND ${CMAKE_COMMAND} -E copy "${lib_file}" "${NATIVE_LIBS_DIR}/"
            COMMENT "Copying ${lib_file}"
        )
    endforeach()

    # 复制库文件到 JAR 目录结构
    add_custom_target(prepare_jar_native_libs ALL
        COMMENT "Preparing native libraries for JAR packaging"
    )
    
    foreach(lib_file IN LISTS NATIVE_LIB_FILES)
        add_custom_command(TARGET prepare_jar_native_libs
            COMMAND ${CMAKE_COMMAND} -E copy "${lib_file}" "${NATIVE_LIBS_JAR_DIR}/nativeLib/"
            COMMENT "Preparing ${lib_file} for JAR"
        )
    endforeach()

    # 编译 SWIG 生成的 Java 文件（工具类）
    add_custom_target(compile_utility_java ALL
        COMMAND ${CMAKE_COMMAND} -E echo "Compiling SWIG utility Java files..."
        COMMAND ${JAVA_COMPILER} -d "${UTILITY_CLASSES_DIR}" "${JAVA_SOURCE_DIR}/NativeLibLoader.java" "${CMAKE_SWIG_OUTDIR}/*.java"
        DEPENDS copy_native_libs prepare_jar_native_libs
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        COMMENT "Compiling SWIG-generated utility Java files"
    )

    # 创建工具类 JAR 包（包含类和本地库）
    add_custom_target(create_utility_jar ALL
        COMMAND ${CMAKE_COMMAND} -E echo "Creating utility JAR with classes and native libraries in nativeLib directory..."
        COMMAND ${JAVA_TOOL} cf "${UTILITY_JAR_FILE}" -C "${UTILITY_CLASSES_DIR}" . -C "${NATIVE_LIBS_JAR_DIR}" .
        DEPENDS compile_utility_java
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        COMMENT "Creating utility JAR with SWIG classes and native libraries"
    )

    # 主程序相关设置（与工具类 JAR 分离）
    set(MAIN_CLASSES_DIR ${CMAKE_CURRENT_BINARY_DIR}/main_classes)
    file(MAKE_DIRECTORY ${MAIN_CLASSES_DIR})

    # 编译 Main.java，使用工具类 JAR 作为依赖
    add_custom_target(compile_main_java ALL
        COMMAND ${CMAKE_COMMAND} -E echo "Compiling Main.java with utility JAR..."
        COMMAND ${JAVA_COMPILER} -cp "${UTILITY_JAR_FILE}" -d "${MAIN_CLASSES_DIR}" "${JAVA_SOURCE_DIR}/Main.java"
        DEPENDS create_utility_jar
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        COMMENT "Compiling Main.java using utility JAR"
    )

    # 运行主程序（使用工具类 JAR 和本地库）
    add_custom_target(run_java
        COMMAND ${CMAKE_COMMAND} -E echo "Running Java program..."
        COMMAND ${Java_JAVA_EXECUTABLE} --enable-native-access=ALL-UNNAMED -cp "${MAIN_CLASSES_DIR};${UTILITY_JAR_FILE}" com.example.counter.Main
        DEPENDS compile_main_java
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        COMMENT "Running Java program"
    )

    # 添加一个目标来显示 JAR 内容（用于调试）
    add_custom_target(show_jar_contents
        COMMAND ${CMAKE_COMMAND} -E echo "=== JAR CONTENTS ==="
        COMMAND ${JAVA_TOOL} tf "${UTILITY_JAR_FILE}"
        DEPENDS create_utility_jar
        COMMENT "Showing JAR contents"
    )

    # 调试目标：检查本地库文件
    add_custom_target(check_native_libs
        COMMAND ${CMAKE_COMMAND} -E echo "=== Checking native libraries ==="
        COMMAND ${CMAKE_COMMAND} -E echo "NATIVE_LIBS_DIR: ${NATIVE_LIBS_DIR}"
        COMMAND ${CMAKE_COMMAND} -E echo "NATIVE_LIBS_JAR_DIR: ${NATIVE_LIBS_JAR_DIR}"
        COMMAND ${CMAKE_COMMAND} -E echo "Files in native_libs directory:"
        COMMAND cmd.exe /c "if exist \"${NATIVE_LIBS_DIR}\" dir \"${NATIVE_LIBS_DIR}\""
        COMMAND ${CMAKE_COMMAND} -E echo "Files in native_libs_jar directory:"
        COMMAND cmd.exe /c "if exist \"${NATIVE_LIBS_JAR_DIR}\" dir \"${NATIVE_LIBS_JAR_DIR}\" /s"
        DEPENDS copy_native_libs prepare_jar_native_libs
        COMMENT "Checking native libraries"
    )

else()
    message(WARNING "java compiler not found, skipping Java compilation")
endif()