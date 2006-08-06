package org.sunflow.core.accel;

import java.util.ArrayList;

import org.sunflow.core.BoundedPrimitive;
import org.sunflow.core.IntersectionAccelerator;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.Ray;
import org.sunflow.math.BoundingBox;
import org.sunflow.system.Memory;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.util.IntArray;

public class BoundingIntervalHierarchy implements IntersectionAccelerator {
    private int[] tree;
    private BoundedPrimitive[] objects;
    private BoundingBox bounds;
    private int maxPrims;

    public BoundingIntervalHierarchy() {
        maxPrims = 2;
    }

    public boolean build(final ArrayList<BoundedPrimitive> objects) {
        this.objects = objects.toArray(new BoundedPrimitive[objects.size()]);
        int[] indices = new int[this.objects.length];
        int i = 0;
        bounds = new BoundingBox();
        UI.printInfo("[BIH] Storing bounding boxes ...");
        for (BoundedPrimitive prim : this.objects) {
            BoundingBox b = prim.getBounds();
            bounds.include(b);
            indices[i] = i;
            i++;
        }
        UI.printInfo("[BIH] Creating tree ...");
        int initialSize = 3 * (2 * 6 * this.objects.length + 1);
        IntArray tempTree = new IntArray((initialSize + 3) / 4);
        BuildStats stats = new BuildStats();
        Timer t = new Timer();
        t.start();
        buildHierarchy(tempTree, indices, stats);
        t.end();
        UI.printInfo("[BIH] Trimming tree ...");
        tree = tempTree.trim();
        tempTree = null; // free memory
        UI.printInfo("[BIH] Sorting primitive pointers ...");
        // resort pointers
        BoundedPrimitive[] prims = new BoundedPrimitive[this.objects.length];
        for (i = 0; i < prims.length; i++)
            prims[i] = this.objects[indices[i]];
        this.objects = prims;
        // free memory
        indices = null;
        // gather stats
        stats.printStats();
        UI.printInfo("[BIH]   * Creation time:  %s", t);
        UI.printInfo("[BIH]   * Usage of init:  %3d%%", 100 * tree.length / initialSize);
        UI.printInfo("[BIH]   * Tree memory:    %s", Memory.sizeof(tree));
        return true;
    }

    private static class BuildStats {
        private int numNodes;
        private int numLeaves;
        private int sumObjects;
        private int minObjects;
        private int maxObjects;
        private int sumDepth;
        private int minDepth;
        private int maxDepth;
        private int numLeaves0;
        private int numLeaves1;
        private int numLeaves2;
        private int numLeaves3;
        private int numLeaves4;
        private int numLeaves4p;

        BuildStats() {
            numNodes = numLeaves = 0;
            sumObjects = 0;
            minObjects = Integer.MAX_VALUE;
            maxObjects = Integer.MIN_VALUE;
            sumDepth = 0;
            minDepth = Integer.MAX_VALUE;
            maxDepth = Integer.MIN_VALUE;
            numLeaves0 = 0;
            numLeaves1 = 0;
            numLeaves2 = 0;
            numLeaves3 = 0;
            numLeaves4 = 0;
            numLeaves4p = 0;
        }

        void updateInner() {
            numNodes++;
        }

        void updateLeaf(int depth, int n) {
            numLeaves++;
            minDepth = Math.min(depth, minDepth);
            maxDepth = Math.max(depth, maxDepth);
            sumDepth += depth;
            minObjects = Math.min(n, minObjects);
            maxObjects = Math.max(n, maxObjects);
            sumObjects += n;
            switch (n) {
                case 0:
                    numLeaves0++;
                    break;
                case 1:
                    numLeaves1++;
                    break;
                case 2:
                    numLeaves2++;
                    break;
                case 3:
                    numLeaves3++;
                    break;
                case 4:
                    numLeaves4++;
                    break;
                default:
                    numLeaves4p++;
                    break;
            }
        }

