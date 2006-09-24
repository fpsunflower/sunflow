package org.sunflow.core.accel2;

import org.sunflow.core.AccelerationStructure;
import org.sunflow.core.AggregateTraceable;
import org.sunflow.core.Instance;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.Ray;
import org.sunflow.math.BoundingBox;

public class SimpleAccelerator implements AccelerationStructure {
    private AggregateTraceable primitives;

    public boolean build(AggregateTraceable primitives) {
        this.primitives = primitives;
        return primitives.numPrimitives() == 1;
    }

    public BoundingBox getBounds() {
        BoundingBox bounds = new BoundingBox();
        bounds.include(primitives.getObjectBound(0, 0), primitives.getObjectBound(0, 2), primitives.getObjectBound(0, 4));
        bounds.include(primitives.getObjectBound(0, 1), primitives.getObjectBound(0, 3), primitives.getObjectBound(0, 5));
        return bounds;
    }

    public void intersect(Ray r, Instance parent, IntersectionState istate) {
        primitives.intersectPrimitive(r, parent, 0, istate);
    }
}