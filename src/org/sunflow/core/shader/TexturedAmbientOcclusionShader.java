package org.sunflow.core.shader;

import org.sunflow.core.ShadingState;
import org.sunflow.core.Texture;
import org.sunflow.core.TextureCache;
import org.sunflow.image.Color;

public class TexturedAmbientOcclusionShader extends AmbientOcclusionShader {
    private Texture tex;

    public TexturedAmbientOcclusionShader(String filename) {
        super(null);
        tex = TextureCache.getTexture(filename);
    }

    public TexturedAmbientOcclusionShader(String filename, Color dark, int samples, float maxDist) {
        super(null, dark, samples, maxDist);
        tex = TextureCache.getTexture(filename);
    }

    @Override
    public Color getBrightColor(ShadingState state) {
        return tex.getPixel(state.getUV().x, state.getUV().y);
    }
}