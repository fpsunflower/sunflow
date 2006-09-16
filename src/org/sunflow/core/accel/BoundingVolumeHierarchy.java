package org.sunflow.core.accel;

import java.util.ArrayList;

import org.sunflow.core.BoundedPrimitive;
import org.sunflow.core.IntersectionAccelerator;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.Ray;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;

public class BoundingVolumeHierarchy implements IntersectionAccelerator {
    private BoundingBox bounds;
    private Node[] nodes;

    public BoundingVolumeHierarchy() {
        bounds = null;
        nodes = null;
    }

    public boolean build(ArrayList<BoundedPrimitive> objects) {
        Timer t = new Timer();
        t.start();
        bounds = new BoundingBox();

        for (BoundedPrimitive p : objects)
            bounds.include(p.getBounds());
        BuildNode root = new BuildNode();
        for (BoundedPrimitive p : objects)
            root.add(p);
        root.split();
        // build array version of the tree
        // count boxes
        nodes = new Node[root.numNodes()];
        // write nodes into the array as specified by the depth first traversal
        // of the tree
        root.copyNodes(nodes, 0);
        t.end();

        // cleanup
        UI.printInfo("[BVH] Bounding volume hierarchy stats:");
        UI.printInfo("[BVH]  * Nodes:      %d", nodes.length);
        UI.printInfo("[BVH]  * Build time: %s", t.toString());
        return true;
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    public void intersect(Ray r, IntersectionState state) {
        int curr = 0;
        float intervalMin, intervalMax;
        float t1, t2;
        float orgX = r.ox;
        float orgY = r.oy;
        float orgZ = r.oz;
        float invDirX = 1.0f / r.dx;
        float invDirY = 1.0f / r.dy;
        float invDirZ = 1.0f / r.dz;
        boolean testCurr;
        while (curr < nodes.length) {
            // test for intersection of ray against bounding box
            testCurr = true;
            if (nodes[curr].b != null) {
                intervalMin = r.getMin();
                intervalMax = r.getMax();
                Point3 bMin = nodes[curr].b.getMinimum();
                Point3 bMax = nodes[curr].b.getMaximum();
                t1 = (bMin.x - orgX) * invDirX;
                t2 = (bMax.x - orgX) * invDirX;
                if (invDirX > 0) {
                    if (t1 > intervalMin)
                        intervalMin = t1;
                    if (t2 < intervalMax)
                        intervalMax = t2;
                    if (intervalMin > intervalMax)
                        testCurr = false;
                } else {
                    if (t2 > intervalMin)
                        intervalMin = t2;
                    if (t1 < intervalMax)
                        intervalMax = t1;
                    if (intervalMin > intervalMax)
                        testCurr = false;
                }
                if (testCurr) {
                    t1 = (bMin.y - orgY) * invDirY;
                    t2 = (bMax.y - orgY) * invDirY;
                    if (invDirY > 0) {
                        if (t1 > intervalMin)
                            intervalMin = t1;
                        if (t2 < intervalMax)
                            intervalMax = t2;
                        if (intervalMin > intervalMax)
                            testCurr = false;
                    } else {
                        if (t2 > intervalMin)
                            intervalMin = t2;
                        if (t1 < intervalMax)
                            intervalMax = t1;
                        if (intervalMin > intervalMax)
                            testCurr = false;
                    }
                }
                if (testCurr) {
                    t1 = (bMin.z - orgZ) * invDirZ;
                    t2 = (bMax.z - orgZ) * invDirZ;
                    if (invDirZ > 0) {
                        if (t1 > intervalMin)
                            intervalMin = t1;
                        if (t2 < intervalMax)
                            intervalMax = t2;
                        if (intervalMin > intervalMax)
                            testCurr = false;
                    } else {
                        if (t2 > intervalMin)
                            intervalMin = t2;
                        if (t1 < intervalMax)
                            intervalMax = t1;
                        if (intervalMin > intervalMax)
                            testCurr = false;
                    }
                }
            }
            if (testCurr) {
                // we actually hit the bounding box
                // intersect the primitive if there is one
                if (nodes[curr].i != null)
                    nodes[curr].i.intersect(r, state);
                curr++;
            } else
                curr = nodes[curr].skipIndex;
        }
    }

    private static class Node {
        BoundingBox b;
        BoundedPrimitive i;
        int skipIndex;

        Node() {
            b = null;
            i = null;
            skipIndex = -1;
        }
    }

    private static class BuildNode {
        BoundingBox b;
        ArrayList<BoundedPrimitive> list;
        BuildNode left;
        BuildNode right;

        BuildNode() {
            b = new BoundingBox();
            list = new ArrayList<BoundedPrimitive>();
            left = right = null;
        }

        int numNodes() {
            if (left == null && right == null)
                return list == null ? 0 : 1;
            return 1 + left.numNodes() + right.numNodes();
        }

        void add(BoundedPrimitive obj) {
            list.add(obj);
            b.include(obj.getBounds());
        }

        private float getValue(int axis, int index) {
            Point3 p = list.get(index).getBounds().getCenter();
            switch (axis) {
                case 0:
                    return p.x;
                case 1:
                    return p.y;
                default:
                    return p.z;
            }
        }

        private void swap(int a, int b) {
            BoundedPrimitive p = list.get(a);
            list.set(a, list.get(b));
            list.set(b, p);
        }

        private void sort(int axis, int start, int end) {
            // slow quicksort
            if (start < end) {
                float partitionValue = getValue(axis, (start + end) / 2);
                int left = start;
                int right = end;
                while (left <= right) {
                    while (left < end && partitionValue > getValue(axis, left))
                        left++;
                    while (right > start && partitionValue < getValue(axis, right))
                        right--;
                    if (left <= right) {
                        swap(left, right);
                        left++;
                        right--;
                    }
                }
                if (start < right)
                    sort(axis, start, right);
                if (left < end)
                    sort(axis, left, end);
            }
        }

        void split() {
            if (list.size() == 0) {
                list = null;
                return;
            }
            if (list.size() == 1)
                return;
            left = new BuildNode();
            right = new BuildNode();
            // pick longest axis
            Vector3 w = b.getExtents();
            int axis;
            if (Math.abs(w.x) > Math.abs(w.y) && Math.abs(w.x) > Math.abs(w.z))
                axis = 0;
            else if (Math.abs(w.y) > Math.abs(w.z))
                axis = 1;
            else
                axis = 2;
            // sort along longest axis
            sort(axis, 0, list.size() - 1);
            // float min = list.size();
            // partition into left and right
            int mid = list.size() / 2;
            for (int i = 0; i < mid; i++)
                left.add(list.get(i));
            for (int i = mid; i < list.size(); i++)
                right.add(list.get(i));
            list = null;
            left.split();
            right.split();
        }

        int copyNodes(Node[] nodes, int start) {
            if (left == null && right == null) {
                // leaf node
                if (list == null)
                    return start;
                nodes[start] = new Node();
                nodes[start].i = list.get(0);
                nodes[start].skipIndex = start + 1;
                return start + 1;
            }
            // split node
            nodes[start] = new Node();
            nodes[start].b = b;
            nodes[start].skipIndex = left.copyNodes(nodes, start + 1);
            return right.copyNodes(nodes, nodes[start].skipIndex);
        }
    }
}