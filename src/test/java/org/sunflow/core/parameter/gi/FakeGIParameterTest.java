package org.sunflow.core.parameter.gi;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.Options;
import org.sunflow.image.Color;
import org.sunflow.math.Vector3;

public class FakeGIParameterTest {

    SunflowAPI api;
    FakeGIParameter gi;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        gi = new FakeGIParameter();
    }

    @Test
    public void testSetupAPI() {
        // Set values
        gi.ground = new Color(0,1,0);
        gi.sky = new Color(1,0,1);
        gi.up = new Vector3(1,2,4);

        // Set parameters
        gi.setup(api);

        Options options = (Options) api.getRenderObjects().get(SunflowAPI.DEFAULT_OPTIONS).obj;

        Assert.assertEquals(GlobalIlluminationParameter.TYPE_FAKE, options.getString(GlobalIlluminationParameter.PARAM_ENGINE, ""));
        Assert.assertArrayEquals(gi.ground.getRGB(), options.getColor(FakeGIParameter.PARAM_GROUND, null).getRGB(), 0);
        Assert.assertArrayEquals(gi.sky.getRGB(), options.getColor(FakeGIParameter.PARAM_SKY, null).getRGB(), 0);
        Assert.assertEquals(gi.up, options.getVector(FakeGIParameter.PARAM_UP,null));
    }

}
