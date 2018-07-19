package org.sunflow.core.parameter.shader;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.image.Color;

public class UberShaderParameter extends ShaderParameter {

    Color diffuse;
    float diffuseBlend;
    String diffuseTexture;

    Color specular;
    float specularBlend;
    String specularTexture;

    int samples;
    float glossyness;


    public UberShaderParameter(String name) {
        super(name);
        // Default values from UberShader
        diffuse = specular = Color.GRAY;
        diffuseTexture = specularTexture = "";
        diffuseBlend = specularBlend = 1;
        glossyness = 0;
        samples = 4;
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter("diffuse", null, diffuse.getRGB());
        if (!diffuseTexture.isEmpty()) {
            api.parameter("diffuse.texture", diffuseTexture);
        }
        api.parameter("diffuse.blend", diffuseBlend);
        api.parameter("specular", null, specular.getRGB());
        if (!specularTexture.isEmpty()) {
            api.parameter("specular.texture", specularTexture);
        }
        api.parameter("specular.blend", specularBlend);
        api.parameter("glossyness", glossyness);
        api.parameter("samples", samples);

        api.shader(name, TYPE_UBER);
    }

    public Color getDiffuse() {
        return diffuse;
    }

    public void setDiffuse(Color diffuse) {
        this.diffuse = diffuse;
    }

    public float getDiffuseBlend() {
        return diffuseBlend;
    }

    public void setDiffuseBlend(float diffuseBlend) {
        this.diffuseBlend = diffuseBlend;
    }

    public String getDiffuseTexture() {
        return diffuseTexture;
    }

    public void setDiffuseTexture(String diffuseTexture) {
        this.diffuseTexture = diffuseTexture;
    }

    public Color getSpecular() {
        return specular;
    }

    public void setSpecular(Color specular) {
        this.specular = specular;
    }

    public String getSpecularTexture() {
        return specularTexture;
    }

    public void setSpecularTexture(String specularTexture) {
        this.specularTexture = specularTexture;
    }

    public float getSpecularBlend() {
        return specularBlend;
    }

    public void setSpecularBlend(float specularBlend) {
        this.specularBlend = specularBlend;
    }

    public int getSamples() {
        return samples;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }

    public float getGlossyness() {
        return glossyness;
    }

    public void setGlossyness(float glossyness) {
        this.glossyness = glossyness;
    }
}
