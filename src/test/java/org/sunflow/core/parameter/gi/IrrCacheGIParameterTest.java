package org.sunflow.core.parameter.gi;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.Options;
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

        Options options = (Options) api.getRenderObjects().get(SunflowAPI.DEFAULT_OPTIONS).obj;

        Assert.assertEquals(GlobalIlluminationParameter.TYPE_IRR_CACHE, options.getString(GlobalIlluminationParameter.PARAM_ENGINE, ""));
        Assert.assertEquals(gi.samples, options.getInt(IrrCacheGIParameter.PARAM_SAMPLES, 0));
        Assert.assertEquals(gi.tolerance, options.getFloat(IrrCacheGIParameter.PARAM_TOLERANCE, 0), 0);
        Assert.assertEquals(gi.minSpacing, options.getFloat(IrrCacheGIParameter.PARAM_MIN_SPACING, 0), 0);
        Assert.assertEquals(gi.maxSpacing, options.getFloat(IrrCacheGIParameter.PARAM_MAX_SPACING, 0), 0);
    }

    @Test
    public void testSetupAPIWithGlobal() {
        // Set values
        IlluminationParameter global = new IlluminationParameter();
        gi.global = global;

        // Set parameters
        gi.setup(api);

        Options options = (Options) api.getRenderObjects().get(SunflowAPI.DEFAULT_OPTIONS).obj;

        Assert.assertEquals(GlobalIlluminationParameter.TYPE_IRR_CACHE, options.getString(GlobalIlluminationParameter.PARAM_ENGINE, ""));
        Assert.assertEquals(gi.global.getMap(), options.getString(IrrCacheGIParameter.PARAM_GLOBAL, ""));
        Assert.assertEquals(gi.global.getEmit(), options.getInt(IrrCacheGIParameter.PARAM_GLOBAL_EMIT, 0));
        Assert.assertEquals(gi.global.getGather(), options.getInt(IrrCacheGIParameter.PARAM_GLOBAL_GATHER, 0));
        Assert.assertEquals(gi.global.getRadius(), options.getFloat(IrrCacheGIParameter.PARAM_GLOBAL_RADIUS, 0), 0);
    }

}
