package org.sunflow.core.parameter.geometry;

import org.sunflow.SunflowAPIInterface;

public class BanchOffParameter extends GeometryParameter {

    @Override
    public void setup(SunflowAPIInterface api) {
        super.setup(api);
        api.geometry(name, TYPE_BANCHOFF);
        setupInstance(api);
    }
}
