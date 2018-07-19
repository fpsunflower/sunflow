package org.sunflow.core.parameter.gi;

import org.sunflow.SunflowAPIInterface;

/**
 * Disabled Global Illumination
 */
public class DisabledGIParameter extends GlobalIlluminationParameter {

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter(GlobalIlluminationParameter.PARAM_ENGINE, GlobalIlluminationParameter.TYPE_NONE);
        super.setup(api);
    }

}
