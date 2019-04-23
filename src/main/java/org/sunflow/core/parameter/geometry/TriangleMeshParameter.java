package org.sunflow.core.parameter.geometry;

import org.sunflow.SunflowAPIInterface;

public class TriangleMeshParameter extends GeometryParameter {

    float[] points;
    float[] normals;
    float[] uvs;
    int[] triangles;

    @Override
    public void setup(SunflowAPIInterface api) {
        super.setup(api);
        if (name == null || name.isEmpty()) {
            throw new RuntimeException("Name cannot be null");
        }
        // create geometry
        api.parameter("triangles", triangles);
        api.parameter("points", "point", "vertex", points);
        if (normals != null) {
            api.parameter("normals", "vector", "vertex", normals);
        }
        if (uvs != null) {
            api.parameter("uvs", "texcoord", "vertex", uvs);
        }
        api.geometry(name, "triangle_mesh");

        setupInstance(api);
    }

    public float[] getPoints() {
        return points;
    }

    public void setPoints(float[] points) {
        this.points = points;
    }

    public float[] getNormals() {
        return normals;
    }

    public void setNormals(float[] normals) {
        this.normals = normals;
    }

    public float[] getUvs() {
        return uvs;
    }

    public void setUvs(float[] uvs) {
        this.uvs = uvs;
    }

    public int[] getTriangles() {
        return triangles;
    }

    public void setTriangles(int[] triangles) {
        this.triangles = triangles;
    }
}
