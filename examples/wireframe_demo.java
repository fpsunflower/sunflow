import org.sunflow.core.ShadingState;
import org.sunflow.core.shader.WireframeShader;
import org.sunflow.image.Color;

public void build() {
    parameter("width", (float) (Math.PI * 0.5 / 8192));
    shader("ao_wire", new WireframeShader() {
        public boolean ambocc = true;
        
        public Color getFillColor(ShadingState state) {
            return ambocc ? state.occlusion(16, 6.0f) : state.getShader().getRadiance(state);
        }
    });
    shaderOverride("ao_wire", true);
    parse("gumbo_and_teapot.sc");
    parameter("aa.min", 2);
    parameter("aa.max", 2);
    parameter("filter", "blackman-harris");
    options(DEFAULT_OPTIONS);
}