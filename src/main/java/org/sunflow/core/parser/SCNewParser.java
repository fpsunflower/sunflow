package org.sunflow.core.parser;

import org.sunflow.PluginRegistry;
import org.sunflow.SunflowAPI;
import org.sunflow.SunflowAPIInterface;
import org.sunflow.core.SceneParser;
import org.sunflow.core.parameter.*;
import org.sunflow.core.parameter.camera.*;
import org.sunflow.core.parameter.geometry.*;
import org.sunflow.core.parameter.gi.*;
import org.sunflow.core.parameter.light.*;
import org.sunflow.core.parameter.modifier.BumpMapModifierParameter;
import org.sunflow.core.parameter.modifier.NormalMapModifierParameter;
import org.sunflow.core.parameter.modifier.PerlinModifierParameter;
import org.sunflow.core.parameter.shader.*;
import org.sunflow.image.Color;
import org.sunflow.image.ColorFactory;
import org.sunflow.image.ColorFactory.ColorSpecificationException;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.Parser;
import org.sunflow.system.Parser.ParserException;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;

/**
 * This class provides a static method for loading files in the Sunflow scene
 * file format.
 */
public class SCNewParser implements SceneParser {
    private static int instanceCounter = 0;
    private int instanceNumber;
    private Parser p;
    private int numLightSamples;
    // used to generate unique names inside this parser
    private HashMap<String, Integer> objectNames;

    public SCNewParser() {
        objectNames = new HashMap<String, Integer>();
        instanceCounter++;
        instanceNumber = instanceCounter;
    }

    private String generateUniqueName(String prefix) {
        // generate a unique name for this class:
        int index = 1;
        Integer value = objectNames.get(prefix);
        if (value != null) {
            index = value;
            objectNames.put(prefix, index + 1);
        } else {
            objectNames.put(prefix, index + 1);
        }
        return String.format("@sc_%d::%s_%d", instanceNumber, prefix, index);
    }

    public boolean parse(String filename, SunflowAPIInterface api) {
        String localDir = new File(filename).getAbsoluteFile().getParentFile().getAbsolutePath();
        numLightSamples = 1;
        Timer timer = new Timer();
        timer.start();
        UI.printInfo(Module.API, "Parsing \"%s\" ...", filename);
        try {
            p = new Parser(filename);
            while (true) {
                String token = p.getNextToken();
                if (token == null)
                    break;
                if (token.equals("image")) {
                    UI.printInfo(Module.API, "Reading image settings ...");
                    parseImageBlock(api);
                } else if (token.equals("background")) {
                    UI.printInfo(Module.API, "Reading background ...");
                    parseBackgroundBlock(api);
                } else if (token.equals("accel")) {
                    UI.printInfo(Module.API, "Reading accelerator type ...");
                    p.getNextToken();
                    UI.printWarning(Module.API, "Setting accelerator type is not recommended - ignoring");
                } else if (token.equals("filter")) {
                    // Deprecated
                    UI.printInfo(Module.API, "Reading image filter type ...");
                    parseFilter(api);
                } else if (token.equals("bucket")) {
                    UI.printInfo(Module.API, "Reading bucket settings ...");
                    parseBucketBlock(api);
                } else if (token.equals("photons")) {
                    UI.printInfo(Module.API, "Reading photon settings ...");
                    parsePhotonBlock(api);
                } else if (token.equals("gi")) {
                    UI.printInfo(Module.API, "Reading global illumination settings ...");
                    parseGIBlock(api);
                } else if (token.equals("lightserver")) {
                    // Deprecated
                    UI.printInfo(Module.API, "Reading light server settings ...");
                    parseLightserverBlock(api);
                } else if (token.equals("trace-depths")) {
                    UI.printInfo(Module.API, "Reading trace depths ...");
                    parseTraceBlock(api);
                } else if (token.equals("camera")) {
                    parseCamera(api);
                } else if (token.equals("shader")) {
                    if (!parseShader(api)) {
                        // Close before return
                        p.close();
                        return false;
                    }
                } else if (token.equals("modifier")) {
                    if (!parseModifier(api)) {
                        // Close before return
                        p.close();
                        return false;
                    }
                } else if (token.equals("override")) {
                    parseOverrideBlock(api);
                } else if (token.equals("object")) {
                    parseObjectBlock(api);
                } else if (token.equals("instance")) {
                    parseInstanceBlock(api);
                } else if (token.equals("light")) {
                    parseLightBlock(api);
                } else if (token.equals("texturepath")) {
                    String path = p.getNextToken();
                    if (!new File(path).isAbsolute()) {
                        path = localDir + File.separator + path;
                    }
                    api.searchpath("texture", path);
                } else if (token.equals("includepath")) {
                    String path = p.getNextToken();
                    if (!new File(path).isAbsolute()) {
                        path = localDir + File.separator + path;
                    }
                    api.searchpath("include", path);
                } else if (token.equals("include")) {
                    String file = p.getNextToken();
                    UI.printInfo(Module.API, "Including: \"%s\" ...", file);
                    api.include(file);
                } else
                    UI.printWarning(Module.API, "Unrecognized token %s", token);
            }
            p.close();
        } catch (ParserException e) {
            UI.printError(Module.API, "%s", e.getMessage());
            e.printStackTrace();
            return false;
        } catch (FileNotFoundException e) {
            UI.printError(Module.API, "%s", e.getMessage());
            return false;
        } catch (IOException e) {
            UI.printError(Module.API, "%s", e.getMessage());
            return false;
        } catch (ColorSpecificationException e) {
            UI.printError(Module.API, "%s", e.getMessage());
            return false;
        }
        timer.end();
        UI.printInfo(Module.API, "Done parsing.");
        UI.printInfo(Module.API, "Parsing time: %s", timer.toString());
        return true;
    }

    private void parseBucketBlock(SunflowAPIInterface api) throws IOException {
        BucketParameter bucket = new BucketParameter();
        bucket.setSize(p.getNextInt());
        bucket.setOrder(p.getNextToken());
        bucket.setup(api);
    }

    private void parseImageBlock(SunflowAPIInterface api) throws IOException, ParserException {
        ImageParameter image = new ImageParameter();

        p.checkNextToken("{");
        if (p.peekNextToken("resolution")) {
            image.setResolutionX(p.getNextInt());
            image.setResolutionY(p.getNextInt());
        }
        if (p.peekNextToken("sampler"))
            image.setSampler(p.getNextToken());
        if (p.peekNextToken("aa")) {
            image.setAAMin(p.getNextInt());
            image.setAAMax(p.getNextInt());
        }
        if (p.peekNextToken("samples")) {
            image.setAASamples(p.getNextInt());
        }
        if (p.peekNextToken("contrast")) {
            image.setAAContrast(p.getNextFloat());
        }
        if (p.peekNextToken("filter")) {
            image.setFilter(p.getNextToken());
        }
        if (p.peekNextToken("jitter")) {
            image.setAAJitter(p.getNextBoolean());
        }
        if (p.peekNextToken("show-aa")) {
            UI.printWarning(Module.API, "Deprecated: show-aa ignored");
            p.getNextBoolean();
        }
        if (p.peekNextToken("cache")) {
            image.setAACache(p.getNextBoolean());
        }
        if (p.peekNextToken("output")) {
            UI.printWarning(Module.API, "Deprecated: output statement ignored");
            p.getNextToken();
        }

        image.setup(api);
        p.checkNextToken("}");
    }

