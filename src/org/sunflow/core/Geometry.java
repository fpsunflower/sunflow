package org.sunflow.core;

import org.sunflow.SunflowAPI;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class Geometry implements RenderObject {
    private Tesselatable tesselatable;
    private PrimitiveList primitives;
    private AccelerationStructure accel;
    private int builtAccel;
    private String acceltype;

    /**
     * Create a geometry from the specified tesselatable object. The actual
     * renderable primitives will be generated on demand.
     * 
     * @param tesselatable
     */
    public Geometry(Tesselatable tesselatable) {
        this.tesselatable = tesselatable;
        primitives = null;
        accel = null;
        builtAccel = 0;
        acceltype = null;
    }

    /**
     * Create a geometry from the specified primitive aggregate
     * 
     * @param primitives
     */
    public Geometry(PrimitiveList primitives) {
        tesselatable = null;
        this.primitives = primitives;
        accel = null;
        builtAccel = 0;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        acceltype = pl.getString("accel", acceltype);
        // clear up old tesselation if it exists
        if (tesselatable != null)
            primitives = null;
        // clear acceleration structure so it will be rebuilt
        accel = null;
        builtAccel = 0;
        if (tesselatable != null)
            return tesselatable.update(pl, api);
        // update primitives
        return primitives.update(pl, api);
    }

    int getNumPrimitives() {
        return primitives == null ? 0 : primitives.getNumPrimitives();
    }

    BoundingBox getWorldBounds(Matrix4 o2w) {
        return primitives == null ? tesselatable.getWorldBounds(o2w) : primitives.getWorldBounds(o2w);
    }

    void intersect(Ray r, IntersectionState state) {
        if (builtAccel == 0)
            build();
        accel.intersect(r, state);
    }

    private synchronized void build() {
        // double check flag
        if (builtAccel != 0)
            return;

        if (tesselatable != null) {
            UI.printInfo(Module.GEOM, "Tesselating geometry ...");
            primitives = tesselatable.tesselate();
        }

        int n = primitives.getNumPrimitives();
        if (n >= 10)
            UI.printInfo(Module.GEOM, "Building acceleration structure for %d primitives ...", n);

        accel = AccelerationStructureFactory.create(acceltype, n, true);
        accel.build(primitives);
        builtAccel = 1;
    }

    void prepareShadingState(ShadingState state) {
        primitives.prepareShadingState(state);
    }
    
    public PrimitiveList getBakingPrimitives() {
        if (builtAccel != 0)
            build();
        return primitives.getBakingPrimitives();
    }
}