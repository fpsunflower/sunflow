package org.sunflow.core.parameter.shader;

import org.sunflow.SunflowAPIInterface;

public class IDShaderParameter extends ShaderParameter {

    public IDShaderParameter(String name) {
        super(name);
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        api.shader(name, TYPE_SHOW_INSTANCE_ID);
    }
}
