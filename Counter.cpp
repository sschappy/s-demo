#include "Counter.hpp"

int CalCulator::addOne() { ++i; return 0; }
int CalCulator::subOne() { --i; return 0; }
int CalCulator::add(int k) { i += k; return 0; }
int CalCulator::sub(int k) { i -= k; return 0;}

const char* getGreeting() {
    return "hello from c++ via SWIG!";
}

int add(int a, int b) {
    return a + b;
}