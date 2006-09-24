package org.sunflow.core;

import org.sunflow.math.BoundingBox;

public interface AccelerationStructure {
    /**
     * Construct an acceleration structure for the specified primitive list.
     * 
     * @param primitives
     * @return
     */
    public boolean build(AggregateTraceable primitives);

    /**
     * Get the bounding box of all enclosed primitives.
     * 
     * @return bounding box of all enclosed primitives
     */
    public BoundingBox getBounds();

    /**
     * Intersect the specified ray with the geometry in local space. The ray
     * will be provided in local space.
     * 
     * @param r ray in local space
     * @param istate state to store the intersection into
     */
    public void intersect(Ray r, Instance parent, IntersectionState istate);
}