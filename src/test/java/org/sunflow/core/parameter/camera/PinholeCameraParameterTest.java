package org.sunflow.core.parameter.camera;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.Camera;

public class PinholeCameraParameterTest {

    SunflowAPI api;
    PinholeCameraParameter camera;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        camera = new PinholeCameraParameter();
    }

    @Test
    public void testSetupAPI() {
        camera.name = "uniqueName";
        camera.shutterOpen = 1.2f;
        camera.shutterClose = 3.2f;

        camera.setup(api);

        Camera apiCamera = (Camera) api.getRenderObjects().get(camera.name).obj;
        Assert.assertEquals(camera.shutterOpen, apiCamera.getShutterOpen(), 0);
        Assert.assertEquals(camera.shutterClose, apiCamera.getShutterClose(), 0);
    }

}
