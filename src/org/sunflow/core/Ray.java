package org.sunflow.core;

import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

/**
 * This class represents a ray as a oriented half line segment. The ray
 * direction is always normalized. The valid region is delimted by two distances
 * along the ray, tMin and tMax.
 */
public final class Ray {
    public float ox, oy, oz;
    public float dx, dy, dz;
    private float tMin;
    private float tMax;
    private static final float EPSILON = 0.01f;

    private Ray() {
    }

    /**
     * Creates a new ray that points from the given origin to the given
     * direction. The ray has infinite length. Note that the parameters are
     * copied, so the ray has a new instance of both. The direction vector is
     * normalized.
     * 
     * @param o ray origin
     * @param d ray direction (need not be normalized)
     */
    public Ray(Point3 o, Vector3 d) {
        ox = o.x;
        oy = o.y;
        oz = o.z;
        dx = d.x;
        dy = d.y;
        dz = d.z;
        float in = 1.0f / (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx *= in;
        dy *= in;
        dz *= in;
        tMin = EPSILON;
        tMax = Float.POSITIVE_INFINITY;
    }

    /**
     * Creates a new ray that points from point a to point b. The created ray
     * will set tMin and tMax to limit the ray to the segment (a,b)
     * (non-inclusive of a and b). This is often used to create shadow rays.
     * 
     * @param a start point
     * @param b end point
     */
    public Ray(Point3 a, Point3 b) {
        ox = a.x;
        oy = a.y;
        oz = a.z;
        dx = b.x - ox;
        dy = b.y - oy;
        dz = b.z - oz;
        tMin = EPSILON;
        float n = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float in = 1.0f / n;
        dx *= in;
        dy *= in;
        dz *= in;
        tMax = n - EPSILON;
    }

    /**
     * Create a new ray by transforming the supplied one by the given matrix. If
     * the matrix is <code>null</code>, the original ray is returned.
     * 
     * @param w2o
     */
    public Ray transform(Matrix4 w2o) {
        if (w2o == null)
            return this;
        Ray r = new Ray();
        r.ox = w2o.transformPX(ox, oy, oz);
        r.oy = w2o.transformPY(ox, oy, oz);
        r.oz = w2o.transformPZ(ox, oy, oz);
        r.dx = w2o.transformVX(dx, dy, dz);
        r.dy = w2o.transformVY(dx, dy, dz);
        r.dz = w2o.transformVZ(dx, dy, dz);
        r.tMin = tMin;
        r.tMax = tMax;
        return r;
    }

    /**
     * Gets the minimum distance along the ray. Usually a small epsilon above 0.
     * 
     * @return value of the smallest distance along the ray
     */
    public final float getMin() {
        return tMin;
    }

    /**
     * Gets the maximum distance along the ray. May be infinite.
     * 
     * @return value of the largest distance along the ray
     */
    public final float getMax() {
        return tMax;
    }

    public final Vector3 getDirection() {
        return new Vector3(dx, dy, dz);
    }

    /**
     * Checks to see if the specified distance falls within the valid range on
     * this ray. This should always be used before an intersection with the ray
     * is detected.
     * 
     * @param t distance to be tested
     * @return <code>true</code> if t falls between the minimum and maximum
     *         distance of this ray, <code>false</code> otherwise
     * @see Primitive
     */
    public final boolean isInside(float t) {
        return (tMin < t) && (t < tMax);
    }

    /**
     * Gets the end point of the ray. A reference to <code>dest</code> is
     * returned to support chaining.
     * 
     * @param dest reference to the point to store
     * @return reference to <code>dest</code>
     */
    public final Point3 getPoint(Point3 dest) {
        dest.x = ox + (tMax * dx);
        dest.y = oy + (tMax * dy);
        dest.z = oz + (tMax * dz);
        return dest;
    }

    /**
     * Computes the dot product of an arbitrary vector with the direction of the
     * ray. This method avoids having to call getDirection() which would
     * instantiate a new Vector object.
     * 
     * @param v arbitrary vector
     * @return dot product of the ray direction and the specified vector
     */
    public final float dot(Vector3 v) {
        return dx * v.x + dy * v.y + dz * v.z;
    }

    /**
     * Updates the maximum to the specified distance if and only if the new
     * distance is smaller than the current one.
     * 
     * @param t new maximum distance
     */
    public final void setMax(float t) {
        tMax = t;
    }
}