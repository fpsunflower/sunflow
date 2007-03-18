package org.sunflow.image;

import org.sunflow.math.MathUtils;

/**
 * This class contains many static helper methods helpfull for encoding colors
 * into files.
 */
public final class ColorEncoder {
    public static final Color[] unpremult(Color[] color, float[] alpha) {
        Color[] output = new Color[color.length];
        for (int i = 0; i < color.length; i++)
            output[i] = color[i].copy().mul(1 / alpha[i]);
        return output;
    }

    public static final Color[] unlinearize(Color[] color) {
        Color[] output = new Color[color.length];
        for (int i = 0; i < color.length; i++)
            output[i] = color[i].copy().toNonLinear();
        return output;
    }

    public static final byte[] quantizeRGB8(Color[] color) {
        byte[] output = new byte[color.length * 3];
        for (int i = 0, index = 0; i < color.length; i++, index += 3) {
            float[] rgb = color[i].getRGB();
            output[index + 0] = (byte) MathUtils.clamp((int) (rgb[0] * 255 + 0.5f), 0, 255);
            output[index + 1] = (byte) MathUtils.clamp((int) (rgb[1] * 255 + 0.5f), 0, 255);
            output[index + 2] = (byte) MathUtils.clamp((int) (rgb[2] * 255 + 0.5f), 0, 255);
        }
        return output;
    }
    
    public static final byte[] quantizeRGBA8(Color[] color, float[] alpha) {
        byte[] output = new byte[color.length * 4];
        for (int i = 0, index = 0; i < color.length; i++, index += 4) {
            float[] rgb = color[i].getRGB();
            output[index + 0] = (byte) MathUtils.clamp((int) (rgb[0] * 255 + 0.5f), 0, 255);
            output[index + 1] = (byte) MathUtils.clamp((int) (rgb[1] * 255 + 0.5f), 0, 255);
            output[index + 2] = (byte) MathUtils.clamp((int) (rgb[2] * 255 + 0.5f), 0, 255);
            output[index + 3] = (byte) MathUtils.clamp((int) (alpha[i] * 255 + 0.5f), 0, 255);
        }
        return output;
    }
    
    public static final int[] quantizeRGBE(Color[] color) {
        int[] output = new int[color.length];
        for (int i = 0; i < color.length; i++)
            output[i] = color[i].toRGBE();
        return output;
    }
}