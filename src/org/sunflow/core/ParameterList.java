package org.sunflow.core;

import org.sunflow.image.Color;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point2;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.UI;
import org.sunflow.util.FastHashMap;

/**
 * This class holds a list of "parameters". These are defined and then passed
 * onto rendering objects through the API. They can hold arbitrary typed and
 * named variables as a unified way of getting data into user objects.
 */
public final class ParameterList {
    private final FastHashMap<String, Parameter> list;
    private int numVerts, numFaces, numFaceVerts;

    private enum ParameterType {
        STRING, INT, BOOL, FLOAT, POINT, VECTOR, TEXCOORD, MATRIX, COLOR
    }

    public enum InterpolationType {
        NONE, FACE, VERTEX, FACEVARYING
    }

    /**
     * Creates an empty ParameterList.
     */
    public ParameterList() {
        list = new FastHashMap<String, Parameter>();
        numVerts = numFaces = numFaceVerts = 0;
    }

    /**
     * Clears the list of all its members. If some members were never used, a
     * warning will be printed to remind the user something may be wrong.
     */
    public void clear(boolean showUnused) {
        if (showUnused) {
            for (FastHashMap.Entry<String, Parameter> e : list) {
                if (!e.getValue().checked)
                    UI.printWarning("[API] Unused parameter: %s - %s", e.getKey(), e.getValue());
            }
        }
        list.clear();
        numVerts = numFaces = numFaceVerts = 0;
    }

    /**
     * Setup how many faces should be used to check member count on "face"
     * interpolated parameters.
     * 
     * @param numFaces number of faces
     */
    public void setFaceCount(int numFaces) {
        this.numFaces = numFaces;
    }

    /**
     * Setup how many vertices should be used to check member count of "vertex"
     * interpolated parameters.
     * 
     * @param numVerts number of vertices
     */
    public void setVertexCount(int numVerts) {
        this.numVerts = numVerts;
    }

    /**
     * Setup how many "face-vertices" should be used to check member count of
     * "facevarying" interpolated parameters. This should be equal to the sum of
     * the number of vertices on each face.
     * 
     * @param numFaceVerts number of "face-vertices"
     */
    public void setFaceVertexCount(int numFaceVerts) {
        this.numFaceVerts = numFaceVerts;
    }

    /**
     * Add the specified string as a parameter. <code>null</code> values are
     * not permitted.
     * 
     * @param name parameter name
     * @param value parameter value
     */
    public void addString(String name, String value) {
        add(name, new Parameter(value));
    }

    /**
     * Add the specified integer as a parameter. <code>null</code> values are
     * not permitted.
     * 
     * @param name parameter name
     * @param value parameter value
     */
    public void addInteger(String name, int value) {
        add(name, new Parameter(value));
    }

    /**
     * Add the specified boolean as a parameter. <code>null</code> values are
     * not permitted.
     * 
     * @param name parameter name
     * @param value parameter value
     */
    public void addBoolean(String name, boolean value) {
        add(name, new Parameter(value));
    }

    /**
     * Add the specified float as a parameter. <code>null</code> values are
     * not permitted.
     * 
     * @param name parameter name
     * @param value parameter value
     */
    public void addFloat(String name, float value) {
        add(name, new Parameter(value));
    }

    /**
     * Add the specified color as a parameter. <code>null</code> values are
     * not permitted.
     * 
     * @param name parameter name
     * @param value parameter value
     */
    public void addColor(String name, Color value) {
        if (value == null)
            throw new NullPointerException();
        add(name, new Parameter(value));
    }

    /**
     * Add the specified array of integers as a parameter. <code>null</code>
     * values are not permitted.
     * 
     * @param name parameter name
     * @param value parameter value
     */
    public void addIntegerArray(String name, int[] array) {
        if (array == null)
            throw new NullPointerException();
        add(name, new Parameter(array));
    }

    /**
     * Add the specified array of integers as a parameter. <code>null</code>
     * values are not permitted.
     * 
     * @param name parameter name
     * @param value parameter value
     */
    public void addStringArray(String name, String[] array) {
        if (array == null)
            throw new NullPointerException();
        add(name, new Parameter(array));
    }

    /**
     * Add the specified floats as a parameter. <code>null</code> values are
     * not permitted.
     * 
     * @param name parameter name
     * @param interp interpolation type
     * @param value parameter value
     */
    public void addFloats(String name, InterpolationType interp, float[] data) {
        if (data == null) {
            UI.printError("[API]] Cannot create float parameter %s -- invalid data length", name);
            return;
        }
        add(name, new Parameter(ParameterType.FLOAT, interp, data));
    }

