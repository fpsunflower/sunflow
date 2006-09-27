package org.sunflow.core;

import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;

final class InstanceList implements PrimitiveList {
    private Instance[] instances;
    private int n;

    InstanceList() {
        clear();
    }

    final void clear() {
        instances = new Instance[10];
        n = 0;
    }

    final void add(Instance instance) {
        if (n == instances.length) {
            Instance[] oldArray = instances;
            instances = new Instance[(n * 3) / 2 + 1];
            System.arraycopy(oldArray, 0, instances, 0, n);
        }
        instances[n] = instance;
        n++;
    }

    final Instance[] trim() {
        if (n < instances.length) {
            Instance[] oldArray = instances;
            instances = new Instance[n];
            System.arraycopy(oldArray, 0, instances, 0, n);
        }
        return instances;
    }

    public final float getPrimitiveBound(int primID, int i) {
        return instances[primID].getBounds().getBound(i);
    }

    public final BoundingBox getWorldBounds(Matrix4 o2w) {
        BoundingBox bounds = new BoundingBox();
        for (Instance i : instances)
            bounds.include(i.getBounds());
        return bounds;
    }

    public final void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        instances[primID].intersect(r, state);
    }

    public final int getNumPrimitives() {
        return n;
    }

    public final int getNumPrimitives(int primID) {
        return instances[primID].getNumPrimitives();
    }

    public final void prepareShadingState(ShadingState state) {
        state.getInstance().prepareShadingState(state);
    }
}