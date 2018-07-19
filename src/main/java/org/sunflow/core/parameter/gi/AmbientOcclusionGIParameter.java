package org.sunflow.core.parameter.gi;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.image.Color;

/**
 * Global Illumination with Ambient Occlusion
 */
public class AmbientOcclusionGIParameter extends GlobalIlluminationParameter {

    public static final String PARAM_BRIGHT = "gi.ambocc.bright";
    public static final String PARAM_DARK = "gi.ambocc.dark";
    public static final String PARAM_SAMPLES = "gi.ambocc.samples";
    public static final String PARAM_MAXDIST = "gi.ambocc.maxdist";

    Color bright;
    Color dark;
    int samples;
    float maxDist = 0;

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter(GlobalIlluminationParameter.PARAM_ENGINE, GlobalIlluminationParameter.TYPE_AMBOCC);
        api.parameter(PARAM_BRIGHT, null, bright.getRGB());
        api.parameter(PARAM_DARK, null, dark.getRGB());
        api.parameter(PARAM_SAMPLES, samples);

        if (maxDist > 0) {
            api.parameter(PARAM_MAXDIST, maxDist);
        }
        super.setup(api);
    }

    public Color getBright() {
        return bright;
    }

    public void setBright(Color bright) {
        this.bright = bright;
    }

    public Color getDark() {
        return dark;
    }

    public void setDark(Color dark) {
        this.dark = dark;
    }

    public int getSamples() {
        return samples;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }

    public float getMaxDist() {
        return maxDist;
    }

    public void setMaxDist(float maxDist) {
        this.maxDist = maxDist;
    }
}
