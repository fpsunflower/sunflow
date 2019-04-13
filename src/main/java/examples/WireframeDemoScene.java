package examples;

import org.sunflow.PluginRegistry;
import org.sunflow.SunflowAPI;
import org.sunflow.core.ShadingState;
import org.sunflow.core.parameter.ImageParameter;
import org.sunflow.core.shader.WireframeShader;
import org.sunflow.image.Color;

public class WireframeDemoScene {

    public static void main(String[] args) {
        SunflowAPI api = new SunflowAPI();
        api.reset();

        // Loading a custom procedural shader
        PluginRegistry.shaderPlugins.registerPlugin("custom_wireframe", CustomWireShader.class);
        api.parameter("width", (float) (Math.PI * 0.5 / 8192));
        api.shader("ao_wire", "custom_wireframe");

        // Including a scene
        GumboAndTeapotScene.buildScene(api);

        // Overriding existent shaders
        api.parameter("override.shader", "ao_wire");
        api.parameter("override.photons", true);

        ImageParameter image = new ImageParameter();
        image.setResolutionX(320);
        image.setResolutionY(240);
        image.setAAMin(2);
        image.setAAMax(2);
        image.setFilter(ImageParameter.FILTER_BLACKMAN_HARRIS);
        image.setup(api);

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

    public static class CustomWireShader extends WireframeShader {
        // set to false to overlay wires on regular shaders
        private boolean ambocc = true;

        public Color getFillColor(ShadingState state) {
            return ambocc ? state.occlusion(16, 6.0f) : state.getShader().getRadiance(state);
        }
    }

}
