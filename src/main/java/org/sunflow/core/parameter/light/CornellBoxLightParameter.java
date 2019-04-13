package org.sunflow.core.parameter.light;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.image.Color;
import org.sunflow.math.Point3;

public class CornellBoxLightParameter extends LightParameter {

    public static final String PARAM_MIN_CORNER = "corner0";
    public static final String PARAM_MAX_CORNER = "corner1";
    public static final String PARAM_LEFT_COLOR = "leftColor";
    public static final String PARAM_RIGHT_COLOR = "rightColor";
    public static final String PARAM_TOP_COLOR = "topColor";
    public static final String PARAM_BOTTOM_COLOR = "bottomColor";
    public static final String PARAM_BACK_COLOR = "backColor";

    int samples;
    Point3 min;
    Point3 max;
    Color left, right, top, bottom, back;
    Color radiance;

    public CornellBoxLightParameter() {
        generateUniqueName("cornellbox");
    }

    @Override
    public void setup(SunflowAPIInterface api) {
        api.parameter(PARAM_MIN_CORNER, min);
        api.parameter(PARAM_MAX_CORNER, max);
        api.parameter(PARAM_LEFT_COLOR, null, left.getRGB());
        api.parameter(PARAM_RIGHT_COLOR, null, right.getRGB());
        api.parameter(PARAM_TOP_COLOR, null, top.getRGB());
        api.parameter(PARAM_BOTTOM_COLOR, null, bottom.getRGB());
        api.parameter(PARAM_BACK_COLOR, null, back.getRGB());
        api.parameter(PARAM_RADIANCE, null, radiance.getRGB());
        api.parameter(PARAM_SAMPLES, samples);

        api.light(name, TYPE_CORNELL_BOX);
    }

    public Point3 getMin() {
        return min;
    }

    public void setMin(Point3 min) {
        this.min = min;
    }

    public Point3 getMax() {
        return max;
    }

    public void setMax(Point3 max) {
        this.max = max;
    }

    public Color getLeft() {
        return left;
    }

    public void setLeft(Color left) {
        this.left = left;
    }

    public Color getRight() {
        return right;
    }

    public void setRight(Color right) {
        this.right = right;
    }

    public Color getTop() {
        return top;
    }

    public void setTop(Color top) {
        this.top = top;
    }

    public Color getBottom() {
        return bottom;
    }

    public void setBottom(Color bottom) {
        this.bottom = bottom;
    }

    public Color getBack() {
        return back;
    }

    public void setBack(Color back) {
        this.back = back;
    }

    public Color getRadiance() {
        return radiance;
    }

    public void setRadiance(Color radiance) {
        this.radiance = radiance;
    }

    public int getSamples() {
        return samples;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }
}
