package org.sunflow.core;

import org.sunflow.image.Color;

import java.util.Arrays;

public class ShadingCache {
    private final Sample[] samples = new Sample[256];
    private int depth;
    // stats
    long hits;
    long misses;
    long sumDepth;
    long numCaches;

    private final float dirTolerance, normalTolerance;

    private static class Sample {
        Instance i;
        Shader s;
        float nx, ny, nz;
        float dx, dy, dz;
        Color c;
    }

    public ShadingCache(float dirTolerance, float normalTolerance) {
        reset();
        hits = 0;
        misses = 0;
        this.dirTolerance = dirTolerance;
        this.normalTolerance = normalTolerance;
    }

    public void reset() {
        sumDepth += depth;
        if (depth > 0)
            numCaches++;
        Arrays.fill(samples, null);
        depth = 0;
    }

    public Color lookup(ShadingState state, Shader shader) {
        if (state.getNormal() == null)
            return null;
        // search further
        for (int i = 0; i < depth; i++) {
            Sample s = samples[i];
            if (s.i != state.getInstance())
                continue;
            if (s.s != shader)
                continue;
            if (state.getRay().dot(s.dx, s.dy, s.dz) < 1 - dirTolerance)
                continue;
            if (state.getNormal().dot(s.nx, s.ny, s.nz) < 1 - normalTolerance)
                continue;
            // we have a match
            hits++;
            return s.c;
        }
        misses++;
        return null;
    }

    public void add(ShadingState state, Shader shader, Color c) {
        if (state.getNormal() == null || depth >= samples.length)
            return;
        Sample s = new Sample();
        s.i = state.getInstance();
        s.s = shader;
        s.c = c;
        s.dx = state.getRay().dx;
        s.dy = state.getRay().dy;
        s.dz = state.getRay().dz;
        s.nx = state.getNormal().x;
        s.ny = state.getNormal().y;
        s.nz = state.getNormal().z;
        samples[depth] = s;
        depth++;
    }
}