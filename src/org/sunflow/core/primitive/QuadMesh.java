package org.sunflow.core.primitive;

import java.io.FileWriter;
import java.io.IOException;

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
import org.sunflow.system.UI.Module;

public class QuadMesh implements PrimitiveList {
    protected float[] points;
    protected int[] quads;
    private FloatParameter normals;
    private FloatParameter uvs;
    private byte[] faceShaders;

    public QuadMesh() {
        quads = null;
        points = null;
        normals = uvs = new FloatParameter();
        faceShaders = null;
    }

    public void writeObj(String filename) {
        try {
            FileWriter file = new FileWriter(filename);
            file.write(String.format("o object\n"));
            for (int i = 0; i < points.length; i += 3)
                file.write(String.format("v %g %g %g\n", points[i], points[i + 1], points[i + 2]));
            file.write("s off\n");
            for (int i = 0; i < quads.length; i += 3)
                file.write(String.format("f %d %d %d\n", quads[i] + 1, quads[i + 1] + 1, quads[i + 2] + 1));
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        {
            int[] quads = pl.getIntArray("quads");
            if (quads != null) {
                this.quads = quads;
            }
        }
        if (quads == null) {
            UI.printError(Module.GEOM, "Unable to update mesh - quad indices are missing");
            return false;
        }
        if (quads.length % 4 != 0)
            UI.printWarning(Module.GEOM, "Quad index data is not a multiple of 4 - some quads may be missing");
        pl.setFaceCount(quads.length / 4);
        {
            FloatParameter pointsP = pl.getPointArray("points");
            if (pointsP != null)
                if (pointsP.interp != InterpolationType.VERTEX)
                    UI.printError(Module.GEOM, "Point interpolation type must be set to \"vertex\" - was \"%s\"", pointsP.interp.name().toLowerCase());
                else {
                    points = pointsP.data;
                }
        }
        if (points == null) {
            UI.printError(Module.GEOM, "Unabled to update mesh - vertices are missing");
            return false;
        }
        pl.setVertexCount(points.length / 3);
        pl.setFaceVertexCount(4 * (quads.length / 4));
        FloatParameter normals = pl.getVectorArray("normals");
        if (normals != null)
            this.normals = normals;
        FloatParameter uvs = pl.getTexCoordArray("uvs");
        if (uvs != null)
            this.uvs = uvs;
        int[] faceShaders = pl.getIntArray("faceshaders");
        if (faceShaders != null && faceShaders.length == quads.length / 4) {
            this.faceShaders = new byte[faceShaders.length];
            for (int i = 0; i < faceShaders.length; i++) {
                int v = faceShaders[i];
                if (v > 255)
                    UI.printWarning(Module.GEOM, "Shader index too large on quad %d", i);
                this.faceShaders[i] = (byte) (v & 0xFF);
            }
        }
        return true;
    }

    public float getPrimitiveBound(int primID, int i) {
        int quad = 4 * primID;
        int a = 3 * quads[quad + 0];
        int b = 3 * quads[quad + 1];
        int c = 3 * quads[quad + 2];
        int d = 3 * quads[quad + 3];
        int axis = i >>> 1;
        if ((i & 1) == 0)
            return MathUtils.min(points[a + axis], points[b + axis], points[c + axis], points[d + axis]);
        else
            return MathUtils.max(points[a + axis], points[b + axis], points[c + axis], points[d + axis]);
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

    private final void intersectTriangleKensler(Ray r, int primID, int a, int b, int c, IntersectionState state) {
        float edge0x = points[b + 0] - points[a + 0];
        float edge0y = points[b + 1] - points[a + 1];
        float edge0z = points[b + 2] - points[a + 2];
        float edge1x = points[a + 0] - points[c + 0];
        float edge1y = points[a + 1] - points[c + 1];
        float edge1z = points[a + 2] - points[c + 2];
        float nx = edge0y * edge1z - edge0z * edge1y;
        float ny = edge0z * edge1x - edge0x * edge1z;
        float nz = edge0x * edge1y - edge0y * edge1x;
        float v = r.dot(nx, ny, nz);
        float iv = 1 / v;
        float edge2x = points[a + 0] - r.ox;
        float edge2y = points[a + 1] - r.oy;
        float edge2z = points[a + 2] - r.oz;
        float va = nx * edge2x + ny * edge2y + nz * edge2z;
        float t = iv * va;
        if (!r.isInside(t))
            return;
        float ix = edge2y * r.dz - edge2z * r.dy;
        float iy = edge2z * r.dx - edge2x * r.dz;
        float iz = edge2x * r.dy - edge2y * r.dx;
        float v1 = ix * edge1x + iy * edge1y + iz * edge1z;
        float beta = iv * v1;
        if (beta < 0)
            return;
        float v2 = ix * edge0x + iy * edge0y + iz * edge0z;
        if ((v1 + v2) * v > v * v)
            return;
        float gamma = iv * v2;
        if (gamma < 0)
            return;
        r.setMax(t);
        state.setIntersection(primID, beta, gamma);
    }

    public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        int quad = 4 * primID;
        int a = 3 * quads[quad + 0];
        int b = 3 * quads[quad + 1];
        int c = 3 * quads[quad + 2];
        int d = 3 * quads[quad + 3];
        intersectTriangleKensler(r, 2 * primID + 0, a, b, c, state);
        intersectTriangleKensler(r, 2 * primID + 1, a, c, d, state);
    }

    public int getNumPrimitives() {
        return quads.length / 4;
    }

    public void prepareShadingState(ShadingState state) {
        state.init();
        Instance parent = state.getInstance();
        int primID = state.getPrimitiveID() / 2;
        boolean even = (state.getPrimitiveID() & 1) == 0;
        float u = state.getU();
        float v = state.getV();
        float w = 1 - u - v;
        state.getRay().getPoint(state.getPoint());
        int quad = 4 * primID;
        int index0 = quads[quad + 0];
        int index1 = quads[quad + (even ? 1 : 2)];
        int index2 = quads[quad + (even ? 2 : 3)];
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
                int idx = 3 * quad;
                float[] normals = this.normals.data;
                state.getNormal().x = w * normals[idx + 0] + u * normals[idx + (even ? 3 : 6)] + v * normals[idx + (even ? 6 : 9)];
                state.getNormal().y = w * normals[idx + 1] + u * normals[idx + (even ? 4 : 7)] + v * normals[idx + (even ? 7 : 10)];
                state.getNormal().z = w * normals[idx + 2] + u * normals[idx + (even ? 5 : 8)] + v * normals[idx + (even ? 8 : 11)];
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
                int idx = quad << 1;
                float[] uvs = this.uvs.data;
                uv00 = uvs[idx + 0];
                uv01 = uvs[idx + 1];
                uv10 = uvs[idx + (even ? 2 : 4)];
                uv11 = uvs[idx + (even ? 3 : 5)];
                uv20 = uvs[idx + (even ? 4 : 6)];
                uv21 = uvs[idx + (even ? 5 : 7)];
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

    protected Point3 getPoint(int i) {
        i *= 3;
        return new Point3(points[i], points[i + 1], points[i + 2]);
    }

    public PrimitiveList getBakingPrimitives() {
        switch (uvs.interp) {
            case NONE:
            case FACE:
                UI.printError(Module.GEOM, "Cannot generate baking surface without texture coordinate data");
                return null;
            default:
                return new BakingSurface();
        }
    }

    private class BakingSurface implements PrimitiveList {
        public PrimitiveList getBakingPrimitives() {
            return null;
        }

        public int getNumPrimitives() {
            return QuadMesh.this.getNumPrimitives();
        }

        public float getPrimitiveBound(int primID, int i) {
            if (i > 3)
                return 0;
            switch (uvs.interp) {
                case NONE:
                case FACE:
                default: {
                    return 0;
                }
                case VERTEX: {
                    int tri = 3 * primID;
                    int index0 = quads[tri + 0];
                    int index1 = quads[tri + 1];
                    int index2 = quads[tri + 2];
                    int i20 = 2 * index0;
                    int i21 = 2 * index1;
                    int i22 = 2 * index2;
                    float[] uvs = QuadMesh.this.uvs.data;
                    switch (i) {
                        case 0:
                            return MathUtils.min(uvs[i20 + 0], uvs[i21 + 0], uvs[i22 + 0]);
                        case 1:
                            return MathUtils.max(uvs[i20 + 0], uvs[i21 + 0], uvs[i22 + 0]);
                        case 2:
                            return MathUtils.min(uvs[i20 + 1], uvs[i21 + 1], uvs[i22 + 1]);
                        case 3:
                            return MathUtils.max(uvs[i20 + 1], uvs[i21 + 1], uvs[i22 + 1]);
                        default:
                            return 0;
                    }
                }
                case FACEVARYING: {
                    int idx = 6 * primID;
                    float[] uvs = QuadMesh.this.uvs.data;
                    switch (i) {
                        case 0:
                            return MathUtils.min(uvs[idx + 0], uvs[idx + 2], uvs[idx + 4]);
                        case 1:
                            return MathUtils.max(uvs[idx + 0], uvs[idx + 2], uvs[idx + 4]);
                        case 2:
                            return MathUtils.min(uvs[idx + 1], uvs[idx + 3], uvs[idx + 5]);
                        case 3:
                            return MathUtils.max(uvs[idx + 1], uvs[idx + 3], uvs[idx + 5]);
                        default:
                            return 0;
                    }
                }
            }
        }

        public BoundingBox getWorldBounds(Matrix4 o2w) {
            BoundingBox bounds = new BoundingBox();
            if (o2w == null) {
                for (int i = 0; i < uvs.data.length; i += 2)
                    bounds.include(uvs.data[i], uvs.data[i + 1], 0);
            } else {
                // transform vertices first
                for (int i = 0; i < uvs.data.length; i += 2) {
                    float x = uvs.data[i];
                    float y = uvs.data[i + 1];
                    float wx = o2w.transformPX(x, y, 0);
                    float wy = o2w.transformPY(x, y, 0);
                    float wz = o2w.transformPZ(x, y, 0);
                    bounds.include(wx, wy, wz);
                }
            }
            return bounds;
        }

        public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
            float uv00 = 0, uv01 = 0, uv10 = 0, uv11 = 0, uv20 = 0, uv21 = 0;
            switch (uvs.interp) {
                case NONE:
                case FACE:
                default:
                    return;
                case VERTEX: {
                    int tri = 3 * primID;
                    int index0 = quads[tri + 0];
                    int index1 = quads[tri + 1];
                    int index2 = quads[tri + 2];
                    int i20 = 2 * index0;
                    int i21 = 2 * index1;
                    int i22 = 2 * index2;
                    float[] uvs = QuadMesh.this.uvs.data;
                    uv00 = uvs[i20 + 0];
                    uv01 = uvs[i20 + 1];
                    uv10 = uvs[i21 + 0];
                    uv11 = uvs[i21 + 1];
                    uv20 = uvs[i22 + 0];
                    uv21 = uvs[i22 + 1];
                    break;

                }
                case FACEVARYING: {
                    int idx = (3 * primID) << 1;
                    float[] uvs = QuadMesh.this.uvs.data;
                    uv00 = uvs[idx + 0];
                    uv01 = uvs[idx + 1];
                    uv10 = uvs[idx + 2];
                    uv11 = uvs[idx + 3];
                    uv20 = uvs[idx + 4];
                    uv21 = uvs[idx + 5];
                    break;
                }
            }

            double edge1x = uv10 - uv00;
            double edge1y = uv11 - uv01;
            double edge2x = uv20 - uv00;
            double edge2y = uv21 - uv01;
            double pvecx = r.dy * 0 - r.dz * edge2y;
            double pvecy = r.dz * edge2x - r.dx * 0;
            double pvecz = r.dx * edge2y - r.dy * edge2x;
            double qvecx, qvecy, qvecz;
            double u, v;
            double det = edge1x * pvecx + edge1y * pvecy + 0 * pvecz;
            if (det > 0) {
                double tvecx = r.ox - uv00;
                double tvecy = r.oy - uv01;
                double tvecz = r.oz;
                u = (tvecx * pvecx + tvecy * pvecy + tvecz * pvecz);
                if (u < 0.0 || u > det)
                    return;
                qvecx = tvecy * 0 - tvecz * edge1y;
                qvecy = tvecz * edge1x - tvecx * 0;
                qvecz = tvecx * edge1y - tvecy * edge1x;
                v = (r.dx * qvecx + r.dy * qvecy + r.dz * qvecz);
                if (v < 0.0 || u + v > det)
                    return;
            } else if (det < 0) {
                double tvecx = r.ox - uv00;
                double tvecy = r.oy - uv01;
                double tvecz = r.oz;
                u = (tvecx * pvecx + tvecy * pvecy + tvecz * pvecz);
                if (u > 0.0 || u < det)
                    return;
                qvecx = tvecy * 0 - tvecz * edge1y;
                qvecy = tvecz * edge1x - tvecx * 0;
                qvecz = tvecx * edge1y - tvecy * edge1x;
                v = (r.dx * qvecx + r.dy * qvecy + r.dz * qvecz);
                if (v > 0.0 || u + v < det)
                    return;
            } else
                return;
            double inv_det = 1.0 / det;
            float t = (float) ((edge2x * qvecx + edge2y * qvecy + 0 * qvecz) * inv_det);
            if (r.isInside(t)) {
                r.setMax(t);
                state.setIntersection(primID, (float) (u * inv_det), (float) (v * inv_det));
            }
        }

        public void prepareShadingState(ShadingState state) {
            state.init();
            Instance parent = state.getInstance();
            int primID = state.getPrimitiveID();
            float u = state.getU();
            float v = state.getV();
            float w = 1 - u - v;
            // state.getRay().getPoint(state.getPoint());
            int tri = 3 * primID;
            int index0 = quads[tri + 0];
            int index1 = quads[tri + 1];
            int index2 = quads[tri + 2];
            Point3 v0p = getPoint(index0);
            Point3 v1p = getPoint(index1);
            Point3 v2p = getPoint(index2);

            // get object space point from barycentric coordinates
            state.getPoint().x = w * v0p.x + u * v1p.x + v * v2p.x;
            state.getPoint().y = w * v0p.y + u * v1p.y + v * v2p.y;
            state.getPoint().z = w * v0p.z + u * v1p.z + v * v2p.z;
            // move into world space
            state.getPoint().set(parent.transformObjectToWorld(state.getPoint()));

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
                    float[] normals = QuadMesh.this.normals.data;
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
                    float[] normals = QuadMesh.this.normals.data;
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
                    float[] uvs = QuadMesh.this.uvs.data;
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
                    float[] uvs = QuadMesh.this.uvs.data;
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

        public boolean update(ParameterList pl, SunflowAPI api) {
            return true;
        }
    }
}