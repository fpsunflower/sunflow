package org.sunflow.core;

import org.sunflow.image.Color;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Vector3;

public interface PhotonStore {
    int numEmit();

    void prepare(BoundingBox sceneBounds);

    void store(ShadingState state, Vector3 dir, Color power, Color diffuse);

    void init();

    boolean allowDiffuseBounced();

    boolean allowReflectionBounced();

    boolean allowRefractionBounced();
}