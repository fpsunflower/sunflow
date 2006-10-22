package org.sunflow.core.tesselatable;

import org.sunflow.SunflowAPI;
import org.sunflow.core.ParameterList;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Tesselatable;
import org.sunflow.core.ParameterList.InterpolationType;
import org.sunflow.core.primitive.Mesh;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public abstract class BezierMesh implements Tesselatable {
    private int subdivs;
    private boolean smooth;
    
    public BezierMesh() {
        subdivs = 8;
        smooth = true;
    }
    
    protected abstract double[][] getPatches();

    public BoundingBox getWorldBounds(Matrix4 o2w) {
        double[][] patchData = getPatches();
        BoundingBox bounds = new BoundingBox();
        if (o2w == null) {
            for (int i = 0; i < patchData.length; i++) {
                double[] patch = patchData[i];
                for (int j = 0; j < patch.length; j += 3)
                    bounds.include((float) patch[j], (float) patch[j + 1], (float) patch[j + 2]);
            }
        } else {
            // transform vertices first
            for (int i = 0; i < patchData.length; i++) {
                double[] patch = patchData[i];
                for (int j = 0; j < patch.length; j += 3) {
                    float x = (float) patch[j];
                    float y = (float) patch[j + 1];
                    float z = (float) patch[j + 2];
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

    private void getPatchPoint(float u, float v, double[] ctrl, float[] bu, float[] bv, float[] bdu, float[] bdv, Point3 p, Vector3 n) {
        double px = 0;
        double py = 0;
        double pz = 0;
        for (int i = 0, index = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++, index += 3) {
                float scale = bu[j] * bv[i];
                px += ctrl[index + 0] * scale;
                py += ctrl[index + 1] * scale;
                pz += ctrl[index + 2] * scale;
            }
        }
        p.x = (float) px;
        p.y = (float) py;
        p.z = (float) pz;
        if (n != null) {
            double dpdux = 0;
            double dpduy = 0;
            double dpduz = 0;
            double dpdvx = 0;
            double dpdvy = 0;
            double dpdvz = 0;
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
            n.x = (float) (dpduy * dpdvz - dpduz * dpdvy);
            n.y = (float) (dpduz * dpdvx - dpdux * dpdvz);
            n.z = (float) (dpdux * dpdvy - dpduy * dpdvx);
        }
    }

    public PrimitiveList tesselate() {
        double[][] patchData = getPatches();
        float[] vertices = new float[patchData.length * (subdivs + 1) * (subdivs + 1) * 3];
        float[] normals = smooth ? new float[patchData.length * (subdivs + 1) * (subdivs + 1) * 3] : null;
        float[] uvs = new float[patchData.length * (subdivs + 1) * (subdivs + 1) * 2];
        int[] triangles = new int[patchData.length * subdivs * subdivs * 2 * 3];

        int vidx = 0, tidx = 0;
        float step = 1.0f / subdivs;
        int vstride = subdivs + 1;
        Point3 p = new Point3();
        Vector3 n = smooth ? new Vector3() : null;
        for (double[] patch : patchData) {
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
}