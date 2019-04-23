package org.sunflow;

import org.sunflow.core.parameter.BucketParameter;
import org.sunflow.core.parameter.ImageParameter;
import org.sunflow.core.parameter.TraceDepthsParameter;
import org.sunflow.core.Display;
import org.sunflow.core.display.FastDisplay;
import org.sunflow.core.display.FileDisplay;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;
import org.sunflow.system.ui.ConsoleInterface;

public class RealtimeBenchmark extends SunflowAPI {
    public RealtimeBenchmark(boolean showGUI, int threads) {
        Display display = showGUI ? new FastDisplay() : new FileDisplay(false);
        UI.printInfo(Module.BENCH, "Preparing benchmarking scene ...");
        // settings
        parameter("threads", threads);
        // spawn regular priority threads
        parameter("threads.lowPriority", false);
        parameter(ImageParameter.PARAM_RESOLUTION_X, 512);
        parameter(ImageParameter.PARAM_RESOLUTION_Y, 512);
        parameter(ImageParameter.PARAM_AA_MIN, -3);
        parameter(ImageParameter.PARAM_AA_MAX, 0);
        parameter(TraceDepthsParameter.PARAM_DEPTHS_DIFFUSE, 1);
        parameter(TraceDepthsParameter.PARAM_DEPTHS_REFLECTION, 1);
        parameter(TraceDepthsParameter.PARAM_DEPTHS_REFRACTION, 0);
        parameter(BucketParameter.PARAM_BUCKET_ORDER, BucketParameter.ORDER_HILBERT);
        parameter(BucketParameter.PARAM_BUCKET_SIZE, 32);
        options(SunflowAPI.DEFAULT_OPTIONS);
        // camera
        Point3 eye = new Point3(30, 0, 10.967f);
        Point3 target = new Point3(0, 0, 5.4f);
        Vector3 up = new Vector3(0, 0, 1);
        parameter("transform", Matrix4.lookAt(eye, target, up));
        parameter("fov", 45.0f);
        camera("camera", "pinhole");
        parameter("camera", "camera");
        options(SunflowAPI.DEFAULT_OPTIONS);
        // geometry
        createGeometry();
        // this first render is not timed, it caches the acceleration data
        // structures and tesselations so they won't be
        // included in the main timing
        UI.printInfo(Module.BENCH, "Rendering warmup frame ...");
        render(SunflowAPI.DEFAULT_OPTIONS, display);
        // now disable all output - and run the benchmark
        UI.set(null);
        Timer t = new Timer();
        t.start();
        float phi = 0;
        int frames = 0;
        while (phi < 4 * Math.PI) {
            eye.x = 30 * (float) Math.cos(phi);
            eye.y = 30 * (float) Math.sin(phi);
            phi += Math.PI / 30;
            frames++;
            // update camera
            parameter("transform", Matrix4.lookAt(eye, target, up));
            camera("camera", null);
            render(SunflowAPI.DEFAULT_OPTIONS, display);
        }
        t.end();
        UI.set(new ConsoleInterface());
        UI.printInfo(Module.BENCH, "Benchmark results:");
        UI.printInfo(Module.BENCH, "  * Average FPS:         %.2f", frames / t.seconds());
        UI.printInfo(Module.BENCH, "  * Total time:          %s", t);
    }

    private void createGeometry() {
        // light source
        parameter("source", new Point3(-15.5945f, -30.0581f, 45.967f));
        parameter("dir", new Vector3(15.5945f, 30.0581f, -45.967f));
        parameter("radius", 60.0f);
        parameter("radiance", null, 3, 3, 3);
        light("light", "directional");

        // gi-engine
        parameter("gi.engine", "fake");
        parameter("gi.fake.sky", null, 0.25f, 0.25f, 0.25f);
        parameter("gi.fake.ground", null, 0.01f, 0.01f, 0.5f);
        parameter("gi.fake.up", new Vector3(0, 0, 1));
        options(DEFAULT_OPTIONS);

        // shaders
        parameter("diffuse", null, 0.5f, 0.5f, 0.5f);
        shader("default", "diffuse");
        parameter("diffuse", null, 0.5f, 0.5f, 0.5f);
        parameter("shiny", 0.2f);
        shader("refl", "shiny_diffuse");
        // objects

        // teapot
        parameter("subdivs", 10);
        geometry("teapot", "teapot");
        parameter("shaders", "default");
        Matrix4 m = Matrix4.IDENTITY;
        m = Matrix4.scale(0.075f).multiply(m);
        m = Matrix4.rotateZ((float) Math.toRadians(-45f)).multiply(m);
        m = Matrix4.translation(-7, 0, 0).multiply(m);
        parameter("transform", m);
        instance("teapot.instance", "teapot");

        // gumbo
        parameter("subdivs", 10);
        geometry("gumbo", "gumbo");
        m = Matrix4.IDENTITY;
        m = Matrix4.scale(0.5f).multiply(m);
        m = Matrix4.rotateZ((float) Math.toRadians(25f)).multiply(m);
        m = Matrix4.translation(3, -7, 0).multiply(m);
        parameter("shaders", "default");
        parameter("transform", m);
        instance("gumbo.instance", "gumbo");

        // ground plane
        parameter("center", new Point3(0, 0, 0));
        parameter("normal", new Vector3(0, 0, 1));
        geometry("ground", "plane");
        parameter("shaders", "refl");
        instance("ground.instance", "ground");
    }
}