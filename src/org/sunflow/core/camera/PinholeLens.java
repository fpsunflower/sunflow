package org.sunflow.core.camera;

import org.sunflow.SunflowAPI;
import org.sunflow.core.CameraLens;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Ray;

public class PinholeLens implements CameraLens {
    private float au, av;
    private float aspect, fov;

    public PinholeLens() {
        fov = 90;
        aspect = 1;
        update();
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        // get parameters
        fov = pl.getFloat("fov", fov);
        aspect = pl.getFloat("aspect", aspect);
        update();
        return true;
    }

    private void update() {
        au = (float) Math.tan(Math.toRadians(fov * 0.5f));
        av = au / aspect;
    }

    public Ray getRay(float x, float y, int imageWidth, int imageHeight, double lensX, double lensY, double time) {
        float du = -au + ((2.0f * au * x) / (imageWidth - 1.0f));
        float dv = -av + ((2.0f * av * y) / (imageHeight - 1.0f));
        return new Ray(0, 0, 0, du, dv, -1);
    }
}