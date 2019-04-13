package org.sunflow.core.parameter.geometry;

import org.sunflow.SunflowAPIInterface;

public abstract class GeometryParameter extends ObjectParameter {

    public void setupInstance(SunflowAPIInterface api) {
        if (instanceParameter != null) {
            instanceParameter.setName(name + ".instance");
            instanceParameter.setGeometry(name);
            instanceParameter.setup(api);
        }
    }

}
