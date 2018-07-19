package org.sunflow.core.parameter.light;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.core.light.DirectionalSpotlight;
import org.sunflow.image.Color;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class DirectionalLightParameter extends LightParameter {

    public static final String PARAM_SOURCE = "source";
    public static final String PARAM_DIRECTION = "dir";
    public static final String PARAM_RADIUS = "radius";

    DirectionalSpotlight light;

    public DirectionalLightParameter() {
        light = new DirectionalSpotlight();
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter(PARAM_SOURCE, light.getSource());
        api.parameter(PARAM_DIRECTION, light.getDirection());
        api.parameter(PARAM_RADIUS, light.getR());
        api.parameter(PARAM_RADIANCE, null, light.getRadiance().getRGB());
        api.light(name, LightParameter.TYPE_DIRECTIONAL);
    }

    public Point3 getSource() {
        return light.getSource();
    }

    public void setSource(Point3 source) {
        light.setSource(source);
    }

    public Vector3 getDirection() {
        return light.getDirection();
    }

    public void setDirection(Point3 target) {
        Vector3 direction = Point3.sub(target, light.getSource(), new Vector3());
        light.setDirection(direction);
    }

    public void setDirection(Vector3 direction) {
        light.setDirection(direction);
    }

    public float getRadius() {
        return light.getR();
    }

    public void setRadius(float r) {
        light.setR(r);
    }

    public Color getRadiance() {
        return light.getRadiance();
    }

    public void setRadiance(Color radiance) {
        light.setRadiance(radiance);
    }

}
