package org.sunflow.core.renderer;

import org.sunflow.core.BucketOrder;
import org.sunflow.core.Display;
import org.sunflow.core.Filter;
import org.sunflow.core.ImageSampler;
import org.sunflow.core.Instance;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.Scene;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.core.bucket.HilbertBucketOrder;
import org.sunflow.core.filter.BoxFilter;
import org.sunflow.image.Color;
import org.sunflow.math.MathUtils;
import org.sunflow.math.QMC;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;

public class BucketRenderer implements ImageSampler {
    private Scene scene;
    private Display display;
    // resolution
    private int imageWidth;
    private int imageHeight;
    // bucketing
    private BucketOrder bucketOrder;
    private int bucketSize;
    private int bucketCounter;
    private int[] bucketCoords;

    // anti-aliasing
    private int minAADepth;
    private int maxAADepth;
    private int superSampling;
    private double invSuperSampling;
    private int subPixelSize;
    private int minStepSize;
    private int maxStepSize;
    private int[] sigma;
    private float contrastThreshold;
    private float thresh;
    private boolean displayAA;

    // filtering
    private Filter filter;
    private int fs;
    private float fhs;

    public BucketRenderer() {
        bucketSize = 32;
        bucketOrder = new HilbertBucketOrder();
        displayAA = false;
        contrastThreshold = 0.1f;
    }

    public void setDisplayAA(boolean displayAA) {
        this.displayAA = displayAA;
    }

    public void setBuckerOrder(BucketOrder order) {
        bucketOrder = order;
    }

    public void setBucketSize(int size) {
        bucketSize = size;
    }

    public void setAA(int min, int max, int n) {
        minAADepth = min;
        maxAADepth = max;
        superSampling = n;
    }

    public boolean prepare(Scene scene, int w, int h) {
        this.scene = scene;

        imageWidth = w;
        imageHeight = h;
        // limit bucket size and compute number of buckets in each direction
        bucketSize = MathUtils.clamp(bucketSize, 16, 512);
        int numBucketsX = (imageWidth + bucketSize - 1) / bucketSize;
        int numBucketsY = (imageHeight + bucketSize - 1) / bucketSize;
        bucketCoords = bucketOrder.getBucketSequence(numBucketsX, numBucketsY);
        // validate AA options
        minAADepth = MathUtils.clamp(minAADepth, -4, 5);
        maxAADepth = MathUtils.clamp(maxAADepth, minAADepth, 5);
        superSampling = MathUtils.clamp(superSampling, 1, 256);
        invSuperSampling = 1.0 / superSampling;
        // compute AA stepping sizes
        subPixelSize = (maxAADepth > 0) ? (1 << maxAADepth) : 1;
        minStepSize = maxAADepth >= 0 ? 1 : 1 << (-maxAADepth);
        if (minAADepth == maxAADepth)
            maxStepSize = minStepSize;
        else
            maxStepSize = minAADepth > 0 ? 1 << minAADepth : subPixelSize << (-minAADepth);
        // sample image adaptively
        thresh = contrastThreshold * (float) Math.pow(2.0f, minAADepth);
        // read filter settings from scene
        filter = scene.getFilter();
        // adjust filter
        if (filter == null)
            filter = new BoxFilter(1);
        fhs = filter.getSize() * 0.5f;
        fs = (int) Math.ceil(subPixelSize * (fhs - 0.5f));

        // prepare QMC sampling
        sigma = QMC.generateSigmaTable(subPixelSize << 7);
        UI.printInfo("[BKT] Bucket renderer settings:");
        UI.printInfo("[BKT]   * Resolution:        %dx%d", imageWidth, imageHeight);
        UI.printInfo("[BKT]   * Bucket size:       %d", bucketSize);
        UI.printInfo("[BKT]   * Number of buckets: %dx%d", numBucketsX, numBucketsY);
        int pixelMinAA = (minAADepth) < 0 ? -(1 << (-minAADepth)) : (1 << minAADepth);
        int pixelMaxAA = (maxAADepth) < 0 ? -(1 << (-maxAADepth)) : (1 << maxAADepth);
        UI.printInfo("[BKT]   * Anti-aliasing:     [%dx%d] -> [%dx%d]", pixelMinAA, pixelMinAA, pixelMaxAA, pixelMaxAA);
        UI.printInfo("[BKT]   * Rays per sample:   %d", superSampling);
        UI.printInfo("[BKT]   * Filter size:       %.2f pixels", filter.getSize());
        return true;
    }

    public void render(Display display) {
        this.display = display;
        display.imageBegin(imageWidth, imageHeight, bucketSize);
        // set members variables
        bucketCounter = 0;
        // start task
        UI.taskStart("Rendering", 0, bucketCoords.length);
        Timer timer = new Timer();
        timer.start();
        Thread[] renderThreads = new Thread[scene.getThreads()];
        for (int i = 0; i < renderThreads.length; i++) {
            renderThreads[i] = new BucketThread(i);
            renderThreads[i].setPriority(scene.getThreadPriority());
            renderThreads[i].start();
        }
        for (int i = 0; i < renderThreads.length; i++) {
            try {
                renderThreads[i].join();
            } catch (InterruptedException e) {
                UI.printError("[BKT] Bucket processing thread %d of %d was interrupted", i + 1, renderThreads.length);
            }
        }
        UI.taskStop();
        timer.end();
        UI.printInfo("[BKT] Render time: %s", timer.toString());
        display.imageEnd();
    }