    private void parseBackgroundBlock(SunflowAPIInterface api) throws IOException, ParserException, ColorSpecificationException {
        BackgroundParameter background = new BackgroundParameter();

        p.checkNextToken("{");
        p.checkNextToken("color");

        background.setColor(parseColor());
        background.setup(api);

        p.checkNextToken("}");
    }

    private void parseFilter(SunflowAPIInterface api) throws IOException, ParserException {
        UI.printWarning(Module.API, "Deprecated keyword \"filter\" - set this option in the image block");
        String name = p.getNextToken();
        api.parameter("filter", name);
        api.options(SunflowAPI.DEFAULT_OPTIONS);
        boolean hasSizeParams = name.equals("box") || name.equals("gaussian") || name.equals("blackman-harris") || name.equals("sinc") || name.equals("triangle");
        if (hasSizeParams) {
            p.getNextFloat();
            p.getNextFloat();
        }
    }

    private void parsePhotonBlock(SunflowAPIInterface api) throws ParserException, IOException {
        PhotonParameter photon = new PhotonParameter();

        boolean globalEmit = false;

        p.checkNextToken("{");
        if (p.peekNextToken("emit")) {
            UI.printWarning(Module.API, "Shared photon emit values are deprecated - specify number of photons to emit per map");
            photon.setNumEmit(p.getNextInt());
            globalEmit = true;
        }
        if (p.peekNextToken("global")) {
            UI.printWarning(Module.API, "Global photon map setting belongs inside the gi block - ignoring");
            if (!globalEmit) {
                p.getNextInt();
            }
            p.getNextToken();
            p.getNextInt();
            p.getNextFloat();
        }
        p.checkNextToken("caustics");
        if (!globalEmit) {
            photon.setNumEmit(p.getNextInt());
        }

        photon.setCaustics(p.getNextToken());
        photon.setCausticsGather(p.getNextInt());
        photon.setCausticsRadius(p.getNextFloat());

        photon.setup(api);
        p.checkNextToken("}");
    }

    private void parseGIBlock(SunflowAPIInterface api) throws ParserException, IOException, ColorSpecificationException {
        p.checkNextToken("{");
        p.checkNextToken("type");
        if (p.peekNextToken("irr-cache")) {
            IrrCacheGIParameter gi = new IrrCacheGIParameter();

            p.checkNextToken("samples");
            gi.setSamples(p.getNextInt());
            p.checkNextToken("tolerance");
            gi.setTolerance(p.getNextFloat());
            p.checkNextToken("spacing");
            gi.setMinSpacing(p.getNextFloat());
            gi.setMaxSpacing(p.getNextFloat());

            // parse global photon map info
            if (p.peekNextToken("global")) {
                gi.setGlobal(new IlluminationParameter());
                gi.getGlobal().setEmit(p.getNextInt());
                gi.getGlobal().setMap(p.getNextToken());
                gi.getGlobal().setGather(p.getNextInt());
                gi.getGlobal().setRadius(p.getNextFloat());
            }
        } else if (p.peekNextToken("path")) {
            PathTracingGIParameter gi = new PathTracingGIParameter();

            p.checkNextToken("samples");
            gi.setSamples(p.getNextInt());

            if (p.peekNextToken("bounces")) {
                UI.printWarning(Module.API, "Deprecated setting: bounces - use diffuse trace depth instead");
                p.getNextInt();
            }

            gi.setup(api);
        } else if (p.peekNextToken("fake")) {
            FakeGIParameter gi = new FakeGIParameter();

            p.checkNextToken("up");
            gi.setUp(parseVector());
            p.checkNextToken("sky");
            gi.setSky(parseColor());
            p.checkNextToken("ground");
            gi.setGround(parseColor());

            gi.setup(api);
        } else if (p.peekNextToken("igi")) {
            InstantGIParameter gi = new InstantGIParameter();

            p.checkNextToken("samples");
            gi.setSamples(p.getNextInt());
            p.checkNextToken("sets");
            gi.setSets(p.getNextInt());

            if (!p.peekNextToken("b")) {
                p.checkNextToken("c");
            }
            gi.setBias(p.getNextFloat());

            p.checkNextToken("bias-samples");
            gi.setBiasSamples(p.getNextInt());

            gi.setup(api);
        } else if (p.peekNextToken("ambocc")) {
            AmbientOcclusionGIParameter gi = new AmbientOcclusionGIParameter();

            p.checkNextToken("bright");
            gi.setBright(parseColor());
            p.checkNextToken("dark");
            gi.setDark(parseColor());
            p.checkNextToken("samples");
            gi.setSamples(p.getNextInt());
            if (p.peekNextToken("maxdist")) {
                gi.setMaxDist(p.getNextFloat());
            }

            gi.setup(api);
        } else if (p.peekNextToken("none") || p.peekNextToken("null")) {
            DisabledGIParameter gi = new DisabledGIParameter();
            // disable GI
            gi.setup(api);
        } else {
            UI.printWarning(Module.API, "Unrecognized gi engine type \"%s\" - ignoring", p.getNextToken());
        }
        api.options(SunflowAPI.DEFAULT_OPTIONS);
        p.checkNextToken("}");
    }

    private void parseOverrideBlock(SunflowAPIInterface api) throws IOException {
        OverrideParameter block = new OverrideParameter();

        block.setShader(p.getNextToken());
        block.setPhotons(p.getNextBoolean());
        block.setup(api);
    }

    private void parseLightserverBlock(SunflowAPIInterface api) throws ParserException, IOException {
        p.checkNextToken("{");
        if (p.peekNextToken("shadows")) {
            UI.printWarning(Module.API, "Deprecated: shadows setting ignored");
            p.getNextBoolean();
        }
        if (p.peekNextToken("direct-samples")) {
            UI.printWarning(Module.API, "Deprecated: use samples keyword in area light definitions");
            numLightSamples = p.getNextInt();
        }
        if (p.peekNextToken("glossy-samples")) {
            UI.printWarning(Module.API, "Deprecated: use samples keyword in glossy shader definitions");
            p.getNextInt();
        }
        if (p.peekNextToken("max-depth")) {
            UI.printWarning(Module.API, "Deprecated: max-depth setting - use trace-depths block instead");
            int d = p.getNextInt();
            api.parameter("depths.diffuse", 1);
            api.parameter("depths.reflection", d - 1);
            api.parameter("depths.refraction", 0);
            api.options(SunflowAPI.DEFAULT_OPTIONS);
        }
        if (p.peekNextToken("global")) {
            UI.printWarning(Module.API, "Deprecated: global settings ignored - use photons block instead");
            p.getNextBoolean();
            p.getNextInt();
            p.getNextInt();
            p.getNextInt();
            p.getNextFloat();
        }
        if (p.peekNextToken("caustics")) {
            UI.printWarning(Module.API, "Deprecated: caustics settings ignored - use photons block instead");
            p.getNextBoolean();
            p.getNextInt();
            p.getNextFloat();
            p.getNextInt();
            p.getNextFloat();
        }
        if (p.peekNextToken("irr-cache")) {
            UI.printWarning(Module.API, "Deprecated: irradiance cache settings ignored - use gi block instead");
            p.getNextInt();
            p.getNextFloat();
            p.getNextFloat();
            p.getNextFloat();
        }
        p.checkNextToken("}");
    }

