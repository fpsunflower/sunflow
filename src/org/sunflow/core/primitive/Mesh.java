package org.sunflow.core.primitive;

import org.sunflow.SunflowAPI;
import org.sunflow.core.Instance;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.ParameterList;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Ray;
import org.sunflow.core.ShadingState;
import org.sunflow.core.ParameterList.FloatParameter;
import org.sunflow.core.ParameterList.InterpolationType;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.MathUtils;
import org.sunflow.math.Matrix4;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.UI;

public class Mesh implements PrimitiveList {
    private static boolean smallTriangles = false;
    protected float[] points;
    protected int[] triangles;
    private WaldTriangle[] triaccel;
    private FloatParameter normals;
    private FloatParameter uvs;
    private byte[] faceShaders;

    public static void setSmallTriangles(boolean smallTriangles) {
        if (smallTriangles)
            UI.printInfo("[TRI] Activating small mesh mode");
        else
            UI.printInfo("[TRI] Disabling small mesh mode");
        Mesh.smallTriangles = smallTriangles;
    }

    public Mesh() {
        triangles = null;
        points = null;
        normals = uvs = new FloatParameter();
        faceShaders = null;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        boolean updatedTopology = false;
        {
            int[] triangles = pl.getIntArray("triangles");
            if (triangles != null) {
                this.triangles = triangles;
                updatedTopology = true;
            }
        }
        if (triangles == null) {
            UI.printError("[TRI] Unable to update mesh - triangle indices are missing");
            return false;
        }
        if (triangles.length % 3 != 0)
            UI.printWarning("[TRI] Triangle index data is not a multiple of 3 - triangles may be missing");
        pl.setFaceCount(triangles.length / 3);
        {
            FloatParameter pointsP = pl.getPointArray("points");
            if (pointsP != null)
                if (pointsP.interp != InterpolationType.VERTEX)
                    UI.printError("[TRI] Point interpolation type must be set to \"vertex\" - was \"%s\"", pointsP.interp.name().toLowerCase());
                else {
                    points = pointsP.data;
                    updatedTopology = true;
                }
        }
        if (points == null) {
            UI.printError("[TRI] Unabled to update mesh - vertices are missing");
            return false;
        }
        pl.setVertexCount(points.length / 3);
        pl.setFaceVertexCount(3 * (triangles.length / 3));
        FloatParameter normals = pl.getVectorArray("normals");
        if (normals != null)
            this.normals = normals;
        FloatParameter uvs = pl.getTexCoordArray("uvs");
        if (uvs != null)
            this.uvs = uvs;
        int[] faceShaders = pl.getIntArray("faceshaders");
        if (faceShaders != null && faceShaders.length == triangles.length / 3) {
            this.faceShaders = new byte[faceShaders.length];
            for (int i = 0; i < faceShaders.length; i++) {
                int v = faceShaders[i];
                if (v > 255)
                    UI.printWarning("[TRI] Shader index too large on triangle %d", i);
                this.faceShaders[i] = (byte) (v & 0xFF);
            }
        }
        if (updatedTopology) {
            // create triangle acceleration structure
            init();
        }
        return true;
    }

    public float getPrimitiveBound(int primID, int i) {
        int tri = 3 * primID;
        int a = 3 * triangles[tri + 0];
        int b = 3 * triangles[tri + 1];
        int c = 3 * triangles[tri + 2];
        int axis = i >>> 1;
        if ((i & 1) == 0)
            return MathUtils.min(points[a + axis], points[b + axis], points[c + axis]);
        else
            return MathUtils.max(points[a + axis], points[b + axis], points[c + axis]);
    }

    public BoundingBox getWorldBounds(Matrix4 o2w) {
        BoundingBox bounds = new BoundingBox();
        if (o2w == null) {
            for (int i = 0; i < points.length; i += 3)
                bounds.include(points[i], points[i + 1], points[i + 2]);
        } else {
            // transform vertices first
            for (int i = 0; i < points.length; i += 3) {
                float x = points[i];
                float y = points[i + 1];
                float z = points[i + 2];
                float wx = o2w.transformPX(x, y, z);
                float wy = o2w.transformPY(x, y, z);
                float wz = o2w.transformPZ(x, y, z);
                bounds.include(wx, wy, wz);
            }
        }
        return bounds;
    }

