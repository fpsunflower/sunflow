package org.sunflow.core.shader;

import org.sunflow.core.ShadingState;
import org.sunflow.core.Texture;
import org.sunflow.core.TextureCache;
import org.sunflow.image.Color;

public class TexturedPhongShader extends PhongShader {
    private Texture tex;

    public TexturedPhongShader(String filename, Color s, float power, int numRays) {
        super(null, s, power, numRays);
        tex = TextureCache.getTexture(filename);
    }

    @Override
    public Color getDiffuse(ShadingState state) {
        return tex.getPixel(state.getUV().x, state.getUV().y);
    }
}