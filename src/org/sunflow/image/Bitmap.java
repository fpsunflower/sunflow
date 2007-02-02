package org.sunflow.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class Bitmap {
    private int[] pixels;
    private int width;
    private int height;
    private boolean isHDR;

    public Bitmap(String filename, boolean isLinear) throws IOException {
        if (filename.endsWith(".hdr")) {
            isHDR = true;
            // load radiance rgbe file
            FileInputStream f = new FileInputStream(filename);
            // parse header
            boolean parseWidth = false, parseHeight = false;
            width = height = 0;
            int last = 0;
            while (width == 0 || height == 0 || last != '\n') {
                int n = f.read();
                switch (n) {
                    case 'Y':
                        parseHeight = last == '-';
                        parseWidth = false;
                        break;
                    case 'X':
                        parseHeight = false;
                        parseWidth = last == '+';
                        break;
                    case ' ':
                        parseWidth &= width == 0;
                        parseHeight &= height == 0;
                        break;
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        if (parseHeight)
                            height = 10 * height + (n - '0');
                        else if (parseWidth)
                            width = 10 * width + (n - '0');
                        break;
                    default:
                        parseWidth = parseHeight = false;
                        break;
                }
                last = n;
            }
            // allocate image
            pixels = new int[width * height];
            if ((width < 8) || (width > 0x7fff)) {
                // run length encoding is not allowed so read flat
                readFlatRGBE(f, 0, width * height);
                return;
            }
            int rasterPos = 0;
            int numScanlines = height;
            int[] scanlineBuffer = new int[4 * width];
            while (numScanlines > 0) {
                int r = f.read();
                int g = f.read();
                int b = f.read();
                int e = f.read();
                if ((r != 2) || (g != 2) || ((b & 0x80) != 0)) {
                    // this file is not run length encoded
                    pixels[rasterPos] = (r << 24) | (g << 16) | (b << 8) | e;
                    readFlatRGBE(f, rasterPos + 1, width * numScanlines - 1);
                    return;
                }

                if (((b << 8) | e) != width) {
                    System.out.println("Invalid scanline width");
                    return;
                }
                int p = 0;
                // read each of the four channels for the scanline into
                // the buffer
                for (int i = 0; i < 4; i++) {
                    if (p % width != 0)
                        System.out.println("Unaligned access to scanline data");
                    int end = (i + 1) * width;
                    while (p < end) {
                        int b0 = f.read();
                        int b1 = f.read();
                        if (b0 > 128) {
                            // a run of the same value
                            int count = b0 - 128;
                            if ((count == 0) || (count > (end - p))) {
                                System.out.println("Bad scanline data - invalid RLE run");
                                return;
                            }
                            while (count-- > 0) {
                                scanlineBuffer[p] = b1;
                                p++;
                            }
                        } else {
                            // a non-run
                            int count = b0;
                            if ((count == 0) || (count > (end - p))) {
                                System.out.println("Bad scanline data - invalid count");
                                return;
                            }
                            scanlineBuffer[p] = b1;
                            p++;
                            if (--count > 0) {
                                for (int x = 0; x < count; x++)
                                    scanlineBuffer[p + x] = f.read();
                                p += count;
                            }
                        }
                    }
                }
                // now convert data from buffer into floats
                for (int i = 0; i < width; i++) {
                    r = scanlineBuffer[i];
                    g = scanlineBuffer[i + width];
                    b = scanlineBuffer[i + 2 * width];
                    e = scanlineBuffer[i + 3 * width];
                    pixels[rasterPos] = (r << 24) | (g << 16) | (b << 8) | e;
                    rasterPos++;
                }
                numScanlines--;
            }
            // flip image
            for (int y = 0, i = 0, ir = (height - 1) * width; y < height / 2; y++, ir -= width) {
                for (int x = 0, i2 = ir; x < width; x++, i++, i2++) {
                    int t = pixels[i];
                    pixels[i] = pixels[i2];
                    pixels[i2] = t;
                }
            }
        } else if (filename.endsWith(".tga")) {
            isHDR = false;
            FileInputStream f = new FileInputStream(filename);
            int pix_ptr = 0, pix = 0, r, j;
            byte[] read = new byte[4];

            // read header
            int idsize = f.read() & 0xFF;
            f.read(); // cmap byte (unsupported)
            int datatype = f.read() & 0xFF;

            // colormap info (not supported)
            f.read();
            f.read();
            f.read();
            f.read();
            f.read();

            f.read(); // xstart, 16 bits
            f.read();
            f.read(); // ystart, 16 bits
            f.read();

            // read resolution
            width = (f.read() & 0xFF);
            width |= ((f.read() & 0xFF) << 8);
            height = (f.read() & 0xFF);
            height |= ((f.read() & 0xFF) << 8);

            pixels = new int[width * height];

            int bpp = (f.read() & 0xFF) / 8;

            int imgdscr = (f.read() & 0xFF);

            // skip image ID
            if (idsize != 0)
                f.skip(idsize);

            switch (datatype) {
                case 10:
                    // RLE RGB image
                    while (pix_ptr < (width * height)) {
                        r = (f.read() & 0xFF);
                        if ((r & 128) == 128) {
                            // a runlength packet
                            r &= 127;
                            f.read(read, 0, bpp);
                            // alpha not yet supported
                            pix = (read[2] & 0xFF) << 16;
                            pix |= (read[1] & 0xFF) << 8;
                            pix |= (read[0] & 0xFF);
                            // replicate pixel
                            pix = isLinear ? pix : RGBSpace.SRGB.rgbToLinear(pix);
                            for (j = 0; j <= r; j++, pix_ptr++)
                                pixels[pix_ptr] = pix;
                        } else {
                            // a raw packet
                            r &= 127;
                            for (j = 0; j <= r; j++, pix_ptr++) {
                                f.read(read, 0, bpp);
                                // alpha not yet supported
                                pix = ((read[2] & 0xFF) << 16);
                                pix |= ((read[1] & 0xFF) << 8);
                                pix |= ((read[0] & 0xFF));
                                pixels[pix_ptr] = isLinear ? pix : RGBSpace.SRGB.rgbToLinear(pix);
                            }
                        }
                    }
                    break;
                case 2:
                    // Uncompressed RGB
                    for (pix_ptr = 0; pix_ptr < (width * height); pix_ptr++) {
                        f.read(read, 0, bpp);
                        // the order is bgr reading from the file
                        // alpha not yet supported
                        pix = ((read[2] & 0xFF) << 16);
                        pix |= ((read[1] & 0xFF) << 8);
                        pix |= ((read[0] & 0xFF));
                        pixels[pix_ptr] = isLinear ? pix : RGBSpace.SRGB.rgbToLinear(pix);
                    }
                    break;
                default:
                    UI.printWarning(Module.IMG, "Unsupported TGA datatype: %s", datatype);
                    break;
            }
            if ((imgdscr & 32) == 32) {
                pix_ptr = 0;
                for (int y = 0; y < (height / 2); y++)
                    for (int x = 0; x < width; x++) {
                        int t = pixels[pix_ptr];
                        pixels[pix_ptr] = pixels[(height - y - 1) * width + x];
                        pixels[(height - y - 1) * width + x] = t;
                        pix_ptr++;
                    }

            }
            f.close();
        } else {
            // regular image, load using Java api
            BufferedImage bi = ImageIO.read(new File(filename));
            width = bi.getWidth();
            height = bi.getHeight();
            isHDR = false;
            pixels = new int[width * height];
            for (int y = 0, index = 0; y < height; y++) {
                for (int x = 0; x < width; x++, index++) {
                    int rgb = bi.getRGB(x, height - 1 - y);
                    pixels[index] = isLinear ? rgb : RGBSpace.SRGB.rgbToLinear(rgb);
                }
            }
        }
    }

    public static void save(BufferedImage image, String filename) {
        Bitmap b = new Bitmap(image.getWidth(), image.getHeight(), false);
        for (int y = 0; y < b.height; y++)
            for (int x = 0; x < b.width; x++)
                b.pixels[((b.height - 1 - y) * b.width) + x] = image.getRGB(x, y);
        if (filename.endsWith(".tga"))
            b.saveTGA(filename);
        else
            b.savePNG(filename);
    }

    private void readFlatRGBE(FileInputStream f, int rasterPos, int numPixels) throws IOException {
        while (numPixels-- > 0) {
            int r = f.read();
            int g = f.read();
            int b = f.read();
            int e = f.read();
            pixels[rasterPos] = (r << 24) | (g << 16) | (b << 8) | e;
            rasterPos++;
        }
    }

    public Bitmap(int w, int h, boolean isHDR) {
        width = w;
        height = h;
        this.isHDR = isHDR;
        pixels = new int[w * h];
    }

    public void setPixel(int x, int y, Color c) {
        if ((x >= 0) && (x < width) && (y >= 0) && (y < height))
            pixels[(y * width) + x] = isHDR ? c.toRGBE() : c.copy().toNonLinear().toRGB();
    }

    public Color getPixel(int x, int y) {
        if ((x >= 0) && (x < width) && (y >= 0) && (y < height))
            return isHDR ? new Color().setRGBE(pixels[(y * width) + x]) : new Color(pixels[(y * width) + x]);
        return Color.BLACK;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void save(String filename) {
        if (filename.endsWith(".hdr"))
            saveHDR(filename);
        else if (filename.endsWith(".png"))
            savePNG(filename);
        else if (filename.endsWith(".tga"))
            saveTGA(filename);
        else
            saveHDR(filename + ".hdr");
    }

    private void savePNG(String filename) {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                bi.setRGB(x, height - 1 - y, isHDR ? getPixel(x, y).toRGB() : pixels[(y * width) + x]);
        try {
            ImageIO.write(bi, "png", new File(filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveHDR(String filename) {
        try {
            FileOutputStream f = new FileOutputStream(filename);
            f.write("#?RGBE\n".getBytes());
            f.write("FORMAT=32-bit_rle_rgbe\n\n".getBytes());
            f.write(("-Y " + height + " +X " + width + "\n").getBytes());
            for (int y = height - 1; y >= 0; y--) {
                for (int x = 0; x < width; x++) {
                    int rgbe = isHDR ? pixels[(y * width) + x] : new Color(pixels[(y * width) + x]).toRGBE();
                    f.write(rgbe >> 24);
                    f.write(rgbe >> 16);
                    f.write(rgbe >> 8);
                    f.write(rgbe);
                }
            }
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveTGA(String filename) {
        try {
            FileOutputStream f = new FileOutputStream(filename);
            // no id, no colormap, uncompressed 3bpp RGB
            byte[] tgaHeader = { 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            f.write(tgaHeader);
            // then the size info
            f.write(width & 0xFF);
            f.write((width >> 8) & 0xFF);
            f.write(height & 0xFF);
            f.write((height >> 8) & 0xFF);
            // bitsperpixel and filler
            f.write(32);
            f.write(0);
            // image data
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pix = isHDR ? getPixel(x, y).toRGB() : pixels[y * width + x];
                    f.write(pix & 0xFF);
                    f.write((pix >> 8) & 0xFF);
                    f.write((pix >> 16) & 0xFF);
                    f.write(0xFF);
                }
            }
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}