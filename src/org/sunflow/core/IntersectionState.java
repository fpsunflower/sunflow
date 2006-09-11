package org.sunflow.core;

public final class IntersectionState {
    public float u, v;
    public Primitive object;
    public StackNode[] stack;

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
     * @param hitU
     *            u surface parameter of the intersection point
     * @param hitV
     *            v surface parameter of the intersection point
     * @see Primitive#intersect(Ray, IntersectionState)
     */
    public final void setIntersection(Primitive object, float hitU, float hitV) {
        this.object = object;
        u = hitU;
        v = hitV;
    }
}