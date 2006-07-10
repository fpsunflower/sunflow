package org.sunflow.core.shader;

import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;

public class ViewCausticsShader implements Shader {
    public Color getRadiance(ShadingState state) {
        state.faceforward();
        state.initCausticSamples();
        return state.diffuse(Color.WHITE);
    }

    public void scatterPhoton(ShadingState state, Color power) {
    }
}