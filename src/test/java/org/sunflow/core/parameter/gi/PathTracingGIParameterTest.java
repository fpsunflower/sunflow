package org.sunflow.core.parameter.gi;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;

public class PathTracingGIParameterTest {

    SunflowAPI api;
    PathTracingGIParameter gi;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        gi = new PathTracingGIParameter();
    }

    @Test
    public void testSetupAPI() {
        // Set values
        gi.samples = 99;

        // Set parameters
        gi.setup(api);

        Assert.assertEquals(GlobalIlluminationParameter.TYPE_PATH, api.getParameterList().getString(GlobalIlluminationParameter.PARAM_ENGINE, ""));
        Assert.assertEquals(gi.samples, api.getParameterList().getInt(PathTracingGIParameter.PARAM_SAMPLES,0));
    }

}