    private void parseTraceBlock(SunflowAPIInterface api) throws ParserException, IOException {
        TraceDepthsParameter traceDepthsParameter = new TraceDepthsParameter();
        p.checkNextToken("{");
        if (p.peekNextToken("diff")) {
            traceDepthsParameter.setDiffuse(p.getNextInt());
        }
        if (p.peekNextToken("refl")) {
            traceDepthsParameter.setReflection(p.getNextInt());
        }
        if (p.peekNextToken("refr")) {
            traceDepthsParameter.setRefraction(p.getNextInt());
        }
        p.checkNextToken("}");

        traceDepthsParameter.setup(api);
        api.options(SunflowAPI.DEFAULT_OPTIONS);
    }

    private void parseCamera(SunflowAPIInterface api) throws ParserException, IOException {
        p.checkNextToken("{");
        p.checkNextToken("type");
        String type = p.getNextToken();
        UI.printInfo(Module.API, "Reading %s camera ...", type);

        float shutterOpen = 0, shutterClose = 0;

        if (p.peekNextToken("shutter")) {
            shutterOpen = p.getNextFloat();
            shutterClose = p.getNextFloat();
        }
        parseCameraTransform(api);
        String name = generateUniqueName("camera");
        if (type.equals(CameraParameter.TYPE_PINHOLE)) {
            PinholeCameraParameter camera = new PinholeCameraParameter();
            camera.setName(name);
            camera.setShutterOpen(shutterOpen);
            camera.setShutterClose(shutterClose);

            p.checkNextToken("fov");
            camera.setFov(p.getNextFloat());
            p.checkNextToken("aspect");
            camera.setAspect(p.getNextFloat());

            if (p.peekNextToken("shift")) {
                camera.setShiftX(p.getNextFloat());
                camera.setShiftY(p.getNextFloat());
            }
            camera.setup(api);
        } else if (type.equals(CameraParameter.TYPE_THINLENS)) {
            ThinLensCameraParameter camera = new ThinLensCameraParameter();
            camera.setName(name);
            camera.setShutterOpen(shutterOpen);
            camera.setShutterClose(shutterClose);

            p.checkNextToken("fov");
            camera.setFov(p.getNextFloat());
            p.checkNextToken("aspect");
            camera.setAspect(p.getNextFloat());
            if (p.peekNextToken("shift")) {
                camera.setShiftX(p.getNextFloat());
                camera.setShiftY(p.getNextFloat());
            }
            p.checkNextToken("fdist");
            camera.setFocusDistance(p.getNextFloat());
            p.checkNextToken("lensr");
            camera.setLensRadius(p.getNextFloat());
            if (p.peekNextToken("sides")) {
                camera.setLensSides(p.getNextInt());
            }
            if (p.peekNextToken("rotation")) {
                camera.setLensRotation(p.getNextFloat());
            }
            camera.setup(api);
        } else if (type.equals(CameraParameter.TYPE_SPHERICAL)) {
            SphericalCameraParameter camera = new SphericalCameraParameter();
            camera.setName(name);
            camera.setShutterOpen(shutterOpen);
            camera.setShutterClose(shutterClose);
            // no extra arguments
            camera.setup(api);
        } else if (type.equals(CameraParameter.TYPE_FISH_EYE)) {
            FishEyeCameraParameter camera = new FishEyeCameraParameter();
            camera.setName(name);
            camera.setShutterOpen(shutterOpen);
            camera.setShutterClose(shutterClose);
            // no extra arguments
            camera.setup(api);
        } else {
            UI.printWarning(Module.API, "Unrecognized camera type: %s", p.getNextToken());
            p.checkNextToken("}");
            return;
        }
        p.checkNextToken("}");
        /*if (name != null) {
            api.parameter("camera", name);
            api.options(SunflowAPI.DEFAULT_OPTIONS);
        }*/
    }

    private void parseCameraTransform(SunflowAPIInterface api) throws ParserException, IOException {
        if (p.peekNextToken("steps")) {
            // motion blur camera
            int n = p.getNextInt();
            api.parameter("transform.steps", n);
            // parse time extents
            p.checkNextToken("times");
            float t0 = p.getNextFloat();
            float t1 = p.getNextFloat();
            api.parameter("transform.times", "float", "none", new float[]{t0, t1});
            for (int i = 0; i < n; i++) {
                parseCameraMatrix(i, api);
            }
        } else {
            parseCameraMatrix(-1, api);
        }
    }

    private void parseCameraMatrix(int index, SunflowAPIInterface api) throws IOException, ParserException {
        String offset = index < 0 ? "" : String.format("[%d]", index);
        if (p.peekNextToken("transform")) {
            // advanced camera
            api.parameter(String.format("transform%s", offset), parseMatrix());
        } else {
            if (index >= 0) {
                p.checkNextToken("{");
            }
            // regular camera specification
            p.checkNextToken("eye");
            Point3 eye = parsePoint();
            p.checkNextToken("target");
            Point3 target = parsePoint();
            p.checkNextToken("up");
            Vector3 up = parseVector();

            // TODO Move logic to camera
            api.parameter(String.format("transform%s", offset), Matrix4.lookAt(eye, target, up));
            if (index >= 0) {
                p.checkNextToken("}");
            }
        }
    }

