package org.sunflow.core.camera;

import org.sunflow.core.Camera;
import org.sunflow.core.Ray;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class ThinLensCamera implements Camera {
    private Point3 eye;

    private OrthoNormalBasis basis;

    private float au, av;

    private float focusDistance;

    private float lensRadius;

    public ThinLensCamera(Point3 eye, Point3 target, Vector3 up, float fov, float aspect, float focusDistance, float lensRadius) {
        this.eye = new Point3(eye);
        this.focusDistance = focusDistance;
        this.lensRadius = lensRadius;
        basis = OrthoNormalBasis.makeFromWV(Point3.sub(eye, target, new Vector3()), up);
        au = (float) Math.tan(Math.toRadians(fov * 0.5f)) * focusDistance;
        av = au / aspect;
    }

    public Ray getRay(float x, float y, int imageWidth, int imageHeight, double lensX, double lensY, double time) {
        float du = -au + ((2.0f * au * x) / (imageWidth - 1.0f));
        float dv = -av + ((2.0f * av * y) / (imageHeight - 1.0f));

        double angle, r;
        // concentric map sampling
        double r1 = 2 * lensX - 1;
        double r2 = 2 * lensY - 1;
        if (r1 > -r2) {
            if (r1 > r2) {
                r = r1;
                angle = 0.25 * Math.PI * r2 / r1;
            } else {
                r = r2;
                angle = 0.25 * Math.PI * (2 - r1 / r2);
            }
        } else {
            if (r1 < r2) {
                r = -r1;
                angle = 0.25 * Math.PI * (4 + r2 / r1);
            } else {
                r = -r2;
                if (r2 != 0)
                    angle = 0.25 * Math.PI * (6 - r1 / r2);
                else
                    angle = 0;
            }
        }
        r *= lensRadius;
        Vector3 l = basis.transform(new Vector3((float) (Math.cos(angle) * r), (float) (Math.sin(angle) * r), 0.0f), new Vector3());
        Vector3 d = basis.transform(new Vector3(du, dv, -focusDistance), new Vector3());
        return new Ray(Point3.add(eye, l, new Point3()), Vector3.sub(d, l, new Vector3()));
    }
}