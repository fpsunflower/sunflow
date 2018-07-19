package org.sunflow.core.parameter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;

public class TraceDepthsParameterTest {

    SunflowAPI api;
    TraceDepthsParameter parameter;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        parameter = new TraceDepthsParameter();
    }

    @Test
    public void testSetupAPI() {
        // Set values
        parameter.diffuse = 1;
        parameter.reflection = 2;
        parameter.refraction = 3;

        // Set parameters
        parameter.setup(api);
        Assert.assertEquals(parameter.diffuse, api.getParameterList().getInt(TraceDepthsParameter.PARAM_DEPTHS_DIFFUSE,0));
        Assert.assertEquals(parameter.reflection, api.getParameterList().getInt(TraceDepthsParameter.PARAM_DEPTHS_REFLECTION,0));
        Assert.assertEquals(parameter.refraction, api.getParameterList().getInt(TraceDepthsParameter.PARAM_DEPTHS_REFRACTION,0));
    }

}
