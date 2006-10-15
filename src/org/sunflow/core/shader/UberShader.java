package org.sunflow.core.shader;

import org.sunflow.SunflowAPI;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Ray;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.core.Texture;
import org.sunflow.core.TextureCache;
import org.sunflow.image.Color;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Vector3;

public class UberShader implements Shader {
    private Color diff;
    private Color refl;
    private Texture tex;
    private float amount;

    public UberShader() {
        diff = Color.GREY;
        refl = Color.GREY;
        tex = null;
        amount = 1;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        diff = pl.getColor("diffuse", diff);
        refl = pl.getColor("reflection", refl);
        String filename = pl.getString("texture", null);
        if (filename != null)
            tex = TextureCache.getTexture(api.resolveTextureFilename(filename));
        amount = pl.getFloat("blend", amount);
        return true;
    }

    public Color getDiffuse(ShadingState state) {
        return tex == null ? diff : Color.blend(diff, tex.getPixel(state.getUV().x, state.getUV().y), amount);
    }

    public Color getRadiance(ShadingState state) {
        // make sure we are on the right side of the material
        state.faceforward();
        // direct lighting
        state.initLightSamples();
        state.initCausticSamples();
        Color d = getDiffuse(state);
        Color lr = state.diffuse(d);
        if (!state.includeSpecular())
            return lr;
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
        ret.sub(refl);
        ret.mul(cos5);
        ret.add(refl);
        return lr.add(ret.mul(state.traceReflection(refRay, 0)));
    }

    public void scatterPhoton(ShadingState state, Color power) {
        Color diffuse;
        // make sure we are on the right side of the material
        state.faceforward();
        diffuse = getDiffuse(state);
        state.storePhoton(state.getRay().getDirection(), power, diffuse);
        float d = diffuse.getAverage();
        float r = refl.getAverage();
        double rnd = state.getRandom(0, 0, 1);
        if (rnd < d) {
            // photon is scattered
            power.mul(diffuse).mul(1.0f / d);
            OrthoNormalBasis onb = state.getBasis();
            double u = 2 * Math.PI * rnd / d;
            double v = state.getRandom(0, 1, 1);
            float s = (float) Math.sqrt(v);
            float s1 = (float) Math.sqrt(1.0 - v);
            Vector3 w = new Vector3((float) Math.cos(u) * s, (float) Math.sin(u) * s, s1);
            w = onb.transform(w, new Vector3());
            state.traceDiffusePhoton(new Ray(state.getPoint(), w), power);
        } else if (rnd < d + r) {
            float cos = -Vector3.dot(state.getNormal(), state.getRay().getDirection());
            power.mul(diffuse).mul(1.0f / d);
            // photon is reflected
            float dn = 2 * cos;
            Vector3 dir = new Vector3();
            dir.x = (dn * state.getNormal().x) + state.getRay().getDirection().x;
            dir.y = (dn * state.getNormal().y) + state.getRay().getDirection().y;
            dir.z = (dn * state.getNormal().z) + state.getRay().getDirection().z;
            state.traceReflectionPhoton(new Ray(state.getPoint(), dir), power);
        }
    }
}