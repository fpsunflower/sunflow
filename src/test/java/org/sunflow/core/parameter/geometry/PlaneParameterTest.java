package org.sunflow.core.parameter.geometry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.Geometry;
import org.sunflow.math.Point3;

public class PlaneParameterTest {

    SunflowAPI api;
    PlaneParameter parameter;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        parameter = new PlaneParameter();
    }

    @Test
    public void testSetupAPI() {
        // Set values
        parameter.setName("uniqueName");

        parameter.setCenter(new Point3(0,0,0));
        parameter.setPoint1(new Point3(0,0,0));
        parameter.setPoint2(new Point3(0,1,0));

        // Set parameters
        parameter.setup(api);

        Geometry geometry = (Geometry) api.getRenderObjects().get(parameter.getName()).obj;
        Assert.assertNotNull(geometry);
    }

}
