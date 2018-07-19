package org.sunflow.core.parameter.geometry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.Geometry;

public class GenericMeshParameterTest {

    SunflowAPI api;
    GenericMeshParameter parameter;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        parameter = new GenericMeshParameter();
    }

    @Test
    public void testSetupAPI() {
        // Set values
        parameter.setName("uniqueName");
        parameter.setPoints(new float[]{0,0,0});
        parameter.setTriangles(new int[]{0});

        // Set parameters
        parameter.setup(api);

        Geometry geometry = (Geometry) api.getRenderObjects().get(parameter.getName()).obj;
        Assert.assertNotNull(geometry);
    }

}
