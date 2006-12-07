package org.sunflow.core;

import org.sunflow.core.accel.BoundingIntervalHierarchy;
import org.sunflow.core.accel.KDTree;
import org.sunflow.core.accel.NullAccelerator;
import org.sunflow.core.accel.UniformGrid;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

class AccelerationStructureFactory {
    static final AccelerationStructure create(String name, int n, boolean primitives) {
        if (name == null || name.equals("auto")) {
            if (primitives) {
                if (n > 20000000)
                    return new UniformGrid();
                else if (n > 2000000)
                    return new BoundingIntervalHierarchy();
                else if (n > 2)
                    return new KDTree();
                else
                    return new NullAccelerator();
            } else {
                if (n > 2)
                    return new BoundingIntervalHierarchy();
                else
                    return new NullAccelerator();
            }
        } else if (name.equals("uniformgrid"))
            return new UniformGrid();
        else if (name.equals("null"))
            return new NullAccelerator();
        else if (name.equals("kdtree"))
            return new KDTree();
        else if (name.equals("bih"))
            return new BoundingIntervalHierarchy();
        else {
            UI.printWarning(Module.ACCEL, "Unrecognized intersection accelerator \"%s\" - using auto", name);
            return create(null, n, primitives);
        }
    }
}