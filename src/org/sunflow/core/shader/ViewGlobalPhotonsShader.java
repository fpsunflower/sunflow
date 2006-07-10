package org.sunflow.core.shader;

import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;

public class ViewGlobalPhotonsShader implements Shader {
    public Color getRadiance(ShadingState state) {
        return state.getGlobalRadiance();
    }

    public void scatterPhoton(ShadingState state, Color power) {
    }
}