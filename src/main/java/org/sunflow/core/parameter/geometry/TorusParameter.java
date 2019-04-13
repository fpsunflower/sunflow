package org.sunflow.core.parameter.geometry;

import org.sunflow.SunflowAPIInterface;

public class TorusParameter extends GeometryParameter {

    float radiusInner;
    float radiusOuter;

    @Override
    public void setup(SunflowAPIInterface api) {
        super.setup(api);
        api.parameter("radiusInner", radiusInner);
        api.parameter("radiusOuter", radiusOuter);
        api.geometry(name, TYPE_TORUS);

        setupInstance(api);
    }

    public float getRadiusInner() {
        return radiusInner;
    }

    public void setRadiusInner(float radiusInner) {
        this.radiusInner = radiusInner;
    }

    public float getRadiusOuter() {
        return radiusOuter;
    }

    public void setRadiusOuter(float radiusOuter) {
        this.radiusOuter = radiusOuter;
    }
}
