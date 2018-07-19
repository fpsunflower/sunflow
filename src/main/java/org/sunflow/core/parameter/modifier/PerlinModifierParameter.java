package org.sunflow.core.parameter.modifier;

import org.sunflow.SunflowAPIInterface;

public class PerlinModifierParameter extends ModifierParameter {

    int function;
    float size;
    float scale;

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter("function", function);
        api.parameter("size", size);
        api.parameter("scale", scale);
        api.modifier(name, TYPE_PERLIN);
    }

    public int getFunction() {
        return function;
    }

    public void setFunction(int function) {
        this.function = function;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }
}
