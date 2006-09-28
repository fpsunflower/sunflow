package org.sunflow.core;

import org.sunflow.core.accel.KDTree;
import org.sunflow.core.accel.NullAccelerator;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;
import org.sunflow.system.UI;

public class Geometry {
    private PrimitiveList primitives;
    private AccelerationStructure accel;

    /**
     * Create a geometry from the specified primitive aggregate
     * 
     * @param primitives
     */
    public Geometry(PrimitiveList primitives) {
        this.primitives = primitives;
        // TODO: construct appropriate acceleration structure from parameters
        accel = null;
        int n = primitives.getNumPrimitives();
        if (n > 2)
            accel = new KDTree();
        else
            accel = new NullAccelerator();
        if (!accel.build(primitives)) {
            UI.printError("[GEO] Unable to build acceleration data structure");
            return;
        }
    }

    int getNumPrimitives() {
        return primitives.getNumPrimitives();
    }

    BoundingBox getWorldBounds(Matrix4 o2w) {
        return primitives.getWorldBounds(o2w);
    }

    void intersect(Ray r, IntersectionState state) {
        accel.intersect(r, state);
    }

    void prepareShadingState(ShadingState state) {
        primitives.prepareShadingState(state);
    }
}