package org.sunflow.core.parameter.gi;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.Options;

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

        Options options = (Options) api.getRenderObjects().get(SunflowAPI.DEFAULT_OPTIONS).obj;

        Assert.assertEquals(GlobalIlluminationParameter.TYPE_IGI, options.getString(GlobalIlluminationParameter.PARAM_ENGINE, ""));
        Assert.assertEquals(gi.biasSamples, options.getInt(InstantGIParameter.PARAM_BIAS_SAMPLES,0));
        Assert.assertEquals(gi.samples, options.getInt(InstantGIParameter.PARAM_SAMPLES,0));
        Assert.assertEquals(gi.sets, options.getInt(InstantGIParameter.PARAM_SETS,0));
        Assert.assertEquals(gi.bias, options.getFloat(InstantGIParameter.PARAM_BIAS,0),0);
    }

}
