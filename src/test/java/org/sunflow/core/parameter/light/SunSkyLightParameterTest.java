package org.sunflow.core.parameter.light;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.light.SunSkyLight;
import org.sunflow.image.Color;
import org.sunflow.math.Vector3;

public class SunSkyLightParameterTest {

    SunSkyLightParameter light;
    SunflowAPI api;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        light = new SunSkyLightParameter();
    }

    @Test
    public void testSetupAPI() {
        light.setName("uniqueName");
        light.setSamples(77);
        light.setGroundColor(new Color(1, 3, 2));
        light.setExtendSky(true);
        light.setTurbidity(99);
        light.setEast(new Vector3(1, 0, 0));
        light.setSunDirection(new Vector3(1, 1, 0));
        light.setUp(new Vector3(0, 1, 0));

        light.setup(api);

        SunSkyLight l = (SunSkyLight) api.getRenderObjects().get(light.name).obj;

        // Expected result is samples + 1
        Assert.assertEquals(light.getSamples() + 1, l.getNumSamples());
    }
}
