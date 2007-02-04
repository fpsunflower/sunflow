package org.sunflow.core;

import org.sunflow.core.gi.GIEngineFactory;
import org.sunflow.core.photonmap.CausticPhotonMap;
import org.sunflow.image.Color;
import org.sunflow.math.Point3;
import org.sunflow.math.QMC;
import org.sunflow.math.Vector3;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

class LightServer {
    // parent
    private Scene scene;

    // lighting
    private LightSource[] lights;

    // shading override
    private Shader shaderOverride;
    private boolean shaderOverridePhotons;

    // direct illumination
    private int maxDiffuseDepth;
    private int maxReflectionDepth;
    private int maxRefractionDepth;

    // indirect illumination
    private CausticPhotonMapInterface causticPhotonMap;
    private GIEngine giEngine;
    private int photonCounter;

    // shading cache
    private CacheEntry[] shadingCache;
    private float shadingCacheResolution;
    private long cacheLookups;
    private long cacheEmptyEntryMisses;
    private long cacheWrongEntryMisses;
    private long cacheEntryAdditions;
    private long cacheHits;

    private static class CacheEntry {
        int cx, cy;
        Sample first;
    }

    private static class Sample {
        Instance i;
        Shader s;
        // int prim;
        float nx, ny, nz;
        Color c;
        Sample next; // linked list
    }

    LightServer(Scene scene) {
        this.scene = scene;
        lights = new LightSource[0];
        causticPhotonMap = null;

        shaderOverride = null;
        shaderOverridePhotons = false;

        maxDiffuseDepth = 1;
        maxReflectionDepth = 4;
        maxRefractionDepth = 4;

        causticPhotonMap = null;
        giEngine = null;

        shadingCache(0);
    }

    void setLights(LightSource[] lights) {
        this.lights = lights;
    }

    void shadingCache(float shadingRate) {
        shadingCache = shadingRate > 0 ? new CacheEntry[4096] : null;
        shadingCacheResolution = (float) (1 / Math.sqrt(shadingRate));
    }

    Scene getScene() {
        return scene;
    }

    void setShaderOverride(Shader shader, boolean photonOverride) {
        shaderOverride = shader;
        shaderOverridePhotons = photonOverride;
    }

    boolean build(Options options) {
        // read options
        maxDiffuseDepth = options.getInt("depths.diffuse", maxDiffuseDepth);
        maxReflectionDepth = options.getInt("depths.reflection", maxReflectionDepth);
        maxRefractionDepth = options.getInt("depths.refraction", maxRefractionDepth);
        giEngine = GIEngineFactory.create(options);
        String caustics = options.getString("caustics", null);
        if (caustics == null || caustics.equals("none"))
            causticPhotonMap = null;
        else if (caustics != null && caustics.equals("kd"))
            causticPhotonMap = new CausticPhotonMap(options);
        else {
            UI.printWarning(Module.LIGHT, "Unrecognized caustics photon map engine \"%s\" - ignoring", caustics);
            causticPhotonMap = null;
        }

        // validate options
        maxDiffuseDepth = Math.max(0, maxDiffuseDepth);
        maxReflectionDepth = Math.max(0, maxReflectionDepth);
        maxRefractionDepth = Math.max(0, maxRefractionDepth);

        Timer t = new Timer();
        t.start();
        // count total number of light samples
        int numLightSamples = 0;
        for (int i = 0; i < lights.length; i++)
            numLightSamples += lights[i].getNumSamples();
        // initialize gi engine
        if (giEngine != null) {
            if (!giEngine.init(scene))
                return false;
        }

        if (!calculatePhotons(causticPhotonMap, "caustic", 0))
            return false;
        t.end();
        cacheLookups = 0;
        cacheHits = 0;
        cacheEmptyEntryMisses = 0;
        cacheWrongEntryMisses = 0;
        cacheEntryAdditions = 0;
        if (shadingCache != null) {
            // clear shading cache
            for (int i = 0; i < shadingCache.length; i++)
                shadingCache[i] = null;
        }
        UI.printInfo(Module.LIGHT, "Light Server stats:");
        UI.printInfo(Module.LIGHT, "  * Light sources found: %d", lights.length);
        UI.printInfo(Module.LIGHT, "  * Light samples:       %d", numLightSamples);
        UI.printInfo(Module.LIGHT, "  * Max raytrace depth:");
        UI.printInfo(Module.LIGHT, "      - Diffuse          %d", maxDiffuseDepth);
        UI.printInfo(Module.LIGHT, "      - Reflection       %d", maxReflectionDepth);
        UI.printInfo(Module.LIGHT, "      - Refraction       %d", maxRefractionDepth);
        UI.printInfo(Module.LIGHT, "  * GI engine            %s", options.getString("gi.engine", "none"));
        UI.printInfo(Module.LIGHT, "  * Caustics:            %s", caustics == null ? "none" : caustics);
        UI.printInfo(Module.LIGHT, "  * Shader override:     %b", shaderOverride);
        UI.printInfo(Module.LIGHT, "  * Photon override:     %b", shaderOverridePhotons);
        UI.printInfo(Module.LIGHT, "  * Shading cache:       %s", shadingCache == null ? "off" : "on");
        UI.printInfo(Module.LIGHT, "  * Build time:          %s", t.toString());
        return true;
    }