        void printStats() {
            UI.printInfo("[BIH] Tree stats:");
            UI.printInfo("[BIH]   * Nodes:          %d", numNodes);
            UI.printInfo("[BIH]   * Leaves:         %d", numLeaves);
            UI.printInfo("[BIH]   * Objects: min    %d", minObjects);
            UI.printInfo("[BIH]              avg    %.2f", (float) sumObjects / numLeaves);
            UI.printInfo("[BIH]            avg(n>0) %.2f", (float) sumObjects / (numLeaves - numLeaves0));
            UI.printInfo("[BIH]              max    %d", maxObjects);
            UI.printInfo("[BIH]   * Depth:   min    %d", minDepth);
            UI.printInfo("[BIH]              avg    %.2f", (float) sumDepth / numLeaves);
            UI.printInfo("[BIH]              max    %d", maxDepth);
            UI.printInfo("[BIH]   * Leaves w/: N=0  %3d%%", 100 * numLeaves0 / numLeaves);
            UI.printInfo("[BIH]                N=1  %3d%%", 100 * numLeaves1 / numLeaves);
            UI.printInfo("[BIH]                N=2  %3d%%", 100 * numLeaves2 / numLeaves);
            UI.printInfo("[BIH]                N=3  %3d%%", 100 * numLeaves3 / numLeaves);
            UI.printInfo("[BIH]                N=4  %3d%%", 100 * numLeaves4 / numLeaves);
            UI.printInfo("[BIH]                N>4  %3d%%", 100 * numLeaves4p / numLeaves);
        }
    }

    private void buildHierarchy(IntArray tempTree, int[] indices, BuildStats stats) {
        // create space for the first node
        tempTree.add(3 << 30); // dummy leaf
        tempTree.add(0);
        tempTree.add(0);
        if (objects.length == 0)
            return;
        // seed bbox
        float[] gridBox = { bounds.getMinimum().x, bounds.getMaximum().x, bounds.getMinimum().y, bounds.getMaximum().y, bounds.getMinimum().z, bounds.getMaximum().z };
        float[] nodeBox = { bounds.getMinimum().x, bounds.getMaximum().x, bounds.getMinimum().y, bounds.getMaximum().y, bounds.getMinimum().z, bounds.getMaximum().z };
        // seed subdivide function
        subdivide(0, objects.length - 1, tempTree, indices, gridBox, nodeBox, 0, 1, stats);
    }

    private void createNode(IntArray tempTree, int nodeIndex, int left, int right) {
        // write leaf node
        tempTree.set(nodeIndex + 0, (3 << 30) | left);
        tempTree.set(nodeIndex + 1, right - left + 1);
    }

