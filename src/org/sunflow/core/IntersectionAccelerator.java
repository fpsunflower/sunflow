package org.sunflow.core;

import java.util.ArrayList;

import org.sunflow.math.BoundingBox;

/**
 * The <code>IntersectionAccelerator</code> interface is used to implement
 * fast ray tracing of large quantities of objects.
 */
public interface IntersectionAccelerator {
    /**
     * Build the data structures needed. This method will be called before
     * rendering, with a list of all objects in the scene.
     * 
     * @param objects array of objects in the scene
     * @return <code>true</code> if the build succeeded, <code>false</code>
     *         if there were errors or interuptions
     */
    boolean build(ArrayList<BoundedPrimitive> objects);

    /**
     * Gets a bounding box enclosing all objects (excluding those with infinite
     * extents).
     * 
     * @return bounding box of all enclosed objects
     */
    BoundingBox getBounds();

    /**
     * Recursively calls {@link Primitive#intersect(Ray, IntersectionState)} on
     * all objects that are likely to interesect the given ray. The accelerator
     * may skip those objects which are trivially known to be off the path of
     * the ray.
     * 
     * @param state current state to record the intersection point
     */
    void intersect(Ray r, IntersectionState state);
}