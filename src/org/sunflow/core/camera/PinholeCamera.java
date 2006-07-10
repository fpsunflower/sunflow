package org.sunflow.core.camera;

import org.sunflow.core.Camera;
import org.sunflow.core.Ray;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class PinholeCamera implements Camera {
    private Point3 eye;

    private OrthoNormalBasis basis;

    private float au, av;

    public PinholeCamera(Point3 eye, Point3 target, Vector3 up, float fov, float aspect) {
        this.eye = new Point3(eye);
        basis = OrthoNormalBasis.makeFromWV(Point3.sub(eye, target, new Vector3()), up);
        au = (float) Math.tan(Math.toRadians(fov * 0.5f));
        av = au / aspect;
    }

    public Ray getRay(float x, float y, int imageWidth, int imageHeight, double lensX, double lensY, double time) {
        float du = -au + ((2.0f * au * x) / (imageWidth - 1.0f));
        float dv = -av + ((2.0f * av * y) / (imageHeight - 1.0f));
        return new Ray(eye, basis.transform(new Vector3(du, dv, -1), new Vector3()));
    }
}