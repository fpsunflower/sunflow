package org.sunflow.core.filter;

import org.sunflow.core.Filter;

public class BoxFilter implements Filter {
    private float s;

    public BoxFilter(float size) {
        s = size;
    }

    public float getSize() {
        return s;
    }

    public float get(float x, float y) {
        return 1.0f;
    }
}