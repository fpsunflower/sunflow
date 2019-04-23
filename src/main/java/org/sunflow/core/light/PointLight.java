package org.sunflow.core.light;

import org.sunflow.SunflowAPI;
import org.sunflow.core.Instance;
import org.sunflow.core.LightSample;
import org.sunflow.core.LightSource;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Ray;
import org.sunflow.core.ShadingState;
import org.sunflow.core.parameter.light.PointLightParameter;
import org.sunflow.image.Color;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class PointLight implements LightSource {
    // Center
    private Point3 lightPoint;

    // Radiance
    private Color color;

    public PointLight() {
        lightPoint = new Point3(0, 0, 0);
        color = Color.WHITE;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        lightPoint = pl.getPoint(PointLightParameter.PARAM_CENTER, lightPoint);
        color = pl.getColor(PointLightParameter.PARAM_POWER, color);
        return true;
    }

    public int getNumSamples() {
        return 1;
    }

    public void getSamples(ShadingState state) {
        Vector3 d = Point3.sub(lightPoint, state.getPoint(), new Vector3());
        if (Vector3.dot(d, state.getNormal()) > 0 && Vector3.dot(d, state.getGeoNormal()) > 0) {
            LightSample dest = new LightSample();
            // prepare shadow ray
            dest.setShadowRay(new Ray(state.getPoint(), lightPoint));
            float scale = 1.0f / (float) (4 * Math.PI * lightPoint.distanceToSquared(state.getPoint()));
            dest.setRadiance(color, color);
            dest.getDiffuseRadiance().mul(scale);
            dest.getSpecularRadiance().mul(scale);
            dest.traceShadow(state);
            state.addSample(dest);
        }
    }

    public void getPhoton(double randX1, double randY1, double randX2, double randY2, Point3 p, Vector3 dir, Color power) {
        p.set(lightPoint);
        float phi = (float) (2 * Math.PI * randX1);
        float s = (float) Math.sqrt(randY1 * (1.0f - randY1));
        dir.x = (float) Math.cos(phi) * s;
        dir.y = (float) Math.sin(phi) * s;
        dir.z = (float) (1 - 2 * randY1);
        power.set(this.color);
    }

    public Point3 getLightPoint() {
        return lightPoint;
    }

    public void setLightPoint(Point3 lightPoint) {
        this.lightPoint = lightPoint;
    }

    public float getPower() {
        return color.getLuminance();
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public Instance createInstance() {
        return null;
    }
}