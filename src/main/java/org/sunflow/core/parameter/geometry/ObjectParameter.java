package org.sunflow.core.parameter.geometry;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.core.parameter.InstanceParameter;
import org.sunflow.core.parameter.Parameter;
import org.sunflow.core.parameter.TransformParameter;
import org.sunflow.core.parameter.modifier.ModifierParameter;
import org.sunflow.core.parameter.shader.ShaderParameter;

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

    public void shaders(String... shaders) {
        if (instanceParameter == null) {
            instanceParameter = new InstanceParameter();
        }

        instanceParameter.shaders(shaders);
    }

    public void shaders(ShaderParameter... shaders) {
        if (instanceParameter == null) {
            instanceParameter = new InstanceParameter();
        }

        String[] names = new String[shaders.length];
        for (int i = 0; i < shaders.length; i++) {
            names[i] = shaders[i].getName();
        }

        instanceParameter.shaders(names);

    }

    public void modifiers(String... modifiers) {
        if (instanceParameter == null) {
            instanceParameter = new InstanceParameter();
        }

        instanceParameter.shaders(modifiers);
    }

    public void modifiers(ModifierParameter... modifiers) {
        if (instanceParameter == null) {
            instanceParameter = new InstanceParameter();
        }

        String[] names = new String[modifiers.length];
        for (int i = 0; i < modifiers.length; i++) {
            names[i] = modifiers[i].getName();
        }

        instanceParameter.modifiers(names);
    }

    public void rotateX(float angle) {
        TransformParameter transformParameter = getInstanceTransform();
        transformParameter.rotateX(angle);
    }

    public void rotateY(float angle) {
        TransformParameter transformParameter = getInstanceTransform();
        transformParameter.rotateY(angle);
    }

    public void rotateZ(float angle) {
        TransformParameter transformParameter = getInstanceTransform();
        transformParameter.rotateZ(angle);
    }

    public void scale(float scale) {
        TransformParameter transformParameter = getInstanceTransform();
        transformParameter.scale(scale);
    }

    public void scale(float x, float y, float z) {
        TransformParameter transformParameter = getInstanceTransform();
        transformParameter.scale(x, y, z);
    }

    public void translate(float x, float y, float z) {
        TransformParameter transformParameter = getInstanceTransform();
        transformParameter.translate(x, y, z);
    }

    private TransformParameter getInstanceTransform() {
        if (instanceParameter == null) {
            instanceParameter = new InstanceParameter();
        }

        TransformParameter transformParameter = instanceParameter.transform();

        if (transformParameter == null) {
            transformParameter = new TransformParameter();
            instanceParameter.transform(transformParameter);
        }
        return transformParameter;
    }

}
