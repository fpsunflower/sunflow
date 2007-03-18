package org.sunflow.image.formats;

import org.sunflow.image.Bitmap2;
import org.sunflow.image.Color;

public class BitmapG8 extends Bitmap2 {
    private int w, h;
    private byte[] data;

    public BitmapG8(int w, int h, byte[] data) {
        this.w = w;
        this.h = h;
        this.data = data;
    }

    public int getWidth() {
        return w;
    }

    public int getHeight() {
        return h;
    }

    public Color readColor(int x, int y) {
        return new Color((data[x + y * w] & 0xFF) * INV255);
    }

    public float readAlpha(int x, int y) {
        return 1;
    }
}