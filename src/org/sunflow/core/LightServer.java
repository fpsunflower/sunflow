package org.sunflow.core;

import java.util.ArrayList;

import org.sunflow.image.Color;
import org.sunflow.math.Point3;
import org.sunflow.math.QMC;
import org.sunflow.math.Vector3;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;

class LightServer {
    // parent
    private Scene scene;

    // lighting
    private ArrayList<LightSource> lightList;
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

    LightServer(Scene scene) {
        this.scene = scene;
        lightList = new ArrayList<LightSource>();
        lights = null;
        causticPhotonMap = null;

        shaderOverride = null;
        shaderOverridePhotons = false;

        maxDiffuseDepth = 1;
        maxReflectionDepth = 4;
        maxRefractionDepth = 4;

        causticPhotonMap = null;
        giEngine = null;
    }

    Scene getScene() {
        return scene;
    }

    void setMaxDepth(int maxDiffuseDepth, int maxReflectionDepth, int maxRefractionDepth) {
        this.maxDiffuseDepth = maxDiffuseDepth;
        this.maxReflectionDepth = maxReflectionDepth;
        this.maxRefractionDepth = maxRefractionDepth;
    }

    void photons(CausticPhotonMapInterface cmap) {
        causticPhotonMap = cmap;
    }

    void setShaderOverride(Shader shader, boolean photonOverride) {
        shaderOverride = shader;
        shaderOverridePhotons = photonOverride;
    }

    void giEngine(GIEngine engine) {
        giEngine = engine;
    }

    void registerLight(LightSource light) {
        lightList.add(light);
    }

    boolean build() {
        // validate options
        maxDiffuseDepth = Math.max(0, maxDiffuseDepth);
        maxReflectionDepth = Math.max(0, maxReflectionDepth);
        maxRefractionDepth = Math.max(0, maxRefractionDepth);

        Timer t = new Timer();
        t.start();
        lights = lightList.toArray(new LightSource[lightList.size()]);
        // count total number of light samples
        int numLightSamples = 0;
        for (int i = 0; i < lights.length; i++) {
            assert lights[i].getNumSamples() >= 0;
            numLightSamples += lights[i].getNumSamples();
        }
        // initialize gi engine
        if (giEngine != null) {
            if (!giEngine.init(scene))
                return false;
        }

        if (!calculatePhotons(causticPhotonMap, "caustic", 0))
            return false;
        t.end();
        UI.printInfo("[LSV] Light Server stats:");
        UI.printInfo("[LSV]   * Light sources found: %d", lights.length);
        UI.printInfo("[LSV]   * Light samples:       %d", numLightSamples);
        UI.printInfo("[LSV]   * Max raytrace depth:");
        UI.printInfo("[LSV]       - Diffuse          %d", maxDiffuseDepth);
        UI.printInfo("[LSV]       - Reflection       %d", maxReflectionDepth);
        UI.printInfo("[LSV]       - Refraction       %d", maxRefractionDepth);
        UI.printInfo("[LSV]   * Shader override:     %b", shaderOverride);
        UI.printInfo("[LSV]   * Photon override:     %b", shaderOverridePhotons);
        UI.printInfo("[LSV]   * Build time:          %s", t.toString());
        return true;
    }

    boolean calculatePhotons(final PhotonStore map, String type, final int seed) {
        if (map == null)
            return true;
        if (lights.length == 0) {
            UI.printError("[LSV] Unable to trace %s photons, no lights in scene", type);
            return false;
        }
        final float[] histogram = new float[lights.length];
        histogram[0] = lights[0].getPower();
        for (int i = 1; i < lights.length; i++)
            histogram[i] = histogram[i - 1] + lights[i].getPower();
        UI.printInfo("[LSV] Tracing %s photons ...", type);
        int numEmittedPhotons = map.numEmit();
        if (numEmittedPhotons <= 0 || histogram[histogram.length - 1] <= 0) {
            UI.printError("[LSV] Photon mapping enabled, but no %s photons to emit", type);
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
                UI.printError("[LSV] Photon thread %d of %d was interrupted", i + 1, photonThreads.length);
                return false;
            }
        }
        if (UI.taskCanceled()) {
            UI.taskStop(); // shut down task cleanly
            return false;
        }
        photonTimer.end();
        UI.taskStop();
        UI.printInfo("[LSV] Tracing time for %s photons: %s", type, photonTimer.toString());
        map.init();
        return true;
    }

    void shadePhoton(ShadingState state, Color power) {
        state.getInstance().prepareShadingState(state);
        // figure out which shader to use
        Shader shader = shaderOverride;
        if (shader == null || !shaderOverridePhotons)
            shader = state.getShader();
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

    ShadingState getRadiance(float rx, float ry, int i, Ray r, IntersectionState istate) {
        scene.trace(r, istate);
        if (istate.hit()) {
            ShadingState state = ShadingState.createState(istate, rx, ry, r, i, this);
            state.setResult(shadeHit(state));
            return state;
        } else
            return null;
    }

    Color shadeHit(ShadingState state) {
        state.getInstance().prepareShadingState(state);
        Shader shader = shaderOverride;
        if (shader == null)
            shader = state.getShader();
        return (shader != null) ? shader.getRadiance(state) : Color.BLACK;
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
        for (int i = 0; i < lights.length; i++) {
            if (!lights[i].isVisible(state))
                continue;
            boolean adaptive = lights[i].isAdaptive();
            // reduce sampling of adaptive lights for diffuse reflections
            int n = adaptive && state.getDiffuseDepth() > 0 ? 1 : lights[i].getNumSamples();
            float inv = 1.0f / n;
            for (int sample = 0; sample < n; sample++) {
                // regular sampling
                LightSample ls = new LightSample();
                lights[i].getSample(sample, state, ls);
                if (ls.isValid()) {
                    // divide by number of samples
                    ls.getDiffuseRadiance().mul(inv);
                    ls.getSpecularRadiance().mul(inv);
                    state.addSample(ls);
                }
            }
        }
    }

    void initCausticSamples(ShadingState state) {
        if (causticPhotonMap != null)
            causticPhotonMap.getSamples(state);
    }
}