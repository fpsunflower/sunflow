package org.sunflow.core;

public final class IntersectionState {
    float u, v;
    Primitive object;
    int id;
    StackNode[] stack;

    public static final class StackNode {
        public int node;
        public float near;
        public float far;
    }

    public IntersectionState() {
        stack = new StackNode[64];
        for (int i = 0; i < stack.length; i++)
            stack[i] = new StackNode();
    }

    public StackNode[] getStack() {
        return stack;
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
     * @param object
     *            reference to the object beeing intersected
     * @param id
     *            element id of the intersected object
     * @param u
     *            u surface parameter of the intersection point
     * @param v
     *            v surface parameter of the intersection point
     * @see Primitive#intersect(Ray, IntersectionState)
     */
    public final void setIntersection(Primitive object, int id, float u, float v) {
        this.object = object;
        this.id = id;
        this.u = u;
        this.v = v;
    }
}