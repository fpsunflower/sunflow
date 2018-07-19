package org.sunflow.core.parameter.gi;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.core.parameter.IlluminationParameter;

/**
 * Global Illumination Irradiance Cache
 */
public class IrrCacheGIParameter extends GlobalIlluminationParameter {

    public static final String PARAM_TOLERANCE = "gi.irr-cache.tolerance";
    public static final String PARAM_SAMPLES = "gi.irr-cache.samples";
    public static final String PARAM_MIN_SPACING = "gi.irr-cache.min_spacing";
    public static final String PARAM_MAX_SPACING = "gi.irr-cache.max_spacing";
    public static final String PARAM_GLOBAL_EMIT = "gi.irr-cache.gmap.emit";
    public static final String PARAM_GLOBAL = "gi.irr-cache.gmap";
    public static final String PARAM_GLOBAL_GATHER = "gi.irr-cache.gmap.gather";
    public static final String PARAM_GLOBAL_RADIUS = "gi.irr-cache.gmap.radius";

    int samples = 0;
    float tolerance;
    float minSpacing;
    float maxSpacing;

    IlluminationParameter global = null;

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter(GlobalIlluminationParameter.PARAM_ENGINE, GlobalIlluminationParameter.TYPE_IRR_CACHE);
        api.parameter(PARAM_SAMPLES, samples);
        api.parameter(PARAM_TOLERANCE, tolerance);
        api.parameter(PARAM_MIN_SPACING, minSpacing);
        api.parameter(PARAM_MAX_SPACING, maxSpacing);

        if (global != null) {
            api.parameter(PARAM_GLOBAL_EMIT, global.getEmit());
            api.parameter(PARAM_GLOBAL, global.getMap());
            api.parameter(PARAM_GLOBAL_GATHER, global.getGather());
            api.parameter(PARAM_GLOBAL_RADIUS, global.getRadius());
        }
        super.setup(api);
    }

    public int getSamples() {
        return samples;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }

    public float getTolerance() {
        return tolerance;
    }

    public void setTolerance(float tolerance) {
        this.tolerance = tolerance;
    }

    public float getMinSpacing() {
        return minSpacing;
    }

    public void setMinSpacing(float minSpacing) {
        this.minSpacing = minSpacing;
    }

    public float getMaxSpacing() {
        return maxSpacing;
    }

    public void setMaxSpacing(float maxSpacing) {
        this.maxSpacing = maxSpacing;
    }

    public IlluminationParameter getGlobal() {
        return global;
    }

    public void setGlobal(IlluminationParameter global) {
        this.global = global;
    }
}
