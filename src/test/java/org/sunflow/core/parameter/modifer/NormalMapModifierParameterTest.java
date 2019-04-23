package org.sunflow.core.parameter.modifer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.modifiers.BumpMappingModifier;
import org.sunflow.core.modifiers.NormalMapModifier;
import org.sunflow.core.parameter.modifier.BumpMapModifierParameter;
import org.sunflow.core.parameter.modifier.NormalMapModifierParameter;

public class NormalMapModifierParameterTest {

    SunflowAPI api;
    NormalMapModifierParameter parameter;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        parameter = new NormalMapModifierParameter();
    }

    @Test
    public void testSetupAPI() {
        // Set values
        parameter.setName("uniqueName");

        // Set parameters
        parameter.setup(api);

        NormalMapModifier modifier = (NormalMapModifier) api.getRenderObjects().get(parameter.getName()).obj;

        Assert.assertNotNull(modifier);
    }

}
