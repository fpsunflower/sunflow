package org.sunflow.core.filter;

import org.sunflow.core.Filter;

public class GaussianFilter implements Filter {
    private float s;
    private float es2;

    public GaussianFilter(float size) {
        s = size;
        es2 = (float) -Math.exp(-s * s);
    }

    public float getSize() {
        return s;
    }

    public float get(float x, float y) {
        float gx = (float) Math.exp(-x * x) + es2;
        float gy = (float) Math.exp(-y * y) + es2;
        return gx * gy;
    }
}