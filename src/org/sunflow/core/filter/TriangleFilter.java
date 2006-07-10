package org.sunflow.core.filter;

import org.sunflow.core.Filter;

public class TriangleFilter implements Filter {
    private float s, inv;

    public TriangleFilter(float size) {
        s = size;
        inv = 1.0f / (s * 0.5f);
    }

    public float getSize() {
        return s;
    }

    public float get(float x, float y) {
        return (1.0f - Math.abs(x * inv)) * (1.0f - Math.abs(y * inv));
    }
}