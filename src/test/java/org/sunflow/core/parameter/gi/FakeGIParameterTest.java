package org.sunflow.core.parameter.gi;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
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

        Assert.assertEquals(GlobalIlluminationParameter.TYPE_FAKE, api.getParameterList().getString(GlobalIlluminationParameter.PARAM_ENGINE, ""));
        Assert.assertArrayEquals(gi.ground.getRGB(), api.getParameterList().getColor(FakeGIParameter.PARAM_GROUND, null).getRGB(), 0);
        Assert.assertArrayEquals(gi.sky.getRGB(), api.getParameterList().getColor(FakeGIParameter.PARAM_SKY, null).getRGB(), 0);
        Assert.assertEquals(gi.up, api.getParameterList().getVector(FakeGIParameter.PARAM_UP,null));
    }

}
