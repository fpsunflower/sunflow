package org.sunflow.core.parser;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import org.sunflow.core.ParameterList.InterpolationType;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point2;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class SCBinaryParser extends SCAbstractParser {
    private DataInputStream stream;

    protected void closeParser() throws IOException {
        stream.close();
    }

    protected boolean hasMoreData() throws IOException {
        return stream.available() > 0;
    }

    protected void openParser(String filename) throws IOException {
        stream = new DataInputStream(new FileInputStream(filename));
    }

    protected boolean parseBoolean() throws IOException {
        return stream.readUnsignedByte() != 0;
    }

    protected float parseFloat() throws IOException {
        return stream.readFloat();
    }

    protected int parseInt() throws IOException {
        return stream.readInt();
    }

    protected Matrix4 parseMatrix() throws IOException {
        return new Matrix4(parseFloatArray(16), true);
    }

    protected Point3 parsePoint() throws IOException {
        float x = parseFloat();
        float y = parseFloat();
        float z = parseFloat();
        return new Point3(x, y, z);
    }

    protected String parseString() throws IOException {
        byte[] b = new byte[stream.readInt()];
        stream.read(b);
        return new String(b, "UTF-8");
    }

    protected Point2 parseTexcoord() throws IOException {
        float x = parseFloat();
        float y = parseFloat();
        return new Point2(x, y);
    }

    protected Vector3 parseVector() throws IOException {
        float x = parseFloat();
        float y = parseFloat();
        float z = parseFloat();
        return new Vector3(x, y, z);
    }

    protected String parseVerbatimString() throws IOException {
        return parseString();
    }

    protected InterpolationType parseInterpolationType() throws IOException {
        int c;
        switch (c = stream.readUnsignedByte()) {
            case 'n':
                return InterpolationType.NONE;
            case 'v':
                return InterpolationType.VERTEX;
            case 'f':
                return InterpolationType.FACEVARYING;
            case 'p':
                return InterpolationType.FACE;
            default:
                UI.printWarning(Module.API, "Unknown byte found for interpolation type %c", (char) c);
                return InterpolationType.NONE;
        }
    }

    protected Keyword parseKeyword() throws IOException {
        int code = stream.read(); // read a single byte - allow for EOF (<0)
        switch (code) {
            case 'p':
                return Keyword.PARAMETER;
            case 'g':
                return Keyword.GEOMETRY;
            case 'i':
                return Keyword.INSTANCE;
            case 's':
                return Keyword.SHADER;
            case 'm':
                return Keyword.MODIFIER;
            case 'l':
                return Keyword.LIGHT;
            case 'c':
                return Keyword.CAMERA;
            case 'o':
                return Keyword.OPTIONS;
            case 'x': {
                // extended keywords (less frequent)
                // note we don't use stream.read() here because we should throw
                // an exception if the end of the file is reached
                switch (stream.readUnsignedByte()) {
                    case 'i':
                        return Keyword.INCLUDE;
                    case 'p':
                        return Keyword.PLUGIN;
                    case 's':
                        return Keyword.SEARCHPATH;
                    default:
                        return null;
                }
            }
            case 't': {
                // data types
                // note we don't use stream.read() here because we should throw
                // an exception if the end of the file is reached
                int type = stream.readUnsignedByte();
                // note that while not all types can be arrays at the moment, we
                // always parse this boolean flag to keep the syntax consistent
                // and allow for future improvements
                boolean isArray = parseBoolean();
                switch (type) {
                    case 's':
                        return isArray ? Keyword.STRING_ARRAY : Keyword.STRING;
                    case 'b':
                        return Keyword.BOOL;
                    case 'i':
                        return isArray ? Keyword.INT_ARRAY : Keyword.INT;
                    case 'f':
                        return isArray ? Keyword.FLOAT_ARRAY : Keyword.FLOAT;
                    case 'c':
                        return Keyword.COLOR;
                    case 'p':
                        return isArray ? Keyword.POINT_ARRAY : Keyword.POINT;
                    case 'v':
                        return isArray ? Keyword.VECTOR_ARRAY : Keyword.VECTOR;
                    case 't':
                        return isArray ? Keyword.TEXCOORD_ARRAY : Keyword.TEXCOORD;
                    case 'm':
                        return isArray ? Keyword.MATRIX_ARRAY : Keyword.MATRIX;
                    default:
                        return null;
                }
            }
            default:
                if (code < 0)
                    return Keyword.END_OF_FILE;
        }
        return null;
    }
}