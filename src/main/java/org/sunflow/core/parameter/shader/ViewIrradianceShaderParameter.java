package org.sunflow.core.parameter.shader;

import org.sunflow.SunflowAPIInterface;

public class ViewIrradianceShaderParameter extends ShaderParameter {

    public ViewIrradianceShaderParameter(String name) {
        super(name);
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        api.shader(name, TYPE_VIEW_IRRADIANCE);
    }
}
