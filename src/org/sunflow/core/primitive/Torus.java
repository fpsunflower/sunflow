package org.sunflow.core.primitive;

import org.sunflow.core.BoundedPrimitive;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.Ray;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.MathUtils;
import org.sunflow.math.Matrix4;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Solvers;
import org.sunflow.math.Vector3;

public class Torus implements BoundedPrimitive {
    private Matrix4 o2w;
    private Matrix4 w2o;
    private float ri2, ro2;
    private float ri;
    private BoundingBox bounds;
    private Shader shader;

    public Torus(Shader shader, Matrix4 o2w, float ri, float ro) {
        this.o2w = o2w;
        this.ri = ri;
        this.ri2 = ri * ri;
        this.ro2 = ro * ro;
        w2o = this.o2w.inverse();
        if (w2o == null)
            throw new RuntimeException("Unable to inverse scale/translate matrix!");
        this.shader = shader;
        bounds = new BoundingBox(-ro - ri, -ro - ri, -ri);
        bounds.include(ro + ri, ro + ri, ri);
        bounds = o2w.transform(bounds);
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
        // get local point
        Point3 p = w2o.transformP(state.getPoint());
        // compute local normal
        float deriv = p.x * p.x + p.y * p.y + p.z * p.z - ri2 - ro2;
        state.getNormal().set(p.x * deriv, p.y * deriv, p.z * deriv + 2 * ro2 * p.z);
        state.getNormal().normalize();
        
        double phi = Math.asin(MathUtils.clamp(p.z / ri, -1, 1));
        double theta = Math.atan2(p.y, p.x);
        if (theta < 0)
            theta += 2 * Math.PI;
        state.getUV().x = (float) (theta / (2 * Math.PI));
        state.getUV().y = (float) ((phi + Math.PI / 2) / Math.PI);
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
        // compute some common factors
        double alpha = rd2x + rd2y + rd2z;
        double beta = 2 * (rox * rdx + roy * rdy + roz * rdz);
        double gamma = (ro2x + ro2y + ro2z) - ri2 - ro2;
        // setup quartic coefficients
        double A = alpha * alpha;
        double B = 2 * alpha * beta;
        double C = beta * beta + 2 * alpha * gamma + 4 * ro2 * rd2z;
        double D = 2 * beta * gamma + 8 * ro2 * roz * rdz;
        double E = gamma * gamma + 4 * ro2 * ro2z - 4 * ro2 * ri2;
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