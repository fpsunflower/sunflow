package org.sunflow.core.parameter;

import org.sunflow.SunflowAPIInterface;

public class InstanceParameter implements Parameter {

    String name;
    String geometry;

    String[] shaders = null;
    String[] modifiers = null;

    TransformParameter transform = null;

    @Override
    public void setup(SunflowAPIInterface api) {
        if (transform != null) {
            transform.setup(api);
        }
        if (shaders != null) {
            api.parameter("shaders", shaders);
        }
        if (modifiers != null) {
            api.parameter("modifiers", modifiers);
        }

        api.instance(name, geometry);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public String[] getShaders() {
        return shaders;
    }

    public void setShaders(String[] shaders) {
        this.shaders = shaders;
    }

    public String[] getModifiers() {
        return modifiers;
    }

    public void setModifiers(String[] modifiers) {
        this.modifiers = modifiers;
    }

    public TransformParameter getTransform() {
        return transform;
    }

    public void setTransform(TransformParameter transform) {
        this.transform = transform;
    }
}
