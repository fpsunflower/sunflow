package org.sunflow.core;

import org.sunflow.core.accel2.KDTree;
import org.sunflow.core.accel2.SimpleAccelerator;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;

public class Geometry {
    private AggregateTraceable primitives;
    private AccelerationStructure accel;

    /**
     * Create a geometry from the specified primitive aggregate
     * 
     * @param primitives
     */
    public Geometry(AggregateTraceable primitives) {
        this.primitives = primitives;
        // TODO: construct appropriate acceleration structure from parameters
        accel = null;
        int n = primitives.numPrimitives();
        if (n == 1)
            accel = new SimpleAccelerator();
        else if (n > 1)
            accel = new KDTree();
        if (accel != null)
            accel.build(primitives);
    }

    BoundingBox getWorldBounds(Matrix4 o2w) {
        return primitives.getWorldBounds(o2w);
    }

    void intersect(Ray r, Instance parent, IntersectionState istate) {
        accel.intersect(r, parent, istate);
    }

    void prepareShadingState(Instance parent, ShadingState state) {
        primitives.prepareShadingState(parent, state.getIntersectionState().id, state);
    }
}