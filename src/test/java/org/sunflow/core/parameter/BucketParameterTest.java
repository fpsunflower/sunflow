package org.sunflow.core.parameter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.Options;

public class BucketParameterTest {

    SunflowAPI api;
    BucketParameter parameter;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        parameter = new BucketParameter();
    }

    @Test
    public void testSetupAPI() {
        // Set values
        parameter.setSize(10);
        parameter.setOrder(BucketParameter.ORDER_HILBERT);

        // Set parameters
        parameter.setup(api);

        Options options = (Options) api.getRenderObjects().get(SunflowAPI.DEFAULT_OPTIONS).obj;

        Assert.assertEquals(parameter.getSize(), options.getInt(BucketParameter.PARAM_BUCKET_SIZE,0));
        Assert.assertEquals(parameter.getOrder(), options.getString(BucketParameter.PARAM_BUCKET_ORDER,""));
    }

}
