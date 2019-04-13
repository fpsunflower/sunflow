package org.sunflow.core.parameter.geometry;

import org.sunflow.SunflowAPIInterface;

public class GenericMeshParameter extends GeometryParameter {

    float[] points;
    int[] triangles;
    float[] normals;
    float[] uvs;

    boolean faceVaryingNormals = false;
    boolean faceVaryingTextures = false;

    int[] faceShaders = null;

    @Override
    public void setup(SunflowAPIInterface api) {
        super.setup(api);
        api.parameter("points", "point", "vertex", points);
        api.parameter("triangles", triangles);

        if (!faceVaryingNormals) {
            api.parameter("normals", "vector", "vertex", normals);
        } else {
            api.parameter("normals", "vector", "facevarying", normals);
        }

        if (!faceVaryingTextures) {
            api.parameter("uvs", "texcoord", "vertex", uvs);
        } else {
            api.parameter("uvs", "texcoord", "facevarying", uvs);
        }

        if (faceShaders != null) {
            api.parameter("faceshaders", faceShaders);
        }

        api.geometry(name, TYPE_TRIANGLE_MESH);

        setupInstance(api);
    }

    public float[] getPoints() {
        return points;
    }

    public void setPoints(float[] points) {
        this.points = points;
    }

    public int[] getTriangles() {
        return triangles;
    }

    public void setTriangles(int[] triangles) {
        this.triangles = triangles;
    }

    public float[] getNormals() {
        return normals;
    }

    public void setNormals(float[] normals) {
        this.normals = normals;
    }

    public boolean isFaceVaryingNormals() {
        return faceVaryingNormals;
    }

    public void setFaceVaryingNormals(boolean faceVaryingNormals) {
        this.faceVaryingNormals = faceVaryingNormals;
    }

    public boolean isFaceVaryingTextures() {
        return faceVaryingTextures;
    }

    public void setFaceVaryingTextures(boolean faceVaryingTextures) {
        this.faceVaryingTextures = faceVaryingTextures;
    }

    public float[] getUvs() {
        return uvs;
    }

    public void setUvs(float[] uvs) {
        this.uvs = uvs;
    }

    public int[] getFaceShaders() {
        return faceShaders;
    }

    public void setFaceShaders(int[] faceShaders) {
        this.faceShaders = faceShaders;
    }
}
