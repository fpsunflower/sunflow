package org.sunflow.core.parameter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.Options;

public class OverrideParameterTest {

    SunflowAPI api;
    OverrideParameter parameter;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        parameter = new OverrideParameter();
    }

    @Test
    public void testSetupAPI() {
        // Set values
        parameter.shader = "shader";
        parameter.photons = true;

        // Set parameters
        parameter.setup(api);

        Options options = (Options) api.getRenderObjects().get(SunflowAPI.DEFAULT_OPTIONS).obj;

        Assert.assertEquals(parameter.shader, options.getString(OverrideParameter.PARAM_OVERRIDE_SHADER,""));
        Assert.assertEquals(parameter.photons, options.getBoolean(OverrideParameter.PARAM_OVERRIDE_PHOTONS,false));
    }

}
