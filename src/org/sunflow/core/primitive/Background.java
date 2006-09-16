package org.sunflow.core.primitive;

import org.sunflow.core.IntersectionState;
import org.sunflow.core.Primitive;
import org.sunflow.core.Ray;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.core.shader.ConstantShader;
import org.sunflow.image.Color;

public class Background implements Primitive {
    private Shader shader;

    public Background(Color c) {
        shader = new ConstantShader(c);
    }

    public void prepareShadingState(ShadingState state) {
        if (state.getDepth() == 0)
            state.setShader(shader);
    }

    public void intersect(Ray r, IntersectionState state) {
        if (r.getMax() == Float.POSITIVE_INFINITY)
            state.setIntersection(this, 0, 0, 0);
    }
}