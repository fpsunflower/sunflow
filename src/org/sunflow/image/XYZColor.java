package org.sunflow.image;

public final class XYZColor {
    private float X, Y, Z;

    public XYZColor() {
    }

    public XYZColor(float X, float Y, float Z) {
        this.X = X;
        this.Y = Y;
        this.Z = Z;
    }

    public final float getX() {
        return X;
    }

    public final float getY() {
        return Y;
    }

    public final float getZ() {
        return Z;
    }

    public final String toString() {
        return String.format("(%.3f, %.3f, %.3f)", X, Y, Z);
    }
}