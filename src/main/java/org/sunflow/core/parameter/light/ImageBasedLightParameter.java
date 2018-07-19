package org.sunflow.core.parameter.light;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.math.Vector3;

public class ImageBasedLightParameter extends LightParameter {

    public static final String PARAM_CENTER = "center";
    public static final String PARAM_UP = "up";
    public static final String PARAM_FIXED = "fixed";
    public static final String PARAM_TEXTURE = "texture";
    public static final String PARAM_LOW_SAMPLES = "lowsamples";
    int samples;
    int lowSamples = 0;

    String texture = "";
    Vector3 center;
    Vector3 up;
    boolean fixed;

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter(PARAM_TEXTURE, texture);
        api.parameter(PARAM_CENTER, center);
        api.parameter(PARAM_UP, up);
        api.parameter(PARAM_FIXED, fixed);
        api.parameter(PARAM_SAMPLES, samples);

        if (lowSamples == 0) {
            api.parameter(PARAM_LOW_SAMPLES, samples);
        }
        api.light(name, TYPE_IMAGE_BASED);
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }

    public Vector3 getCenter() {
        return center;
    }

    public void setCenter(Vector3 center) {
        this.center = center;
    }

    public Vector3 getUp() {
        return up;
    }

    public void setUp(Vector3 up) {
        this.up = up;
    }

    public boolean isFixed() {
        return fixed;
    }

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    public int getSamples() {
        return samples;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }

    public int getLowSamples() {
        return lowSamples;
    }

    public void setLowSamples(int lowSamples) {
        this.lowSamples = lowSamples;
    }
}
