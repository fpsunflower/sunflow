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
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.system.Parser.ParserException;
import org.sunflow.system.UI.Module;

public abstract class SCAbstractParser implements SceneParser {
    private String current;

    public boolean parse(String filename, SunflowAPI api) {
        Timer timer = new Timer();
        timer.start();
        UI.printInfo(Module.API, "Parsing \"%s\" ...", filename);
        try {
            openParser(filename);
            while (hasMoreData()) {
                if (peekTokens("parameter", "param", "p")) {
                    parseParameter(api);
                } else if (peekTokens("geometry", "geo", "g")) {
                    String name = parseString();
                    String type = parseString();
                    if (type.equals("incremental"))
                        type = null;
                    api.geometry(name, type);
                } else if (peekTokens("instance", "inst", "i")) {
                    String name = parseString();
                    String geoname = parseString();
                    if (geoname.equals("incremental"))
                        geoname = null;
                    api.instance(name, geoname);
                } else if (peekTokens("shader", "shd", "s")) {
                    String name = parseString();
                    String type = parseString();
                    if (type.equals("incremental"))
                        type = null;
                    api.shader(name, type);
                } else if (peekTokens("modifier", "mod", "m")) {
                    String name = parseString();
                    String type = parseString();
                    if (type.equals("incremental"))
                        type = null;
                    api.modifier(name, type);
                } else if (peekTokens("light", "l")) {
                    String name = parseString();
                    String type = parseString();
                    if (type.equals("incremental"))
                        type = null;
                    api.light(name, type);
                } else if (peekTokens("camera", "cam", "c")) {
                    String name = parseString();
                    String type = parseString();
                    if (type.equals("incremental"))
                        type = null;
                    api.camera(name, type);
                } else if (peekTokens("options", "opt", "o")) {
                    api.options(parseString());
                } else if (peekTokens("include", "inc")) {
                    String file = parseString();
                    UI.printInfo(Module.API, "Including: \"%s\" ...", file);
                    api.parse(file);
                } else if (peekTokens("plugin", "plug")) {
                    parsePlugin();
                } else if (peekTokens("searchpath", "spath", "sp")) {
                    if (peekTokens("include", "inc"))
                        api.addIncludeSearchPath(parseString());
                    else if (peekTokens("texture", "tex"))
                        api.addTextureSearchPath(parseString());
                    else
                        UI.printWarning(Module.API, "Unrecognized search path type \"%s\"", current);
                } else
                    UI.printWarning(Module.API, "Unrecognized token \"%s\"", current);
            }
            closeParser();
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

    private boolean peekTokens(String token, String... tokens) throws IOException {
        if (current == null)
            current = parseString();
        if (current.equals(token)) {
            current = null;
            return true;
        }
        for (String t : tokens) {
            if (current.equals(t)) {
                current = null;
                return true;
            }
        }
        return false;
    }

    private void parseParameter(SunflowAPI api) throws IOException, ParserException {
        String name = parseString();
        if (peekTokens("string", "str", "s"))
            api.parameter(name, parseString());
        else if (peekTokens("boolean", "bool", "b"))
            api.parameter(name, parseBoolean());
        else if (peekTokens("integer", "int", "i"))
            api.parameter(name, parseInt());
        else if (peekTokens("float", "flt", "f"))
            api.parameter(name, parseFloat());
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
            int n = parseInt();
            api.parameter(name, parseStringArray(n));
        } else if (peekTokens("integer[]", "int[]", "i[]")) {
            int n = parseInt();
            api.parameter(name, parseIntArray(n));
        } else if (peekTokens("float[]", "flt[]", "f[]")) {
            int n = parseInt();
            api.parameter(name, "float", parseInterpolationType(), parseFloatArray(n));
        } else if (peekTokens("point[]", "pnt[]", "p[]")) {
            int n = parseInt();
            api.parameter(name, "point", parseInterpolationType(), parseFloatArray(3 * n));
        } else if (peekTokens("vector[]", "vec[]", "v[]")) {
            int n = parseInt();
            api.parameter(name, "vector", parseInterpolationType(), parseFloatArray(3 * n));
        } else if (peekTokens("texcoord[]", "tex[]", "t[]")) {
            int n = parseInt();
            api.parameter(name, "texcoord", parseInterpolationType(), parseFloatArray(2 * n));
        } else if (peekTokens("matrix[]", "mat[]", "m[]")) {
            int n = parseInt();
            api.parameter(name, "matrix", parseInterpolationType(), parseMatrixArray(n));
        } else {
            // bad parameter type - warn and ignore
            UI.printWarning(Module.API, "Unknown parameter type \"%s\" - ignoring");
        }
    }

    private String parseInterpolationType() throws IOException {
        String interp = "none";
        if (peekTokens("none"))
            interp = "none";
        else if (peekTokens("vertex"))
            interp = "vertex";
        else if (peekTokens("facevarying"))
            interp = "facevarying";
        return interp;
    }

    private void parsePlugin() throws IOException, ParserException {
        String type = parseString();
        String name = parseString();
        String code = parseVerbatimString();
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

    private String[] parseStringArray(int size) throws IOException {
        String[] data = new String[size];
        for (int i = 0; i < size; i++)
            data[i] = parseString();
        return data;
    }

    private int[] parseIntArray(int size) throws IOException {
        int[] data = new int[size];
        for (int i = 0; i < size; i++)
            data[i] = parseInt();
        return data;
    }

    private float[] parseFloatArray(int size) throws IOException {
        float[] data = new float[size];
        for (int i = 0; i < size; i++)
            data[i] = parseFloat();
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

    // abstract methods - to be implemented by subclasses

    protected abstract void openParser(String filename) throws IOException;

    protected abstract void closeParser() throws IOException;

    protected abstract boolean hasMoreData() throws IOException;

    protected abstract boolean parseBoolean() throws IOException;

    protected abstract int parseInt() throws IOException;

    protected abstract float parseFloat() throws IOException;

    protected abstract String parseString() throws IOException;

    protected abstract String parseVerbatimString() throws IOException;

    protected abstract Point3 parsePoint() throws IOException;

    protected abstract Vector3 parseVector() throws IOException;

    protected abstract Point2 parseTexcoord() throws IOException;

    protected abstract Matrix4 parseMatrix() throws IOException;

    protected abstract Color parseColor() throws IOException;
}