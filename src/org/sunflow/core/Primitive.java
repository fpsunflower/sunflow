package org.sunflow.core;

/**
 * This interface is implemented by all geometric primitives that are to be
 * rendered.
 */
public interface Primitive {
    void prepareShadingState(ShadingState state);

    void intersect(Ray r, IntersectionState istate);
}