package org.sunflow.core.parameter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.Instance;
import org.sunflow.core.shader.ConstantShader;
import org.sunflow.image.Color;

public class BackgroundParameterTest {

    SunflowAPI api;
    BackgroundParameter parameter;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        parameter = new BackgroundParameter();
    }

    @Test
    public void testSetupAPI() {
        parameter.setColor(new Color(1f,0,0));

        // Set parameters
        parameter.setup(api);

        Assert.assertTrue(api.getRenderObjects().has(BackgroundParameter.PARAM_BACKGROUND_INSTANCE));
        Assert.assertTrue(api.getRenderObjects().has(BackgroundParameter.PARAM_BACKGROUND_SHADER));
        Assert.assertTrue(api.getRenderObjects().has(BackgroundParameter.PARAM_BACKGROUND));

        Instance instance = (Instance) api.getRenderObjects().
                get(BackgroundParameter.PARAM_BACKGROUND_INSTANCE).obj;
        ConstantShader constantShader = (ConstantShader) instance.getShader(0);
        Color color = constantShader.getRadiance(null);

        Assert.assertArrayEquals(parameter.color.getRGB(), color.getRGB(), 0);
    }

}
