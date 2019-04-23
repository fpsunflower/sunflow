package org.sunflow;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SunflowAPITest {

    SunflowAPI api;

    @Before
    public void setUp() {
        api = new SunflowAPI();
    }


    @Test
    public void testReset() {
        Assert.assertNotNull(api.scene);
        Assert.assertNotNull(api.includeSearchPath);
        Assert.assertNotNull(api.textureSearchPath);
        Assert.assertNotNull(api.parameterList);
        Assert.assertNotNull(api.renderObjects);
        Assert.assertEquals(1, api.currentFrame);
    }

    @Test
    public void testParameters() {
        api.parameter("hello", "world");
        Assert.assertEquals("world", api.parameterList.getString("hello", ""));
    }

}
