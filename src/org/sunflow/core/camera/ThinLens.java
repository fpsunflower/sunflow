package org.sunflow.core.camera;

import org.sunflow.SunflowAPI;
import org.sunflow.core.CameraLens;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Ray;

public class ThinLens implements CameraLens {
    private float au, av;
    private float aspect, fov;
    private float focusDistance;
    private float lensRadius;

    public ThinLens() {
        focusDistance = 1;
        lensRadius = 0;
        fov = 90;
        aspect = 1;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        // get parameters
        fov = pl.getFloat("fov", fov);
        aspect = pl.getFloat("aspect", aspect);
        focusDistance = pl.getFloat("focusDistance", focusDistance);
        lensRadius = pl.getFloat("lensRadius", lensRadius);
        update();
        return true;
    }

    private void update() {
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
        // point on the lens
        float eyeX = (float) (Math.cos(angle) * r);
        float eyeY = (float) (Math.sin(angle) * r);
        float eyeZ = 0;
        // point on the image plane
        float dirX = du;
        float dirY = dv;
        float dirZ = -focusDistance;
        // ray
        return new Ray(eyeX, eyeY, eyeZ, dirX - eyeX, dirY - eyeY, dirZ - eyeZ);
    }
}