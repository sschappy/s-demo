#include "Counter.hpp"

// int Counter::addOne() { ++i; return 0; }
// int Counter::subOne() { --i; return 0; }
// int Counter::add(int k) { i += k; return 0; }
// int Counter::sub(int k) { i -= k; return 0;}

#include <iostream>

const char* getGreeting() {
    return "hello from c++ via SWIG!";
}

int add(int a, int b) {
    return a + b;
}