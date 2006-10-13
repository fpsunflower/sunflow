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
import org.sunflow.core.Geometry;
import org.sunflow.core.Instance;
import org.sunflow.core.SceneParser;
import org.sunflow.core.camera.PinholeCamera;
import org.sunflow.core.primitive.Mesh;
import org.sunflow.core.shader.SimpleShader;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.Parser;
import org.sunflow.system.UI;

public class RA2Parser implements SceneParser {
    public boolean parse(String filename, SunflowAPI api) {
        try {
            UI.printInfo("[RA2] Reading geometry: \"%s\" ...", filename);
            File file = new File(filename);
            FileInputStream stream = new FileInputStream(filename);
            MappedByteBuffer map = stream.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            map.order(ByteOrder.LITTLE_ENDIAN);
            FloatBuffer buffer = map.asFloatBuffer();
            float[] data = new float[buffer.capacity()];
            for (int i = 0; i < data.length; i++)
                data[i] = buffer.get(i);
            Mesh mesh = new Mesh(data, null);
            Geometry geo = new Geometry(mesh);
            Instance instance = new Instance(new SimpleShader(), null, geo);
            api.instance(instance);
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        try {
            filename = filename.replace(".ra2", ".txt");
            UI.printInfo("[RA2] Reading camera  : \"%s\" ...", filename);
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
                    UI.printWarning("[RA2] Invalid up vector specification - using Z axis");
                    up.set(0, 0, 1);
                    break;
            }
            api.camera(new PinholeCamera(eye, to, up, 80.0f, 1.0f));
            api.resolution(1024, 1024);
            p.close();
        } catch (FileNotFoundException e) {
            UI.printWarning("[RA2] Camera file not found");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}