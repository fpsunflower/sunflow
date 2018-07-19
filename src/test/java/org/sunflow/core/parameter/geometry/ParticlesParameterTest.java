package org.sunflow.core.parameter.geometry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.Geometry;

public class ParticlesParameterTest {

    SunflowAPI api;
    ParticlesParameter parameter;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        parameter = new ParticlesParameter();
    }

    @Test
    public void testSetupAPI() {
        // Set values
        parameter.setName("uniqueName");

        // Set parameters
        parameter.setup(api);

        Geometry geometry = (Geometry) api.getRenderObjects().get(parameter.getName()).obj;
        Assert.assertNotNull(geometry);
    }

}
