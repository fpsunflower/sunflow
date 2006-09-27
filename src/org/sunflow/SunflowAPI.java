package org.sunflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

import org.codehaus.janino.ClassBodyEvaluator;
import org.codehaus.janino.CompileException;
import org.codehaus.janino.Scanner;
import org.codehaus.janino.Parser.ParseException;
import org.codehaus.janino.Scanner.ScanException;
import org.sunflow.core.AccelerationStructure;
import org.sunflow.core.BucketOrder;
import org.sunflow.core.Camera;
import org.sunflow.core.CausticPhotonMapInterface;
import org.sunflow.core.Display;
import org.sunflow.core.Filter;
import org.sunflow.core.GIEngine;
import org.sunflow.core.Geometry;
import org.sunflow.core.ImageSampler;
import org.sunflow.core.Instance;
import org.sunflow.core.LightSource;
import org.sunflow.core.Scene;
import org.sunflow.core.SceneParser;
import org.sunflow.core.Shader;
import org.sunflow.core.accel.BoundingIntervalHierarchy;
import org.sunflow.core.accel.KDTree;
import org.sunflow.core.accel.NullAccelerator;
import org.sunflow.core.accel.UniformGrid;
import org.sunflow.core.bucket.ColumnBucketOrder;
import org.sunflow.core.bucket.DiagonalBucketOrder;
import org.sunflow.core.bucket.HilbertBucketOrder;
import org.sunflow.core.bucket.RowBucketOrder;
import org.sunflow.core.bucket.SpiralBucketOrder;
import org.sunflow.core.filter.BlackmanHarrisFilter;
import org.sunflow.core.filter.BoxFilter;
import org.sunflow.core.filter.CatmullRomFilter;
import org.sunflow.core.filter.GaussianFilter;
import org.sunflow.core.filter.LanczosFilter;
import org.sunflow.core.filter.MitchellFilter;
import org.sunflow.core.filter.SincFilter;
import org.sunflow.core.filter.TriangleFilter;
import org.sunflow.core.light.PointLight;
import org.sunflow.core.parser.RA2Parser;
import org.sunflow.core.parser.RA3Parser;
import org.sunflow.core.parser.SCParser;
import org.sunflow.core.parser.TriParser;
import org.sunflow.core.primitive.Sphere;
import org.sunflow.core.renderer.BucketRenderer;
import org.sunflow.core.renderer.ProgressiveRenderer;
import org.sunflow.core.renderer.SimpleRenderer;
import org.sunflow.image.Color;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;
import org.sunflow.system.SearchPath;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;

/**
 * This API gives a simple interface for creating scenes procedurally. This is
 * the main entry point to Sunflow. To use this class, extend from it and
 * implement the build method which may execute arbitrary code to create a
 * scene.
 */
public class SunflowAPI {
    public static final String VERSION = "0.07.0";

    private Scene scene;
    private HashMap<String, Shader> shadersTable;
    private Shader currentShader;
    private BucketRenderer bucketRenderer;
    private ProgressiveRenderer progressiveRenderer;
    private SearchPath includeSearchPath;
    private SearchPath textureSearchPath;

    /**
     * The default constructor is only available to sub-classes.
     */
    protected SunflowAPI() {
        reset();
    }

    /**
     * Reset the state of the API completely. The scene, shader table and all
     * rendering options are set back to their default values.
     */
    public final void reset() {
        scene = new Scene();
        bucketRenderer = new BucketRenderer();
        progressiveRenderer = new ProgressiveRenderer();
        shadersTable = new HashMap<String, Shader>();
        currentShader = null;
        includeSearchPath = new SearchPath("include");
        textureSearchPath = new SearchPath("texture");
    }

    /**
     * Add the specified path to the list of directories which are searched
     * automatically to resolve scene filenames.
     * 
     * @param path
     */
    public final void addIncludeSearchPath(String path) {
        includeSearchPath.addSearchPath(path);
    }

