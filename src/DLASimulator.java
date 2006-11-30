import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import org.sunflow.SunflowAPI;
import org.sunflow.core.Display;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Ray;
import org.sunflow.core.camera.PinholeLens;
import org.sunflow.core.display.FileDisplay;
import org.sunflow.core.display.FrameDisplay;
import org.sunflow.core.primitive.DLASurface;
import org.sunflow.core.shader.AmbientOcclusionShader;
import org.sunflow.core.shader.SimpleShader;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.MathUtils;
import org.sunflow.math.Point3;
import org.sunflow.math.Solvers;
import org.sunflow.math.Vector3;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;
import org.sunflow.util.FloatArray;
import org.sunflow.util.IntArray;

public class DLASimulator {
    public static void main(String[] args) {
        SunflowAPI api = SunflowAPI.create(null);
        api.shader("ao", new AmbientOcclusionShader());
        final float size = 5;
        final float radius = 0.03f;
        final int numFrames = 24 * 16;
        Display display = null;
        if (args.length == 0) {
            final int numParticles = 1000000;
            DLAParticleGrid grid = new DLAParticleGrid(new BoundingBox(size), numParticles, radius);
            grid.addParticle(0, 0, 0); // add a particle right in the center
            Timer timer = new Timer();
            timer.start();
            int delta = 100;
            double last = 0;
            for (int i = 1; i < numParticles; i++) {
                if (i % delta == 0) {
                    timer.end();
                    double sec = timer.seconds();
                    if (sec - last < 5)
                        delta *= 10;
                    last = sec;
                    sec = (sec / i) * (numParticles - i);
                    UI.printInfo(Module.USER, "Simulating particle %8d (%s elapsed - %s remaining)...", i, timer, Timer.toString(sec));
                }
                // start with a random seed on the boundary of the surface
                double rx = Math.random();
                double ry = Math.random();
                int side = (int) (rx * 6);
                rx = rx * 6 - side;
                float ox, oy, oz;
                switch (side) {
                    case 0:
                        ox = -size;
                        oy = (float) (2 * size * rx - size);
                        oz = (float) (2 * size * ry - size);
                        break;
                    case 1:
                        ox = +size;
                        oy = (float) (2 * size * rx - size);
                        oz = (float) (2 * size * ry - size);
                        break;
                    case 2:
                        oy = -size;
                        oz = (float) (2 * size * rx - size);
                        ox = (float) (2 * size * ry - size);
                        break;
                    case 3:
                        oy = +size;
                        oz = (float) (2 * size * rx - size);
                        ox = (float) (2 * size * ry - size);
                        break;
                    case 4:
                        oz = -size;
                        ox = (float) (2 * size * rx - size);
                        oy = (float) (2 * size * ry - size);
                        break;
                    case 5:
                    default:
                        oz = +size;
                        ox = (float) (2 * size * rx - size);
                        oy = (float) (2 * size * ry - size);
                        break;
                }
                boolean stored = false;
                for (int iter = 0; iter < 1000000; iter++) {
                    double s, a;
                    double dx, dy, dz;
                    do {
                        dx = -1 + 2 * Math.random();
                        dy = -1 + 2 * Math.random();
                        s = (dx) * (dx) + (dy) * (dy);
                    } while (s > 1.0);
                    dz = -1 + 2 * s;
                    a = 2 * Math.sqrt(1 - s);
                    dx *= a;
                    dy *= a;

                    // bounce backwards off the bounds
                    if (ox + dx < -size || ox + dx > +size)
                        dx = -dx;
                    if (oy + dy < -size || oy + dy > +size)
                        dy = -dy;
                    if (oz + dz < -size || oz + dz > +size)
                        dz = -dz;

                    if (!grid.isInside((float) (ox + dx), (float) (oy + dy), (float) (oz + dz))) {
                        UI.printWarning(Module.USER, "Particle %d escaped bounds on iteration %d!", i, iter);
                        UI.printWarning(Module.USER, "Particle pos: (%7.3f, %7.3f, %7.3f)", ox, oy, oz);
                        UI.printWarning(Module.USER, "Particle dir: (%7.3f, %7.3f, %7.3f)", dx, dy, dz);
                        UI.printWarning(Module.USER, "Particle dst: (%7.3f, %7.3f, %7.3f)", ox + dx, oy + dy, oz + dz);
                        break;
                    }

                    Ray r = new Ray(ox, oy, oz, (float) dx, (float) dy, (float) dz);
                    r.setMax(1);
                    float t = grid.intersect(r);
                    if (t > 0 && t < 1) {
                        // found a hit! we can store the particle
                        float px = (float) (ox + t * dx);
                        float py = (float) (oy + t * dy);
                        float pz = (float) (oz + t * dz);
                        grid.addParticle(px, py, pz);
                        stored = true;
                        break;
                    } else {
                        ox += dx;
                        oy += dy;
                        oz += dz;
                    }
                }
                if (!stored)
                    UI.printWarning(Module.USER, "Particle %d couldn't be stored", i);
            }
            timer.end();
            UI.printInfo(Module.USER, "Particle tracing took: %s", timer);
            UI.printInfo(Module.USER, "Writing particles to file ...");

            try {
                FileOutputStream file = new FileOutputStream("/home/ckulla/Desktop/particles.dla");
                DataOutputStream stream = new DataOutputStream(file);
                for (float p : grid.particles.trim())
                    stream.writeFloat(p);
                file.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            UI.printInfo(Module.USER, "Writing particles to file done.");
            api.parameter("particles", "point", "vertex", grid.particles.trim());
            api.parameter("num", grid.particles.getSize() / 3);
            api.parameter("radius", radius);
            api.geometry("particles.geo", new DLASurface());
            display = new FrameDisplay();
        } else if (args.length == 2 || args.length == 3) {
            String filename = args[0];
            int frameNumber = Integer.parseInt(args[1]);
            UI.printInfo(Module.USER, "Loading particle file: %s", filename);
            int numParticles = (int) (new File(filename).length() / 12);
            int n = Math.min((numParticles + numFrames - 1) / numFrames * frameNumber, numParticles);
            try {
                File file = new File(filename);
                FileInputStream stream = new FileInputStream(filename);
                MappedByteBuffer map = stream.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
                FloatBuffer buffer = map.asFloatBuffer();
                float[] data = new float[buffer.capacity()];
                for (int i = 0; i < data.length; i++)
                    data[i] = buffer.get(i);
                stream.close();
                api.parameter("particles", "point", "vertex", data);
                api.parameter("num", n);
                api.parameter("radius", radius);
                api.geometry("particles.geo", new DLASurface());
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            if (args.length > 2)
                display = new FileDisplay(args[2]);
            else
                display = new FrameDisplay();
        } else if (args.length == 1) {
            String filename = args[0];
            UI.printInfo(Module.USER, "Loading particle file: %s", filename);
            int numParticles = (int) (new File(filename).length() / 12);
            UI.printInfo(Module.USER, "Found %d particles ...", numParticles);
            try {
                File file = new File(filename);
                FileInputStream stream = new FileInputStream(filename);
                MappedByteBuffer map = stream.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
                FloatBuffer buffer = map.asFloatBuffer();
                float[] data = new float[buffer.capacity()];
                for (int i = 0; i < data.length; i++)
                    data[i] = buffer.get(i);
                stream.close();
                api.parameter("particles", "point", "vertex", data);
                api.parameter("num", 1);
                api.parameter("radius", radius);
                api.geometry("particles.geo", new DLASurface());
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            api.shader("quick", new SimpleShader());
            api.parameter("shaders", new String[] { "quick" });
            api.instance("particles.instance", "particles.geo");
            api.parameter("target", new Point3(0, 0, 0));
            api.parameter("eye", new Point3(0, 0, -size * 2));
            api.parameter("up", new Vector3(0, 1, 0));
            api.parameter("fov", 90.0f);
            api.parameter("aspect", 1.0f);
            api.camera("cam", new PinholeLens());
            api.parameter("aa.min", 0);
            api.parameter("aa.max", 2);
            api.filter("mitchell");
            api.parameter("resolutionX", 1024);
            api.parameter("resolutionY", 1024);
            api.parameter("camera", "cam");
            api.options(SunflowAPI.DEFAULT_OPTIONS);
            for (int frameNumber = 1; frameNumber <= numFrames; frameNumber++) {
                int n = Math.min((numParticles + numFrames - 1) / numFrames * frameNumber, numParticles);
                api.parameter("num", n);
                api.geometry("particles.geo", (PrimitiveList) null); // update
                api.render(SunflowAPI.DEFAULT_OPTIONS, new FileDisplay(String.format("%s/file.%04d.png", new File(filename).getAbsoluteFile().getParent(), frameNumber)));
            }
        } else {
            System.exit(1);
        }
        api.parameter("shaders", new String[] { "ao" });
        api.instance("particles.instance", "particles.geo");
        api.parameter("target", new Point3(0, 0, 0));
        api.parameter("eye", new Point3(0, 0, -size * 2));
        api.parameter("up", new Vector3(0, 1, 0));
        api.parameter("fov", 90.0f);
        api.parameter("aspect", 1.0f);
        api.camera("cam", new PinholeLens());
        api.parameter("aa.min", 0);
        api.parameter("aa.max", 2);
        api.filter("mitchell");
        api.parameter("resolutionX", 1024);
        api.parameter("resolutionY", 1024);
        api.parameter("camera", "cam");
        api.options(SunflowAPI.DEFAULT_OPTIONS);
        api.render(SunflowAPI.DEFAULT_OPTIONS, display);
    }

    private static class DLAParticleGrid {
        private IntArray[] voxels;
        private FloatArray particles;
        private float r, r2; // particle radius
        private BoundingBox bounds;
        private int nx, ny, nz;
        private float voxelwx, voxelwy, voxelwz;
        private float invVoxelwx, invVoxelwy, invVoxelwz;

        DLAParticleGrid(BoundingBox bounds, int np, float radius) {
            this.bounds = bounds;
            bounds.enlargeUlps();
            Vector3 w = bounds.getExtents();
            double s = Math.pow((w.x * w.y * w.z) / np, 1 / 3.0);
            nx = MathUtils.clamp((int) ((w.x / s) + 0.5), 1, 256);
            ny = MathUtils.clamp((int) ((w.y / s) + 0.5), 1, 256);
            nz = MathUtils.clamp((int) ((w.z / s) + 0.5), 1, 256);
            voxelwx = w.x / nx;
            voxelwy = w.y / ny;
            voxelwz = w.z / nz;
            invVoxelwx = 1 / voxelwx;
            invVoxelwy = 1 / voxelwy;
            invVoxelwz = 1 / voxelwz;
            r = radius;
            r2 = r * r;
            voxels = new IntArray[nx * ny * nz];
            particles = new FloatArray(np * 3);

        }

        boolean isInside(float x, float y, float z) {
            return bounds.contains(x, y, z);
        }

        void addParticle(float x, float y, float z) {
            // add particle to list
            int pid = particles.getSize() / 3;
            particles.add(x);
            particles.add(y);
            particles.add(z);
            // add particle to grid
            int[] imin = new int[3];
            int[] imax = new int[3];
            getGridIndex(x - r, y - r, z - r, imin);
            getGridIndex(x + r, y + r, z + r, imax);
            for (int ix = imin[0]; ix <= imax[0]; ix++) {
                for (int iy = imin[1]; iy <= imax[1]; iy++) {
                    for (int iz = imin[2]; iz <= imax[2]; iz++) {
                        int idx = ix + (nx * iy) + (nx * ny * iz);
                        // TODO: add sphere/box intersection test
                        if (voxels[idx] == null)
                            voxels[idx] = new IntArray(4);
                        voxels[idx].add(pid);
                    }
                }
            }
        }

        public float intersect(Ray r) {
            float intervalMin = r.getMin();
            float intervalMax = r.getMax();
            float orgX = r.ox;
            float dirX = r.dx, invDirX = 1 / dirX;
            float t1, t2;
            t1 = (bounds.getMinimum().x - orgX) * invDirX;
            t2 = (bounds.getMaximum().x - orgX) * invDirX;
            if (invDirX > 0) {
                if (t1 > intervalMin)
                    intervalMin = t1;
                if (t2 < intervalMax)
                    intervalMax = t2;
            } else {
                if (t2 > intervalMin)
                    intervalMin = t2;
                if (t1 < intervalMax)
                    intervalMax = t1;
            }
            if (intervalMin > intervalMax)
                return 0;
            float orgY = r.oy;
            float dirY = r.dy, invDirY = 1 / dirY;
            t1 = (bounds.getMinimum().y - orgY) * invDirY;
            t2 = (bounds.getMaximum().y - orgY) * invDirY;
            if (invDirY > 0) {
                if (t1 > intervalMin)
                    intervalMin = t1;
                if (t2 < intervalMax)
                    intervalMax = t2;
            } else {
                if (t2 > intervalMin)
                    intervalMin = t2;
                if (t1 < intervalMax)
                    intervalMax = t1;
            }
            if (intervalMin > intervalMax)
                return 0;
            float orgZ = r.oz;
            float dirZ = r.dz, invDirZ = 1 / dirZ;
            t1 = (bounds.getMinimum().z - orgZ) * invDirZ;
            t2 = (bounds.getMaximum().z - orgZ) * invDirZ;
            if (invDirZ > 0) {
                if (t1 > intervalMin)
                    intervalMin = t1;
                if (t2 < intervalMax)
                    intervalMax = t2;
            } else {
                if (t2 > intervalMin)
                    intervalMin = t2;
                if (t1 < intervalMax)
                    intervalMax = t1;
            }
            if (intervalMin > intervalMax)
                return 0;
            // box is hit at [intervalMin, intervalMax]
            orgX += intervalMin * dirX;
            orgY += intervalMin * dirY;
            orgZ += intervalMin * dirZ;
            // locate starting point inside the grid
            // and set up 3D-DDA vars
            int indxX, indxY, indxZ;
            int stepX, stepY, stepZ;
            int stopX, stopY, stopZ;
            float deltaX, deltaY, deltaZ;
            float tnextX, tnextY, tnextZ;
            // stepping factors along X
            indxX = (int) ((orgX - bounds.getMinimum().x) * invVoxelwx);
            if (indxX < 0)
                indxX = 0;
            else if (indxX >= nx)
                indxX = nx - 1;
            if (Math.abs(dirX) < 1e-6f) {
                stepX = 0;
                stopX = indxX;
                deltaX = 0;
                tnextX = Float.POSITIVE_INFINITY;
            } else if (dirX > 0) {
                stepX = 1;
                stopX = nx;
                deltaX = voxelwx * invDirX;
                tnextX = intervalMin + ((indxX + 1) * voxelwx + bounds.getMinimum().x - orgX) * invDirX;
            } else {
                stepX = -1;
                stopX = -1;
                deltaX = -voxelwx * invDirX;
                tnextX = intervalMin + (indxX * voxelwx + bounds.getMinimum().x - orgX) * invDirX;
            }
            // stepping factors along Y
            indxY = (int) ((orgY - bounds.getMinimum().y) * invVoxelwy);
            if (indxY < 0)
                indxY = 0;
            else if (indxY >= ny)
                indxY = ny - 1;
            if (Math.abs(dirY) < 1e-6f) {
                stepY = 0;
                stopY = indxY;
                deltaY = 0;
                tnextY = Float.POSITIVE_INFINITY;
            } else if (dirY > 0) {
                stepY = 1;
                stopY = ny;
                deltaY = voxelwy * invDirY;
                tnextY = intervalMin + ((indxY + 1) * voxelwy + bounds.getMinimum().y - orgY) * invDirY;
            } else {
                stepY = -1;
                stopY = -1;
                deltaY = -voxelwy * invDirY;
                tnextY = intervalMin + (indxY * voxelwy + bounds.getMinimum().y - orgY) * invDirY;
            }
            // stepping factors along Z
            indxZ = (int) ((orgZ - bounds.getMinimum().z) * invVoxelwz);
            if (indxZ < 0)
                indxZ = 0;
            else if (indxZ >= nz)
                indxZ = nz - 1;
            if (Math.abs(dirZ) < 1e-6f) {
                stepZ = 0;
                stopZ = indxZ;
                deltaZ = 0;
                tnextZ = Float.POSITIVE_INFINITY;
            } else if (dirZ > 0) {
                stepZ = 1;
                stopZ = nz;
                deltaZ = voxelwz * invDirZ;
                tnextZ = intervalMin + ((indxZ + 1) * voxelwz + bounds.getMinimum().z - orgZ) * invDirZ;
            } else {
                stepZ = -1;
                stopZ = -1;
                deltaZ = -voxelwz * invDirZ;
                tnextZ = intervalMin + (indxZ * voxelwz + bounds.getMinimum().z - orgZ) * invDirZ;
            }
            int cellstepX = stepX;
            int cellstepY = stepY * nx;
            int cellstepZ = stepZ * ny * nx;
            int cell = indxX + indxY * nx + indxZ * ny * nx;
            // trace through the grid
            for (;;) {
                if (tnextX < tnextY && tnextX < tnextZ) {
                    if (voxels[cell] != null) {
                        boolean hit = false;
                        for (int i = 0; i < voxels[cell].getSize(); i++) {
                            int i3 = 3 * voxels[cell].get(i);
                            float ocx = r.ox - particles.get(i3 + 0);
                            float ocy = r.oy - particles.get(i3 + 1);
                            float ocz = r.oz - particles.get(i3 + 2);
                            float qa = r.dx * r.dx + r.dy * r.dy + r.dz * r.dz;
                            float qb = 2 * ((r.dx * ocx) + (r.dy * ocy) + (r.dz * ocz));
                            float qc = ((ocx * ocx) + (ocy * ocy) + (ocz * ocz)) - r2;
                            double[] t = Solvers.solveQuadric(qa, qb, qc);
                            if (t != null) {
                                // early rejection
                                if (t[0] >= r.getMax() || t[1] <= r.getMin())
                                    continue;
                                if (t[0] > r.getMin())
                                    r.setMax((float) t[0]);
                                else
                                    r.setMax((float) t[1]);
                                hit = true;
                            }
                        }
                        if (hit && (r.getMax() < tnextX && r.getMax() < intervalMax))
                            return r.getMax();
                    }
                    intervalMin = tnextX;
                    if (intervalMin > intervalMax)
                        return 0;
                    indxX += stepX;
                    if (indxX == stopX)
                        return 0;
                    tnextX += deltaX;
                    cell += cellstepX;
                } else if (tnextY < tnextZ) {
                    if (voxels[cell] != null) {
                        boolean hit = false;
                        for (int i = 0; i < voxels[cell].getSize(); i++) {
                            int i3 = 3 * voxels[cell].get(i);
                            float ocx = r.ox - particles.get(i3 + 0);
                            float ocy = r.oy - particles.get(i3 + 1);
                            float ocz = r.oz - particles.get(i3 + 2);
                            float qa = r.dx * r.dx + r.dy * r.dy + r.dz * r.dz;
                            float qb = 2 * ((r.dx * ocx) + (r.dy * ocy) + (r.dz * ocz));
                            float qc = ((ocx * ocx) + (ocy * ocy) + (ocz * ocz)) - r2;
                            double[] t = Solvers.solveQuadric(qa, qb, qc);
                            if (t != null) {
                                // early rejection
                                if (t[0] >= r.getMax() || t[1] <= r.getMin())
                                    continue;
                                if (t[0] > r.getMin())
                                    r.setMax((float) t[0]);
                                else
                                    r.setMax((float) t[1]);
                                hit = true;
                            }
                        }
                        if (hit && (r.getMax() < tnextY && r.getMax() < intervalMax))
                            return r.getMax();
                    }
                    intervalMin = tnextY;
                    if (intervalMin > intervalMax)
                        return 0;
                    indxY += stepY;
                    if (indxY == stopY)
                        return 0;
                    tnextY += deltaY;
                    cell += cellstepY;
                } else {
                    if (voxels[cell] != null) {
                        boolean hit = false;
                        for (int i = 0; i < voxels[cell].getSize(); i++) {
                            int i3 = 3 * voxels[cell].get(i);
                            float ocx = r.ox - particles.get(i3 + 0);
                            float ocy = r.oy - particles.get(i3 + 1);
                            float ocz = r.oz - particles.get(i3 + 2);
                            float qa = r.dx * r.dx + r.dy * r.dy + r.dz * r.dz;
                            float qb = 2 * ((r.dx * ocx) + (r.dy * ocy) + (r.dz * ocz));
                            float qc = ((ocx * ocx) + (ocy * ocy) + (ocz * ocz)) - r2;
                            double[] t = Solvers.solveQuadric(qa, qb, qc);
                            if (t != null) {
                                // early rejection
                                if (t[0] >= r.getMax() || t[1] <= r.getMin())
                                    continue;
                                if (t[0] > r.getMin())
                                    r.setMax((float) t[0]);
                                else
                                    r.setMax((float) t[1]);
                                hit = true;
                            }
                        }
                        if (hit && (r.getMax() < tnextZ && r.getMax() < intervalMax))
                            return r.getMax();
                    }
                    intervalMin = tnextZ;
                    if (intervalMin > intervalMax)
                        return 0;
                    indxZ += stepZ;
                    if (indxZ == stopZ)
                        return 0;
                    tnextZ += deltaZ;
                    cell += cellstepZ;
                }
            }
        }

        private void getGridIndex(float x, float y, float z, int[] i) {
            i[0] = MathUtils.clamp((int) ((x - bounds.getMinimum().x) * invVoxelwx), 0, nx - 1);
            i[1] = MathUtils.clamp((int) ((y - bounds.getMinimum().y) * invVoxelwy), 0, ny - 1);
            i[2] = MathUtils.clamp((int) ((z - bounds.getMinimum().z) * invVoxelwz), 0, nz - 1);
        }
    }
}