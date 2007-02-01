package org.sunflow.core.light;

import org.sunflow.SunflowAPI;
import org.sunflow.core.LightSample;
import org.sunflow.core.LightSource;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Ray;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.core.primitive.TriangleMesh;
import org.sunflow.image.Color;
import org.sunflow.math.MathUtils;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class TriangleMeshLight extends TriangleMesh implements Shader {
    private Color radiance;
    private int numSamples;

    public TriangleMeshLight() {
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
            int a = triangles[tri3 + 0];
            int b = triangles[tri3 + 1];
            int c = triangles[tri3 + 2];
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

        public int getNumSamples() {
            return numSamples;
        }

        private final boolean intersectTriangleKensler(Ray r) {
            int a = 3 * triangles[tri3 + 0];
            int b = 3 * triangles[tri3 + 1];
            int c = 3 * triangles[tri3 + 2];
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
            if (t <= 0)
                return false;
            float ix = edge2y * r.dz - edge2z * r.dy;
            float iy = edge2z * r.dx - edge2x * r.dz;
            float iz = edge2x * r.dy - edge2y * r.dx;
            float v1 = ix * edge1x + iy * edge1y + iz * edge1z;
            float beta = iv * v1;
            if (beta < 0)
                return false;
            float v2 = ix * edge0x + iy * edge0y + iz * edge0z;
            if ((v1 + v2) * v > v * v)
                return false;
            float gamma = iv * v2;
            if (gamma < 0)
                return false;
            // FIXME: arbitrary bias, should handle as in other places
            r.setMax(t - 1e-3f);
            return true;
        }

        public void getSamples(ShadingState state) {
            if (numSamples == 0)
                return;
            Vector3 n = state.getNormal();
            Point3 p = state.getPoint();
            // vector towards each vertex of the light source
            Vector3 p0 = Point3.sub(getPoint(triangles[tri3 + 0]), p, new Vector3());
            // cull triangle if it is facing the wrong way
            if (Vector3.dot(p0, ng) >= 0)
                return;
            Vector3 p1 = Point3.sub(getPoint(triangles[tri3 + 1]), p, new Vector3());
            Vector3 p2 = Point3.sub(getPoint(triangles[tri3 + 2]), p, new Vector3());
            // if all three vertices are below the hemisphere, stop
            if (Vector3.dot(p0, n) <= 0 && Vector3.dot(p1, n) <= 0 && Vector3.dot(p2, n) <= 0)
                return;
            p0.normalize();
            p1.normalize();
            p2.normalize();
            float dot = Vector3.dot(p2, p0);
            Vector3 h = new Vector3();
            h.x = p2.x - dot * p0.x;
            h.y = p2.y - dot * p0.y;
            h.z = p2.z - dot * p0.z;
            float hlen = h.length();
            if (hlen > 1e-6f)
                h.div(hlen);
            else
                return;
            Vector3 n0 = Vector3.cross(p0, p1, new Vector3());
            float len0 = n0.length();
            if (len0 > 1e-6f)
                n0.div(len0);
            else
                return;
            Vector3 n1 = Vector3.cross(p1, p2, new Vector3());
            float len1 = n1.length();
            if (len1 > 1e-6f)
                n1.div(len1);
            else
                return;
            Vector3 n2 = Vector3.cross(p2, p0, new Vector3());
            float len2 = n2.length();
            if (len2 > 1e-6f)
                n2.div(len2);
            else
                return;

            float cosAlpha = MathUtils.clamp(-Vector3.dot(n2, n0), -1.0f, 1.0f);
            float cosBeta = MathUtils.clamp(-Vector3.dot(n0, n1), -1.0f, 1.0f);
            float cosGamma = MathUtils.clamp(-Vector3.dot(n1, n2), -1.0f, 1.0f);

            float alpha = (float) Math.acos(cosAlpha);
            float beta = (float) Math.acos(cosBeta);
            float gamma = (float) Math.acos(cosGamma);

            float area = alpha + beta + gamma - (float) Math.PI;

            float cosC = MathUtils.clamp(Vector3.dot(p0, p1), -1.0f, 1.0f);
            float salpha = (float) Math.sin(alpha);
            float product = salpha * cosC;

            // use lower sampling depth for diffuse bounces
            int samples = state.getDiffuseDepth() > 0 ? 1 : numSamples;
            Color c = Color.mul(area / samples, radiance);
            for (int i = 0; i < samples; i++) {
                // random offset on unit square
                double randX = state.getRandom(i, 0, samples);
                double randY = state.getRandom(i, 1, samples);

                float phi = (float) randX * area - alpha + (float) Math.PI;
                float sinPhi = (float) Math.sin(phi);
                float cosPhi = (float) Math.cos(phi);

                float u = cosPhi + cosAlpha;
                float v = sinPhi - product;

                float q = (-v + cosAlpha * (cosPhi * -v + sinPhi * u)) / (salpha * (sinPhi * -v - cosPhi * u));
                float q1 = 1.0f - q * q;
                if (q1 < 0.0f)
                    q1 = 0.0f;

                float sqrtq1 = (float) Math.sqrt(q1);
                float ncx = q * p0.x + sqrtq1 * h.x;
                float ncy = q * p0.y + sqrtq1 * h.y;
                float ncz = q * p0.z + sqrtq1 * h.z;
                dot = p1.dot(ncx, ncy, ncz);
                float z = 1.0f - (float) randY * (1.0f - dot);
                float z1 = 1.0f - z * z;
                if (z1 < 0.0f)
                    z1 = 0.0f;
                Vector3 nd = new Vector3();
                nd.x = ncx - dot * p1.x;
                nd.y = ncy - dot * p1.y;
                nd.z = ncz - dot * p1.z;
                nd.normalize();
                float sqrtz1 = (float) Math.sqrt(z1);
                Vector3 result = new Vector3();
                result.x = z * p1.x + sqrtz1 * nd.x;
                result.y = z * p1.y + sqrtz1 * nd.y;
                result.z = z * p1.z + sqrtz1 * nd.z;

                // make sure the sample is in the right hemisphere - facing in
                // the right direction
                if (Vector3.dot(result, n) > 0 && Vector3.dot(result, state.getGeoNormal()) > 0 && Vector3.dot(result, ng) < 0) {
                    // compute intersection with triangle (if any)
                    Ray shadowRay = new Ray(state.getPoint(), result);
                    if (!intersectTriangleKensler(shadowRay))
                        continue;
                    LightSample dest = new LightSample();
                    dest.setShadowRay(shadowRay);
                    // prepare sample
                    dest.setRadiance(c, c);
                    dest.traceShadow(state);
                    state.addSample(dest);
                }
            }
        }

        public void getPhoton(double randX1, double randY1, double randX2, double randY2, Point3 p, Vector3 dir, Color power) {
            double s = Math.sqrt(1 - randX2);
            float u = (float) (randY2 * s);
            float v = (float) (1 - s);
            float w = 1 - u - v;
            int index0 = 3 * triangles[tri3 + 0];
            int index1 = 3 * triangles[tri3 + 1];
            int index2 = 3 * triangles[tri3 + 2];
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