package org.sunflow.core.light;

import org.sunflow.core.LightSample;
import org.sunflow.core.LightSource;
import org.sunflow.core.Ray;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class DirectionalSpotlight implements LightSource {
    private Point3 src;
    private Vector3 dir;
    private OrthoNormalBasis basis;
    private float r, r2;
    private Color radiance;

    public DirectionalSpotlight(Point3 src, Point3 target, float r, Color radiance) {
        this.src = new Point3(src);
        dir = Point3.sub(target, src, new Vector3()).normalize();
        basis = OrthoNormalBasis.makeFromW(dir);
        this.r = r;
        this.r2 = r * r;
        this.radiance = new Color(radiance);
    }

    public int getNumSamples() {
        return 1;
    }

    public boolean isVisible(ShadingState state) {
        if (Vector3.dot(dir, state.getNormal()) < 0.0) {
            // project point onto source plane
            float x = state.getPoint().x - src.x;
            float y = state.getPoint().y - src.y;
            float z = state.getPoint().z - src.z;
            float t = ((x * dir.x) + (y * dir.y) + (z * dir.z));
            if (t >= 0.0) {
                x -= (t * dir.x);
                y -= (t * dir.y);
                z -= (t * dir.z);
                return (((x * x) + (y * y) + (z * z)) <= r2);
            }
        }
        return false;
    }

    public void getSample(int i, ShadingState state, LightSample dest) {
        // project point onto source plane
        float x = state.getPoint().x - src.x;
        float y = state.getPoint().y - src.y;
        float z = state.getPoint().z - src.z;
        float t = ((x * dir.x) + (y * dir.y) + (z * dir.z));
        x -= (t * dir.x);
        y -= (t * dir.y);
        z -= (t * dir.z);
        Point3 p = new Point3();
        p.x = src.x + x;
        p.y = src.y + y;
        p.z = src.z + z;
        dest.setShadowRay(new Ray(state.getPoint(), p));
        dest.setRadiance(radiance, radiance);
        dest.traceShadow(state);
    }

    public void getPhoton(double randX1, double randY1, double randX2, double randY2, Point3 p, Vector3 dir, Color power) {
        float phi = (float) (2 * Math.PI * randX1);
        float s = (float) Math.sqrt(1.0f - randY1);
        dir.x = r * (float) Math.cos(phi) * s;
        dir.y = r * (float) Math.sin(phi) * s;
        dir.z = 0;
        basis.transform(dir);
        Point3.add(src, dir, p);
        dir.set(this.dir);
        power.set(radiance).mul((float) Math.PI * r2);
    }

    public boolean isAdaptive() {
        return false;
    }

    public float getPower() {
        return radiance.copy().mul((float) Math.PI * r2).getLuminance();
    }
}