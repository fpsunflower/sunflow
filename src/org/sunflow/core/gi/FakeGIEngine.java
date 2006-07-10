package org.sunflow.core.gi;

import org.sunflow.core.GIEngine;
import org.sunflow.core.Scene;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.Vector3;

/**
 * This is a quick way to get a bit of ambient lighting into your scene with
 * hardly any overhead. It's based on the formula found here:
 * 
 * @link http://www.cs.utah.edu/~shirley/papers/rtrt/node7.html#SECTION00031100000000000000
 */
public class FakeGIEngine implements GIEngine {
    private Vector3 up;
    private Color sky;
    private Color ground;

    public FakeGIEngine(Vector3 up, Color sky, Color ground) {
        this.up = new Vector3(up).normalize();
        this.sky = sky;
        this.ground = ground;
        up.normalize();
        sky.mul((float) Math.PI);
        ground.mul((float) Math.PI);
    }

    public Color getIrradiance(ShadingState state, Color diffuseReflectance) {
        float cosTheta = Vector3.dot(up, state.getNormal());
        float sine = (1 - cosTheta * cosTheta) * 0.5f;
        if (cosTheta > 0)
            return Color.blend(sky, ground, sine);
        else
            return Color.blend(ground, sky, sine);
    }

    public Color getGlobalRadiance(ShadingState state) {
        return Color.BLACK;
    }

    public boolean init(Scene scene) {
        return true;
    }
}