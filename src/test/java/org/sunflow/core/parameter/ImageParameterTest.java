package org.sunflow.core.parameter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.Options;

public class ImageParameterTest {

    SunflowAPI api;
    ImageParameter parameter;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        parameter = new ImageParameter();
    }

    @Test
    public void testSetupAPI() {
        // Set values
        parameter.resolutionX = 320;
        parameter.resolutionY = 240;

        parameter.aaMin = 1;
        parameter.aaMax = 1;
        parameter.aaSamples = 2;
        parameter.aaContrast = 1.1f;
        parameter.aaJitter = true;
        parameter.aaCache = true;

        parameter.filter = "filter";
        parameter.sampler = "sampler";

        // Set parameters
        parameter.setup(api);

        Options options = (Options) api.getRenderObjects().get(SunflowAPI.DEFAULT_OPTIONS).obj;

        Assert.assertEquals(parameter.resolutionX, options.getInt(ImageParameter.PARAM_RESOLUTION_X,0));
        Assert.assertEquals(parameter.resolutionY, options.getInt(ImageParameter.PARAM_RESOLUTION_Y,0));

        Assert.assertEquals(parameter.aaMin, options.getInt(ImageParameter.PARAM_AA_MIN,0));
        Assert.assertEquals(parameter.aaMax, options.getInt(ImageParameter.PARAM_AA_MAX,0));
        Assert.assertEquals(parameter.aaSamples, options.getInt(ImageParameter.PARAM_AA_SAMPLES,0));
        Assert.assertEquals(parameter.aaContrast, options.getFloat(ImageParameter.PARAM_AA_CONTRAST,0), 0);
        Assert.assertEquals(parameter.aaCache, options.getBoolean(ImageParameter.PARAM_AA_CACHE,false));
        Assert.assertEquals(parameter.aaJitter, options.getBoolean(ImageParameter.PARAM_AA_JITTER,false));

        Assert.assertEquals(parameter.filter, options.getString(ImageParameter.PARAM_FILTER,""));
        Assert.assertEquals(parameter.sampler, options.getString(ImageParameter.PARAM_SAMPLER,""));
    }

}
