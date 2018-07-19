package org.sunflow.core.parameter;

import org.sunflow.SunflowAPI;
import org.sunflow.SunflowAPIInterface;

public class PhotonParameter implements Parameter {

    public static final String PARAM_CAUSTICS = "caustics";
    public static final String PARAM_CAUSTICS_EMIT = "caustics.emit";
    public static final String PARAM_CAUSTICS_GATHER = "caustics.gather";
    public static final String PARAM_CAUSTICS_RADIUS = "caustics.radius";

    IlluminationParameter caustics;

    public PhotonParameter() {
        caustics = new IlluminationParameter();
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter(PARAM_CAUSTICS, caustics.map);
        api.parameter(PARAM_CAUSTICS_EMIT, caustics.emit);
        api.parameter(PARAM_CAUSTICS_GATHER, caustics.gather);
        api.parameter(PARAM_CAUSTICS_RADIUS, caustics.radius);
        api.options(SunflowAPI.DEFAULT_OPTIONS);
    }

    public int getNumEmit() {
        return caustics.emit;
    }

    public void setNumEmit(int numEmit) {
        caustics.emit = numEmit;
    }

    public void setCaustics(String caustics) {
        this.caustics.map = caustics;
    }

    public void setCausticsGather(int causticsGather) {
        caustics.gather = causticsGather;
    }

    public void setCausticsRadius(float causticsRadius) {
        caustics.radius = causticsRadius;
    }
}
