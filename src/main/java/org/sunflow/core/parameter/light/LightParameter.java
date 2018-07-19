package org.sunflow.core.parameter.light;

import org.sunflow.core.parameter.Parameter;

public abstract class LightParameter implements Parameter {

    public static final String PARAM_RADIANCE = "radiance";
    public static final String PARAM_SAMPLES = "samples";

    public static final String TYPE_CORNELL_BOX = "cornell_box";
    public static final String TYPE_DIRECTIONAL = "directional";
    public static final String TYPE_IMAGE_BASED = "ibl";
    public static final String TYPE_POINTLIGHT = "point";
    public static final String TYPE_SPHERE = "sphere";
    public static final String TYPE_SUNSKY = "sunsky";
    public static final String TYPE_TRIANGLE_MESH = "triangle_mesh";

    protected String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
