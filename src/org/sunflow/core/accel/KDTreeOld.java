package org.sunflow.core.accel;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.sunflow.core.BoundedPrimitive;
import org.sunflow.core.IntersectionAccelerator;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.Ray;
import org.sunflow.image.Color;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Point3;
import org.sunflow.system.Memory;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.util.IntArray;

public class KDTreeOld implements IntersectionAccelerator {
    private static final float INTERSECT_COST = 0.5f;
    private static final float TRAVERSAL_COST = 1;
    private static final float EMPTY_BONUS = 0.2f;
    private static final float CUTOFF_RATIO = 1e-6f;
    private static final int MAX_DEPTH = 64;

    private static boolean dump = false;
    private static String dumpPrefix = "kdtree";

    private int[] tree;
    private BoundedPrimitive[] objects;
    private BoundingBox bounds;
    private int maxPrims;

    public static void setDumpMode(boolean dump, String prefix) {
        KDTreeOld.dump = dump;
        KDTreeOld.dumpPrefix = prefix;
    }

    public KDTreeOld() {
        this(0);
    }

    public KDTreeOld(int maxPrims) {
        this.maxPrims = maxPrims;
    }

    public boolean build(ArrayList<BoundedPrimitive> objects) {
        UI.printInfo("[KDT] KDTree settings");
        UI.printInfo("[KDT]   * Max Leaf Size:  %d", maxPrims);
        UI.printInfo("[KDT]   * Max Depth:      %d", MAX_DEPTH);
        UI.printInfo("[KDT]   * Traversal cost: %.2f", TRAVERSAL_COST);
        UI.printInfo("[KDT]   * Intersect cost: %.2f", INTERSECT_COST);
        UI.printInfo("[KDT]   * Cutoff ratio:   %.2f", CUTOFF_RATIO);
        UI.printInfo("[KDT]   * Empty bonus:    %.2f", EMPTY_BONUS);
        UI.printInfo("[KDT]   * Dump leaves:    %s", dump ? "enabled" : "disabled");
        Timer total = new Timer();
        total.start();
        this.objects = objects.toArray(new BoundedPrimitive[objects.size()]);
        bounds = new BoundingBox();
        float[] objectBounds = new float[this.objects.length * 6];
        float[] rootBounds = new float[6];
        int[] prims = new int[this.objects.length];
        UI.taskStart("[KDT] Preparing objects", 0, this.objects.length);
        int i = 0, ib = 0;
        for (BoundedPrimitive o : objects) {
            BoundingBox b = o.getBounds();
            prims[i] = i;
            bounds.include(b);
            objectBounds[ib + 0] = b.getMinimum().x;
            objectBounds[ib + 1] = b.getMaximum().x;
            objectBounds[ib + 2] = b.getMinimum().y;
            objectBounds[ib + 3] = b.getMaximum().y;
            objectBounds[ib + 4] = b.getMinimum().z;
            objectBounds[ib + 5] = b.getMaximum().z;
            UI.taskUpdate(i);
            if (UI.taskCanceled()) {
                UI.taskStop();
                return false;
            }
            i++;
            ib += 6;
        }
        UI.taskStop();
        rootBounds[0] = bounds.getMinimum().x;
        rootBounds[1] = bounds.getMaximum().x;
        rootBounds[2] = bounds.getMinimum().y;
        rootBounds[3] = bounds.getMaximum().y;
        rootBounds[4] = bounds.getMinimum().z;
        rootBounds[5] = bounds.getMaximum().z;
        UI.taskStart("[KDT] Building tree", 0, 4);
        UI.taskUpdate(0);
        Timer t = new Timer();
        t.start();
        IntArray tempTree = new IntArray();
        ArrayList<BoundedPrimitive> tempList = new ArrayList<BoundedPrimitive>();
        tempTree.add(0);
        tempTree.add(1);
        buildTree(rootBounds, objectBounds, 1, prims, tempTree, 0, tempList);
        UI.taskUpdate(1);
        // remove references to let GC reclaim the memory when it needs to
        rootBounds = objectBounds = null;
        UI.taskUpdate(2);
        tree = tempTree.trim();
        tempTree = null;
        UI.taskUpdate(3);
        this.objects = tempList.toArray(new BoundedPrimitive[tempList.size()]);
        tempList = null;
        t.end();
        UI.taskStop();
        int numNodes = numNodes(0);
        int numLeaves = numLeaves(0);
        int numObjects = numObjects(0);
        total.end();
        UI.printInfo("[KDT] KDTree stats:");
        UI.printInfo("[KDT]   * Nodes:          %d", numNodes);
        UI.printInfo("[KDT]   * Leaves:         %d", numLeaves);
        UI.printInfo("[KDT]   * Objects: min    %d", minObjects(0));
        UI.printInfo("[KDT]              avg    %.2f", (float) numObjects / numLeaves);
        UI.printInfo("[KDT]              max    %d", maxObjects(0));
        UI.printInfo("[KDT]   * Depth:   min    %d", minDepth(0, 1));
        UI.printInfo("[KDT]              avg    %.2f", (float) sumDepth(0, 1) / numLeaves);
        UI.printInfo("[KDT]              max    %d", maxDepth(0, 1));
        UI.printInfo("[KDT]   * Leaves w/: N=0  %3d%%", 100 * numNLeaves(0, 0, 0) / numLeaves);
        UI.printInfo("[KDT]                N=1  %3d%%", 100 * numNLeaves(0, 1, 1) / numLeaves);
        UI.printInfo("[KDT]                N=2  %3d%%", 100 * numNLeaves(0, 2, 2) / numLeaves);
        UI.printInfo("[KDT]                N=3  %3d%%", 100 * numNLeaves(0, 3, 3) / numLeaves);
        UI.printInfo("[KDT]                N=4  %3d%%", 100 * numNLeaves(0, 4, 4) / numLeaves);
        UI.printInfo("[KDT]                N>4  %3d%%", 100 * numNLeaves(0, 5, Integer.MAX_VALUE) / numLeaves);
        UI.printInfo("[KDT]   * Node memory:    %s", Memory.sizeof(tree));
        UI.printInfo("[KDT]   * Object array:   %d", this.objects.length);
        UI.printInfo("[KDT]   * Tree creation:  %s", t.toString());
        UI.printInfo("[KDT]   * Build time:     %s", total.toString());
        if (dump) {
            try {
                UI.printInfo("[KDT] Dumping mtls to %s.mtl ...", dumpPrefix);
                FileWriter mtlFile = new FileWriter(dumpPrefix + ".mtl");
                int maxN = maxObjects(0);
                for (int n = 0; n <= maxN; n++) {
                    float blend = (float) n / (float) maxN;
                    Color nc;
                    if (blend < 0.25)
                        nc = Color.blend(Color.BLUE, Color.GREEN, blend / 0.25f);
                    else if (blend < 0.5)
                        nc = Color.blend(Color.GREEN, Color.YELLOW, (blend - 0.25f) / 0.25f);
                    else if (blend < 0.75)
                        nc = Color.blend(Color.YELLOW, Color.RED, (blend - 0.50f) / 0.25f);
                    else
                        nc = Color.MAGENTA;
                    mtlFile.write(String.format("newmtl mtl%d\n", n));
                    float[] rgb = nc.getRGB();
                    mtlFile.write("Ka 0.1 0.1 0.1\n");
                    mtlFile.write(String.format("Kd %.12g %.12g %.12g\n", rgb[0], rgb[1], rgb[2]));
                    mtlFile.write("illum 1\n\n");
                }
                FileWriter objFile = new FileWriter(dumpPrefix + ".obj");
                UI.printInfo("[KDT] Dumping tree to %s.obj ...", dumpPrefix);
                dumpObj(0, 0, maxN, new BoundingBox(bounds), objFile, mtlFile);
                objFile.close();
                mtlFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private int dumpObj(int offset, int vertOffset, int maxN, BoundingBox bounds, FileWriter file, FileWriter mtlFile) throws IOException {
        if (offset == 0)
            file.write(String.format("mtllib %s.mtl\n", dumpPrefix));
        int nextOffset = tree[offset];
        if ((nextOffset & (3 << 30)) == (3 << 30)) {
            // leaf
            int n = tree[offset + 1];
            if (n > 0) {
                // output the current voxel to the file
                Point3 min = bounds.getMinimum();
                Point3 max = bounds.getMaximum();
                file.write(String.format("o node%d\n", offset));
                file.write(String.format("v %g %g %g\n", max.x, max.y, min.z));
                file.write(String.format("v %g %g %g\n", max.x, min.y, min.z));
                file.write(String.format("v %g %g %g\n", min.x, min.y, min.z));
                file.write(String.format("v %g %g %g\n", min.x, max.y, min.z));
                file.write(String.format("v %g %g %g\n", max.x, max.y, max.z));
                file.write(String.format("v %g %g %g\n", max.x, min.y, max.z));
                file.write(String.format("v %g %g %g\n", min.x, min.y, max.z));
                file.write(String.format("v %g %g %g\n", min.x, max.y, max.z));
                int v0 = vertOffset;
                file.write(String.format("usemtl mtl%d\n", n));
                file.write("s off\n");
                file.write(String.format("f %d %d %d %d\n", v0 + 1, v0 + 2, v0 + 3, v0 + 4));
                file.write(String.format("f %d %d %d %d\n", v0 + 5, v0 + 8, v0 + 7, v0 + 6));
                file.write(String.format("f %d %d %d %d\n", v0 + 1, v0 + 5, v0 + 6, v0 + 2));
                file.write(String.format("f %d %d %d %d\n", v0 + 2, v0 + 6, v0 + 7, v0 + 3));
                file.write(String.format("f %d %d %d %d\n", v0 + 3, v0 + 7, v0 + 8, v0 + 4));
                file.write(String.format("f %d %d %d %d\n", v0 + 5, v0 + 1, v0 + 4, v0 + 8));
                vertOffset += 8;
            }
            return vertOffset;
        } else {
            // node, recurse
            int axis = nextOffset & (3 << 30), v0;
            float split = Float.intBitsToFloat(tree[offset + 1]), min, max;
            nextOffset &= ~(3 << 30);
            switch (axis) {
                case 0:
                    max = bounds.getMaximum().x;
                    bounds.getMaximum().x = split;
                    v0 = dumpObj(nextOffset, vertOffset, maxN, bounds, file, mtlFile);
                    // restore and go to other side
                    bounds.getMaximum().x = max;
                    min = bounds.getMinimum().x;
                    bounds.getMinimum().x = split;
                    v0 = dumpObj(nextOffset + 2, v0, maxN, bounds, file, mtlFile);
                    bounds.getMinimum().x = min;
                    break;
                case 1 << 30:
                    max = bounds.getMaximum().y;
                    bounds.getMaximum().y = split;
                    v0 = dumpObj(nextOffset, vertOffset, maxN, bounds, file, mtlFile);
                    // restore and go to other side
                    bounds.getMaximum().y = max;
                    min = bounds.getMinimum().y;
                    bounds.getMinimum().y = split;
                    v0 = dumpObj(nextOffset + 2, v0, maxN, bounds, file, mtlFile);
                    bounds.getMinimum().y = min;
                    break;
                case 2 << 30:
                    max = bounds.getMaximum().z;
                    bounds.getMaximum().z = split;
                    v0 = dumpObj(nextOffset, vertOffset, maxN, bounds, file, mtlFile);
                    // restore and go to other side
                    bounds.getMaximum().z = max;
                    min = bounds.getMinimum().z;
                    bounds.getMinimum().z = split;
                    v0 = dumpObj(nextOffset + 2, v0, maxN, bounds, file, mtlFile);
                    // restore and go to other side
                    bounds.getMinimum().z = min;
                    break;
                default:
                    v0 = vertOffset;
                    break;
            }
            return v0;
        }
    }

    private int numNodes(int offset) {
        int nextOffset = tree[offset];
        if ((nextOffset & (3 << 30)) == (3 << 30))
            return 0;
        else {
            nextOffset &= ~(3 << 30);
            return 1 + numNodes(nextOffset) + numNodes(nextOffset + 2);
        }
    }

    private int numLeaves(int offset) {
        int nextOffset = tree[offset];
        if ((nextOffset & (3 << 30)) == (3 << 30))
            return 1;
        else {
            nextOffset &= ~(3 << 30);
            return numLeaves(nextOffset) + numLeaves(nextOffset + 2);
        }
    }

    private int numNLeaves(int offset, int min, int max) {
        int nextOffset = tree[offset];
        if ((nextOffset & (3 << 30)) == (3 << 30)) {
            if (tree[offset + 1] >= min && tree[offset + 1] <= max)
                return 1;
            else
                return 0;
        } else {
            nextOffset &= ~(3 << 30);
            return numNLeaves(nextOffset, min, max) + numNLeaves(nextOffset + 2, min, max);
        }
    }

    private int numObjects(int offset) {
        int nextOffset = tree[offset];
        if ((nextOffset & (3 << 30)) == (3 << 30))
            return tree[offset + 1];
        else {
            nextOffset &= ~(3 << 30);
            return numObjects(nextOffset) + numObjects(nextOffset + 2);
        }
    }

    private int minObjects(int offset) {
        int nextOffset = tree[offset];
        if ((nextOffset & (3 << 30)) == (3 << 30))
            return tree[offset + 1];
        else {
            nextOffset &= ~(3 << 30);
            return Math.min(minObjects(nextOffset), minObjects(nextOffset + 2));
        }
    }

    private int maxObjects(int offset) {
        int nextOffset = tree[offset];
        if ((nextOffset & (3 << 30)) == (3 << 30))
            return tree[offset + 1];
        else {
            nextOffset &= ~(3 << 30);
            return Math.max(maxObjects(nextOffset), maxObjects(nextOffset + 2));
        }
    }

    private int minDepth(int offset, int depth) {
        int nextOffset = tree[offset];
        if ((nextOffset & (3 << 30)) == (3 << 30))
            return depth;
        else {
            nextOffset &= ~(3 << 30);
            return Math.min(minDepth(nextOffset, depth + 1), minDepth(nextOffset + 2, depth + 1));
        }
    }

    private int maxDepth(int offset, int depth) {
        int nextOffset = tree[offset];
        if ((nextOffset & (3 << 30)) == (3 << 30))
            return depth;
        else {
            nextOffset &= ~(3 << 30);
            return Math.max(maxDepth(nextOffset, depth + 1), maxDepth(nextOffset + 2, depth + 1));
        }
    }

    private int sumDepth(int offset, int depth) {
        int nextOffset = tree[offset];
        if ((nextOffset & (3 << 30)) == (3 << 30))
            return depth;
        else {
            nextOffset &= ~(3 << 30);
            return sumDepth(nextOffset, depth + 1) + sumDepth(nextOffset + 2, depth + 1);
        }
    }

    // 3 histograms on the stack:
    static int[] b0 = new int[2048];
    static int[] b1 = new int[2048];
    static int[] b2 = new int[2048];

    // pack split values into a 64bit integer
    private static long pack(float split, boolean isLeft, int object) {
        // pack float in sortable form
        int f = Float.floatToRawIntBits(split);
        int m = (-(f >>> 31)) | 0x80000000;
        int top = f ^ m;
        long p = ((long) top & 0xFFFFFFFFL) << 32;
        // pack isLeft bit
        if (!isLeft)
            p |= 0x80000000L;
        // pack object number
        p |= (object & 0x7FFFFFFFL);
        return p;
    }

    private static int unpackObject(long p) {
        return (int) (p & 0x7FFFFFFFL);
    }

    private static float unpackSplit(long p) {
        int f = (int) ((p >>> 32) & 0xFFFFFFFFL);
        int m = ((f >>> 31) - 1) | 0x80000000;
        return Float.intBitsToFloat(f ^ m);
    }

    private static boolean unpackIsLeft(long p) {
        return (p & 0x80000000L) == 0;
    }

    // compare splits
    private static boolean compareSplits(long a, long b) {
        a >>>= 31;
        b >>>= 31;
        return a < b;
    }

    // radix sort - returns sorted result
    private static void radix11(long[] array, long[] sort, int n) {
        Arrays.fill(b0, 0);
        Arrays.fill(b1, 0);
        Arrays.fill(b2, 0);
        // 1. parallel histogramming pass
        for (int i = 0; i < n; i++) {
            long pi = array[i];
            b0[(int) (pi >>> 31) & 0x7FF]++;
            b1[(int) (pi >>> 42) & 0x7FF]++;
            b2[(int) (pi >>> 53)]++;
        }

        // 2. Sum the histograms -- each histogram entry records the number
        // of values preceding itself.
        {
            int sum0 = 0, sum1 = 0, sum2 = 0;
            int tsum;
            for (int i = 0; i < 2048; i++) {
                tsum = b0[i] + sum0;
                b0[i] = sum0 - 1;
                sum0 = tsum;
                tsum = b1[i] + sum1;
                b1[i] = sum1 - 1;
                sum1 = tsum;
                tsum = b2[i] + sum2;
                b2[i] = sum2 - 1;
                sum2 = tsum;
            }
        }

        // byte 0: floatflip entire value, read/write histogram, write out
        // flipped
        for (int i = 0; i < n; i++) {
            long pi = array[i];
            int pos = (int) (pi >>> 31) & 0x7FF;
            sort[++b0[pos]] = pi;
        }
        // byte 1: read/write histogram, copy
        // sorted -> array
        for (int i = 0; i < n; i++) {
            long si = sort[i];
            int pos = (int) (si >>> 42) & 0x7FF;
            array[++b1[pos]] = si;
        }

        // byte 2: read/write histogram, copy & flip out
        // array -> sorted
        for (int i = 0; i < n; i++) {
            long ai = array[i];
            int pos = (int) (ai >>> 53);
            sort[++b2[pos]] = ai;
        }
    }

    private static int med3(int a, int b, int c, long[] d) {
        return (compareSplits(d[a], d[b]) ? (compareSplits(d[b], d[c]) ? b : compareSplits(d[a], d[c]) ? c : a) : (compareSplits(d[c], d[b]) ? b : compareSplits(d[c], d[a]) ? c : a));
    }

    private static void swap(int i, int j, long[] a) {
        long c = a[i];
        a[i] = a[j];
        a[j] = c;
    }

    private static void vecswap(int i, int j, int n, long[] a) {
        for (; n > 0; i++, j++, n--)
            swap(i, j, a);
    }

    private static void qsort(long[] array, int from, int count) {
        // Use an insertion sort on small arrays.
        if (count <= 7) {
            for (int i = from + 1; i < from + count; i++)
                for (int j = i; j > from && compareSplits(array[j], array[j - 1]); j--)
                    swap(j, j - 1, array);
            return;
        }

        // Determine a good median element.
        int mid = count / 2;
        int lo = from;
        int hi = from + count - 1;

        if (count > 40) { // big arrays, pseudomedian of 9
            int s = count / 8;
            lo = med3(lo, lo + s, lo + 2 * s, array);
            mid = med3(mid - s, mid, mid + s, array);
            hi = med3(hi - 2 * s, hi - s, hi, array);
        }
        mid = med3(lo, mid, hi, array);

        int a, b, c, d;
        long comp;

        // Pull the median element out of the fray, and use it as a pivot.
        swap(from, mid, array);
        a = b = from;
        c = d = from + count - 1;

        // Repeatedly move b and c to each other, swapping elements so
        // that all elements before index b are less than the pivot, and all
        // elements after index c are greater than the pivot. a and b track
        // the elements equal to the pivot.
        while (true) {
            while (b <= c && (comp = (array[b] >>> 31) - (array[from] >>> 31)) <= 0) {
                if (comp == 0) {
                    swap(a, b, array);
                    a++;
                }
                b++;
            }
            while (c >= b && (comp = (array[c] >>> 31) - (array[from] >>> 31)) >= 0) {
                if (comp == 0) {
                    swap(c, d, array);
                    d--;
                }
                c--;
            }
            if (b > c)
                break;
            swap(b, c, array);
            b++;
            c--;
        }

        // Swap pivot(s) back in place, the recurse on left and right
        // sections.
        hi = from + count;
        int span;
        span = Math.min(a - from, b - a);
        vecswap(from, b - span, span, array);

        span = Math.min(d - c, hi - d - 1);
        vecswap(b, hi - span, span, array);

        span = b - a;
        if (span > 1)
            qsort(array, from, span);

        span = d - c;
        if (span > 1)
            qsort(array, hi - span, span);
    }

    void buildTree(float[] nodeBounds, float[] objectBounds, int depth, int[] objects, IntArray tempTree, int offset, ArrayList<BoundedPrimitive> tempList) {
        // get node bounding box extents
        float minx = nodeBounds[0];
        float maxx = nodeBounds[1];
        float miny = nodeBounds[2];
        float maxy = nodeBounds[3];
        float minz = nodeBounds[4];
        float maxz = nodeBounds[5];
        float dx = maxx - minx;
        float dy = maxy - miny;
        float dz = maxz - minz;
        if (dx < 0 || dy < 0 || dz < 0)
            System.out.println("warning negative bounds!!");
        float area = (dx * dy + dy * dz + dz * dx);
        float probability = area / bounds.getArea();
        if (objects.length > maxPrims && depth < MAX_DEPTH && probability > CUTOFF_RATIO) {
            // search for best possible split
            float bestCost = INTERSECT_COST * objects.length;
            int bestOffset = -1;
            float bestSplit = 0;
            // sort axes by length
            int[] axisOrder = new int[3];
            if (dx > dy) {
                if (dy > dz) {
                    axisOrder[0] = 0;
                    axisOrder[1] = 1;
                    axisOrder[2] = 2;
                } else {
                    if (dz > dx) {
                        axisOrder[0] = 2;
                        axisOrder[1] = 0;
                        axisOrder[2] = 1;
                    } else {
                        axisOrder[0] = 0;
                        axisOrder[1] = 2;
                        axisOrder[2] = 1;
                    }
                }
            } else {
                if (dx > dz) {
                    axisOrder[0] = 1;
                    axisOrder[1] = 0;
                    axisOrder[2] = 2;
                } else {
                    if (dy > dz) {
                        axisOrder[0] = 1;
                        axisOrder[1] = 2;
                        axisOrder[2] = 0;
                    } else {
                        axisOrder[0] = 2;
                        axisOrder[1] = 1;
                        axisOrder[2] = 0;
                    }
                }
            }
            // inverse area of the bounding box (factor of 2 ommitted)
            float ISECT_COST = INTERSECT_COST / area;
            int bnl = 0, bnr = 0;
            long[] splits = new long[2 * objects.length];
            long[] sorted = objects.length > 1024 ? new long[2 * objects.length] : null;
            for (int r = 0; r < 3; r++) {
                int axis = axisOrder[r];
                for (int i = 0, i2 = 0; i < objects.length; i++) {
                    int idx = 6 * objects[i] + axis * 2;
                    splits[i2++] = pack(objectBounds[idx + 0], true, i);// + Long.MIN_VALUE;
                    splits[i2++] = pack(objectBounds[idx + 1], false, i);// + Long.MIN_VALUE;
                }
                // sort splits
                if (sorted != null) {
                    // use radix sort for large arrays
                    radix11(splits, sorted, 2 * objects.length);
                    long[] t = splits;
                    splits = sorted;
                    sorted = t;
                } else {
                    // use a plain old quick sort for small arrays
                    qsort(splits, 0, 2 * objects.length);
                }
//                Arrays.sort(splits);
                if (!unpackIsLeft(splits[0]))
                    System.out.println("sort warning start @ D=" + depth);
                if (unpackIsLeft(splits[2 * objects.length - 1]))
                    System.out.println("sort warning end   @ D=" + depth);
                // search for best cost
                int numLeft = 0;
                int numRight = objects.length;
                float nodeMin;
                float nodeMax;
                float dp;
                float ds;
                switch (axis) {
                    case 0:
                        nodeMin = minx;
                        nodeMax = maxx;
                        dp = dy * dz;
                        ds = dy + dz;
                        break;
                    case 1:
                        nodeMin = miny;
                        nodeMax = maxy;
                        dp = dx * dz;
                        ds = dx + dz;
                        break;
                    default:
                        nodeMin = minz;
                        nodeMax = maxz;
                        dp = dy * dx;
                        ds = dy + dx;
                        break;
                }
                for (int i = 0; i < 2 * objects.length; i++) {
                    long ptr = splits[i];// + Long.MIN_VALUE;
                    //splits[i] = ptr;
                    float s = unpackSplit(ptr);
                    boolean isLeft = unpackIsLeft(ptr);
                    if (!isLeft)
                        numRight--;
                    if (s >= nodeMin && s <= nodeMax) {
                        // left and right surface area (factor of 2
                        // ommitted)
                        float lp = (dp + (s - nodeMin) * ds);
                        float rp = (dp + (nodeMax - s) * ds);
                        float eb = ((numLeft == 0 && s > nodeMin) || (numRight == 0 && s < nodeMax)) ? EMPTY_BONUS : 0;
                        float cost = TRAVERSAL_COST + ISECT_COST * (1 - eb) * (lp * numLeft + rp * numRight);
                        if (cost < bestCost) {
                            bestCost = cost;
                            bestSplit = s;
                            bestOffset = i;
                            bnl = numLeft;
                            bnr = numRight;
                        }
                    }
                    if (isLeft)
                        numLeft++;
                }
                if (numLeft != objects.length || numRight != 0)
                    System.out.println("ERROR: didn't scan full range of objects");
                // no best split found so far
                if (bestOffset == -1)
                    continue;
                // node
                int[] objectsL = new int[bnl];
                int[] objectsR = new int[bnr];
                for (int j = 0, k = 0; j < bestOffset; j++) {
                    if (unpackIsLeft(splits[j])) {
                        objectsL[k] = objects[unpackObject(splits[j])];
                        k++;
                    }
                }
                for (int j = bestOffset + 1, k = 0; j < 2 * objects.length; j++) {
                    if (!unpackIsLeft(splits[j])) {
                        objectsR[k] = objects[unpackObject(splits[j])];
                        k++;
                    }
                }
                // free up some memory
                objects = null;
                splits = null;
                //sorted = null;
                // allocate child nodes
                int nextOffset = tempTree.getSize();
                tempTree.add(0);
                tempTree.add(0);
                tempTree.add(0);
                tempTree.add(0);
                // create current node
                tempTree.set(offset + 0, (axis << 30) | nextOffset);
                tempTree.set(offset + 1, Float.floatToRawIntBits(bestSplit));

                // recurse for child nodes
                nodeBounds[2 * axis + 1] = bestSplit;
                buildTree(nodeBounds, objectBounds, depth + 1, objectsL, tempTree, nextOffset, tempList);
                nodeBounds[0] = minx;
                nodeBounds[1] = maxx;
                nodeBounds[2] = miny;
                nodeBounds[3] = maxy;
                nodeBounds[4] = minz;
                nodeBounds[5] = maxz;
                nodeBounds[2 * axis] = bestSplit;
                buildTree(nodeBounds, objectBounds, depth + 1, objectsR, tempTree, nextOffset + 2, tempList);
                nodeBounds[0] = minx;
                nodeBounds[1] = maxx;
                nodeBounds[2] = miny;
                nodeBounds[3] = maxy;
                nodeBounds[4] = minz;
                nodeBounds[5] = maxz;
                return;
            }
        }
        // create leaf node
        int listOffset = tempList.size();
        for (int o : objects)
            tempList.add(this.objects[o]);
        tempTree.set(offset + 0, (3 << 30) | listOffset);
        tempTree.set(offset + 1, objects.length);
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
            if (intervalMin > intervalMax)
                return;
        } else {
            if (t2 > intervalMin)
                intervalMin = t2;
            if (t1 < intervalMax)
                intervalMax = t1;
            if (intervalMin > intervalMax)
                return;
        }
        float orgY = r.oy;
        float dirY = r.dy, invDirY = 1 / dirY;
        t1 = (bounds.getMinimum().y - orgY) * invDirY;
        t2 = (bounds.getMaximum().y - orgY) * invDirY;
        if (invDirY > 0) {
            if (t1 > intervalMin)
                intervalMin = t1;
            if (t2 < intervalMax)
                intervalMax = t2;
            if (intervalMin > intervalMax)
                return;
        } else {
            if (t2 > intervalMin)
                intervalMin = t2;
            if (t1 < intervalMax)
                intervalMax = t1;
            if (intervalMin > intervalMax)
                return;
        }
        float orgZ = r.oz;
        float dirZ = r.dz, invDirZ = 1 / dirZ;
        t1 = (bounds.getMinimum().z - orgZ) * invDirZ;
        t2 = (bounds.getMaximum().z - orgZ) * invDirZ;
        if (invDirZ > 0) {
            if (t1 > intervalMin)
                intervalMin = t1;
            if (t2 < intervalMax)
                intervalMax = t2;
            if (intervalMin > intervalMax)
                return;
        } else {
            if (t2 > intervalMin)
                intervalMin = t2;
            if (t1 < intervalMax)
                intervalMax = t1;
            if (intervalMin > intervalMax)
                return;
        }

        // compute custom offsets from direction sign bit
        int offsetXFront = (Float.floatToRawIntBits(dirX) & (1 << 31)) >>> 30;
        int offsetYFront = (Float.floatToRawIntBits(dirY) & (1 << 31)) >>> 30;
        int offsetZFront = (Float.floatToRawIntBits(dirZ) & (1 << 31)) >>> 30;

        int offsetXBack = offsetXFront ^ 2;
        int offsetYBack = offsetYFront ^ 2;
        int offsetZBack = offsetZFront ^ 2;

        int[] nodeStack = state.iscratch;
        float[] tStack = state.fscratch;

        int nstackPos = 0;
        int tstackPos = 0;
        int node = 0;

        while (true) {
            while (true) {
                int axis = tree[node] & (3 << 30);
                // leaf?
                if (axis == (3 << 30))
                    break;
                int offset = tree[node] & ~(3 << 30);
                switch (axis) {
                    case 0: {
                        float d = (Float.intBitsToFloat(tree[node + 1]) - orgX) * invDirX;
                        int back = offset + offsetXBack;
                        int front = offset + offsetXFront;
                        node = back;
                        if (d < intervalMin)
                            continue;
                        node = front;
                        if (d > intervalMax)
                            continue;
                        // push back node
                        nodeStack[nstackPos] = back;
                        tStack[tstackPos + 0] = Math.max(d, intervalMin);
                        tStack[tstackPos + 1] = intervalMax;
                        nstackPos++;
                        tstackPos += 2;
                        // update ray interval for front node
                        intervalMax = Math.min(d, intervalMax);
                        continue;
                    }
                    case 1 << 30: {
                        // y axis
                        float d = (Float.intBitsToFloat(tree[node + 1]) - orgY) * invDirY;
                        int back = offset + offsetYBack;
                        int front = offset + offsetYFront;
                        node = back;
                        if (d < intervalMin)
                            continue;
                        node = front;
                        if (d > intervalMax)
                            continue;
                        // push back node
                        nodeStack[nstackPos] = back;
                        tStack[tstackPos + 0] = Math.max(d, intervalMin);
                        tStack[tstackPos + 1] = intervalMax;
                        nstackPos++;
                        tstackPos += 2;
                        // update ray interval for front node
                        intervalMax = Math.min(d, intervalMax);
                        continue;
                    }
                    case 2 << 30: {
                        // z axis
                        float d = (Float.intBitsToFloat(tree[node + 1]) - orgZ) * invDirZ;
                        int back = offset + offsetZBack;
                        int front = offset + offsetZFront;
                        node = back;
                        if (d < intervalMin)
                            continue;
                        node = front;
                        if (d > intervalMax)
                            continue;
                        // push back node
                        nodeStack[nstackPos] = back;
                        tStack[tstackPos + 0] = Math.max(d, intervalMin);
                        tStack[tstackPos + 1] = intervalMax;
                        nstackPos++;
                        tstackPos += 2;
                        // update ray interval for front node
                        intervalMax = Math.min(d, intervalMax);
                        continue;
                    }
                    default:
                        break;
                } // switch
            } // traversal loop
            // test some objects
            int n = tree[node + 1];
            if (n > 0) {
                int o = tree[node] & ~(3 << 30);
                for (int i = 0; i < n; i++)
                    objects[o + i].intersect(r, state);
                if (r.getMax() < intervalMax)
                    return;
            }
            // stack is empty?
            if (nstackPos == 0)
                return;
            // move back up the stack
            nstackPos--;
            tstackPos -= 2;
            node = nodeStack[nstackPos];
            intervalMin = tStack[tstackPos + 0];
            intervalMax = tStack[tstackPos + 1];
        }
    }
}