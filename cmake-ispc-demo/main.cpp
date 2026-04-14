#include <cmath>
#include <iostream>
#include <vector>

extern "C" void vector_add(const float* a, const float* b, float* out, int n);

int main() {
    constexpr int n = 16;
    std::vector<float> a(n), b(n), out(n, 0.0f);

    for (int i = 0; i < n; ++i) {
        a[i] = static_cast<float>(i);
        b[i] = static_cast<float>(i * 2);
    }

    vector_add(a.data(), b.data(), out.data(), n);

    bool ok = true;
    for (int i = 0; i < n; ++i) {
        const float expected = a[i] + b[i];
        if (std::fabs(out[i] - expected) > 1e-6f) {
            ok = false;
            break;
        }
    }

    if (!ok) {
        std::cerr << "ISPC vector_add check failed." << std::endl;
        return 1;
    }

    std::cout << "ISPC vector_add check passed. out[0]=" << out[0]
              << ", out[" << (n - 1) << "]=" << out[n - 1] << std::endl;
    return 0;
}