    private boolean parseShader(SunflowAPIInterface api) throws ParserException, IOException, ColorSpecificationException {
        p.checkNextToken("{");
        p.checkNextToken("name");
        String name = p.getNextToken();
        UI.printInfo(Module.API, "Reading shader: %s ...", name);
        p.checkNextToken("type");
        if (p.peekNextToken("diffuse")) {
            DiffuseShaderParameter shader = new DiffuseShaderParameter(name);

            if (p.peekNextToken("diff")) {
                shader.setDiffuse(parseColor());
            } else if (p.peekNextToken("texture")) {
                shader.setTexture(p.getNextToken());
            } else {
                UI.printWarning(Module.API, "Unrecognized option in diffuse shader block: %s", p.getNextToken());
            }
            shader.setup(api);
        } else if (p.peekNextToken("phong")) {
            PhongShaderParameter shaderParameter = new PhongShaderParameter(name);
            if (p.peekNextToken("texture")) {
                shaderParameter.setTexture(p.getNextToken());
            } else {
                p.checkNextToken("diff");
                shaderParameter.setDiffuse(parseColor());
            }
            p.checkNextToken("spec");
            shaderParameter.setSpecular(parseColor());
            shaderParameter.setPower(p.getNextFloat());

            if (p.peekNextToken("samples")) {
                shaderParameter.setSamples(p.getNextInt());
            }
            shaderParameter.setup(api);
        } else if (p.peekNextToken("amb-occ") || p.peekNextToken("amb-occ2")) {
            AmbientOcclusionShaderParameter shaderParameter = new AmbientOcclusionShaderParameter(name);

            if (p.peekNextToken("diff") || p.peekNextToken("bright")) {
                shaderParameter.setBright(parseColor());
            } else if (p.peekNextToken("texture")) {
                shaderParameter.setTexture(p.getNextToken());
            }

            if (p.peekNextToken("dark")) {
                shaderParameter.setDark(parseColor());
                p.checkNextToken("samples");
                shaderParameter.setSamples(p.getNextInt());
                p.checkNextToken("dist");
                shaderParameter.setMaxDist(p.getNextFloat());
            }
            shaderParameter.setup(api);
        } else if (p.peekNextToken("mirror")) {
            MirrorShaderParameter shaderParameter = new MirrorShaderParameter(name);
            p.checkNextToken("refl");
            shaderParameter.setReflection(parseColor());
            shaderParameter.setup(api);
        } else if (p.peekNextToken("glass")) {
            GlassShaderParameter shaderParameter = new GlassShaderParameter(name);

            p.checkNextToken("eta");
            shaderParameter.setEta(p.getNextFloat());
            p.checkNextToken("color");
            shaderParameter.setColor(parseColor());

            if (p.peekNextToken("absorption.distance") || p.peekNextToken("absorbtion.distance")) {
                shaderParameter.setAbsorptionDistance(p.getNextFloat());
            }
            if (p.peekNextToken("absorption.color") || p.peekNextToken("absorbtion.color")) {
                shaderParameter.setAbsorptionColor(parseColor());
            }
            shaderParameter.setup(api);
        } else if (p.peekNextToken("shiny")) {
            ShinyShaderParameter shaderParameter = new ShinyShaderParameter(name);

            if (p.peekNextToken("texture")) {
                shaderParameter.setTexture(p.getNextToken());
            } else {
                p.checkNextToken("diff");
                shaderParameter.setDiffuse(parseColor());
            }
            p.checkNextToken("refl");
            shaderParameter.setShininess(p.getNextFloat());
            shaderParameter.setup(api);
        } else if (p.peekNextToken("ward")) {
            WardShaderParameter shaderParameter = new WardShaderParameter(name);

            if (p.peekNextToken("texture")) {
                shaderParameter.setTexture(p.getNextToken());
            } else {
                p.checkNextToken("diff");
                shaderParameter.setDiffuse(parseColor());
            }
            p.checkNextToken("spec");
            shaderParameter.setSpecular(parseColor());

            p.checkNextToken("rough");
            shaderParameter.setRoughnessX(p.getNextFloat());
            shaderParameter.setRoughnessY(p.getNextFloat());

            if (p.peekNextToken("samples")) {
                shaderParameter.setSamples(p.getNextInt());
            }
            shaderParameter.setup(api);
        } else if (p.peekNextToken("view-caustics")) {
            ViewCausticsShaderParameter shaderParameter = new ViewCausticsShaderParameter(name);
            shaderParameter.setup(api);
        } else if (p.peekNextToken("view-irradiance")) {
            ViewIrradianceShaderParameter shaderParameter = new ViewIrradianceShaderParameter(name);
            shaderParameter.setup(api);
        } else if (p.peekNextToken("view-global")) {
            ViewGlobalShaderParameter shaderParameter = new ViewGlobalShaderParameter(name);
            shaderParameter.setup(api);
        } else if (p.peekNextToken("constant")) {
            ConstantShaderParameter shaderParameter = new ConstantShaderParameter(name);
            // backwards compatibility -- peek only
            p.peekNextToken("color");
            shaderParameter.setColor(parseColor());
            shaderParameter.setup(api);
        } else if (p.peekNextToken("janino")) {
            String typename = p.peekNextToken("typename") ? p.getNextToken() : PluginRegistry.shaderPlugins.generateUniqueName("janino_shader");
            if (!PluginRegistry.shaderPlugins.registerPlugin(typename, p.getNextCodeBlock()))
                return false;
            api.shader(name, typename);
        } else if (p.peekNextToken("id")) {
            IDShaderParameter shaderParameter = new IDShaderParameter(name);
            shaderParameter.setup(api);
        } else if (p.peekNextToken("uber")) {
            UberShaderParameter shaderParameter = new UberShaderParameter(name);

            if (p.peekNextToken("diff")) {
                shaderParameter.setDiffuse(parseColor());
            }
            if (p.peekNextToken("diff.texture")) {
                shaderParameter.setDiffuseTexture(p.getNextToken());
            }
            if (p.peekNextToken("diff.blend")) {
                shaderParameter.setDiffuseBlend(p.getNextFloat());
            }
            if (p.peekNextToken("refl") || p.peekNextToken("spec")) {
                shaderParameter.setSpecular(parseColor());
            }
            if (p.peekNextToken("texture")) {
                // deprecated
                UI.printWarning(Module.API, "Deprecated uber shader parameter \"texture\" - please use \"diffuse.texture\" and \"diffuse.blend\" instead");
                shaderParameter.setDiffuseTexture(p.getNextToken());
                shaderParameter.setDiffuseBlend(p.getNextFloat());
            }
            if (p.peekNextToken("spec.texture")) {
                shaderParameter.setSpecularTexture(p.getNextToken());
            }
            if (p.peekNextToken("spec.blend")) {
                shaderParameter.setSpecularBlend(p.getNextFloat());
            }
            if (p.peekNextToken("glossy")) {
                shaderParameter.setGlossyness(p.getNextFloat());
            }
            if (p.peekNextToken("samples")) {
                shaderParameter.setSamples(p.getNextInt());
            }
            shaderParameter.setup(api);
        } else {
            UI.printWarning(Module.API, "Unrecognized shader type: %s", p.getNextToken());
        }
        p.checkNextToken("}");
        return true;
    }

    private boolean parseModifier(SunflowAPIInterface api) throws ParserException, IOException {
        p.checkNextToken("{");
        p.checkNextToken("name");
        String name = p.getNextToken();
        UI.printInfo(Module.API, "Reading modifier: %s ...", name);
        p.checkNextToken("type");
        if (p.peekNextToken("bump")) {
            BumpMapModifierParameter parameter = new BumpMapModifierParameter();
            parameter.setName(name);
            p.checkNextToken("texture");
            parameter.setTexture(p.getNextToken());
            p.checkNextToken("scale");
            parameter.setScale(p.getNextFloat());
            parameter.setup(api);
        } else if (p.peekNextToken("normalmap")) {
            NormalMapModifierParameter parameter = new NormalMapModifierParameter();
            parameter.setName(name);
            p.checkNextToken("texture");
            parameter.setTexture(p.getNextToken());
            parameter.setup(api);
        } else if (p.peekNextToken("perlin")) {
            PerlinModifierParameter parameter = new PerlinModifierParameter();
            p.checkNextToken("function");
            parameter.setFunction(p.getNextInt());
            p.checkNextToken("size");
            parameter.setSize(p.getNextFloat());
            p.checkNextToken("scale");
            parameter.setScale(p.getNextFloat());
            parameter.setup(api);
        } else {
            UI.printWarning(Module.API, "Unrecognized modifier type: %s", p.getNextToken());
        }
        p.checkNextToken("}");
        return true;
    }