    /**
     * Add the specified points as a parameter. <code>null</code> values are
     * not permitted.
     * 
     * @param name parameter name
     * @param interp interpolation type
     * @param value parameter value
     */
    public void addPoints(String name, InterpolationType interp, float[] data) {
        if (data == null || data.length % 3 != 0) {
            UI.printError("[API]] Cannot create point parameter %s -- invalid data length", name);
            return;
        }
        add(name, new Parameter(ParameterType.POINT, interp, data));
    }

    /**
     * Add the specified vectors as a parameter. <code>null</code> values are
     * not permitted.
     * 
     * @param name parameter name
     * @param interp interpolation type
     * @param value parameter value
     */

    public void addVectors(String name, InterpolationType interp, float[] data) {
        if (data == null || data.length % 3 != 0) {
            UI.printError("[API]] Cannot create vector parameter %s -- invalid data length", name);
            return;
        }
        add(name, new Parameter(ParameterType.VECTOR, interp, data));
    }

    /**
     * Add the specified texture coordinates as a parameter. <code>null</code>
     * values are not permitted.
     * 
     * @param name parameter name
     * @param interp interpolation type
     * @param value parameter value
     */
    public void addTexCoords(String name, InterpolationType interp, float[] data) {
        if (data == null || data.length % 2 != 0) {
            UI.printError("[API]] Cannot create texcoord parameter %s -- invalid data length", name);
            return;
        }
        add(name, new Parameter(ParameterType.TEXCOORD, interp, data));
    }

    /**
     * Add the specified matrices as a parameter. <code>null</code> values are
     * not permitted.
     * 
     * @param name parameter name
     * @param interp interpolation type
     * @param value parameter value
     */
    public void addMatrices(String name, InterpolationType interp, float[] data) {
        if (data == null || data.length % 16 != 0) {
            UI.printError("[API]] Cannot create matrix parameter %s -- invalid data length", name);
            return;
        }
        add(name, new Parameter(ParameterType.MATRIX, interp, data));
    }

    private void add(String name, Parameter param) {
        if (name == null)
            UI.printError("[API] Cannot declare parameter with null name");
        else if (list.put(name, param) != null)
            UI.printWarning("[API] Parameter %s was already defined -- overwriting", name);
    }

