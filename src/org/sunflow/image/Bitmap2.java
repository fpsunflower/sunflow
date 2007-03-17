package org.sunflow.image;

public interface Bitmap2 {
    public static final float INV255 = 1.0f / 255;
    public static final float INV65535 = 1.0f / 65535;

    public int getWidth();

    public int getHeight();

    public Color readColor(int x, int y);

    public float readAlpha(int x, int y);
}