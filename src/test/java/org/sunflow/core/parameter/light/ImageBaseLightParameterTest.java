package org.sunflow.core.parameter.light;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.light.ImageBasedLight;
import org.sunflow.math.Vector3;

public class ImageBaseLightParameterTest {

    ImageBasedLightParameter light;
    SunflowAPI api;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        light = new ImageBasedLightParameter();
    }

    @Test
    public void testSetupAPI() {
        light.setName("uniqueName");
        light.setSamples(88);
        light.setCenter(new Vector3(0, 2, 1));
        light.setUp(new Vector3(0, -1, 0));
        light.setFixed(true);

        light.setup(api);

        ImageBasedLight l = (ImageBasedLight) api.getRenderObjects().get(light.name).obj;

        Assert.assertEquals(light.getSamples(), l.getNumSamples());
    }
}
