package examples;

import org.sunflow.SunflowAPI;
import org.sunflow.core.parameter.ImageParameter;
import org.sunflow.core.parameter.InstanceParameter;
import org.sunflow.core.parameter.PhotonParameter;
import org.sunflow.core.parameter.TraceDepthsParameter;
import org.sunflow.core.parameter.camera.PinholeCameraParameter;
import org.sunflow.core.parameter.geometry.BoxParameter;
import org.sunflow.core.parameter.geometry.SphereParameter;
import org.sunflow.core.parameter.gi.InstantGIParameter;
import org.sunflow.core.parameter.light.CornellBoxLightParameter;
import org.sunflow.core.parameter.shader.GlassShaderParameter;
import org.sunflow.core.parameter.shader.MirrorShaderParameter;
import org.sunflow.image.Color;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class CornellBoxJensenWithBoxesScene {

    public static void main(String[] args) {
        SunflowAPI api = new SunflowAPI();
        api.reset();

        ImageParameter image = new ImageParameter();
        image.setResolutionX(800);
        image.setResolutionY(600);
        image.setAAMin(0);
        image.setAAMax(2);
        image.setFilter(ImageParameter.FILTER_GAUSSIAN);
        image.setup(api);

        TraceDepthsParameter traceDepths = new TraceDepthsParameter();
        traceDepths.setDiffuse(4);
        traceDepths.setReflection(3);
        traceDepths.setRefraction(2);
        traceDepths.setup(api);

        PhotonParameter photons = new PhotonParameter();
        photons.setNumEmit(1000000);
        photons.setCaustics("kd");
        photons.setCausticsGather(100);
        photons.setCausticsRadius(0.5f);
        photons.setup(api);

        InstantGIParameter gi = new InstantGIParameter();
        gi.setSamples(64);
        gi.setSets(1);
        gi.setBias(0.00003f);
        gi.setBiasSamples(0);
        gi.setup(api);

        PinholeCameraParameter camera = new PinholeCameraParameter();

        camera.setName("camera");
        Point3 eye = new Point3(0, -205, 50);
        Point3 target = new Point3(0, 0, 50);
        Vector3 up = new Vector3(0, 0, 1);

        camera.setupTransform(api, eye,target,up);

        camera.setFov(45f);
        camera.setAspect(1.333333f);
        camera.setup(api);

        // Materials
        MirrorShaderParameter mirror = new MirrorShaderParameter("Mirror");
        mirror.setReflection(new Color(0.7f, 0.7f, 0.7f));
        mirror.setup(api);

        GlassShaderParameter glass = new GlassShaderParameter("Glass");
        glass.setEta(1.6f);
        glass.setAbsorptionColor(new Color(1, 1, 1));
        glass.setup(api);

        // Lights
        CornellBoxLightParameter lightParameter = new CornellBoxLightParameter();
        lightParameter.setName("cornell-box-light");
        lightParameter.setMin(new Point3(-60, -60, 0));
        lightParameter.setMax(new Point3(60, 60, 100));
        lightParameter.setLeft(new Color(0.8f, 0.25f, 0.25f));
        lightParameter.setRight(new Color(0.25f, 0.25f, 0.8f));
        lightParameter.setTop(new Color(0.7f, 0.7f, 0.7f));
        lightParameter.setBottom(new Color(0.7f, 0.7f, 0.7f));
        lightParameter.setBack(new Color(0.7f, 0.7f, 0.7f));
        lightParameter.setRadiance(new Color(15, 15, 15));
        lightParameter.setSamples(32);
        lightParameter.setup(api);

        BoxParameter mirrorBox = new BoxParameter();
        mirrorBox.setName("mirror-box");
        mirrorBox.setMin(new Point3(-50, 10, 0));
        mirrorBox.setMax(new Point3(-10, 50, 40));
        mirrorBox.shaders(mirror);
        mirrorBox.setup(api);

        BoxParameter glassBox = new BoxParameter();
        glassBox.setName("glass-box");
        glassBox.setMin(new Point3(8, -22, 0));
        glassBox.setMax(new Point3(48, 22, 40));
        glassBox.shaders(glass);
        glassBox.setup(api);

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
