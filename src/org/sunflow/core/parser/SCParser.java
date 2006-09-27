package org.sunflow.core.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;

import org.codehaus.janino.ClassBodyEvaluator;
import org.codehaus.janino.CompileException;
import org.codehaus.janino.Scanner;
import org.codehaus.janino.Parser.ParseException;
import org.codehaus.janino.Scanner.ScanException;
import org.sunflow.SunflowAPI;
import org.sunflow.core.CausticPhotonMapInterface;
import org.sunflow.core.Geometry;
import org.sunflow.core.GlobalPhotonMapInterface;
import org.sunflow.core.Instance;
import org.sunflow.core.SceneParser;
import org.sunflow.core.Shader;
import org.sunflow.core.camera.PinholeCamera;
import org.sunflow.core.camera.SphericalCamera;
import org.sunflow.core.camera.ThinLensCamera;
import org.sunflow.core.filter.BlackmanHarrisFilter;
import org.sunflow.core.filter.BoxFilter;
import org.sunflow.core.filter.GaussianFilter;
import org.sunflow.core.filter.SincFilter;
import org.sunflow.core.filter.TriangleFilter;
import org.sunflow.core.gi.AmbientOcclusionGIEngine;
import org.sunflow.core.gi.FakeGIEngine;
import org.sunflow.core.gi.InstantGI;
import org.sunflow.core.gi.IrradianceCacheGIEngine;
import org.sunflow.core.gi.PathTracingGIEngine;
import org.sunflow.core.light.DirectionalSpotlight;
import org.sunflow.core.light.ImageBasedLight;
import org.sunflow.core.light.MeshLight;
import org.sunflow.core.photonmap.CausticPhotonMap;
import org.sunflow.core.photonmap.GlobalPhotonMap;
import org.sunflow.core.photonmap.GridPhotonMap;
import org.sunflow.core.primitive.Background;
import org.sunflow.core.primitive.BanchoffSurface;
import org.sunflow.core.primitive.CornellBox;
import org.sunflow.core.primitive.Mesh;
import org.sunflow.core.primitive.Plane;
import org.sunflow.core.primitive.Torus;
import org.sunflow.core.shader.AmbientOcclusionShader;
import org.sunflow.core.shader.AnisotropicWardShader;
import org.sunflow.core.shader.ConstantShader;
import org.sunflow.core.shader.DiffuseShader;
import org.sunflow.core.shader.GlassShader;
import org.sunflow.core.shader.IDShader;
import org.sunflow.core.shader.MirrorShader;
import org.sunflow.core.shader.PhongShader;
import org.sunflow.core.shader.ShinyDiffuseShader;
import org.sunflow.core.shader.TexturedAmbientOcclusionShader;
import org.sunflow.core.shader.TexturedDiffuseShader;
import org.sunflow.core.shader.TexturedPhongShader;
import org.sunflow.core.shader.TexturedShinyDiffuseShader;
import org.sunflow.core.shader.TexturedWardShader;
import org.sunflow.core.shader.UberShader;
import org.sunflow.core.shader.ViewCausticsShader;
import org.sunflow.core.shader.ViewGlobalPhotonsShader;
import org.sunflow.core.shader.ViewIrradianceShader;
import org.sunflow.image.Color;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.Parser;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.system.Parser.ParserException;

/**
 * This class provides a static method for loading files in the Sunflow scene
 * file format.
 */
public class SCParser implements SceneParser {
    private Parser p;
    private int numLightSamples;
    private int numGlossySamples;

    public SCParser() {
    }

