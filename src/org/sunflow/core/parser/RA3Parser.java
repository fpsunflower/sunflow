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
import org.sunflow.core.ShadingState;
import org.sunflow.core.primitive.Mesh;
import org.sunflow.image.Color;
import org.sunflow.system.UI;

public class RA3Parser implements SceneParser {
    public boolean parse(String filename, SunflowAPI api) {
        try {
            UI.printInfo("[RA3] Reading geometry: \"%s\" ...", filename);
            File file = new File(filename);
            FileInputStream stream = new FileInputStream(filename);
            MappedByteBuffer map = stream.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            map.order(ByteOrder.LITTLE_ENDIAN);
            IntBuffer ints = map.asIntBuffer();
            FloatBuffer buffer = map.asFloatBuffer();
            int numVerts = ints.get(0);
            int numTris = ints.get(1);
            UI.printInfo("[RA3]   * Reading %d vertices ...", numVerts);
            float[] verts = new float[3 * numVerts];
            for (int i = 0; i < verts.length; i++)
                verts[i] = buffer.get(2 + i);
            UI.printInfo("[RA3]   * Reading %d triangles ...", numTris);
            int[] tris = new int[3 * numTris];
            for (int i = 0; i < tris.length; i++)
                tris[i] = ints.get(2 + verts.length + i);
            UI.printInfo("[RA3]   * Creating mesh ...");
            Mesh mesh = new Mesh();
            mesh.points(verts);
            mesh.triangles(tris);
            Shader s = api.shader("ra3shader");
            mesh.shader(s == null ? new SimpleShader() : s);
            api.mesh(mesh);
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static class SimpleShader implements Shader {
        public Color getRadiance(ShadingState state) {
            return new Color(Math.abs(state.getRay().dot(state.getNormal())));
        }

        public void scatterPhoton(ShadingState state, Color power) {}
    }
}