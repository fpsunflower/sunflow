package org.sunflow.core;

import org.sunflow.SunflowAPI;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class Instance implements RenderObject {
    private Matrix4 o2w;
    private Matrix4 w2o;
    private BoundingBox bounds;
    private Geometry geometry;
    private Shader[] shaders;

    public boolean update(ParameterList pl, SunflowAPI api) {
        String geometryName = pl.getString("geometry", null);
        if (geometry == null || geometryName != null) {
            if (geometryName == null) {
                UI.printError(Module.GEOM, "geometry parameter missing - unable to create instance");
                return false;
            }
            geometry = api.lookupGeometry(geometryName);
            if (geometry == null) {
                UI.printError(Module.GEOM, "Geometry \"%s\" was not declared yet - instance is invalid", geometryName);
                return false;
            }
        }
        String[] shaderNames = pl.getStringArray("shaders", null);
        if (shaderNames != null) {
            // new shader names have been provided
            shaders = new Shader[shaderNames.length];
            for (int i = 0; i < shaders.length; i++) {
                shaders[i] = api.lookupShader(shaderNames[i]);
                if (shaders[i] == null)
                    UI.printWarning(Module.GEOM, "Shader \"%s\" was not declared yet - ignoring", shaders[i]);
            }
        } else {
            // re-use existing shader array
        }
        Matrix4 transform = pl.getMatrix("transform", o2w);
        if (transform != o2w) {
            o2w = transform;
            if (o2w != null) {
                w2o = o2w.inverse();
                if (w2o == null) {
                    UI.printError(Module.GEOM, "Unable to compute transform inverse");
                    return false;
                }
            } else
                o2w = w2o = null;
        }
        return true;
    }

    public void updateBounds() {
        bounds = geometry.getWorldBounds(o2w);
    }
    
    public boolean hasGeometry(Geometry g) {
        return geometry == g;
    }

    public void removeShader(Shader s) {
        if (shaders != null) {
            for (int i = 0; i < shaders.length; i++)
                if (shaders[i] == s)
                    shaders[i] = null;
        }
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    public int getNumPrimitives() {
        return geometry.getNumPrimitives();
    }

    public void intersect(Ray r, IntersectionState state) {
        Ray localRay = r.transform(w2o);
        state.current = this;
        geometry.intersect(localRay, state);
        // FIXME: transfer max distance to current ray
        r.setMax(localRay.getMax());
    }

    public void prepareShadingState(ShadingState state) {
        geometry.prepareShadingState(state);
    }

    public Shader getShader(int i) {
        if (shaders == null || i < 0 || i >= shaders.length)
            return null;
        return shaders[i];
    }

    public Point3 transformObjectToWorld(Point3 p) {
        return o2w == null ? new Point3(p) : o2w.transformP(p);
    }

    public Point3 transformWorldToObject(Point3 p) {
        return o2w == null ? new Point3(p) : w2o.transformP(p);
    }

    public Vector3 transformNormalObjectToWorld(Vector3 n) {
        return o2w == null ? new Vector3(n) : w2o.transformTransposeV(n);
    }

    public Vector3 transformNormalWorldToObject(Vector3 n) {
        return o2w == null ? new Vector3(n) : o2w.transformTransposeV(n);
    }

    public Vector3 transformVectorObjectToWorld(Vector3 v) {
        return o2w == null ? new Vector3(v) : o2w.transformV(v);
    }

    public Vector3 transformVectorWorldToObject(Vector3 v) {
        return o2w == null ? new Vector3(v) : w2o.transformV(v);
    }
}