package org.sunflow.core.light;

import org.sunflow.core.IntersectionState;
import org.sunflow.core.LightSample;
import org.sunflow.core.LightSource;
import org.sunflow.core.Primitive;
import org.sunflow.core.Ray;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.core.Texture;
import org.sunflow.core.TextureCache;
import org.sunflow.image.Bitmap;
import org.sunflow.image.Color;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.QMC;
import org.sunflow.math.Vector3;

public class ImageBasedLight implements Primitive, LightSource, Shader {
    private Texture texture;
    private OrthoNormalBasis basis;
    private int numSamples;
    private float jacobian;
    private float[] colHistogram;
    private float[][] imageHistogram;
    private Vector3[] samples;
    private Color[] colors;

    public ImageBasedLight(String filename, Vector3 center, Vector3 up, int numSamples, boolean lockSamples) {
        texture = TextureCache.getTexture(filename);

        basis = OrthoNormalBasis.makeFromWV(center, up);
        basis.swapWU();
        basis.flipV();
        Bitmap b = texture.getBitmap();
        if (b == null)
            numSamples = 0;
        this.numSamples = numSamples;

        imageHistogram = new float[b.getWidth()][b.getHeight()];
        colHistogram = new float[b.getWidth()];
        float du = 1.0f / b.getWidth();
        float dv = 1.0f / b.getHeight();
        for (int x = 0; x < b.getWidth(); x++) {
            for (int y = 0; y < b.getHeight(); y++) {
                float u = (x + 0.5f) * du;
                float v = (y + 0.5f) * dv;
                Color c = texture.getPixel(u, v);
                // box filter the image
                // c.add(texture.getPixel(u + du, v));
                // c.add(texture.getPixel(u + du, v+ dv));
                // c.add(texture.getPixel(u, v + dv));
                // c.mul(0.25f);
                imageHistogram[x][y] = c.getLuminance() * (float) Math.sin(Math.PI * v);
                if (y > 0)
                    imageHistogram[x][y] += imageHistogram[x][y - 1];
            }
            colHistogram[x] = imageHistogram[x][b.getHeight() - 1];
            if (x > 0)
                colHistogram[x] += colHistogram[x - 1];
            for (int y = 0; y < b.getHeight(); y++)
                imageHistogram[x][y] /= imageHistogram[x][b.getHeight() - 1];
        }
        for (int x = 0; x < b.getWidth(); x++)
            colHistogram[x] /= colHistogram[b.getWidth() - 1];
        jacobian = (float) (2 * Math.PI * Math.PI) / (b.getWidth() * b.getHeight());

        if (lockSamples) {
            // Bitmap loc = new Bitmap(filename);
            samples = new Vector3[numSamples];
            colors = new Color[numSamples];
            for (int i = 0; i < numSamples; i++) {
                double randX = (double) i / (double) numSamples;
                double randY = QMC.halton(0, i);
                int x = 0;
                while (randX >= colHistogram[x] && x < colHistogram.length - 1)
                    x++;
                float[] rowHistogram = imageHistogram[x];
                int y = 0;
                while (randY >= rowHistogram[y] && y < rowHistogram.length - 1)
                    y++;
                // sample from (x, y)
                float u = (float) ((x == 0) ? (randX / colHistogram[0]) : ((randX - colHistogram[x - 1]) / (colHistogram[x] - colHistogram[x - 1])));
                float v = (float) ((y == 0) ? (randY / rowHistogram[0]) : ((randY - rowHistogram[y - 1]) / (rowHistogram[y] - rowHistogram[y - 1])));

                float px = ((x == 0) ? colHistogram[0] : (colHistogram[x] - colHistogram[x - 1]));
                float py = ((y == 0) ? rowHistogram[0] : (rowHistogram[y] - rowHistogram[y - 1]));

                float su = (x + u) / colHistogram.length;
                float sv = (y + v) / rowHistogram.length;

                float invP = (float) Math.sin(sv * Math.PI) * jacobian / (px * py);
                samples[i] = getDirection(su, sv);
                basis.transform(samples[i]);
                colors[i] = texture.getPixel(su, sv).mul(invP);
                // loc.setPixel(x, y, Color.YELLOW.copy().mul(1e6f));
            }
            // loc.save("samples.hdr");
            // histograms are no longer needed
            colHistogram = null;
            imageHistogram = null;
        }
    }

