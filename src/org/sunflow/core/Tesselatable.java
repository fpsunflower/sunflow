package org.sunflow.core;

import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;

public interface Tesselatable extends RenderObject {
    public PrimitiveList tesselate();

    public BoundingBox getWorldBounds(Matrix4 o2w);
}