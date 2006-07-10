package org.sunflow.core.light;

import org.sunflow.core.LightSample;
import org.sunflow.core.LightSource;
import org.sunflow.core.Ray;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class PointLight implements LightSource {
    private Point3 lightPoint;
    private Color power;

    public PointLight(Point3 lightPoint, Color power) {
        this.lightPoint = lightPoint;
        this.power = power;
    }

    public int getNumSamples() {
        return 1;
    }

    public boolean isVisible(ShadingState state) {
        Point3 p = state.getPoint();
        Vector3 n = state.getNormal();
        return (Vector3.dot(Point3.sub(lightPoint, p, new Vector3()), n) > 0.0);
    }

    public void getSample(int i, ShadingState state, LightSample dest) {
        // prepare shadow ray
        dest.setShadowRay(new Ray(state.getPoint(), lightPoint));
        float scale = 1.0f / (float) (4 * Math.PI * lightPoint.distanceToSquared(state.getPoint()));
        dest.setRadiance(power, power);
        dest.getDiffuseRadiance().mul(scale);
        dest.getSpecularRadiance().mul(scale);
        dest.traceShadow(state);
    }

    public void getPhoton(double randX1, double randY1, double randX2, double randY2, Point3 p, Vector3 dir, Color power) {
        p.set(lightPoint);
        float phi = (float) (2 * Math.PI * randX1);
        float s = (float) Math.sqrt(randY1 * (1.0f - randY1));
        dir.x = (float) Math.cos(phi) * s;
        dir.y = (float) Math.sin(phi) * s;
        dir.z = (float) (1 - 2 * randY1);
        power.set(this.power);
    }

    public boolean isAdaptive() {
        return false;
    }

    public float getPower() {
        return power.getLuminance();
    }
}