    public void prepareShadingState(ShadingState state) {
        if (state.includeLights())
            state.setShader(this);
    }

    public void intersect(Ray r, IntersectionState state) {
        if (r.getMax() == Float.POSITIVE_INFINITY)
            state.setIntersection(this, 0, 0, 0);
    }

    public int getNumSamples() {
        return numSamples;
    }

    public boolean isVisible(ShadingState state) {
        return true;
    }

    public void getSample(int i, ShadingState state, LightSample dest) {
        if (samples == null) {
            // random offset on unit square, we use the infinite version of
            // getRandom because the light sampling is adaptive
            double randX = state.getRandom(i, 0);
            double randY = state.getRandom(i, 1);

            int x = 0;
            while (randX >= colHistogram[x] && x < colHistogram.length - 1)
                x++;
            float[] rowHistogram = imageHistogram[x];
            int y = 0;
            while (randY >= rowHistogram[y] && y < rowHistogram.length - 1)
                y++;
            // sample from (x, y)
            float u = (float) ((x == 0) ? (randX / colHistogram[0]) : ((randX - colHistogram[x - 1]) / (colHistogram[x] - colHistogram[x - 1])));
            float v = (float) ((y == 0) ? (randY / rowHistogram[0]) : ((randY - rowHistogram[y - 1]) / (rowHistogram[y] - rowHistogram[y - 1])));

            float px = ((x == 0) ? colHistogram[0] : (colHistogram[x] - colHistogram[x - 1]));
            float py = ((y == 0) ? rowHistogram[0] : (rowHistogram[y] - rowHistogram[y - 1]));

            float su = (x + u) / colHistogram.length;
            float sv = (y + v) / rowHistogram.length;
            float invP = (float) Math.sin(sv * Math.PI) * jacobian / (px * py);
            Vector3 dir = getDirection(su, sv);
            basis.transform(dir);
            if (Vector3.dot(dir, state.getNormal()) > 0) {
                dest.setShadowRay(new Ray(state.getPoint(), dir));
                dest.getShadowRay().setMax(Float.MAX_VALUE);
                Color radiance = texture.getPixel(su, sv);
                dest.setRadiance(radiance, radiance);
                dest.getDiffuseRadiance().mul(invP);
                dest.getSpecularRadiance().mul(invP);
                dest.traceShadow(state);
            }
        } else {
            if (Vector3.dot(samples[i], state.getNormal()) > 0) {
                dest.setShadowRay(new Ray(state.getPoint(), samples[i]));
                dest.getShadowRay().setMax(Float.MAX_VALUE);
                dest.setRadiance(colors[i], colors[i]);
                dest.traceShadow(state);
            }
        }
    }

    public void getPhoton(double randX1, double randY1, double randX2, double randY2, Point3 p, Vector3 dir, Color power) {
    }

    public Color getRadiance(ShadingState state) {
        // lookup texture based on ray direction
        return state.includeLights() ? getColor(basis.untransform(state.getRay().getDirection(), new Vector3())) : Color.BLACK;
    }

    private Color getColor(Vector3 dir) {
        float u, v;
        // assume lon/lat format
        double phi = 0, theta = 0;
        phi = Math.acos(dir.y);
        theta = Math.atan2(dir.z, dir.x);
        u = (float) (0.5 - 0.5 * theta / Math.PI);
        v = (float) (phi / Math.PI);
        return texture.getPixel(u, v);
    }

    private Vector3 getDirection(float u, float v) {
        Vector3 dest = new Vector3();
        double phi = 0, theta = 0;
        theta = u * 2 * Math.PI;
        phi = v * Math.PI;
        double sin_phi = Math.sin(phi);
        dest.x = (float) (-sin_phi * Math.cos(theta));
        dest.y = (float) Math.cos(phi);
        dest.z = (float) (sin_phi * Math.sin(theta));
        return dest;
    }

    public void scatterPhoton(ShadingState state, Color power) {
    }

    public boolean isAdaptive() {
        return samples == null;
    }

    public float getPower() {
        return 0;
    }
}