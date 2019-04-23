package org.sunflow.core.parameter.geometry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.Geometry;
import org.sunflow.math.Point3;

public class BoxParameterTest {

    SunflowAPI api;
    BoxParameter parameter;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        parameter = new BoxParameter();
    }

    @Test
    public void testSetupAPI() {
        // Set values
        parameter.setName("uniqueName");

        parameter.setMin(new Point3(-1,-1,-1));
        parameter.setMax(new Point3(1,1,1));

        // Set parameters
        parameter.setup(api);

        Geometry geometry = (Geometry) api.getRenderObjects().get(parameter.getName()).obj;
        Assert.assertNotNull(geometry);
    }

}
