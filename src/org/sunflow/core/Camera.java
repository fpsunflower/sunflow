package org.sunflow.core;

/**
 * Represents a mapping from the 3D scene onto the final image. A camera is
 * responsible for determining what ray to cast through each pixel.
 */
public interface Camera {
    /**
     * Create a new {@link Ray ray}to be cast through pixel (x,y) on the image
     * plane. Two sampling parameters are provided for lens sampling. They are
     * guarenteed to be in the interval [0,1). They can be used to perturb the
     * position of the source of the ray on the lens of the camera for DOF
     * effects.
     * 
     * @param x x coordinate of the (sub)pixel
     * @param y y coordinate of the (sub)pixel
     * @param imageWidth image width in (sub)pixels
     * @param imageHeight image height in (sub)pixels
     * @param lensX x lens sampling parameter
     * @param lensY y lens sampling parameter
     * @return a new ray passing through the given pixel
     */
    public Ray getRay(float x, float y, int imageWidth, int imageHeight, double lensX, double lensY, double time);
}