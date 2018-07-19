package org.sunflow.core.parameter.modifier;

import org.sunflow.core.parameter.Parameter;

public abstract class ModifierParameter implements Parameter {

    public static final String TYPE_BUMP_MAP = "bump_map";
    public static final String TYPE_NORMAL_MAP = "normal_map";
    public static final String TYPE_PERLIN = "perlin";

    protected String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
