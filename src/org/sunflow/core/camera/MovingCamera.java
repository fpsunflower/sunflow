package org.sunflow.core.camera;

import org.sunflow.core.CameraLens;
import org.sunflow.core.Ray;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public abstract class MovingCamera implements CameraLens {
    private float au, av;

    protected MovingCamera(float fov, float aspect) {
        au = (float) Math.tan(Math.toRadians(fov * 0.5f));
        av = au / aspect;
    }

    protected abstract Point3 getEye(double time);

    protected abstract Point3 getTarget(double time);

    protected abstract Vector3 getUp(double time);

    public Ray getRay(float x, float y, int imageWidth, int imageHeight, double lensX, double lensY, double time) {
        Point3 eye = getEye(time);
        OrthoNormalBasis basis = OrthoNormalBasis.makeFromWV(Point3.sub(eye, getTarget(time), new Vector3()), getUp(time));
        float du = -au + ((2.0f * au * x) / (imageWidth - 1.0f));
        float dv = -av + ((2.0f * av * y) / (imageHeight - 1.0f));
        return new Ray(eye, basis.transform(new Vector3(du, dv, -1), new Vector3()));
    }
}
