package org.sunflow.core.parameter;

import org.sunflow.SunflowAPIInterface;

public class TraceDepthsParameter implements Parameter {

    public static final String PARAM_DEPTHS_DIFFUSE = "depths.diffuse";
    public static final String PARAM_DEPTHS_REFLECTION = "depths.reflection";
    public static final String PARAM_DEPTHS_REFRACTION = "depths.refraction";

    int diffuse = 0;
    int reflection = 0;
    int refraction = 0;

    @Override
    public void setup(SunflowAPIInterface api) {
        if (diffuse > 0) {
            api.parameter(PARAM_DEPTHS_DIFFUSE, diffuse);
        }
        if (reflection > 0) {
            api.parameter(PARAM_DEPTHS_REFLECTION, reflection);
        }
        if (refraction > 0) {
            api.parameter(PARAM_DEPTHS_REFRACTION, refraction);
        }
    }

    public int getDiffuse() {
        return diffuse;
    }

    public void setDiffuse(int diffuse) {
        this.diffuse = diffuse;
    }

    public int getReflection() {
        return reflection;
    }

    public void setReflection(int reflection) {
        this.reflection = reflection;
    }

    public int getRefraction() {
        return refraction;
    }

    public void setRefraction(int refraction) {
        this.refraction = refraction;
    }
}