    void showStats() {
        if (shadingCache == null)
            return;
        int numUsedEntries = 0;
        for (CacheEntry e : shadingCache)
            numUsedEntries += (e != null) ? 1 : 0;
        UI.printInfo(Module.LIGHT, "Shading cache stats:");
        UI.printInfo(Module.LIGHT, "  * Used entries:        %d (%d%%)", numUsedEntries, (100 * numUsedEntries) / shadingCache.length);
        UI.printInfo(Module.LIGHT, "  * Lookups:             %d", cacheLookups);
        UI.printInfo(Module.LIGHT, "  * Hits:                %d", cacheHits);
        UI.printInfo(Module.LIGHT, "  * Hit rate:            %d%%", (100 * cacheHits) / cacheLookups);
        UI.printInfo(Module.LIGHT, "  * Empty entry misses:  %d", cacheEmptyEntryMisses);
        UI.printInfo(Module.LIGHT, "  * Wrong entry misses:  %d", cacheWrongEntryMisses);
        UI.printInfo(Module.LIGHT, "  * Entry adds:          %d", cacheEntryAdditions);
    }

    boolean calculatePhotons(final PhotonStore map, String type, final int seed) {
        if (map == null)
            return true;
        if (lights.length == 0) {
            UI.printError(Module.LIGHT, "Unable to trace %s photons, no lights in scene", type);
            return false;
        }
        final float[] histogram = new float[lights.length];
        histogram[0] = lights[0].getPower();
        for (int i = 1; i < lights.length; i++)
            histogram[i] = histogram[i - 1] + lights[i].getPower();
        UI.printInfo(Module.LIGHT, "Tracing %s photons ...", type);
        int numEmittedPhotons = map.numEmit();
        if (numEmittedPhotons <= 0 || histogram[histogram.length - 1] <= 0) {
            UI.printError(Module.LIGHT, "Photon mapping enabled, but no %s photons to emit", type);
            return false;
        }
        map.prepare(scene.getBounds());
        UI.taskStart("Tracing " + type + " photons", 0, numEmittedPhotons);
        Thread[] photonThreads = new Thread[scene.getThreads()];
        final float scale = 1.0f / numEmittedPhotons;
        int delta = numEmittedPhotons / photonThreads.length;
        photonCounter = 0;
        Timer photonTimer = new Timer();
        photonTimer.start();
        for (int i = 0; i < photonThreads.length; i++) {
            final int threadID = i;
            final int start = threadID * delta;
            final int end = (threadID == (photonThreads.length - 1)) ? numEmittedPhotons : (threadID + 1) * delta;
            photonThreads[i] = new Thread(new Runnable() {
                public void run() {
                    IntersectionState istate = new IntersectionState();
                    for (int i = start; i < end; i++) {
                        synchronized (LightServer.this) {
                            UI.taskUpdate(photonCounter);
                            photonCounter++;
                            if (UI.taskCanceled())
                                return;
                        }

                        int qmcI = i + seed;

                        double rand = QMC.halton(0, qmcI) * histogram[histogram.length - 1];
                        int j = 0;
                        while (rand >= histogram[j] && j < histogram.length)
                            j++;
                        // make sure we didn't pick a zero-probability light
                        if (j == histogram.length)
                            continue;

                        double randX1 = (j == 0) ? rand / histogram[0] : (rand - histogram[j]) / (histogram[j] - histogram[j - 1]);
                        double randY1 = QMC.halton(1, qmcI);
                        double randX2 = QMC.halton(2, qmcI);
                        double randY2 = QMC.halton(3, qmcI);
                        Point3 pt = new Point3();
                        Vector3 dir = new Vector3();
                        Color power = new Color();
                        lights[j].getPhoton(randX1, randY1, randX2, randY2, pt, dir, power);
                        power.mul(scale);
                        Ray r = new Ray(pt, dir);
                        scene.trace(r, istate);
                        if (istate.hit())
                            shadePhoton(ShadingState.createPhotonState(r, istate, qmcI, map, LightServer.this), power);
                    }
                }
            });
            photonThreads[i].setPriority(scene.getThreadPriority());
            photonThreads[i].start();
        }
        for (int i = 0; i < photonThreads.length; i++) {
            try {
                photonThreads[i].join();
            } catch (InterruptedException e) {
                UI.printError(Module.LIGHT, "Photon thread %d of %d was interrupted", i + 1, photonThreads.length);
                return false;
            }
        }
        if (UI.taskCanceled()) {
            UI.taskStop(); // shut down task cleanly
            return false;
        }
        photonTimer.end();
        UI.taskStop();
        UI.printInfo(Module.LIGHT, "Tracing time for %s photons: %s", type, photonTimer.toString());
        map.init();
        return true;
    }

