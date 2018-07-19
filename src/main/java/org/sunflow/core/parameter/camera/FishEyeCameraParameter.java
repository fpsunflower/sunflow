package org.sunflow.core.parameter.camera;

import org.sunflow.SunflowAPIInterface;

public class FishEyeCameraParameter extends CameraParameter {

    // FisheyeLens lens;

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter(PARAM_SHUTTER_OPEN, shutterOpen);
        api.parameter(PARAM_SHUTTER_CLOSE, shutterClose);
        api.camera(name, TYPE_FISH_EYE);
        super.setup(api);
    }

}
