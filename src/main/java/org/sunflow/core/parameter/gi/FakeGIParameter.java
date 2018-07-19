package org.sunflow.core.parameter.gi;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.image.Color;
import org.sunflow.math.Vector3;

public class FakeGIParameter extends GlobalIlluminationParameter {

    public static final String PARAM_SKY = "gi.fake.sky";
    public static final String PARAM_GROUND = "gi.fake.ground";
    public static final String PARAM_UP = "gi.fake.up";
    Color ground;
    Color sky;
    Vector3 up;

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter(GlobalIlluminationParameter.PARAM_ENGINE, GlobalIlluminationParameter.TYPE_FAKE);
        api.parameter(PARAM_SKY, null, sky.getRGB());
        api.parameter(PARAM_GROUND, null, ground.getRGB());
        api.parameter(PARAM_UP, up);
        super.setup(api);
    }

    public Color getGround() {
        return ground;
    }

    public void setGround(Color ground) {
        this.ground = ground;
    }

    public Color getSky() {
        return sky;
    }

    public void setSky(Color sky) {
        this.sky = sky;
    }

    public Vector3 getUp() {
        return up;
    }

    public void setUp(Vector3 up) {
        this.up = up;
    }
}
