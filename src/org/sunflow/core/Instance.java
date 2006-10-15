package org.sunflow.core;

import org.sunflow.SunflowAPI;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.UI;

public class Instance implements RenderObject {
    private Matrix4 o2w;
    private Matrix4 w2o;
    private BoundingBox bounds;
    private Shader[] shaders;
    private Geometry geometry;

    public Instance(Geometry geometry) {
        this.geometry = geometry;
        o2w = w2o = null;
        shaders = null;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        String[] shaders = pl.getStringArray("shaders");
        if (shaders != null) {
            this.shaders = new Shader[shaders.length];
            for (int i = 0; i < shaders.length; i++)
                this.shaders[i] = api.lookupShader(shaders[i]);
        }
        o2w = pl.getMatrix("transform", o2w);
        if (o2w != null) {
            w2o = o2w.inverse();
            if (w2o == null) {
                UI.printError("[GEO] Unable to compute transform inverse");
                return false;
            }
        } else
            o2w = w2o = null;
        bounds = geometry.getWorldBounds(o2w);
        return true;
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    public int getNumPrimitives() {
        return geometry.getNumPrimitives();
    }

    public void intersect(Ray r, IntersectionState state) {
        Ray localRay = r.transform(w2o);
        state.current = this;
        geometry.intersect(localRay, state);
        // FIXME: transfer max distance to current ray
        r.setMax(localRay.getMax());
    }

    public void prepareShadingState(ShadingState state) {
        geometry.prepareShadingState(state);
    }

    public Shader getShader(int i) {
        if (shaders == null || i < 0 || i >= shaders.length)
            return null;
        return shaders[i];
    }

    public Point3 transformObjectToWorld(Point3 p) {
        return o2w == null ? new Point3(p) : o2w.transformP(p);
    }

    public Point3 transformWorldToObject(Point3 p) {
        return o2w == null ? new Point3(p) : w2o.transformP(p);
    }

    public Vector3 transformNormalObjectToWorld(Vector3 n) {
        return o2w == null ? new Vector3(n) : w2o.transformTransposeV(n);
    }

    public Vector3 transformNormalWorldToObject(Vector3 n) {
        return o2w == null ? new Vector3(n) : o2w.transformTransposeV(n);
    }

    public Vector3 transformVectorObjectToWorld(Vector3 v) {
        return o2w == null ? new Vector3(v) : o2w.transformV(v);
    }

    public Vector3 transformVectorWorldToObject(Vector3 v) {
        return o2w == null ? new Vector3(v) : w2o.transformV(v);
    }
}