package org.sunflow.core.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import org.sunflow.SunflowAPI;
import org.sunflow.core.SceneParser;
import org.sunflow.core.camera.PinholeLens;
import org.sunflow.core.primitive.TriangleMesh;
import org.sunflow.core.shader.SimpleShader;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.Parser;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class RA2Parser implements SceneParser {
    public boolean parse(String filename, SunflowAPI api) {
        try {
            UI.printInfo(Module.USER, "RA2 - Reading geometry: \"%s\" ...", filename);
            File file = new File(filename);
            FileInputStream stream = new FileInputStream(filename);
            MappedByteBuffer map = stream.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            map.order(ByteOrder.LITTLE_ENDIAN);
            FloatBuffer buffer = map.asFloatBuffer();
            float[] data = new float[buffer.capacity()];
            for (int i = 0; i < data.length; i++)
                data[i] = buffer.get(i);
            stream.close();
            api.parameter("points", "point", "vertex", data);
            int[] triangles = new int[3 * (data.length / 9)];
            for (int i = 0; i < triangles.length; i++)
                triangles[i] = i;
            // create geo
            api.parameter("triangles", triangles);
            api.geometry(filename, new TriangleMesh());
            // create shader
            api.shader(filename + ".shader", new SimpleShader());
            // create instance
            api.parameter("shaders", filename + ".shader");
            api.instance(filename + ".instance", filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        try {
            filename = filename.replace(".ra2", ".txt");
            UI.printInfo(Module.USER, "RA2 - Reading camera  : \"%s\" ...", filename);
            Parser p = new Parser(filename);
            Point3 eye = new Point3();
            eye.x = p.getNextFloat();
            eye.y = p.getNextFloat();
            eye.z = p.getNextFloat();
            Point3 to = new Point3();
            to.x = p.getNextFloat();
            to.y = p.getNextFloat();
            to.z = p.getNextFloat();
            Vector3 up = new Vector3();
            switch (p.getNextInt()) {
                case 0:
                    up.set(1, 0, 0);
                    break;
                case 1:
                    up.set(0, 1, 0);
                    break;
                case 2:
                    up.set(0, 0, 1);
                    break;
                default:
                    UI.printWarning(Module.USER, "RA2 - Invalid up vector specification - using Z axis");
                    up.set(0, 0, 1);
                    break;
            }
            api.parameter("eye", eye);
            api.parameter("target", to);
            api.parameter("up", up);
            String name = api.getUniqueName("camera");
            api.parameter("fov", 80f);
            api.camera(name, new PinholeLens());
            api.parameter("camera", name);
            api.parameter("resolutionX", 1024);
            api.parameter("resolutionY", 1024);
            api.options(SunflowAPI.DEFAULT_OPTIONS);
            p.close();
        } catch (FileNotFoundException e) {
            UI.printWarning(Module.USER, "RA2 - Camera file not found");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}