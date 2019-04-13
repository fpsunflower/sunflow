package org.sunflow.core.parameter.light;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.image.Color;
import org.sunflow.math.Vector3;

public class SunSkyLightParameter extends LightParameter {

    public static final String PARAM_TURBIDITY = "turbidity";
    public static final String PARAM_SUN_DIRECTION = "sundir";
    public static final String PARAM_EAST = "east";
    public static final String PARAM_UP = "up";
    public static final String PARAM_GROUND_EXTENDSKY = "ground.extendsky";
    public static final String PARAM_GROUND_COLOR = "ground.color";
    Vector3 up;
    Vector3 east;
    Vector3 sunDirection;

    float turbidity;
    int samples;
    boolean extendSky = false;

    Color groundColor = null;

    public SunSkyLightParameter() {
        generateUniqueName("sunsky");
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter(PARAM_UP, up);
        api.parameter(PARAM_EAST, east);
        api.parameter(PARAM_SUN_DIRECTION, sunDirection);
        api.parameter(PARAM_TURBIDITY, turbidity);
        api.parameter(LightParameter.PARAM_SAMPLES, samples);
        api.parameter(PARAM_GROUND_EXTENDSKY, extendSky);

        if (groundColor != null) {
            api.parameter(PARAM_GROUND_COLOR, null, groundColor.getRGB());
        }

        api.light(name, TYPE_SUNSKY);
    }

    public Vector3 getUp() {
        return up;
    }

    public void setUp(Vector3 up) {
        this.up = up;
    }

    public Vector3 getEast() {
        return east;
    }

    public void setEast(Vector3 east) {
        this.east = east;
    }

    public Vector3 getSunDirection() {
        return sunDirection;
    }

    public void setSunDirection(Vector3 sunDirection) {
        this.sunDirection = sunDirection;
    }

    public float getTurbidity() {
        return turbidity;
    }

    public void setTurbidity(float turbidity) {
        this.turbidity = turbidity;
    }

    public int getSamples() {
        return samples;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }

    public boolean isExtendSky() {
        return extendSky;
    }

    public void setExtendSky(boolean extendSky) {
        this.extendSky = extendSky;
    }

    public Color getGroundColor() {
        return groundColor;
    }

    public void setGroundColor(Color groundColor) {
        this.groundColor = groundColor;
    }
}
