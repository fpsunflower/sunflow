package org.sunflow.core.primitive;

import org.sunflow.core.BoundedPrimitive;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.Ray;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Vector3;

public abstract class CubeGrid implements BoundedPrimitive {
    private int nx, ny, nz;
    private float voxelwx, voxelwy, voxelwz;
    private float invVoxelwx, invVoxelwy, invVoxelwz;
    private BoundingBox bounds;
    private Shader shader;

    protected CubeGrid(BoundingBox bounds, Shader shader) {
        this(1, 1, 1, bounds, shader);
    }

    protected CubeGrid(int nx, int ny, int nz, BoundingBox bounds, Shader shader) {
        this.bounds = bounds;
        this.shader = shader;
        setSize(nx, ny, nz);
    }

    protected void setSize(int nx, int ny, int nz) {
        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
        Vector3 w = bounds.getExtents();
        voxelwx = w.x / nx;
        voxelwy = w.y / ny;
        voxelwz = w.z / nz;
        invVoxelwx = 1 / voxelwx;
        invVoxelwy = 1 / voxelwy;
        invVoxelwz = 1 / voxelwz;
    }

    protected abstract boolean inside(int x, int y, int z);

    public BoundingBox getBounds() {
        return bounds;
    }

    public boolean intersects(BoundingBox box) {
        return box.intersects(bounds);
    }

    public void prepareShadingState(ShadingState state) {
        state.init();
        state.getRay().getPoint(state.getPoint());
        int n = (int) state.getU();
        switch (n) {
            case 0:
                state.getNormal().set(new Vector3(-1, 0, 0));
                break;
            case 1:
                state.getNormal().set(new Vector3(1, 0, 0));
                break;
            case 2:
                state.getNormal().set(new Vector3(0, -1, 0));
                break;
            case 3:
                state.getNormal().set(new Vector3(0, 1, 0));
                break;
            case 4:
                state.getNormal().set(new Vector3(0, 0, -1));
                break;
            case 5:
                state.getNormal().set(new Vector3(0, 0, 1));
                break;
            default:
                state.getNormal().set(new Vector3(0, 0, 0));
                break;
        }
        state.getGeoNormal().set(state.getNormal());
        state.setBasis(OrthoNormalBasis.makeFromW(state.getNormal()));
        state.setShader(shader);
    }

    public void intersect(Ray r, IntersectionState state) {
        float intervalMin = r.getMin();
        float intervalMax = r.getMax();
        float orgX = r.ox;
        float orgY = r.oy;
        float orgZ = r.oz;
        float dirX = r.dx, invDirX = 1 / dirX;
        float dirY = r.dy, invDirY = 1 / dirY;
        float dirZ = r.dz, invDirZ = 1 / dirZ;
        float t1, t2;
        t1 = (bounds.getMinimum().x - orgX) * invDirX;
        t2 = (bounds.getMaximum().x - orgX) * invDirX;
        int curr = -1;
        if (invDirX > 0) {
            if (t1 > intervalMin) {
                intervalMin = t1;
                curr = 0;
            }
            if (t2 < intervalMax) intervalMax = t2;
            if (intervalMin > intervalMax) return;
        } else {
            if (t2 > intervalMin) {
                intervalMin = t2;
                curr = 1;
            }
            if (t1 < intervalMax) intervalMax = t1;
            if (intervalMin > intervalMax) return;
        }
        t1 = (bounds.getMinimum().y - orgY) * invDirY;
        t2 = (bounds.getMaximum().y - orgY) * invDirY;
        if (invDirY > 0) {
            if (t1 > intervalMin) {
                intervalMin = t1;
                curr = 2;
            }
            if (t2 < intervalMax) intervalMax = t2;
            if (intervalMin > intervalMax) return;
        } else {
            if (t2 > intervalMin) {
                intervalMin = t2;
                curr = 3;
            }
            if (t1 < intervalMax) intervalMax = t1;
            if (intervalMin > intervalMax) return;
        }
        t1 = (bounds.getMinimum().z - orgZ) * invDirZ;
        t2 = (bounds.getMaximum().z - orgZ) * invDirZ;
        if (invDirZ > 0) {
            if (t1 > intervalMin) {
                intervalMin = t1;
                curr = 4;
            }
            if (t2 < intervalMax) intervalMax = t2;
            if (intervalMin > intervalMax) return;
        } else {
            if (t2 > intervalMin) {
                intervalMin = t2;
                curr = 5;
            }
            if (t1 < intervalMax) intervalMax = t1;
            if (intervalMin > intervalMax) return;
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
        else if (indxX >= nx) indxX = nx - 1;
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
        else if (indxY >= ny) indxY = ny - 1;
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
        else if (indxZ >= nz) indxZ = nz - 1;
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
        // are we starting inside the cube
        boolean isInside = inside(indxX, indxY, indxZ) && bounds.contains(r.ox, r.oy, r.oz);
        // trace through the grid
        for (;;) {
            if (inside(indxX, indxY, indxZ) != isInside) {
                // we hit a boundary
                r.setMax(intervalMin);
                // if we are inside, the last bit needs to be flipped
                if (isInside) curr ^= 1;
                state.setIntersection(this, curr, 0);
                return;
            }
            if (tnextX < tnextY && tnextX < tnextZ) {
                curr = dirX > 0 ? 0 : 1;
                intervalMin = tnextX;
                if (intervalMin > intervalMax) return;
                indxX += stepX;
                if (indxX == stopX) return;
                tnextX += deltaX;
            } else if (tnextY < tnextZ) {
                curr = dirY > 0 ? 2 : 3;
                intervalMin = tnextY;
                if (intervalMin > intervalMax) return;
                indxY += stepY;
                if (indxY == stopY) return;
                tnextY += deltaY;
            } else {
                curr = dirZ > 0 ? 4 : 5;
                intervalMin = tnextZ;
                if (intervalMin > intervalMax) return;
                indxZ += stepZ;
                if (indxZ == stopZ) return;
                tnextZ += deltaZ;
            }
        }
    }
}