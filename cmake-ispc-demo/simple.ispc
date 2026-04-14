export void vector_add(
    uniform const float * uniform a,
    uniform const float * uniform b,
    uniform float * uniform out,
    uniform int n)
{
    foreach (i = 0 ... n) {
        out[i] = a[i] + b[i];
    }
}
