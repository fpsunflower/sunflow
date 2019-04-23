package org.sunflow.core.parameter.geometry;

import org.sunflow.SunflowAPIInterface;

public class TeapotParameter extends GeometryParameter {

    int subdivs = 1;
    boolean smooth = false;

    public TeapotParameter() {

    }

    public TeapotParameter(String name) {
        this.name = name;
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        super.setup(api);
        api.parameter("subdivs", subdivs);
        api.parameter("smooth", smooth);

        if (instanceParameter == null || instanceParameter.geometry() == null) {
            api.geometry(name, TYPE_TEAPOT);
        }

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
