package org.sunflow.core;

import org.sunflow.SunflowAPI;
import org.sunflow.math.Matrix4;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class Camera implements RenderObject {
    private final CameraLens lens;
    private Matrix4 c2w;

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
}