package org.sunflow.core.primitive;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.math.Point3;

public class TriangleMeshTest {

    TriangleMesh mesh;

    @Before
    public void setUp() {
        mesh = new TriangleMesh();
    }

    @Test
    public void testInit() {
        mesh.points = new float[]{0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6, 6, 7, 7, 7, 8, 8, 8};
        mesh.triangles = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};

        Assert.assertEquals(3, mesh.getNumPrimitives());

        Assert.assertEquals(new Point3(0, 0, 0), mesh.getPoint(0));
        Assert.assertEquals(new Point3(1, 1, 1), mesh.getPoint(1));
        Assert.assertEquals(new Point3(2, 2, 2), mesh.getPoint(2));
    }
}
