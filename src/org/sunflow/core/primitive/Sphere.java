package org.sunflow.core.primitive;

import org.sunflow.core.BoundedPrimitive;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.Ray;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Solvers;
import org.sunflow.math.Vector3;

public class Sphere implements BoundedPrimitive {
    private Matrix4 o2w;
    private Matrix4 w2o;
    private BoundingBox bounds;
    private Shader shader;

    public Sphere(Shader shader, Matrix4 o2w) {
        this.o2w = o2w;
        w2o = o2w.inverse();
        if (w2o == null)
            throw new RuntimeException("Unable to inverse scale/translate matrix!");
        this.shader = shader;
        bounds = new BoundingBox(-1, -1, -1);
        bounds.include(1, 1, 1);
        bounds = o2w.transform(bounds);
    }
    
    public Sphere(Shader shader, Point3 center, float radius) {
        this(shader, Matrix4.translation(center.x, center.y, center.z).multiply(Matrix4.scale(radius)));
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    public float getBound(int i) {
        switch (i) {
            case 0:
                return bounds.getMinimum().x;
            case 1:
                return bounds.getMaximum().x;
            case 2:
                return bounds.getMinimum().y;
            case 3:
                return bounds.getMaximum().y;
            case 4:
                return bounds.getMinimum().z;
            case 5:
                return bounds.getMaximum().z;
            default:
                return 0;
        }
    }

    public boolean intersects(BoundingBox box) {
        return box.intersects(bounds);
    }

    public void prepareShadingState(ShadingState state) {
        state.init();
        state.getRay().getPoint(state.getPoint());
        Point3 localPoint = w2o.transformP(state.getPoint());
        state.getNormal().set(localPoint.x, localPoint.y, localPoint.z);
        state.getNormal().normalize();

        float phi = (float) Math.atan2(state.getNormal().y, state.getNormal().x);
        if (phi < 0)
            phi += 2 * Math.PI;
        float theta = (float) Math.acos(state.getNormal().z);
        state.getUV().y = theta / (float) Math.PI;
        state.getUV().x = phi / (float) (2 * Math.PI);
        Vector3 v = new Vector3();
        v.x = -2 * (float) Math.PI * state.getNormal().y;
        v.y = 2 * (float) Math.PI * state.getNormal().x;
        v.z = 0;
        state.setShader(shader);
        // into object space
        Vector3 worldNormal = w2o.transformTransposeV(state.getNormal());
        v = o2w.transformV(v);
        state.getNormal().set(worldNormal);
        state.getNormal().normalize();
        state.getGeoNormal().set(state.getNormal());
        state.setBasis(OrthoNormalBasis.makeFromWV(state.getNormal(), v));

    }

    public void intersect(Ray r, IntersectionState state) {
        // transform ray into local space
        float rox = w2o.transformPX(r.ox, r.oy, r.oz);
        float roy = w2o.transformPY(r.ox, r.oy, r.oz);
        float roz = w2o.transformPZ(r.ox, r.oy, r.oz);
        float rdx = w2o.transformVX(r.dx, r.dy, r.dz);
        float rdy = w2o.transformVY(r.dx, r.dy, r.dz);
        float rdz = w2o.transformVZ(r.dx, r.dy, r.dz);
        // intersect in local space
        float qa = rdx * rdx + rdy * rdy + rdz * rdz;
        float qb = 2 * ((rdx * rox) + (rdy * roy) + (rdz * roz));
        float qc = ((rox * rox) + (roy * roy) + (roz * roz)) - 1;
        float[] t = Solvers.solveQuadric(qa, qb, qc);
        if (t != null) {
            // early rejection
            if (t[0] >= r.getMax() || t[1] <= r.getMin())
                return;
            if (t[0] > r.getMin())
                r.setMax(t[0]);
            else
                r.setMax(t[1]);
            state.setIntersection(this, 0, 0);
        }
    }
}