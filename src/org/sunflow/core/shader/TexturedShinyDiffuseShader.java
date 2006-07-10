package org.sunflow.core.shader;

import org.sunflow.core.ShadingState;
import org.sunflow.core.Texture;
import org.sunflow.core.TextureCache;
import org.sunflow.image.Color;

public class TexturedShinyDiffuseShader extends ShinyDiffuseShader {
    private Texture texture;

    public TexturedShinyDiffuseShader(String filename, float r) {
        super(null, r);
        texture = TextureCache.getTexture(filename);
    }
    
    public Color getDiffuse(ShadingState state) {
        return texture.getPixel(state.getUV().x, state.getUV().y);
    }
}