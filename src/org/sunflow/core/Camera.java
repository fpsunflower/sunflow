package org.sunflow.core;

import org.sunflow.SunflowAPI;
import org.sunflow.math.Matrix4;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

/**
 * This class represents a camera to the renderer. It handles the mapping of
 * camera space to world space, as well as the mounting of {@link CameraLens}
 * objects which compute the actual projection.
 */
public class Camera implements RenderObject {
    private final CameraLens lens;
    private Matrix4[] c2w;
    private Matrix4[] w2c;

    public Camera(CameraLens lens) {
        this.lens = lens;
        c2w = w2c = new Matrix4[1]; // null
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        int n = pl.getInt("transform.steps", 0);
        if (n <= 0) {
            // no motion blur, get regular arguments or leave unchanged
            updateCameraMatrix(-1, pl);
        } else {
            // new motion blur settings - get transform for each step
            c2w = new Matrix4[n];
            for (int i = 0; i < n; i++) {
                if (!updateCameraMatrix(i, pl)) {
                    UI.printError(Module.CAM, "Camera matrix for step %d was not specified!", i + 1);
                    return false;
                }
            }
        }
        w2c = new Matrix4[c2w.length];
        for (int i = 0; i < c2w.length; i++) {
            if (c2w[i] != null) {
                w2c[i] = c2w[i].inverse();
                if (w2c[i] == null) {
                    UI.printError(Module.CAM, "Camera matrix is not invertible");
                    return false;
                }
            } else
                w2c[i] = null;
        }
        return lens.update(pl, api);
    }

    private boolean updateCameraMatrix(int index, ParameterList pl) {
        String offset = index < 0 ? "" : String.format("[%d]", index);
        if (index < 0)
            index = 0;
        Matrix4 transform = pl.getMatrix(String.format("transform%s", offset), null);
        if (transform == null) {
            // no transform was specified, check eye/target/up
            Point3 eye = pl.getPoint(String.format("eye%s", offset), null);
            Point3 target = pl.getPoint(String.format("target%s", offset), null);
            Vector3 up = pl.getVector(String.format("up%s", offset), null);
            if (eye != null && target != null && up != null) {
                c2w[index] = Matrix4.fromBasis(OrthoNormalBasis.makeFromWV(Point3.sub(eye, target, new Vector3()), up));
                c2w[index] = Matrix4.translation(eye.x, eye.y, eye.z).multiply(c2w[index]);
            } else {
                // the matrix for this index was not specified
                // return an error, unless this is a regular update
                return offset.length() == 0;
            }
        } else
            c2w[index] = transform;
        return true;
    }

    /**
     * Generate a ray passing though the specified point on the image plane.
     * Additional random variables are provided for the lens to optionally
     * compute depth-of-field or motion blur effects. Note that the camera may
     * return <code>null</code> for invalid arguments or for pixels which
     * don't project to anything.
     * 
     * @param x x pixel coordinate
     * @param y y pixel coordinate
     * @param imageWidth width of the image in pixels
     * @param imageHeight height of the image in pixels
     * @param lensX a random variable in [0,1) to be used for DOF sampling
     * @param lensY a random variable in [0,1) to be used for DOF sampling
     * @param time a random variable in [0,1) to be used for motion blur
     *            sampling
     * @return a ray passing through the specified pixel, or <code>null</code>
     */
    public Ray getRay(float x, float y, int imageWidth, int imageHeight, double lensX, double lensY, double time) {
        Ray r = lens.getRay(x, y, imageWidth, imageHeight, lensX, lensY, time);
        if (r != null) {
            if (c2w.length == 1) {
                // regular sampling
                r = r.transform(c2w[0]);
            } else {
                // motion blur
                double nt = time * (c2w.length - 1);
                int idx0 = (int) nt;
                int idx1 = Math.min(idx0 + 1, c2w.length - 1);
                r = r.transform(Matrix4.blend(c2w[idx0], c2w[idx1], (float) (nt - idx0)));
            }
            // renormalize to account for scale factors embeded in the transform
            r.normalize();
        }
        return r;
    }

    /**
     * Generate a ray from the origin of camera space toward the specified
     * point.
     * 
     * @param p point in world space
     * @return ray from the origin of camera space to the specified point
     */
    Ray getRay(Point3 p) {
        return new Ray(c2w == null ? new Point3(0, 0, 0) : c2w[0].transformP(new Point3(0, 0, 0)), p);
    }

    /**
     * Returns a transformation matrix mapping camera space to world space.
     * 
     * @return a transformation matrix
     */
    Matrix4 getCameraToWorld() {
        return c2w == null ? Matrix4.IDENTITY : c2w[0];
    }

    /**
     * Returns a transformation matrix mapping world space to camera space.
     * 
     * @return a transformation matrix
     */
    Matrix4 getWorldToCamera() {
        return w2c == null ? Matrix4.IDENTITY : w2c[0];
    }
}