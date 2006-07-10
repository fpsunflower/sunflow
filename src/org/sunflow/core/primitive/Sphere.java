package org.sunflow.core.primitive;

import org.sunflow.core.BoundedPrimitive;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.Ray;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class Sphere implements BoundedPrimitive {
    private Point3 center;
    private float r, r2;
    private Shader shader;

    public Sphere(Shader shader, Point3 center, float radius) {
        this.center = center;
        r = radius;
        r2 = r * r;
        this.shader = shader;
    }

    public BoundingBox getBounds() {
        BoundingBox bounds = new BoundingBox();
        bounds.include(new Point3(center.x - r, center.y - r, center.z - r));
        bounds.include(new Point3(center.x + r, center.y + r, center.z + r));
        return bounds;
    }

    public float getBound(int i) {
        switch (i) {
            case 0:
                return center.x - r;
            case 1:
                return center.x + r;
            case 2:
                return center.y - r;
            case 3:
                return center.y + r;
            case 4:
                return center.z - r;
            case 5:
                return center.z + r;
            default:
                return 0;
        }
    }
    
    public boolean intersects(BoundingBox box) {
        float a, b;
        float dmax = 0;
        float dmin = 0;
        a = (center.x - box.getMinimum().x) * (center.x - box.getMinimum().x);
        b = (center.x - box.getMaximum().x) * (center.x - box.getMaximum().x);
        dmax += Math.max(a, b);
        if (center.x < box.getMinimum().x)
            dmin += a;
        else if (center.x > box.getMaximum().x)
            dmin += b;
        a = (center.y - box.getMinimum().y) * (center.y - box.getMinimum().y);
        b = (center.y - box.getMaximum().y) * (center.y - box.getMaximum().y);
        dmax += Math.max(a, b);
        if (center.y < box.getMinimum().y)
            dmin += a;
        else if (center.y > box.getMaximum().y)
            dmin += b;
        a = (center.z - box.getMinimum().z) * (center.z - box.getMinimum().z);
        b = (center.z - box.getMaximum().z) * (center.z - box.getMaximum().z);
        dmax += Math.max(a, b);
        if (center.z < box.getMinimum().z)
            dmin += a;
        else if (center.z > box.getMaximum().z)
            dmin += b;
        return ((dmin <= r2) && (r2 <= dmax));
    }

    public void prepareShadingState(ShadingState state) {
        state.init();
        state.getRay().getPoint(state.getPoint());
        Point3.sub(state.getPoint(), center, state.getNormal());
        state.getNormal().normalize();
        state.getUV().y = (float) (Math.acos(state.getNormal().z) / Math.PI);
        if (state.getNormal().y >= 0.0)
            state.getUV().x = (float) (Math.acos(state.getNormal().x / Math.sin(Math.PI * state.getUV().y)) / (2 * Math.PI));
        else
            state.getUV().x = (float) ((Math.PI + Math.acos(state.getNormal().x / Math.sin(Math.PI * state.getUV().y))) / (2 * Math.PI));
        state.getGeoNormal().set(state.getNormal());

        Vector3 v = new Vector3();
        v.x = -2 * (float) Math.PI * state.getNormal().y;
        v.y = 2 * (float) Math.PI * state.getNormal().x;
        v.z = 0;

        state.setBasis(OrthoNormalBasis.makeFromWV(state.getNormal(), v));
        state.setShader(shader);
    }

    public void intersect(Ray r, IntersectionState state) {
        float ocx = r.ox - center.x;
        float ocy = r.oy - center.y;
        float ocz = r.oz - center.z;
        float qb = (r.dx * ocx) + (r.dy * ocy) + (r.dz * ocz);
        float qc = ((ocx * ocx) + (ocy * ocy) + (ocz * ocz)) - r2;
        float det = (qb * qb) - qc;
        if (det >= 0.0) {
            det = (float) Math.sqrt(det);
            float t = -det - qb;
            if (r.isInside(t)) {
                r.setMax(t);
                state.setIntersection(this, 0, 0);
            } else {
                t = det - qb;
                if (r.isInside(t)) {
                    r.setMax(t);
                    state.setIntersection(this, 0, 0);
                }
            }
        }
    }
}