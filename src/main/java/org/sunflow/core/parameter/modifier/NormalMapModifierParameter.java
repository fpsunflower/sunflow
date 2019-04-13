package org.sunflow.core.parameter.modifier;

import org.sunflow.SunflowAPIInterface;

public class NormalMapModifierParameter extends ModifierParameter {

    String texture = "";

    public NormalMapModifierParameter() {

    }

    public NormalMapModifierParameter(String name) {
        this.name = name;
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter("texture", texture);
        api.modifier(name, TYPE_NORMAL_MAP);
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }
}
