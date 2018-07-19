package org.sunflow.core.parameter;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.math.Matrix4;

public class TransformParameter implements Parameter {

    public static final String INTERPOLATION_NONE = "none";

    float[] times;
    Matrix4[] transforms;

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

}
