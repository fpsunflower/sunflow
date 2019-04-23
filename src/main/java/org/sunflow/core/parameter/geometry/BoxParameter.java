package org.sunflow.core.parameter.geometry;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class BoxParameter extends GeometryParameter {

    Point3 min;
    Point3 max;

    public BoxParameter() {

    }

    public BoxParameter(String name) {
        super(name);
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        super.setup(api);
        api.parameter("min", min);
        api.parameter("max", max);

        api.geometry(name, TYPE_BOX);

        setupInstance(api);
    }

    public Point3 getMin() {
        return min;
    }

    public void setMin(Point3 min) {
        this.min = min;
    }

    public Point3 getMax() {
        return max;
    }

    public void setMax(Point3 max) {
        this.max = max;
    }
}
