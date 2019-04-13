package examples;

import org.sunflow.SunflowAPI;
import org.sunflow.core.parameter.ImageParameter;
import org.sunflow.core.parameter.camera.PinholeCameraParameter;
import org.sunflow.core.parameter.geometry.GumboParameter;
import org.sunflow.core.parameter.geometry.PlaneParameter;
import org.sunflow.core.parameter.geometry.TeapotParameter;
import org.sunflow.core.parameter.light.SunSkyLightParameter;
import org.sunflow.core.parameter.shader.DiffuseShaderParameter;
import org.sunflow.core.parameter.shader.ShinyShaderParameter;
import org.sunflow.image.Color;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class GumboAndTeapotScene {

    public static void main(String[] args) {
        SunflowAPI api = new SunflowAPI();
        api.reset();

        ImageParameter image = new ImageParameter();
        image.setResolutionX(800);
        image.setResolutionY(450);
        image.setAAMin(0);
        image.setAAMax(1);
        image.setFilter(ImageParameter.FILTER_TRIANGLE);
        image.setup(api);

        PinholeCameraParameter camera = new PinholeCameraParameter();

        camera.setName("camera");
        Point3 eye = new Point3(-18.19f, 8.97f, -0.93f);
        Point3 target = new Point3(-0.690f, 0.97f, -0.93f);
        Vector3 up = new Vector3(0, 1, 0);

        camera.setupTransform(api, eye, target, up);

        camera.setFov(30f);
        camera.setAspect(1.777777777777f);
        camera.setup(api);

        SunSkyLightParameter lightParameter = new SunSkyLightParameter();
        lightParameter.setName("sunsky");
        lightParameter.setUp(new Vector3(0, 1, 0));
        lightParameter.setEast(new Vector3(0, 0, 1));
        lightParameter.setSunDirection(new Vector3(1, 1, 1));
        lightParameter.setTurbidity(4);
        lightParameter.setSamples(64);
        lightParameter.setup(api);

        // Materials
        ShinyShaderParameter shiny = new ShinyShaderParameter("default");
        shiny.setDiffuse(new Color(0.2f, 0.2f, 0.2f));
        shiny.setShininess(0.1f);
        shiny.setup(api);

        DiffuseShaderParameter simple = new DiffuseShaderParameter("simple");
        simple.setDiffuse(new Color(0.2f, 0.2f, 0.2f));
        simple.setup(api);

        DiffuseShaderParameter simpleRed = new DiffuseShaderParameter("simple_red");
        simpleRed.setDiffuse(new Color(0.8f, 0.2f, 0.2f).toLinear());
        simpleRed.setup(api);

        DiffuseShaderParameter simpleGreen = new DiffuseShaderParameter("simple_green");
        simpleGreen.setDiffuse(new Color(0.2f, 0.8f, 0.2f).toLinear());
        simpleGreen.setup(api);

        DiffuseShaderParameter simpleBlue = new DiffuseShaderParameter("simple_blue");
        simpleBlue.setDiffuse(new Color(0.2f, 0.2f, 0.8f).toLinear());
        simpleBlue.setup(api);

        DiffuseShaderParameter simpleYellow = new DiffuseShaderParameter("simple_yellow");
        simpleYellow.setDiffuse(new Color(0.8f, 0.8f, 0.2f).toLinear());
        simpleYellow.setup(api);

        DiffuseShaderParameter floorShader = new DiffuseShaderParameter("floor");
        floorShader.setDiffuse(new Color(0.1f, 0.1f, 0.1f));
        floorShader.setup(api);

        GumboParameter gumbo0 = new GumboParameter();
        gumbo0.setName("gumbo_0");
        gumbo0.setSubdivs(7);
        gumbo0.shaders(shiny);
        gumbo0.rotateX(-90);
        gumbo0.scale(0.1f);
        gumbo0.rotateY(75);
        gumbo0.translate(-0.25f, 0, 0.63f);
        gumbo0.setup(api);

        GumboParameter gumbo1 = new GumboParameter();
        gumbo1.setName("gumbo_1");
        gumbo1.setSubdivs(4);
        gumbo1.shaders(simpleRed);
        gumbo1.rotateX(-90);
        gumbo1.scale(0.1f);
        gumbo1.rotateY(25);
        gumbo1.translate(1.5f, 0, -1.5f);
        gumbo1.setup(api);

        GumboParameter gumbo2 = new GumboParameter();
        gumbo2.setName("gumbo_2");
        gumbo2.setSubdivs(3);
        gumbo2.shaders(simpleBlue);
        gumbo2.rotateX(-90);
        gumbo2.scale(0.1f);
        gumbo2.rotateY(25);
        gumbo2.translate(0, 0, -3f);
        gumbo2.setSmooth(false);
        gumbo2.setup(api);

        GumboParameter gumbo3 = new GumboParameter();
        gumbo3.setName("gumbo_3");
        gumbo3.setSubdivs(6);
        gumbo3.shaders(simpleGreen);
        gumbo3.rotateX(-90);
        gumbo3.scale(0.1f);
        gumbo3.rotateY(-25);
        gumbo3.translate(1.5f, 0, 1.5f);
        gumbo3.setSmooth(false);
        gumbo3.setup(api);

        GumboParameter gumbo4 = new GumboParameter();
        gumbo4.setName("gumbo_4");
        gumbo4.setSubdivs(8);
        gumbo4.shaders(simpleYellow);
        gumbo4.rotateX(-90);
        gumbo4.scale(0.1f);
        gumbo4.rotateY(-25);
        gumbo4.translate(0f, 0, 3f);
        gumbo4.setSmooth(false);
        gumbo4.setup(api);

        PlaneParameter floor = new PlaneParameter();
        floor.shaders(floorShader);
        floor.setCenter(new Point3(0, 0, 0));
        floor.setNormal(new Vector3(0, 1, 0));
        floor.setup(api);

        TeapotParameter teapot0 = new TeapotParameter("teapot_0");
        teapot0.shaders(shiny);
        teapot0.rotateX(-90);
        teapot0.scale(0.008f);
        teapot0.rotateY(245f);
        teapot0.translate(-3, 0, -1);
        teapot0.setSubdivs(7);
        teapot0.setup(api);

        TeapotParameter teapot1 = new TeapotParameter("teapot_1");
        teapot1.shaders(simpleYellow);
        teapot1.rotateX(-90);
        teapot1.scale(0.008f);
        teapot1.rotateY(245f);
        teapot1.translate(-1.5f, 0, -3);
        teapot1.setSubdivs(4);
        teapot1.setSmooth(false);
        teapot1.setup(api);

        TeapotParameter teapot2 = new TeapotParameter("teapot_2");
        teapot2.shaders(simpleGreen);
        teapot2.rotateX(-90);
        teapot2.scale(0.008f);
        teapot2.rotateY(245f);
        teapot2.translate(0, 0, -5);
        teapot2.setSubdivs(3);
        teapot2.setSmooth(false);
        teapot2.setup(api);

        TeapotParameter teapot3 = new TeapotParameter("teapot_3");
        teapot3.shaders(simpleRed);
        teapot3.rotateX(-90);
        teapot3.scale(0.008f);
        teapot3.rotateY(245f);
        teapot3.translate(-1.5f, 0, 1);
        teapot3.setSubdivs(5);
        teapot3.setSmooth(false);
        teapot3.setup(api);

        TeapotParameter teapot4 = new TeapotParameter("teapot_4");
        teapot4.shaders(simpleBlue);
        teapot4.rotateX(-90);
        teapot4.scale(0.008f);
        teapot4.rotateY(245f);
        teapot4.translate(0, 0, 3);
        teapot4.setSubdivs(7);
        teapot4.setSmooth(false);
        teapot4.setup(api);

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
