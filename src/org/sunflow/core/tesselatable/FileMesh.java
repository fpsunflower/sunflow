package org.sunflow.core.tesselatable;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import org.sunflow.SunflowAPI;
import org.sunflow.core.ParameterList;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Tesselatable;
import org.sunflow.core.ParameterList.InterpolationType;
import org.sunflow.core.primitive.TriangleMesh;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;
import org.sunflow.system.Memory;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;
import org.sunflow.util.FloatArray;
import org.sunflow.util.IntArray;

public class FileMesh implements Tesselatable {
    String filename = null;

    public BoundingBox getWorldBounds(Matrix4 o2w) {
        // world bounds can't be computed without reading file
        // return null so the mesh will be loaded right away
        return null;
    }

    public PrimitiveList tesselate() {
        if (filename.endsWith(".ra3")) {
            try {
                UI.printInfo(Module.GEOM, "RA3 - Reading geometry: \"%s\" ...", filename);
                File file = new File(filename);
                FileInputStream stream = new FileInputStream(filename);
                MappedByteBuffer map = stream.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
                map.order(ByteOrder.LITTLE_ENDIAN);
                IntBuffer ints = map.asIntBuffer();
                FloatBuffer buffer = map.asFloatBuffer();
                int numVerts = ints.get(0);
                int numTris = ints.get(1);
                UI.printInfo(Module.GEOM, "RA3 -   * Reading %d vertices ...", numVerts);
                float[] verts = new float[3 * numVerts];
                for (int i = 0; i < verts.length; i++)
                    verts[i] = buffer.get(2 + i);
                UI.printInfo(Module.GEOM, "RA3 -   * Reading %d triangles ...", numTris);
                int[] tris = new int[3 * numTris];
                for (int i = 0; i < tris.length; i++)
                    tris[i] = ints.get(2 + verts.length + i);
                stream.close();
                UI.printInfo(Module.GEOM, "RA3 -   * Creating mesh ...");
                // create geometry
                ParameterList pl = new ParameterList();
                pl.addIntegerArray("triangles", tris);
                pl.addPoints("points", InterpolationType.VERTEX, verts);
                TriangleMesh m = new TriangleMesh();
                if (m.update(pl, null))
                    return m;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                UI.printError(Module.GEOM, "Unable to read mesh file \"%s\" - file not found", filename);
            } catch (IOException e) {
                e.printStackTrace();
                UI.printError(Module.GEOM, "Unable to read mesh file \"%s\" - I/O error occured", filename);
            }
        } else if (filename.endsWith(".obj")) {
            int lineNumber = 1;
            try {
                UI.printInfo(Module.GEOM, "OBJ - Reading geometry: \"%s\" ...", filename);
                FloatArray verts = new FloatArray();
                IntArray tris = new IntArray();
                FileReader file = new FileReader(filename);
                BufferedReader bf = new BufferedReader(file);
                String line;
                while ((line = bf.readLine()) != null) {
                    if (line.startsWith("v")) {
                        String[] v = line.split("\\s+");
                        verts.add(Float.parseFloat(v[1]));
                        verts.add(Float.parseFloat(v[2]));
                        verts.add(Float.parseFloat(v[3]));
                    } else if (line.startsWith("f")) {
                        String[] f = line.split("\\s+");
                        if (f.length == 5) {
                            tris.add(Integer.parseInt(f[1]) - 1);
                            tris.add(Integer.parseInt(f[2]) - 1);
                            tris.add(Integer.parseInt(f[3]) - 1);
                            tris.add(Integer.parseInt(f[1]) - 1);
                            tris.add(Integer.parseInt(f[3]) - 1);
                            tris.add(Integer.parseInt(f[4]) - 1);
                        } else if (f.length == 4) {
                            tris.add(Integer.parseInt(f[1]) - 1);
                            tris.add(Integer.parseInt(f[2]) - 1);
                            tris.add(Integer.parseInt(f[3]) - 1);
                        }
                    }
                    if (lineNumber % 100000 == 0)
                        UI.printInfo(Module.GEOM, "OBJ -   * Parsed %7d lines ...", lineNumber);
                    lineNumber++;
                }
                file.close();
                UI.printInfo(Module.GEOM, "OBJ -   * Creating mesh ...");
                ParameterList pl = new ParameterList();
                pl.addIntegerArray("triangles", tris.trim());
                pl.addPoints("points", InterpolationType.VERTEX, verts.trim());
                TriangleMesh m = new TriangleMesh();
                if (m.update(pl, null))
                    return m;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                UI.printError(Module.GEOM, "Unable to read mesh file \"%s\" - file not found", filename);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                UI.printError(Module.GEOM, "Unable to read mesh file \"%s\" - syntax error at line %d", lineNumber);
            } catch (IOException e) {
                e.printStackTrace();
                UI.printError(Module.GEOM, "Unable to read mesh file \"%s\" - I/O error occured", filename);
            }
        } else if (filename.endsWith(".stl")) {
            try {
                UI.printInfo(Module.GEOM, "STL - Reading geometry: \"%s\" ...", filename);
                FileInputStream file = new FileInputStream(filename);
                DataInputStream stream = new DataInputStream(new BufferedInputStream(file));
                file.skip(80);
                int numTris = getLittleEndianInt(stream.readInt());
                UI.printInfo(Module.GEOM, "STL -   * Reading %d triangles ...", numTris);
                long filesize = new File(filename).length();
                if (filesize != (84 + 50 * numTris)) {
                    UI.printWarning(Module.GEOM, "STL - Size of file mismatch (expecting %s, found %s)", Memory.bytesToString(84 + 14 * numTris), Memory.bytesToString(filesize));
                    return null;
                }
                int[] tris = new int[3 * numTris];
                float[] verts = new float[9 * numTris];
                for (int i = 0, i3 = 0, index = 0; i < numTris; i++, i3 += 3) {
                    // skip normal
                    stream.readInt();
                    stream.readInt();
                    stream.readInt();
                    for (int j = 0; j < 3; j++, index += 3) {
                        tris[i3 + j] = i3 + j;
                        // get xyz
                        verts[index + 0] = getLittleEndianFloat(stream.readInt());
                        verts[index + 1] = getLittleEndianFloat(stream.readInt());
                        verts[index + 2] = getLittleEndianFloat(stream.readInt());
                    }
                    stream.readShort();
                    if ((i + 1) % 100000 == 0)
                        UI.printInfo(Module.GEOM, "STL -   * Parsed %7d triangles ...", i + 1);
                }
                file.close();
                // create geometry
                UI.printInfo(Module.GEOM, "STL -   * Creating mesh ...");
                ParameterList pl = new ParameterList();
                pl.addIntegerArray("triangles", tris);
                pl.addPoints("points", InterpolationType.VERTEX, verts);
                TriangleMesh m = new TriangleMesh();
                if (m.update(pl, null))
                    return m;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                UI.printError(Module.GEOM, "Unable to read mesh file \"%s\" - file not found", filename);
            } catch (IOException e) {
                e.printStackTrace();
                UI.printError(Module.GEOM, "Unable to read mesh file \"%s\" - I/O error occured", filename);
            }
        } else
            UI.printWarning(Module.GEOM, "Unable to read mesh file \"%s\" - unrecognized format", filename);
        return null;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        String file = pl.getString("filename", null);
        if (file != null)
            filename = api.resolveIncludeFilename(file);
        return filename != null;
    }

    private int getLittleEndianInt(int i) {
        // input integer has its bytes in big endian byte order
        // swap them around
        return (i >>> 24) | ((i >>> 8) & 0xFF00) | ((i << 8) & 0xFF0000) | (i << 24);
    }

    private float getLittleEndianFloat(int i) {
        // input integer has its bytes in big endian byte order
        // swap them around and interpret data as floating point
        return Float.intBitsToFloat(getLittleEndianInt(i));
    }
}