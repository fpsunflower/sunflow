package org.sunflow.core.camera;

import org.sunflow.core.Camera;
import org.sunflow.core.Ray;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class SphericalCamera implements Camera {
    private Point3 center;

    private OrthoNormalBasis basis;

    public SphericalCamera(Point3 eye, Point3 target, Vector3 up) {
        center = new Point3(eye);
        basis = OrthoNormalBasis.makeFromWV(Point3.sub(eye, target, new Vector3()), up);
    }

    public Ray getRay(float x, float y, int imageWidth, int imageHeight, double lensX, double lensY, double time) {
        // Generate environment camera ray direction
        double theta = 2 * Math.PI * x / imageWidth + Math.PI / 2;
        double phi = Math.PI * (imageHeight - 1 - y) / imageHeight;
        Vector3 dir = new Vector3((float) (Math.cos(theta) * Math.sin(phi)), (float) (Math.cos(phi)), (float) (Math.sin(theta) * Math.sin(phi)));
        return new Ray(center, basis.transform(dir));
    }
}