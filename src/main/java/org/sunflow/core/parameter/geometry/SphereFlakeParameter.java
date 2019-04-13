package org.sunflow.core.parameter.geometry;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.math.Vector3;

public class SphereFlakeParameter extends GeometryParameter {

    int level = 2;
    float radius = 1;
    Vector3 axis;

    public SphereFlakeParameter() {
        // Default values from SphereFlake
        level = 2;
        radius = 1;
        axis = new Vector3(0, 0, 1);
    }

    public SphereFlakeParameter(String name) {
        this();
        this.name = name;
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        super.setup(api);

        api.parameter("level", level);

        if (axis != null) {
            api.parameter("axis", axis);
        }

        if (radius > 0) {
            api.parameter("radius", radius);
        }

        api.geometry(name, TYPE_SPHEREFLAKE);

        setupInstance(api);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public Vector3 getAxis() {
        return axis;
    }

    public void setAxis(Vector3 axis) {
        this.axis = axis;
    }
}
