package org.sunflow;

import java.util.HashMap;

import org.sunflow.core.Display;
import org.sunflow.core.ParameterList.InterpolationType;
import org.sunflow.core.parser.SCAbstractParser.Keyword;
import org.sunflow.image.ColorFactory;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point2;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public abstract class FileSunflowAPI implements SunflowAPIInterface {
    private int frame;
    private HashMap<String, Integer> objectNames;

    protected FileSunflowAPI() {
        frame = 1;
        objectNames = new HashMap<String, Integer>();
        reset();
    }

    public void camera(String name, String lensType) {
        writeKeyword(Keyword.CAMERA);
        writeString(name);
        writeString(lensType);
        writeNewline(0);
        writeNewline(0);
    }

    public void geometry(String name, String typeName) {
        writeKeyword(Keyword.GEOMETRY);
        writeString(name);
        writeString(typeName);
        writeNewline(0);
        writeNewline(0);
    }

    public int getCurrentFrame() {
        return frame;
    }

    public String getUniqueName(String prefix) {
        int index = 1;
        Integer value = objectNames.get(prefix);
        if (value != null) {
            index = value;
            objectNames.put(prefix, index + 1);
        } else {
            objectNames.put(prefix, index + 1);
        }
        return String.format("%s_%d", prefix, index);
    }

    public void instance(String name, String geoname) {
        writeKeyword(Keyword.INSTANCE);
        writeString(name);
        writeString(geoname);
        writeNewline(0);
        writeNewline(0);
    }

    public void light(String name, String lightType) {
        writeKeyword(Keyword.LIGHT);
        writeString(name);
        writeString(lightType);
        writeNewline(0);
        writeNewline(0);
    }

    public void modifier(String name, String modifierType) {
        writeKeyword(Keyword.MODIFIER);
        writeString(name);
        writeString(modifierType);
        writeNewline(0);
        writeNewline(0);
    }

    public void options(String name) {
        writeKeyword(Keyword.OPTIONS);
        writeString(name);
        writeNewline(0);
        writeNewline(0);
    }

    public void parameter(String name, String value) {
        writeKeyword(Keyword.PARAMETER);
        writeString(name);
        writeKeyword(Keyword.STRING);
        writeString(value);
        writeNewline(0);
    }

    public void parameter(String name, boolean value) {
        writeKeyword(Keyword.PARAMETER);
        writeString(name);
        writeKeyword(Keyword.BOOL);
        writeBoolean(value);
        writeNewline(0);
    }

    public void parameter(String name, int value) {
        writeKeyword(Keyword.PARAMETER);
        writeString(name);
        writeKeyword(Keyword.INT);
        writeInt(value);
        writeNewline(0);
    }

    public void parameter(String name, float value) {
        writeKeyword(Keyword.PARAMETER);
        writeString(name);
        writeKeyword(Keyword.FLOAT);
        writeFloat(value);
        writeNewline(0);
    }

    public void parameter(String name, String colorspace, float... data) {
        writeKeyword(Keyword.PARAMETER);
        writeString(name);
        writeKeyword(Keyword.COLOR);
        if (colorspace == null)
            writeString(colorspace = ColorFactory.getInternalColorspace());
        else
            writeString(colorspace);
        if (ColorFactory.getRequiredDataValues(colorspace) == -1)
            writeInt(data.length);
        int idx = 0;
        int step = 9;
        for (float f : data) {
            if (data.length > step && idx % step == 0)
                writeNewline(1);
            writeFloat(f);
            idx++;
        }
        writeNewline(0);
    }

    public void parameter(String name, Point3 value) {
        writeKeyword(Keyword.PARAMETER);
        writeString(name);
        writeKeyword(Keyword.POINT);
        writeFloat(value.x);
        writeFloat(value.y);
        writeFloat(value.z);
        writeNewline(0);
    }

    public void parameter(String name, Vector3 value) {
        writeKeyword(Keyword.PARAMETER);
        writeString(name);
        writeKeyword(Keyword.VECTOR);
        writeFloat(value.x);
        writeFloat(value.y);
        writeFloat(value.z);
        writeNewline(0);
    }

    public void parameter(String name, Point2 value) {
        writeKeyword(Keyword.PARAMETER);
        writeString(name);
        writeKeyword(Keyword.TEXCOORD);
        writeFloat(value.x);
        writeFloat(value.y);
        writeNewline(0);
    }

    public void parameter(String name, Matrix4 value) {
        writeKeyword(Keyword.PARAMETER);
        writeString(name);
        writeKeyword(Keyword.MATRIX);
        writeMatrix(value);
        writeNewline(0);
    }

    public void parameter(String name, int[] value) {
        writeKeyword(Keyword.PARAMETER);
        writeString(name);
        writeKeyword(Keyword.INT_ARRAY);
        writeInt(value.length);
        int idx = 0;
        int step = 9;
        for (int v : value) {
            if (idx % step == 0)
                writeNewline(1);
            writeInt(v);
            idx++;
        }
        writeNewline(0);
    }

    public void parameter(String name, String[] value) {
        writeKeyword(Keyword.PARAMETER);
        writeString(name);
        writeKeyword(Keyword.STRING_ARRAY);
        writeInt(value.length);
        for (String v : value) {
            writeNewline(1);
            writeString(v);
        }
        writeNewline(0);
    }

    public void parameter(String name, String type, String interpolation, float[] data) {
        InterpolationType interp;
        try {
            interp = InterpolationType.valueOf(interpolation.toUpperCase());
        } catch (IllegalArgumentException e) {
            UI.printError(Module.API, "Unknown interpolation type: %s -- ignoring parameter \"%s\"", interpolation, name);
            return;
        }
        Keyword typeKeyword;
        int lengthFactor;
        if (type.equals("float")) {
            typeKeyword = Keyword.FLOAT_ARRAY;
            lengthFactor = 1;
        } else if (type.equals("point")) {
            typeKeyword = Keyword.POINT_ARRAY;
            lengthFactor = 3;
        } else if (type.equals("vector")) {
            typeKeyword = Keyword.VECTOR_ARRAY;
            lengthFactor = 3;
        } else if (type.equals("texcoord")) {
            typeKeyword = Keyword.TEXCOORD_ARRAY;
            lengthFactor = 2;
        } else if (type.equals("matrix")) {
            typeKeyword = Keyword.MATRIX_ARRAY;
            lengthFactor = 16;
        } else {
            UI.printError(Module.API, "Unknown parameter type: %s -- ignoring parameter \"%s\"", type, name);
            return;
        }
        writeKeyword(Keyword.PARAMETER);

        writeString(name);
        writeKeyword(typeKeyword);
        writeInterpolationType(interp);
        writeInt(data.length / lengthFactor);
        int idx = 0;
        if (data.length > 16)
            lengthFactor *= 8;
        for (float v : data) {
            if (lengthFactor > 1 && idx % lengthFactor == 0)
                writeNewline(1);
            writeFloat(v);
            idx++;
        }
        writeNewline(0);
    }

    public boolean parse(String filename) {
        writeKeyword(Keyword.INCLUDE);
        writeString(filename);
        writeNewline(0);
        writeNewline(0);
        return true;
    }

    public void plugin(String type, String name, String code) {
        writeKeyword(Keyword.PLUGIN);
        writeString(type);
        writeString(name);
        writeVerbatimString(code);
        writeNewline(0);
        writeNewline(0);
    }

    public void remove(String name) {
        writeKeyword(Keyword.REMOVE);
        writeString(name);
        writeNewline(0);
        writeNewline(0);
    }

    public void render(String optionsName, Display display) {
        UI.printWarning(Module.API, "Unable to render file stream");
    }

    public void reset() {
        frame = 1;
        objectNames.clear();
    }

    public void searchpath(String type, String path) {
        writeKeyword(Keyword.SEARCHPATH);
        writeString(type);
        writeString(path);
        writeNewline(0);
        writeNewline(0);

    }

    public void setCurrentFrame(int currentFrame) {
        writeKeyword(Keyword.FRAME);
        writeInt(frame = currentFrame);
        writeNewline(0);
        writeNewline(0);
    }

    public void shader(String name, String shaderType) {
        writeKeyword(Keyword.SHADER);
        writeString(name);
        writeString(shaderType);
        writeNewline(0);
        writeNewline(0);
    }

    protected abstract void writeKeyword(Keyword keyword);

    protected abstract void writeInterpolationType(InterpolationType interp);

    protected abstract void writeBoolean(boolean value);

    protected abstract void writeInt(int value);

    protected abstract void writeFloat(float value);

    protected abstract void writeString(String string);

    protected abstract void writeVerbatimString(String string);

    protected abstract void writeMatrix(Matrix4 value);

    protected abstract void writeNewline(int indentNext);
}