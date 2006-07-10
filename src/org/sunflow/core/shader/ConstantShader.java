package org.sunflow.core.shader;

import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;

public class ConstantShader implements Shader {
    private Color c;

    public ConstantShader(Color c) {
        this.c = c.copy();
    }

    public Color getRadiance(ShadingState state) {
        return c;
    }

    public void scatterPhoton(ShadingState state, Color power) {}
}