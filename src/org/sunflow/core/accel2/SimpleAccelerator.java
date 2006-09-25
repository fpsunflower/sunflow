package org.sunflow.core.accel2;

import org.sunflow.core.AccelerationStructure;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Ray;
import org.sunflow.math.BoundingBox;

public class SimpleAccelerator implements AccelerationStructure {
    private PrimitiveList primitives;

    public boolean build(PrimitiveList primitives) {
        this.primitives = primitives;
        return primitives.getNumPrimitives() == 1;
    }

    public BoundingBox getBounds() {
        BoundingBox bounds = new BoundingBox();
        bounds.include(primitives.getPrimitiveBound(0, 0), primitives.getPrimitiveBound(0, 2), primitives.getPrimitiveBound(0, 4));
        bounds.include(primitives.getPrimitiveBound(0, 1), primitives.getPrimitiveBound(0, 3), primitives.getPrimitiveBound(0, 5));
        return bounds;
    }

    public void intersect(Ray r, IntersectionState istate) {
        primitives.intersectPrimitive(r, 0, istate);
    }
}