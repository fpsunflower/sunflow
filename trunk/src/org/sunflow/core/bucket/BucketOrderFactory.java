package org.sunflow.core.bucket;

import org.sunflow.core.BucketOrder;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class BucketOrderFactory {
    public static BucketOrder create(String order) {
        boolean flip = false;
        if (order.startsWith("inverse") || order.startsWith("invert") || order.startsWith("reverse")) {
            String[] tokens = order.split("\\s+");
            if (tokens.length == 2) {
                order = tokens[1];
                flip = true;
            }
        }
        BucketOrder o = null;
        if (order.equals("row"))
            o = new RowBucketOrder();
        else if (order.equals("column"))
            o = new ColumnBucketOrder();
        else if (order.equals("diagonal"))
            o = new DiagonalBucketOrder();
        else if (order.equals("spiral"))
            o = new SpiralBucketOrder();
        else if (order.equals("hilbert"))
            o = new HilbertBucketOrder();
        else if (order.equals("random"))
            o = new RandomBucketOrder();
        if (o == null) {
            UI.printWarning(Module.BCKT, "Unrecognized bucket ordering: \"%s\" - using hilbert", order);
            return new HilbertBucketOrder();
        } else {
            if (flip)
                o = new InvertedBucketOrder(o);
            return o;
        }
    }
}