package org.sunflow.core.parameter;

import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class InstanceParameterTest {
    SunflowAPI api;
    InstanceParameter parameter;

    @Before
    public void setUp() {
        api = mock(SunflowAPI.class);
        api.reset();

        parameter = new InstanceParameter();
    }

    @Test
    public void testSetupAPI() {
        // Set values
        parameter.setName("uniqueName");

        parameter.setup(api);

        verify(api, times(1)).instance(anyString(), anyString());
    }
}
