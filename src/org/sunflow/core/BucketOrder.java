package org.sunflow.core;

/**
 * Creates an array of coordinates that iterate over the tiled screen. Classes
 * which implement this interface are responsible for guarenteeing the entire
 * screen is tiled. No attempt is made to check for duplicates or incomplete
 * coverage.
 */
public interface BucketOrder {
    /**
     * Computes the order in which each coordinate on the screen should be
     * visited.
     * 
     * @param nbw
     *            Number of buckets in the X direction
     * @param nbh
     *            Number of buckets in the Y direction
     * @return Array of coordinates with interleaved X, Y of the positions of
     *         buckets to be rendered.
     */
    int[] getBucketSequence(int nbw, int nbh);
}