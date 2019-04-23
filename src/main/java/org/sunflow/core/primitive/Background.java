package org.sunflow.core.primitive;

import org.sunflow.SunflowAPI;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.ParameterList;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Ray;
import org.sunflow.core.ShadingState;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;

public class Background implements PrimitiveList {
    public Background() {
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        return true;
    }

    public void prepareShadingState(ShadingState state) {
        if (state.getDepth() == 0)
            state.setShader(state.getInstance().getShader(0));
    }

    public int getNumPrimitives() {
        return 1;
    }

    public float getPrimitiveBound(int primID, int i) {
        return 0;
    }

    public BoundingBox getWorldBounds(Matrix4 o2w) {
        return null;
    }

    public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        if (r.getMax() == Float.POSITIVE_INFINITY)
            state.setIntersection(0);
    }

    public PrimitiveList getBakingPrimitives() {
        return null;
    }
}