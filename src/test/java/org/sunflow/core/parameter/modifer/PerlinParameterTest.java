package org.sunflow.core.parameter.modifer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.Modifier;
import org.sunflow.core.Options;
import org.sunflow.core.modifiers.PerlinModifier;
import org.sunflow.core.parameter.PhotonParameter;
import org.sunflow.core.parameter.modifier.PerlinModifierParameter;

public class PerlinParameterTest {

    SunflowAPI api;
    PerlinModifierParameter parameter;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        parameter = new PerlinModifierParameter();
    }

    @Test
    public void testSetupAPI() {
        // Set values
        parameter.setName("uniqueName");
        parameter.setFunction(99);
        parameter.setScale(2.12f);
        parameter.setSize(1.543f);

        // Set parameters
        parameter.setup(api);

        PerlinModifier modifier = (PerlinModifier) api.getRenderObjects().get(parameter.getName()).obj;

        Assert.assertEquals(parameter.getFunction(), modifier.getFunction());
        Assert.assertEquals(parameter.getScale(), modifier.getScale(),0);
        Assert.assertEquals(parameter.getSize(), modifier.getSize(),0);
    }

}
