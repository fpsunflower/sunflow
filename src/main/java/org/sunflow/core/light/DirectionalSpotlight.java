package org.sunflow.core.light;

import org.sunflow.SunflowAPI;
import org.sunflow.core.Instance;
import org.sunflow.core.LightSample;
import org.sunflow.core.LightSource;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Ray;
import org.sunflow.core.ShadingState;
import org.sunflow.core.parameter.light.DirectionalLightParameter;
import org.sunflow.core.parameter.light.LightParameter;
import org.sunflow.image.Color;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class DirectionalSpotlight implements LightSource {
    private Point3 source;
    private Vector3 direction;
    private OrthoNormalBasis basis;
    // Radius
    private float r;
    // Radius^2
    private float r2;
    private Color radiance;

    public DirectionalSpotlight() {
        source = new Point3(0, 0, 0);
        direction = new Vector3(0, 0, -1);
        direction.normalize();
        basis = OrthoNormalBasis.makeFromW(direction);
        r = 1;
        r2 = r * r;
        radiance = Color.WHITE;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        source = pl.getPoint(DirectionalLightParameter.PARAM_SOURCE, source);
        direction = pl.getVector(DirectionalLightParameter.PARAM_DIRECTION, direction);
        direction.normalize();
        r = pl.getFloat(DirectionalLightParameter.PARAM_RADIUS, r);
        basis = OrthoNormalBasis.makeFromW(direction);
        r2 = r * r;
        radiance = pl.getColor(LightParameter.PARAM_RADIANCE, radiance);
        return true;
    }

    public int getNumSamples() {
        return 1;
    }

    public int getLowSamples() {
        return 1;
    }

    public void getSamples(ShadingState state) {
        if (Vector3.dot(direction, state.getGeoNormal()) < 0 && Vector3.dot(direction, state.getNormal()) < 0) {
            // project point onto source plane
            float x = state.getPoint().x - source.x;
            float y = state.getPoint().y - source.y;
            float z = state.getPoint().z - source.z;
            float t = ((x * direction.x) + (y * direction.y) + (z * direction.z));
            if (t >= 0.0) {
                x -= (t * direction.x);
                y -= (t * direction.y);
                z -= (t * direction.z);
                if (((x * x) + (y * y) + (z * z)) <= r2) {
                    Point3 p = new Point3();
                    p.x = source.x + x;
                    p.y = source.y + y;
                    p.z = source.z + z;
                    LightSample dest = new LightSample();
                    dest.setShadowRay(new Ray(state.getPoint(), p));
                    dest.setRadiance(radiance, radiance);
                    dest.traceShadow(state);
                    state.addSample(dest);
                }
            }
        }
    }

    public void getPhoton(double randX1, double randY1, double randX2, double randY2, Point3 p, Vector3 dir, Color power) {
        float phi = (float) (2 * Math.PI * randX1);
        float s = (float) Math.sqrt(1.0f - randY1);
        dir.x = r * (float) Math.cos(phi) * s;
        dir.y = r * (float) Math.sin(phi) * s;
        dir.z = 0;
        basis.transform(dir);
        Point3.add(source, dir, p);
        dir.set(this.direction);
        power.set(radiance).mul((float) Math.PI * r2);
    }

    public float getPower() {
        return radiance.copy().mul((float) Math.PI * r2).getLuminance();
    }

    public Instance createInstance() {
        return null;
    }

    public Point3 getSource() {
        return source;
    }

    public void setSource(Point3 source) {
        this.source = source;
    }

    public Vector3 getDirection() {
        return direction;
    }

    public void setDirection(Vector3 direction) {
        this.direction = direction;
    }

    public float getR() {
        return r;
    }

    public void setR(float r) {
        this.r = r;
    }

    public Color getRadiance() {
        return radiance;
    }

    public void setRadiance(Color radiance) {
        this.radiance = radiance;
    }
}