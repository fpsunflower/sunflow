package org.sunflow.core.parameter.geometry;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.core.parameter.InstanceParameter;
import org.sunflow.core.parameter.Parameter;

public class ObjectParameter implements Parameter {

    public static final String TYPE_BANCHOFF = "banchoff";
    public static final String TYPE_BEZIER_MESH = "bezier_mesh";
    public static final String TYPE_CYLINDER = "cylinder";
    public static final String TYPE_GUMBO = "gumbo";
    public static final String TYPE_HAIR = "hair";
    public static final String TYPE_JULIA = "julia";
    public static final String TYPE_TORUS = "torus";
    public static final String TYPE_SPHERE = "sphere";
    public static final String TYPE_SPHEREFLAKE = "sphereflake";
    public static final String TYPE_PARTICLES = "particles";
    public static final String TYPE_PLANE = "plane";
    public static final String TYPE_TEAPOT = "teapot";
    public static final String TYPE_TRIANGLE_MESH = "triangle_mesh";
    public static final String TYPE_FILE_MESH = "file_mesh";

    public static final String PARAM_ACCEL = "accel";

    protected String name = "none";
    protected String accel = "";

    protected InstanceParameter instanceParameter;

    public String getAccel() {
        return accel;
    }

    public void setAccel(String accel) {
        this.accel = accel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public InstanceParameter getInstanceParameter() {
        return instanceParameter;
    }

    public void setInstanceParameter(InstanceParameter instanceParameter) {
        this.instanceParameter = instanceParameter;
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        if (!accel.isEmpty()) {
            api.parameter(PARAM_ACCEL, accel);
        }
    }
}
