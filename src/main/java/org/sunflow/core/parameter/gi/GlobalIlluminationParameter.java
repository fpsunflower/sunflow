package org.sunflow.core.parameter.gi;

import org.sunflow.SunflowAPI;
import org.sunflow.SunflowAPIInterface;
import org.sunflow.core.parameter.Parameter;

/**
 * Global Illumination (GI)
 */
public class GlobalIlluminationParameter implements Parameter {

    public static final String PARAM_ENGINE = "gi.engine";

    public static final String TYPE_AMBOCC = "ambocc";
    public static final String TYPE_FAKE = "fake";
    public static final String TYPE_IGI = "igi";
    public static final String TYPE_IRR_CACHE = "irr-cache";
    public static final String TYPE_PATH = "path";


    public static final String TYPE_NONE = "none";

    @Override
    public void setup(SunflowAPIInterface api) {
        api.options(SunflowAPI.DEFAULT_OPTIONS);
    }
}
