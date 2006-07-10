package org.sunflow.core.shader;

import org.sunflow.core.ShadingState;
import org.sunflow.core.Texture;
import org.sunflow.core.TextureCache;
import org.sunflow.image.Color;

public class TexturedDiffuseShader extends DiffuseShader {
    private Texture tex;

    public TexturedDiffuseShader(String filename) {
        tex = TextureCache.getTexture(filename);
    }

    @Override
    public Color getDiffuse(ShadingState state) {
        return tex.getPixel(state.getUV().x, state.getUV().y);
    }
}