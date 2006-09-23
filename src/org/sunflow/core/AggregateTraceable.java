package org.sunflow.core;

import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;

/**
 * This class represents an object made up of many traceable elements. You may
 * use an {@link IntersectionAccelerator} to turn this into a regular
 * {@link Traceable} object which may be instanced.
 */
public interface AggregateTraceable {
    /**
     * Compute a bounding box of this object in world space, using the specified
     * object-to-world transformation matrix. The bounds should be as exact as
     * possible, if they are difficult or expensive to compute exactly, you may
     * use {@link Matrix4#transform(BoundingBox)}. If the matrix is
     * <code>null</code> no transformation is needed.
     * 
     * @param o2w object to world transformation matrix
     * @return object bounding box in world space
     */
    public BoundingBox getWorldBounds(Matrix4 o2w);

    /**
     * Returns the number of individual primtives in this aggregate object.
     * 
     * @return number of primitives
     */
    public int numPrimitives();

    /**
     * Retrieve the bounding box component of a particular primitive.
     * 
     * @param primID primitive index
     * @param i bounding box side index
     * @return value of the request bound
     */
    public float getBound(int primID, int i);

    /**
     * Intersect the specified primitive in local space.
     * 
     * @param ray ray in the object's local space
     * @param parent instance currently being intersected
     * @param primID primitive index to intersect
     * @param state intersection state
     */
    public void intersectPrimitive(Ray ray, Instance parent, int primID, IntersectionState state);

    /**
     * Prepare the specified {@link ShadingState} by setting all of its internal
     * parameters. The provided instance can be used to transform between object
     * and world space.
     * 
     * @param parent instance which was hit
     * @param primID primitive index which was hit
     * @param state shading state to fill in
     */
    public void prepareShadingState(Instance parent, int primID, ShadingState state);
}