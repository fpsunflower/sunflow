package org.sunflow.core;

/**
 * This class is used to store ray/object intersections. It also provides
 * additional data to assist {@link AccelerationStructure} objects with
 * traversal.
 */
public final class IntersectionState {
    private static final int MAX_STACK_SIZE = 64;
    float u, v;
    Instance instance;
    int id;
    private final StackNode[][] stacks = new StackNode[2][MAX_STACK_SIZE];
    private final float[] rstack;
    Instance current;

    /**
     * Traversal stack node, helps with tree-based {@link AccelerationStructure}
     * traversal.
     */
    public static final class StackNode {
        public int node;
        public float near;
        public float far;
    }

    /**
     * Initializes all traversal stacks.
     */
    public IntersectionState() {
        for (int i = 0; i < stacks.length; i++)
            for (int j = 0; j < stacks[i].length; j++)
                stacks[i][j] = new StackNode();
        rstack = new float[53 * 256];
    }

    /**
     * Get stack object for tree based {@link AccelerationStructure}s.
     * 
     * @return array of stack nodes
     */
    public final StackNode[] getStack() {
        return current == null ? stacks[0] : stacks[1];
    }

    /**
     * Used for algorithms which do bounding box based ray intersection.
     * 
     * @return array of floating point values for the stack
     */
    public final float[] getRobustStack() {
        return rstack;
    }

    /**
     * Checks to see if a hit has been recorded.
     * 
     * @return <code>true</code> if a hit has been recorded,
     *         <code>false</code> otherwise
     */
    public final boolean hit() {
        return instance != null;
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