    public boolean parse(String filename, SunflowAPI api) {
        String localDir = new File(filename).getAbsoluteFile().getParentFile().getAbsolutePath();
        numLightSamples = 1;
        numGlossySamples = 4;
        Timer timer = new Timer();
        timer.start();
        UI.printInfo("[API] Parsing \"%s\" ...", filename);
        try {
            p = new Parser(filename);
            while (true) {
                String token = p.getNextToken();
                if (token == null)
                    break;
                if (token.equals("image")) {
                    UI.printInfo("[API] Reading image settings ...");
                    parseImageBlock(api);
                } else if (token.equals("background")) {
                    UI.printInfo("[API] Reading background ...");
                    parseBackgroundBlock(api);
                } else if (token.equals("accel")) {
                    UI.printInfo("[API] Reading accelerator type ...");
                    api.accel(p.getNextToken());
                } else if (token.equals("filter")) {
                    UI.printInfo("[API] Reading image filter type ...");
                    parseFilter(api);
                } else if (token.equals("bucket")) {
                    UI.printInfo("[API] Reading bucket settings ...");
                    api.bucketSize(p.getNextInt());
                    api.bucketOrder(p.getNextToken());
                } else if (token.equals("photons")) {
                    UI.printInfo("[API] Reading photon settings ...");
                    parsePhotonBlock(api);
                } else if (token.equals("gi")) {
                    UI.printInfo("[API] Reading global illumination settings ...");
                    parseGIBlock(api);
                } else if (token.equals("lightserver")) {
                    UI.printInfo("[API] Reading light server settings ...");
                    parseLightserverBlock(api);
                } else if (token.equals("trace-depths")) {
                    UI.printInfo("[API] Reading trace depths ...");
                    parseTraceBlock(api);
                } else if (token.equals("camera")) {
                    UI.printInfo("[API] Reading camera ...");
                    parseCamera(api);
                } else if (token.equals("shader")) {
                    if (!parseShader(api))
                        return false;
                } else if (token.equals("override")) {
                    api.shaderOverride(p.getNextToken(), p.getNextBoolean());
                } else if (token.equals("object")) {
                    parseObjectBlock(api);
                } else if (token.equals("light")) {
                    parseLightBlock(api);
                } else if (token.equals("texturepath")) {
                    String path = p.getNextToken();
                    if (!new File(path).isAbsolute())
                        path = localDir + File.separator + path;
                    api.addTextureSearchPath(path);
                } else if (token.equals("includepath")) {
                    String path = p.getNextToken();
                    if (!new File(path).isAbsolute())
                        path = localDir + File.separator + path;
                    api.addIncludeSearchPath(path);
                } else if (token.equals("include")) {
                    String file = p.getNextToken();
                    UI.printInfo("[API] Including: \"%s\" ...", file);
                    api.parse(file);
                } else
                    UI.printWarning("[API] Unrecognized token %s", token);
            }
            p.close();
        } catch (ParserException e) {
            UI.printError("[API] %s", e.getMessage());
            e.printStackTrace();
            return false;
        } catch (FileNotFoundException e) {
            UI.printError("[API] %s", e.getMessage());
            return false;
        } catch (IOException e) {
            UI.printError("[API] %s", e.getMessage());
            return false;
        }
        timer.end();
        UI.printInfo("[API] Done parsing.");
        UI.printInfo("[API] Parsing time: %s", timer.toString());
        return true;
    }

    private void parseImageBlock(SunflowAPI api) throws IOException, ParserException {
        p.checkNextToken("{");
        p.checkNextToken("resolution");
        api.resolution(p.getNextInt(), p.getNextInt());
        p.checkNextToken("aa");
        int min = p.getNextInt();
        int max = p.getNextInt();
        api.antiAliasing(min, max);
        if (p.peekNextToken("samples")) {
            int n = p.getNextInt();
            api.antiAliasing(min, max, n);
        }
        if (p.peekNextToken("show-aa")) {
            UI.printWarning("[API] Deprecated: show-aa ignored");
            p.getNextBoolean();
        }
        if (p.peekNextToken("output")) {
            UI.printWarning("[API] Deprecated: output statement ignored");
            p.getNextToken();
        }
        p.checkNextToken("}");
    }

    private void parseBackgroundBlock(SunflowAPI api) throws IOException, ParserException {
        p.checkNextToken("{");
        p.checkNextToken("color");
        Color bg = parseColor();
        Geometry geo = new Geometry(new Background());
        api.instance(new Instance(new ConstantShader(bg), null, geo));
        p.checkNextToken("}");
    }

    private void parseFilter(SunflowAPI api) throws IOException, ParserException {
        if (p.peekNextToken("box")) {
            float w = p.getNextFloat();
            float h = p.getNextFloat();
            float s = (w + h) * 0.5f;
            api.filter(new BoxFilter(s));
        } else if (p.peekNextToken("gaussian")) {
            float w = p.getNextFloat();
            float h = p.getNextFloat();
            float s = (w + h) * 0.5f;
            api.filter(new GaussianFilter(s));
        } else if (p.peekNextToken("blackman-harris")) {
            float w = p.getNextFloat();
            float h = p.getNextFloat();
            float s = (w + h) * 0.5f;
            api.filter(new BlackmanHarrisFilter(s));
        } else if (p.peekNextToken("sinc")) {
            float w = p.getNextFloat();
            float h = p.getNextFloat();
            float s = (w + h) * 0.5f;
            api.filter(new SincFilter(s));
        } else if (p.peekNextToken("triangle")) {
            float w = p.getNextFloat();
            float h = p.getNextFloat();
            float s = (w + h) * 0.5f;
            api.filter(new TriangleFilter(s));
        } else
            api.filter(p.getNextToken());
    }

