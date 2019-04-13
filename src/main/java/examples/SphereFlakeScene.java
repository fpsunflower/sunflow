package examples;

import org.sunflow.SunflowAPI;
import org.sunflow.core.parameter.ImageParameter;
import org.sunflow.core.parameter.InstanceParameter;
import org.sunflow.core.parameter.TraceDepthsParameter;
import org.sunflow.core.parameter.camera.PinholeCameraParameter;
import org.sunflow.core.parameter.camera.ThinLensCameraParameter;
import org.sunflow.core.parameter.geometry.PlaneParameter;
import org.sunflow.core.parameter.geometry.SphereFlakeParameter;
import org.sunflow.core.parameter.geometry.TeapotParameter;
import org.sunflow.core.parameter.gi.GlobalIlluminationParameter;
import org.sunflow.core.parameter.gi.PathTracingGIParameter;
import org.sunflow.core.parameter.light.SunSkyLightParameter;
import org.sunflow.core.parameter.modifier.BumpMapModifierParameter;
import org.sunflow.core.parameter.modifier.NormalMapModifierParameter;
import org.sunflow.core.parameter.shader.DiffuseShaderParameter;
import org.sunflow.core.parameter.shader.GlassShaderParameter;
import org.sunflow.core.parameter.shader.PhongShaderParameter;
import org.sunflow.core.parameter.shader.ShinyShaderParameter;
import org.sunflow.image.Color;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class SphereFlakeScene {

    public static void main(String[] args) {
        SunflowAPI api = new SunflowAPI();
        api.reset();

        ImageParameter image = new ImageParameter();
        image.setResolutionX(1920);
        image.setResolutionY(1080);
        image.setAAMin(4);
        image.setAAMax(4);
        image.setAASamples(4);
        image.setFilter(ImageParameter.FILTER_GAUSSIAN);
        image.setup(api);

        TraceDepthsParameter traceDepths = new TraceDepthsParameter();
        traceDepths.setDiffuse(1);
        traceDepths.setReflection(1);
        traceDepths.setRefraction(0);
        traceDepths.setup(api);

        ThinLensCameraParameter camera = new ThinLensCameraParameter();

        camera.setName("camera");

        Point3 eye = new Point3(-5, 0, -0.9f);
        Point3 target = new Point3(0, 0, 0.2f);
        Vector3 up = new Vector3(0, 0, 1);

        camera.setupTransform(api, eye, target, up);

        camera.setFov(60f);
        camera.setAspect(1.777777777777f);
        camera.setFocusDistance(5);
        camera.setLensRadius(0.01f);
        camera.setup(api);

        PathTracingGIParameter gi = new PathTracingGIParameter();
        gi.setSamples(16);
        gi.setup(api);

        DiffuseShaderParameter simple1 = new DiffuseShaderParameter("simple1");
        simple1.setDiffuse(new Color(0.5f, 0.5f, 0.5f));
        simple1.setup(api);

        GlassShaderParameter glassy = new GlassShaderParameter("glassy");
        glassy.setEta(1.333f);
        glassy.setColor(new Color(0.8f, 0.8f, 0.8f));
        glassy.setAbsorptionDistance(15);
        glassy.setAbsorptionColor(new Color(0.2f, 0.7f, 0.2f).toNonLinear());
        glassy.setup(api);

        SunSkyLightParameter lightParameter = new SunSkyLightParameter();
        lightParameter.setName("sunsky");
        lightParameter.setUp(new Vector3(0, 0, 1));
        lightParameter.setEast(new Vector3(0, 1, 0));
        lightParameter.setSunDirection(new Vector3(-1, 1, 0.2f));
        lightParameter.setTurbidity(2);
        lightParameter.setSamples(32);
        lightParameter.setup(api);

        PhongShaderParameter metal = new PhongShaderParameter("metal");
        metal.setDiffuse(new Color(0.1f,0.1f,0.1f));
        metal.setSpecular(new Color(0.1f,0.1f,0.1f));
        metal.setSamples(4);
        metal.setup(api);

        SphereFlakeParameter sphereFlakeParameter = new SphereFlakeParameter("flake");
        sphereFlakeParameter.setInstanceParameter(new InstanceParameter().shaders("metal"));
        sphereFlakeParameter.setLevel(7);
        sphereFlakeParameter.setup(api);

        PlaneParameter planeParameter = new PlaneParameter();
        planeParameter.shaders(simple1);
        planeParameter.setCenter(new Point3(0,0,-1));
        planeParameter.setNormal(new Vector3(0,0,1));
        planeParameter.setup(api);

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
