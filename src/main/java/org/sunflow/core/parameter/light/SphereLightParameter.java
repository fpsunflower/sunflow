package org.sunflow.core.parameter.light;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.core.light.SphereLight;
import org.sunflow.image.Color;
import org.sunflow.math.Point3;

public class SphereLightParameter extends LightParameter {

    public static final String PARAM_CENTER = "center";
    public static final String PARAM_RADIUS = "radius";
    private SphereLight light;

    public SphereLightParameter() {
        light = new SphereLight();
        generateUniqueName("spherelight");
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter(LightParameter.PARAM_RADIANCE, null, light.getRadiance().getRGB());
        api.parameter(PARAM_CENTER, light.getCenter());
        api.parameter(PARAM_RADIUS, light.getRadius());
        api.parameter(LightParameter.PARAM_SAMPLES, light.getNumSamples());
        api.light(name, LightParameter.TYPE_SPHERE);
    }

    public Color getRadiance() {
        return light.getRadiance();
    }

    public void setRadiance(Color radiance) {
        light.setRadiance(radiance);
    }

    public int getSamples() {
        return light.getNumSamples();
    }

    public void setSamples(int numSamples) {
        light.setNumSamples(numSamples);
    }

    public Point3 getCenter() {
        return light.getCenter();
    }

    public void setCenter(Point3 center) {
        light.setCenter(center);
    }

    public float getRadius() {
        return light.getRadius();
    }

    public void setRadius(float radius) {
        light.setRadius(radius);
    }

    public float getR2() {
        return light.getR2();
    }

    public void setR2(float r2) {
        light.setR2(r2);
    }

}
