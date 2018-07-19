package org.sunflow.core.parameter;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.core.ParameterList;
import org.sunflow.core.parameter.shader.ShaderParameter;
import org.sunflow.image.Color;

public class BackgroundParameter implements Parameter {

    public static final String PARAM_BACKGROUND = "background";
    public static final String PARAM_BACKGROUND_SHADER = "background.shader";
    public static final String PARAM_BACKGROUND_INSTANCE = "background.instance";
    public static final String PARAM_TYPE_BACKGROUND = "background";

    Color color;

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter(ParameterList.PARAM_COLOR, null, color.getRGB());
        api.shader(PARAM_BACKGROUND_SHADER, ShaderParameter.TYPE_CONSTANT);
        api.geometry(PARAM_BACKGROUND, PARAM_TYPE_BACKGROUND);
        api.parameter(ParameterList.PARAM_SHADERS, PARAM_BACKGROUND_SHADER);
        api.instance(PARAM_BACKGROUND_INSTANCE, PARAM_TYPE_BACKGROUND);
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
