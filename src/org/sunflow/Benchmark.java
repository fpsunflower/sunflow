package org.sunflow;

import org.sunflow.core.camera.PinholeCamera;
import org.sunflow.core.display.FileDisplay;
import org.sunflow.core.display.OpenExrDisplay;
import org.sunflow.core.gi.InstantGI;
import org.sunflow.core.photonmap.CausticPhotonMap;
import org.sunflow.core.primitive.CornellBox;
import org.sunflow.core.shader.GlassShader;
import org.sunflow.core.shader.MirrorShader;
import org.sunflow.image.Color;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;

public class Benchmark extends SunflowAPI {
    private int repeat = 5;
    private Timer[] timers = new Timer[3];

    private void buildJensenCornellBox() {
        // camera
        camera(new PinholeCamera(new Point3(0, -205, 50), new Point3(0, 0, 50), new Vector3(0, 0, 1), 45, 4.0f / 3.0f));
        // cornell box
        Color grey = new Color(0.70f, 0.70f, 0.70f);
        Color blue = new Color(0.25f, 0.25f, 0.80f);
        Color red = new Color(0.80f, 0.25f, 0.25f);
        Color emit = new Color(15, 15, 15);
        CornellBox box = new CornellBox(new Point3(-60, -60, 0), new Point3(60, 60, 100), red, blue, grey, grey, grey, emit, 32);
        primitive(box);
        light(box);
        // spheres
        shader("Mirror", new MirrorShader(new Color(0.70f, 0.70f, 0.70f)));
        sphere(-30, 30, 20, 20);
        shader("Glass", new GlassShader(1.6f, Color.WHITE));
        sphere(28, 2, 20, 20);
    }

    private void showResults() {
        UI.printInfo("[API] Results:");
        int i = 0;
        long total = 0;
        for (Timer t : timers) {
            total += t.nanos();
            UI.printInfo("[API]   * Test %d: %s", i, Timer.toString(t.nanos() / repeat));
            i++;
        }
        UI.printInfo("[API]   * Total: %s", Timer.toString(total));
    }

    public void build() {
        UI.printInfo("[API] Benchmark mode started ...");
        reset();
        // settings
        resolution(1024, 768);
        antiAliasing(0, 2);
        accel("null");
        filter("gaussian");
        traceDepth(4, 3, 2);
        bucketOrder("hilbert");
        bucketSize(32);
        // geometry
        buildJensenCornellBox();
        UI.printInfo("[API] Test 01: Direct lighting ...");
        render(new OpenExrDisplay("bench_01.exr", "zip", "float"));
        timers[0] = new Timer();
        timers[0].start();
        for (int i = 0; i < repeat; i++)
            render(new FileDisplay(false));
        timers[0].end();
        UI.printInfo("[API] Test 01: Direct lighting complete.");
        UI.printInfo("[API] Test 02: Caustics ...");
        photons(new CausticPhotonMap(1000000, 100, 0.5f, 1.1f));
        render(new OpenExrDisplay("bench_02.exr", "zip", "float"));
        timers[1] = new Timer();
        timers[1].start();
        for (int i = 0; i < repeat; i++)
            render(new FileDisplay(false));
        timers[1].end();
        UI.printInfo("[API] Test 02: Caustics complete.");
        UI.printInfo("[API] Test 03: Global illumination ...");
        giEngine(new InstantGI(128, 1, 0.00003f, 0));
        render(new OpenExrDisplay("bench_03.exr", "zip", "float"));
        timers[2] = new Timer();
        timers[2].start();
        for (int i = 0; i < repeat; i++)
            render(new FileDisplay(false));
        timers[2].end();
        UI.printInfo("[API] Test 03: Global illumination complete.");
        // quit
        showResults();
        System.exit(0);
    }
}