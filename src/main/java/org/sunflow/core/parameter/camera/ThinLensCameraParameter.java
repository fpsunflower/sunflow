package org.sunflow.core.parameter.camera;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.core.camera.ThinLens;

public class ThinLensCameraParameter extends CameraParameter {

    public static final String PARAM_FOCUS_DISTANCE = "focus.distance";
    public static final String PARAM_LENS_RADIUS = "lens.radius";
    public static final String PARAM_LENS_SIDES = "lens.sides";
    public static final String PARAM_LENS_ROTATION = "lens.rotation";

    private ThinLens lens;

    public ThinLensCameraParameter() {
        lens = new ThinLens();
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter(PARAM_SHUTTER_OPEN, shutterOpen);
        api.parameter(PARAM_SHUTTER_CLOSE, shutterClose);
        api.parameter(PARAM_FOV, lens.getFov());
        api.parameter(PARAM_ASPECT, lens.getAspect());
        api.parameter(PARAM_SHIFT_X, lens.getShiftX());
        api.parameter(PARAM_SHIFT_Y, lens.getShiftY());
        api.parameter(PARAM_FOCUS_DISTANCE, lens.getFocusDistance());
        api.parameter(PARAM_LENS_RADIUS, lens.getLensRadius());
        api.parameter(PARAM_LENS_SIDES, lens.getLensSides());
        api.parameter(PARAM_LENS_ROTATION, lens.getLensRotation());
        api.camera(name, TYPE_THINLENS);
        super.setup(api);
    }

    public float getAspect() {
        return lens.getAspect();
    }

    public void setAspect(float aspect) {
        lens.setAspect(aspect);
    }

    public float getFov() {
        return lens.getFov();
    }

    public void setFov(float fov) {
        lens.setFov(fov);
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

    public float getFocusDistance() {
        return lens.getFocusDistance();
    }

    public void setFocusDistance(float focusDistance) {
        lens.setFocusDistance(focusDistance);
    }

    public float getLensRadius() {
        return lens.getLensRadius();
    }

    public void setLensRadius(float lensRadius) {
        lens.setLensRadius(lensRadius);
    }

    public int getLensSides() {
        return lens.getLensSides();
    }

    public void setLensSides(int lensSides) {
        lens.setLensSides(lensSides);
    }

    public float getLensRotation() {
        return lens.getLensRotation();
    }

    public void setLensRotation(float lensRotation) {
        lens.setLensRotation(lensRotation);
    }

    public float getLensRotationRadians() {
        return lens.getLensRotationRadians();
    }

    public void setLensRotationRadians(float lensRotationRadians) {
        lens.setLensRotationRadians(lensRotationRadians);
    }

}
