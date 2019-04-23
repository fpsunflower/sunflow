package org.sunflow.core.parameter;

import org.sunflow.SunflowAPI;
import org.sunflow.SunflowAPIInterface;

public class BucketParameter implements Parameter {

    public static final String PARAM_BUCKET_SIZE = "bucket.size";
    public static final String PARAM_BUCKET_ORDER = "bucket.order";
    public static final String ORDER_COLUMN = "column";
    public static final String ORDER_DIAGONAL = "diagonal";
    public static final String ORDER_HILBERT = "hilbert";
    public static final String ORDER_SPIRAL = "spiral";
    public static final String ORDER_RANDOM = "random";
    public static final String ORDER_ROW = "row";

    private int size = 0;
    private String order = "";

    @Override
    public void setup(SunflowAPIInterface api) {
        if (size > 0) {
            api.parameter(PARAM_BUCKET_SIZE, size);
        }
        if (!order.isEmpty()) {
            api.parameter(PARAM_BUCKET_ORDER, order);
        }

        api.options(SunflowAPI.DEFAULT_OPTIONS);
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }
}
