package org.sunflow.core.parameter;

public class IlluminationParameter {

    int emit = 0;
    String map = "";
    int gather = 0;
    float radius = 0;

    public int getEmit() {
        return emit;
    }

    public void setEmit(int emit) {
        this.emit = emit;
    }

    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public int getGather() {
        return gather;
    }

    public void setGather(int gather) {
        this.gather = gather;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }
}
