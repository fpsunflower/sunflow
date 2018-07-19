package org.sunflow.core.parameter;

import org.sunflow.SunflowAPI;
import org.sunflow.SunflowAPIInterface;

/**
 * Image Block based on SCParser.parseImageBlock()
 */
public class ImageParameter implements Parameter {

    public static final String PARAM_AA_CACHE = "aa.cache";
    public static final String PARAM_AA_CONTRAST = "aa.contrast";
    public static final String PARAM_AA_DISPLAY = "aa.display";
    public static final String PARAM_AA_JITTER = "aa.jitter";
    public static final String PARAM_AA_MIN = "aa.min";
    public static final String PARAM_AA_MAX = "aa.max";
    public static final String PARAM_AA_SAMPLES = "aa.samples";
    public static final String PARAM_RESOLUTION_X = "resolutionX";
    public static final String PARAM_RESOLUTION_Y = "resolutionY";
    public static final String PARAM_SAMPLER = "sampler";
    public static final String PARAM_FILTER = "filter";

    public static final String FILTER_TRIANGLE = "triangle";
    public static final String FILTER_GAUSSIAN = "gaussian";
    public static final String FILTER_MITCHELL = "mitchel";
    public static final String FILTER_BLACKMAN_HARRIS = "blackman-harris";

    int resolutionX = 1920;
    int resolutionY = 1080;
    int aaMin = 0;
    int aaMax = 2;
    int aaSamples = 4;
    float aaContrast = 0;
    boolean aaJitter = false;
    boolean aaCache = false;

    String sampler = "";
    String filter = "";

    public void setup(SunflowAPIInterface api) {
        if (resolutionX > 0) {
            api.parameter(PARAM_RESOLUTION_X, resolutionX);
        }
        if (resolutionY > 0) {
            api.parameter(PARAM_RESOLUTION_Y, resolutionY);
        }

        // Always set AA params
        api.parameter(PARAM_AA_MIN, aaMin);
        api.parameter(PARAM_AA_MAX, aaMax);

        if (aaSamples > 0) {
            api.parameter(PARAM_AA_SAMPLES, aaSamples);
        }
        if (aaContrast != 0) {
            api.parameter(PARAM_AA_CONTRAST, aaContrast);
        }

        api.parameter(PARAM_AA_JITTER, aaJitter);

        if (!sampler.isEmpty()) {
            api.parameter(PARAM_SAMPLER, sampler);
        }
        if (!filter.isEmpty()) {
            api.parameter(PARAM_FILTER, filter);
        }

        api.parameter(PARAM_AA_CACHE, aaCache);

        api.options(SunflowAPI.DEFAULT_OPTIONS);
    }

    public int getResolutionX() {
        return resolutionX;
    }

    public void setResolutionX(int resolutionX) {
        this.resolutionX = resolutionX;
    }

    public int getResolutionY() {
        return resolutionY;
    }

    public void setResolutionY(int resolutionY) {
        this.resolutionY = resolutionY;
    }

    public int getAAMin() {
        return aaMin;
    }

    public void setAAMin(int aaMin) {
        this.aaMin = aaMin;
    }

    public int getAAMax() {
        return aaMax;
    }

    public void setAAMax(int aaMax) {
        this.aaMax = aaMax;
    }

    public int getAASamples() {
        return aaSamples;
    }

    public void setAASamples(int aaSamples) {
        this.aaSamples = aaSamples;
    }

    public float getAAContrast() {
        return aaContrast;
    }

    public void setAAContrast(float aaContrast) {
        this.aaContrast = aaContrast;
    }

    public boolean isAAJitter() {
        return aaJitter;
    }

    public void setAAJitter(boolean aaJitter) {
        this.aaJitter = aaJitter;
    }

    public boolean isAACache() {
        return aaCache;
    }

    public void setAACache(boolean aaCache) {
        this.aaCache = aaCache;
    }

    public String getSampler() {
        return sampler;
    }

    public void setSampler(String sampler) {
        this.sampler = sampler;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }
}
