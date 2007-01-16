package org.sunflow.core.filter;

import org.sunflow.core.Filter;

public final class FilterFactory {
    public static final Filter get(String filter) {
        if (filter.equals("box"))
            return new BoxFilter(1);
        else if (filter.equals("gaussian"))
            return new GaussianFilter(3);
        else if (filter.equals("mitchell"))
            return new MitchellFilter();
        else if (filter.equals("catmull-rom"))
            return new CatmullRomFilter();
        else if (filter.equals("blackman-harris"))
            return new BlackmanHarrisFilter(4);
        else if (filter.equals("sinc"))
            return new SincFilter(4);
        else if (filter.equals("lanczos"))
            return new LanczosFilter();
        else if (filter.equals("triangle"))
            return new TriangleFilter(2);
        else
            return null;       
    }
}