package org.sunflow.core.accel2;

import org.sunflow.core.AccelerationStructure;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Ray;

public class SimpleAccelerator implements AccelerationStructure {
    private PrimitiveList primitives;

    public boolean build(PrimitiveList primitives) {
        this.primitives = primitives;
        return primitives.getNumPrimitives() == 1;
    }

    public void intersect(Ray r, IntersectionState istate) {
        primitives.intersectPrimitive(r, 0, istate);
    }
}