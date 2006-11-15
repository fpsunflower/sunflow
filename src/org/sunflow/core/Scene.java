package org.sunflow.core;

import org.sunflow.core.display.FrameDisplay;
import org.sunflow.image.Color;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.MathUtils;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

/**
 * Represents a entire scene, defined as a collection of objects viewed by a
 * camera.
 */
public class Scene {
    // scene storage
    private LightServer lightServer;
    private InstanceList instanceList;
    private InstanceList infiniteInstanceList;
    private Camera camera;
    private AccelerationStructure intAccel;
    private String acceltype;

    private boolean rebuildAccel;

    // image size
    private int imageWidth;
    private int imageHeight;

    // filtering
    private Filter filter;

    // global options
    private int threads;
    private boolean lowPriority;

    /**
     * Creates an empty scene with default anti-aliasing parameters.
     */
    public Scene() {
        lightServer = new LightServer(this);
        instanceList = new InstanceList();
        infiniteInstanceList = new InstanceList();
        acceltype = "auto";

        camera = null;
        imageWidth = 640;
        imageHeight = 480;
        filter = null;
        threads = 0;
        lowPriority = true;

        rebuildAccel = true;
    }

    /**
     * Get number of allowed threads for multi-threaded operations.
     * 
     * @return number of threads that can be started
     */
    public int getThreads() {
        return threads <= 0 ? Runtime.getRuntime().availableProcessors() : threads;
    }

    /**
     * Get the priority level to assign to multi-threaded operations.
     * 
     * @return thread priority
     */
    public int getThreadPriority() {
        return lowPriority ? Thread.MIN_PRIORITY : Thread.NORM_PRIORITY;
    }

    /**
     * Sets the intersetion accelerator used to accelerate raytracing. This can
     * be changed up until rendertime.
     * 
     * @param accel intersection accelerator to use
     */
    public void setIntersectionAccelerator(String name) {
        rebuildAccel = acceltype.equals(name);
        acceltype = name;
    }

    /**
     * Sets the current camera (no support for multiple cameras yet).
     * 
     * @param camera camera to be used as the viewpoint for the scene
     */
    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    /**
     * Update the instance lists for this scene.
     * 
     * @param instances regular instances
     * @param infinite infinite instances (no bounds)
     */
    public void setInstanceLists(Instance[] instances, Instance[] infinite) {
        infiniteInstanceList = new InstanceList(infinite);
        instanceList = new InstanceList(instances);
        rebuildAccel = true;
    }

    /**
     * Update the light list for this scene.
     * 
     * @param lights array of light source objects
     */
    public void setLightList(LightSource[] lights) {
        lightServer.setLights(lights);
    }

    /**
     * Sets which filter type used to filter image samples into the final.
     * pixels.
     * 
     * @param f filter to be used
     */
    public void setFilter(Filter f) {
        filter = f;
    }

    /**
     * Sets the type of map to use for caustic photons. You may pass
     * <code>null</code> to disable caustics.
     * 
     * @param cmap caustic photon map data structure
     */
    public void photons(CausticPhotonMapInterface cmap) {
        lightServer.photons(cmap);
    }

    /**
     * Enables shader overiding (set null to disable). The specified shader will
     * be used to shade all surfaces
     * 
     * @param shader shader to run over all surfaces, or <code>null</code> to
     *            disable overriding
     * @param photonOverride <code>true</code> to override photon scattering
     *            with this shader or <code>false</code> to run the regular
     *            shaders
     */
    public void setShaderOverride(Shader shader, boolean photonOverride) {
        lightServer.setShaderOverride(shader, photonOverride);
    }

    public void giEngine(GIEngine engine) {
        lightServer.giEngine(engine);
    }

    public ShadingState getRadiance(IntersectionState istate, float rx, float ry, double lensU, double lensV, double time, int instance) {
        Ray r = camera.getRay(rx, ry, imageWidth, imageHeight, lensU, lensV, time);
        return r != null ? lightServer.getRadiance(rx, ry, instance, r, istate) : null;
    }

    public Filter getFilter() {
        return filter;
    }

    public BoundingBox getBounds() {
        return instanceList.getWorldBounds(null);
    }

    void trace(Ray r, IntersectionState state) {
        // reset object
        state.instance = null;
        state.current = null;
        for (int i = 0; i < infiniteInstanceList.getNumPrimitives(); i++)
            infiniteInstanceList.intersectPrimitive(r, i, state);
        // reset for next accel structure
        state.current = null;
        intAccel.intersect(r, state);
    }

    Color traceShadow(Ray r, IntersectionState state) {
        trace(r, state);
        return state.hit() ? Color.WHITE : Color.BLACK;
    }

    public void render(Options options, ImageSampler sampler, Display display) {
        if (display == null)
            display = new FrameDisplay();
        if (camera == null) {
            UI.printError(Module.SCENE, "No camera found");
            return;
        }
        // read from options
        threads = options.getInt("threads", threads);
        lowPriority = options.getBoolean("threads.lowPriority", lowPriority);
        imageWidth = options.getInt("resolutionX", imageWidth);
        imageHeight = options.getInt("resolutionY", imageHeight);
        // limit resolution to 16k
        imageWidth = MathUtils.clamp(imageWidth, 1, 1 << 14);
        imageHeight = MathUtils.clamp(imageHeight, 1, 1 << 14);

        // count scene primitives
        long numPrimitives = 0;
        for (int i = 0; i < instanceList.getNumPrimitives(); i++)
            numPrimitives += instanceList.getNumPrimitives(i);
        UI.printInfo(Module.SCENE, "Scene stats:");
        UI.printInfo(Module.SCENE, "  * Infinite instances:  %d", infiniteInstanceList.getNumPrimitives());
        UI.printInfo(Module.SCENE, "  * Instances:           %d", instanceList.getNumPrimitives());
        UI.printInfo(Module.SCENE, "  * Primitives:          %d", numPrimitives);
        if (rebuildAccel) {
            intAccel = AccelerationStructureFactory.create(acceltype, instanceList.getNumPrimitives(), false);
            intAccel.build(instanceList);
            rebuildAccel = false;
        }
        UI.printInfo(Module.SCENE, "  * Scene bounds:        %s", getBounds());
        UI.printInfo(Module.SCENE, "  * Scene center:        %s", getBounds().getCenter());
        UI.printInfo(Module.SCENE, "  * Scene diameter:      %.2f", getBounds().getExtents().length());
        if (sampler == null)
            return;
        if (!lightServer.build(options))
            return;
        // render
        UI.printInfo(Module.SCENE, "Rendering ...");
        sampler.prepare(options, this, imageWidth, imageHeight);
        sampler.render(display);
        lightServer.showStats();
        UI.printInfo(Module.SCENE, "Done.");
    }

    public boolean calculatePhotons(PhotonStore map, String type, int seed) {
        return lightServer.calculatePhotons(map, type, seed);
    }
}