    void shadePhoton(ShadingState state, Color power) {
        state.getInstance().prepareShadingState(state);
        Shader shader = getPhotonShader(state);
        // scatter photon
        if (shader != null)
            shader.scatterPhoton(state, power);
    }

    void traceDiffusePhoton(ShadingState previous, Ray r, Color power) {
        if (previous.getDiffuseDepth() >= maxDiffuseDepth)
            return;
        IntersectionState istate = previous.getIntersectionState();
        scene.trace(r, istate);
        if (previous.getIntersectionState().hit()) {
            // create a new shading context
            ShadingState state = ShadingState.createDiffuseBounceState(previous, r, 0);
            shadePhoton(state, power);
        }
    }

    void traceReflectionPhoton(ShadingState previous, Ray r, Color power) {
        if (previous.getReflectionDepth() >= maxReflectionDepth)
            return;
        IntersectionState istate = previous.getIntersectionState();
        scene.trace(r, istate);
        if (previous.getIntersectionState().hit()) {
            // create a new shading context
            ShadingState state = ShadingState.createReflectionBounceState(previous, r, 0);
            shadePhoton(state, power);
        }
    }

    void traceRefractionPhoton(ShadingState previous, Ray r, Color power) {
        if (previous.getRefractionDepth() >= maxRefractionDepth)
            return;
        IntersectionState istate = previous.getIntersectionState();
        scene.trace(r, istate);
        if (previous.getIntersectionState().hit()) {
            // create a new shading context
            ShadingState state = ShadingState.createRefractionBounceState(previous, r, 0);
            shadePhoton(state, power);
        }
    }

    private Shader getShader(ShadingState state) {
        return shaderOverride != null ? shaderOverride : state.getShader();
    }

    private Shader getPhotonShader(ShadingState state) {
        return (shaderOverride != null && shaderOverridePhotons) ? shaderOverride : state.getShader();

    }

    ShadingState getRadiance(float rx, float ry, int i, Ray r, IntersectionState istate) {
        scene.trace(r, istate);
        if (istate.hit()) {
            ShadingState state = ShadingState.createState(istate, rx, ry, r, i, this);
            state.getInstance().prepareShadingState(state);
            Shader shader = getShader(state);
            if (shader == null) {
                state.setResult(Color.BLACK);
                return state;
            }
            if (shadingCache != null) {
                Color c = lookupShadingCache(state, shader);
                if (c != null) {
                    state.setResult(c);
                    return state;
                }
            }
            state.setResult(shader.getRadiance(state));
            if (shadingCache != null)
                addShadingCache(state, shader, state.getResult());
            return state;
        } else
            return null;
    }

    void shadeBakeResult(ShadingState state) {
        Shader shader = getShader(state);
        if (shader != null)
            state.setResult(shader.getRadiance(state));
        else
            state.setResult(Color.BLACK);
    }

    Color shadeHit(ShadingState state) {
        state.getInstance().prepareShadingState(state);
        Shader shader = getShader(state);
        return (shader != null) ? shader.getRadiance(state) : Color.BLACK;
    }

    private static final int hash(int x, int y) {
        // long bits = java.lang.Double.doubleToLongBits(x);
        // bits ^= java.lang.Double.doubleToLongBits(y) * 31;
        // return (((int) bits) ^ ((int) (bits >> 32)));
        return x ^ y;
    }