    private void parsePhotonBlock(SunflowAPI api) throws ParserException, IOException {
        String type;
        int gather;
        float radius;
        int numEmit = 0;
        boolean globalEmit = false;
        p.checkNextToken("{");
        if (p.peekNextToken("emit")) {
            UI.printWarning("[API] Shared photon emit values are deprectated - specify number of photons to emit per map");
            numEmit = p.getNextInt();
            globalEmit = true;
        }
        if (p.peekNextToken("global")) {
            UI.printWarning("[API] Global photon map setting belonds inside the gi block - ignoring");
            if (!globalEmit)
                p.getNextInt();
            p.getNextToken();
            p.getNextInt();
            p.getNextFloat();
        }
        p.checkNextToken("caustics");
        if (!globalEmit)
            numEmit = p.getNextInt();
        type = p.getNextToken();
        gather = p.getNextInt();
        radius = p.getNextFloat();
        CausticPhotonMapInterface cmap = null;
        if (type.equals("kd"))
            cmap = new CausticPhotonMap(numEmit, gather, radius, 1.1f);
        else if (type.equals("none"))
            cmap = null;
        else
            UI.printWarning("[API] Unrecognized caustic photon map type: %s", type);
        api.photons(cmap);
        p.checkNextToken("}");
    }

    private void parseGIBlock(SunflowAPI api) throws ParserException, IOException {
        p.checkNextToken("{");
        p.checkNextToken("type");
        if (p.peekNextToken("irr-cache")) {
            p.checkNextToken("samples");
            int samples = p.getNextInt();
            p.checkNextToken("tolerance");
            float tolerance = p.getNextFloat();
            p.checkNextToken("spacing");
            float min = p.getNextFloat();
            float max = p.getNextFloat();
            // parse global photon map info
            GlobalPhotonMapInterface gmap = null;
            if (p.peekNextToken("global")) {
                int numEmit = p.getNextInt();
                String type = p.getNextToken();
                int gather = p.getNextInt();
                float radius = p.getNextFloat();
                if (type.equals("kd"))
                    gmap = new GlobalPhotonMap(numEmit, gather, radius);
                else if (type.equals("grid"))
                    gmap = new GridPhotonMap(numEmit, gather, radius);
                else if (type.equals("none"))
                    gmap = null;
                else
                    UI.printWarning("[API] Unrecognized global photon map type: %s", type);
            }
            api.giEngine(new IrradianceCacheGIEngine(samples, tolerance, min, max, gmap));
        } else if (p.peekNextToken("path")) {
            p.checkNextToken("samples");
            int samples = p.getNextInt();
            if (p.peekNextToken("bounces")) {
                UI.printWarning("[API] Deprecated setting: bounces - use diffuse trace depth instead");
                p.getNextInt();
            }
            api.giEngine(new PathTracingGIEngine(samples));
        } else if (p.peekNextToken("fake")) {
            p.checkNextToken("up");
            Vector3 up = parseVector();
            p.checkNextToken("sky");
            Color sky = parseColor();
            p.checkNextToken("ground");
            Color ground = parseColor();
            api.giEngine(new FakeGIEngine(up, sky, ground));
        } else if (p.peekNextToken("igi")) {
            p.checkNextToken("samples");
            int samples = p.getNextInt();
            p.checkNextToken("sets");
            int sets = p.getNextInt();
            p.checkNextToken("b");
            float b = p.getNextFloat();
            p.checkNextToken("bias-samples");
            int bias = p.getNextInt();
            api.giEngine(new InstantGI(samples, sets, b, bias));
        } else if (p.peekNextToken("ambocc")) {
            p.checkNextToken("bright");
            Color bright = parseColor();
            p.checkNextToken("dark");
            Color dark = parseColor();
            p.checkNextToken("samples");
            int samples = p.getNextInt();
            float maxdist = 0;
            if (p.peekNextToken("maxdist"))
                maxdist = p.getNextFloat();
            api.giEngine(new AmbientOcclusionGIEngine(bright, dark, samples, maxdist));
        } else if (p.peekNextToken("none") || p.peekNextToken("null")) {
            // disable GI
            api.giEngine(null);
        } else
            UI.printWarning("[API] Unrecognized gi engine type: %s", p.getNextToken());
        p.checkNextToken("}");
    }

