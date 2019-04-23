package org.sunflow.core.parameter.shader;

import org.sunflow.core.parameter.Parameter;

public abstract class ShaderParameter implements Parameter {

    public static final String TYPE_AMBIENT_OCCLUSION = "ambient_occlusion";
    public static final String TYPE_TEXTURED_AMBIENT_OCCLUSION = "textured_ambient_occlusion";
    public static final String TYPE_CONSTANT = "constant";
    public static final String TYPE_DIFFUSE = "diffuse";
    public static final String TYPE_TEXTURED_DIFFUSE = "textured_diffuse";
    public static final String TYPE_GLASS = "glass";
    public static final String TYPE_MIRROR = "mirror";
    public static final String TYPE_PHONG = "phong";
    public static final String TYPE_TEXTURED_PHONG = "textured_phong";
    public static final String TYPE_SHINY_DIFFUSE = "shiny_diffuse";
    public static final String TYPE_TEXTURED_SHINY_DIFFUSE = "textured_shiny_diffuse";
    public static final String TYPE_UBER = "uber";
    public static final String TYPE_WARD = "ward";
    public static final String TYPE_SHOW_INSTANCE_ID = "show_instance_id";
    public static final String TYPE_TEXTURED_WARD = "textured_ward";
    public static final String TYPE_VIEW_CAUSTICS = "view_caustics";
    public static final String TYPE_VIEW_IRRADIANCE = "view_irradiance";
    public static final String TYPE_VIEW_GLOBAL = "view_global";
    public static final String TYPE_NONE = "none";

    protected String name;

    public ShaderParameter() {
        super();
    }

    public ShaderParameter(String name) {
        super();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
