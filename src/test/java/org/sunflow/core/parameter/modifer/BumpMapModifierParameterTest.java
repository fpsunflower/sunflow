package org.sunflow.core.parameter.modifer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.modifiers.BumpMappingModifier;
import org.sunflow.core.modifiers.PerlinModifier;
import org.sunflow.core.parameter.modifier.BumpMapModifierParameter;
import org.sunflow.core.parameter.modifier.PerlinModifierParameter;

public class BumpMapModifierParameterTest {

    SunflowAPI api;
    BumpMapModifierParameter parameter;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        parameter = new BumpMapModifierParameter();
    }

    @Test
    public void testSetupAPI() {
        // Set values
        parameter.setName("uniqueName");
        parameter.setScale(2.12f);

        // Set parameters
        parameter.setup(api);

        BumpMappingModifier modifier = (BumpMappingModifier) api.getRenderObjects().get(parameter.getName()).obj;

        Assert.assertEquals(parameter.getScale(), modifier.getScale(),0);
    }

}
