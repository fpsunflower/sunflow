package org.sunflow.core.tesselatable;

import org.sunflow.SunflowAPI;
import org.sunflow.core.Instance;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.ParameterList;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Ray;
import org.sunflow.core.ShadingState;
import org.sunflow.core.Tesselatable;
import org.sunflow.core.ParameterList.InterpolationType;
import org.sunflow.core.primitive.Mesh;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class BezierMesh implements PrimitiveList, Tesselatable {
    private int subdivs;
    private boolean smooth;
    private float[][] patches;

    public BezierMesh(float[][] patches) {
        subdivs = 8;
        smooth = true;
        // convert to single precision
        this.patches = patches;
    }

    public BoundingBox getWorldBounds(Matrix4 o2w) {
        BoundingBox bounds = new BoundingBox();
        if (o2w == null) {
            for (int i = 0; i < patches.length; i++) {
                float[] patch = patches[i];
                for (int j = 0; j < patch.length; j += 3)
                    bounds.include(patch[j], patch[j + 1], patch[j + 2]);
            }
        } else {
            // transform vertices first
            for (int i = 0; i < patches.length; i++) {
                float[] patch = patches[i];
                for (int j = 0; j < patch.length; j += 3) {
                    float x = patch[j];
                    float y = patch[j + 1];
                    float z = patch[j + 2];
                    float wx = o2w.transformPX(x, y, z);
                    float wy = o2w.transformPY(x, y, z);
                    float wz = o2w.transformPZ(x, y, z);
                    bounds.include(wx, wy, wz);
                }
            }
        }
        return bounds;
    }

    private float[] bernstein(float u) {
        float[] b = new float[4];
        float i = 1 - u;
        b[0] = i * i * i;
        b[1] = 3 * u * i * i;
        b[2] = 3 * u * u * i;
        b[3] = u * u * u;
        return b;
    }

    private float[] bernsteinDeriv(float u) {
        if (!smooth)
            return null;
        float[] b = new float[4];
        float i = 1 - u;
        b[0] = 3 * (0 - i * i);
        b[1] = 3 * (i * i - 2 * u * i);
        b[2] = 3 * (2 * u * i - u * u);
        b[3] = 3 * (u * u - 0);
        return b;
    }

    private void getPatchPoint(float u, float v, float[] ctrl, float[] bu, float[] bv, float[] bdu, float[] bdv, Point3 p, Vector3 n) {
        float px = 0;
        float py = 0;
        float pz = 0;
        for (int i = 0, index = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++, index += 3) {
                float scale = bu[j] * bv[i];
                px += ctrl[index + 0] * scale;
                py += ctrl[index + 1] * scale;
                pz += ctrl[index + 2] * scale;
            }
        }
        p.x = px;
        p.y = py;
        p.z = pz;
        if (n != null) {
            float dpdux = 0;
            float dpduy = 0;
            float dpduz = 0;
            float dpdvx = 0;
            float dpdvy = 0;
            float dpdvz = 0;
            for (int i = 0, index = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++, index += 3) {
                    float scaleu = bdu[j] * bv[i];
                    dpdux += ctrl[index + 0] * scaleu;
                    dpduy += ctrl[index + 1] * scaleu;
                    dpduz += ctrl[index + 2] * scaleu;
                    float scalev = bu[j] * bdv[i];
                    dpdvx += ctrl[index + 0] * scalev;
                    dpdvy += ctrl[index + 1] * scalev;
                    dpdvz += ctrl[index + 2] * scalev;
                }
            }
            // surface normal
            n.x = (dpduy * dpdvz - dpduz * dpdvy);
            n.y = (dpduz * dpdvx - dpdux * dpdvz);
            n.z = (dpdux * dpdvy - dpduy * dpdvx);
        }
    }

    public PrimitiveList tesselate() {
        float[] vertices = new float[patches.length * (subdivs + 1) * (subdivs + 1) * 3];
        float[] normals = smooth ? new float[patches.length * (subdivs + 1) * (subdivs + 1) * 3] : null;
        float[] uvs = new float[patches.length * (subdivs + 1) * (subdivs + 1) * 2];
        int[] triangles = new int[patches.length * subdivs * subdivs * 2 * 3];

        int vidx = 0, tidx = 0;
        float step = 1.0f / subdivs;
        int vstride = subdivs + 1;
        Point3 p = new Point3();
        Vector3 n = smooth ? new Vector3() : null;
        for (float[] patch : patches) {
            // create patch vertices
            for (int i = 0, voff = 0; i <= subdivs; i++) {
                float u = i * step;
                float[] bu = bernstein(u);
                float[] bdu = bernsteinDeriv(u);
                for (int j = 0; j <= subdivs; j++, voff += 3) {
                    float v = j * step;
                    float[] bv = bernstein(v);
                    float[] bdv = bernsteinDeriv(v);
                    getPatchPoint(u, v, patch, bu, bv, bdu, bdv, p, n);
                    vertices[vidx + voff + 0] = p.x;
                    vertices[vidx + voff + 1] = p.y;
                    vertices[vidx + voff + 2] = p.z;
                    if (smooth) {
                        normals[vidx + voff + 0] = n.x;
                        normals[vidx + voff + 1] = n.y;
                        normals[vidx + voff + 2] = n.z;
                    }
                    uvs[(vidx + voff) / 3 * 2 + 0] = u;
                    uvs[(vidx + voff) / 3 * 2 + 1] = v;
                }
            }
            // generate patch triangles
            for (int i = 0, vbase = vidx / 3; i < subdivs; i++) {
                for (int j = 0; j < subdivs; j++) {
                    int v00 = (i + 0) * vstride + (j + 0);
                    int v10 = (i + 1) * vstride + (j + 0);
                    int v01 = (i + 0) * vstride + (j + 1);
                    int v11 = (i + 1) * vstride + (j + 1);
                    // add 2 triangles
                    triangles[tidx + 0] = vbase + v00;
                    triangles[tidx + 1] = vbase + v10;
                    triangles[tidx + 2] = vbase + v01;
                    triangles[tidx + 3] = vbase + v10;
                    triangles[tidx + 4] = vbase + v11;
                    triangles[tidx + 5] = vbase + v01;
                    tidx += 6;
                }
            }
            vidx += vstride * vstride * 3;
        }
        ParameterList pl = new ParameterList();
        pl.addPoints("points", InterpolationType.VERTEX, vertices);
        pl.addIntegerArray("triangles", triangles);
        pl.addTexCoords("uvs", InterpolationType.VERTEX, uvs);
        if (smooth)
            pl.addVectors("normals", InterpolationType.VERTEX, normals);
        Mesh m = new Mesh();
        m.update(pl, null);
        pl.clear(true);
        return m;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        subdivs = pl.getInt("subdivs", subdivs);
        smooth = pl.getBoolean("smooth", smooth);
        return subdivs > 0;
    }

    public int getNumPrimitives() {
        return patches.length;
    }

    public float getPrimitiveBound(int primID, int i) {
        float[] patch = patches[primID];
        int axis = i >>> 1;
        if ((i & 1) == 0) {
            float min = patch[axis];
            for (int j = axis + 3; j < patch.length; j += 3)
                if (min > patch[j])
                    min = patch[j];
            return min;
        } else {
            float max = patch[axis];
            for (int j = axis + 3; j < patch.length; j += 3)
                if (max < patch[j])
                    max = patch[j];
            return max;
        }
    }

    public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        // ray patch intersection
        final float[] stack = state.getRobustStack();
        {
            // init patch
            float[] patch = patches[primID];
            for (int i = 0; i < 4 * 4 * 3; i++)
                stack[i] = patch[i];
            stack[48] = Float.POSITIVE_INFINITY; // bbox size
            stack[49] = 0; // umin
            stack[50] = 0; // vmin
            stack[51] = 1; // umax
            stack[52] = 1; // vmax
        }
        // 53 elements per stack item
        int stackpos = 0;
        float orgX = r.ox;
        float dirX = r.dx, invDirX = 1 / dirX;
        float orgY = r.oy;
        float dirY = r.dy, invDirY = 1 / dirY;
        float orgZ = r.oz;
        float dirZ = r.dz, invDirZ = 1 / dirZ;
        float t1, t2;
        while (stackpos >= 0) {
            float intervalMin = r.getMin();
            float intervalMax = r.getMax();
            // x-axis bbox
            float minx = stack[stackpos + 0];
            float maxx = stack[stackpos + 0];
            for (int j = 1, idx = stackpos + 3; j < 4 * 4; j++, idx += 3) {
                if (minx > stack[idx])
                    minx = stack[idx];
                if (maxx < stack[idx])
                    maxx = stack[idx];
            }
            t1 = (minx - orgX) * invDirX;
            t2 = (maxx - orgX) * invDirX;
            if (invDirX > 0) {
                if (t1 > intervalMin)
                    intervalMin = t1;
                if (t2 < intervalMax)
                    intervalMax = t2;
            } else {
                if (t2 > intervalMin)
                    intervalMin = t2;
                if (t1 < intervalMax)
                    intervalMax = t1;
            }
            if (intervalMin > intervalMax) {
                stackpos -= 53;
                continue;
            }
            // y-axis bbox
            float miny = stack[stackpos + 1];
            float maxy = stack[stackpos + 1];
            for (int j = 1, idx = stackpos + 4; j < 4 * 4; j++, idx += 3) {
                if (miny > stack[idx])
                    miny = stack[idx];
                if (maxy < stack[idx])
                    maxy = stack[idx];
            }
            t1 = (miny - orgY) * invDirY;
            t2 = (maxy - orgY) * invDirY;
            if (invDirY > 0) {
                if (t1 > intervalMin)
                    intervalMin = t1;
                if (t2 < intervalMax)
                    intervalMax = t2;
            } else {
                if (t2 > intervalMin)
                    intervalMin = t2;
                if (t1 < intervalMax)
                    intervalMax = t1;
            }
            if (intervalMin > intervalMax) {
                stackpos -= 53;
                continue;
            }
            // z-axis bbox
            float minz = stack[stackpos + 2];
            float maxz = stack[stackpos + 2];
            for (int j = 1, idx = stackpos + 5; j < 4 * 4; j++, idx += 3) {
                if (minz > stack[idx])
                    minz = stack[idx];
                if (maxz < stack[idx])
                    maxz = stack[idx];
            }

            t1 = (minz - orgZ) * invDirZ;
            t2 = (maxz - orgZ) * invDirZ;
            if (invDirZ > 0) {
                if (t1 > intervalMin)
                    intervalMin = t1;
                if (t2 < intervalMax)
                    intervalMax = t2;
            } else {
                if (t2 > intervalMin)
                    intervalMin = t2;
                if (t1 < intervalMax)
                    intervalMax = t1;
            }
            if (intervalMin > intervalMax) {
                stackpos -= 53;
                continue;
            }
            // intersection was found - keep going
            float size = (maxx - minx) + (maxy - miny) + (maxz - minz);
            if (Float.floatToRawIntBits(stack[stackpos + 48]) == Float.floatToRawIntBits(size)) {
                // L1 norm is 0, we are done
                r.setMax(intervalMin);
                state.setIntersection(primID, stack[stackpos + 49], stack[stackpos + 50]);
                stackpos -= 53;
                continue;
            }
            // not small enough yet - subdivide

            // lets pick a subdivision axis first:

            float sizeu = 0;
            float sizev = 0;
            for (int i = 0; i < 3; i++) {
                sizeu += Math.abs(stack[stackpos + (0 * 4 + 3) * 3 + i] - stack[stackpos + i]);
                sizev += Math.abs(stack[stackpos + (3 * 4 + 0) * 3 + i] - stack[stackpos + i]);
            }

            if (sizeu > sizev) {
                // split in U direction
                for (int i = 0; i < 4; i++) {
                    for (int axis = 0; axis < 3; axis++) {
                        // load data
                        float p0 = stack[stackpos + (i * 4 + 0) * 3 + axis];
                        float p1 = stack[stackpos + (i * 4 + 1) * 3 + axis];
                        float p2 = stack[stackpos + (i * 4 + 2) * 3 + axis];
                        float p3 = stack[stackpos + (i * 4 + 3) * 3 + axis];

                        // Split curve in the middle
                        float q0 = p0;
                        float q1 = (p0 + p1) * 0.5f;
                        float q2 = q1 * 0.5f + (p1 + p2) * 0.25f;
                        float r3 = p3;
                        float r2 = (p2 + p3) * 0.5f;
                        float r1 = r2 * 0.5f + (p1 + p2) * 0.25f;
                        float q3 = (q2 + r1) * 0.5f;
                        float r0 = q3;

                        // load new curve data into the stack
                        stack[stackpos + (i * 4 + 0) * 3 + axis] = q0;
                        stack[stackpos + (i * 4 + 1) * 3 + axis] = q1;
                        stack[stackpos + (i * 4 + 2) * 3 + axis] = q2;
                        stack[stackpos + (i * 4 + 3) * 3 + axis] = q3;

                        stack[stackpos + 53 + (i * 4 + 0) * 3 + axis] = r0;
                        stack[stackpos + 53 + (i * 4 + 1) * 3 + axis] = r1;
                        stack[stackpos + 53 + (i * 4 + 2) * 3 + axis] = r2;
                        stack[stackpos + 53 + (i * 4 + 3) * 3 + axis] = r3;
                    }
                }
                // copy current bbox size
                stack[stackpos + 48] = stack[stackpos + 53 + 48] = size;
                // finally - split uv ranges
                float umin = stack[stackpos + 49];
                float umax = stack[stackpos + 51];
                stack[stackpos + 49] = umin;
                stack[stackpos + 53 + 50] = stack[stackpos + 50];
                stack[stackpos + 51] = stack[stackpos + 53 + 49] = (umin + umax) * 0.5f;
                stack[stackpos + 53 + 51] = umax;
                stack[stackpos + 53 + 52] = stack[stackpos + 52];
            } else {
                // split in V direction
                for (int i = 0; i < 4; i++) {
                    for (int axis = 0; axis < 3; axis++) {
                        // load data
                        float p0 = stack[stackpos + (0 * 4 + i) * 3 + axis];
                        float p1 = stack[stackpos + (1 * 4 + i) * 3 + axis];
                        float p2 = stack[stackpos + (2 * 4 + i) * 3 + axis];
                        float p3 = stack[stackpos + (3 * 4 + i) * 3 + axis];

                        // Split curve in the middle
                        float q0 = p0;
                        float q1 = (p0 + p1) * 0.5f;
                        float q2 = q1 * 0.5f + (p1 + p2) * 0.25f;
                        float r3 = p3;
                        float r2 = (p2 + p3) * 0.5f;
                        float r1 = r2 * 0.5f + (p1 + p2) * 0.25f;
                        float q3 = (q2 + r1) * 0.5f;
                        float r0 = q3;

                        // load new curve data into the stack
                        stack[stackpos + (0 * 4 + i) * 3 + axis] = q0;
                        stack[stackpos + (1 * 4 + i) * 3 + axis] = q1;
                        stack[stackpos + (2 * 4 + i) * 3 + axis] = q2;
                        stack[stackpos + (3 * 4 + i) * 3 + axis] = q3;

                        stack[stackpos + 53 + (0 * 4 + i) * 3 + axis] = r0;
                        stack[stackpos + 53 + (1 * 4 + i) * 3 + axis] = r1;
                        stack[stackpos + 53 + (2 * 4 + i) * 3 + axis] = r2;
                        stack[stackpos + 53 + (3 * 4 + i) * 3 + axis] = r3;
                    }
                }
                // copy current bbox size
                stack[stackpos + 48] = stack[stackpos + 53 + 48] = size;
                // finally - split uv ranges
                float vmin = stack[stackpos + 50];
                float vmax = stack[stackpos + 52];
                stack[stackpos + 53 + 49] = stack[stackpos + 49];
                stack[stackpos + 50] = vmin;
                stack[stackpos + 52] = stack[stackpos + 53 + 50] = (vmin + vmax) * 0.5f;
                stack[stackpos + 53 + 51] = stack[stackpos + 51];
                stack[stackpos + 53 + 52] = vmax;
            }
            stackpos += 53;
        }
    }

    public void prepareShadingState(ShadingState state) {
        state.init();
        state.getRay().getPoint(state.getPoint());
        Instance parent = state.getInstance();
        float u = state.getU();
        float v = state.getV();
        float[] bu = bernstein(u);
        float[] bdu = bernsteinDeriv(u);
        float[] bv = bernstein(v);
        float[] bdv = bernsteinDeriv(v);
        getPatchPoint(u, v, patches[state.getPrimitiveID()], bu, bv, bdu, bdv, new Point3(), state.getNormal());
        state.getNormal().set(parent.transformNormalObjectToWorld(state.getNormal()));
        state.getNormal().normalize();
        state.getGeoNormal().set(state.getNormal());
        state.getUV().set(u, v);
        state.setShader(parent.getShader(0));
        // FIXME: use actual derivatives to create basis
        state.setBasis(OrthoNormalBasis.makeFromW(state.getNormal()));
    }
}