package org.sunflow.core;

import org.sunflow.image.Color;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

/**
 * The <code>LightSource</code> interface is used to represent any light
 * emitting primitive. It permits efficient sampling of direct illumination.
 */
public interface LightSource {
    /**
     * Returns true if adaptive sampling can be used
     * 
     * @return <code>true</code> if this light source can be adaptively
     *         sampled, <code>false</code> otherwise
     */
    boolean isAdaptive();

    /**
     * Return the number of samples to take from this light source
     * 
     * @return a number of samples, must be greater than 0
     */
    int getNumSamples();

    /**
     * Checks to see if the light is trivally visible from the current render
     * state.
     * 
     * @param state
     *            currente render state
     * @return <code>true</code> if the light source is visible,
     *         <code>false</code> otherwise
     */
    boolean isVisible(ShadingState state);

    /**
     * Creates a light sample on the light source that points towards the vertex
     * in the current state. This method will determine if it is necessary to
     * trace shadows for this sample. If a light sample has null direction or
     * radiance it will be treated as invisible. A null shadow ray simply
     * indicates the sample should not cast shadows.
     * 
     * @param i
     *            current sample number
     * @param state
     *            current state, including point to be
     * @param dest
     *            light sample to be filled in
     * @see LightSample
     */
    void getSample(int i, ShadingState state, LightSample dest);

    /**
     * Gets a photon to emit from this light source by setting each of the
     * arguments. The two sampling parameters are points on the unit square that
     * can be used to sample a position and/or direction for the emitted photon.
     * 
     * @param randX1
     *            sampling parameter
     * @param randY1
     *            sampling parameter
     * @param randX2
     *            sampling parameter
     * @param randY2
     *            sampling parameter
     * @param p
     *            position to shoot the photon from
     * @param dir
     *            direction to shoot the photon in
     * @param power
     *            power of the photon
     */
    void getPhoton(double randX1, double randY1, double randX2, double randY2, Point3 p, Vector3 dir, Color power);

    /**
     * Get the total power emitted by this light source. Lights that have 0
     * power will not emit any photons.
     * 
     * @return light source power
     */
    float getPower();
}