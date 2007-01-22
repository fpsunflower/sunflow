package org.sunflow.system;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;

import org.sunflow.core.Display;
import org.sunflow.image.Bitmap;
import org.sunflow.image.Color;

@SuppressWarnings("serial")
public class ImagePanel extends JPanel implements Display {
    private static final int[] BORDERS = { Color.RED.toRGB(),
            Color.GREEN.toRGB(), Color.BLUE.toRGB(), Color.YELLOW.toRGB(),
            Color.CYAN.toRGB(), Color.MAGENTA.toRGB() };
    private BufferedImage image;
    private float xo, yo;
    private float w, h;
    private long repaintCounter;

    private class ScrollZoomListener extends MouseInputAdapter implements MouseWheelListener {
        int mx;
        int my;
        boolean dragging;
        boolean zooming;

        public void mousePressed(MouseEvent e) {
            mx = e.getX();
            my = e.getY();
            switch (e.getButton()) {
                case MouseEvent.BUTTON1:
                    dragging = true;
                    zooming = false;
                    break;
                case MouseEvent.BUTTON2: {
                    dragging = zooming = false;
                    // if CTRL is pressed
                    if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK)
                        fit();
                    else
                        reset();
                    break;
                }
                case MouseEvent.BUTTON3:
                    zooming = true;
                    dragging = false;
                    break;
                default:
                    return;
            }
            repaint();
        }

        public void mouseDragged(MouseEvent e) {
            int mx2 = e.getX();
            int my2 = e.getY();
            if (dragging)
                drag(mx2 - mx, my2 - my);
            if (zooming)
                zoom(mx2 - mx, my2 - my);
            mx = mx2;
            my = my2;
        }

        public void mouseReleased(MouseEvent e) {
            // same behaviour
            mouseDragged(e);
        }

        public void mouseWheelMoved(MouseWheelEvent e) {
            zoom(-20 * e.getWheelRotation(), 0);
        }
    }

    public ImagePanel() {
        setPreferredSize(new Dimension(640, 480));
        image = null;
        xo = yo = 0;
        w = h = 0;
        ScrollZoomListener listener = new ScrollZoomListener();
        addMouseListener(listener);
        addMouseMotionListener(listener);
        addMouseWheelListener(listener);
    }

    public void save(String filename) {
        Bitmap.save(image, filename);
    }

    private synchronized void drag(int dx, int dy) {
        xo += dx;
        yo += dy;
        repaint();
    }

    private synchronized void zoom(int dx, int dy) {
        int a = Math.max(dx, dy);
        int b = Math.min(dx, dy);
        if (Math.abs(b) > Math.abs(a))
            a = b;
        if (a == 0)
            return;
        // window center
        float cx = getWidth() * 0.5f;
        float cy = getHeight() * 0.5f;

        // origin of the image in window space
        float x = xo + (getWidth() - w) * 0.5f;
        float y = yo + (getHeight() - h) * 0.5f;

        // coordinates of the pixel we are over
        float sx = cx - x;
        float sy = cy - y;

        // scale
        if (w + a > 100) {
            h = (w + a) * h / w;
            sx = (w + a) * sx / w;
            sy = (w + a) * sy / w;
            w = (w + a);
        }

        // restore center pixel

        float x2 = cx - sx;
        float y2 = cy - sy;

        xo = (x2 - (getWidth() - w) * 0.5f);
        yo = (y2 - (getHeight() - h) * 0.5f);

        repaint();
    }

    public synchronized void reset() {
        xo = yo = 0;
        if (image != null) {
            w = image.getWidth();
            h = image.getHeight();
        }
        repaint();
    }

    public synchronized void fit() {
        xo = yo = 0;
        if (image != null) {
            float wx = Math.max(getWidth() - 10, 100);
            float hx = wx * image.getHeight() / image.getWidth();
            float hy = Math.max(getHeight() - 10, 100);
            float wy = hy * image.getWidth() / image.getHeight();
            if (hx > hy) {
                w = wy;
                h = hy;
            } else {
                w = wx;
                h = hx;
            }
            repaint();
        }
    }

    public synchronized void imageBegin(int w, int h, int bucketSize) {
        if (image != null && w == image.getWidth() && h == image.getHeight()) {
            // dull image if it has same resolution (75%)
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int rgb = image.getRGB(x, y);
                    image.setRGB(x, y, ((rgb & 0x00FEFEFE) >>> 1) + ((rgb & 0x00FCFCFC) >>> 2));
                }
            }
        } else {
            // allocate new framebuffer
            image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            // center
            this.w = w;
            this.h = h;
            xo = yo = 0;
        }
        repaintCounter = System.nanoTime();
        repaint();
    }

    public synchronized void imagePrepare(int x, int y, int w, int h, int id) {
        int border = BORDERS[id % BORDERS.length];
        for (int by = 0; by < h; by++) {
            for (int bx = 0; bx < w; bx++) {
                if (bx == 0 || bx == w - 1) {
                    if (5 * by < h || 5 * (h - by - 1) < h)
                        image.setRGB(x + bx, y + by, border);
                } else if (by == 0 || by == h - 1) {
                    if (5 * bx < w || 5 * (w - bx - 1) < w)
                        image.setRGB(x + bx, y + by, border);
                }
            }
        }
        repaint();
    }

    public synchronized void imageUpdate(int x, int y, int w, int h, Color[] data) {
        if ((image == null) || (data == null))
            return;
        for (int j = 0, index = 0; j < h; j++)
            for (int i = 0; i < w; i++, index++)
                image.setRGB(x + i, y + j, data[index].copy().toNonLinear().toRGB());
        repaint();
    }

    public synchronized void imageFill(int x, int y, int w, int h, Color c) {
        if ((image == null) || (c == null))
            return;
        int rgb = c.copy().toNonLinear().toRGB();
        for (int j = 0, index = 0; j < h; j++)
            for (int i = 0; i < w; i++, index++)
                image.setRGB(x + i, y + j, rgb);
        fastRepaint();
    }

    public void imageEnd() {
        repaint();
    }

    private void fastRepaint() {
        long t = System.nanoTime();
        if (repaintCounter + 125000000 < t) {
            repaintCounter = t;
            repaint();
        }
    }

    @Override
    public synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image == null)
            return;
        int x = (int) Math.round(xo + (getWidth() - w) * 0.5f);
        int y = (int) Math.round(yo + (getHeight() - h) * 0.5f);
        int iw = (int) Math.round(w);
        int ih = (int) Math.round(h);
        int x0 = x - 1;
        int y0 = y - 1;
        int x1 = x + iw + 1;
        int y1 = y + ih + 1;
        g.setColor(java.awt.Color.WHITE);
        g.drawLine(x0, y0, x1, y0);
        g.drawLine(x1, y0, x1, y1);
        g.drawLine(x1, y1, x0, y1);
        g.drawLine(x0, y1, x0, y0);
        g.drawImage(image, x, y, iw, ih, this);
    }
}