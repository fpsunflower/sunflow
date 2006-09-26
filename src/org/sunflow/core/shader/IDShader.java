package org.sunflow.core.shader;

import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;

public class IDShader implements Shader {
    public Color getRadiance(ShadingState state) {
        return new Color(state.getInstance().hashCode() + state.getPrimitiveID());
    }

    public void scatterPhoton(ShadingState state, Color power) {
    }
}