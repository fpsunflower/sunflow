package org.sunflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;

import org.codehaus.janino.ClassBodyEvaluator;
import org.codehaus.janino.CompileException;
import org.codehaus.janino.Scanner;
import org.codehaus.janino.Parser.ParseException;
import org.codehaus.janino.Scanner.ScanException;
import org.sunflow.core.Camera;
import org.sunflow.core.CameraLens;
import org.sunflow.core.Display;
import org.sunflow.core.Geometry;
import org.sunflow.core.ImageSampler;
import org.sunflow.core.Instance;
import org.sunflow.core.LightSource;
import org.sunflow.core.Modifier;
import org.sunflow.core.Options;
import org.sunflow.core.ParameterList;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.RenderObject;
import org.sunflow.core.Scene;
import org.sunflow.core.SceneParser;
import org.sunflow.core.Shader;
import org.sunflow.core.Tesselatable;
import org.sunflow.core.ParameterList.InterpolationType;
import org.sunflow.core.parser.RA2Parser;
import org.sunflow.core.parser.RA3Parser;
import org.sunflow.core.parser.SCParser;
import org.sunflow.core.parser.ShaveRibParser;
import org.sunflow.core.parser.TriParser;
import org.sunflow.core.renderer.BucketRenderer;
import org.sunflow.core.renderer.ProgressiveRenderer;
import org.sunflow.core.renderer.SimpleRenderer;
import org.sunflow.image.Color;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.SearchPath;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

/**
 * This API gives a simple interface for creating scenes procedurally. This is
 * the main entry point to Sunflow. To use this class, extend from it and
 * implement the build method which may execute arbitrary code to create a
 * scene.
 */
public class SunflowAPI {
    public static final String VERSION = "0.07.2";
    public static final String DEFAULT_OPTIONS = "::options";

    private Scene scene;
    private BucketRenderer bucketRenderer;
    private ProgressiveRenderer progressiveRenderer;
    private SearchPath includeSearchPath;
    private SearchPath textureSearchPath;
    private ParameterList parameterList;
    private RenderObjectMap renderObjects;
    private int currentFrame;

    /**
     * This is a quick system test which verifies that the user has launched
     * Java properly.
     */
    public static void runSystemCheck() {
        final long RECOMMENDED_MAX_SIZE = 800;
        long maxMb = Runtime.getRuntime().maxMemory() / 1048576;
        if (maxMb < RECOMMENDED_MAX_SIZE)
            UI.printError(Module.API, "JVM available memory is below %d MB (found %d MB only).\nPlease make sure you launched the program with the -Xmx command line options.", RECOMMENDED_MAX_SIZE, maxMb);
        String compiler = System.getProperty("java.vm.name");
        if (compiler == null || !(compiler.contains("HotSpot") && compiler.contains("Server")))
            UI.printError(Module.API, "You do not appear to be running Sun's server JVM\nPerformance may suffer");
        UI.printDetailed(Module.API, "Java environment settings:");
        UI.printDetailed(Module.API, "  * Max memory available : %d MB", maxMb);
        UI.printDetailed(Module.API, "  * Virtual machine name : %s", compiler == null ? "<unknown" : compiler);
        UI.printDetailed(Module.API, "  * Operating system     : %s", System.getProperty("os.name"));
        UI.printDetailed(Module.API, "  * CPU architecture     : %s", System.getProperty("os.arch"));
    }

    /**
     * Creates an empty scene.
     */
    public SunflowAPI() {
        reset();
    }

    /**
     * Reset the state of the API completely. The object table is cleared, and
     * all search paths areset back to their default values.
     */
    public final void reset() {
        scene = new Scene();
        bucketRenderer = new BucketRenderer();
        progressiveRenderer = new ProgressiveRenderer();
        includeSearchPath = new SearchPath("include");
        textureSearchPath = new SearchPath("texture");
        parameterList = new ParameterList();
        renderObjects = new RenderObjectMap();
        currentFrame = 1;
    }

    /**
     * Returns a name currently not being used by any other object. The returned
     * name is of the form "prefix_n" where n is an integer starting at 1. Only
     * a simple linear search is performed, so this method should be used only
     * when there is no other way to guarentee uniqueness.
     * 
     * @param prefix name prefix
     * @return a unique name not used by any rendering object
     */
    public final String getUniqueName(String prefix) {
        // generate a unique name based on the given prefix
        int counter = 1;
        String name;
        do {
            name = String.format("%s_%d", prefix, counter);
            counter++;
        } while (renderObjects.has(name));
        return name;
    }

