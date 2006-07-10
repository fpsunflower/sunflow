package org.sunflow.core;

import org.sunflow.image.Color;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public interface GlobalPhotonMapInterface extends PhotonStore {
    Color getRadiance(Point3 p, Vector3 n);
}