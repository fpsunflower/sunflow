package org.sunflow.core.parameter;

import org.sunflow.SunflowAPIInterface;

public class InstanceParameter implements Parameter {

    private String name;
    private String geometry;

    private String[] shaders = null;
    private String[] modifiers = null;

    private TransformParameter transform = null;

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

    public String name() {
        return name;
    }

    public InstanceParameter name(String name) {
        this.name = name;
        return this;
    }

    public String geometry() {
        return geometry;
    }

    public InstanceParameter geometry(String geometry) {
        this.geometry = geometry;
        return this;
    }

    public String[] shaders() {
        return shaders;
    }

    public InstanceParameter shaders(String... shaders) {
        this.shaders = shaders;
        return this;
    }

    public String[] modifiers() {
        return modifiers;
    }

    public InstanceParameter modifiers(String... modifiers) {
        this.modifiers = modifiers;
        return this;
    }

    public TransformParameter transform() {
        return transform;
    }

    public InstanceParameter transform(TransformParameter transform) {
        this.transform = transform;
        return this;
    }
}
