package org.sunflow;

import org.sunflow.core.Display;
import org.sunflow.core.camera.PinholeCamera;
import org.sunflow.core.display.FastDisplay;
import org.sunflow.core.display.FileDisplay;
import org.sunflow.core.gi.FakeGIEngine;
import org.sunflow.core.light.DirectionalSpotlight;
import org.sunflow.core.primitive.Plane;
import org.sunflow.core.shader.DiffuseShader;
import org.sunflow.core.shader.ShinyDiffuseShader;
import org.sunflow.core.tesselatable.Gumbo;
import org.sunflow.core.tesselatable.Teapot;
import org.sunflow.image.Color;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.system.ui.ConsoleInterface;

public class RealtimeBenchmark extends SunflowAPI {
    public RealtimeBenchmark(boolean showGUI, int threads) {
        Display display = showGUI ? new FastDisplay() : new FileDisplay(false);
        UI.printInfo("[BCH] Preparing benchmarking scene ...");
        threads(threads, true); // spawn regular priority threads
        resolution(512, 512);
        // settings
        antiAliasing(-3, 0);
        traceDepth(1, 1, 0);
        bucketOrder("hilbert");
        bucketSize(32);
        accel("bih");
        // camera
        Point3 eye = new Point3(30, 0, 10.967f);
        Point3 target = new Point3(0, 0, 5.4f);
        Vector3 up = new Vector3(0, 0, 1);
        camera(new PinholeCamera(eye, target, up, 45, 1));
        // geometry
        createGeometry();
        // this first render is not timed, it caches the acceleration data
        // structures and tesselations so they won't be
        // included in the main timing
        UI.printInfo("[BCH] Rendering warmup frame ...");
        render(display);
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
            camera(new PinholeCamera(eye, target, up, 45, 1));
            render(display);

        }
        t.end();
        UI.set(new ConsoleInterface());
        UI.printInfo("[BCH] Benchmark results:");
        UI.printInfo("[BCH]   * FPS:                 %.2f", frames / t.seconds());
        UI.printInfo("[BCH]   * Total time:          %s", t);
    }

    private void createGeometry() {
        // light source
        parameter("source", new Point3(-15.5945f, -30.0581f, 45.967f));
        parameter("dir", new Vector3(15.5945f, 30.0581f, -45.967f));
        parameter("radius", 60.0f);
        parameter("radiance", Color.white().mul(3));
        light("light", new DirectionalSpotlight());

        // gi-engine
        giEngine(new FakeGIEngine(new Vector3(0, 0, 1), new Color(0.25f, 0.25f, 0.25f), new Color(0.01f, 0.01f, 0.5f)));

        // shaders
        parameter("diffuse", Color.white().mul(0.5f));
        shader("default", new DiffuseShader());
        parameter("diffuse", Color.white().mul(0.5f));
        parameter("shiny", 0.2f);
        shader("refl", new ShinyDiffuseShader());
        // objects

        // teapot
        parameter("subdivs", 10);
        geometry("teapot", new Teapot());
        parameter("shaders", "default");
        Matrix4 m = Matrix4.IDENTITY;
        m = Matrix4.scale(0.075f).multiply(m);
        m = Matrix4.rotateZ((float) Math.toRadians(-45f)).multiply(m);
        m = Matrix4.translation(-7, 0, 0).multiply(m);
        parameter("transform", m);
        instance("teapot.instance", "teapot");

        // gumbo
        parameter("subdivs", 10);
        geometry("gumbo", new Gumbo());
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
        geometry("ground", new Plane());
        parameter("shaders", "refl");
        instance("ground.instance", "ground");
    }
}