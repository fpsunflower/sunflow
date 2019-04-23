package org.sunflow.core.parameter.light;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sunflow.SunflowAPI;
import org.sunflow.core.primitive.CornellBox;
import org.sunflow.image.Color;
import org.sunflow.math.Point3;

import java.util.Random;

public class CornellBoxLightParameterTest {

    CornellBoxLightParameter light;
    SunflowAPI api;

    @Before
    public void setUp() {
        api = new SunflowAPI();
        light = new CornellBoxLightParameter();
    }

    @Test
    public void testSetupAPI() {
        light.setName("uniqueName");
        light.setSamples(22);
        light.setRadiance(randomColor());
        light.setMin(new Point3(-1,-2,-4));
        light.setMax(new Point3(1,3,5));
        light.setTop(randomColor());
        light.setBottom(randomColor());
        light.setLeft(randomColor());
        light.setRight(randomColor());
        light.setBack(randomColor());

        // Set parameters
        light.setup(api);

        CornellBox l = (CornellBox) api.getRenderObjects().get(light.name).obj;

        Assert.assertEquals(light.getSamples(), l.getNumSamples());
        //Assert.assertArrayEquals(light.getRadiance().getRGB(), l.getRadiance(null).getRGB(), 0);
        Assert.assertArrayEquals(light.getTop().getRGB(), l.getTop().getRGB(), 0);
        Assert.assertArrayEquals(light.getBottom().getRGB(), l.getBottom().getRGB(), 0);
        Assert.assertArrayEquals(light.getLeft().getRGB(), l.getLeft().getRGB(), 0);
        Assert.assertArrayEquals(light.getRight().getRGB(), l.getRight().getRGB(), 0);
        Assert.assertArrayEquals(light.getBack().getRGB(), l.getBack().getRGB(), 0);

        Assert.assertEquals(light.min.x, l.getBounds().getBound(0), 0.1f);
        Assert.assertEquals(light.max.x, l.getBounds().getBound(1), 0.1f);
        Assert.assertEquals(light.min.y, l.getBounds().getBound(2), 0.1f);
        Assert.assertEquals(light.max.y, l.getBounds().getBound(3), 0.1f);
        Assert.assertEquals(light.min.z, l.getBounds().getBound(4), 0.1f);
        Assert.assertEquals(light.max.z, l.getBounds().getBound(5), 0.1f);
    }

    private Color randomColor() {
        Random random = new Random();
        return new Color(random.nextFloat(), random.nextFloat(), random.nextFloat());
    }

}
