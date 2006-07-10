package org.sunflow.core.accel;

import java.util.ArrayList;

import org.sunflow.core.BoundedPrimitive;
import org.sunflow.core.IntersectionAccelerator;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.Ray;
import org.sunflow.math.BoundingBox;

public class NullAccelerator implements IntersectionAccelerator {
    private BoundedPrimitive[] objects;
    private BoundingBox bounds;

    public NullAccelerator() {
        objects = null;
        bounds = null;
    }

    public boolean build(ArrayList<BoundedPrimitive> objects) {
        this.objects = objects.toArray(new BoundedPrimitive[objects.size()]);
        bounds = new BoundingBox();
        for (BoundedPrimitive o : objects)
            bounds.include(o.getBounds());
        return true;
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    public void intersect(Ray r, IntersectionState state) {
        for (BoundedPrimitive o : objects)
            o.intersect(r, state);
    }
}