    /**
     * Declare a parameter with the specified name and value. This parameter
     * will be added to the currently active parameter list.
     * 
     * @param name parameter name
     * @param value parameter value
     */
    public final void parameter(String name, String value) {
        parameterList.addString(name, value);
    }

    /**
     * Declare a parameter with the specified name and value. This parameter
     * will be added to the currently active parameter list.
     * 
     * @param name parameter name
     * @param value parameter value
     */
    public final void parameter(String name, boolean value) {
        parameterList.addBoolean(name, value);
    }

    /**
     * Declare a parameter with the specified name and value. This parameter
     * will be added to the currently active parameter list.
     * 
     * @param name parameter name
     * @param value parameter value
     */
    public final void parameter(String name, int value) {
        parameterList.addInteger(name, value);
    }

    /**
     * Declare a parameter with the specified name and value. This parameter
     * will be added to the currently active parameter list.
     * 
     * @param name parameter name
     * @param value parameter value
     */
    public final void parameter(String name, float value) {
        parameterList.addFloat(name, value);
    }

    /**
     * Declare a parameter with the specified name and value. This parameter
     * will be added to the currently active parameter list.
     * 
     * @param name parameter name
     * @param value parameter value
     */
    public final void parameter(String name, Color value) {
        parameterList.addColor(name, value);
    }

    /**
     * Declare a parameter with the specified name and value. This parameter
     * will be added to the currently active parameter list.
     * 
     * @param name parameter name
     * @param value parameter value
     */
    public final void parameter(String name, Point3 value) {
        parameterList.addPoints(name, InterpolationType.NONE, new float[] {
                value.x, value.y, value.z });
    }

    /**
     * Declare a parameter with the specified name and value. This parameter
     * will be added to the currently active parameter list.
     * 
     * @param name parameter name
     * @param value parameter value
     */
    public final void parameter(String name, Vector3 value) {
        parameterList.addVectors(name, InterpolationType.NONE, new float[] {
                value.x, value.y, value.z });
    }

    /**
     * Declare a parameter with the specified name and value. This parameter
     * will be added to the currently active parameter list.
     * 
     * @param name parameter name
     * @param value parameter value
     */
    public final void parameter(String name, Matrix4 value) {
        parameterList.addMatrices(name, InterpolationType.NONE, value.asRowMajor());
    }

    /**
     * Declare a parameter with the specified name and value. This parameter
     * will be added to the currently active parameter list.
     * 
     * @param name parameter name
     * @param value parameter value
     */
    public final void parameter(String name, int[] value) {
        parameterList.addIntegerArray(name, value);
    }

    /**
     * Declare a parameter with the specified name and value. This parameter
     * will be added to the currently active parameter list.
     * 
     * @param name parameter name
     * @param value parameter value
     */
    public final void parameter(String name, String[] value) {
        parameterList.addStringArray(name, value);
    }

    /**
     * Declare a parameter with the specified name. The type may be one of the
     * follow: "float", "point", "vector", "texcoord", "matrix". The
     * interpolation determines how the parameter is to be interpreted over
     * surface (see {@link InterpolationType}). The data is specified in a
     * flattened float array.
     * 
     * @param name parameter name
     * @param type parameter data type
     * @param interpolation parameter interpolation mode
     * @param data raw floating point data
     */
    public final void parameter(String name, String type, String interpolation, float[] data) {
        InterpolationType interp;
        try {
            interp = InterpolationType.valueOf(interpolation.toUpperCase());
        } catch (IllegalArgumentException e) {
            UI.printError(Module.API, "Unknown interpolation type: %s -- ignoring parameter \"%s\"", interpolation, name);
            return;
        }
        if (type.equals("float"))
            parameterList.addFloats(name, interp, data);
        else if (type.equals("point"))
            parameterList.addPoints(name, interp, data);
        else if (type.equals("vector"))
            parameterList.addVectors(name, interp, data);
        else if (type.equals("texcoord"))
            parameterList.addTexCoords(name, interp, data);
        else if (type.equals("matrix"))
            parameterList.addMatrices(name, interp, data);
        else
            UI.printError(Module.API, "Unknown parameter type: %s -- ignoring parameter \"%s\"", type, name);
    }

    /**
     * Remove the specified render object. Note that this may cause the removal
     * of other objects which depended on it.
     * 
     * @param name name of the object to remove
     */
    public void remove(String name) {
        renderObjects.remove(name);
    }

