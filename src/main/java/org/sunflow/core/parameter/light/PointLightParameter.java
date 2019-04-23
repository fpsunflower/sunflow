package org.sunflow.core.parameter.light;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.core.light.PointLight;
import org.sunflow.image.Color;
import org.sunflow.math.Point3;

public class PointLightParameter extends LightParameter {

    public static final String PARAM_CENTER = "center";
    public static final String PARAM_POWER = "power";
    private PointLight light;

    public PointLightParameter() {
        light = new PointLight();
        generateUniqueName("pointlight");
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter(PARAM_CENTER, getCenter());
        api.parameter(PARAM_POWER, null, getColor().getRGB());
        api.light(name, LightParameter.TYPE_POINTLIGHT);
    }

    public Point3 getCenter() {
        return light.getLightPoint();
    }

    public void setCenter(Point3 lightPoint) {
        light.setLightPoint(lightPoint);
    }

    public Color getColor() {
        return light.getColor();
    }

    public void setColor(Color color) {
        light.setColor(color);
    }

}
