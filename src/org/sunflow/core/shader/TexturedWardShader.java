package org.sunflow.core.shader;

import org.sunflow.core.ShadingState;
import org.sunflow.core.Texture;
import org.sunflow.core.TextureCache;
import org.sunflow.image.Color;

public class TexturedWardShader extends AnisotropicWardShader {
    private Texture tex;

    public TexturedWardShader(String filename, Color s, float rx, float ry, int numRays) {
        super(null, s, rx, ry, numRays);
        tex = TextureCache.getTexture(filename);
    }

    @Override
    public Color getDiffuse(ShadingState state) {
        return tex.getPixel(state.getUV().x, state.getUV().y);
    }
}