    private class BucketThread extends Thread {
        private int threadID;

        BucketThread(int threadID) {
            this.threadID = threadID;
        }

        public void run() {
            IntersectionState istate = new IntersectionState();
            while (true) {
                int bx, by;
                synchronized (BucketRenderer.this) {
                    if (bucketCounter >= bucketCoords.length)
                        return;
                    UI.taskUpdate(bucketCounter);
                    bx = bucketCoords[bucketCounter + 0];
                    by = bucketCoords[bucketCounter + 1];
                    bucketCounter += 2;
                }
                renderBucket(display, bx, by, threadID, istate);
                if (UI.taskCanceled())
                    return;
            }
        }
    }

    private void renderBucket(Display display, int bx, int by, int threadID, IntersectionState istate) {
        // pixel sized extents
        int x0 = bx * bucketSize;
        int y0 = by * bucketSize;
        int bw = Math.min(bucketSize, imageWidth - x0);
        int bh = Math.min(bucketSize, imageHeight - y0);

        // prepare bucket
        display.imagePrepare(x0, y0, bw, bh, threadID);

        Color[] bucketRGB = new Color[bw * bh];

        // subpixel extents
        int sx0 = x0 * subPixelSize - fs;
        int sy0 = y0 * subPixelSize - fs;
        int sbw = bw * subPixelSize + fs * 2;
        int sbh = bh * subPixelSize + fs * 2;

        // round up to align with maximum step size
        sbw = (sbw + (maxStepSize - 1)) & (~(maxStepSize - 1));
        sbh = (sbh + (maxStepSize - 1)) & (~(maxStepSize - 1));
        // extra padding as needed
        if (maxStepSize > 1) {
            sbw++;
            sbh++;
        }
        // allocate bucket memory
        ImageSample[] samples = new ImageSample[sbw * sbh];
        // allocate samples and compute jitter offsets
        float invSubPixelSize = 1.0f / subPixelSize;
        for (int y = 0, index = 0; y < sbh; y++) {
            for (int x = 0; x < sbw; x++, index++) {
                int sx = sx0 + x;
                int sy = sy0 + y;
                int j = sx & (sigma.length - 1);
                int k = sy & (sigma.length - 1);
                int i = j * sigma.length + sigma[k];
                float dx = maxAADepth >= 0 ? 0.5f : (float) sigma[k] / (float) sigma.length;
                float dy = maxAADepth >= 0 ? 0.5f : (float) sigma[j] / (float) sigma.length;
                float rx = (sx + dx) * invSubPixelSize;
                float ry = (sy + dy) * invSubPixelSize;
                ry = imageHeight - ry - 1;
                samples[index] = new ImageSample(rx, ry, i);
            }
        }
        for (int x = 0; x < sbw - 1; x += maxStepSize)
            for (int y = 0; y < sbh - 1; y += maxStepSize)
                refineSamples(samples, sbw, x, y, maxStepSize, thresh, istate);
        if (displayAA) {
            // color coded image of what is visible
            float invArea = invSubPixelSize * invSubPixelSize;
            for (int y = 0, index = 0; y < bh; y++) {
                for (int x = 0; x < bw; x++, index++) {
                    int sampled = 0;
                    for (int i = 0; i < subPixelSize; i++) {
                        for (int j = 0; j < subPixelSize; j++) {
                            int sx = x * subPixelSize + fs + i;
                            int sy = y * subPixelSize + fs + j;
                            int s = sx + sy * sbw;
                            sampled += samples[s].sampled ? 1 : 0;
                        }
                    }
                    bucketRGB[index] = new Color(sampled * invArea);
                }
            }
        } else {
            // filter samples into pixels
            float cy = imageHeight - 1 - (y0 + 0.5f);
            for (int y = 0, index = 0; y < bh; y++, cy--) {
                float cx = x0 + 0.5f;
                for (int x = 0; x < bw; x++, index++, cx++) {
                    Color c = Color.black();
                    float weight = 0.0f;
                    for (int j = -fs, sy = y * subPixelSize; j <= fs; j++, sy++) {
                        for (int i = -fs, sx = x * subPixelSize, s = sx + sy * sbw; i <= fs; i++, sx++, s++) {
                            float dx = samples[s].rx - cx;
                            if (Math.abs(dx) > fhs)
                                continue;
                            float dy = samples[s].ry - cy;
                            if (Math.abs(dy) > fhs)
                                continue;
                            float f = filter.get(dx, dy);
                            c.madd(f, samples[s].c);
                            weight += f;
                        }
                    }
                    c.mul(1.0f / weight);
                    bucketRGB[index] = c;
                }
            }
        }
        // update pixels
        display.imageUpdate(x0, y0, bw, bh, bucketRGB);
    }

