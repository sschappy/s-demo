#ifndef __COUNTER_H__
#define __COUNTER_H__

class CalCulator {
public:
    int addOne(); 
    int subOne();
    int add(int k);
    int sub(int k);
    int test() { return i; }
private:
    int i{10};
};

extern const char* getGreeting();
extern int add(int a, int b);

#endif