package org.sunflow.core.parameter.shader;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.image.Color;

public class WardShaderParameter extends ShaderParameter {


    String texture = "";
    Color diffuse;
    Color specular;

    int samples;
    float roughnessX, roughnessY;

    public WardShaderParameter(String name) {
        super(name);
    }

    @Override
    public void setup(SunflowAPIInterface api) {

        api.parameter("specular", null, specular.getRGB());
        api.parameter("roughnessX", roughnessX);
        api.parameter("roughnessY", roughnessY);
        api.parameter("samples", samples);

        if (texture.isEmpty()) {
            api.parameter("diffuse", null, diffuse.getRGB());
            api.shader(name, TYPE_WARD);
        } else {
            api.parameter("texture", texture);
            api.shader(name, TYPE_TEXTURED_WARD);
        }
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }

    public Color getDiffuse() {
        return diffuse;
    }

    public void setDiffuse(Color diffuse) {
        this.diffuse = diffuse;
    }

    public Color getSpecular() {
        return specular;
    }

    public void setSpecular(Color specular) {
        this.specular = specular;
    }

    public float getRoughnessX() {
        return roughnessX;
    }

    public void setRoughnessX(float roughnessX) {
        this.roughnessX = roughnessX;
    }

    public float getRoughnessY() {
        return roughnessY;
    }

    public void setRoughnessY(float roughnessY) {
        this.roughnessY = roughnessY;
    }

    public int getSamples() {
        return samples;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }
}
