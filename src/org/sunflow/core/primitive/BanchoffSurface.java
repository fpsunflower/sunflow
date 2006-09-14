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

public class BanchoffSurface implements BoundedPrimitive {
    private Matrix4 o2w;
    private Matrix4 w2o;
    private BoundingBox bounds;
    private Shader shader;

    public BanchoffSurface(Shader shader, Matrix4 o2w) {
        this.o2w = o2w;
        w2o = this.o2w.inverse();
        if (w2o == null)
            throw new RuntimeException("Unable to inverse scale/translate matrix!");
        this.shader = shader;
        bounds = new BoundingBox(-1.5f, -1.5f, -1.5f);
        bounds.include(1.5f, 1.5f, 1.5f);
        bounds = o2w.transform(bounds);
    }

    public BanchoffSurface(Shader shader, Point3 center, float radius) {
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
        Point3 n = w2o.transformP(state.getPoint());
        state.getNormal().set(n.x * (2 * n.x * n.x - 1), n.y * (2 * n.y * n.y - 1), n.z * (2 * n.z * n.z - 1));
        state.getNormal().normalize();
        state.setShader(shader);
        // into object space
        Vector3 worldNormal = w2o.transformTransposeV(state.getNormal());
        state.getNormal().set(worldNormal);
        state.getNormal().normalize();
        state.getGeoNormal().set(state.getNormal());
        state.setBasis(OrthoNormalBasis.makeFromW(state.getNormal()));
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
        float rd2x = rdx * rdx;
        float rd2y = rdy * rdy;
        float rd2z = rdz * rdz;
        float ro2x = rox * rox;
        float ro2y = roy * roy;
        float ro2z = roz * roz;
        // setup the quartic coefficients
        // some common terms could probably be shared across these
        double A = (rd2y * rd2y + rd2z * rd2z + rd2x * rd2x);
        double B = 4 * (roy * rd2y * rdy + roz * rdz * rd2z + rox * rdx * rd2x);
        double C = (-rd2x - rd2y - rd2z + 6 * (ro2y * rd2y + ro2z * rd2z + ro2x * rd2x));
        double D = 2 * (2 * ro2z * roz * rdz - roz * rdz + 2 * ro2x * rox * rdx + 2 * ro2y * roy * rdy - rox * rdx - roy * rdy);
        double E = 3.0f / 8.0f + (-ro2z + ro2z * ro2z - ro2y + ro2y * ro2y - ro2x + ro2x * ro2x);
        // solve equation
        double[] t = Solvers.solveQuartic(A, B, C, D, E);
        if (t != null) {
            // early rejection
            if (t[0] >= r.getMax() || t[t.length - 1] <= r.getMin())
                return;
            // find first intersection in front of the ray
            for (int i = 0; i < t.length; i++) {
                if (t[i] > r.getMin()) {
                    r.setMax((float) t[i]);
                    state.setIntersection(this, 0, 0);
                    return;
                }
            }
        }
    }
}