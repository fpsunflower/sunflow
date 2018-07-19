package org.sunflow.core.parameter.gi;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;

public class InstantGIParameterTest {

    SunflowAPI api;
    InstantGIParameter gi;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        gi = new InstantGIParameter();
    }

    @Test
    public void testSetupAPI() {
        // Set values
        gi.biasSamples = 1;
        gi.samples = 2;
        gi.sets = 3;
        gi.bias = 123f;

        // Set parameters
        gi.setup(api);

        Assert.assertEquals(GlobalIlluminationParameter.TYPE_IGI, api.getParameterList().getString(GlobalIlluminationParameter.PARAM_ENGINE, ""));
        Assert.assertEquals(gi.biasSamples, api.getParameterList().getInt(InstantGIParameter.PARAM_BIAS_SAMPLES,0));
        Assert.assertEquals(gi.samples, api.getParameterList().getInt(InstantGIParameter.PARAM_SAMPLES,0));
        Assert.assertEquals(gi.sets, api.getParameterList().getInt(InstantGIParameter.PARAM_SETS,0));
        Assert.assertEquals(gi.bias, api.getParameterList().getFloat(InstantGIParameter.PARAM_BIAS,0),0);
    }

}
