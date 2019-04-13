package examples;

import org.sunflow.SunflowAPI;
import org.sunflow.core.parameter.ImageParameter;
import org.sunflow.core.parameter.TraceDepthsParameter;
import org.sunflow.core.parameter.camera.PinholeCameraParameter;
import org.sunflow.core.parameter.geometry.JuliaParameter;
import org.sunflow.core.parameter.gi.PathTracingGIParameter;
import org.sunflow.core.parameter.light.SphereLightParameter;
import org.sunflow.core.parameter.shader.DiffuseShaderParameter;
import org.sunflow.image.Color;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class JuliaScene {

    public static void main(String[] args) {
        SunflowAPI api = new SunflowAPI();
        api.reset();

        ImageParameter image = new ImageParameter();
        image.setResolutionX(512);
        image.setResolutionY(512);
        image.setAAMin(0);
        image.setAAMax(2);
        image.setFilter(ImageParameter.FILTER_GAUSSIAN);
        image.setup(api);

        TraceDepthsParameter traceDepths = new TraceDepthsParameter();
        traceDepths.setDiffuse(1);
        traceDepths.setReflection(0);
        traceDepths.setRefraction(0);
        traceDepths.setup(api);

        PinholeCameraParameter camera = new PinholeCameraParameter();

        camera.setName("camera");
        Point3 eye = new Point3(-5, 0, 0);
        Point3 target = new Point3(0, 0, 0);
        Vector3 up = new Vector3(0, 1, 0);

        camera.setupTransform(api, eye, target, up);

        camera.setFov(58f);
        camera.setAspect(1);
        camera.setup(api);

        PathTracingGIParameter gi = new PathTracingGIParameter();
        gi.setSamples(16);
        gi.setup(api);

        DiffuseShaderParameter simple1 = new DiffuseShaderParameter("simple1");
        simple1.setDiffuse(new Color(0.5f, 0.5f, 0.5f).toLinear());
        simple1.setup(api);

        SphereLightParameter light0 = new SphereLightParameter();
        light0.setRadiance(new Color(1, 1, 0.6f).toLinear().mul(60));
        light0.setCenter(new Point3(-5, 7, 5));
        light0.setRadius(2);
        light0.setSamples(8);
        light0.setup(api);

        SphereLightParameter light1 = new SphereLightParameter();
        light1.setRadiance(new Color(0.6f, 0.6f, 1f).toLinear().mul(20));
        light1.setCenter(new Point3(-15, -17, -15));
        light1.setRadius(5);
        light1.setSamples(8);
        light1.setup(api);

        JuliaParameter left = new JuliaParameter("left");
        left.shaders(simple1);
        left.scale(2);
        left.rotateY(45);
        left.rotateX(-55);
        left.setIterations(8);
        left.setEpsilon(0.001f);
        left.setQuaternion(-0.125f, -0.256f, 0.847f, 0.0895f);
        left.setup(api);

        finalRender(api);
    }

    private static void previewRender(SunflowAPI api) {
        api.parameter("sampler", "ipr");
        api.options(SunflowAPI.DEFAULT_OPTIONS);
        api.render(SunflowAPI.DEFAULT_OPTIONS, null);
    }

    private static void finalRender(SunflowAPI api) {
        api.parameter("sampler", "bucket");
        api.options(SunflowAPI.DEFAULT_OPTIONS);
        api.render(SunflowAPI.DEFAULT_OPTIONS, null);
    }

}