    private void subdivide(int left, int right, IntArray tempTree, int[] indices, float[] gridBox, float[] nodeBox, int nodeIndex, int depth, BuildStats stats) {
        if ((right - left + 1) <= maxPrims || depth >= 64) {
            // write leaf node
            stats.updateLeaf(depth, right - left + 1);
            createNode(tempTree, nodeIndex, left, right);
            return;
        }
        // calculate extents
        int axis = -1, prevAxis, rightOrig;
        float clipL = Float.NaN, clipR = Float.NaN;
        float split = Float.NaN, prevSplit;
        boolean wasLeft = true;
        while (true) {
            prevAxis = axis;
            prevSplit = split;
            // perform quick consistency checks
            float d[] = { gridBox[1] - gridBox[0], gridBox[3] - gridBox[2], gridBox[5] - gridBox[4] };
            if (d[0] < 0 || d[1] < 0 || d[2] < 0)
                throw new IllegalStateException("negative node extents");
            for (int i = 0; i < 3; i++) {
                if (nodeBox[2 * i + 1] < gridBox[2 * i] || nodeBox[2 * i] > gridBox[2 * i + 1]) {
                    UI.printError("[BIH] Reached tree area in error - discarding node with: %d objects", right - left + 1);
                    throw new IllegalStateException("invalid node overlap");
                }
            }
            // find longest axis
            if (d[0] > d[1] && d[0] > d[2])
                axis = 0;
            else if (d[1] > d[2])
                axis = 1;
            else
                axis = 2;
            split = 0.5f * (gridBox[2 * axis] + gridBox[2 * axis + 1]);
            if (nodeBox[2 * axis + 1] < split) {
                // node is completely on the left - keep looping on that half
                gridBox[2 * axis + 1] = split;
                wasLeft = true;
                continue;
            }
            if (nodeBox[2 * axis + 0] > split) {
                // node is completely on the right - keep looping on that half
                gridBox[2 * axis + 0] = split;
                wasLeft = false;
                continue;
            }
            if (prevAxis != -1) {
                // second time through - lets create the previous split
                // since it produced empty space
                int nextIndex = tempTree.getSize();
                // allocate child node
                tempTree.add(0);
                tempTree.add(0);
                tempTree.add(0);
                if (wasLeft) {
                    // create a node with a left child
                    // write leaf node
                    stats.updateInner();
                    tempTree.set(nodeIndex + 0, (prevAxis << 30) | nextIndex);
                    tempTree.set(nodeIndex + 1, Float.floatToRawIntBits(nodeBox[2 * prevAxis + 1]));
                    tempTree.set(nodeIndex + 2, Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
                } else {
                    // create a node with a right child
                    // write leaf node
                    stats.updateInner();
                    tempTree.set(nodeIndex + 0, (prevAxis << 30) | (nextIndex - 3));
                    tempTree.set(nodeIndex + 1, Float.floatToRawIntBits(Float.NEGATIVE_INFINITY));
                    tempTree.set(nodeIndex + 2, Float.floatToRawIntBits(nodeBox[2 * prevAxis + 0]));
                }
                // count stats for the unused leaf
                depth++;
                stats.updateLeaf(depth, 0);
                // now we keep going as we are, with a new nodeIndex:
                nodeIndex = nextIndex;
            }
            // partition L/R subsets
            clipL = Float.NEGATIVE_INFINITY;
            clipR = Float.POSITIVE_INFINITY;
            rightOrig = right; // save this for later
            for (int i = left; i <= right;) {
                int obj = indices[i];
                float minb = objects[obj].getBound(2 * axis + 0);
                float maxb = objects[obj].getBound(2 * axis + 1);
                float center = (minb + maxb) * 0.5f;
                if (center <= split) {
                    // stay left
                    i++;
                    if (clipL < maxb)
                        clipL = maxb;
                } else {
                    // move to the right most
                    int t = indices[i];
                    indices[i] = indices[right];
                    indices[right] = t;
                    right--;
                    if (clipR > minb)
                        clipR = minb;
                }
            }
            // ensure we are making progress in the subdivision
            if (right == rightOrig) {
                // all left
                if (prevAxis == axis && prevSplit == split) {
                    // we are stuck here - create a leaf
                    stats.updateLeaf(depth, right - left + 1);
                    createNode(tempTree, nodeIndex, left, right);
                    return;
                }
                gridBox[2 * axis + 1] = split;
            } else if (left > right) {
                // all right
                right = rightOrig;
                if (prevAxis == axis && prevSplit == split) {
                    // we are stuck here - create a leaf
                    stats.updateLeaf(depth, right - left + 1);
                    createNode(tempTree, nodeIndex, left, right);
                    return;
                }
                gridBox[2 * axis + 0] = split;
            } else {
                // we are actually splitting stuff
                break;
            }
        }
        // compute index of child nodes
        int nextIndex = tempTree.getSize();
        // allocate left node
        int nl = right - left + 1;
        int nr = rightOrig - (right + 1) + 1;
        if (nl > 0) {
            tempTree.add(0);
            tempTree.add(0);
            tempTree.add(0);
        } else
            nextIndex -= 3;
        // allocate right node
        if (nr > 0) {
            tempTree.add(0);
            tempTree.add(0);
            tempTree.add(0);
        }
        // write leaf node
        stats.updateInner();
        tempTree.set(nodeIndex + 0, (axis << 30) | nextIndex);
        tempTree.set(nodeIndex + 1, Float.floatToRawIntBits(clipL));
        tempTree.set(nodeIndex + 2, Float.floatToRawIntBits(clipR));
        // prepare L/R child boxes
        float[] gridBoxL = new float[6];
        float[] gridBoxR = new float[6];
        float[] nodeBoxL = new float[6];
        float[] nodeBoxR = new float[6];
        for (int i = 0; i < 6; i++) {
            gridBoxL[i] = gridBoxR[i] = gridBox[i];
            nodeBoxL[i] = nodeBoxR[i] = nodeBox[i];
        }
        gridBoxL[2 * axis + 1] = gridBoxR[2 * axis] = split;
        nodeBoxL[2 * axis + 1] = clipL;
        nodeBoxR[2 * axis + 0] = clipR;
        // free memory
        gridBox = nodeBox = null;
        // recurse
        if (nl > 0)
            subdivide(left, right, tempTree, indices, gridBoxL, nodeBoxL, nextIndex, depth + 1, stats);
        else
            stats.updateLeaf(depth + 1, 0);
        if (nr > 0)
            subdivide(right + 1, rightOrig, tempTree, indices, gridBoxR, nodeBoxR, nextIndex + 3, depth + 1, stats);
        else
            stats.updateLeaf(depth + 1, 0);
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    public void intersect(Ray r, IntersectionState state) {
        float intervalMin = r.getMin();
        float intervalMax = r.getMax();
        float orgX = r.ox;
        float dirX = r.dx, invDirX = 1 / dirX;
        float t1, t2;
        t1 = (bounds.getMinimum().x - orgX) * invDirX;
        t2 = (bounds.getMaximum().x - orgX) * invDirX;
        if (invDirX > 0) {
            if (t1 > intervalMin)
                intervalMin = t1;
            if (t2 < intervalMax)
                intervalMax = t2;
        } else {
            if (t2 > intervalMin)
                intervalMin = t2;
            if (t1 < intervalMax)
                intervalMax = t1;
        }
        if (intervalMin > intervalMax)
            return;
        float orgY = r.oy;
        float dirY = r.dy, invDirY = 1 / dirY;
        t1 = (bounds.getMinimum().y - orgY) * invDirY;
        t2 = (bounds.getMaximum().y - orgY) * invDirY;
        if (invDirY > 0) {
            if (t1 > intervalMin)
                intervalMin = t1;
            if (t2 < intervalMax)
                intervalMax = t2;
        } else {
            if (t2 > intervalMin)
                intervalMin = t2;
            if (t1 < intervalMax)
                intervalMax = t1;
        }
        if (intervalMin > intervalMax)
            return;
        float orgZ = r.oz;
        float dirZ = r.dz, invDirZ = 1 / dirZ;
        t1 = (bounds.getMinimum().z - orgZ) * invDirZ;
        t2 = (bounds.getMaximum().z - orgZ) * invDirZ;
        if (invDirZ > 0) {
            if (t1 > intervalMin)
                intervalMin = t1;
            if (t2 < intervalMax)
                intervalMax = t2;
        } else {
            if (t2 > intervalMin)
                intervalMin = t2;
            if (t1 < intervalMax)
                intervalMax = t1;
        }
        if (intervalMin > intervalMax)
            return;

        // compute custom offsets from direction sign bit

        int offsetXFront = Float.floatToRawIntBits(dirX) >>> 31;
        int offsetYFront = Float.floatToRawIntBits(dirY) >>> 31;
        int offsetZFront = Float.floatToRawIntBits(dirZ) >>> 31;

        int offsetXBack = offsetXFront ^ 1;
        int offsetYBack = offsetYFront ^ 1;
        int offsetZBack = offsetZFront ^ 1;

        int offsetXFront3 = offsetXFront * 3;
        int offsetYFront3 = offsetYFront * 3;
        int offsetZFront3 = offsetZFront * 3;

        int offsetXBack3 = offsetXBack * 3;
        int offsetYBack3 = offsetYBack * 3;
        int offsetZBack3 = offsetZBack * 3;

        // avoid always adding 1 during the inner loop
        offsetXFront++;
        offsetYFront++;
        offsetZFront++;
        offsetXBack++;
        offsetYBack++;
        offsetZBack++;

        int[] nodeStack = state.iscratch;
        float[] tStack = state.fscratch;

        int nstackPos = 0;
        int tstackPos = 0;
        int node = 0;

        while (true) {
            pushloop: while (true) {
                int tn = tree[node];
                int axis = tn & (3 << 30);
                int offset = tn & ~(3 << 30);
                switch (axis) {
                    case 0: {
                        // x axis
                        float tf = (Float.intBitsToFloat(tree[node + offsetXFront]) - orgX) * invDirX;
                        float tb = (Float.intBitsToFloat(tree[node + offsetXBack]) - orgX) * invDirX;
                        // ray passes between clip zones
                        if (tf < intervalMin && tb > intervalMax)
                            break pushloop;
                        int back = offset + offsetXBack3;
                        node = back;
                        // ray passes through far node only
                        if (tf < intervalMin) {
                            intervalMin = (tb >= intervalMin) ? tb : intervalMin;
                            continue;
                        }
                        node = offset + offsetXFront3; // front
                        // ray passes through near node only
                        if (tb > intervalMax) {
                            intervalMax = (tf <= intervalMax) ? tf : intervalMax;
                            continue;
                        }
                        // ray passes through both nodes
                        // push back node
                        nodeStack[nstackPos] = back;
                        tStack[tstackPos + 0] = (tb >= intervalMin) ? tb : intervalMin;
                        tStack[tstackPos + 1] = intervalMax;
                        nstackPos++;
                        tstackPos += 2;
                        // update ray interval for front node
                        intervalMax = (tf <= intervalMax) ? tf : intervalMax;
                        continue;
                    }
                    case 1 << 30: {
                        float tf = (Float.intBitsToFloat(tree[node + offsetYFront]) - orgY) * invDirY;
                        float tb = (Float.intBitsToFloat(tree[node + offsetYBack]) - orgY) * invDirY;
                        // ray passes between clip zones
                        if (tf < intervalMin && tb > intervalMax)
                            break pushloop;
                        int back = offset + offsetYBack3;
                        node = back;
                        // ray passes through far node only
                        if (tf < intervalMin) {
                            intervalMin = (tb >= intervalMin) ? tb : intervalMin;
                            continue;
                        }
                        node = offset + offsetYFront3; // front
                        // ray passes through near node only
                        if (tb > intervalMax) {
                            intervalMax = (tf <= intervalMax) ? tf : intervalMax;
                            continue;
                        }
                        // ray passes through both nodes
                        // push back node
                        nodeStack[nstackPos] = back;
                        tStack[tstackPos + 0] = (tb >= intervalMin) ? tb : intervalMin;
                        tStack[tstackPos + 1] = intervalMax;
                        nstackPos++;
                        tstackPos += 2;
                        // update ray interval for front node
                        intervalMax = (tf <= intervalMax) ? tf : intervalMax;
                        continue;
                    }
                    case 2 << 30: {
                        // z axis
                        float tf = (Float.intBitsToFloat(tree[node + offsetZFront]) - orgZ) * invDirZ;
                        float tb = (Float.intBitsToFloat(tree[node + offsetZBack]) - orgZ) * invDirZ;
                        // ray passes between clip zones
                        if (tf < intervalMin && tb > intervalMax)
                            break pushloop;
                        int back = offset + offsetZBack3;
                        node = back;
                        // ray passes through far node only
                        if (tf < intervalMin) {
                            intervalMin = (tb >= intervalMin) ? tb : intervalMin;
                            continue;
                        }
                        node = offset + offsetZFront3; // front
                        // ray passes through near node only
                        if (tb > intervalMax) {
                            intervalMax = (tf <= intervalMax) ? tf : intervalMax;
                            continue;
                        }
                        // ray passes through both nodes
                        // push back node
                        nodeStack[nstackPos] = back;
                        tStack[tstackPos + 0] = (tb >= intervalMin) ? tb : intervalMin;
                        tStack[tstackPos + 1] = intervalMax;
                        nstackPos++;
                        tstackPos += 2;
                        // update ray interval for front node
                        intervalMax = (tf <= intervalMax) ? tf : intervalMax;
                        continue;
                    }
                    default: {
                        // leaf - test some objects
                        int n = tree[node + 1];
                        while (n > 0) {
                            objects[offset].intersect(r, state);
                            n--;
                            offset++;
                        }
                        break pushloop;
                    }
                } // switch
            } // traversal loop
            do {
                // stack is empty?
                if (nstackPos == 0)
                    return;
                // move back up the stack
                nstackPos--;
                tstackPos -= 2;
                node = nodeStack[nstackPos];
                intervalMin = tStack[tstackPos + 0];
                intervalMax = tStack[tstackPos + 1];
                // early termination
            } while (r.getMax() < intervalMin);
        }
    }
}