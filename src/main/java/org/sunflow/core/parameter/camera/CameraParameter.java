package org.sunflow.core.parameter.camera;

import org.sunflow.SunflowAPI;
import org.sunflow.SunflowAPIInterface;
import org.sunflow.core.parameter.Parameter;

public class CameraParameter implements Parameter {

    public static final String TYPE_FISH_EYE = "fisheye";
    public static final String TYPE_PINHOLE = "pinhole";
    public static final String TYPE_SPHERICAL = "spherical";
    public static final String TYPE_THINLENS = "thinlens";

    public static final String PARAM_FOV = "fov";
    public static final String PARAM_ASPECT = "aspect";
    public static final String PARAM_SHIFT_X = "shift.x";
    public static final String PARAM_SHIFT_Y = "shift.y";
    public static final String PARAM_SHUTTER_OPEN = "shutter.open";
    public static final String PARAM_SHUTTER_CLOSE = "shutter.close";
    public static final String PARAM_CAMERA = "camera";

    // Default values from Camera
    protected float shutterOpen = 0;
    protected float shutterClose = 0;

    protected String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getShutterOpen() {
        return shutterOpen;
    }

    public void setShutterOpen(float shutterOpen) {
        this.shutterOpen = shutterOpen;
    }

    public float getShutterClose() {
        return shutterClose;
    }

    public void setShutterClose(float shutterClose) {
        this.shutterClose = shutterClose;
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter(PARAM_CAMERA, name);
        api.options(SunflowAPI.DEFAULT_OPTIONS);
    }
}
