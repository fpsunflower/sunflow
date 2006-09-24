package org.sunflow.core;

import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;

class InstanceList implements AggregateTraceable {
    private Instance[] instances;
    private BoundingBox bounds;

    InstanceList(Instance[] instances) {
        this.instances = instances;
        bounds = new BoundingBox();
        for (Instance i : instances)
            bounds.include(i.getBounds());
    }

    public float getObjectBound(int primID, int i) {
        return instances[primID].getBound(i);
    }

    public BoundingBox getWorldBounds(Matrix4 o2w) {
        assert o2w == null;
        return bounds;
    }

    public void intersectPrimitive(Ray r, Instance parent, int primID, IntersectionState state) {
        assert parent == null;
        instances[primID].intersect(r, state);
    }

    public int numPrimitives() {
        return instances.length;
    }

    public void prepareShadingState(Instance parent, int primID, ShadingState state) {
        assert parent == null;
        instances[primID].prepareShadingState(state);
    }
}