package org.sunflow.image;

public class RGBSpace {
    private final float gamma, breakPoint;
    private final float slope, slopeMatch, segmentOffset;
    private final float xr, yr, zr, xg, yg, zg, xb, yb, zb;
    private final float xw, yw, zw;
    private final float rx, ry, rz, gx, gy, gz, bx, by, bz;
    private final float rw, gw, bw;

    public RGBSpace(float xRed, float yRed, float xGreen, float yGreen, float xBlue, float yBlue, float xWhite, float yWhite, float gamma, float breakPoint) {
        this.gamma = gamma;
        this.breakPoint = breakPoint;

        // TODO: these expressions could probably be simplified
        slope = 1 / (gamma / (float) Math.pow(breakPoint, 1 / gamma - 1) - gamma * breakPoint + breakPoint);
        slopeMatch = gamma * slope / (float) Math.pow(breakPoint, 1 / gamma - 1);
        segmentOffset = slopeMatch * (float) Math.pow(breakPoint, 1 / gamma) - slope * breakPoint;

        xr = xRed;
        yr = yRed;
        zr = 1 - (xr + yr);
        xg = xGreen;
        yg = yGreen;
        zg = 1 - (xg + yg);
        xb = xBlue;
        yb = yBlue;
        zb = 1 - (xb + yb);

        xw = xWhite;
        yw = yWhite;
        zw = 1 - (xw + yw);

        // xyz -> rgb matrix, before scaling to white.
        float rx = (yg * zb) - (yb * zg);
        float ry = (xb * zg) - (xg * zb);
        float rz = (xg * yb) - (xb * yg);
        float gx = (yb * zr) - (yr * zb);
        float gy = (xr * zb) - (xb * zr);
        float gz = (xb * yr) - (xr * yb);
        float bx = (yr * zg) - (yg * zr);
        float by = (xg * zr) - (xr * zg);
        float bz = (xr * yg) - (xg * yr);

        // White scaling factors. Dividing by yw scales the white luminance to
        // unity, as conventional.
        rw = ((rx * xw) + (ry * yw) + (rz * zw)) / yw;
        gw = ((gx * xw) + (gy * yw) + (gz * zw)) / yw;
        bw = ((bx * xw) + (by * yw) + (bz * zw)) / yw;

        // xyz -> rgb matrix, correctly scaled to white

        this.rx = rx / rw;
        this.ry = ry / rw;
        this.rz = rz / rw;
        this.gx = gx / gw;
        this.gy = gy / gw;
        this.gz = gz / gw;
        this.bx = bx / bw;
        this.by = by / bw;
        this.bz = bz / bw;
    }

    public Color convertXYZtoRGB(float X, float Y, float Z) {
        float r = (rx * X) + (ry * Y) + (rz * Z);
        float g = (gx * X) + (gy * Y) + (gz * Z);
        float b = (bx * X) + (by * Y) + (bz * Z);
        return new Color(r, g, b);
    }

    public boolean insideGamut(float r, float g, float b) {
        return r >= 0 && g >= 0 && b >= 0;
    }

    public float gammaCorrect(float v) {
        if (v <= breakPoint)
            return slope * v;
        else
            return slopeMatch * (float) Math.pow(v, 1 / gamma) - segmentOffset;
    }

    public float ungammaCorrect(float vp) {
        if (vp <= breakPoint * slope)
            return vp / slope;
        else
            return (float) Math.pow((vp + segmentOffset) / slopeMatch, gamma);
    }

    public String toString() {
        String info = "Gamma function parameters:\n";
        info += String.format("  * Gamma          = %7.4f\n", gamma);
        info += String.format("  * Breakpoint     = %7.4f\n", breakPoint);
        info += String.format("  * Slope          = %7.4f\n", slope);
        info += String.format("  * Slope Match    = %7.4f\n", slopeMatch);
        info += String.format("  * Segment Offset = %7.4f\n", segmentOffset);
        return info;
    }

    public static void main(String[] args) {
        RGBSpace srgb = new RGBSpace(0.64f, 0.33f, 0.30f, 0.60f, 0.15f, 0.06f, 0.3127f, 0.3291f, 2.4f, 0.00304f);
        System.out.println(srgb.toString());
        RGBSpace rec709 = new RGBSpace(0.64f, 0.33f, 0.30f, 0.60f, 0.15f, 0.06f, 0.3127f, 0.3291f, 20.0f / 9.0f, 0.018f);
        System.out.println(rec709.toString());
    }
}