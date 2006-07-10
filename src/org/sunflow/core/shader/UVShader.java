package org.sunflow.core.shader;

import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;

public class UVShader implements Shader {

    public Color getRadiance(ShadingState state) {
        return new Color(state.getUV().x, state.getUV().y, 0);
    }

    public void scatterPhoton(ShadingState state, Color power) {
    }
}