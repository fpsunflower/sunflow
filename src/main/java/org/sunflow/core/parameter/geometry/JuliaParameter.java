package org.sunflow.core.parameter.geometry;

import org.sunflow.SunflowAPIInterface;

public class JuliaParameter extends GeometryParameter {

    // Quaternion
    float cx, cy, cz, cw;

    int iterations = 1;
    float epsilon;

    @Override
    public void setup(SunflowAPIInterface api) {
        super.setup(api);

        api.parameter("cw", cw);
        api.parameter("cx", cx);
        api.parameter("cy", cy);
        api.parameter("cz", cz);
        api.parameter("iterations", iterations);
        api.parameter("epsilon", epsilon);

        api.geometry(name, TYPE_JULIA);

        setupInstance(api);
    }

    public float getCx() {
        return cx;
    }

    public void setCx(float cx) {
        this.cx = cx;
    }

    public float getCy() {
        return cy;
    }

    public void setCy(float cy) {
        this.cy = cy;
    }

    public float getCz() {
        return cz;
    }

    public void setCz(float cz) {
        this.cz = cz;
    }

    public float getCw() {
        return cw;
    }

    public void setCw(float cw) {
        this.cw = cw;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public float getEpsilon() {
        return epsilon;
    }

    public void setEpsilon(float epsilon) {
        this.epsilon = epsilon;
    }
}