    /**
     * Adds the specified path to the list of directories which are searched
     * automatically to resolve texture filenames.
     */
    public final void addTextureSearchPath(String path) {
        textureSearchPath.addSearchPath(path);
    }

    /**
     * Attempts to resolve the specified filename by checking it against the
     * texture search path.
     * 
     * @param filename filename
     * @return a path which matches the filename, or filename if no matches are
     *         found
     */
    public final String resolveTextureFilename(String filename) {
        return textureSearchPath.resolvePath(filename);
    }

    /**
     * Sets the number of threads to use for all multi-threadable processes.
     * 
     * @param threads Number of threads, 0 means autodect the number of
     *            available cpu cores.
     * @param lowPriority Create low priority threads, this improves the
     *            responsiveness of the GUI while slightly reducing performance
     *            if the machine has many background tasks.
     */
    public final void threads(int threads, boolean lowPriority) {
        scene.setThreads(threads, lowPriority);
    }

    /**
     * Sets the desired image resolution in pixels.
     * 
     * @param w width of the final image in pixels
     * @param h height of the final image in pixels
     */
    public final void resolution(int w, int h) {
        scene.setResolution(w, h);
    }

    /**
     * Sets the bucket size in pixels to be used by the bucket renderer.
     * 
     * @param size bucket size in pixels
     */
    public final void bucketSize(int size) {
        bucketRenderer.setBucketSize(size);
    }

    /**
     * Sets the bucket ordering scheme for the bucket renderer. This version
     * instantiates built-in bucket oreders by their short string name: "row",
     * "column", "diagonal", "spiral", "hilbert". Invalid strings are ignored.
     * 
     * @param order name of a built-in bucket order
     */
    public final void bucketOrder(String order) {
        if (order.equals("row"))
            bucketOrder(new RowBucketOrder());
        else if (order.equals("column"))
            bucketOrder(new ColumnBucketOrder());
        else if (order.equals("diagonal"))
            bucketOrder(new DiagonalBucketOrder());
        else if (order.equals("spiral"))
            bucketOrder(new SpiralBucketOrder());
        else if (order.equals("hilbert"))
            bucketOrder(new HilbertBucketOrder());
        else
            UI.printWarning("[API] Unrecognized bucket ordering: \"%s\"", order);
    }

    /**
     * Sets the bucket order object for the bucket renderer explicitly.
     * 
     * @param order bucket order object
     */
    public final void bucketOrder(BucketOrder order) {
        bucketRenderer.setBuckerOrder(order);
    }

    /**
     * Sets the anti-aliasing depths for the bucket renderer. This version of
     * the method sets the number of super samples (for depth-of-field and
     * motion blur) to 1.
     * 
     * @param minDepth minimum AA depth
     * @param maxDepth maximum AA depth
     */
    public final void antiAliasing(int minDepth, int maxDepth) {
        antiAliasing(minDepth, maxDepth, 1);
    }

    /**
     * Sets the anti-aliasing depths for the bucket renderer as well as the
     * number of super samples per (sub)pixel. These are used to refine
     * depth-of-field and motion blur independently of spatial anti-aliasing.
     * 
     * @param minDepth minimum AA depth
     * @param maxDepth maximum AA depth
     * @param superSample number of samples per (sub)pixel.
     */
    public final void antiAliasing(int minDepth, int maxDepth, int superSample) {
        bucketRenderer.setAA(minDepth, maxDepth, superSample);
    }

    /**
     * Sets the diagnostic mode which displays a greyscale image of
     * anti-aliasing quality. This is mainly meant as a debugging tool though it
     * may be usefull to diagnose long rendering times. Properly tuned AA depths
     * should result in a mostly grey image in which only the visually
     * significant edges are anti-aliased.
     * 
     * @param displayAA display AA density
     */
    public final void displayAA(boolean displayAA) {
        bucketRenderer.setDisplayAA(displayAA);
    }

