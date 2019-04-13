package org.sunflow.core.parameter.geometry;

import org.sunflow.SunflowAPIInterface;

public class HairParameter extends GeometryParameter {

    int segments;
    float width;
    float[] points;

    @Override
    public void setup(SunflowAPIInterface api) {
        super.setup(api);
        api.parameter("segments", segments);
        api.parameter("widths", width);
        api.parameter("points", "point", "vertex", points);
        api.geometry(name, TYPE_HAIR);
        setupInstance(api);
    }

    public int getSegments() {
        return segments;
    }

    public void setSegments(int segments) {
        this.segments = segments;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float[] getPoints() {
        return points;
    }

    public void setPoints(float[] points) {
        this.points = points;
    }
}
