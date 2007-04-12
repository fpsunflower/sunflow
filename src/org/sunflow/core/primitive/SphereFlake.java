package org.sunflow.core.primitive;

import org.sunflow.SunflowAPI;
import org.sunflow.core.Instance;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.ParameterList;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Ray;
import org.sunflow.core.ShadingState;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.MathUtils;
import org.sunflow.math.Matrix4;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class SphereFlake implements PrimitiveList {
    private static final float[] boundingRadiusOffset = new float[100];
    private static final float[] lowerX = new float[6];
    private static final float[] lowerY = new float[6];
    private static final float[] upperX = new float[3];
    private static final float[] upperY = new float[3];
    private int level = 2;

    static {
        // geometric series table, to compute bounding radius quickly
        for (int i = 0, r = 3; i < boundingRadiusOffset.length; i++, r *= 3)
            boundingRadiusOffset[i] = (float) (r - 3.0f) / r;
        // lower ring
        double a = 0, daL = 2 * Math.PI / 6, daU = 2 * Math.PI / 3;
        for (int i = 0; i < 6; i++) {
            lowerX[i] = (float) Math.sin(a);
            lowerY[i] = (float) Math.cos(a);
            a += daL;
        }
        a -= daL / 3; // tweak
        for (int i = 0; i < 3; i++) {
            upperX[i] = (float) Math.sin(a);
            upperY[i] = (float) Math.cos(a);
            a += daU;
        }

    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        level = MathUtils.clamp(pl.getInt("level", level), 0, 20);
        return true;
    }

    public BoundingBox getWorldBounds(Matrix4 o2w) {
        BoundingBox bounds = new BoundingBox(getPrimitiveBound(0, 1));
        if (o2w != null)
            bounds = o2w.transform(bounds);
        return bounds;
    }

    public float getPrimitiveBound(int primID, int i) {
        float br = 1 + boundingRadiusOffset[level];
        return (i & 1) == 0 ? -br : br;
    }

    public int getNumPrimitives() {
        return 1;
    }

    public void prepareShadingState(ShadingState state) {
        state.init();
        state.getRay().getPoint(state.getPoint());
        Instance parent = state.getInstance();
        Point3 localPoint = parent.transformWorldToObject(state.getPoint());

        float cx = state.getIntersectionState().getRobustStack()[0];
        float cy = state.getIntersectionState().getRobustStack()[1];
        float cz = state.getIntersectionState().getRobustStack()[2];

        state.getNormal().set(localPoint.x - cx, localPoint.y - cy, localPoint.z - cz);
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
        state.setShader(parent.getShader(0));
        state.setModifier(parent.getModifier(0));
        // into world space
        Vector3 worldNormal = parent.transformNormalObjectToWorld(state.getNormal());
        v = parent.transformVectorObjectToWorld(v);
        state.getNormal().set(worldNormal);
        state.getNormal().normalize();
        state.getGeoNormal().set(state.getNormal());
        // compute basis in world space
        state.setBasis(OrthoNormalBasis.makeFromWV(state.getNormal(), v));

    }

    public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        // intersect in local space
        float qa = r.dx * r.dx + r.dy * r.dy + r.dz * r.dz;
        float dx = +0.25f;
        float dy = +1.00f;
        float dz = -0.50f;
        // normalize
        float n = 1 / (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx *= n;
        dy *= n;
        dz *= n;
        intersectFlake(r, state, level, qa, 1 / qa, 0, 0, 0, dx, dy, dz, 1);
    }

    private void intersectFlake(Ray r, IntersectionState state, int level, float qa, float qaInv, float cx, float cy, float cz, float dx, float dy, float dz, float radius) {
        if (level <= 0) {
            // we reached the bottom - intersect sphere and bail out
            float vcx = cx - r.ox;
            float vcy = cy - r.oy;
            float vcz = cz - r.oz;
            float b = r.dx * vcx + r.dy * vcy + r.dz * vcz;
            float disc = b * b - qa * ((vcx * vcx + vcy * vcy + vcz * vcz) - radius * radius);
            if (disc > 0) {
                // intersects - check t values
                float d = (float) Math.sqrt(disc);
                float t1 = (b - d) * qaInv;
                float t2 = (b + d) * qaInv;
                if (t1 >= r.getMax() || t2 <= r.getMin())
                    return;
                if (t1 > r.getMin())
                    r.setMax((float) t1);
                else
                    r.setMax((float) t2);
                state.setIntersection(0, 0, 0);
                // FIXME: save information about the intersection in the robust
                // stack
                state.getRobustStack()[0] = cx;
                state.getRobustStack()[1] = cy;
                state.getRobustStack()[2] = cz;
            }
        } else {
            float boundRadius = radius * (1 + boundingRadiusOffset[level]);
            float vcx = cx - r.ox;
            float vcy = cy - r.oy;
            float vcz = cz - r.oz;
            float b = r.dx * vcx + r.dy * vcy + r.dz * vcz;
            float vcd = (vcx * vcx + vcy * vcy + vcz * vcz);
            float disc = b * b - qa * (vcd - boundRadius * boundRadius);
            if (disc > 0) {
                // intersects - check t values
                float d = (float) Math.sqrt(disc);
                float t1 = (b - d) * qaInv;
                float t2 = (b + d) * qaInv;
                if (t1 >= r.getMax() || t2 <= r.getMin())
                    return;

                // we hit the bounds, now compute intersection with the actual
                // leaf sphere
                disc = b * b - qa * (vcd - radius * radius);
                if (disc > 0) {
                    d = (float) Math.sqrt(disc);
                    t1 = (b - d) * qaInv;
                    t2 = (b + d) * qaInv;
                    if (t1 >= r.getMax() || t2 <= r.getMin()) {
                        // no hit
                    } else {
                        if (t1 > r.getMin())
                            r.setMax((float) t1);
                        else
                            r.setMax((float) t2);
                        state.setIntersection(0, 0, 0);
                        // FIXME: save information about the intersection in the
                        // robust stack
                        state.getRobustStack()[0] = cx;
                        state.getRobustStack()[1] = cy;
                        state.getRobustStack()[2] = cz;
                    }
                }

                // recursively intersect 9 other spheres
                // step1: compute basis around displacement vector
                float b1x, b1y, b1z;
                if ((dx * dx != 1) && (dy * dy != 1) && (dz * dz != 1)) {
                    b1x = dx;
                    b1y = dy;
                    b1z = dz;
                    if (dy * dy > dx * dx) {
                        if (dy * dy > dz * dz)
                            b1y = -b1y;
                        else
                            b1z = -b1z;
                    } else if (dz * dz > dx * dx)
                        b1z = -b1z;
                    else
                        b1x = -b1x;
                } else {
                    b1x = dz;
                    b1y = dx;
                    b1z = dy;
                }
                float b2x = dy * b1z - dz * b1y;
                float b2y = dz * b1x - dx * b1z;
                float b2z = dx * b1y - dy * b1x;
                b1x = dy * b2z - dz * b2y;
                b1y = dz * b2x - dx * b2z;
                b1z = dx * b2y - dy * b2x;
                // step2: generate lower ring
                float nr = radius * (1 / 3.0f), scale = radius + nr;
                for (int i = 0; i < 6; i++) {
                    float ndx = -0.2f * dx + lowerX[i] * b1x + lowerY[i] * b2x;
                    float ndy = -0.2f * dy + lowerX[i] * b1y + lowerY[i] * b2y;
                    float ndz = -0.2f * dz + lowerX[i] * b1z + lowerY[i] * b2z;
                    float n = 1 / (float) Math.sqrt(ndx * ndx + ndy * ndy + ndz * ndz);
                    ndx *= n;
                    ndy *= n;
                    ndz *= n;
                    intersectFlake(r, state, level - 1, qa, qaInv, cx + scale * ndx, cy + scale * ndy, cz + scale * ndz, ndx, ndy, ndz, nr);
                }
                // step3: generate upper ring
                for (int i = 0; i < 3; i++) {
                    float ndx = 0.6f * dx + upperX[i] * b1x + upperY[i] * b2x;
                    float ndy = 0.6f * dy + upperX[i] * b1y + upperY[i] * b2y;
                    float ndz = 0.6f * dz + upperX[i] * b1z + upperY[i] * b2z;
                    float n = 1 / (float) Math.sqrt(ndx * ndx + ndy * ndy + ndz * ndz);
                    ndx *= n;
                    ndy *= n;
                    ndz *= n;
                    intersectFlake(r, state, level - 1, qa, qaInv, cx + scale * ndx, cy + scale * ndy, cz + scale * ndz, ndx, ndy, ndz, nr);
                }
            }
        }
    }

    public PrimitiveList getBakingPrimitives() {
        return null;
    }
}