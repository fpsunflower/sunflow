package org.sunflow.core.filter;

import org.sunflow.core.Filter;

public class BoxFilter implements Filter {
    public float getSize() {
        return 1.0f;
    }

    public float get(float x, float y) {
        return 1.0f;
    }
}