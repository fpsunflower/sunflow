package org.sunflow.core.parameter.gi;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.image.Color;

public class AmbientOcclusionGIParameterTest {

    SunflowAPI api;
    AmbientOcclusionGIParameter gi;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        gi = new AmbientOcclusionGIParameter();
    }

    @Test
    public void testSetupAPI() {
        // Set values
        gi.dark = new Color(0,1,0);
        gi.bright = new Color(1,0,1);
        gi.maxDist = 2.222f;
        gi.samples = 99;

        // Set parameters
        gi.setup(api);

        Assert.assertEquals(GlobalIlluminationParameter.TYPE_AMBOCC, api.getParameterList().getString(GlobalIlluminationParameter.PARAM_ENGINE, ""));
        Assert.assertArrayEquals(gi.bright.getRGB(), api.getParameterList().getColor(AmbientOcclusionGIParameter.PARAM_BRIGHT, null).getRGB(), 0);
        Assert.assertArrayEquals(gi.dark.getRGB(), api.getParameterList().getColor(AmbientOcclusionGIParameter.PARAM_DARK, null).getRGB(), 0);
        Assert.assertEquals(gi.samples, api.getParameterList().getInt(AmbientOcclusionGIParameter.PARAM_SAMPLES,0));
        Assert.assertEquals(gi.maxDist, api.getParameterList().getFloat(AmbientOcclusionGIParameter.PARAM_MAXDIST,0), 0);
    }

}
