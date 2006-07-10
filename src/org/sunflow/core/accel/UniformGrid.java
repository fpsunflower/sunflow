package org.sunflow.core.accel;

import java.util.ArrayList;

import org.sunflow.core.BoundedPrimitive;
import org.sunflow.core.IntersectionAccelerator;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.Ray;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.MathUtils;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;

public final class UniformGrid implements IntersectionAccelerator {
    private int nx, ny, nz;
    private BoundingBox bounds;
    private BoundedPrimitive[][] cells;
    private float voxelwx, voxelwy, voxelwz;
    private float invVoxelwx, invVoxelwy, invVoxelwz;

    public UniformGrid() {
        nx = ny = nz = 0;
        bounds = null;
        cells = null;
        voxelwx = voxelwy = voxelwz = 0;
        invVoxelwx = invVoxelwy = invVoxelwz = 0;
    }

    @SuppressWarnings("serial")
    private static class BuildList extends ArrayList<BoundedPrimitive> {
    }

    public boolean build(ArrayList<BoundedPrimitive> objects) {
        Timer t = new Timer();
        t.start();
        // compute bounds
        bounds = new BoundingBox();
        for (BoundedPrimitive p : objects)
            bounds.include(p.getBounds());
        // create grid from number of objects
        bounds.enlargeUlps();
        Vector3 w = bounds.getExtents();
        double s = Math.pow((w.x * w.y * w.z) / objects.size(), 1 / 3.0);
        nx = MathUtils.clamp((int) ((w.x / s) + 0.5), 1, 256);
        ny = MathUtils.clamp((int) ((w.y / s) + 0.5), 1, 256);
        nz = MathUtils.clamp((int) ((w.z / s) + 0.5), 1, 256);
        voxelwx = w.x / nx;
        voxelwy = w.y / ny;
        voxelwz = w.z / nz;
        invVoxelwx = 1 / voxelwx;
        invVoxelwy = 1 / voxelwy;
        invVoxelwz = 1 / voxelwz;
        UI.printInfo("[ACC] Creating grid: %dx%dx%d ...", nx, ny, nz);
        BuildList[] buildCells = new BuildList[nx * ny * nz];
        // add all objects into the grid cells they overlap
        int[] i0 = new int[3];
        int[] i1 = new int[3];
        UI.taskStart("Creating Uniform Grid", 0, objects.size());
        int numCellsPerObject = 0;
        int i = 0;
        for (BoundedPrimitive object : objects) {
            UI.taskUpdate(i);
            if (UI.taskCanceled()) {
                UI.taskStop();
                return false;
            }
            i++;
            BoundingBox objectBounds = object.getBounds();
            getGridIndex(objectBounds.getMinimum(), i0);
            getGridIndex(objectBounds.getMaximum(), i1);
            for (int ix = i0[0]; ix <= i1[0]; ix++)
                for (int iy = i0[1]; iy <= i1[1]; iy++)
                    for (int iz = i0[2]; iz <= i1[2]; iz++)
                        if (object.intersects(getGridBox(ix, iy, iz))) {
                            int idx = ix + (nx * iy) + (nx * ny * iz);
                            if (buildCells[idx] == null)
                                buildCells[idx] = new BuildList();
                            buildCells[idx].add(object);
                            numCellsPerObject++;
                        }
        }
        UI.taskStop();
        UI.printInfo("[ACC] Building cells ...");
        int numEmpty = 0;
        int numInFull = 0;
        cells = new BoundedPrimitive[nx * ny * nz][];
        i = 0;
        for (BuildList cell : buildCells) {
            if (cell != null) {
                if (cell.size() == 0) {
                    numEmpty++;
                    cell = null;
                } else {
                    cells[i] = cell.toArray(new BoundedPrimitive[cell.size()]);
                    numInFull += cell.size();
                }
                cell.clear();
            } else
                numEmpty++;
            i++;
        }
        t.end();
        UI.printInfo("[ACC] Uniform grid statistics:");
        UI.printInfo("[ACC]   * Grid cells:          %d", cells.length);
        UI.printInfo("[ACC]   * Used cells:          %d", cells.length - numEmpty);
        UI.printInfo("[ACC]   * Empty cells:         %d", numEmpty);
        UI.printInfo("[ACC]   * Occupancy:           %.2f%%", 100.0 * (cells.length - numEmpty) / cells.length);
        UI.printInfo("[ACC]   * Objects/Cell:        %.2f", (double) numInFull / (double) cells.length);
        UI.printInfo("[ACC]   * Objects/Used Cell:   %.2f", (double) numInFull / (double) (cells.length - numEmpty));
        UI.printInfo("[ACC]   * Cells/Object:        %.2f", (double) numCellsPerObject / (double) objects.size());
        UI.printInfo("[ACC]   * Build time:          %s", t.toString());
        return true;
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    public void intersect(Ray r, IntersectionState state) {
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
            if (intervalMin > intervalMax)
                return;
        } else {
            if (t2 > intervalMin)
                intervalMin = t2;
            if (t1 < intervalMax)
                intervalMax = t1;
            if (intervalMin > intervalMax)
                return;
        }
        float orgY = r.oy;
        float dirY = r.dy, invDirY = 1 / dirY;
        t1 = (bounds.getMinimum().y - orgY) * invDirY;
        t2 = (bounds.getMaximum().y - orgY) * invDirY;
        if (invDirY > 0) {
            if (t1 > intervalMin)
                intervalMin = t1;
            if (t2 < intervalMax)
                intervalMax = t2;
            if (intervalMin > intervalMax)
                return;
        } else {
            if (t2 > intervalMin)
                intervalMin = t2;
            if (t1 < intervalMax)
                intervalMax = t1;
            if (intervalMin > intervalMax)
                return;
        }
        float orgZ = r.oz;
        float dirZ = r.dz, invDirZ = 1 / dirZ;
        t1 = (bounds.getMinimum().z - orgZ) * invDirZ;
        t2 = (bounds.getMaximum().z - orgZ) * invDirZ;
        if (invDirZ > 0) {
            if (t1 > intervalMin)
                intervalMin = t1;
            if (t2 < intervalMax)
                intervalMax = t2;
            if (intervalMin > intervalMax)
                return;
        } else {
            if (t2 > intervalMin)
                intervalMin = t2;
            if (t1 < intervalMax)
                intervalMax = t1;
            if (intervalMin > intervalMax)
                return;
        }
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
                if (cells[cell] != null) {
                    for (BoundedPrimitive o : cells[cell])
                        o.intersect(r, state);
                    if (state.hit() && (r.getMax() < tnextX && r.getMax() < intervalMax))
                        return;
                }
                intervalMin = tnextX;
                if (intervalMin > intervalMax)
                    return;
                indxX += stepX;
                if (indxX == stopX)
                    return;
                tnextX += deltaX;
                cell += cellstepX;
            } else if (tnextY < tnextZ) {
                if (cells[cell] != null) {
                    for (BoundedPrimitive o : cells[cell])
                        o.intersect(r, state);
                    if (state.hit() && (r.getMax() < tnextY && r.getMax() < intervalMax))
                        return;
                }
                intervalMin = tnextY;
                if (intervalMin > intervalMax)
                    return;
                indxY += stepY;
                if (indxY == stopY)
                    return;
                tnextY += deltaY;
                cell += cellstepY;
            } else {
                if (cells[cell] != null) {
                    for (BoundedPrimitive o : cells[cell])
                        o.intersect(r, state);
                    if (state.hit() && (r.getMax() < tnextZ && r.getMax() < intervalMax))
                        return;
                }
                intervalMin = tnextZ;
                if (intervalMin > intervalMax)
                    return;
                indxZ += stepZ;
                if (indxZ == stopZ)
                    return;
                tnextZ += deltaZ;
                cell += cellstepZ;
            }
        }
    }

    private void getGridIndex(Point3 p, int[] i) {
        i[0] = MathUtils.clamp((int) ((p.x - bounds.getMinimum().x) * invVoxelwx), 0, nx - 1);
        i[1] = MathUtils.clamp((int) ((p.y - bounds.getMinimum().y) * invVoxelwy), 0, ny - 1);
        i[2] = MathUtils.clamp((int) ((p.z - bounds.getMinimum().z) * invVoxelwz), 0, nz - 1);
    }

    private BoundingBox getGridBox(int x, int y, int z) {
        BoundingBox box = new BoundingBox();
        box.getMinimum().x = bounds.getMinimum().x + x * voxelwx;
        box.getMinimum().y = bounds.getMinimum().y + y * voxelwy;
        box.getMinimum().z = bounds.getMinimum().z + z * voxelwz;
        box.getMaximum().x = box.getMinimum().x + voxelwx;
        box.getMaximum().y = box.getMinimum().y + voxelwy;
        box.getMaximum().z = box.getMinimum().z + voxelwz;
        return box;
    }
}