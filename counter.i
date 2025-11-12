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