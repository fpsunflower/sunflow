package org.sunflow.core.parameter.light;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.light.ImageBasedLight;
import org.sunflow.core.light.PointLight;
import org.sunflow.image.Color;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class PointLightParameterTest {

    PointLightParameter light;
    SunflowAPI api;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        light = new PointLightParameter();
    }

    @Test
    public void testSetupAPI() {
        light.setName("uniqueName");
        light.setColor(new Color(1,5,6));
        light.setCenter(new Point3(0, 2, 1));

        light.setup(api);

        PointLight l = (PointLight) api.getRenderObjects().get(light.name).obj;

        Assert.assertEquals(light.getCenter(), l.getLightPoint());
        Assert.assertArrayEquals(light.getColor().getRGB(), l.getColor().getRGB(), 0);
    }
}