    /**
     * Update the specfied object using the currently active parameter list. The
     * object is removed if the update fails to avoid leaving inconsistently set
     * objects in the list.
     * 
     * @param name name of the object to update
     * @return <code>true</code> if the update was succesfull, or
     *         <code>false</code> if the update failed
     */
    public boolean update(String name) {
        boolean success = renderObjects.update(name, parameterList, this);
        parameterList.clear(success);
        return success;
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
     * Attempts to resolve the specified filename by checking it against the
     * include search path.
     * 
     * @param filename filename
     * @return a path which matches the filename, or filename if no matches are
     *         found
     */
    public final String resolveIncludeFilename(String filename) {
        return includeSearchPath.resolvePath(filename);
    }

    /**
     * Defines a shader with a given name. If the shader object is
     * <code>null</code>, the shader with the given name will be updated (if
     * it exists).
     * 
     * @param name a unique name given to the shader
     * @param shader a shader object
     */
    public final void shader(String name, Shader shader) {
        if (shader != null) {
            // we are declaring a shader for the first time
            if (renderObjects.has(name)) {
                UI.printError(Module.API, "Unable to declare shader \"%s\", name is already in use", name);
                parameterList.clear(true);
                return;
            }
            renderObjects.put(name, shader);
        }
        // update existing shader (only if it is valid)
        if (lookupShader(name) != null)
            update(name);
        else {
            UI.printError(Module.API, "Unable to update shader \"%s\" - shader object was not found", name);
            parameterList.clear(true);
        }
    }

    /**
     * Defines a modifier with a given name. If the modifier object is
     * <code>null</code>, the modifier with the given name will be updated
     * (if it exists).
     * 
     * @param name a unique name given to the modifier
     * @param modifier a modifier object
     */
    public final void modifier(String name, Modifier modifier) {
        if (modifier != null) {
            // we are declaring a shader for the first time
            if (renderObjects.has(name)) {
                UI.printError(Module.API, "Unable to declare modifier \"%s\", name is already in use", name);
                parameterList.clear(true);
                return;
            }
            renderObjects.put(name, modifier);
        }
        // update existing shader (only if it is valid)
        if (lookupModifier(name) != null)
            update(name);
        else {
            UI.printError(Module.API, "Unable to update modifier \"%s\" - modifier object was not found", name);
            parameterList.clear(true);
        }
    }

    /**
     * Defines a geometry with a given name. The geometry is built from the
     * specified {@link PrimitiveList}. If the primitives object is
     * <code>null</code>, the geometry with the given name will be updated
     * (if it exists).
     * 
     * @param name a unique name given to the geometry
     * @param primitives primitives to create the geometry from
     */
    public final void geometry(String name, PrimitiveList primitives) {
        if (primitives != null) {
            // we are declaring a geometry for the first time
            if (renderObjects.has(name)) {
                UI.printError(Module.API, "Unable to declare geometry \"%s\", name is already in use", name);
                parameterList.clear(true);
                return;
            }
            renderObjects.put(name, primitives);
        }
        if (lookupGeometry(name) != null)
            update(name);
        else {
            UI.printError(Module.API, "Unable to update geometry \"%s\" - geometry object was not found", name);
            parameterList.clear(true);
        }
    }

    /**
     * Defines a geometry with a given name. The geometry is built from the
     * specified {@link Tesselatable}. If the object is <code>null</code>,
     * the geometry with the given name will be updated (if it exists).
     * 
     * @param name a unique name given to the geometry
     * @param tesselatable the tesselatable object to create the geometry from
     */
    public final void geometry(String name, Tesselatable tesselatable) {
        if (tesselatable != null) {
            // we are declaring a geometry for the first time
            if (renderObjects.has(name)) {
                UI.printError(Module.API, "Unable to declare geometry \"%s\", name is already in use", name);
                parameterList.clear(true);
                return;
            }
            renderObjects.put(name, tesselatable);
        }
        if (lookupGeometry(name) != null)
            update(name);
        else {
            UI.printError(Module.API, "Unable to update geometry \"%s\" - geometry object was not found", name);
            parameterList.clear(true);
        }
    }

    /**
     * Instance the specified geometry into the scene. If geoname is
     * <code>null</code>, the specified instance object will be updated (if
     * it exists). It is not possible to change the instancing relationship
     * after the instance has been created.
     * 
     * @param name instance name
     * @param geoname name of the geometry to instance
     */
    public final void instance(String name, String geoname) {
        if (geoname != null) {
            // we are declaring this instance for the first time
            if (renderObjects.has(name)) {
                UI.printError(Module.API, "Unable to declare instance \"%s\", name is already in use", name);
                parameterList.clear(true);
                return;
            }
            parameter("geometry", geoname);
            renderObjects.put(name, new Instance());
        }
        if (lookupInstance(name) != null)
            update(name);
        else {
            UI.printError(Module.API, "Unable to update instance \"%s\" - instance object was not found", name);
            parameterList.clear(true);
        }
    }

    /**
     * Adds the specified light to the scene.
     * 
     * @param light light source object
     */
    public final void light(String name, LightSource light) {
        if (light != null) {
            // we are declaring this light for the first time
            if (renderObjects.has(name)) {
                UI.printError(Module.API, "Unable to declare light \"%s\", name is already in use", name);
                parameterList.clear(true);
                return;
            }
            renderObjects.put(name, light);
        }
        if (lookupLight(name) != null)
            update(name);
        else {
            UI.printError(Module.API, "Unable to update instance \"%s\" - instance object was not found", name);
            parameterList.clear(true);
        }
    }

    /**
     * Defines a camera with a given name. The camera is built from the
     * specified {@link CameraLens}. If the lens object is <code>null</code>,
     * the camera with the given name will be updated (if it exists). It isn't
     * possible to change the lens of an existing camera.
     * 
     * @param name camera name
     * @param lens camera lens to use
     */
    public final void camera(String name, CameraLens lens) {
        if (lens != null) {
            // we are declaring this camera for the first time
            if (renderObjects.has(name)) {
                UI.printError(Module.API, "Unable to declare camera \"%s\", name is already in use", name);
                parameterList.clear(true);
                return;
            }
            renderObjects.put(name, new Camera(lens));
        }
        // update existing shader (only if it is valid)
        if (lookupCamera(name) != null)
            update(name);
        else {
            UI.printError(Module.API, "Unable to update camera \"%s\" - camera object was not found", name);
            parameterList.clear(true);
        }
    }

    /**
     * Defines an option object to hold the current parameters. If the object
     * already exists, the values will simply override previous ones.
     * 
     * @param name
     */
    public final void options(String name) {
        if (lookupOptions(name) == null) {
            if (renderObjects.has(name)) {
                UI.printError(Module.API, "Unable to declare options \"%s\", name is already in use", name);
                parameterList.clear(true);
                return;
            }
            renderObjects.put(name, new Options());
        }
        assert lookupOptions(name) != null;
        update(name);
    }

    /**
     * Retrieve a geometry object by its name, or <code>null</code> if no
     * geometry was found, or if the specified object is not a geometry.
     * 
     * @param name geometry name
     * @return the geometry object associated with that name
     */
    public final Geometry lookupGeometry(String name) {
        return renderObjects.lookupGeometry(name);
    }

    /**
     * Retrieve an instance object by its name, or <code>null</code> if no
     * instance was found, or if the specified object is not an instance.
     * 
     * @param name instance name
     * @return the instance object associated with that name
     */
    private final Instance lookupInstance(String name) {
        return renderObjects.lookupInstance(name);
    }

    /**
     * Retrieve a shader object by its name, or <code>null</code> if no shader
     * was found, or if the specified object is not a shader.
     * 
     * @param name camera name
     * @return the camera object associate with that name
     */
    private final Camera lookupCamera(String name) {
        return renderObjects.lookupCamera(name);
    }

    private final Options lookupOptions(String name) {
        return renderObjects.lookupOptions(name);
    }

    /**
     * Retrieve a shader object by its name, or <code>null</code> if no shader
     * was found, or if the specified object is not a shader.
     * 
     * @param name shader name
     * @return the shader object associated with that name
     */
    public final Shader lookupShader(String name) {
        return renderObjects.lookupShader(name);
    }

    /**
     * Retrieve a modifier object by its name, or <code>null</code> if no
     * modifier was found, or if the specified object is not a modifier.
     * 
     * @param name modifier name
     * @return the modifier object associated with that name
     */
    public final Modifier lookupModifier(String name) {
        return renderObjects.lookupModifier(name);
    }

    /**
     * Retrieve a light object by its name, or <code>null</code> if no shader
     * was found, or if the specified object is not a light.
     * 
     * @param name light name
     * @return the light object associated with that name
     */
    private final LightSource lookupLight(String name) {
        return renderObjects.lookupLight(name);
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
        scene.setShaderOverride(lookupShader(name), photonOverride);
    }

    /**
     * Render using the specified options and the specified display. If the
     * specified options do not exist - defaults will be used.
     * 
     * @param optionsName name of the {@link RenderObject} which contains the
     *            options
     * @param display display object
     */
    public final void render(String optionsName, Display display) {
        renderObjects.updateScene(scene);
        Options opt = lookupOptions(optionsName);
        if (opt == null)
            opt = new Options();
        scene.setCamera(lookupCamera(opt.getString("camera", null)));

        // baking
        String bakingInstanceName = opt.getString("baking.instance", null);
        if (bakingInstanceName != null) {
            Instance bakingInstance = lookupInstance(bakingInstanceName);
            if (bakingInstance == null) {
                UI.printError(Module.API, "Unable to bake instance \"%s\" - not found", bakingInstanceName);
                return;
            }
            scene.setBakingInstance(bakingInstance);
        } else
            scene.setBakingInstance(null);

        String samplerName = opt.getString("sampler", "bucket");
        ImageSampler sampler = null;
        if (samplerName.equals("none") || samplerName.equals("null"))
            sampler = null;
        else if (samplerName.equals("bucket"))
            sampler = bucketRenderer;
        else if (samplerName.equals("ipr"))
            sampler = progressiveRenderer;
        else if (samplerName.equals("fast"))
            sampler = new SimpleRenderer();
        else {
            UI.printError(Module.API, "Unknown sampler type: %s - aborting", samplerName);
            return;
        }
        scene.render(opt, sampler, display);
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
        else if (filename.endsWith(".rib"))
            parser = new ShaveRibParser();
        else {
            UI.printError(Module.API, "Unable to find a suitable parser for: \"%s\"", filename);
            return false;
        }
        String currentFolder = new File(filename).getAbsoluteFile().getParentFile().getAbsolutePath();
        includeSearchPath.addSearchPath(currentFolder);
        textureSearchPath.addSearchPath(currentFolder);
        return parser.parse(filename, this);
    }

    /**
     * Retrieve the bounding box of the scene. This method will be valid only
     * after a first call to {@link #render(String, Display)} has been made.
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
    public static SunflowAPI create(String filename, int frameNumber) {
        if (filename == null)
            return new SunflowAPI();
        SunflowAPI api = null;
        if (filename.endsWith(".java")) {
            Timer t = new Timer();
            UI.printInfo(Module.API, "Compiling \"" + filename + "\" ...");
            t.start();
            try {
                FileInputStream stream = new FileInputStream(filename);
                api = (SunflowAPI) ClassBodyEvaluator.createFastClassBodyEvaluator(new Scanner(filename, stream), SunflowAPI.class, ClassLoader.getSystemClassLoader());
                stream.close();
            } catch (CompileException e) {
                UI.printError(Module.API, "Could not compile: \"%s\"", filename);
                UI.printError(Module.API, "%s", e.getMessage());
                return null;
            } catch (ParseException e) {
                UI.printError(Module.API, "Could not compile: \"%s\"", filename);
                UI.printError(Module.API, "%s", e.getMessage());
                return null;
            } catch (ScanException e) {
                UI.printError(Module.API, "Could not compile: \"%s\"", filename);
                UI.printError(Module.API, "%s", e.getMessage());
                return null;
            } catch (IOException e) {
                UI.printError(Module.API, "Could not compile: \"%s\"", filename);
                UI.printError(Module.API, "%s", e.getMessage());
                return null;
            }
            t.end();
            UI.printInfo(Module.API, "Compile time: " + t.toString());
            if (api != null) {
                String currentFolder = new File(filename).getAbsoluteFile().getParentFile().getAbsolutePath();
                api.includeSearchPath.addSearchPath(currentFolder);
                api.textureSearchPath.addSearchPath(currentFolder);
            }
            UI.printInfo(Module.API, "Build script running ...");
            t.start();
            api.setCurrentFrame(frameNumber);
            api.build();
            t.end();
            UI.printInfo(Module.API, "Build script time: %s", t.toString());
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
            UI.printInfo(Module.API, "Compile time: %s", t.toString());
            return api;
        } catch (CompileException e) {
            UI.printError(Module.API, "%s", e.getMessage());
            return null;
        } catch (ParseException e) {
            UI.printError(Module.API, "%s", e.getMessage());
            return null;
        } catch (ScanException e) {
            UI.printError(Module.API, "%s", e.getMessage());
            return null;
        } catch (IOException e) {
            UI.printError(Module.API, "%s", e.getMessage());
            return null;
        }
    }

    /**
     * Read the value of the current frame. This value is intended only for
     * procedural animation creation. It is not used by the Sunflow core in
     * anyway. The default value is 1.
     * 
     * @return current frame number
     */
    public int getCurrentFrame() {
        return currentFrame;
    }

    /**
     * Set the value of the current frame. This value is intended only for
     * procedural animation creation. It is not used by the Sunflow core in
     * anyway. The default value is 1.
     * 
     * @param currentFrame current frame number
     */
    public void setCurrentFrame(int currentFrame) {
        this.currentFrame = currentFrame;
    }
}