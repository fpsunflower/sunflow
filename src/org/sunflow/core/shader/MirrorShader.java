package org.sunflow.core.shader;

import org.sunflow.core.Ray;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.Vector3;

public class MirrorShader implements Shader {
    private Color reflect;

    public MirrorShader(Color reflect) {
        this.reflect = reflect.copy();
    }

    public Color getRadiance(ShadingState state) {
        if (!state.includeSpecular()) return Color.BLACK;
        state.faceforward();
        float cos = state.getCosND();
        float dn = 2 * cos;
        Vector3 refDir = new Vector3();
        refDir.x = (dn * state.getNormal().x) + state.getRay().getDirection().x;
        refDir.y = (dn * state.getNormal().y) + state.getRay().getDirection().y;
        refDir.z = (dn * state.getNormal().z) + state.getRay().getDirection().z;
        Ray refRay = new Ray(state.getPoint(), refDir);

        // compute Fresnel term
        cos = 1 - cos;
        float cos2 = cos * cos;
        float cos5 = cos2 * cos2 * cos;
        Color ret = Color.white();
        ret.sub(reflect);
        ret.mul(cos5);
        ret.add(reflect);
        return ret.mul(state.traceReflection(refRay, 0));
    }

    public void scatterPhoton(ShadingState state, Color power) {
        float avg = reflect.getAverage();
        double rnd = state.getRandom(0, 0, 1);
        if (rnd >= avg) return;
        state.faceforward();
        float cos = state.getCosND();
        power.mul(reflect).mul(1.0f / avg);
        // photon is reflected
        float dn = 2 * cos;
        Vector3 dir = new Vector3();
        dir.x = (dn * state.getNormal().x) + state.getRay().getDirection().x;
        dir.y = (dn * state.getNormal().y) + state.getRay().getDirection().y;
        dir.z = (dn * state.getNormal().z) + state.getRay().getDirection().z;
        state.traceReflectionPhoton(new Ray(state.getPoint(), dir), power);
    }
}