    /**
     * Sets the image filter to be used. The filter is created with optimal
     * dimensions. Only built-in filters are supported by this method. Valid
     * names are: "box", "gaussian", "mitchell", "catmull-rom",
     * "blackman-harris", "sinc", "lanczos", "triangle". Invalid names are
     * ignored.
     * 
     * @param filter built-in filter name
     */
    public final void filter(String filter) {
        // create filter with optimal size
        if (filter.equals("box")) {
            filter(new BoxFilter(1));
        } else if (filter.equals("gaussian")) {
            filter(new GaussianFilter(2));
        } else if (filter.equals("mitchell")) {
            filter(new MitchellFilter());
        } else if (filter.equals("catmull-rom")) {
            filter(new CatmullRomFilter());
        } else if (filter.equals("blackman-harris")) {
            filter(new BlackmanHarrisFilter(4));
        } else if (filter.equals("sinc")) {
            filter(new SincFilter(4));
        } else if (filter.equals("lanczos")) {
            filter(new LanczosFilter());
        } else if (filter.equals("triangle")) {
            filter(new TriangleFilter(2));
        } else
            UI.printWarning("[API] Unrecognized filter type: \"%s\"", filter);
    }

    /**
     * Sets the image filter object directly.
     * 
     * @param filter
     */
    public final void filter(Filter filter) {
        scene.setFilter(filter);
    }

    /**
     * Set the ray-tracing depth per bounce type. Diffuse depth controls all
     * global illumination algorithms while reflection and refraction depths
     * control specular reflections.
     * 
     * @param diffuseDepth number of allowed diffuse bounces
     * @param reflectionDepth number of reflection levels
     * @param refractionDepth number of refraction levels
     */
    public final void traceDepth(int diffuseDepth, int reflectionDepth, int refractionDepth) {
        scene.setMaxDepth(diffuseDepth, reflectionDepth, refractionDepth);
    }

    /**
     * Sets the type of caustic photons to use. To disable caustics use:
     * <code>photons(null);</code>.
     * 
     * @param cmap Caustic photon object.
     */
    public final void photons(CausticPhotonMapInterface cmap) {
        scene.photons(cmap);
    }

    /**
     * Sets the global illumination engine to be used. To disable GI
     * computations use: <code>giEngine(null);</code>.
     * 
     * @param engine global illumination engine to use.
     */
    public final void giEngine(GIEngine engine) {
        scene.giEngine(engine);
    }

    /**
     * Sets the ray intersection acceleration method to one of the built-in
     * types. Valid names are: "uniformgrid", "null", "bvh", "kdtree",
     * "kdtree_old". Other names are ignored.
     * 
     * @param accel name of a built-in intersection accelerator.
     */
    public final void accel(String accel) {
        if (accel.equals("uniformgrid"))
            accel(new UniformGrid());
        else if (accel.equals("null"))
            accel(new NullAccelerator());
        else if (accel.equals("kdtree"))
            accel(new KDTree());
        else if (accel.equals("bih"))
            accel(new BoundingIntervalHierarchy());
        else
            UI.printWarning("[API] Unrecognized intersection accelerator: \"%s\"", accel);
    }

    /**
     * Sets the acceleration structure object directly.
     * 
     * @param accel intersetion accelerator to use for rendering
     */
    public final void accel(AccelerationStructure accel) {
        scene.setIntersectionAccelerator(accel);
    }

    /**
     * Defines a shader with a given name and makes it the currently "active"
     * shader. Shader names must be unique, attempts to redeclare an existing
     * shader are ignored.
     * 
     * @param name a unique name given to the shader
     * @param shader a shader object
     */
    public final void shader(String name, Shader shader) {
        if (!shadersTable.containsKey(name)) {
            shadersTable.put(name, shader);
            currentShader = shader;
        } else
            UI.printWarning("[API] Shader \"%s\" was already defined - ignoring", name);
    }

    /**
     * Retrieve a shader object by its name, or <code>null</code> if no shader
     * was found. This also sets the "active" shader.
     * 
     * @param name shader name
     * @return the shader object associated with that name
     */
    public final Shader shader(String name) {
        currentShader = shadersTable.get(name);
        return currentShader;
    }

