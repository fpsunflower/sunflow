package org.sunflow.core;

import org.sunflow.SunflowAPI;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;

final class InstanceList implements PrimitiveList {
    private Instance[] instances;

    InstanceList() {
        instances = new Instance[0];
    }

    InstanceList(Instance[] instances) {
        this.instances = instances;
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
        return instances.length;
    }

    public final int getNumPrimitives(int primID) {
        return instances[primID].getNumPrimitives();
    }

    public final void prepareShadingState(ShadingState state) {
        state.getInstance().prepareShadingState(state);
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        // TODO: build accelstructure into this (?)
        return true;
    }

    public PrimitiveList getBakingPrimitives() {
        return null;
    }
}