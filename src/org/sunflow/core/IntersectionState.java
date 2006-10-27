package org.sunflow.core;

public final class IntersectionState {
    private static final int MAX_STACK_SIZE = 64;
    float u, v;
    Instance instance;
    int id;
    private final StackNode[] stack;
    private final float[] rstack;
    Instance current;

    public static final class StackNode {
        public int node;
        public float near;
        public float far;
    }

    public IntersectionState() {
        stack = new StackNode[MAX_STACK_SIZE * 2];
        for (int i = 0; i < stack.length; i++)
            stack[i] = new StackNode();
        rstack = new float[53 * 256];
    }

    public final StackNode[] getStack() {
        return stack;
    }

    public final int getStackTop() {
        return current == null ? 0 : MAX_STACK_SIZE;
    }
    
    public final float[] getRobustStack() {
        return rstack;
    }

    /**
     * Checks to see if a hit has been recorded.
     * 
     * @return <code>true</code> if a hit has been recorded,
     *         <code>false</code> otherwise
     * @see #setIntersection(Primitive, float, float)
     */
    public final boolean hit() {
        return instance != null;
    }

    /**
     * Record an intersection with the specified object.The u and v parameters
     * are used to pinpoint the location on the surface if needed.
     * 
     * @param object reference to the object beeing intersected
     * @param u u surface parameter of the intersection point
     * @param v v surface parameter of the intersection point
     * @see Primitive#intersect(Ray, IntersectionState)
     */
    public final void setIntersection(Instance object, float u, float v) {
        instance = object;
        this.u = u;
        this.v = v;
    }

    /**
     * Record an intersection with the specified primitive id. The parent object
     * is assumed to be the current instance. The u and v parameters are used to
     * pinpoint the location on the surface if needed.
     * 
     * @param id primitive id of the intersected object
     * @param u u surface paramater of the intersection point
     * @param v v surface parameter of the intersection point
     */
    public final void setIntersection(int id, float u, float v) {
        instance = current;
        this.id = id;
        this.u = u;
        this.v = v;
    }
}