    /**
     * Sets a global shader override to the specified shader name. If the shader
     * is not found, the overriding is disabled. The second parameter controls
     * whether the override applies to the photon tracing process.
     * 
     * @param name shader name
     * @param photonOverride apply override to photon tracing phase
     */
    public final void shaderOverride(String name, boolean photonOverride) {
        Shader shader = shadersTable.get(name);
        scene.setShaderOverride(shader, photonOverride);
    }

    /**
     * Created a sphere at the specified coordinates. The currently active
     * shader is used.
     * 
     * @param x x coordinate of the sphere center
     * @param y y coordinate of the sphere center
     * @param z z coordinate of the sphere center
     * @param radius sphere radius
     */
    public final void sphere(float x, float y, float z, float radius) {
        Sphere sphere = new Sphere();
        Geometry geo = new Geometry(sphere);
        Matrix4 transform = Matrix4.translation(x, y, z).multiply(Matrix4.scale(radius));
        instance(new Instance(new Shader[] { currentShader }, transform, geo));
    }

    /**
     * Create a sphere with the specified transform. The transform is applied to
     * a unit-radius sphere centered at the origin. The currently active shader
     * is used.
     * 
     * @param m object to world transformation matrix
     */
    public final void sphere(Matrix4 m) {
        Sphere sphere = new Sphere();
        Geometry geo = new Geometry(sphere);
        instance(new Instance(new Shader[] { currentShader }, m, geo));
    }

    /**
     * Adds the specified instance to the scene.
     * 
     * @param instance
     */
    public final void instance(Instance instance) {
        scene.addInstance(instance);
    }

    /**
     * Adds the specified light to the scene.
     * 
     * @param light light source object
     */
    public final void light(LightSource light) {
        scene.addLight(light);
    }

    /**
     * Creats a point light with specified power
     * 
     * @param x x coordinate of the point light
     * @param y y coordinate of the point light
     * @param z z coordinate of the point light
     * @param power light power
     */
    public final void pointLight(float x, float y, float z, Color power) {
        light(new PointLight(new Point3(x, y, z), power));
    }

    /**
     * Sets the current camera for the scene
     * 
     * @param cam camera object
     */
    public final void camera(Camera cam) {
        scene.addCamera(cam);
    }

    /**
     * Render the scene with the specified built-in image sampler to the
     * specified display. Valid sampler names are "bucket", "ipr",
     * "fast".Attempts to use an unknown sampler are ignored.
     * 
     * @param sampler built-in sampler name
     * @param display display object
     */
    public final void render(String sampler, Display display) {
        if (sampler == null || sampler.equals("none") || sampler.equals("null"))
            render((ImageSampler) null, display);
        else if (sampler.equals("bucket"))
            render(bucketRenderer, display);
        else if (sampler.equals("ipr"))
            render(progressiveRenderer, display);
        else if (sampler.equals("fast"))
            render(new SimpleRenderer(), display);
        else
            UI.printError("[API] Unknown sampler type: %s - aborting", sampler);
    }

    /**
     * Render using the specified image sampler object and the specified
     * display.
     * 
     * @param sampler image sampler
     * @param display display object
     */
    public final void render(ImageSampler sampler, Display display) {
        scene.render(sampler, display);
    }

    /**
     * Render using the bucket sampler to the specified display.
     * 
     * @param display display object
     */
    public final void render(Display display) {
        render(bucketRenderer, display);
    }

    /**
     * Render using the progressive sampler to the specified display.
     * 
     * @param display display object
     */
    public final void progressiveRender(Display display) {
        render(progressiveRenderer, display);
    }

