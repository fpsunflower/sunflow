This is a simple Maya exporter for Sunflow. Load the plugin via "Window/Settings/Plug-in Manager ...". Pick the file that corresponds to your OS and Maya version (by the folder and last two digits).

The exporter is invoked by typing:

sunflowExport "c:/path/to/your_scene.sc";

in a MEL window. Make sure you use forward slashes for the filename.


The exporter is still very basic. Here is what is supported so far:

 * Polygon meshes
 * Instancing
 * Multiple shaders per mesh
 * Lambert shaders (Phong,Blinn, etc ... will be read as lambert)
 * Maya Area lights - number of samples is taken from the raytrace shadow setting, color and intensity are exported
 * Maya directional light - the first one in your scene will be translated into a Sun/Sky dome. The number of samples is set by the raytrace shadow settings (32+ recommended) - color/intensity is ignored.
 * Perspective cameras. All cameras flagged as "renderable" will be exported. Only the last one to be exported will be used for rendering.

All other options can be tweaked inside the exported file. Please refer to the example scenes for syntax examples.