    public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        if (triaccel != null) {
            // optional fast intersection method
            triaccel[primID].intersect(r, primID, state);
            return;
        }
        // ray-triangle intersection here
        int tri = 3 * primID;
        int a = 3 * triangles[tri + 0];
        int b = 3 * triangles[tri + 1];
        int c = 3 * triangles[tri + 2];
        double edge1x = points[b + 0] - points[a + 0];
        double edge1y = points[b + 1] - points[a + 1];
        double edge1z = points[b + 2] - points[a + 2];
        double edge2x = points[c + 0] - points[a + 0];
        double edge2y = points[c + 1] - points[a + 1];
        double edge2z = points[c + 2] - points[a + 2];
        double pvecx = r.dy * edge2z - r.dz * edge2y;
        double pvecy = r.dz * edge2x - r.dx * edge2z;
        double pvecz = r.dx * edge2y - r.dy * edge2x;
        double qvecx, qvecy, qvecz;
        double u, v;
        double det = edge1x * pvecx + edge1y * pvecy + edge1z * pvecz;
        if (det > 0) {
            double tvecx = r.ox - points[a + 0];
            double tvecy = r.oy - points[a + 1];
            double tvecz = r.oz - points[a + 2];
            u = (tvecx * pvecx + tvecy * pvecy + tvecz * pvecz);
            if (u < 0.0 || u > det)
                return;
            qvecx = tvecy * edge1z - tvecz * edge1y;
            qvecy = tvecz * edge1x - tvecx * edge1z;
            qvecz = tvecx * edge1y - tvecy * edge1x;
            v = (r.dx * qvecx + r.dy * qvecy + r.dz * qvecz);
            if (v < 0.0 || u + v > det)
                return;
        } else if (det < 0) {
            double tvecx = r.ox - points[a + 0];
            double tvecy = r.oy - points[a + 1];
            double tvecz = r.oz - points[a + 2];
            u = (tvecx * pvecx + tvecy * pvecy + tvecz * pvecz);
            if (u > 0.0 || u < det)
                return;
            qvecx = tvecy * edge1z - tvecz * edge1y;
            qvecy = tvecz * edge1x - tvecx * edge1z;
            qvecz = tvecx * edge1y - tvecy * edge1x;
            v = (r.dx * qvecx + r.dy * qvecy + r.dz * qvecz);
            if (v > 0.0 || u + v < det)
                return;
        } else
            return;
        double inv_det = 1.0 / det;
        float t = (float) ((edge2x * qvecx + edge2y * qvecy + edge2z * qvecz) * inv_det);
        if (r.isInside(t)) {
            r.setMax(t);
            state.setIntersection(primID, (float) (u * inv_det), (float) (v * inv_det));
        }
    }

    public int getNumPrimitives() {
        if (triangles == null)
            return points.length / 9;
        else
            return triangles.length / 3;
    }

    public void prepareShadingState(ShadingState state) {
        state.init();
        Instance parent = state.getInstance();
        int primID = state.getPrimitiveID();
        float u = state.getU();
        float v = state.getV();
        float w = 1 - u - v;
        state.getRay().getPoint(state.getPoint());
        int tri = 3 * primID;
        int index0 = triangles[tri + 0];
        int index1 = triangles[tri + 1];
        int index2 = triangles[tri + 2];
        Point3 v0p = getPoint(index0);
        Point3 v1p = getPoint(index1);
        Point3 v2p = getPoint(index2);
        Vector3 ng = Point3.normal(v0p, v1p, v2p);
        if (parent != null)
            ng = parent.transformNormalObjectToWorld(ng);
        ng.normalize();
        state.getGeoNormal().set(ng);
        switch (normals.interp) {
            case NONE:
            case FACE: {
                state.getNormal().set(ng);
                break;
            }
            case VERTEX: {
                int i30 = 3 * index0;
                int i31 = 3 * index1;
                int i32 = 3 * index2;
                float[] normals = this.normals.data;
                state.getNormal().x = w * normals[i30 + 0] + u * normals[i31 + 0] + v * normals[i32 + 0];
                state.getNormal().y = w * normals[i30 + 1] + u * normals[i31 + 1] + v * normals[i32 + 1];
                state.getNormal().z = w * normals[i30 + 2] + u * normals[i31 + 2] + v * normals[i32 + 2];
                if (parent != null)
                    state.getNormal().set(parent.transformNormalObjectToWorld(state.getNormal()));
                state.getNormal().normalize();
                break;
            }
            case FACEVARYING: {
                int idx = 3 * tri;
                float[] normals = this.normals.data;
                state.getNormal().x = w * normals[idx + 0] + u * normals[idx + 3] + v * normals[idx + 6];
                state.getNormal().y = w * normals[idx + 1] + u * normals[idx + 4] + v * normals[idx + 7];
                state.getNormal().z = w * normals[idx + 2] + u * normals[idx + 5] + v * normals[idx + 8];
                if (parent != null)
                    state.getNormal().set(parent.transformNormalObjectToWorld(state.getNormal()));
                state.getNormal().normalize();
                break;
            }
        }
        float uv00 = 0, uv01 = 0, uv10 = 0, uv11 = 0, uv20 = 0, uv21 = 0;
        switch (uvs.interp) {
            case NONE:
            case FACE: {
                state.getUV().x = 0;
                state.getUV().y = 0;
                break;
            }
            case VERTEX: {
                int i20 = 2 * index0;
                int i21 = 2 * index1;
                int i22 = 2 * index2;
                float[] uvs = this.uvs.data;
                uv00 = uvs[i20 + 0];
                uv01 = uvs[i20 + 1];
                uv10 = uvs[i21 + 0];
                uv11 = uvs[i21 + 1];
                uv20 = uvs[i22 + 0];
                uv21 = uvs[i22 + 1];
                break;
            }
            case FACEVARYING: {
                int idx = tri << 1;
                float[] uvs = this.uvs.data;
                uv00 = uvs[idx + 0];
                uv01 = uvs[idx + 1];
                uv10 = uvs[idx + 2];
                uv11 = uvs[idx + 3];
                uv20 = uvs[idx + 4];
                uv21 = uvs[idx + 5];
                break;
            }
        }
        if (uvs.interp != InterpolationType.NONE) {
            // get exact uv coords and compute tangent vectors
            state.getUV().x = w * uv00 + u * uv10 + v * uv20;
            state.getUV().y = w * uv01 + u * uv11 + v * uv21;
            float du1 = uv00 - uv20;
            float du2 = uv10 - uv20;
            float dv1 = uv01 - uv21;
            float dv2 = uv11 - uv21;
            Vector3 dp1 = Point3.sub(v0p, v2p, new Vector3()), dp2 = Point3.sub(v1p, v2p, new Vector3());
            float determinant = du1 * dv2 - dv1 * du2;
            if (determinant == 0.0f) {
                // create basis in world space
                state.setBasis(OrthoNormalBasis.makeFromW(state.getNormal()));
            } else {
                float invdet = 1.f / determinant;
                // Vector3 dpdu = new Vector3();
                // dpdu.x = (dv2 * dp1.x - dv1 * dp2.x) * invdet;
                // dpdu.y = (dv2 * dp1.y - dv1 * dp2.y) * invdet;
                // dpdu.z = (dv2 * dp1.z - dv1 * dp2.z) * invdet;
                Vector3 dpdv = new Vector3();
                dpdv.x = (-du2 * dp1.x + du1 * dp2.x) * invdet;
                dpdv.y = (-du2 * dp1.y + du1 * dp2.y) * invdet;
                dpdv.z = (-du2 * dp1.z + du1 * dp2.z) * invdet;
                if (parent != null)
                    dpdv = parent.transformVectorObjectToWorld(dpdv);
                // create basis in world space
                state.setBasis(OrthoNormalBasis.makeFromWV(state.getNormal(), dpdv));
            }
        } else
            state.setBasis(OrthoNormalBasis.makeFromW(state.getNormal()));
        int shaderIndex = faceShaders == null ? 0 : (faceShaders[primID] & 0xFF);
        state.setShader(parent.getShader(shaderIndex));
    }

    public void init() {
        triaccel = null;
        int nt = getNumPrimitives();
        if (!smallTriangles) {
            // too many triangles? -- don't generate triaccel to save memory
            if (nt > 2000000) {
                UI.printWarning("[TRI] Too many triangles -- triaccel generation skipped");
                return;
            }
            triaccel = new WaldTriangle[nt];
            for (int i = 0; i < nt; i++)
                triaccel[i] = new WaldTriangle(this, i);
        }
    }

    protected Point3 getPoint(int i) {
        i *= 3;
        return new Point3(points[i], points[i + 1], points[i + 2]);
    }

    private static final class WaldTriangle {
        // private data for fast triangle intersection testing
        private int k;
        private float nu, nv, nd;
        private float bnu, bnv, bnd;
        private float cnu, cnv, cnd;

        private WaldTriangle(Mesh mesh, int tri) {
            k = 0;
            tri *= 3;
            int index0 = mesh.triangles[tri + 0];
            int index1 = mesh.triangles[tri + 1];
            int index2 = mesh.triangles[tri + 2];
            Point3 v0p = mesh.getPoint(index0);
            Point3 v1p = mesh.getPoint(index1);
            Point3 v2p = mesh.getPoint(index2);
            Vector3 ng = Point3.normal(v0p, v1p, v2p);
            if (Math.abs(ng.x) > Math.abs(ng.y) && Math.abs(ng.x) > Math.abs(ng.z))
                k = 0;
            else if (Math.abs(ng.y) > Math.abs(ng.z))
                k = 1;
            else
                k = 2;
            float ax, ay, bx, by, cx, cy;
            switch (k) {
                case 0: {
                    nu = ng.y / ng.x;
                    nv = ng.z / ng.x;
                    nd = v0p.x + (nu * v0p.y) + (nv * v0p.z);
                    ax = v0p.y;
                    ay = v0p.z;
                    bx = v2p.y - ax;
                    by = v2p.z - ay;
                    cx = v1p.y - ax;
                    cy = v1p.z - ay;
                    break;
                }
                case 1: {
                    nu = ng.z / ng.y;
                    nv = ng.x / ng.y;
                    nd = (nv * v0p.x) + v0p.y + (nu * v0p.z);
                    ax = v0p.z;
                    ay = v0p.x;
                    bx = v2p.z - ax;
                    by = v2p.x - ay;
                    cx = v1p.z - ax;
                    cy = v1p.x - ay;
                    break;
                }
                case 2:
                default: {
                    nu = ng.x / ng.z;
                    nv = ng.y / ng.z;
                    nd = (nu * v0p.x) + (nv * v0p.y) + v0p.z;
                    ax = v0p.x;
                    ay = v0p.y;
                    bx = v2p.x - ax;
                    by = v2p.y - ay;
                    cx = v1p.x - ax;
                    cy = v1p.y - ay;
                }
            }
            float det = bx * cy - by * cx;
            bnu = -by / det;
            bnv = bx / det;
            bnd = (by * ax - bx * ay) / det;
            cnu = cy / det;
            cnv = -cx / det;
            cnd = (cx * ay - cy * ax) / det;
        }

        void intersect(Ray r, int primID, IntersectionState state) {
            switch (k) {
                case 0: {
                    float det = 1.0f / (r.dx + nu * r.dy + nv * r.dz);
                    float t = (nd - r.ox - nu * r.oy - nv * r.oz) * det;
                    if (!r.isInside(t))
                        return;
                    float hu = r.oy + t * r.dy;
                    float hv = r.oz + t * r.dz;
                    float u = hu * bnu + hv * bnv + bnd;
                    if (u < 0.0f)
                        return;
                    float v = hu * cnu + hv * cnv + cnd;
                    if (v < 0.0f)
                        return;
                    if (u + v > 1.0f)
                        return;
                    r.setMax(t);
                    state.setIntersection(primID, u, v);
                    return;
                }
                case 1: {
                    float det = 1.0f / (r.dy + nu * r.dz + nv * r.dx);
                    float t = (nd - r.oy - nu * r.oz - nv * r.ox) * det;
                    if (!r.isInside(t))
                        return;
                    float hu = r.oz + t * r.dz;
                    float hv = r.ox + t * r.dx;
                    float u = hu * bnu + hv * bnv + bnd;
                    if (u < 0.0f)
                        return;
                    float v = hu * cnu + hv * cnv + cnd;
                    if (v < 0.0f)
                        return;
                    if (u + v > 1.0f)
                        return;
                    r.setMax(t);
                    state.setIntersection(primID, u, v);
                    return;
                }
                case 2: {
                    float det = 1.0f / (r.dz + nu * r.dx + nv * r.dy);
                    float t = (nd - r.oz - nu * r.ox - nv * r.oy) * det;
                    if (!r.isInside(t))
                        return;
                    float hu = r.ox + t * r.dx;
                    float hv = r.oy + t * r.dy;
                    float u = hu * bnu + hv * bnv + bnd;
                    if (u < 0.0f)
                        return;
                    float v = hu * cnu + hv * cnv + cnd;
                    if (v < 0.0f)
                        return;
                    if (u + v > 1.0f)
                        return;
                    r.setMax(t);
                    state.setIntersection(primID, u, v);
                    return;
                }
            }
        }
    }
}