package org.sunflow.core.shader;

import org.sunflow.core.Ray;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Vector3;

public class AmbientOcclusionShader implements Shader {
    private Color bright;
    private Color dark;
    private int samples;
    private float maxDist;

    public AmbientOcclusionShader(Color d) {
        this(d, Float.POSITIVE_INFINITY);
    }

    public AmbientOcclusionShader(Color bright, float maxDist) {
        this(bright, Color.BLACK, 32, maxDist);
    }

    public AmbientOcclusionShader(Color bright, Color dark, int samples, float maxDist) {
        this.bright = bright;
        this.dark = dark;
        this.samples = Math.max(samples, 1);
        this.maxDist = (maxDist <= 0) ? Float.POSITIVE_INFINITY : maxDist;
    }

    public Color getBrightColor(ShadingState state) {
        return bright;
    }

    public Color getRadiance(ShadingState state) {
        // make sure we are on the right side of the material
        state.faceforward();
        OrthoNormalBasis onb = state.getBasis();
        Vector3 w = new Vector3();
        Color bright = getBrightColor(state);
        Color result = Color.black();
        for (int i = 0; i < samples; i++) {
            float xi = (float) state.getRandom(i, 0, samples);
            float xj = (float) state.getRandom(i, 1, samples);
            float phi = (float) (2 * Math.PI * xi);
            float cosPhi = (float) Math.cos(phi);
            float sinPhi = (float) Math.sin(phi);
            float sinTheta = (float) Math.sqrt(xj);
            float cosTheta = (float) Math.sqrt(1.0f - xj);
            w.x = cosPhi * sinTheta;
            w.y = sinPhi * sinTheta;
            w.z = cosTheta;
            onb.transform(w);
            Ray r = new Ray(state.getPoint(), w);
            r.setMax(maxDist);
            result.add(Color.blend(bright, dark, state.traceShadow(r)));
        }
        return result.mul(1.0f / samples);
    }

    public void scatterPhoton(ShadingState state, Color power) {
    }
}