    public String getString(String name, String defaultValue) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.STRING, InterpolationType.NONE, 1, p))
            return p.getStringValue();
        return defaultValue;
    }

    public String[] getStringArray(String name, String[] defaultValue) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.STRING, InterpolationType.NONE, -1, p))
            return p.getStrings();
        return defaultValue;
    }

    public int getInt(String name, int defaultValue) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.INT, InterpolationType.NONE, 1, p))
            return p.getIntValue();
        return defaultValue;
    }

    public int[] getIntArray(String name) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.INT, InterpolationType.NONE, -1, p))
            return p.getInts();
        return null;
    }

    public boolean getBoolean(String name, boolean defaultValue) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.BOOL, InterpolationType.NONE, 1, p))
            return p.getBoolValue();
        return defaultValue;
    }

    public float getFloat(String name, float defaultValue) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.FLOAT, InterpolationType.NONE, 1, p))
            return p.getFloatValue();
        return defaultValue;
    }

    public Color getColor(String name, Color defaultValue) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.COLOR, InterpolationType.NONE, 1, p))
            return p.getColor();
        return defaultValue;
    }

    public Point3 getPoint(String name, Point3 defaultValue) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.POINT, InterpolationType.NONE, 1, p))
            return p.getPoint();
        return defaultValue;
    }

    public Vector3 getVector(String name, Vector3 defaultValue) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.VECTOR, InterpolationType.NONE, 1, p))
            return p.getVector();
        return defaultValue;
    }

    public Point2 getTexCoord(String name, Point2 defaultValue) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.TEXCOORD, InterpolationType.NONE, 1, p))
            return p.getTexCoord();
        return defaultValue;
    }

    public Matrix4 getMatrix(String name, Matrix4 defaultValue) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.MATRIX, InterpolationType.NONE, 1, p))
            return p.getMatrix();
        return defaultValue;
    }

    public FloatParameter getFloatArray(String name) {
        return getFloatParameter(name, ParameterType.FLOAT, list.get(name));
    }

    public FloatParameter getPointArray(String name) {
        return getFloatParameter(name, ParameterType.POINT, list.get(name));
    }

    public FloatParameter getVectorArray(String name) {
        return getFloatParameter(name, ParameterType.VECTOR, list.get(name));
    }

    public FloatParameter getTexCoordArray(String name) {
        return getFloatParameter(name, ParameterType.TEXCOORD, list.get(name));
    }

    public FloatParameter getMatrixArray(String name) {
        return getFloatParameter(name, ParameterType.MATRIX, list.get(name));
    }

    private boolean isValidParameter(String name, ParameterType type, InterpolationType interp, int requestedSize, Parameter p) {
        if (p == null)
            return false;
        if (p.type != type) {
            UI.printWarning("[API] Parameter %s requested as a %s - declared as %s", name, type.name().toLowerCase(), p.type.name().toLowerCase());
            return false;
        }
        if (p.interp != interp) {
            UI.printWarning("[API] Parameter %s requested as a %s - declared as %s", name, interp.name().toLowerCase(), p.interp.name().toLowerCase());
            return false;
        }
        if (requestedSize > 0 && p.size() != requestedSize) {
            UI.printWarning("[API] Parameter %s requires %d %s - declared with %d", name, requestedSize, requestedSize == 1 ? "value" : "values", p.size());
            return false;
        }
        p.checked = true;
        return true;
    }

    private FloatParameter getFloatParameter(String name, ParameterType type, Parameter p) {
        if (p == null)
            return null;
        switch (p.interp) {
            case NONE:
                if (!isValidParameter(name, type, p.interp, -1, p))
                    return null;
                break;
            case VERTEX:
                if (!isValidParameter(name, type, p.interp, numVerts, p))
                    return null;
                break;
            case FACE:
                if (!isValidParameter(name, type, p.interp, numFaces, p))
                    return null;
                break;
            case FACEVARYING:
                if (!isValidParameter(name, type, p.interp, numFaceVerts, p))
                    return null;
                break;
            default:
                return null;
        }
        return p.getFloats();
    }

    public static final class FloatParameter {
        public final InterpolationType interp;
        public final float[] data;

        public FloatParameter() {
            this(InterpolationType.NONE, null);
        }

        public FloatParameter(float f) {
            this(InterpolationType.NONE, new float[] { f });
        }

        private FloatParameter(InterpolationType interp, float[] data) {
            this.interp = interp;
            this.data = data;
        }
    }

    private static final class Parameter {
        private ParameterType type;
        private InterpolationType interp;
        private float[] floats;
        private int[] ints;
        private String[] strs;
        private Color color;
        private boolean bool;
        private boolean checked;

        private Parameter(String value) {
            type = ParameterType.STRING;
            interp = InterpolationType.NONE;
            strs = new String[] { value };
            checked = false;
        }

        private Parameter(int value) {
            type = ParameterType.INT;
            interp = InterpolationType.NONE;
            ints = new int[] { value };
            checked = false;
        }

        private Parameter(boolean value) {
            type = ParameterType.BOOL;
            interp = InterpolationType.NONE;
            bool = value;
            checked = false;
        }

        private Parameter(float value) {
            type = ParameterType.FLOAT;
            interp = InterpolationType.NONE;
            floats = new float[] { value };
            checked = false;
        }

        private Parameter(int[] array) {
            type = ParameterType.INT;
            interp = InterpolationType.NONE;
            ints = array;
            checked = false;
        }

        private Parameter(String[] array) {
            type = ParameterType.STRING;
            interp = InterpolationType.NONE;
            strs = array;
            checked = false;
        }

        private Parameter(Color c) {
            type = ParameterType.COLOR;
            interp = InterpolationType.NONE;
            color = c;
            checked = false;
        }

        private Parameter(ParameterType type, InterpolationType interp, float[] data) {
            this.type = type;
            this.interp = interp;
            floats = data;
            checked = false;
        }

        private int size() {
            // number of elements
            switch (type) {
                case STRING:
                    return strs.length;
                case INT:
                    return ints.length;
                case BOOL:
                    return 1;
                case FLOAT:
                    return floats.length;
                case POINT:
                    return floats.length / 3;
                case VECTOR:
                    return floats.length / 3;
                case TEXCOORD:
                    return floats.length / 2;
                case MATRIX:
                    return floats.length / 16;
                case COLOR:
                    return 1;
                default:
                    return -1;
            }
        }

        public String toString() {
            return String.format("%s%s[%d]", interp == InterpolationType.NONE ? "" : interp.name().toLowerCase() + " ", type.name().toLowerCase(), size());
        }

        private String getStringValue() {
            return strs[0];
        }

        private boolean getBoolValue() {
            return bool;
        }

        private int getIntValue() {
            return ints[0];
        }

        private int[] getInts() {
            return ints;
        }

        private String[] getStrings() {
            return strs;
        }

        private float getFloatValue() {
            return floats[0];
        }

        private FloatParameter getFloats() {
            return new FloatParameter(interp, floats);
        }

        private Point3 getPoint() {
            return new Point3(floats[0], floats[1], floats[2]);
        }

        private Vector3 getVector() {
            return new Vector3(floats[0], floats[1], floats[2]);
        }

        private Point2 getTexCoord() {
            return new Point2(floats[0], floats[1]);
        }

        private Matrix4 getMatrix() {
            return new Matrix4(floats, true);
        }

        private Color getColor() {
            return color;
        }
    }
}