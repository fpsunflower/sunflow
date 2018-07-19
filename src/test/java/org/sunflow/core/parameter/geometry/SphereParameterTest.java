package org.sunflow.core.parameter.geometry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.Geometry;
import org.sunflow.math.Point3;

public class SphereParameterTest {

    SunflowAPI api;
    SphereParameter parameter;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        parameter = new SphereParameter();
    }

    @Test
    public void testSetupAPI() {
        // Set values
        parameter.setName("uniqueName");
        parameter.setCenter(new Point3(0,0,0));
        parameter.setRadius(1);

        // Set parameters
        parameter.setup(api);

        Geometry geometry = (Geometry) api.getRenderObjects().get(parameter.getName()).obj;
        Assert.assertNotNull(geometry);
    }

}
