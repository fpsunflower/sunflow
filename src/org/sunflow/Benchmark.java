package org.sunflow;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.sunflow.core.Display;
import org.sunflow.core.Tesselatable;
import org.sunflow.core.camera.PinholeLens;
import org.sunflow.core.display.FileDisplay;
import org.sunflow.core.light.TriangleMeshLight;
import org.sunflow.core.primitive.Sphere;
import org.sunflow.core.primitive.TriangleMesh;
import org.sunflow.core.shader.DiffuseShader;
import org.sunflow.core.shader.GlassShader;
import org.sunflow.core.shader.MirrorShader;
import org.sunflow.core.tesselatable.Teapot;
import org.sunflow.image.Color;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.BenchmarkFramework;
import org.sunflow.system.BenchmarkTest;
import org.sunflow.system.UI;
import org.sunflow.system.UserInterface;
import org.sunflow.system.UI.Module;
import org.sunflow.system.UI.PrintLevel;

public class Benchmark implements BenchmarkTest, UserInterface, Display {
    private int resolution;
    private boolean showOutput;
    private boolean showBenchmarkOutput;
    private boolean saveOutput;
    private int threads;
    private int[] referenceImage;
    private int[] validationImage;
    private int errorThreshold;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Benchmark options:");
            System.out.println("  -regen                        Regenerate reference images for a variety of sizes");
            System.out.println("  -bench [threads] [resolution] Run a single iteration of the benchmark using the specified thread count and image resolution");
            System.out.println("                                Default: threads=0 (auto-detect cpus), resolution=256");
        } else if (args[0].equals("-regen")) {
            int[] sizes = { 32, 64, 96, 128, 256, 384, 512 };
            for (int s : sizes) {
                // run a single iteration to generate the reference image
                Benchmark b = new Benchmark(s, true, false, true);
                b.kernelMain();
            }
        } else if (args[0].equals("-bench")) {
            int threads = 0, resolution = 256;
            if (args.length > 1)
                threads = Integer.parseInt(args[1]);
            if (args.length > 2)
                resolution = Integer.parseInt(args[2]);
            Benchmark benchmark = new Benchmark(resolution, false, true, false, threads);
            benchmark.kernelBegin();
            benchmark.kernelMain();
            benchmark.kernelEnd();
        }
    }

    public Benchmark() {
        this(384, false, true, false);
    }

    public Benchmark(int resolution, boolean showOutput, boolean showBenchmarkOutput, boolean saveOutput) {
        this(resolution, showOutput, showBenchmarkOutput, saveOutput, 0);
    }

    public Benchmark(int resolution, boolean showOutput, boolean showBenchmarkOutput, boolean saveOutput, int threads) {
        UI.set(this);
        this.resolution = resolution;
        this.showOutput = showOutput;
        this.showBenchmarkOutput = showBenchmarkOutput;
        this.saveOutput = saveOutput;
        this.threads = threads;
        errorThreshold = 6;
        // fetch reference image from resources (jar file or classpath)
        if (saveOutput)
            return;
        URL imageURL = Benchmark.class.getResource(String.format("/resources/golden_%04X.png", resolution));
        if (imageURL == null)
            UI.printError(Module.BENCH, "Unable to find reference frame!");
        UI.printInfo(Module.BENCH, "Loading reference image from: %s", imageURL);
        try {
            BufferedImage bi = ImageIO.read(imageURL);
            if (bi.getWidth() != resolution || bi.getHeight() != resolution)
                UI.printError(Module.BENCH, "Reference image has invalid resolution! Expected %dx%d found %dx%d", resolution, resolution, bi.getWidth(), bi.getHeight());
            referenceImage = new int[resolution * resolution];
            for (int y = 0, i = 0; y < resolution; y++)
                for (int x = 0; x < resolution; x++, i++)
                    referenceImage[i] = bi.getRGB(x, resolution - 1 - y); // flip
        } catch (IOException e) {
            UI.printError(Module.BENCH, "Unable to load reference frame!");
        }
    }

    public void execute() {
        // 10 iterations maximum - 10 minute time limit
        BenchmarkFramework framework = new BenchmarkFramework(10, 600);
        framework.execute(this);
    }

    private class BenchmarkScene extends SunflowAPI {
        public BenchmarkScene() {
            build();
            render(SunflowAPI.DEFAULT_OPTIONS, saveOutput ? new FileDisplay(String.format("resources/golden_%04X.png", resolution)) : Benchmark.this);
        }

        public void build() {
            // settings
            parameter("threads", threads);
            // spawn regular priority threads
            parameter("threads.lowPriority", false);
            parameter("resolutionX", resolution);
            parameter("resolutionY", resolution);
            parameter("aa.min", -1);
            parameter("aa.max", 1);
            parameter("filter", "triangle");
            parameter("depths.diffuse", 2);
            parameter("depths.reflection", 2);
            parameter("depths.refraction", 2);
            parameter("bucket.order", "hilbert");
            parameter("bucket.size", 32);
            // gi options
            parameter("gi.engine", "igi");
            parameter("gi.igi.samples", 90);
            parameter("gi.igi.c", 0.000008f);
            options(SunflowAPI.DEFAULT_OPTIONS);
            buildCornellBox();
        }

        private void buildCornellBox() {
            // camera
            parameter("eye", new Point3(0, 0, -600));
            parameter("target", new Point3(0, 0, 0));
            parameter("up", new Vector3(0, 1, 0));
            parameter("fov", 45.0f);
            camera("main_camera", new PinholeLens());
            parameter("camera", "main_camera");
            options(SunflowAPI.DEFAULT_OPTIONS);
            // cornell box
            Color gray = new Color(0.70f, 0.70f, 0.70f);
            Color blue = new Color(0.25f, 0.25f, 0.80f);
            Color red = new Color(0.80f, 0.25f, 0.25f);
            Color emit = new Color(15, 15, 15);

            float minX = -200;
            float maxX = 200;
            float minY = -160;
            float maxY = minY + 400;
            float minZ = -250;
            float maxZ = 200;

            float[] verts = new float[] { minX, minY, minZ, maxX, minY, minZ,
                    maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, minZ, maxX,
                    maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, };
            int[] indices = new int[] { 0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4, 1,
                    2, 5, 5, 6, 2, 2, 3, 6, 6, 7, 3, 0, 3, 4, 4, 7, 3 };

            parameter("diffuse", gray);
            shader("gray_shader", new DiffuseShader());
            parameter("diffuse", red);
            shader("red_shader", new DiffuseShader());
            parameter("diffuse", blue);
            shader("blue_shader", new DiffuseShader());

            // build walls
            parameter("triangles", indices);
            parameter("points", "point", "vertex", verts);
            parameter("faceshaders", new int[] { 0, 0, 0, 0, 1, 1, 0, 0, 2, 2 });
            geometry("walls", new TriangleMesh());

            // instance walls
            parameter("shaders", new String[] { "gray_shader", "red_shader",
                    "blue_shader" });
            instance("walls.instance", "walls");

            // create mesh light
            parameter("points", "point", "vertex", new float[] { -50, maxY - 1,
                    -50, 50, maxY - 1, -50, 50, maxY - 1, 50, -50, maxY - 1, 50 });
            parameter("triangles", new int[] { 0, 1, 2, 2, 3, 0 });
            parameter("radiance", emit);
            parameter("samples", 8);
            TriangleMeshLight light = new TriangleMeshLight();
            light.init("light", this);

            // spheres
            parameter("eta", 1.6f);
            shader("Glass", new GlassShader());
            sphere("glass_sphere", "Glass", -120, minY + 55, -150, 50);
            parameter("color", new Color(0.70f, 0.70f, 0.70f));
            shader("Mirror", new MirrorShader());
            sphere("mirror_sphere", "Mirror", 100, minY + 60, -50, 50);

            // scanned model
            geometry("teapot", (Tesselatable) new Teapot());
            parameter("transform", Matrix4.translation(80, -50, 100).multiply(Matrix4.rotateX((float) -Math.PI / 6)).multiply(Matrix4.rotateY((float) Math.PI / 4)).multiply(Matrix4.rotateX((float) -Math.PI / 2).multiply(Matrix4.scale(1.2f))));
            parameter("shaders", "gray_shader");
            instance("teapot.instance1", "teapot");
            parameter("transform", Matrix4.translation(-80, -160, 50).multiply(Matrix4.rotateY((float) Math.PI / 4)).multiply(Matrix4.rotateX((float) -Math.PI / 2).multiply(Matrix4.scale(1.2f))));
            parameter("shaders", "gray_shader");
            instance("teapot.instance2", "teapot");
        }

        private void sphere(String name, String shaderName, float x, float y, float z, float radius) {
            geometry(name, new Sphere());
            parameter("transform", Matrix4.translation(x, y, z).multiply(Matrix4.scale(radius)));
            parameter("shaders", shaderName);
            instance(name + ".instance", name);
        }
    }

    public void kernelBegin() {
        // allocate a fresh validation target
        validationImage = new int[resolution * resolution];
    }

    public void kernelMain() {
        // this builds and renders the scene
        new BenchmarkScene();
    }

    public void kernelEnd() {
        // make sure the rendered image was correct
        int diff = 0;
        if (referenceImage != null && validationImage.length == referenceImage.length) {
            for (int i = 0; i < validationImage.length; i++) {
                // count absolute RGB differences
                diff += Math.abs((validationImage[i] & 0xFF) - (referenceImage[i] & 0xFF));
                diff += Math.abs(((validationImage[i] >> 8) & 0xFF) - ((referenceImage[i] >> 8) & 0xFF));
                diff += Math.abs(((validationImage[i] >> 16) & 0xFF) - ((referenceImage[i] >> 16) & 0xFF));
            }
            if (diff > errorThreshold)
                UI.printError(Module.BENCH, "Image check failed! - #errors: %d", diff);
            else
                UI.printInfo(Module.BENCH, "Image check passed!");
        } else
            UI.printError(Module.BENCH, "Image check failed! - reference is not comparable");

    }

    public void print(Module m, PrintLevel level, String s) {
        if (showOutput || (showBenchmarkOutput && m == Module.BENCH))
            System.out.println(UI.formatOutput(m, level, s));
        if (level == PrintLevel.ERROR)
            throw new RuntimeException(s);
    }

    public void taskStart(String s, int min, int max) {
        // render progress display not needed
    }

    public void taskStop() {
        // render progress display not needed
    }

    public void taskUpdate(int current) {
        // render progress display not needed
    }

    public void imageBegin(int w, int h, int bucketSize) {
        // we can assume w == h == resolution
    }

    public void imageEnd() {
        // nothing needs to be done - image verification is done externally
    }

    public void imageFill(int x, int y, int w, int h, Color c) {
        // this is not used
    }

    public void imagePrepare(int x, int y, int w, int h, int id) {
        // this is not needed
    }

    public void imageUpdate(int x, int y, int w, int h, Color[] data) {
        // copy bucket data to validation image
        for (int j = 0, index = 0; j < h; j++, y++)
            for (int i = 0, offset = x + resolution * (resolution - 1 - y); i < w; i++, index++, offset++)
                validationImage[offset] = data[index].copy().toNonLinear().toRGB();
    }
}