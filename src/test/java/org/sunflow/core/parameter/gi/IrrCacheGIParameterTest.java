package org.sunflow.core.parameter.gi;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.parameter.IlluminationParameter;

public class IrrCacheGIParameterTest {

    SunflowAPI api;
    IrrCacheGIParameter gi;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        gi = new IrrCacheGIParameter();
    }

    @Test
    public void testSetupAPI() {
        // Set values
        gi.tolerance = 0.2f;
        gi.samples = 20;
        gi.minSpacing = 0.111f;
        gi.maxSpacing = 0.212f;

        // Set parameters
        gi.setup(api);

        Assert.assertEquals(GlobalIlluminationParameter.TYPE_IRR_CACHE, api.getParameterList().getString(GlobalIlluminationParameter.PARAM_ENGINE, ""));
        Assert.assertEquals(gi.samples, api.getParameterList().getInt(IrrCacheGIParameter.PARAM_SAMPLES, 0));
        Assert.assertEquals(gi.tolerance, api.getParameterList().getFloat(IrrCacheGIParameter.PARAM_TOLERANCE, 0), 0);
        Assert.assertEquals(gi.minSpacing, api.getParameterList().getFloat(IrrCacheGIParameter.PARAM_MIN_SPACING, 0), 0);
        Assert.assertEquals(gi.maxSpacing, api.getParameterList().getFloat(IrrCacheGIParameter.PARAM_MAX_SPACING, 0), 0);
    }

    @Test
    public void testSetupAPIWithGlobal() {
        // Set values

        IlluminationParameter global = new IlluminationParameter();
        gi.global = global;

        // Set parameters
        gi.setup(api);

        Assert.assertEquals(GlobalIlluminationParameter.TYPE_IRR_CACHE, api.getParameterList().getString(GlobalIlluminationParameter.PARAM_ENGINE, ""));
        Assert.assertEquals(gi.global.getMap(), api.getParameterList().getString(IrrCacheGIParameter.PARAM_GLOBAL, ""));
        Assert.assertEquals(gi.global.getEmit(), api.getParameterList().getInt(IrrCacheGIParameter.PARAM_GLOBAL_EMIT, 0));
        Assert.assertEquals(gi.global.getGather(), api.getParameterList().getInt(IrrCacheGIParameter.PARAM_GLOBAL_GATHER, 0));
        Assert.assertEquals(gi.global.getRadius(), api.getParameterList().getFloat(IrrCacheGIParameter.PARAM_GLOBAL_RADIUS, 0), 0);
    }

}
