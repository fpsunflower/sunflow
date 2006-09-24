package org.sunflow.core.accel2;

import org.sunflow.core.AccelerationStructure;
import org.sunflow.core.AggregateTraceable;
import org.sunflow.core.Instance;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.Ray;
import org.sunflow.math.BoundingBox;

public class NullAccelerator implements AccelerationStructure {
    private AggregateTraceable primitives;
    private BoundingBox bounds;
    private int n;

    public NullAccelerator() {
        primitives = null;
        bounds = null;
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    public boolean build(AggregateTraceable primitives) {
        this.primitives = primitives;
        n = primitives.numPrimitives();
        bounds = primitives.getWorldBounds(null);
        return true;
    }

    public void intersect(Ray r, Instance parent, IntersectionState state) {
        for (int i = 0; i < n; i++)
            primitives.intersectPrimitive(r, parent, i, state);
    }
}