    private void parseObjectBlock(SunflowAPIInterface api) throws ParserException, IOException {
        p.checkNextToken("{");
        InstanceParameter instanceParameter = new InstanceParameter();

        String name = "";
        String accel = "";

        boolean noInstance = false;
        //Matrix4[] transform = null;
        //float transformTime0 = 0, transformTime1 = 0;
        String[] shaders = null;
        String[] modifiers = null;
        if (p.peekNextToken("noinstance")) {
            // this indicates that the geometry is to be created, but not
            // instanced into the scene
            noInstance = true;
        } else {
            // these are the parameters to be passed to the instance
            if (p.peekNextToken("shaders")) {
                int n = p.getNextInt();
                shaders = new String[n];
                for (int i = 0; i < n; i++)
                    shaders[i] = p.getNextToken();
            } else {
                p.checkNextToken("shader");
                shaders = new String[]{p.getNextToken()};
            }
            instanceParameter.shaders(shaders);

            if (p.peekNextToken("modifiers")) {
                int n = p.getNextInt();
                modifiers = new String[n];
                for (int i = 0; i < n; i++)
                    modifiers[i] = p.getNextToken();
            } else if (p.peekNextToken("modifier")) {
                modifiers = new String[]{p.getNextToken()};
            }
            instanceParameter.modifiers(modifiers);

            // Can be null
            TransformParameter transform = checkParseTransform();
            instanceParameter.transform(transform);
        }
        if (p.peekNextToken("accel")) {
            accel = p.getNextToken();
        }
        p.checkNextToken("type");
        String type = p.getNextToken();
        if (p.peekNextToken("name")) {
            name = p.getNextToken();
        } else {
            name = generateUniqueName(type);
        }

        if (type.equals("mesh")) {
            UI.printWarning(Module.API, "Deprecated object type: mesh");
            UI.printInfo(Module.API, "Reading mesh: %s ...", name);

            TriangleMeshParameter geometry = new TriangleMeshParameter();
            geometry.setName(name);
            geometry.setAccel(accel);
            geometry.setInstanceParameter(instanceParameter);

            int numVertices = p.getNextInt();
            int numTriangles = p.getNextInt();
            float[] points = new float[numVertices * 3];
            float[] normals = new float[numVertices * 3];
            float[] uvs = new float[numVertices * 2];
            for (int i = 0; i < numVertices; i++) {
                p.checkNextToken("v");
                points[3 * i + 0] = p.getNextFloat();
                points[3 * i + 1] = p.getNextFloat();
                points[3 * i + 2] = p.getNextFloat();
                normals[3 * i + 0] = p.getNextFloat();
                normals[3 * i + 1] = p.getNextFloat();
                normals[3 * i + 2] = p.getNextFloat();
                uvs[2 * i + 0] = p.getNextFloat();
                uvs[2 * i + 1] = p.getNextFloat();
            }
            int[] triangles = new int[numTriangles * 3];
            for (int i = 0; i < numTriangles; i++) {
                p.checkNextToken("t");
                triangles[i * 3 + 0] = p.getNextInt();
                triangles[i * 3 + 1] = p.getNextInt();
                triangles[i * 3 + 2] = p.getNextInt();
            }

            geometry.setPoints(points);
            geometry.setNormals(normals);
            geometry.setUvs(uvs);
            geometry.setTriangles(triangles);
            geometry.setup(api);
        } else if (type.equals("flat-mesh")) {
            UI.printWarning(Module.API, "Deprecated object type: flat-mesh");
            UI.printInfo(Module.API, "Reading flat mesh: %s ...", name);

            TriangleMeshParameter geometry = new TriangleMeshParameter();
            geometry.setName(name);
            geometry.setAccel(accel);
            geometry.setInstanceParameter(instanceParameter);

            int numVertices = p.getNextInt();
            int numTriangles = p.getNextInt();
            float[] points = new float[numVertices * 3];
            float[] uvs = new float[numVertices * 2];
            for (int i = 0; i < numVertices; i++) {
                p.checkNextToken("v");
                points[3 * i + 0] = p.getNextFloat();
                points[3 * i + 1] = p.getNextFloat();
                points[3 * i + 2] = p.getNextFloat();
                p.getNextFloat();
                p.getNextFloat();
                p.getNextFloat();
                uvs[2 * i + 0] = p.getNextFloat();
                uvs[2 * i + 1] = p.getNextFloat();
            }
            int[] triangles = new int[numTriangles * 3];
            for (int i = 0; i < numTriangles; i++) {
                p.checkNextToken("t");
                triangles[i * 3 + 0] = p.getNextInt();
                triangles[i * 3 + 1] = p.getNextInt();
                triangles[i * 3 + 2] = p.getNextInt();
            }

            geometry.setPoints(points);
            geometry.setUvs(uvs);
            geometry.setTriangles(triangles);
            geometry.setup(api);
        } else if (type.equals("sphere")) {
            UI.printInfo(Module.API, "Reading sphere ...");
            instanceParameter
                    .name(name + ".instance")
                    .geometry(name);

            SphereParameter geometry = new SphereParameter();
            geometry.setName(name);
            geometry.setInstanceParameter(instanceParameter);

            if (instanceParameter.transform() == null && !noInstance) {
                // legacy method of specifying transformation for spheres
                p.checkNextToken("c");
                float x = p.getNextFloat();
                float y = p.getNextFloat();
                float z = p.getNextFloat();
                geometry.setCenter(new Point3(x, y, z));
                p.checkNextToken("r");
                float radius = p.getNextFloat();
                geometry.setRadius(radius);

                geometry.setup(api);
                // disable future instancing - instance has already been created
                noInstance = true;
            }
        } else if (type.equals("cylinder")) {
            UI.printInfo(Module.API, "Reading cylinder ...");

            CylinderParameter parameter = new CylinderParameter();
            parameter.setName(name);
            parameter.setInstanceParameter(instanceParameter);

            parameter.setup(api);
        } else if (type.equals("banchoff")) {
            UI.printInfo(Module.API, "Reading banchoff ...");

            BanchOffParameter parameter = new BanchOffParameter();
            parameter.setName(name);
            parameter.setInstanceParameter(instanceParameter);

            parameter.setup(api);
        } else if (type.equals("torus")) {
            UI.printInfo(Module.API, "Reading torus ...");

            TorusParameter geometry = new TorusParameter();
            geometry.setName(name);
            geometry.setInstanceParameter(instanceParameter);

            p.checkNextToken("r");
            geometry.setRadiusInner(p.getNextFloat());
            geometry.setRadiusOuter(p.getNextFloat());
            geometry.setup(api);
        } else if (type.equals("sphereflake")) {
            UI.printInfo(Module.API, "Reading sphereflake ...");
            SphereFlakeParameter geometry = new SphereFlakeParameter();
            geometry.setName(name);

            if (p.peekNextToken("level")) {
                geometry.setLevel(p.getNextInt());
            }
            if (p.peekNextToken("axis")) {
                geometry.setAxis(parseVector());
            }
            if (p.peekNextToken("radius")) {
                geometry.setRadius(p.getNextFloat());
            }
            geometry.setup(api);
        } else if (type.equals("plane")) {
            UI.printInfo(Module.API, "Reading plane ...");
            PlaneParameter geometry = new PlaneParameter();
            geometry.setName(name);
            p.checkNextToken("p");
            geometry.setCenter(parsePoint());
            if (p.peekNextToken("n")) {
                geometry.setNormal(parseVector());
            } else {
                p.checkNextToken("p");
                geometry.setPoint1(parsePoint());
                p.checkNextToken("p");
                geometry.setPoint2(parsePoint());
            }
            geometry.setup(api);
        } else if (type.equals("generic-mesh")) {
            UI.printInfo(Module.API, "Reading generic mesh: %s ... ", name);
            GenericMeshParameter geometry = new GenericMeshParameter();
            // parse vertices
            p.checkNextToken("points");
            int np = p.getNextInt();
            float[] points = parseFloatArray(np * 3);
            geometry.setPoints(points);
            // parse triangle indices
            p.checkNextToken("triangles");
            int nt = p.getNextInt();
            geometry.setTriangles(parseIntArray(nt * 3));
            // parse normals
            p.checkNextToken("normals");
            if (p.peekNextToken("vertex")) {
                geometry.setNormals(parseFloatArray(np * 3));
            } else if (p.peekNextToken("facevarying")) {
                geometry.setFaceVaryingNormals(true);
                geometry.setNormals(parseFloatArray(nt * 9));
            } else {
                p.checkNextToken("none");
            }

            // parse texture coordinates
            p.checkNextToken("uvs");
            if (p.peekNextToken("vertex")) {
                geometry.setUvs(parseFloatArray(np * 2));
            } else if (p.peekNextToken("facevarying")) {
                geometry.setFaceVaryingTextures(true);
                geometry.setUvs(parseFloatArray(nt * 6));
            } else {
                p.checkNextToken("none");
            }
            if (p.peekNextToken("face_shaders")) {
                geometry.setFaceShaders(parseIntArray(nt));
            }
            geometry.setup(api);
        } else if (type.equals("hair")) {
            UI.printInfo(Module.API, "Reading hair curves: %s ... ", name);
            HairParameter geometry = new HairParameter();
            p.checkNextToken("segments");
            geometry.setSegments(p.getNextInt());
            p.checkNextToken("width");
            geometry.setWidth(p.getNextFloat());
            p.checkNextToken("points");
            geometry.setPoints(parseFloatArray(p.getNextInt()));
            geometry.setup(api);
        } else if (type.equals("janino-tesselatable")) {
            UI.printInfo(Module.API, "Reading procedural primitive: %s ... ", name);
            String typename = p.peekNextToken("typename") ? p.getNextToken() : PluginRegistry.shaderPlugins.generateUniqueName("janino_shader");
            if (!PluginRegistry.tesselatablePlugins.registerPlugin(typename, p.getNextCodeBlock())) {
                noInstance = true;
            } else {
                api.geometry(name, typename);
            }
        } else if (type.equals("teapot")) {
            UI.printInfo(Module.API, "Reading teapot: %s ... ", name);
            TeapotParameter geometry = new TeapotParameter();
            geometry.setName(name);

            if (p.peekNextToken("subdivs")) {
                geometry.setSubdivs(p.getNextInt());
            }
            if (p.peekNextToken("smooth")) {
                geometry.setSmooth(p.getNextBoolean());
            }
            geometry.setup(api);
        } else if (type.equals("gumbo")) {
            UI.printInfo(Module.API, "Reading gumbo: %s ... ", name);
            GumboParameter geometry = new GumboParameter();
            geometry.setName(name);

            if (p.peekNextToken("subdivs")) {
                geometry.setSubdivs(p.getNextInt());
            }
            if (p.peekNextToken("smooth")) {
                geometry.setSmooth(p.getNextBoolean());
            }
            geometry.setup(api);
        } else if (type.equals("julia")) {
            UI.printInfo(Module.API, "Reading julia fractal: %s ... ", name);
            JuliaParameter geometry = new JuliaParameter();
            geometry.setName(name);
            if (p.peekNextToken("q")) {
                geometry.setCw(p.getNextFloat());
                geometry.setCx(p.getNextFloat());
                geometry.setCy(p.getNextFloat());
                geometry.setCz(p.getNextFloat());
            }
            if (p.peekNextToken("iterations")) {
                geometry.setIterations(p.getNextInt());
            }
            if (p.peekNextToken("epsilon")) {
                geometry.setEpsilon(p.getNextFloat());
            }
            geometry.setup(api);
        } else if (type.equals("particles") || type.equals("dlasurface")) {
            ParticlesParameter geometry = new ParticlesParameter();
            geometry.setName(name);
            if (type.equals("dlasurface")) {
                UI.printWarning(Module.API, "Deprecated object type: \"dlasurface\" - please use \"particles\" instead");
            }
            float[] data;
            if (p.peekNextToken("filename")) {
                // FIXME: this code should be moved into an on demand loading
                // primitive
                String filename = p.getNextToken();
                boolean littleEndian = false;
                if (p.peekNextToken("little_endian")) {
                    littleEndian = true;
                }
                UI.printInfo(Module.USER, "Loading particle file: %s", filename);
                File file = new File(filename);
                FileInputStream stream = new FileInputStream(filename);
                MappedByteBuffer map = stream.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
                if (littleEndian)
                    map.order(ByteOrder.LITTLE_ENDIAN);
                FloatBuffer buffer = map.asFloatBuffer();
                data = new float[buffer.capacity()];
                for (int i = 0; i < data.length; i++) {
                    data[i] = buffer.get(i);
                }
                stream.close();
            } else {
                p.checkNextToken("points");
                int n = p.getNextInt();
                data = parseFloatArray(n * 3); // read 3n points
            }

            geometry.setPoints(data);

            if (p.peekNextToken("num")) {
                geometry.setNum(p.getNextInt());
            } else {
                geometry.setNum(data.length / 3);
            }

            p.checkNextToken("radius");
            geometry.setRadius(p.getNextFloat());
            geometry.setup(api);
        } else if (type.equals("file-mesh")) {
            UI.printInfo(Module.API, "Reading file mesh: %s ... ", name);
            FileMeshParameter geometry = new FileMeshParameter();
            geometry.setName(name);

            p.checkNextToken("filename");
            geometry.setFilename(p.getNextToken());

            if (p.peekNextToken("smooth_normals")) {
                geometry.setSmoothNormals(p.getNextBoolean());
            }
            geometry.setup(api);
        } else if (type.equals("bezier-mesh")) {
            UI.printInfo(Module.API, "Reading bezier mesh: %s ... ", name);
            BezierMeshParameter geometry = new BezierMeshParameter();
            geometry.setName(name);

            p.checkNextToken("n");
            //int nu, nv;

            geometry.setNu(p.getNextInt());
            geometry.setNv(p.getNextInt());

            if (p.peekNextToken("wrap")) {
                geometry.setUwrap(p.getNextBoolean());
                geometry.setVwrap(p.getNextBoolean());
            }
            p.checkNextToken("points");
            float[] points = new float[3 * geometry.getNu() * geometry.getNv()];
            for (int i = 0; i < points.length; i++) {
                points[i] = p.getNextFloat();
            }

            geometry.setPoints(points);

            if (p.peekNextToken("subdivs")) {
                geometry.setSubdivs(p.getNextInt());
            }
            if (p.peekNextToken("smooth")) {
                geometry.setSmooth(p.getNextBoolean());
            }
            geometry.setup(api);
        } else {
            UI.printWarning(Module.API, "Unrecognized object type: %s", p.getNextToken());
            noInstance = true;
        }
        if (!noInstance) {
            instanceParameter
                    .name(name + ".instance")
                    .geometry(name);
            instanceParameter.setup(api);

            // create instance
            /*api.parameter("shaders", shaders);
            if (modifiers != null)
                api.parameter("modifiers", modifiers);
            if (transform != null && transform.length > 0) {
                if (transform.length == 1)
                    api.parameter("transform", transform[0]);
                else {
                    api.parameter("transform.steps", transform.length);
                    api.parameter("transform.times", "float", "none", new float[]{
                            transformTime0, transformTime1});
                    for (int i = 0; i < transform.length; i++)
                        api.parameter(String.format("transform[%d]", i), transform[i]);
                }
            }
            api.instance(name + ".instance", name);*/
        }
        p.checkNextToken("}");
    }

