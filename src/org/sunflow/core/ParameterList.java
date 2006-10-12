package org.sunflow.core;

import java.util.HashMap;
import java.util.Map;

import org.sunflow.image.Color;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point2;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.UI;

/**
 * This class holds a list of "parameters". These are defined and then passed
 * onto rendering objects through the API. They can hold arbitrary typed and
 * named variables as a unified way of getting data into user objects.
 */
public final class ParameterList {
    private final HashMap<String, Parameter> list;
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
        list = new HashMap<String, Parameter>();
        numVerts = numFaces = numFaceVerts = 0;
    }

    /**
     * Clears the list of all its members. If some members were never used, a
     * warning will be printed to remind the user something may be wrong.
     */
    public void clear() {
        for (Map.Entry<String, Parameter> e : list.entrySet()) {
            if (!e.getValue().checked)
                UI.printWarning("[API] Unused parameter: %s", e.getKey());
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
        if (name == null || value == null)
            throw new NullPointerException();
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
        if (name == null)
            throw new NullPointerException();
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
        if (name == null)
            throw new NullPointerException();
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
        if (name == null)
            throw new NullPointerException();
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
        if (name == null || value == null)
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
        if (name == null || array == null)
            throw new NullPointerException();
        add(name, new Parameter(array));
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
        if (name == null || data == null)
            throw new NullPointerException();
        if (data.length % 3 != 0) {
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
        if (name == null || data == null)
            throw new NullPointerException();
        if (data.length % 3 != 0) {
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
        if (name == null || data == null)
            throw new NullPointerException();
        if (data.length % 2 != 0) {
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
        if (name == null || data == null)
            throw new NullPointerException();
        if (data.length % 16 != 0) {
            UI.printError("[API]] Cannot create matrix parameter %s -- invalid data length", name);
            return;
        }
        add(name, new Parameter(ParameterType.MATRIX, interp, data));
    }

    private void add(String name, Parameter param) {
        if (list.put(name, param) != null)
            UI.printWarning("[API] Parameter %s was already defined -- overwriting", name);
    }

    public String getString(String name, String defaultValue) {
        Parameter p = list.get(name);
        if (p != null && p.type == ParameterType.STRING) {
            p.checked = true;
            return p.getStringValue();
        }
        return defaultValue;
    }

    public int getInt(String name, int defaultValue) {
        Parameter p = list.get(name);
        if (p != null && p.type == ParameterType.INT && p.interp == InterpolationType.NONE && p.size() == 1) {
            p.checked = true;
            return p.getIntValue();
        }
        return defaultValue;
    }

    public int[] getIntArray(String name) {
        Parameter p = list.get(name);
        if (p != null && p.type == ParameterType.INT && p.interp == InterpolationType.NONE) {
            p.checked = true;
            return p.getInts();
        }
        return null;
    }

    public boolean getBoolean(String name, boolean defaultValue) {
        Parameter p = list.get(name);
        if (p != null && p.type == ParameterType.BOOL && p.interp == InterpolationType.NONE && p.size() == 1) {
            p.checked = true;
            return p.getBoolValue();
        }
        return defaultValue;
    }

    public float getFloat(String name, float defaultValue) {
        Parameter p = list.get(name);
        if (p != null && p.type == ParameterType.FLOAT && p.interp == InterpolationType.NONE && p.size() == 1) {
            p.checked = true;
            return p.getFloatValue();
        }
        return defaultValue;
    }

    public Color getColor(String name, Color defaultValue) {
        Parameter p = list.get(name);
        if (p != null && p.type == ParameterType.COLOR) {
            p.checked = true;
            return p.getColor();
        }
        return defaultValue;
    }

    public Point3 getPoint(String name) {
        Parameter p = list.get(name);
        if (p != null && p.type == ParameterType.POINT && p.interp == InterpolationType.NONE && p.size() == 1) {
            p.checked = true;
            return p.getPoint();
        }
        return null;
    }

    public Vector3 getVector(String name) {
        Parameter p = list.get(name);
        if (p != null && p.type == ParameterType.VECTOR && p.interp == InterpolationType.NONE && p.size() == 1) {
            p.checked = true;
            return p.getVector();
        }
        return null;
    }

    public Point2 getTexCoord(String name) {
        Parameter p = list.get(name);
        if (p != null && p.type == ParameterType.TEXCOORD && p.interp == InterpolationType.NONE && p.size() == 1) {
            p.checked = true;
            return p.getTexCoord();
        }
        return null;
    }

    public Matrix4 getMatrix(String name, Matrix4 defaultValue) {
        Parameter p = list.get(name);
        if (p != null && p.type == ParameterType.MATRIX && p.interp == InterpolationType.NONE && p.size() == 1) {
            p.checked = true;
            return p.getMatrix();
        }
        return defaultValue;
    }

    public FloatParameter getPointArray(String name) {
        Parameter p = list.get(name);
        if (p != null && p.type == ParameterType.POINT)
            return getFloatParameter(name, p);
        return null;
    }

    public FloatParameter getVectorArray(String name) {
        Parameter p = list.get(name);
        if (p != null && p.type == ParameterType.VECTOR)
            return getFloatParameter(name, p);
        return null;
    }

    public FloatParameter getTexCoordArray(String name) {
        Parameter p = list.get(name);
        if (p != null && p.type == ParameterType.TEXCOORD)
            return getFloatParameter(name, p);
        return null;
    }

    public FloatParameter getMatrixArray(String name) {
        Parameter p = list.get(name);
        if (p != null && p.type == ParameterType.MATRIX)
            return getFloatParameter(name, p);
        return null;
    }

    private FloatParameter getFloatParameter(String name, Parameter p) {
        p.checked = true;
        switch (p.interp) {
            case NONE:
                return p.getFloats();
            case VERTEX:
                if (numVerts <= 0 || p.size() == numVerts)
                    return p.getFloats();
                UI.printWarning("[API] Parameter %s expecting %d values for vertex interpolation, found %d -- ignoring", name, numVerts, p.size());
                break;
            case FACE:
                if (numFaces <= 0 || p.size() == numFaces)
                    return p.getFloats();
                UI.printWarning("[API] Parameter %s expecting %d values for face interpolation, found %d -- ignoring", name, numFaces, p.size());
                break;
            case FACEVARYING:
                if (numFaceVerts <= 0 || p.size() == numFaceVerts)
                    return p.getFloats();
                UI.printWarning("[API] Parameter %s expecting %d values for facevarying interpolation, found %d -- ignoring", name, numFaces, p.size());
                break;
            default:
        }
        return null;
    }

    public static final class FloatParameter {
        public final InterpolationType interp;
        public final float[] data;

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
        private String str;
        private Color color;
        private boolean bool;
        private boolean checked;

        private Parameter(String value) {
            type = ParameterType.STRING;
            interp = InterpolationType.NONE;
            str = value;
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
                    return 1;
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
            return String.format("%s%s[%d]", interp == InterpolationType.NONE ? "" : " " + interp.name().toLowerCase() + " ", type.name().toLowerCase(), size());
        }

        private String getStringValue() {
            return str;
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