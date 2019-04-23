package org.sunflow.core.parameter.gi;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.Options;

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

        Options options = (Options) api.getRenderObjects().get(SunflowAPI.DEFAULT_OPTIONS).obj;

        Assert.assertEquals(GlobalIlluminationParameter.TYPE_PATH, options.getString(GlobalIlluminationParameter.PARAM_ENGINE, ""));
        Assert.assertEquals(gi.samples, options.getInt(PathTracingGIParameter.PARAM_SAMPLES,0));
    }

}
