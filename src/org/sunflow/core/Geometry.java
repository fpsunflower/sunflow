package org.sunflow.core;

import org.sunflow.core.accel.BoundingIntervalHierarchy;
import org.sunflow.core.accel.KDTree;
import org.sunflow.core.accel.NullAccelerator;
import org.sunflow.core.accel.UniformGrid;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;
import org.sunflow.system.UI;

public class Geometry {
    private PrimitiveList primitives;
    private AccelerationStructure accel;
    private int builtAccel;

    /**
     * Create a geometry from the specified primitive aggregate
     * 
     * @param primitives
     */
    public Geometry(PrimitiveList primitives) {
        this.primitives = primitives;
        accel = null;
        builtAccel = 0;
    }

    int getNumPrimitives() {
        return primitives.getNumPrimitives();
    }

    BoundingBox getWorldBounds(Matrix4 o2w) {
        return primitives.getWorldBounds(o2w);
    }

    void intersect(Ray r, IntersectionState state) {
        if (builtAccel == 0)
            build();
        accel.intersect(r, state);
    }

    private synchronized void build() {
        // double check flag
        if (builtAccel != 0)
            return;

        int n = primitives.getNumPrimitives();
        if (n >= 10)
            UI.printInfo("[GEO] Building acceleration structure for %d primitives ...", n);
        if (n > 20000000)
            accel = new UniformGrid();
        else if (n > 2000000)
            accel = new BoundingIntervalHierarchy();
        else if (n > 2)
            accel = new KDTree();
        else
            accel = new NullAccelerator();
        accel.build(primitives);
        builtAccel = 1;
    }

    void prepareShadingState(ShadingState state) {
        primitives.prepareShadingState(state);
    }
}