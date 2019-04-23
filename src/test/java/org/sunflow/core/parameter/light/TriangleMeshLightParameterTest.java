package org.sunflow.core.parameter.light;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.light.TriangleMeshLight;
import org.sunflow.image.Color;

public class TriangleMeshLightParameterTest {

    TriangleMeshLightParameter light;
    SunflowAPI api;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        light = new TriangleMeshLightParameter();
    }

    @Test
    public void testSetupAPI() {
        light.setName("uniqueName");
        light.setSamples(77);
        light.setPoints(new float[]{0, 0, 0, 1, 1, 1, 1, 0, 0});
        light.setTriangles(new int[]{0, 2, 1});
        light.setRadiance(new Color(0, 1, 1));

        light.setup(api);

        TriangleMeshLight l = (TriangleMeshLight) api.getRenderObjects().get(light.name).obj;

        Assert.assertEquals(light.getSamples(), l.getNumSamples());
    }
}
