package org.sunflow.core.parameter.camera;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.core.camera.PinholeLens;

public class PinholeCameraParameter extends CameraParameter {

    private PinholeLens lens;

    public PinholeCameraParameter() {
        lens = new PinholeLens();
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter(PARAM_SHUTTER_OPEN, shutterOpen);
        api.parameter(PARAM_SHUTTER_CLOSE, shutterClose);
        api.parameter(PARAM_FOV, lens.getFov());
        api.parameter(PARAM_ASPECT, lens.getAspect());
        api.parameter(PARAM_SHIFT_X, lens.getShiftX());
        api.parameter(PARAM_SHIFT_Y, lens.getShiftY());

        api.camera(name, TYPE_PINHOLE);
        super.setup(api);
    }

    public float getFov() {
        return lens.getFov();
    }

    public void setFov(float fov) {
        lens.setFov(fov);
    }

    public float getAspect() {
        return lens.getAspect();
    }

    public void setAspect(float aspect) {
        lens.setAspect(aspect);
    }

    public float getShiftX() {
        return lens.getShiftX();
    }

    public void setShiftX(float shiftX) {
        lens.setShiftX(shiftX);
    }

    public float getShiftY() {
        return lens.getShiftY();
    }

    public void setShiftY(float shiftY) {
        lens.setShiftY(shiftY);
    }

}
