package org.sunflow.core.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import org.sunflow.SunflowAPI;
import org.sunflow.core.SceneParser;
import org.sunflow.core.Shader;
import org.sunflow.core.primitive.TriangleMesh;
import org.sunflow.core.shader.SimpleShader;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class RA3Parser implements SceneParser {
    public boolean parse(String filename, SunflowAPI api) {
        try {
            UI.printInfo(Module.USER, "RA3 - Reading geometry: \"%s\" ...", filename);
            File file = new File(filename);
            FileInputStream stream = new FileInputStream(filename);
            MappedByteBuffer map = stream.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            map.order(ByteOrder.LITTLE_ENDIAN);
            IntBuffer ints = map.asIntBuffer();
            FloatBuffer buffer = map.asFloatBuffer();
            int numVerts = ints.get(0);
            int numTris = ints.get(1);
            UI.printInfo(Module.USER, "RA3 -   * Reading %d vertices ...", numVerts);
            float[] verts = new float[3 * numVerts];
            for (int i = 0; i < verts.length; i++)
                verts[i] = buffer.get(2 + i);
            UI.printInfo(Module.USER, "RA3 -   * Reading %d triangles ...", numTris);
            int[] tris = new int[3 * numTris];
            for (int i = 0; i < tris.length; i++)
                tris[i] = ints.get(2 + verts.length + i);
            stream.close();
            UI.printInfo(Module.USER, "RA3 -   * Creating mesh ...");

            // create geometry
            api.parameter("triangles", tris);
            api.parameter("points", "point", "vertex", verts);
            api.geometry(filename, new TriangleMesh());

            // create shader
            Shader s = api.lookupShader("ra3shader");
            if (s == null) {
                // create default shader
                api.shader(filename + ".shader", new SimpleShader());
                api.parameter("shaders", filename + ".shader");
            } else {
                // reuse existing shader
                api.parameter("shaders", "ra3shader");
            }

            // create instance
            api.instance(filename + ".instance", filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}