package org.sunflow.core.parameter.shader;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.image.Color;

public class ConstantShaderParameter extends ShaderParameter {

    private Color color;

    public ConstantShaderParameter(String name) {
        super(name);
        color = Color.WHITE;
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter("color", null, color.getRGB());
        api.shader(name, TYPE_CONSTANT);
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
