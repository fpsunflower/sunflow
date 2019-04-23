package org.sunflow.core.parameter.shader;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.image.Color;

public class DiffuseShaderParameter extends ShaderParameter {

    String texture = "";
    Color diffuse;

    public DiffuseShaderParameter(String name) {
        super(name);
        diffuse = Color.WHITE;
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        if (texture.isEmpty()) {
            api.parameter("diffuse", null, diffuse.getRGB());
            api.shader(name, TYPE_DIFFUSE);
        } else {
            api.parameter("texture", texture);
            api.shader(name, TYPE_TEXTURED_DIFFUSE);
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
}
