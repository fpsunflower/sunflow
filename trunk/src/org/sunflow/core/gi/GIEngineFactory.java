package org.sunflow.core.gi;

import org.sunflow.core.GIEngine;
import org.sunflow.core.Options;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public final class GIEngineFactory {
    public static final GIEngine create(Options options) {
        String type = options.getString("gi.engine", null);
        if (type == null || type.equals("null") || type.equals("none"))
            return null;
        else if (type.equals("ambocc"))
            return new AmbientOcclusionGIEngine(options);
        else if (type.equals("fake"))
            return new FakeGIEngine(options);
        else if (type.equals("igi"))
            return new InstantGI(options);
        else if (type.equals("irr-cache"))
            return new IrradianceCacheGIEngine(options);
        else if (type.equals("path"))
            return new PathTracingGIEngine(options);
        else {
            UI.printWarning(Module.LIGHT, "Unrecognized GI engine type \"%s\" - ignoring", type);
            return null;
        }
    }
}