    private void parseInstanceBlock(SunflowAPIInterface api) throws ParserException, IOException {
        InstanceParameter parameter = new InstanceParameter();
        p.checkNextToken("{");
        p.checkNextToken("name");
        parameter.name(p.getNextToken());
        UI.printInfo(Module.API, "Reading instance: %s ...", parameter.name());
        p.checkNextToken("geometry");
        parameter.geometry(p.getNextToken());

        TransformParameter transformParameter = parseTransform();
        parameter.transform(transformParameter);

        String[] shaders;
        if (p.peekNextToken("shaders")) {
            int n = p.getNextInt();
            shaders = new String[n];
            for (int i = 0; i < n; i++) {
                shaders[i] = p.getNextToken();
            }
        } else {
            p.checkNextToken("shader");
            shaders = new String[]{p.getNextToken()};
        }
        parameter.shaders(shaders);

        String[] modifiers = null;
        if (p.peekNextToken("modifiers")) {
            int n = p.getNextInt();
            modifiers = new String[n];
            for (int i = 0; i < n; i++) {
                modifiers[i] = p.getNextToken();
            }
        } else if (p.peekNextToken("modifier")) {
            modifiers = new String[]{p.getNextToken()};
        }
        parameter.modifiers(modifiers);
        parameter.setup(api);

        p.checkNextToken("}");
    }

