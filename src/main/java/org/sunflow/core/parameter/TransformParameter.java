package org.sunflow.core.parameter;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.math.Matrix4;

public class TransformParameter implements Parameter {

    public static final String INTERPOLATION_NONE = "none";

    float[] times;
    Matrix4[] transforms = new Matrix4[]{Matrix4.IDENTITY};

    String interpolation = INTERPOLATION_NONE;

    @Override
    public void setup(SunflowAPIInterface api) {
        if (times == null) {
            api.parameter("transform", transforms[0]);
        } else {
            int steps = times.length;
            api.parameter("transform.steps", steps);
            api.parameter("transform.times", "float", interpolation, times);
            for (int i = 0; i < steps; i++) {
                api.parameter(String.format("transform[%d]", i), transforms[i]);
            }
        }
    }

    public float[] getTimes() {
        return times;
    }

    public void setTimes(float[] times) {
        this.times = times;
    }

    public Matrix4[] getTransforms() {
        return transforms;
    }

    public void setTransforms(Matrix4[] transforms) {
        this.transforms = transforms;
    }

    public TransformParameter rotateX(float angle) {
        Matrix4 t = Matrix4.rotateX((float) Math.toRadians(angle));
        transforms[0] = t.multiply(transforms[0]);
        return this;
    }

    public TransformParameter rotateY(float angle) {
        Matrix4 t = Matrix4.rotateY((float) Math.toRadians(angle));
        transforms[0] = t.multiply(transforms[0]);
        return this;
    }

    public TransformParameter rotateZ(float angle) {
        Matrix4 t = Matrix4.rotateZ((float) Math.toRadians(angle));
        transforms[0] = t.multiply(transforms[0]);
        return this;
    }

    public TransformParameter scale(float scale) {
        Matrix4 t = Matrix4.scale(scale);
        transforms[0] = t.multiply(transforms[0]);
        return this;
    }

    public TransformParameter scale(float x, float y, float z) {
        Matrix4 t = Matrix4.scale(x, y, z);
        transforms[0] = t.multiply(transforms[0]);
        return this;
    }

    public TransformParameter translate(float x, float y, float z) {
        Matrix4 t = Matrix4.translation(x, y, z);
        transforms[0] = t.multiply(transforms[0]);
        return this;
    }
}
