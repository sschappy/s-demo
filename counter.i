/* counter.i - 修正后的SWIG接口文件 */
%module counter

/* 明确指示SWIG使用C++编译器和语法 */
%inline %{
/* 内联C++代码块，确保后续解析在C++上下文中进行 */
%}

%{
/* 包含原始C++头文件 */
#include "Counter.hpp"
%}

/* 使用C++模式解析并封装整个头文件 */
%include "Counter.hpp"

// 不再需要在这里加载库，因为 NativeLibLoader 会在类加载时处理
// 只需要确保 NativeLibLoader 在 JNI 类之前被加载
%pragma(java) jniclasscode=%{
    // 确保 NativeLibLoader 类被加载，这会触发静态块中的库加载
    static {
        try {
            Class.forName("com.example.counter.NativeLibLoader");
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load NativeLibLoader: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
%}