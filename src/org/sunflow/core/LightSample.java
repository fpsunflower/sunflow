package org.sunflow.core;

import org.sunflow.image.Color;
import org.sunflow.math.Vector3;

/**
 * Represents a sample taken from a light source that faces a point being
 * shaded.
 */
public class LightSample {
    private Ray shadowRay; // ray to be used to evaluate if the point is in shadow
    private Color ldiff;
    private Color lspec;
    LightSample next; // pointer to next item in a linked list of samples

    public LightSample() {
        ldiff = lspec = null;
        shadowRay = null;
        next = null;
    }
    
    public boolean isValid() {
        return ldiff != null && lspec != null && shadowRay != null;
    }

    public void setShadowRay(Ray shadowRay) {
        this.shadowRay = shadowRay;
    }

    public final void traceShadow(ShadingState state) {
        Color opacity = state.traceShadow(shadowRay);
        Color.blend(ldiff, Color.BLACK, opacity, ldiff);
        Color.blend(lspec, Color.BLACK, opacity, lspec);
    }
    
    public Ray getShadowRay() {
        return shadowRay;
    }

    public Color getDiffuseRadiance() {
        return ldiff;
    }

    public Color getSpecularRadiance() {
        return lspec;
    }

    public void setRadiance(Color d, Color s) {
        ldiff = d.copy();
        lspec = s.copy();
    }
    
    public float dot(Vector3 v) {
        return shadowRay.dx * v.x + shadowRay.dy * v.y + shadowRay.dz * v.z;
    }
}