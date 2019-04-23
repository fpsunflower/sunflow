package org.sunflow.core.parameter.shader;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.image.Color;

public class AmbientOcclusionShaderParameter extends ShaderParameter {

    String texture = "";
    Color bright;
    Color dark = null;
    int samples = 1;
    float maxDist = 1;

    public AmbientOcclusionShaderParameter(String name) {
        super(name);
        // Default values from AmbientOcclusionShader
        bright = Color.WHITE;
        dark = Color.BLACK;
        samples = 32;
        maxDist = Float.POSITIVE_INFINITY;
    }

    @Override
    public void setup(SunflowAPIInterface api) {

        if(dark!=null) {
            api.parameter("dark", null, dark.getRGB());
            api.parameter("samples", samples);
            api.parameter("maxdist", maxDist);
        }

        if(texture.isEmpty()) {
            api.parameter("bright", null, bright.getRGB());
            api.shader(name, TYPE_AMBIENT_OCCLUSION);
        } else {
            api.parameter("texture", texture);
            api.shader(name, TYPE_TEXTURED_AMBIENT_OCCLUSION);
        }
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
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