    private void computeSubPixel(ImageSample sample, IntersectionState istate) {
        float x = sample.rx;
        float y = sample.ry;
        double q0 = QMC.halton(1, sample.i);
        double q1 = QMC.halton(2, sample.i);
        double q2 = QMC.halton(3, sample.i);
        if (superSampling > 1) {
            // multiple sampling
            sample.add(scene.getRadiance(istate, x, y, q1, q2, q0, sample.i));
            for (int i = 1; i < superSampling; i++) {
                double time = QMC.mod1(q0 + i * invSuperSampling);
                double lensU = QMC.mod1(q1 + QMC.halton(0, i));
                double lensV = QMC.mod1(q2 + QMC.halton(1, i));
                sample.add(scene.getRadiance(istate, x, y, lensU, lensV, time, sample.i + i));
            }
            sample.scale((float) invSuperSampling);
        } else {
            // single sample
            sample.set(scene.getRadiance(istate, x, y, q1, q2, q0, sample.i));
        }
    }

    private void refineSamples(ImageSample[] samples, int sbw, int x, int y, int stepSize, float thresh, IntersectionState istate) {
        int dx = stepSize;
        int dy = stepSize * sbw;
        int i00 = x + y * sbw;
        ImageSample s00 = samples[i00];
        ImageSample s01 = samples[i00 + dy];
        ImageSample s10 = samples[i00 + dx];
        ImageSample s11 = samples[i00 + dx + dy];
        if (!s00.sampled)
            computeSubPixel(s00, istate);
        if (!s01.sampled)
            computeSubPixel(s01, istate);
        if (!s10.sampled)
            computeSubPixel(s10, istate);
        if (!s11.sampled)
            computeSubPixel(s11, istate);
        if (stepSize > minStepSize) {
            if (s00.isDifferent(s01, thresh) || s00.isDifferent(s10, thresh) || s00.isDifferent(s11, thresh) || s01.isDifferent(s11, thresh) || s10.isDifferent(s11, thresh) || s01.isDifferent(s10, thresh)) {
                stepSize >>= 1;
                thresh *= 2;
                refineSamples(samples, sbw, x, y, stepSize, thresh, istate);
                refineSamples(samples, sbw, x + stepSize, y, stepSize, thresh, istate);
                refineSamples(samples, sbw, x, y + stepSize, stepSize, thresh, istate);
                refineSamples(samples, sbw, x + stepSize, y + stepSize, stepSize, thresh, istate);
                return;
            }
        }

        // interpolate remaining samples
        float ds = 1.0f / stepSize;
        for (int i = 0; i <= stepSize; i++)
            for (int j = 0; j <= stepSize; j++)
                if (!samples[x + i + (y + j) * sbw].processed())
                    ImageSample.bilerp(samples[x + i + (y + j) * sbw], s00, s01, s10, s11, i * ds, j * ds);
    }

    private static class ImageSample {
        float rx, ry;
        int i, n;
        Color c;
        Instance instance;
        Shader shader;
        float nx, ny, nz;
        boolean sampled;

        ImageSample(float rx, float ry, int i) {
            this.rx = rx;
            this.ry = ry;
            this.i = i;
            n = 0;
            c = null;
            instance = null;
            shader = null;
            nx = ny = nz = 0;
            sampled = false;
        }

        void set(ShadingState state) {
            if (state == null)
                c = Color.BLACK;
            else {
                c = state.getResult();
                shader = state.getShader();
                instance = state.getInstance();
                if (state.getNormal() != null) {
                    nx = state.getNormal().x;
                    ny = state.getNormal().y;
                    nz = state.getNormal().z;
                }
            }
            sampled = true;
        }

        void add(ShadingState state) {
            if (n == 0) {
                c = Color.black();
                sampled = true;
            }
            if (state != null)
                c.add(state.getResult());
            n++;
        }

        void scale(float s) {
            c.mul(s);
        }

        boolean processed() {
            return c != null;
        }

        boolean isDifferent(ImageSample sample, float thresh) {
            if (instance != sample.instance)
                return true;
            if (shader != sample.shader)
                return true;
            if (shader == null)
                return false;
            if (Color.hasContrast(c, sample.c, thresh))
                return true;
            float dot = (nx * sample.nx + ny * sample.ny + nz * sample.nz);
            return dot < 0.9f;
        }

        static ImageSample bilerp(ImageSample result, ImageSample i00, ImageSample i01, ImageSample i10, ImageSample i11, float dx, float dy) {
            float k00 = (1.0f - dx) * (1.0f - dy);
            float k01 = (1.0f - dx) * dy;
            float k10 = dx * (1.0f - dy);
            float k11 = dx * dy;
            Color c00 = i00.c;
            Color c01 = i01.c;
            Color c10 = i10.c;
            Color c11 = i11.c;
            Color c = Color.mul(k00, c00);
            c.madd(k01, c01);
            c.madd(k10, c10);
            c.madd(k11, c11);
            result.c = c;
            return result;
        }
    }
}