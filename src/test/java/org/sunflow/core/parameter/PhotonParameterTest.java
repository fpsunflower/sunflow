package org.sunflow.core.parameter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.Options;

public class PhotonParameterTest {

    SunflowAPI api;
    PhotonParameter parameter;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        parameter = new PhotonParameter();
    }

    @Test
    public void testSetupAPI() {
        // Set values
        parameter.caustics.emit = 10;
        parameter.caustics.map = "default_value";
        parameter.caustics.gather = 2;
        parameter.caustics.radius = 1.2f;

        // Set parameters
        parameter.setup(api);

        Options options = (Options) api.getRenderObjects().get(SunflowAPI.DEFAULT_OPTIONS).obj;

        Assert.assertEquals(parameter.caustics.map, options.getString(PhotonParameter.PARAM_CAUSTICS,""));
        Assert.assertEquals(parameter.caustics.emit, options.getInt(PhotonParameter.PARAM_CAUSTICS_EMIT,0));
        Assert.assertEquals(parameter.caustics.gather, options.getInt(PhotonParameter.PARAM_CAUSTICS_GATHER,0));
        Assert.assertEquals(parameter.caustics.radius, options.getFloat(PhotonParameter.PARAM_CAUSTICS_RADIUS,0), 0);
    }

}
