package org.sunflow.core;

import org.sunflow.SunflowAPI;

public interface SceneParser {
    public boolean parse(String filename, SunflowAPI api);
}