    /**
     * Parse the specified filename. The include paths are searched first. The
     * contents of the file are simply added to the active scene. This allows to
     * break up a scene into parts, even across file formats. The appropriate
     * parser is chosen based on file extension.
     * 
     * @param filename filename to load
     * @return <code>true</code> upon sucess, <code>false</code> if an error
     *         occured.
     */
    public final boolean parse(String filename) {
        if (filename == null)
            return false;
        filename = includeSearchPath.resolvePath(filename);
        SceneParser parser = null;
        if (filename.endsWith(".sc"))
            parser = new SCParser();
        else if (filename.endsWith(".ra2"))
            parser = new RA2Parser();
        else if (filename.endsWith(".ra3"))
            parser = new RA3Parser();
        else if (filename.endsWith(".tri"))
            parser = new TriParser();
        else
            return false;
        String currentFolder = new File(filename).getAbsoluteFile().getParentFile().getAbsolutePath();
        includeSearchPath.addSearchPath(currentFolder);
        textureSearchPath.addSearchPath(currentFolder);
        return parser.parse(filename, this);
    }

    /**
     * Retrieve the bounding box of the scene.
     */
    public final BoundingBox getBounds() {
        return scene.getBounds();
    }

    /**
     * This method does nothing, but may be overriden to create scenes
     * procedurally.
     */
    public void build() {
    }

    /**
     * Create an API object from the specified file. Java files are read by
     * Janino and are expected to implement a build method (they implement a
     * derived class of SunflowAPI. The build method is called if the code
     * compiles succesfully. Other files types are handled by the parse method.
     * 
     * @param filename filename to load
     * @return a valid SunflowAPI object or <code>null</code> on failure
     */
    public static SunflowAPI create(String filename) {
        if (filename == null)
            return new SunflowAPI();
        SunflowAPI api = null;
        if (filename.endsWith(".java")) {
            Timer t = new Timer();
            UI.printInfo("[API] Compiling \"" + filename + "\" ...");
            t.start();
            try {
                FileInputStream stream = new FileInputStream(filename);
                api = (SunflowAPI) ClassBodyEvaluator.createFastClassBodyEvaluator(new Scanner(filename, stream), SunflowAPI.class, ClassLoader.getSystemClassLoader());
                stream.close();
            } catch (CompileException e) {
                UI.printError("[API] Could not compile: \"%s\"", filename);
                UI.printError("[API] %s", e.getMessage());
                return null;
            } catch (ParseException e) {
                UI.printError("[API] Could not compile: \"%s\"", filename);
                UI.printError("[API] %s", e.getMessage());
                return null;
            } catch (ScanException e) {
                UI.printError("[API] Could not compile: \"%s\"", filename);
                UI.printError("[API] %s", e.getMessage());
                return null;
            } catch (IOException e) {
                UI.printError("[API] Could not compile: \"%s\"", filename);
                UI.printError("[API] %s", e.getMessage());
                return null;
            }
            t.end();
            UI.printInfo("[API] Compile time: " + t.toString());
            UI.printInfo("[API] Build script running ...");
            t.start();
            api.build();
            t.end();
            UI.printInfo("[API] Build script time: %s", t.toString());
        } else {
            api = new SunflowAPI();
            api = api.parse(filename) ? api : null;
        }
        return api;
    }

    /**
     * Compile the specified code string via Janino. The code must implement a
     * build method as described above. The build method is not called on the
     * output, it is up the caller to do so.
     * 
     * @param code java code string
     * @return a valid SunflowAPI object upon succes, <code>null</code>
     *         otherwise.
     */
    public static SunflowAPI compile(String code) {
        try {
            Timer t = new Timer();
            t.start();
            SunflowAPI api = (SunflowAPI) ClassBodyEvaluator.createFastClassBodyEvaluator(new Scanner(null, new StringReader(code)), SunflowAPI.class, (ClassLoader) null);
            t.end();
            UI.printInfo("[API] Compile time: %s", t.toString());
            return api;
        } catch (CompileException e) {
            UI.printError("[API] %s", e.getMessage());
            return null;
        } catch (ParseException e) {
            UI.printError("[API] %s", e.getMessage());
            return null;
        } catch (ScanException e) {
            UI.printError("[API] %s", e.getMessage());
            return null;
        } catch (IOException e) {
            UI.printError("[API] %s", e.getMessage());
            return null;
        }
    }
}