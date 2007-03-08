package org.sunflow.core.parser;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.sunflow.PluginRegistry;
import org.sunflow.SunflowAPI;
import org.sunflow.core.SceneParser;
import org.sunflow.image.Color;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point2;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.Parser;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.system.Parser.ParserException;
import org.sunflow.system.UI.Module;

public class SCAsciiParser implements SceneParser {
    private Parser p;

    public boolean parse(String filename, SunflowAPI api) {
        Timer timer = new Timer();
        timer.start();
        UI.printInfo(Module.API, "Parsing \"%s\" ...", filename);
        try {
            p = new Parser(filename);
            while (true) {
                if (peekTokens("parameter", "param", "p")) {
                    parseParameter(api);
                } else if (peekTokens("geometry", "geo", "g")) {
                    String name = p.getNextToken();
                    String type = p.getNextToken();
                    if (type.equals("incremental"))
                        type = null;
                    api.geometry(name, type);
                } else if (peekTokens("instance", "inst", "i")) {
                    String name = p.getNextToken();
                    String geoname = p.getNextToken();
                    if (geoname.equals("incremental"))
                        geoname = null;
                    api.instance(name, geoname);
                } else if (peekTokens("shader", "shd", "s")) {
                    String name = p.getNextToken();
                    String type = p.getNextToken();
                    if (type.equals("incremental"))
                        type = null;
                    api.shader(name, type);
                } else if (peekTokens("modifier", "mod", "m")) {
                    String name = p.getNextToken();
                    String type = p.getNextToken();
                    if (type.equals("incremental"))
                        type = null;
                    api.modifier(name, type);
                } else if (peekTokens("light", "l")) {
                    String name = p.getNextToken();
                    String type = p.getNextToken();
                    if (type.equals("incremental"))
                        type = null;
                    api.light(name, type);
                } else if (peekTokens("camera", "cam", "c")) {
                    String name = p.getNextToken();
                    String type = p.getNextToken();
                    if (type.equals("incremental"))
                        type = null;
                    api.camera(name, type);
                } else if (peekTokens("options", "opt", "o")) {
                    api.options(p.getNextToken());
                } else if (peekTokens("include", "inc")) {
                    String file = p.getNextToken();
                    UI.printInfo(Module.API, "Including: \"%s\" ...", file);
                    api.parse(file);
                } else if (peekTokens("plugin", "plug")) {
                    parsePlugin();
                } else if (peekTokens("searchpath", "spath", "sp")) {
                    if (peekTokens("include", "inc"))
                        api.addIncludeSearchPath(p.getNextToken());
                    else if (peekTokens("texture", "tex"))
                        api.addTextureSearchPath(p.getNextToken());
                    else
                        UI.printWarning(Module.API, "Unrecognized search path type \"%s\"", p.getNextToken());
                } else {
                    String token = p.getNextToken();
                    if (token == null)
                        break;
                    UI.printWarning(Module.API, "Unrecognized token \"%s\"", token);
                }
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
        }
        timer.end();
        UI.printInfo(Module.API, "Done parsing (took %s)", timer.toString());
        return true;
    }

    private void parseParameter(SunflowAPI api) throws IOException, ParserException {
        String name = p.getNextToken();
        if (peekTokens("string", "str", "s"))
            api.parameter(name, p.getNextToken());
        else if (peekTokens("boolean", "bool", "b"))
            api.parameter(name, p.getNextBoolean());
        else if (peekTokens("integer", "int", "i"))
            api.parameter(name, p.getNextInt());
        else if (peekTokens("float", "flt", "f"))
            api.parameter(name, p.getNextFloat());
        else if (peekTokens("color", "col", "c"))
            api.parameter(name, parseColor());
        else if (peekTokens("point", "pnt", "p"))
            api.parameter(name, parsePoint());
        else if (peekTokens("vector", "vec", "v"))
            api.parameter(name, parseVector());
        else if (peekTokens("texcoord", "tex", "t"))
            api.parameter(name, parseTexcoord());
        else if (peekTokens("matrix", "mat", "m"))
            api.parameter(name, parseMatrix());
        else if (peekTokens("string[]", "str[]", "s[]")) {
            int n = p.getNextInt();
            p.checkNextToken("{");
            api.parameter(name, parseStringArray(n));
            p.checkNextToken("}");
        } else if (peekTokens("integer[]", "int[]", "i[]")) {
            int n = p.getNextInt();
            p.checkNextToken("{");
            api.parameter(name, parseIntArray(n));
            p.checkNextToken("}");
        } else if (peekTokens("float[]", "flt[]", "f[]")) {
            int n = p.getNextInt();
            p.checkNextToken("{");
            api.parameter(name, "float", parseInterpolationType(), parseFloatArray(n));
            p.checkNextToken("}");
        } else if (peekTokens("point[]", "pnt[]", "p[]")) {
            int n = p.getNextInt();
            p.checkNextToken("{");
            api.parameter(name, "point", parseInterpolationType(), parseFloatArray(3 * n));
            p.checkNextToken("}");
        } else if (peekTokens("vector[]", "vec[]", "v[]")) {
            int n = p.getNextInt();
            p.checkNextToken("{");
            api.parameter(name, "vector", parseInterpolationType(), parseFloatArray(3 * n));
            p.checkNextToken("}");
        } else if (peekTokens("texcoord[]", "tex[]", "t[]")) {
            int n = p.getNextInt();
            p.checkNextToken("{");
            api.parameter(name, "texcoord", parseInterpolationType(), parseFloatArray(2 * n));
            p.checkNextToken("}");
        } else if (peekTokens("matrix[]", "mat[]", "m[]")) {
            int n = p.getNextInt();
            p.checkNextToken("{");
            api.parameter(name, "matrix", parseInterpolationType(), parseMatrixArray(n));
            p.checkNextToken("}");
        } else {
            // bad parameter type - warn and ignore
            UI.printWarning(Module.API, "Unknown parameter type \"%s\" - ignoring");
        }
    }

    private void parsePlugin() throws IOException, ParserException {
        String type = p.getNextToken();
        String name = p.getNextToken();
        String code = p.getNextCodeBlock();
        if (type.equals("primitive"))
            PluginRegistry.primitivePlugins.registerPlugin(name, code);
        else if (type.equals("tesselatable"))
            PluginRegistry.tesselatablePlugins.registerPlugin(name, code);
        else if (type.equals("shader"))
            PluginRegistry.shaderPlugins.registerPlugin(name, code);
        else if (type.equals("modifier"))
            PluginRegistry.modifierPlugins.registerPlugin(name, code);
        else if (type.equals("camera_lens"))
            PluginRegistry.cameraLensPlugins.registerPlugin(name, code);
        else if (type.equals("light"))
            PluginRegistry.lightSourcePlugins.registerPlugin(name, code);
        else if (type.equals("accel"))
            PluginRegistry.accelPlugins.registerPlugin(name, code);
        else if (type.equals("bucket_order"))
            PluginRegistry.bucketOrderPlugins.registerPlugin(name, code);
        else if (type.equals("filter"))
            PluginRegistry.filterPlugins.registerPlugin(name, code);
        else if (type.equals("gi_engine"))
            PluginRegistry.giEnginePlugins.registerPlugin(name, code);
        else if (type.equals("caustic_photon_map"))
            PluginRegistry.causticPhotonMapPlugins.registerPlugin(name, code);
        else if (type.equals("global_photon_map"))
            PluginRegistry.globalPhotonMapPlugins.registerPlugin(name, code);
        else if (type.equals("image_sampler"))
            PluginRegistry.imageSamplerPlugins.registerPlugin(name, code);
        else if (type.equals("parser"))
            PluginRegistry.parserPlugins.registerPlugin(name, code);
        else
            UI.printWarning(Module.API, "Unrecognized plugin type: \"%s\" - ignoring", type);
    }

    private boolean peekTokens(String token, String... tokens) throws IOException {
        if (p.peekNextToken(token))
            return true;
        for (String t : tokens)
            if (p.peekNextToken(t))
                return true;
        return false;
    }

    private String parseInterpolationType() throws IOException {
        String interp = "none";
        if (p.peekNextToken("none"))
            interp = "none";
        else if (p.peekNextToken("vertex"))
            interp = "vertex";
        else if (p.peekNextToken("facevarying"))
            interp = "facevarying";
        return interp;
    }

    private Color parseColor() throws IOException {
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
            UI.printWarning(Module.API, "Unrecognized color space: %s", space);
        return c;
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

    private Point2 parseTexcoord() throws IOException {
        float x = p.getNextFloat();
        float y = p.getNextFloat();
        return new Point2(x, y);
    }

    private String[] parseStringArray(int size) throws IOException {
        String[] data = new String[size];
        for (int i = 0; i < size; i++)
            data[i] = p.getNextToken();
        return data;
    }

    private int[] parseIntArray(int size) throws IOException {
        int[] data = new int[size];
        for (int i = 0; i < size; i++)
            data[i] = p.getNextInt();
        return data;
    }

    private float[] parseFloatArray(int size) throws IOException {
        float[] data = new float[size];
        for (int i = 0; i < size; i++)
            data[i] = p.getNextFloat();
        return data;
    }

    private float[] parseMatrixArray(int size) throws IOException, ParserException {
        float[] data = new float[16 * size];
        for (int i = 0, offset = 0; i < size; i++, offset += 16) {
            // copy the next matrix into a linear array - in row major order
            float[] rowdata = parseMatrix().asRowMajor();
            for (int j = 0; j < 16; j++)
                data[offset + j] = rowdata[j];
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