    private void parseLightserverBlock(SunflowAPI api) throws ParserException, IOException {
        p.checkNextToken("{");
        if (p.peekNextToken("shadows")) {
            UI.printWarning("[API] Deprecated: shadows setting ignored");
            p.getNextBoolean();
        }
        if (p.peekNextToken("direct-samples")) {
            UI.printWarning("[API] Deprecated: use samples keyword in area light definitions");
            numLightSamples = p.getNextInt();
        }
        if (p.peekNextToken("glossy-samples")) {
            UI.printWarning("[API] Deprecated: use samples keyword in glossy shader definitions");
            numGlossySamples = p.getNextInt();
        }
        if (p.peekNextToken("max-depth")) {
            UI.printWarning("[API] Deprecated: max-depth setting - use trace-depths block instead");
            int d = p.getNextInt();
            api.traceDepth(1, d - 1, 0);
        }
        if (p.peekNextToken("global")) {
            UI.printWarning("[API] Deprecated: global settings ignored - use photons block instead");
            p.getNextBoolean();
            p.getNextInt();
            p.getNextInt();
            p.getNextInt();
            p.getNextFloat();
        }
        if (p.peekNextToken("caustics")) {
            UI.printWarning("[API] Deprecated: caustics settings ignored - use photons block instead");
            p.getNextBoolean();
            p.getNextInt();
            p.getNextFloat();
            p.getNextInt();
            p.getNextFloat();
        }
        if (p.peekNextToken("irr-cache")) {
            UI.printWarning("[API] Deprecated: irradiance cache settings ignored - use gi block instead");
            p.getNextInt();
            p.getNextFloat();
            p.getNextFloat();
            p.getNextFloat();
        }
        p.checkNextToken("}");
    }

    private void parseTraceBlock(SunflowAPI api) throws ParserException, IOException {
        int diff = 0, refl = 0, refr = 0;
        p.checkNextToken("{");
        if (p.peekNextToken("diff"))
            diff = p.getNextInt();
        if (p.peekNextToken("refl"))
            refl = p.getNextInt();
        if (p.peekNextToken("refr"))
            refr = p.getNextInt();
        p.checkNextToken("}");
        api.traceDepth(diff, refl, refr);
    }

    private void parseCamera(SunflowAPI api) throws ParserException, IOException {
        p.checkNextToken("{");
        p.checkNextToken("type");
        if (p.peekNextToken("pinhole")) {
            UI.printInfo("[API] Reading pinhole camera ...");
            p.checkNextToken("eye");
            Point3 eye = parsePoint();
            p.checkNextToken("target");
            Point3 target = parsePoint();
            p.checkNextToken("up");
            Vector3 up = parseVector();
            p.checkNextToken("fov");
            float fov = p.getNextFloat();
            p.checkNextToken("aspect");
            float aspect = p.getNextFloat();
            api.camera(new PinholeCamera(eye, target, up, fov, aspect));
        } else if (p.peekNextToken("thinlens")) {
            UI.printInfo("[API] Reading thinlens camera ...");
            p.checkNextToken("eye");
            Point3 eye = parsePoint();
            p.checkNextToken("target");
            Point3 target = parsePoint();
            p.checkNextToken("up");
            Vector3 up = parseVector();
            p.checkNextToken("fov");
            float fov = p.getNextFloat();
            p.checkNextToken("aspect");
            float aspect = p.getNextFloat();
            p.checkNextToken("fdist");
            float fdist = p.getNextFloat();
            p.checkNextToken("lensr");
            float lensr = p.getNextFloat();
            api.camera(new ThinLensCamera(eye, target, up, fov, aspect, fdist, lensr));
        } else if (p.peekNextToken("spherical")) {
            UI.printInfo("[API] Reading spherical camera ...");
            p.checkNextToken("eye");
            Point3 eye = parsePoint();
            p.checkNextToken("target");
            Point3 target = parsePoint();
            p.checkNextToken("up");
            Vector3 up = parseVector();
            api.camera(new SphericalCamera(eye, target, up));
        } else
            UI.printWarning("[API] Unrecognized camera type: %s", p.getNextToken());
        p.checkNextToken("}");
    }

