package org.sunflow.core;

import java.io.IOException;

import org.sunflow.image.Bitmap;
import org.sunflow.image.Color;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Vector3;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

/**
 * Represents a 2D texture, typically used by {@link Shader shaders}.
 */
public class Texture {
    private String filename;
    private boolean isLinear;
    private Bitmap bitmap;
    private int loaded;

    /**
     * Creates a new texture from the specfied file.
     * 
     * @param filename image file to load
     * @param isLinear is the texture gamma corrected already?
     */
    Texture(String filename, boolean isLinear) {
        this.filename = filename;
        this.isLinear = isLinear;
        loaded = 0;
    }

    private synchronized void load() {
        if (loaded != 0)
            return;
        try {
            UI.printInfo(Module.TEX, "Reading texture bitmap from: \"%s\" ...", filename);
            bitmap = new Bitmap(filename, isLinear);
            if (bitmap.getWidth() == 0 || bitmap.getHeight() == 0)
                bitmap = null;
        } catch (IOException e) {
            UI.printError(Module.TEX, "%s", e.getMessage());
        }
        loaded = 1;
    }

    public Bitmap getBitmap() {
        if (loaded == 0)
            load();
        return bitmap;
    }

    /**
     * Gets the color at location (x,y) in the texture. The lookup is performed
     * using the fractional component of the coordinates, treating the texture
     * as a unit square tiled in both directions. Bicubic filtering is performed
     * on the four nearest pixels to the lookup point.
     * 
     * @param x x coordinate into the texture
     * @param y y coordinate into the texture
     * @return filtered color at location (x,y)
     */
    public Color getPixel(float x, float y) {
        Bitmap bitmap = getBitmap();
        if (bitmap == null)
            return Color.BLACK;
        x = x - (int) x;
        y = y - (int) y;
        if (x < 0)
            x++;
        if (y < 0)
            y++;
        float dx = (float) x * (bitmap.getWidth() - 1);
        float dy = (float) y * (bitmap.getHeight() - 1);
        int ix0 = (int) dx;
        int iy0 = (int) dy;
        int ix1 = (ix0 + 1) % bitmap.getWidth();
        int iy1 = (iy0 + 1) % bitmap.getHeight();
        float u = dx - ix0;
        float v = dy - iy0;
        u = u * u * (3.0f - (2.0f * u));
        v = v * v * (3.0f - (2.0f * v));
        float k00 = (1.0f - u) * (1.0f - v);
        Color c00 = bitmap.getPixel(ix0, iy0);
        float k01 = (1.0f - u) * v;
        Color c01 = bitmap.getPixel(ix0, iy1);
        float k10 = u * (1.0f - v);
        Color c10 = bitmap.getPixel(ix1, iy0);
        float k11 = u * v;
        Color c11 = bitmap.getPixel(ix1, iy1);
        Color c = Color.mul(k00, c00);
        c.madd(k01, c01);
        c.madd(k10, c10);
        c.madd(k11, c11);
        return c;
    }

    public Vector3 getNormal(float x, float y, OrthoNormalBasis basis) {
        float[] rgb = getPixel(x, y).getRGB();
        return basis.transform(new Vector3(2 * rgb[0] - 1, 2 * rgb[1] - 1, 2 * rgb[2] - 1)).normalize();
    }

    public Vector3 getBump(float x, float y, OrthoNormalBasis basis, float scale) {
        Bitmap bitmap = getBitmap();
        if (bitmap == null)
            return basis.transform(new Vector3(0, 0, 1));
        float dx = 1.0f / (bitmap.getWidth() - 1);
        float dy = 1.0f / (bitmap.getHeight() - 1);
        float b0 = getPixel(x, y).getLuminance();
        float bx = getPixel(x + dx, y).getLuminance();
        float by = getPixel(x, y + dy).getLuminance();
        return basis.transform(new Vector3(scale * (bx - b0) / dx, scale * (by - b0) / dy, 1)).normalize();
    }
}