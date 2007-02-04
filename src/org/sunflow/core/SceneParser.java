package org.sunflow.core;

import org.sunflow.SunflowAPI;

/**
 * Simple interface to allow for scene creation from arbitrary file formats.
 */
public interface SceneParser {
    /**
     * Parse the specified file to create a scene description into the provided
     * {@link SunflowAPI} object.
     * 
     * @param filename filename to parse
     * @param api scene to parse the file into
     * @return <code>true</code> upon sucess, or <code>false</code> if
     *         errors have occured.
     */
    public boolean parse(String filename, SunflowAPI api);
}