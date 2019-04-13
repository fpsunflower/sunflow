package examples;

import org.sunflow.SunflowAPI;
import org.sunflow.core.parameter.ImageParameter;
import org.sunflow.core.parameter.camera.PinholeCameraParameter;
import org.sunflow.core.parameter.geometry.PlaneParameter;
import org.sunflow.core.parameter.geometry.SphereParameter;
import org.sunflow.core.parameter.geometry.TeapotParameter;
import org.sunflow.core.parameter.light.SunSkyLightParameter;
import org.sunflow.core.parameter.modifier.BumpMapModifierParameter;
import org.sunflow.core.parameter.modifier.NormalMapModifierParameter;
import org.sunflow.core.parameter.shader.DiffuseShaderParameter;
import org.sunflow.core.parameter.shader.GlassShaderParameter;
import org.sunflow.core.parameter.shader.ShinyShaderParameter;
import org.sunflow.image.Color;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class BumpScene {

    public static void main(String[] args) {
        SunflowAPI api = new SunflowAPI();
        api.reset();
        api.searchpath("texture", System.getProperty("user.dir") + "/examples/");
        buildScene(api);

        finalRender(api);
    }

    public static void buildScene(SunflowAPI api) {
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
        lightParameter.setSunDirection(new Vector3(-1, 1, -1));
        lightParameter.setTurbidity(2);
        lightParameter.setSamples(32);
        lightParameter.setup(api);

        NormalMapModifierParameter bumpy01 = new NormalMapModifierParameter("bumpy_01");
        bumpy01.setTexture("textures/brick_normal.jpg");
        bumpy01.setup(api);

        BumpMapModifierParameter bumpy02 = new BumpMapModifierParameter("bumpy_02");
        bumpy02.setTexture("textures/dirty_bump.jpg");
        bumpy02.setScale(0.02f);
        bumpy02.setup(api);

        BumpMapModifierParameter bumpy03 = new BumpMapModifierParameter("bumpy_03");
        bumpy03.setTexture("textures/reptileskin_bump.png");
        bumpy03.setScale(0.02f);
        bumpy03.setup(api);

        BumpMapModifierParameter bumpy04 = new BumpMapModifierParameter("bumpy_04");
        bumpy04.setTexture("textures/shiphull_bump.png");
        bumpy04.setScale(0.15f);
        bumpy04.setup(api);

        BumpMapModifierParameter bumpy05 = new BumpMapModifierParameter("bumpy_05");
        bumpy05.setTexture("textures/slime_bump.jpg");
        bumpy05.setScale(0.15f);
        bumpy05.setup(api);

        ShinyShaderParameter shiny = new ShinyShaderParameter("default");
        shiny.setDiffuse(new Color(0.2f, 0.2f, 0.2f));
        shiny.setShininess(0.3f);
        shiny.setup(api);

        GlassShaderParameter glassy = new GlassShaderParameter("glassy");
        glassy.setEta(1.2f);
        glassy.setColor(new Color(0.8f, 0.8f, 0.8f));
        glassy.setAbsorptionDistance(7);
        glassy.setAbsorptionColor(new Color(0.2f, 0.7f, 0.2f).toLinear());
        glassy.setup(api);

        DiffuseShaderParameter simpleRed = new DiffuseShaderParameter("simple_red");
        simpleRed.setDiffuse(new Color(0.7f, 0.15f, 0.15f).toLinear());
        simpleRed.setup(api);

        DiffuseShaderParameter simpleGreen = new DiffuseShaderParameter("simple_green");
        simpleGreen.setDiffuse(new Color(0.15f, 0.7f, 0.15f).toLinear());
        simpleGreen.setup(api);

        DiffuseShaderParameter simpleYellow = new DiffuseShaderParameter("simple_yellow");
        simpleYellow.setDiffuse(new Color(0.8f, 0.8f, 0.2f).toLinear());
        simpleYellow.setup(api);

        DiffuseShaderParameter floorShader = new DiffuseShaderParameter("floor");
        //floorShader.setDiffuse(new Color(0.3f, 0.3f, 0.3f));
        floorShader.setTexture("textures/brick_color.jpg");
        floorShader.setup(api);

        PlaneParameter floor = new PlaneParameter();
        floor.shaders(floorShader);
        floor.modifiers(bumpy01);
        floor.setCenter(new Point3(0, 0, 0));
        floor.setPoint1(new Point3(4, 0, 3));
        floor.setPoint2(new Point3(-3, 0, 4));
        floor.setup(api);

        TeapotParameter teapot0 = new TeapotParameter("teapot_0");
        teapot0.shaders(simpleGreen);
        teapot0.modifiers(bumpy03);
        teapot0.rotateX(-90);
        teapot0.scale(0.018f);
        teapot0.rotateY(245f);
        teapot0.translate(1.5f, 0, -1);
        teapot0.setSubdivs(20);
        teapot0.setup(api);

        SphereParameter sphere0 = new SphereParameter("sphere_0");
        sphere0.shaders(glassy);
        sphere0.rotateX(35);
        sphere0.scale(1.5f);
        sphere0.rotateY(245);
        sphere0.translate(1.5f, 1.5f, 3);
        sphere0.setup(api);

        SphereParameter sphere1 = new SphereParameter("sphere_1");
        sphere1.shaders(shiny);
        sphere1.modifiers(bumpy05);
        sphere1.rotateX(35);
        sphere1.scale(1.5f);
        sphere1.rotateY(245);
        sphere1.translate(1.5f, 1.5f, -5);
        sphere1.setup(api);

        TeapotParameter teapot1 = new TeapotParameter("teapot_1");
        teapot1.geometry(teapot0);
        teapot1.rotateX(-90);
        teapot1.scale(0.018f);
        teapot1.rotateY(245f);
        teapot1.translate(-1.5f, 0, -3);
        teapot1.shaders(simpleYellow);
        teapot1.modifiers(bumpy04);
        teapot1.setup(api);

        TeapotParameter teapot3 = new TeapotParameter("teapot_3");
        teapot3.geometry(teapot0);
        teapot3.shaders(simpleRed);
        teapot3.modifiers(bumpy02);
        teapot3.rotateX(-90);
        teapot3.scale(0.018f);
        teapot3.rotateY(245f);
        teapot3.translate(-1.5f, 0, 1);
        teapot3.setup(api);
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