    private boolean parseShader(SunflowAPI api) throws ParserException, IOException {
        p.checkNextToken("{");
        p.checkNextToken("name");
        String name = p.getNextToken();
        UI.printInfo("[API] Reading shader: %s ...", name);
        p.checkNextToken("type");
        if (p.peekNextToken("diffuse")) {
            if (p.peekNextToken("diff")) {
                Color d = parseColor();
                api.shader(name, new DiffuseShader(d));
            } else if (p.peekNextToken("texture")) {
                String filename = api.resolveTextureFilename(p.getNextToken());
                api.shader(name, new TexturedDiffuseShader(filename));
            } else
                UI.printWarning("[API] Unrecognized option in diffuse shader block: %s", p.getNextToken());
        } else if (p.peekNextToken("phong")) {
            String tex = null;
            Color d = null;
            if (p.peekNextToken("texture"))
                tex = api.resolveTextureFilename(p.getNextToken());
            else {
                p.checkNextToken("diff");
                d = parseColor();
            }
            p.checkNextToken("spec");
            Color s = parseColor();
            float sn = p.getNextFloat();
            int samples = numGlossySamples;
            if (p.peekNextToken("samples"))
                samples = p.getNextInt();
            else
                UI.printWarning("[API] Samples keyword not found - defaulting to %d", samples);
            if (tex != null)
                api.shader(name, new TexturedPhongShader(tex, s, sn, samples));
            else
                api.shader(name, new PhongShader(d, s, sn, samples));
        } else if (p.peekNextToken("amb-occ") || p.peekNextToken("amb-occ2")) {
            if (p.peekNextToken("diff")) {
                Color d = parseColor();
                api.shader(name, new AmbientOcclusionShader(d));
            } else if (p.peekNextToken("texture")) {
                String texture = api.resolveTextureFilename(p.getNextToken());
                if (p.peekNextToken("dark")) {
                    Color d = parseColor();
                    p.checkNextToken("samples");
                    int n = p.getNextInt();
                    p.checkNextToken("dist");
                    float dist = p.getNextFloat();
                    api.shader(name, new TexturedAmbientOcclusionShader(texture, d, n, dist));
                } else
                    api.shader(name, new TexturedAmbientOcclusionShader(texture));
            } else if (p.peekNextToken("bright")) {
                Color b = parseColor();
                p.checkNextToken("dark");
                Color d = parseColor();
                p.checkNextToken("samples");
                int n = p.getNextInt();
                p.checkNextToken("dist");
                float dist = p.getNextFloat();
                api.shader(name, new AmbientOcclusionShader(b, d, n, dist));
            } else
                UI.printWarning("[API] Unrecognized option in ambient occlusion shader block: %s", p.getNextToken());
        } else if (p.peekNextToken("mirror")) {
            p.checkNextToken("refl");
            Color r = parseColor();
            api.shader(name, new MirrorShader(r));
        } else if (p.peekNextToken("glass")) {
            p.checkNextToken("eta");
            float eta = p.getNextFloat();
            p.checkNextToken("color");
            Color c = parseColor();
            api.shader(name, new GlassShader(eta, c));
        } else if (p.peekNextToken("shiny")) {
            if (p.peekNextToken("diff")) {
                Color d = parseColor();
                p.checkNextToken("refl");
                float r = p.getNextFloat();
                api.shader(name, new ShinyDiffuseShader(d, r));
            } else if (p.peekNextToken("texture")) {
                String tex = api.resolveTextureFilename(p.getNextToken());
                p.checkNextToken("refl");
                float r = p.getNextFloat();
                api.shader(name, new TexturedShinyDiffuseShader(tex, r));
            } else
                UI.printWarning("[API] Unrecognized option in shiny shader block: %s", p.getNextToken());
        } else if (p.peekNextToken("ward")) {
            String tex = null;
            Color d = null;
            if (p.peekNextToken("texture"))
                tex = api.resolveTextureFilename(p.getNextToken());
            else {
                p.checkNextToken("diff");
                d = parseColor();
            }
            p.checkNextToken("spec");
            Color s = parseColor();
            p.checkNextToken("rough");
            float rx = p.getNextFloat();
            float ry = p.getNextFloat();
            int samples = numGlossySamples;
            if (p.peekNextToken("samples"))
                samples = p.getNextInt();
            else
                UI.printWarning("[API] Samples keyword not found - defaulting to %d", samples);
            if (tex != null)
                api.shader(name, new TexturedWardShader(tex, s, rx, ry, samples));
            else
                api.shader(name, new AnisotropicWardShader(d, s, rx, ry, samples));
        } else if (p.peekNextToken("view-caustics")) {
            api.shader(name, new ViewCausticsShader());
        } else if (p.peekNextToken("view-irradiance")) {
            api.shader(name, new ViewIrradianceShader());
        } else if (p.peekNextToken("view-global")) {
            api.shader(name, new ViewGlobalPhotonsShader());
        } else if (p.peekNextToken("constant")) {
            p.peekNextToken("color"); // we don't check for this token for
            // backwards compatibility
            Color c = parseColor();
            api.shader(name, new ConstantShader(c));
        } else if (p.peekNextToken("janino")) {
            String code = p.getNextCodeBlock();
            try {
                Shader shader = (Shader) ClassBodyEvaluator.createFastClassBodyEvaluator(new Scanner(null, new StringReader(code)), Shader.class, ClassLoader.getSystemClassLoader());
                api.shader(name, shader);
            } catch (CompileException e) {
                UI.printInfo("[API] Compiling: %s", code);
                UI.printError("[API] %s", e.getMessage());
                e.printStackTrace();
                return false;
            } catch (ParseException e) {
                UI.printInfo("[API] Compiling: %s", code);
                UI.printError("[API] %s", e.getMessage());
                e.printStackTrace();
                return false;
            } catch (ScanException e) {
                UI.printInfo("[API] Compiling: %s", code);
                UI.printError("[API] %s", e.getMessage());
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                UI.printInfo("[API] Compiling: %s", code);
                UI.printError("[API] %s", e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else if (p.peekNextToken("id")) {
            api.shader(name, new IDShader());
        } else if (p.peekNextToken("uber")) {

            p.checkNextToken("diff");
            Color d = parseColor();
            p.checkNextToken("refl");
            Color r = parseColor();
            String tex = null;
            float amount = 0; // amount of texture to blend in to diffuse
            if (p.peekNextToken("texture")) {
                tex = api.resolveTextureFilename(p.getNextToken());
                amount = p.getNextFloat();
            }
            api.shader(name, new UberShader(d, r, tex, amount));
        } else
            UI.printWarning("[API] Unrecognized shader type: %s", p.getNextToken());
        p.checkNextToken("}");
        return true;
    }

    private void parseObjectBlock(SunflowAPI api) throws ParserException, IOException {
        p.checkNextToken("{");
        Shader[] shaders = null;
        boolean multiShader = false;
        if (p.peekNextToken("shaders")) {
            int n = p.getNextInt();
            shaders = new Shader[n];
            for (int i = 0; i < n; i++)
                shaders[i] = api.shader(p.getNextToken());
            multiShader = true;
        } else {
            p.checkNextToken("shader");
            shaders = new Shader[1];
            shaders[0] = api.shader(p.getNextToken());
        }
        p.checkNextToken("type");
        if (p.peekNextToken("mesh")) {
            UI.printWarning("[API] Deprecated object type: mesh");
            p.checkNextToken("name");
            UI.printInfo("[API] Reading mesh: %s ...", p.getNextToken());
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
            Mesh mesh = new Mesh(points, triangles);
            mesh.normals(Mesh.InterpType.VERTEX, normals);
            mesh.uvs(Mesh.InterpType.VERTEX, uvs);
            Geometry geo = new Geometry(mesh);
            Instance instance = new Instance(shaders, null, geo);
            api.instance(instance);
        } else if (p.peekNextToken("flat-mesh")) {
            UI.printWarning("[API] Deprecated object type: flat-mesh");
            p.checkNextToken("name");
            UI.printInfo("[API] Reading flat mesh: %s ...", p.getNextToken());
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
            Mesh mesh = new Mesh(points, triangles);
            mesh.normals(Mesh.InterpType.NONE, null);
            mesh.uvs(Mesh.InterpType.VERTEX, uvs);
            Geometry geo = new Geometry(mesh);
            Instance instance = new Instance(shaders, null, geo);
            api.instance(instance);
        } else if (p.peekNextToken("sphere")) {
            UI.printInfo("[API] Reading sphere ...");
            if (p.peekNextToken("c")) {
                float cx = p.getNextFloat();
                float cy = p.getNextFloat();
                float cz = p.getNextFloat();
                p.checkNextToken("r");
                float r = p.getNextFloat();
                api.sphere(cx, cy, cz, r);
            } else {
                Matrix4 m = parseMatrix();
                api.sphere(m);
            }
        } else if (p.peekNextToken("banchoff")) {
            UI.printInfo("[API] Reading banchoff ...");
            Matrix4 m = parseMatrix();
            BanchoffSurface surf = new BanchoffSurface();
            Geometry g = new Geometry(surf);
            api.instance(new Instance(shaders, m, g));
        } else if (p.peekNextToken("torus")) {
            UI.printInfo("[API] Reading banchoff ...");
            Matrix4 m = parseMatrix();
            p.checkNextToken("r");
            float ri = p.getNextFloat();
            float ro = p.getNextFloat();
            Torus torus = new Torus(ri, ro);
            Geometry geo = new Geometry(torus);
            api.instance(new Instance(shaders, m, geo));
        } else if (p.peekNextToken("plane")) {
            UI.printInfo("[API] Reading plane ...");
            Plane plane = null;
            p.checkNextToken("p");
            Point3 p0 = parsePoint();
            if (p.peekNextToken("n")) {
                Vector3 n = parseVector();
                plane = new Plane(p0, n);
            } else if (p.peekNextToken("p")) {
                Point3 pu = parsePoint();
                p.checkNextToken("p");
                Point3 pv = parsePoint();
                plane = new Plane(p0, pu, pv);
            }
            if (plane != null) {
                Geometry geo = new Geometry(plane);
                api.instance(new Instance(shaders, null, geo));
            }
        } else if (p.peekNextToken("cornellbox")) {
            UI.printInfo("[API] Reading cornell box ...");
            p.checkNextToken("corner0");
            Point3 c0 = parsePoint();
            p.checkNextToken("corner1");
            Point3 c1 = parsePoint();
            p.checkNextToken("left");
            Color left = parseColor();
            p.checkNextToken("right");
            Color right = parseColor();
            p.checkNextToken("top");
            Color top = parseColor();
            p.checkNextToken("bottom");
            Color bottom = parseColor();
            p.checkNextToken("back");
            Color back = parseColor();
            p.checkNextToken("emit");
            Color emit = parseColor();
            int samples = numLightSamples;
            if (p.peekNextToken("samples"))
                samples = p.getNextInt();
            else
                UI.printWarning("[API] Samples keyword not found - defaulting to %d", samples);
            CornellBox box = new CornellBox(c0, c1, left, right, top, bottom, back, emit, samples);
            Geometry geo = new Geometry(box);
            api.instance(new Instance((Shader) null, null, geo));
            api.light(box);
        } else if (p.peekNextToken("generic-mesh")) {
            p.checkNextToken("name");
            UI.printInfo("[API] Reading generic mesh: %s ... ", p.getNextToken());
            // parse vertices
            p.checkNextToken("points");
            int np = p.getNextInt();
            float[] points = new float[np * 3];
            for (int i = 0; i < points.length; i++)
                points[i] = p.getNextFloat();
            // parse triangle indices
            p.checkNextToken("triangles");
            int nt = p.getNextInt();
            int[] triangles = new int[nt * 3];
            for (int i = 0; i < triangles.length; i++)
                triangles[i] = p.getNextInt();
            // create basic mesh
            Mesh mesh = new Mesh(points, triangles);
            // parse normals
            p.checkNextToken("normals");
            if (p.peekNextToken("vertex")) {
                float[] normals = new float[np * 3];
                for (int i = 0; i < normals.length; i++)
                    normals[i] = p.getNextFloat();
                mesh.normals(Mesh.InterpType.VERTEX, normals);
            } else if (p.peekNextToken("facevarying")) {
                float[] normals = new float[nt * 9];
                for (int i = 0; i < normals.length; i++)
                    normals[i] = p.getNextFloat();
                mesh.normals(Mesh.InterpType.FACEVARYING, normals);
            } else {
                p.checkNextToken("none");
                mesh.normals(Mesh.InterpType.NONE, null);
            }
            // parse texture coordinates
            p.checkNextToken("uvs");
            float[] uvs = null;
            if (p.peekNextToken("vertex")) {
                uvs = new float[np * 2];
                for (int i = 0; i < uvs.length; i++)
                    uvs[i] = p.getNextFloat();
                mesh.uvs(Mesh.InterpType.VERTEX, uvs);
            } else if (p.peekNextToken("facevarying")) {
                uvs = new float[nt * 6];
                for (int i = 0; i < uvs.length; i++)
                    uvs[i] = p.getNextFloat();
                mesh.uvs(Mesh.InterpType.FACEVARYING, uvs);
            } else {
                p.checkNextToken("none");
                mesh.uvs(Mesh.InterpType.NONE, null);
            }
            if (multiShader) {
                p.checkNextToken("face_shaders");
                byte[] faceShaders = new byte[nt];
                for (int i = 0; i < faceShaders.length; i++)
                    faceShaders[i] = (byte) p.getNextInt();
                mesh.faceShaders(faceShaders);
            }
            Geometry geo = new Geometry(mesh);
            Instance instance = new Instance(shaders, null, geo);
            api.instance(instance);
        } else
            UI.printWarning("[API] Unrecognized object type: %s", p.getNextToken());
        p.checkNextToken("}");
    }

    private void parseLightBlock(SunflowAPI api) throws ParserException, IOException {
        p.checkNextToken("{");
        p.checkNextToken("type");
        if (p.peekNextToken("mesh")) {
            UI.printWarning("[API] Deprecated light type: mesh");
            p.checkNextToken("name");
            UI.printInfo("[API] Reading light mesh: %s ...", p.getNextToken());
            p.checkNextToken("emit");
            Color e = parseColor();
            int samples = numLightSamples;
            if (p.peekNextToken("samples"))
                samples = p.getNextInt();
            else
                UI.printWarning("[API] Samples keyword not found - defaulting to %d", samples);
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
            MeshLight mesh = new MeshLight(points, triangles, e, samples);
            mesh.init(api);
        } else if (p.peekNextToken("point")) {
            UI.printInfo("[API] Reading point light ...");
            Color pow;
            if (p.peekNextToken("color")) {
                pow = parseColor();
                p.checkNextToken("power");
                float po = p.getNextFloat();
                pow.mul(po);
            } else {
                UI.printWarning("[API] Deprecated color specification - please use color and power instead");
                p.checkNextToken("power");
                pow = parseColor();
            }
            p.checkNextToken("p");
            float px = p.getNextFloat();
            float py = p.getNextFloat();
            float pz = p.getNextFloat();
            api.pointLight(px, py, pz, pow);
        } else if (p.peekNextToken("directional")) {
            UI.printInfo("[API] Reading directional light ...");
            p.checkNextToken("source");
            Point3 s = parsePoint();
            p.checkNextToken("target");
            Point3 t = parsePoint();
            p.checkNextToken("radius");
            float r = p.getNextFloat();
            p.checkNextToken("emit");
            Color e = parseColor();
            if (p.peekNextToken("intensity")) {
                float i = p.getNextFloat();
                e.mul(i);
            } else
                UI.printWarning("[API] Deprecated color specification - please use emit and intensity instead");
            api.light(new DirectionalSpotlight(s, t, r, e));
        } else if (p.peekNextToken("ibl")) {
            UI.printInfo("[API] Reading image based light ...");
            p.checkNextToken("image");
            String img = api.resolveTextureFilename(p.getNextToken());
            p.checkNextToken("center");
            Vector3 c = parseVector();
            p.checkNextToken("up");
            Vector3 u = parseVector();
            p.checkNextToken("lock");
            boolean lock = p.getNextBoolean();
            int samples = numLightSamples;
            if (p.peekNextToken("samples"))
                samples = p.getNextInt();
            else
                UI.printWarning("[API] Samples keyword not found - defaulting to %d", samples);
            ImageBasedLight ibl = new ImageBasedLight(img, c, u, samples, lock);
            ibl.init(api);
        } else if (p.peekNextToken("meshlight")) {
            p.checkNextToken("name");
            UI.printInfo("[API] Reading meshlight: %s ...", p.getNextToken());
            p.checkNextToken("emit");
            Color e = parseColor();
            if (p.peekNextToken("radiance")) {
                float r = p.getNextFloat();
                e.mul(r);
            } else
                UI.printWarning("[API] Deprecated color specification - please use emit and radiance instead");
            int samples = numLightSamples;
            if (p.peekNextToken("samples"))
                samples = p.getNextInt();
            else
                UI.printWarning("[API] Samples keyword not found - defaulting to %d", samples);
            // parse vertices
            p.checkNextToken("points");
            int np = p.getNextInt();
            float[] points = new float[np * 3];
            for (int i = 0; i < points.length; i++)
                points[i] = p.getNextFloat();
            // parse triangle indices
            p.checkNextToken("triangles");
            int nt = p.getNextInt();
            int[] triangles = new int[nt * 3];
            for (int i = 0; i < triangles.length; i++)
                triangles[i] = p.getNextInt();
            MeshLight mesh = new MeshLight(points, triangles, e, samples);
            mesh.init(api);
        } else
            UI.printWarning("[API] Unrecognized object type: %s", p.getNextToken());
        p.checkNextToken("}");
    }

    private Color parseColor() throws IOException, ParserException {
        if (p.peekNextToken("{")) {
            String space = p.getNextToken();
            Color c = null;
            if (space.equals("sRGB nonlinear")) {
                float r = p.getNextFloat();
                float g = p.getNextFloat();
                float b = p.getNextFloat();
                c = new Color(r, g, b);
                c.toLinear();
            } else if (space.equals("sRGB linear")) {
                float r = p.getNextFloat();
                float g = p.getNextFloat();
                float b = p.getNextFloat();
                c = new Color(r, g, b);
            } else
                UI.printWarning("[API] Unrecognized color space: %s", space);
            p.checkNextToken("}");
            return c;
        } else {
            float r = p.getNextFloat();
            float g = p.getNextFloat();
            float b = p.getNextFloat();
            return new Color(r, g, b);
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

    private Matrix4 parseMatrix() throws IOException, ParserException {
        p.checkNextToken("matrix");
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
                UI.printWarning("[API] Unrecognized transformation type: %s", p.getNextToken());
            if (t != null)
                m = t.multiply(m);
        }
        return m;
    }
}