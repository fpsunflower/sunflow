package org.sunflow.core.parameter.shader;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.image.Color;

public class ShinyShaderParameter extends ShaderParameter {

    String texture = "";
    Color diffuse;
    float shininess;

    public ShinyShaderParameter(String name) {
        super(name);
        diffuse = Color.GRAY;
        shininess = 0.5f;
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter("shiny", shininess);

        if (texture.isEmpty()) {
            api.parameter("diffuse", null, diffuse.getRGB());
            api.shader(name, TYPE_SHINY_DIFFUSE);
        } else {
            api.parameter("texture", texture);
            api.shader(name, TYPE_TEXTURED_SHINY_DIFFUSE);
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

    public float getShininess() {
        return shininess;
    }

    /**
     * Refl parameter
     * @param shininess
     */
    public void setShininess(float shininess) {
        this.shininess = shininess;
    }
}
