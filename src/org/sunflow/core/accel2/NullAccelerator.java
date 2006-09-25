package org.sunflow.core.accel2;

import org.sunflow.core.AccelerationStructure;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Ray;
import org.sunflow.math.BoundingBox;

public class NullAccelerator implements AccelerationStructure {
    private PrimitiveList primitives;
    private BoundingBox bounds;
    private int n;

    public NullAccelerator() {
        primitives = null;
        bounds = null;
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    public boolean build(PrimitiveList primitives) {
        this.primitives = primitives;
        n = primitives.getNumPrimitives();
        bounds = primitives.getWorldBounds(null);
        return true;
    }

    public void intersect(Ray r, IntersectionState state) {
        for (int i = 0; i < n; i++)
            primitives.intersectPrimitive(r, i, state);
    }
}