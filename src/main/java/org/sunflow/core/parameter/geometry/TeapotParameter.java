package org.sunflow.core.parameter.geometry;

import org.sunflow.SunflowAPIInterface;

public class TeapotParameter extends GeometryParameter {

    int subdivs = 1;
    boolean smooth = false;

    @Override
    public void setup(SunflowAPIInterface api) {
        super.setup(api);
        api.parameter("subdivs", subdivs);
        api.parameter("smooth", smooth);
        api.geometry(name, TYPE_TEAPOT);

        setupInstance(api);
    }

    public int getSubdivs() {
        return subdivs;
    }

    public void setSubdivs(int subdivs) {
        this.subdivs = subdivs;
    }

    public boolean isSmooth() {
        return smooth;
    }

    public void setSmooth(boolean smooth) {
        this.smooth = smooth;
    }
}
