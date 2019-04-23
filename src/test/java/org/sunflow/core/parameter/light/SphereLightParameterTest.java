package org.sunflow.core.parameter.light;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.light.PointLight;
import org.sunflow.core.light.SphereLight;
import org.sunflow.image.Color;
import org.sunflow.math.Point3;

public class SphereLightParameterTest {

    SphereLightParameter light;
    SunflowAPI api;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        light = new SphereLightParameter();
    }

    @Test
    public void testSetupAPI() {
        light.setName("uniqueName");
        light.setSamples(77);
        light.setCenter(new Point3(0, 2, 1));
        light.setRadius(5);
        light.setRadiance(new Color(3,2,1));

        light.setup(api);

        SphereLight l = (SphereLight) api.getRenderObjects().get(light.name).obj;

        Assert.assertEquals(light.getSamples(), l.getNumSamples());
        Assert.assertEquals(light.getCenter(), l.getCenter());
        Assert.assertEquals(light.getRadius(), l.getRadius(), 0);
        Assert.assertArrayEquals(light.getRadiance().getRGB(), l.getRadiance().getRGB(), 0);
    }
}
