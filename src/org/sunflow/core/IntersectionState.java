package org.sunflow.core;

public final class IntersectionState {
    private static final int MAX_STACK_SIZE = 64;
    float u, v;
    Primitive object;
    int id;
    StackNode[] stack;
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
    }

    public final StackNode[] getStack() {
        return stack;
    }

    public final int getStackTop() {
        return current == null ? 0 : MAX_STACK_SIZE;
    }
    
    /**
     * Checks to see if a hit has been recorded.
     * 
     * @return <code>true</code> if a hit has been recorded,
     *         <code>false</code> otherwise
     * @see #setIntersection(Primitive, float, float)
     */
    public final boolean hit() {
        return object != null;
    }

    /**
     * Record an intersection with the specified object.The u and v parameters
     * are used to pinpoint the location on the surface if needed.
     * 
     * @param object reference to the object beeing intersected
     * @param id primitive id of the intersected object
     * @param u u surface parameter of the intersection point
     * @param v v surface parameter of the intersection point
     * @see Primitive#intersect(Ray, IntersectionState)
     */
    public final void setIntersection(Primitive object, int id, float u, float v) {
        this.object = object;
        this.id = id;
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
        object = current;
        this.id = id;
        this.u = u;
        this.v = v;
    }
}