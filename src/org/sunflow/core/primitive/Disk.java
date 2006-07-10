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

public class Disk implements BoundedPrimitive {
    private Point3 center;
    private Vector3 normal;
    private float radius;
    private Shader shader;
    private OrthoNormalBasis basis;

    public Disk(Shader shader, Point3 p, Vector3 n) {
        this.shader = shader;
        center = p;
        normal = n;
        radius = 1.5f;
        basis = OrthoNormalBasis.makeFromW(n);
    }

    public BoundingBox getBounds() {
        BoundingBox b = new BoundingBox();
        b.include(center);
        b.getMinimum().x -= radius;
        b.getMinimum().y -= radius;
        b.getMinimum().z -= radius;
        b.getMaximum().x += radius;
        b.getMaximum().y += radius;
        b.getMaximum().z += radius;
        return null;
    }

    public float getBound(int i) {
        switch (i) {
            case 0:
                return center.x - radius;
            case 1:
                return center.x + radius;
            case 2:
                return center.y - radius;
            case 3:
                return center.y + radius;
            case 4:
                return center.z - radius;
            case 5:
                return center.z + radius;
            default:
                return 0;
        }
    }

    public boolean intersects(BoundingBox box) {
        return getBounds().intersects(box);
    }

    public void prepareShadingState(ShadingState state) {
        state.init();
        state.getRay().getPoint(state.getPoint());
        state.getNormal().set(normal);
        state.getUV().x = 0.0f;
        state.getUV().y = 0.0f;
        state.getGeoNormal().set(normal);
        state.setShader(shader);
        state.setBasis(basis);
    }

    public void intersect(Ray r, IntersectionState state) {
        float dn = normal.x * r.dx + normal.y * r.dy + normal.z * r.dz;
        if (dn == 0.0)
            return;
        float t = (((center.x - r.ox) * normal.x) + ((center.y - r.oy) * normal.y) + ((center.z - r.oz) * normal.z)) / dn;
        if (r.isInside(t)) {
            float opx = center.x - r.ox - t * r.dx;
            float opy = center.y - r.oy - t * r.dy;
            float opz = center.z - r.oz - t * r.dz;
            if (opx * opx + opy * opy + opz * opz < radius * radius) {
                r.setMax(t);
                state.setIntersection(this, 0, 0);
            }
        }
    }
}