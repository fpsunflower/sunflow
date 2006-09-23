package org.sunflow.core;

import org.sunflow.image.Color;

/**
 * A shader represents a particular light-surface interaction. Shaders may be
 * used as sources of light, reflectors of light, and even as surface modifiers
 * for effects such as bump-mapping.
 */
public interface Shader {
    /**
     * Gets the radiance for a specified rendering state. When this method is
     * called, you can assume that a hit has been registered in the state and
     * that the hit vertex has been computed.
     * 
     * @param state current render state
     * @return color emitted or reflected by the shader
     * @see Primitive#prepareShadingState(ShadingState)
     */
    public Color getRadiance(ShadingState state);

    /**
     * Scatter a photon with the specied power. Incoming photon direction is
     * specified by the ray attached to the current render state.
     * 
     * @param state current state
     * @param power power of the incoming photon.
     */
    public void scatterPhoton(ShadingState state, Color power);
}