package org.sunflow.core.parameter.gi;

import org.sunflow.SunflowAPIInterface;

public class PathTracingGIParameter extends GlobalIlluminationParameter {

    public static final String PARAM_SAMPLES = "gi.path.samples";

    int samples;

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter(GlobalIlluminationParameter.PARAM_ENGINE, GlobalIlluminationParameter.TYPE_PATH);
        api.parameter(PARAM_SAMPLES, samples);
        super.setup(api);
    }

    public int getSamples() {
        return samples;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }
}
