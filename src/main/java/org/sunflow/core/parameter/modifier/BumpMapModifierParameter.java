package org.sunflow.core.parameter.modifier;

import org.sunflow.SunflowAPIInterface;

public class BumpMapModifierParameter extends ModifierParameter {

    float scale;
    String texture = "";

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter("texture", texture);
        api.parameter("scale", scale);
        api.modifier(name, TYPE_BUMP_MAP);
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }
}
