package org.sunflow.core;

import org.sunflow.math.BoundingBox;

/**
 * This represents an intersectable geometry with finite and computable extents.
 * The bounds allow these primitives to be placed in intersection accelerators.
 */
public interface BoundedPrimitive extends Primitive {
    /**
     * Gets a bounding box that encloses the surface as best as possible.
     * 
     * @return a bounding box for the surface
     */
    BoundingBox getBounds();

    /**
     * Gets a specific coordinate of the surface's bounding box.
     * 
     * @param i
     * @return
     */
    float getBound(int i);

    /**
     * Checks to see if the box intersects the surface. The test must treat the
     * box as a solid. May return a conservative result by using
     * {@link BoundingBox#intersects(BoundingBox)} on the
     * {@link #getBounds() boundingbox}of this object.
     * 
     * @param box box to be intersected with the surface
     * @return <code>true</code> if the surface intersects the box,
     *         <code>false</code> otherwise
     */
    boolean intersects(BoundingBox box);
}