    private TransformParameter parseTransform() throws ParserException, IOException {
        TransformParameter transformParameter = new TransformParameter();
        p.checkNextToken("transform");
        if (p.peekNextToken("steps")) {
            int n = p.getNextInt();

            p.checkNextToken("times");
            float[] times = new float[n];
            for (int i = 0; i < n; i++) {
                times[i] = p.getNextFloat();
            }
            transformParameter.setTimes(times);

            Matrix4[] transforms = new Matrix4[n];
            for (int i = 0; i < n; i++) {
                transforms[i] = parseMatrix();
            }
        } else {
            Matrix4[] transforms = new Matrix4[]{parseMatrix()};
            transformParameter.setTransforms(transforms);
        }
        return transformParameter;
    }

    private TransformParameter checkParseTransform() throws ParserException, IOException {
        TransformParameter transformParameter = new TransformParameter();
        if(p.peekNextToken("transform")) {
            if (p.peekNextToken("steps")) {
                int n = p.getNextInt();

                p.checkNextToken("times");
                float[] times = new float[n];
                for (int i = 0; i < n; i++) {
                    times[i] = p.getNextFloat();
                }
                transformParameter.setTimes(times);

                Matrix4[] transforms = new Matrix4[n];
                for (int i = 0; i < n; i++) {
                    transforms[i] = parseMatrix();
                }
            } else {
                Matrix4[] transforms = new Matrix4[]{parseMatrix()};
                transformParameter.setTransforms(transforms);
            }
        } else {
            return null;
        }
        return transformParameter;
    }

    private void parseLightBlock(SunflowAPIInterface api) throws ParserException, IOException, ColorSpecificationException {
        p.checkNextToken("{");
        p.checkNextToken("type");
        if (p.peekNextToken("mesh")) {
            UI.printWarning(Module.API, "Deprecated light type: mesh");
            p.checkNextToken("name");
            String name = p.getNextToken();
            UI.printInfo(Module.API, "Reading light mesh: %s ...", name);
            p.checkNextToken("emit");
            api.parameter("radiance", null, parseColor().getRGB());
            int samples = numLightSamples;
            if (p.peekNextToken("samples"))
                samples = p.getNextInt();
            else
                UI.printWarning(Module.API, "Samples keyword not found - defaulting to %d", samples);
            api.parameter("samples", samples);
            int numVertices = p.getNextInt();
            int numTriangles = p.getNextInt();
            float[] points = new float[3 * numVertices];
            int[] triangles = new int[3 * numTriangles];
            for (int i = 0; i < numVertices; i++) {
                p.checkNextToken("v");
                points[3 * i + 0] = p.getNextFloat();
                points[3 * i + 1] = p.getNextFloat();
                points[3 * i + 2] = p.getNextFloat();
                // ignored
                p.getNextFloat();
                p.getNextFloat();
                p.getNextFloat();
                p.getNextFloat();
                p.getNextFloat();
            }
            for (int i = 0; i < numTriangles; i++) {
                p.checkNextToken("t");
                triangles[3 * i + 0] = p.getNextInt();
                triangles[3 * i + 1] = p.getNextInt();
                triangles[3 * i + 2] = p.getNextInt();
            }
            api.parameter("points", "point", "vertex", points);
            api.parameter("triangles", triangles);
            api.light(name, "triangle_mesh");
        } else if (p.peekNextToken("point")) {
            UI.printInfo(Module.API, "Reading point light ...");
            PointLightParameter light = new PointLightParameter();
            light.setName(generateUniqueName("pointlight"));

            Color color;
            if (p.peekNextToken("color")) {
                color = parseColor();
                p.checkNextToken("power");
                float power = p.getNextFloat();
                color.mul(power);
            } else {
                UI.printWarning(Module.API, "Deprecated color specification - please use color and power instead");
                p.checkNextToken("power");
                color = parseColor();
            }
            light.setColor(color);

            p.checkNextToken("p");
            light.setCenter(parsePoint());
            light.setup(api);
        } else if (p.peekNextToken("spherical")) {
            UI.printInfo(Module.API, "Reading spherical light ...");
            SphereLightParameter light = new SphereLightParameter();
            light.setName(generateUniqueName("spherelight"));
            p.checkNextToken("color");
            Color color = parseColor();
            p.checkNextToken("radiance");
            float power = p.getNextFloat();
            color.mul(power);
            light.setRadiance(color);
            p.checkNextToken("center");
            light.setCenter(parsePoint());
            p.checkNextToken("radius");
            light.setRadius(p.getNextFloat());
            p.checkNextToken("samples");
            light.setSamples(p.getNextInt());
            light.setup(api);
        } else if (p.peekNextToken("directional")) {
            UI.printInfo(Module.API, "Reading directional light ...");
            DirectionalLightParameter light = new DirectionalLightParameter();
            light.setName(generateUniqueName("dirlight"));
            p.checkNextToken("source");
            light.setSource(parsePoint());
            p.checkNextToken("target");
            light.setDirection(parsePoint());
            p.checkNextToken("radius");
            light.setRadius(p.getNextFloat());
            p.checkNextToken("emit");
            Color color = parseColor();
            if (p.peekNextToken("intensity")) {
                float power = p.getNextFloat();
                color.mul(power);
            } else {
                UI.printWarning(Module.API, "Deprecated color specification - please use emit and intensity instead");
            }
            light.setRadiance(color);
            light.setup(api);
        } else if (p.peekNextToken("ibl")) {
            UI.printInfo(Module.API, "Reading image based light ...");
            ImageBasedLightParameter light = new ImageBasedLightParameter();
            light.setName(generateUniqueName("ibl"));
            p.checkNextToken("image");
            light.setTexture(p.getNextToken());
            p.checkNextToken("center");
            light.setCenter(parseVector());
            p.checkNextToken("up");
            light.setUp(parseVector());
            p.checkNextToken("lock");
            light.setFixed(p.getNextBoolean());

            light.setSamples(numLightSamples);

            if (p.peekNextToken("samples")) {
                light.setSamples(p.getNextInt());
            } else {
                UI.printWarning(Module.API, "Samples keyword not found - defaulting to %d", numLightSamples);
            }

            if (p.peekNextToken("lowsamples")) {
                light.setLowSamples(p.getNextInt());
            }

            light.setup(api);
        } else if (p.peekNextToken("meshlight")) {
            p.checkNextToken("name");
            TriangleMeshLightParameter light = new TriangleMeshLightParameter();
            light.setName(p.getNextToken());

            UI.printInfo(Module.API, "Reading meshlight: %s ...", light.getName());
            p.checkNextToken("emit");
            Color color = parseColor();
            if (p.peekNextToken("radiance")) {
                float r = p.getNextFloat();
                color.mul(r);
            } else {
                UI.printWarning(Module.API, "Deprecated color specification - please use emit and radiance instead");
            }
            light.setRadiance(color);
            light.setSamples(numLightSamples);

            if (p.peekNextToken("samples")) {
                light.setSamples(p.getNextInt());
            } else {
                UI.printWarning(Module.API, "Samples keyword not found - defaulting to %d", light.getSamples());
            }

            // parse vertices
            p.checkNextToken("points");
            int np = p.getNextInt();
            float[] points = parseFloatArray(np * 3);
            light.setPoints(points);

            // parse triangle indices
            p.checkNextToken("triangles");
            int nt = p.getNextInt();
            int[] triangles = parseIntArray(nt * 3);
            light.setTriangles(triangles);
            light.setup(api);
        } else if (p.peekNextToken("sunsky")) {
            SunSkyLightParameter light = new SunSkyLightParameter();
            light.setName(generateUniqueName("sunsky"));
            p.checkNextToken("up");
            light.setUp(parseVector());
            p.checkNextToken("east");
            light.setEast(parseVector());
            p.checkNextToken("sundir");
            light.setSunDirection(parseVector());
            p.checkNextToken("turbidity");
            light.setTurbidity(p.getNextFloat());

            // TODO Is possible not to have samples in sun sky?
            // light.setSamples(numLightSamples);
            if (p.peekNextToken("samples")) {
                light.setSamples(p.getNextInt());
            }

            if (p.peekNextToken("ground.extendsky")) {
                light.setExtendSky(p.getNextBoolean());
            } else if (p.peekNextToken("ground.color")) {
                light.setGroundColor(parseColor());
            }
            light.setup(api);
        } else if (p.peekNextToken("cornellbox")) {
            UI.printInfo(Module.API, "Reading cornell box ...");
            CornellBoxLightParameter light = new CornellBoxLightParameter();
            light.setName(generateUniqueName("cornellbox"));

            p.checkNextToken("corner0");
            light.setMin(parsePoint());
            p.checkNextToken("corner1");
            light.setMax(parsePoint());
            p.checkNextToken("left");
            light.setLeft(parseColor());
            p.checkNextToken("right");
            light.setRight(parseColor());
            p.checkNextToken("top");
            light.setTop(parseColor());
            p.checkNextToken("bottom");
            light.setBottom(parseColor());
            p.checkNextToken("back");
            light.setBack(parseColor());
            p.checkNextToken("emit");
            light.setRadiance(parseColor());

            // TODO Is possible not to have samples in cornell box?
            // light.setSamples(numLightSamples);
            if (p.peekNextToken("samples")) {
                light.setSamples(p.getNextInt());
            }
            light.setup(api);
        } else {
            UI.printWarning(Module.API, "Unrecognized object type: %s", p.getNextToken());
        }
        p.checkNextToken("}");
    }

