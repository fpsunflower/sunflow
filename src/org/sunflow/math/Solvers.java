package org.sunflow.math;

public final class Solvers {
    /**
     * Solves the equation ax^2+bx+c=0. Solutions are returned in a sorted
     * floating point array if they exist.
     * 
     * @param a
     *            coefficient of x^2
     * @param b
     *            coefficient of x^1
     * @param c
     *            coefficient of x^0
     * @return an array containing the two real roots, or <code>null</code> if
     *         no real solutions exist
     */
    public static final float[] solveQuadric(float a, float b, float c) {
        double disc = b * b - 4 * a * c;
        if (disc < 0)
            return null;
        disc = Math.sqrt(disc);
        float q = (float) ((b < 0) ? -0.5 * (b - disc) : -0.5 * (b + disc));
        float t0 = q / a;
        float t1 = c / q;
        // return sorted array
        return (t0 > t1) ? new float[] { t1, t0 } : new float[] { t0, t1 };
    }

    /**
     * Solve the equation ax^3+bx^2+cx+d=0. Solutions are returned in a sorted
     * floating point array if they exist.
     * 
     * @param a
     *            coefficient of x^3
     * @param b
     *            coefficient of x^2
     * @param c
     *            coefficient of x^1
     * @param d
     *            coefficient of x^0
     * @return an array containing the real roots of the cubic equation or
     *         <code>null</code> if no solutions were found
     */
    public static final float[] solveCubic(float a, float b, float c, float d) {
        if (a == 0)
            return solveQuadric(b, c, d);
        float invA = 1 / a;
        double A = b * invA;
        double B = c * invA;
        double C = d * invA;
        double A2 = A * A;
        double p = (1.0 / 3 * (-1.0 / 3 * A2 + B));
        double q = (1.0 / 2 * (2.0 / 27 * A * A2 - 1.0 / 3 * A * B + C));
        double p3 = p * p * p;
        double D = q * q + p3;
        double offset = -(1.0 / 3) * A;
        if (D == 0) {
            if (q == 0)
                return new float[] { (float) offset };
            else {
                double u = Math.cbrt(-q);
                if (u > 0)
                    return new float[] { (float) (-u + offset), (float) (2 * u + offset) };
                else
                    return new float[] { (float) (2 * u + offset), (float) (-u + offset) };
            }
        } else if (D < 0) {
            double phi = (1.0 / 3 * Math.acos(-q / Math.sqrt(-p3)));
            double t = 2 * Math.sqrt(-p);
            float s0 = (float) (offset + t * Math.cos(phi));
            float s1 = (float) (offset - t * Math.cos(phi + Math.PI / 3));
            float s2 = (float) (offset - t * Math.cos(phi - Math.PI / 3));
            // do mini-sort
            if (s0 < s1 && s0 < s2) {
                // s0 is min
                if (s1 < s2)
                    return new float[] { s0, s1, s2 };
                else
                    return new float[] { s0, s2, s1 };
            } else if (s1 < s2) {
                // s0 is not min
                // s1 is min
                if (s0 < s2)
                    return new float[] { s1, s0, s2 };
                else
                    return new float[] { s1, s2, s0 };
            } else {
                // s0 is not min
                // s2 is min
                if (s0 < s1)
                    return new float[] { s2, s0, s1 };
                else
                    return new float[] { s2, s1, s0 };
            }
        } else {
            double sqrtD = Math.sqrt(D);
            double u = Math.cbrt(sqrtD - q);
            double v = -Math.cbrt(sqrtD + q);
            return new float[] { (float) (u + v + offset) };
        }
    }
}