    private synchronized Color lookupShadingCache(ShadingState state, Shader shader) {
        if (state.getNormal() == null)
            return null;
        cacheLookups++;
        int cx = (int) (state.getRasterX() * shadingCacheResolution);
        int cy = (int) (state.getRasterY() * shadingCacheResolution);
        int hash = hash(cx, cy);
        CacheEntry e = shadingCache[hash & (shadingCache.length - 1)];
        if (e == null) {
            cacheEmptyEntryMisses++;
            return null;
        }
        // entry maps to correct pixel
        if (e.cx == cx && e.cy == cy) {
            // search further
            for (Sample s = e.first; s != null; s = s.next) {
                if (s.i != state.getInstance())
                    continue;
                // if (s.prim != state.getPrimitiveID())
                // continue;
                if (s.s != shader)
                    continue;
                if (state.getNormal().dot(s.nx, s.ny, s.nz) < 0.95f)
                    continue;
                // we have a match
                cacheHits++;
                return s.c;
            }
        } else
            cacheWrongEntryMisses++;
        return null;
    }

    private synchronized void addShadingCache(ShadingState state, Shader shader, Color c) {
        // don't cache samples with null normals
        if (state.getNormal() == null)
            return;
        cacheEntryAdditions++;
        int cx = (int) (state.getRasterX() * shadingCacheResolution);
        int cy = (int) (state.getRasterY() * shadingCacheResolution);
        int h = hash(cx, cy) & (shadingCache.length - 1);
        CacheEntry e = shadingCache[h];
        // new entry ?
        if (e == null)
            e = shadingCache[h] = new CacheEntry();
        Sample s = new Sample();
        s.i = state.getInstance();
        // s.prim = state.getPrimitiveID();
        s.s = shader;
        s.c = c;
        s.nx = state.getNormal().x;
        s.ny = state.getNormal().y;
        s.nz = state.getNormal().z;
        if (e.cx == cx && e.cy == cy) {
            // same pixel - just add to the front of the list
            s.next = e.first;
            e.first = s;
        } else {
            // different pixel - new list
            e.cx = cx;
            e.cy = cy;
            s.next = null;
            e.first = s;
        }
    }

    Color traceGlossy(ShadingState previous, Ray r, int i) {
        // limit path depth and disable caustic paths
        if (previous.getReflectionDepth() >= maxReflectionDepth || previous.getDiffuseDepth() > 0)
            return Color.BLACK;
        IntersectionState istate = previous.getIntersectionState();
        scene.trace(r, istate);
        return istate.hit() ? shadeHit(ShadingState.createGlossyBounceState(previous, r, i)) : Color.BLACK;
    }

    Color traceReflection(ShadingState previous, Ray r, int i) {
        // limit path depth and disable caustic paths
        if (previous.getReflectionDepth() >= maxReflectionDepth || previous.getDiffuseDepth() > 0)
            return Color.BLACK;
        IntersectionState istate = previous.getIntersectionState();
        scene.trace(r, istate);
        return istate.hit() ? shadeHit(ShadingState.createReflectionBounceState(previous, r, i)) : Color.BLACK;
    }

    Color traceRefraction(ShadingState previous, Ray r, int i) {
        // limit path depth and disable caustic paths
        if (previous.getRefractionDepth() >= maxRefractionDepth || previous.getDiffuseDepth() > 0)
            return Color.BLACK;
        IntersectionState istate = previous.getIntersectionState();
        scene.trace(r, istate);
        return istate.hit() ? shadeHit(ShadingState.createRefractionBounceState(previous, r, i)) : Color.BLACK;
    }

    ShadingState traceFinalGather(ShadingState previous, Ray r, int i) {
        if (previous.getDiffuseDepth() >= maxDiffuseDepth)
            return null;
        IntersectionState istate = previous.getIntersectionState();
        scene.trace(r, istate);
        return istate.hit() ? ShadingState.createFinalGatherState(previous, r, i) : null;
    }

    Color getGlobalRadiance(ShadingState state) {
        if (giEngine == null)
            return Color.BLACK;
        return giEngine.getGlobalRadiance(state);
    }

    Color getIrradiance(ShadingState state, Color diffuseReflectance) {
        // no gi engine, or we have already exceeded number of available bounces
        if (giEngine == null || state.getDiffuseDepth() >= maxDiffuseDepth)
            return Color.BLACK;
        return giEngine.getIrradiance(state, diffuseReflectance);
    }

    void initLightSamples(ShadingState state) {
        for (LightSource l : lights)
            l.getSamples(state);
    }

    void initCausticSamples(ShadingState state) {
        if (causticPhotonMap != null)
            causticPhotonMap.getSamples(state);
    }
}