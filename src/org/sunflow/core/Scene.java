package org.sunflow.core;

import org.sunflow.core.accel2.SimpleAccelerator;
import org.sunflow.core.accel2.UniformGrid;
import org.sunflow.core.display.FrameDisplay;
import org.sunflow.image.Color;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.MathUtils;
import org.sunflow.system.UI;

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

    private boolean changedGeometry;

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
        intAccel = new UniformGrid();

        camera = null;
        imageWidth = 640;
        imageHeight = 480;
        filter = null;
        threads = 0;
        lowPriority = true;

        changedGeometry = true;
    }

    public void setThreads(int threads, boolean lowPriority) {
        this.threads = threads;
        this.lowPriority = lowPriority;
    }

    public int getThreads() {
        return threads <= 0 ? Runtime.getRuntime().availableProcessors() : threads;
    }

    public int getThreadPriority() {
        return lowPriority ? Thread.MIN_PRIORITY : Thread.NORM_PRIORITY;
    }

    /**
     * Set final image resolution in pixels
     * 
     * @param imageWidth width in pixels
     * @param imageHeight height in pixels
     */
    public void setResolution(int imageWidth, int imageHeight) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    /**
     * Sets the intersetion accelerator used to accelerate raytracing. This can
     * be changed up until rendertime.
     * 
     * @param accel intersection accelerator to use
     */
    public void setIntersectionAccelerator(AccelerationStructure accel) {
        intAccel = accel;
        changedGeometry = true;
    }

    /**
     * Sets the current camera (no support for multiple cameras yet).
     * 
     * @param camera camera to be used as the viewpoint for the scene
     */
    public void addCamera(Camera camera) {
        this.camera = camera;
    }

    /**
     * Add an object to the scene.
     * 
     * @param prim object to be added to the scene
     */
    public void addInstance(Instance instance) {
        if (instance.getBounds() == null)
            infiniteInstanceList.add(instance);
        else {
            instanceList.add(instance);
            changedGeometry = true;
        }
    }

    /**
     * Adds a light source to the scene. Note that for area light sources you
     * will need to call {@link #addPrimitive(Primitive)}in order to make the
     * light source visible to the raytracer.
     * 
     * @param light light to be added to the scene
     */
    public void addLight(LightSource light) {
        lightServer.registerLight(light);
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
     * Sets the maximum raytracing depths per bounce type.
     * 
     * @param diffuseDepth maximum diffuse trace depth
     * @param reflectionDepth maximum reflection trace depth
     * @param refractionDepth maximum refraction trace depth
     */
    public void setMaxDepth(int diffuseDepth, int reflectionDepth, int refractionDepth) {
        lightServer.setMaxDepth(diffuseDepth, reflectionDepth, refractionDepth);
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
        return lightServer.getRadiance(ry, ry, instance, r, istate);
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

    public void render(ImageSampler sampler, Display display) {
        if (display == null)
            display = new FrameDisplay();
        if (camera == null) {
            UI.printError("[SCN] No camera found");
            return;
        }
        // limit resolution to 16k
        imageWidth = MathUtils.clamp(imageWidth, 1, 1 << 14);
        imageHeight = MathUtils.clamp(imageHeight, 1, 1 << 14);
        UI.printInfo("[SCN] Scene stats:");
        UI.printInfo("[SCN]   * Infinite Primitives: %d", infiniteInstanceList.getNumPrimitives());
        UI.printInfo("[SCN]   * Instances:           %d", instanceList.getNumPrimitives());
        if (changedGeometry) {
            instanceList.trim();
            // use special case if we have only one instance in the scene
            if (instanceList.getNumPrimitives() == 1)
                intAccel = new SimpleAccelerator();
            if (!intAccel.build(instanceList))
                return;
            changedGeometry = false;
        }
        infiniteInstanceList.trim();
        UI.printInfo("[SCN]   * Scene bounds:        %s", getBounds());
        UI.printInfo("[SCN]   * Scene center:        %s", getBounds().getCenter());
        UI.printInfo("[SCN]   * Scene diameter:      %.2f", getBounds().getExtents().length());
        if (sampler == null)
            return;
        if (!lightServer.build())
            return;
        // render
        UI.printInfo("[SCN] Rendering ...");
        sampler.prepare(this, imageWidth, imageHeight);
        sampler.render(display);
        UI.printInfo("[SCN] Done.");
    }

    public boolean calculatePhotons(PhotonStore map, String type, int seed) {
        return lightServer.calculatePhotons(map, type, seed);
    }
}