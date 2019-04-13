package org.sunflow.core.parameter.geometry;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;

public class SphereParameter extends GeometryParameter {

    Point3 center;
    float radius;

    @Override
    public void setup(SunflowAPIInterface api) {
        super.setup(api);
        api.geometry(name, TYPE_SPHERE);
        api.parameter("transform", Matrix4.translation(center.x, center.y, center.z).multiply(Matrix4.scale(radius)));

        setupInstance(api);
    }

    public Point3 getCenter() {
        return center;
    }

    public void setCenter(Point3 center) {
        this.center = center;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }
}
