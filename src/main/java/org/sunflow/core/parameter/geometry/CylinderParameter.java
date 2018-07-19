package org.sunflow.core.parameter.geometry;

import org.sunflow.SunflowAPIInterface;

public class CylinderParameter extends ObjectParameter {

    @Override
    public void setup(SunflowAPIInterface api) {
        super.setup(api);
        api.geometry(name, TYPE_CYLINDER);
    }
}
