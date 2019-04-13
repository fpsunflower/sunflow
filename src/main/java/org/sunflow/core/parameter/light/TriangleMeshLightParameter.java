package org.sunflow.core.parameter.light;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.image.Color;

public class TriangleMeshLightParameter extends LightParameter {

    public static final String PARAM_POINTS = "points";
    public static final String PARAM_TRIANGLES = "triangles";

    int samples;
    Color radiance;
    float[] points;
    int[] triangles;

    public TriangleMeshLightParameter() {
        generateUniqueName("meshlight");
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter(PARAM_RADIANCE, null, radiance.getRGB());
        api.parameter(PARAM_SAMPLES, samples);
        api.parameter(PARAM_POINTS, "point", "vertex", points);
        api.parameter(PARAM_TRIANGLES, triangles);
        api.light(name, TYPE_TRIANGLE_MESH);
    }

    public Color getRadiance() {
        return radiance;
    }

    public void setRadiance(Color radiance) {
        this.radiance = radiance;
    }

    public int getSamples() {
        return samples;
    }

    public void setSamples(int samples) {
        this.samples = samples;
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
}
