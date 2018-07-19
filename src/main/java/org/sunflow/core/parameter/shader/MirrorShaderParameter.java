package org.sunflow.core.parameter.shader;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.image.Color;

public class MirrorShaderParameter extends ShaderParameter {

    private Color color;

    public MirrorShaderParameter(String name) {
        super(name);
        color = Color.WHITE;
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter("color", null, color.getRGB());
        api.shader(name, TYPE_MIRROR);
    }

    public Color getColor() {
        return color;
    }

    public void setReflection(Color color) {
        this.color = color;
    }
}
