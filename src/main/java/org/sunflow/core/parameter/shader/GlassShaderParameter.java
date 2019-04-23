package org.sunflow.core.parameter.shader;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.image.Color;

public class GlassShaderParameter extends ShaderParameter {

    float eta;
    Color color;
    float absorptionDistance;
    Color absorptionColor;

    public GlassShaderParameter(String name) {
        super(name);
        // Default values from GlassShader
        eta = 1.3f;
        color = Color.WHITE;
        absorptionDistance = 0; // disabled by default
        absorptionColor = Color.GRAY; // 50% absorbtion
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter("eta", eta);
        api.parameter("color", null, color.getRGB());
        api.parameter("absorption.distance", absorptionDistance);
        api.parameter("absorption.color", null, absorptionColor.getRGB());
        api.shader(name, TYPE_GLASS);
    }

    public float getEta() {
        return eta;
    }

    public void setEta(float eta) {
        this.eta = eta;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public float getAbsorptionDistance() {
        return absorptionDistance;
    }

    public void setAbsorptionDistance(float absorptionDistance) {
        this.absorptionDistance = absorptionDistance;
    }

    public Color getAbsorptionColor() {
        return absorptionColor;
    }

    public void setAbsorptionColor(Color absorptionColor) {
        this.absorptionColor = absorptionColor;
    }
}
