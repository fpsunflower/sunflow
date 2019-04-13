package org.sunflow.core.parameter.geometry;

import org.sunflow.SunflowAPIInterface;

public class ParticlesParameter extends GeometryParameter {

    int num;
    float radius;
    float[] points;

    @Override
    public void setup(SunflowAPIInterface api) {
        super.setup(api);
        api.parameter("particles", "point", "vertex", points);
        api.parameter("num", num);
        api.parameter("radius", radius);
        api.geometry(name, TYPE_PARTICLES);

        setupInstance(api);
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public float[] getPoints() {
        return points;
    }

    public void setPoints(float[] points) {
        this.points = points;
    }
}
