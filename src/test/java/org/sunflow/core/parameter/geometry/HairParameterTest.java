package org.sunflow.core.parameter.geometry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.Geometry;

public class HairParameterTest {

    SunflowAPI api;
    HairParameter parameter;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        parameter = new HairParameter();
    }

    @Test
    public void testSetupAPI() {
        parameter.setWidth(1f);
        parameter.setSegments(3);
        parameter.setPoints(new float[]{0, 0, 0, 1, 1, 1, 2, 2, 2});

        // Set values
        parameter.setName("uniqueName");

        // Set parameters
        parameter.setup(api);

        Geometry geometry = (Geometry) api.getRenderObjects().get(parameter.getName()).obj;
        Assert.assertNotNull(geometry);
    }

}
