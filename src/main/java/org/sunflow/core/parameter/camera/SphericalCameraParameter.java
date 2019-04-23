package org.sunflow.core.parameter.camera;

import org.sunflow.SunflowAPIInterface;

public class SphericalCameraParameter extends CameraParameter {

    // SphericalLens lens;

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter(PARAM_SHUTTER_OPEN, shutterOpen);
        api.parameter(PARAM_SHUTTER_CLOSE, shutterClose);
        api.camera(name, TYPE_SPHERICAL);
        super.setup(api);
    }

}
