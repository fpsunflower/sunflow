package org.sunflow;

import org.sunflow.core.Display;
import org.sunflow.core.camera.SphericalLens;
import org.sunflow.core.display.FileDisplay;
import org.sunflow.core.display.FrameDisplay;
import org.sunflow.core.light.SunSkyLight;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class SunskyTest extends SunflowAPI {
    public SunskyTest(boolean showGUI, int threads) {
        Display display = showGUI ? new FrameDisplay() : new FileDisplay(false);
        // settings
        parameter("resolutionX", 1024);
        parameter("resolutionY", 512);
        parameter("aa.min", -3);
        parameter("aa.max", 0);
        parameter("depths.diffuse", 1);
        parameter("depths.reflection", 1);
        parameter("depths.refraction", 0);
        parameter("bucket.order", "hilbert");
        parameter("bucket.size", 32);
        options(SunflowAPI.DEFAULT_OPTIONS);
        // camera
        Point3 eye = new Point3(-1, 0, 0);
        Point3 target = new Point3(0, 0, 0);
        Vector3 up = new Vector3(0, 0, 1);
        parameter("eye", eye);
        parameter("target", target);
        parameter("up", up);
        String name = getUniqueName("camera");
        camera(name, new SphericalLens());
        parameter("camera", name);
        options(SunflowAPI.DEFAULT_OPTIONS);
        new SunSkyLight().init("sunsky", this);
        render(SunflowAPI.DEFAULT_OPTIONS, display);
    }

    public static void main(String[] args) {
        new SunskyTest(true, 0).build();
    }
}