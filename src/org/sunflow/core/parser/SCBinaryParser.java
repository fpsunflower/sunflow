package org.sunflow.core.parser;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
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
import org.sunflow.system.UI.Module;

public class SCBinaryParser implements SceneParser {
    private DataInputStream stream;
    private String current;

    public boolean parse(String filename, SunflowAPI api) {
        Timer timer = new Timer();
        timer.start();
        UI.printInfo(Module.API, "Parsing \"%s\" ...", filename);
        try {
            stream = new DataInputStream(new FileInputStream(filename));
            while (true) {
                if (stream.available() == 0)
                    break;
                int code = getNextCode();
                switch (code) {
                    case 'p': {
                        parseParameter(api);
                        break;
                    }
                    case 'g': {
                        String name = getNextString();
                        String type = getNextString();
                        if (type.equals("incremental"))
                            type = null;
                        api.geometry(name, type);
                        break;
                    }
                    case 'i': {
                        String name = getNextString();
                        String geoname = getNextString();
                        if (geoname.equals("incremental"))
                            geoname = null;
                        api.instance(name, geoname);
                        break;
                    }
                    case 's': {
                        String name = getNextString();
                        String type = getNextString();
                        if (type.equals("incremental"))
                            type = null;
                        api.shader(name, type);
                        break;
                    }
                    case 'm': {
                        String name = getNextString();
                        String type = getNextString();
                        if (type.equals("incremental"))
                            type = null;
                        api.modifier(name, type);
                        break;
                    }
                    case 'l': {
                        String name = getNextString();
                        String type = getNextString();
                        if (type.equals("incremental"))
                            type = null;
                        api.light(name, type);
                        break;
                    }
                    case 'c': {
                        String name = getNextString();
                        String type = getNextString();
                        if (type.equals("incremental"))
                            type = null;
                        api.camera(name, type);
                        break;
                    }
                    case 'o': {
                        api.options(getNextString());
                        break;
                    }
                    case 'x': {
                        // extended syntax
                        code = getNextCode();
                        switch (code) {
                            case 'i': {
                                String file = getNextString();
                                UI.printInfo(Module.API, "Including: \"%s\" ...", file);
                                api.parse(file);
                                break;
                            }
                            case 'p': {
                                // plugin
                                parsePlugin();
                                break;
                            }
                            case 's': {
                                code = getNextCode();
                                switch (code) {
                                    case 'i': {
                                        api.addIncludeSearchPath(getNextString());
                                        break;
                                    }
                                    case 't': {
                                        api.addTextureSearchPath(getNextString());
                                        break;
                                    }
                                    default:
                                        throw new UnknownBinaryCodeException(code, "searchpath code decoder");
                                }
                                break;
                            }
                            default: {
                                throw new UnknownBinaryCodeException(code, "extended code decoder");
                            }
                        }
                    }
                    default: {
                        throw new UnknownBinaryCodeException(code, "main code decoder");
                    }
                }
            }
            stream.close();
        } catch (EOFException e) {
            UI.printError(Module.API, "%s", e.getMessage());
            return false;
        } catch (FileNotFoundException e) {
            UI.printError(Module.API, "%s", e.getMessage());
            return false;
        } catch (IOException e) {
            UI.printError(Module.API, "%s", e.getMessage());
            return false;
        } catch (UnknownBinaryCodeException e) {
            UI.printError(Module.API, "%s", e.getMessage());
            return false;
        }
        timer.end();
        UI.printInfo(Module.API, "Done parsing (took %s)", timer.toString());
        return true;
    }

