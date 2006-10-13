package org.sunflow.core.shader;

import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;

public class SimpleShader implements Shader {
    public Color getRadiance(ShadingState state) {
        return new Color(Math.abs(state.getRay().dot(state.getNormal())));
    }

    public void scatterPhoton(ShadingState state, Color power) {
    }
}