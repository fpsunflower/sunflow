package org.sunflow.core.parameter.gi;

import org.sunflow.SunflowAPIInterface;

/**
 * Instant Global Illumination
 */
public class InstantGIParameter extends GlobalIlluminationParameter {

    public static final String PARAM_SAMPLES = "gi.igi.samples";
    public static final String PARAM_SETS = "gi.igi.sets";
    public static final String PARAM_BIAS = "gi.igi.bias";
    public static final String PARAM_BIAS_SAMPLES = "gi.igi.bias_samples";

    int samples;
    int sets;
    float bias;
    int biasSamples;

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter(GlobalIlluminationParameter.PARAM_ENGINE, GlobalIlluminationParameter.TYPE_IGI);
        api.parameter(PARAM_SAMPLES, samples);
        api.parameter(PARAM_SETS, sets);
        api.parameter(PARAM_BIAS, bias);
        api.parameter(PARAM_BIAS_SAMPLES, biasSamples);
        super.setup(api);
    }

    public int getSamples() {
        return samples;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }

    public int getSets() {
        return sets;
    }

    public void setSets(int sets) {
        this.sets = sets;
    }

    public float getBias() {
        return bias;
    }

    public void setBias(float bias) {
        this.bias = bias;
    }

    public int getBiasSamples() {
        return biasSamples;
    }

    public void setBiasSamples(int biasSamples) {
        this.biasSamples = biasSamples;
    }
}