    private Color parseColor() throws IOException, ParserException, ColorSpecificationException {
        if (p.peekNextToken("{")) {
            String space = p.getNextToken();
            int req = ColorFactory.getRequiredDataValues(space);
            if (req == -2) {
                UI.printWarning(Module.API, "Unrecognized color space: %s", space);
                return null;
            } else if (req == -1) {
                // array required, parse how many values are required
                req = p.getNextInt();
            }
            Color c = ColorFactory.createColor(space, parseFloatArray(req));
            p.checkNextToken("}");
            return c;
        } else {
            float r = p.getNextFloat();
            float g = p.getNextFloat();
            float b = p.getNextFloat();
            return ColorFactory.createColor(null, r, g, b);
        }
    }

    private Point3 parsePoint() throws IOException {
        float x = p.getNextFloat();
        float y = p.getNextFloat();
        float z = p.getNextFloat();
        return new Point3(x, y, z);
    }

    private Vector3 parseVector() throws IOException {
        float x = p.getNextFloat();
        float y = p.getNextFloat();
        float z = p.getNextFloat();
        return new Vector3(x, y, z);
    }

    private int[] parseIntArray(int size) throws IOException {
        int[] data = new int[size];
        for (int i = 0; i < size; i++) {
            data[i] = p.getNextInt();
        }
        return data;
    }

    private float[] parseFloatArray(int size) throws IOException {
        float[] data = new float[size];
        for (int i = 0; i < size; i++) {
            data[i] = p.getNextFloat();
        }
        return data;
    }

    private Matrix4 parseMatrix() throws IOException, ParserException {
        if (p.peekNextToken("row")) {
            return new Matrix4(parseFloatArray(16), true);
        } else if (p.peekNextToken("col")) {
            return new Matrix4(parseFloatArray(16), false);
        } else {
            Matrix4 m = Matrix4.IDENTITY;
            p.checkNextToken("{");
            while (!p.peekNextToken("}")) {
                Matrix4 t = null;
                if (p.peekNextToken("translate")) {
                    float x = p.getNextFloat();
                    float y = p.getNextFloat();
                    float z = p.getNextFloat();
                    t = Matrix4.translation(x, y, z);
                } else if (p.peekNextToken("scaleu")) {
                    float s = p.getNextFloat();
                    t = Matrix4.scale(s);
                } else if (p.peekNextToken("scale")) {
                    float x = p.getNextFloat();
                    float y = p.getNextFloat();
                    float z = p.getNextFloat();
                    t = Matrix4.scale(x, y, z);
                } else if (p.peekNextToken("rotatex")) {
                    float angle = p.getNextFloat();
                    t = Matrix4.rotateX((float) Math.toRadians(angle));
                } else if (p.peekNextToken("rotatey")) {
                    float angle = p.getNextFloat();
                    t = Matrix4.rotateY((float) Math.toRadians(angle));
                } else if (p.peekNextToken("rotatez")) {
                    float angle = p.getNextFloat();
                    t = Matrix4.rotateZ((float) Math.toRadians(angle));
                } else if (p.peekNextToken("rotate")) {
                    float x = p.getNextFloat();
                    float y = p.getNextFloat();
                    float z = p.getNextFloat();
                    float angle = p.getNextFloat();
                    t = Matrix4.rotate(x, y, z, (float) Math.toRadians(angle));
                } else
                    UI.printWarning(Module.API, "Unrecognized transformation type: %s", p.getNextToken());
                if (t != null)
                    m = t.multiply(m);
            }
            return m;
        }
    }
}