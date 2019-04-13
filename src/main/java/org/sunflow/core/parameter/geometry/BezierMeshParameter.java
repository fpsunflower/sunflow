package org.sunflow.core.parameter.geometry;

import org.sunflow.SunflowAPIInterface;

public class BezierMeshParameter extends GeometryParameter {

    int nu, nv;
    boolean uwrap = false, vwrap = false;
    float[] points;

    int subdivs = 1;
    boolean smooth = false;

    @Override
    public void setup(SunflowAPIInterface api) {
        super.setup(api);
        api.parameter("nu", nu);
        api.parameter("nv", nv);
        api.parameter("uwrap", uwrap);
        api.parameter("vwrap", vwrap);
        api.parameter("points", "point", "vertex", points);
        api.parameter("subdivs", subdivs);
        api.parameter("smooth", smooth);
        api.geometry(name, TYPE_BEZIER_MESH);

        setupInstance(api);
    }

    public int getNu() {
        return nu;
    }

    public void setNu(int nu) {
        this.nu = nu;
    }

    public int getNv() {
        return nv;
    }

    public void setNv(int nv) {
        this.nv = nv;
    }

    public boolean isUwrap() {
        return uwrap;
    }

    public void setUwrap(boolean uwrap) {
        this.uwrap = uwrap;
    }

    public boolean isVwrap() {
        return vwrap;
    }

    public void setVwrap(boolean vwrap) {
        this.vwrap = vwrap;
    }

    public float[] getPoints() {
        return points;
    }

    public void setPoints(float[] points) {
        this.points = points;
    }

    public int getSubdivs() {
        return subdivs;
    }

    public void setSubdivs(int subdivs) {
        this.subdivs = subdivs;
    }

    public boolean isSmooth() {
        return smooth;
    }

    public void setSmooth(boolean smooth) {
        this.smooth = smooth;
    }
}
