package org.sunflow.core.parameter;

import org.sunflow.SunflowAPI;
import org.sunflow.SunflowAPIInterface;

public class OverrideParameter implements Parameter {

    public static final String PARAM_OVERRIDE_SHADER = "override.shader";
    public static final String PARAM_OVERRIDE_PHOTONS = "override.photons";

    String shader = "";
    boolean photons = false;

    @Override
    public void setup(SunflowAPIInterface api) {

        if (!shader.isEmpty()) {
            api.parameter(PARAM_OVERRIDE_SHADER, shader);
        }
        api.parameter(PARAM_OVERRIDE_PHOTONS, photons);

        api.options(SunflowAPI.DEFAULT_OPTIONS);
    }

    public String getShader() {
        return shader;
    }

    public void setShader(String shader) {
        this.shader = shader;
    }

    public boolean isPhotons() {
        return photons;
    }

    public void setPhotons(boolean photons) {
        this.photons = photons;
    }
}
