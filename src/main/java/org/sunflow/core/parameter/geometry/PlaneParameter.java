package org.sunflow.core.parameter.geometry;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class PlaneParameter extends GeometryParameter {

    Point3 center;
    Point3 point1;
    Point3 point2;
    Vector3 normal;

    @Override
    public void setup(SunflowAPIInterface api) {
        super.setup(api);
        api.parameter("center", center);
        if (normal != null) {
            api.parameter("normal", normal);
        } else {
            api.parameter("point1", point1);
            api.parameter("point2", point2);
        }
        api.geometry(name, TYPE_PLANE);

        setupInstance(api);
    }

    public Point3 getCenter() {
        return center;
    }

    public void setCenter(Point3 center) {
        this.center = center;
    }

    public Point3 getPoint1() {
        return point1;
    }

    public void setPoint1(Point3 point1) {
        this.point1 = point1;
    }

    public Point3 getPoint2() {
        return point2;
    }

    public void setPoint2(Point3 point2) {
        this.point2 = point2;
    }

    public Vector3 getNormal() {
        return normal;
    }

    public void setNormal(Vector3 normal) {
        this.normal = normal;
    }
}
