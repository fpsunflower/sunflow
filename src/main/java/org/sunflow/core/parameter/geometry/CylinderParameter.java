package org.sunflow.core.parameter.geometry;

import org.sunflow.SunflowAPIInterface;

public class CylinderParameter extends GeometryParameter {

    @Override
    public void setup(SunflowAPIInterface api) {
        super.setup(api);
        api.geometry(name, TYPE_CYLINDER);

        setupInstance(api);
    }
}