    private void parseParameter(SunflowAPI api) throws IOException, UnknownBinaryCodeException {
        String name = getNextString();
        if (peekTokens("string", "str", "s"))
            api.parameter(name, getNextString());
        else if (peekTokens("boolean", "bool", "b"))
            api.parameter(name, getNextBoolean());
        else if (peekTokens("integer", "int", "i"))
            api.parameter(name, getNextInt());
        else if (peekTokens("float", "flt", "f"))
            api.parameter(name, getNextFloat());
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
            int n = getNextInt();
            api.parameter(name, parseStringArray(n));
        } else if (peekTokens("integer[]", "int[]", "i[]")) {
            int n = getNextInt();
            api.parameter(name, parseIntArray(n));
        } else if (peekTokens("float[]", "flt[]", "f[]")) {
            int n = getNextInt();
            api.parameter(name, "float", parseInterpolationType(), parseFloatArray(n));
        } else if (peekTokens("point[]", "pnt[]", "p[]")) {
            int n = getNextInt();
            api.parameter(name, "point", parseInterpolationType(), parseFloatArray(3 * n));
        } else if (peekTokens("vector[]", "vec[]", "v[]")) {
            int n = getNextInt();
            api.parameter(name, "vector", parseInterpolationType(), parseFloatArray(3 * n));
        } else if (peekTokens("texcoord[]", "tex[]", "t[]")) {
            int n = getNextInt();
            api.parameter(name, "texcoord", parseInterpolationType(), parseFloatArray(2 * n));
        } else if (peekTokens("matrix[]", "mat[]", "m[]")) {
            int n = getNextInt();
            api.parameter(name, "matrix", parseInterpolationType(), parseFloatArray(16 * n));
        } else {
            // bad parameter type - warn and ignore
            UI.printWarning(Module.API, "Unknown parameter type \"%s\" - ignoring");
        }
    }

    private void parsePlugin() throws IOException {
        String type = getNextString();
        String name = getNextString();
        String code = getNextString();
        UI.printInfo(Module.API, "Reading custom %s plugin \"%s\" ...", type, name);
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

    private String getNextString() throws IOException {
        byte[] b = new byte[stream.readInt()];
        stream.read(b);
        return new String(b, "UTF-8");
    }

    private int getNextCode() throws IOException {
        return stream.readUnsignedByte(); // an 8-bit code
    }

    private boolean getNextBoolean() throws IOException {
        int code = getNextCode();
        switch (code) {
            case 't':
                return true;
            case 'f':
                return false;
            default:
                UI.printWarning(Module.API, "Invalid boolean code %c - you must use 't' or 'f' - defaulting to false", code);
                return false;
        }
    }

    private int getNextInt() throws IOException {
        return stream.readInt();
    }

    private float getNextFloat() throws IOException {
        return stream.readFloat();
    }

    private boolean peekTokens(String token, String... tokens) throws IOException {
        if (current == null)
            current = getNextString();
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

    private String parseInterpolationType() throws IOException, UnknownBinaryCodeException {
        int code = getNextCode();
        switch (code) {
            case 'v':
                return "vertex";
            case 'f':
                return "facevarying";
            case 'n':
                return "none";
            default:
                throw new UnknownBinaryCodeException(code, "interpolation type decoder");
        }
    }

    private Color parseColor() throws IOException {
        Color c = null;
        if (peekTokens("sRGB nonlinear")) {
            float r = getNextFloat();
            float g = getNextFloat();
            float b = getNextFloat();
            c = new Color(r, g, b);
            c.toLinear();
        } else if (peekTokens("sRGB linear")) {
            float r = getNextFloat();
            float g = getNextFloat();
            float b = getNextFloat();
            c = new Color(r, g, b);
        } else
            UI.printWarning(Module.API, "Unrecognized color space: %s", current);
        return c;
    }

    private Point3 parsePoint() throws IOException {
        float x = getNextFloat();
        float y = getNextFloat();
        float z = getNextFloat();
        return new Point3(x, y, z);
    }

    private Vector3 parseVector() throws IOException {
        float x = getNextFloat();
        float y = getNextFloat();
        float z = getNextFloat();
        return new Vector3(x, y, z);
    }

    private Point2 parseTexcoord() throws IOException {
        float x = getNextFloat();
        float y = getNextFloat();
        return new Point2(x, y);
    }

    private Matrix4 parseMatrix() throws IOException {
        return new Matrix4(parseFloatArray(16), true);
    }

    private String[] parseStringArray(int size) throws IOException {
        String[] data = new String[size];
        for (int i = 0; i < size; i++)
            data[i] = getNextString();
        return data;
    }

    private int[] parseIntArray(int size) throws IOException {
        int[] data = new int[size];
        for (int i = 0; i < size; i++)
            data[i] = getNextInt();
        return data;
    }

    private float[] parseFloatArray(int size) throws IOException {
        float[] data = new float[size];
        for (int i = 0; i < size; i++)
            data[i] = getNextFloat();
        return data;
    }

    @SuppressWarnings("serial")
    private static class UnknownBinaryCodeException extends Exception {
        private UnknownBinaryCodeException(int code, String area) {
            super(String.format("Unknown code %d (='%c') in %s", code, code, area));
        }
    }
}