package org.sunflow.core.parameter.shader;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.image.Color;

public class PhongShaderParameter extends ShaderParameter {

    String texture = "";
    Color diffuse;
    Color specular;
    float power;
    int samples;

    public PhongShaderParameter(String name) {
        super(name);
        diffuse = Color.GRAY;
        specular = Color.GRAY;
        power = 20;
        // Number of Rays
        samples = 4;
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter("specular", null, specular.getRGB());
        api.parameter("power", power);
        api.parameter("samples", samples);

        if (texture.isEmpty()) {
            api.parameter("diffuse", null, diffuse.getRGB());
            api.shader(name, TYPE_PHONG);
        } else {
            api.parameter("texture", texture);
            api.shader(name, TYPE_TEXTURED_PHONG);
        }
    }

    public Color getDiffuse() {
        return diffuse;
    }

    public void setDiffuse(Color diffuse) {
        this.diffuse = diffuse;
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }

    public Color getSpecular() {
        return specular;
    }

    public void setSpecular(Color specular) {
        this.specular = specular;
    }

    public float getPower() {
        return power;
    }

    public void setPower(float power) {
        this.power = power;
    }

    public int getSamples() {
        return samples;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }
}
