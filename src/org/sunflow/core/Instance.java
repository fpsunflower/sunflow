package org.sunflow.core;

import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class Instance implements BoundedPrimitive {
    private Matrix4 o2w;
    private Matrix4 w2o;
    private BoundingBox bounds;
    private Shader[] shaders;
    private Traceable traceable;

    public Instance(Shader[] shaders, Matrix4 o2w, Traceable traceable) {
        this.shaders = shaders;
        this.o2w = o2w;
        this.traceable = traceable;
        if (o2w != null) {
            w2o = o2w.inverse();
            if (w2o == null)
                throw new RuntimeException("Unable to inverse scale/translate matrix!");
        } else
            o2w = w2o = null;
        bounds = traceable.getWorldBounds(o2w);
    }

    public float getBound(int i) {
        return bounds.getBound(i);
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    public boolean intersects(BoundingBox box) {
        return bounds.intersects(box);
    }

    public void intersect(Ray r, IntersectionState istate) {
        Ray localRay = r.transform(w2o);
        traceable.intersect(localRay, this, istate);
        // FIXME: transfer max distance to current ray
        r.setMax(localRay.getMax());
    }

    public void prepareShadingState(ShadingState state) {
        traceable.prepareShadingState(this, state);
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