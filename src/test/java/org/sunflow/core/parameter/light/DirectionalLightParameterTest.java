package org.sunflow.core.parameter.light;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.light.DirectionalSpotlight;
import org.sunflow.image.Color;
import org.sunflow.math.Point3;

public class DirectionalLightParameterTest {

    DirectionalLightParameter light;
    SunflowAPI api;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        light = new DirectionalLightParameter();
    }

    @Test
    public void testSetupAPI() {
        light.setName("uniqueName");
        light.setRadiance(new Color(1f,0,0));
        light.setRadius(2);
        light.setSource(new Point3(1,2,3));
        light.setDirection(new Point3(2,2,3));

        // Set parameters
        light.setup(api);

        DirectionalSpotlight l = (DirectionalSpotlight) api.getRenderObjects().get(light.name).obj;

        Assert.assertEquals(light.getSource(), l.getSource());
        Assert.assertEquals(light.getRadius(), l.getR(), 0);
        Assert.assertArrayEquals(light.getRadiance().getRGB(), l.getRadiance().getRGB(), 0);

        Assert.assertEquals(1, l.getDirection().x, 0);
        Assert.assertEquals(0, l.getDirection().y, 0);
        Assert.assertEquals(0, l.getDirection().z, 0);
    }

}
