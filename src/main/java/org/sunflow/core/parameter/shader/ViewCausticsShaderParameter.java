package org.sunflow.core.parameter.shader;

import org.sunflow.SunflowAPIInterface;

public class ViewCausticsShaderParameter extends ShaderParameter {

    public ViewCausticsShaderParameter(String name) {
        super(name);
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        api.shader(name, TYPE_VIEW_CAUSTICS);
    }
}
