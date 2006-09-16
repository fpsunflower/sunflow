package org.sunflow.core;

import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;

/**
 * This interface represents objects which can be intersected in local space.
 * They are meant to be raytraced only through the {@link Instance} class.
 */
public interface Traceable {
    /**
     * Compute a bounding box of this object in world space, using the specified
     * object-to-world transformation matrix. The bounds should be as exact as
     * possible, if they are difficult or expensive to compute exactly, you may
     * use {@link Matrix4#transform(BoundingBox)}. If the matrix is
     * <code>null</code> no transformation is needed.
     * 
     * @param o2w
     *            object to world transformation matrix
     * @return object bounding box in world space
     */
    public BoundingBox getWorldBounds(Matrix4 o2w);

    /**
     * Intersect the specified ray with the geometry in local space. The ray
     * will be provided in local space.
     * 
     * @param r
     *            ray in local space
     * @param istate
     *            state to store the intersection into
     */
    public void intersect(Ray r, Instance parent, IntersectionState istate);

    /**
     * Prepare the specified {@link ShadingState} by setting all of its internal
     * parameters. The provided instance can be used to transform between object
     * and world space.
     * 
     * @param parent
     *            instance which was hit
     * @param state
     *            shading state to fill in
     */
    public void prepareShadingState(Instance parent, ShadingState state);
}