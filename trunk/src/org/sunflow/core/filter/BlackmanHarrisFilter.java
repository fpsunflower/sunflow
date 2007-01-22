package org.sunflow.core.filter;

import org.sunflow.core.Filter;

public class BlackmanHarrisFilter implements Filter {
    private float s, inv;

    public BlackmanHarrisFilter(float size) {
        s = size;
        inv = 1.0f / (s * 0.5f);
    }

    public float getSize() {
        return s;
    }

    public float get(float x, float y) {
        return bh1d(x * inv) * bh1d(y * inv);
    }

    private float bh1d(float x) {
        if (x < -1.0f || x > 1.0f)
            return 0.0f;
        x = (x + 1) * 0.5f;
        final double A0 = 0.35875;
        final double A1 = -0.48829;
        final double A2 = 0.14128;
        final double A3 = -0.01168;
        return (float) (A0 + A1 * Math.cos(2 * Math.PI * x) + A2 * Math.cos(4 * Math.PI * x) + A3 * Math.cos(6 * Math.PI * x));
    }
}