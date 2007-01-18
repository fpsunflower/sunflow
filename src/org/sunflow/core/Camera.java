package org.sunflow.core;

import org.sunflow.SunflowAPI;
import org.sunflow.math.Matrix4;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class Camera implements RenderObject {
    private final CameraLens lens;
    private Matrix4 c2w;
    private Matrix4 w2c;

    public Camera(CameraLens lens) {
        this.lens = lens;
        c2w = null;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        Matrix4 transform = pl.getMatrix("transform", null);
        if (transform == null) {
            // no transform was specified, check eye/target/up
            Point3 eye = pl.getPoint("eye", null);
            Point3 target = pl.getPoint("target", null);
            Vector3 up = pl.getVector("up", null);
            if (eye != null && target != null && up != null) {
                c2w = Matrix4.fromBasis(OrthoNormalBasis.makeFromWV(Point3.sub(eye, target, new Vector3()), up));
                c2w = Matrix4.translation(eye.x, eye.y, eye.z).multiply(c2w);
            }
        } else
            c2w = transform;
        if (c2w != null) {
            w2c = c2w.inverse();
            if (w2c == null) {
                UI.printError(Module.CAM, "Camera matrix is not invertible");
                return false;
            }
        } else
            w2c = null;
        return lens.update(pl, api);
    }

    public Ray getRay(float x, float y, int imageWidth, int imageHeight, double lensX, double lensY, double time) {
        Ray r = lens.getRay(x, y, imageWidth, imageHeight, lensX, lensY, time);
        if (r != null && c2w != null) {
            r = r.transform(c2w);
            // renormalize to account for scale factors embeded in the transform
            r.normalize();
        }
        return r;
    }

    /**
     * Generate a ray from the origin of camera space toward the specified point
     * 
     * @param p point in world space
     * @return ray from the origin of camera space to the specified point
     */
    Ray getRay(Point3 p) {
        return new Ray(c2w.transformP(new Point3(0, 0, 0)), p);
    }

    Matrix4 getCameraToWorld() {
        return c2w == null ? Matrix4.IDENTITY : c2w;
    }

    Matrix4 getWorldToCamera() {
        return w2c == null ? Matrix4.IDENTITY : w2c;
    }
}