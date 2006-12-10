package org.sunflow.core.light;

import org.sunflow.SunflowAPI;
import org.sunflow.core.LightSample;
import org.sunflow.core.LightSource;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Ray;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.core.primitive.Mesh;
import org.sunflow.image.Color;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class MeshLight extends Mesh implements Shader {
    private Color radiance;
    private int numSamples;

    public MeshLight() {
        radiance = Color.WHITE;
        numSamples = 4;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        radiance = pl.getColor("radiance", radiance);
        numSamples = pl.getInt("samples", numSamples);
        return super.update(pl, api);
    }

    public void init(String name, SunflowAPI api) {
        api.geometry(name, this);
        api.shader(name + ".shader", this);
        api.parameter("shaders", name + ".shader");
        api.instance(name + ".instance", name);
        for (int i = 0, j = 0; i < triangles.length; i += 3, j++) {
            TriangleLight t = new TriangleLight(j);
            String lname = String.format("%s.light[%d]", name, j);
            api.light(lname, t);
        }
    }

    private class TriangleLight implements LightSource {
        private int tri3;
        private float area;
        private Vector3 ng;

        TriangleLight(int tri) {
            tri3 = 3 * tri;
            int a, b, c;
            if (triangles == null) {
                // implicit indexing
                a = tri3 + 0;
                b = tri3 + 1;
                c = tri3 + 2;
            } else {
                // explicit indexing
                a = triangles[tri3 + 0];
                b = triangles[tri3 + 1];
                c = triangles[tri3 + 2];
            }
            Point3 v0p = getPoint(a);
            Point3 v1p = getPoint(b);
            Point3 v2p = getPoint(c);
            ng = Point3.normal(v0p, v1p, v2p);
            area = 0.5f * ng.length();
            ng.normalize();
        }

        public boolean update(ParameterList pl, SunflowAPI api) {
            return true;
        }

        public boolean isAdaptive() {
            return true;
        }

        public int getNumSamples() {
            return numSamples;
        }

        public boolean isVisible(ShadingState state) {
            Point3 p = state.getPoint();
            Vector3 n = state.getNormal();
            Vector3 sub = new Vector3();
            int a, b, c;
            if (triangles == null) {
                // implicit indexing
                a = tri3 + 0;
                b = tri3 + 1;
                c = tri3 + 2;
            } else {
                // explicit indexing
                a = triangles[tri3 + 0];
                b = triangles[tri3 + 1];
                c = triangles[tri3 + 2];
            }
            Point3 v0p = getPoint(a);
            Point3.sub(v0p, p, sub);
            if ((Vector3.dot(sub, n) > 0.0) || (Vector3.dot(sub, ng) < 0.0))
                return true;
            Point3 v1p = getPoint(b);
            Point3.sub(v1p, p, sub);
            if ((Vector3.dot(sub, n) > 0.0) || (Vector3.dot(sub, ng) < 0.0))
                return true;
            Point3 v2p = getPoint(c);
            Point3.sub(v2p, p, sub);
            if ((Vector3.dot(sub, n) > 0.0) || (Vector3.dot(sub, ng) < 0.0))
                return true;
            return false;
        }

        public void getSample(int i, ShadingState state, LightSample dest) {
            // random offset on unit square
            double randX = state.getRandom(i, 0);
            double randY = state.getRandom(i, 1);

            float s = (float) Math.sqrt(1 - randX);
            float u = (float) randY * s;
            float v = 1 - s;
            float w = 1 - u - v;

            Point3 p = new Point3();
            int a, b, c;
            if (triangles == null) {
                // implicit indexing
                a = tri3 + 0;
                b = tri3 + 1;
                c = tri3 + 2;
            } else {
                // explicit indexing
                a = triangles[tri3 + 0];
                b = triangles[tri3 + 1];
                c = triangles[tri3 + 2];
            }
            int index0 = 3 * a;
            int index1 = 3 * b;
            int index2 = 3 * c;
            p.x = w * points[index0 + 0] + u * points[index1 + 0] + v * points[index2 + 0];
            p.y = w * points[index0 + 1] + u * points[index1 + 1] + v * points[index2 + 1];
            p.z = w * points[index0 + 2] + u * points[index1 + 2] + v * points[index2 + 2];
            p.x += 0.001f * ng.x;
            p.y += 0.001f * ng.y;
            p.z += 0.001f * ng.z;

            // compute shadow ray to the sampled point
            dest.setShadowRay(new Ray(state.getPoint(), p));

            // check that the direction of the sample is the same as the
            // normal
            float cosNx = dest.dot(state.getNormal());
            if (cosNx <= 0)
                return;

            // light source facing point ?
            // (need to check with light source's normal)
            float cosNy = -dest.dot(ng);
            if (cosNy > 0) {
                // prepare sample
                float g = cosNy / state.getPoint().distanceToSquared(p);
                float scale = g * area;
                dest.setRadiance(radiance, radiance);
                dest.getDiffuseRadiance().mul(scale);
                dest.getSpecularRadiance().mul(scale);
                dest.traceShadow(state);
            }
        }

        public void getPhoton(double randX1, double randY1, double randX2, double randY2, Point3 p, Vector3 dir, Color power) {
            double s = Math.sqrt(1 - randX2);
            float u = (float) (randY2 * s);
            float v = (float) (1 - s);
            float w = 1 - u - v;
            int a, b, c;
            if (triangles == null) {
                // implicit indexing
                a = tri3 + 0;
                b = tri3 + 1;
                c = tri3 + 2;
            } else {
                // explicit indexing
                a = triangles[tri3 + 0];
                b = triangles[tri3 + 1];
                c = triangles[tri3 + 2];
            }
            int index0 = 3 * a;
            int index1 = 3 * b;
            int index2 = 3 * c;
            p.x = w * points[index0 + 0] + u * points[index1 + 0] + v * points[index2 + 0];
            p.y = w * points[index0 + 1] + u * points[index1 + 1] + v * points[index2 + 1];
            p.z = w * points[index0 + 2] + u * points[index1 + 2] + v * points[index2 + 2];
            p.x += 0.001f * ng.x;
            p.y += 0.001f * ng.y;
            p.z += 0.001f * ng.z;
            OrthoNormalBasis onb = OrthoNormalBasis.makeFromW(ng);
            u = (float) (2 * Math.PI * randX1);
            s = Math.sqrt(randY1);
            onb.transform(new Vector3((float) (Math.cos(u) * s), (float) (Math.sin(u) * s), (float) (Math.sqrt(1 - randY1))), dir);
            Color.mul((float) Math.PI * area, radiance, power);
        }

        public float getPower() {
            return radiance.copy().mul((float) Math.PI * area).getLuminance();
        }
    }

    public Color getRadiance(ShadingState state) {
        if (!state.includeLights())
            return Color.BLACK;
        state.faceforward();
        // emit constant radiance
        return state.isBehind() ? Color.BLACK : radiance;
    }

    public void scatterPhoton(ShadingState state, Color power) {
        // do not scatter photons
    }
}