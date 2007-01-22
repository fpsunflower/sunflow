package org.sunflow.math;

import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public final class QMC {
    private static final int NUM = 128;
    private static final int[][] SIGMA = new int[NUM][];
    private static final int[] PRIMES = new int[NUM];

    static {
        UI.printInfo(Module.QMC, "Initializing Faure scrambling tables ...");
        // build table of first primes
        PRIMES[0] = 2;
        for (int i = 1; i < PRIMES.length; i++)
            PRIMES[i] = nextPrime(PRIMES[i - 1]);
        int[][] table = new int[PRIMES[PRIMES.length - 1] + 1][];
        table[2] = new int[2];
        table[2][0] = 0;
        table[2][1] = 1;
        for (int i = 3; i <= PRIMES[PRIMES.length - 1]; i++) {
            table[i] = new int[i];
            if ((i & 1) == 0) {
                int[] prev = table[i >> 1];
                for (int j = 0; j < prev.length; j++)
                    table[i][j] = 2 * prev[j];
                for (int j = 0; j < prev.length; j++)
                    table[i][prev.length + j] = 2 * prev[j] + 1;
            } else {
                int[] prev = table[i - 1];
                int med = (i - 1) >> 1;
                for (int j = 0; j < med; j++)
                    table[i][j] = prev[j] + ((prev[j] >= med) ? 1 : 0);
                table[i][med] = med;
                for (int j = 0; j < med; j++)
                    table[i][med + j + 1] = prev[j + med] + ((prev[j + med] >= med) ? 1 : 0);
            }
        }
        for (int i = 0; i < PRIMES.length; i++) {
            int p = PRIMES[i];
            SIGMA[i] = new int[p];
            System.arraycopy(table[p], 0, SIGMA[i], 0, p);
        }
    }

    private static final int nextPrime(int p) {
        p = p + (p & 1) + 1;
        while (true) {
            int div = 3;
            boolean isPrime = true;
            while (isPrime && ((div * div) <= p)) {
                isPrime = ((p % div) != 0);
                div += 2;
            }
            if (isPrime)
                return p;
            p += 2;
        }
    }

    private QMC() {
    }

    public static double riVDC(int bits, int r) {
        bits = (bits << 16) | (bits >>> 16);
        bits = ((bits & 0x00ff00ff) << 8) | ((bits & 0xff00ff00) >>> 8);
        bits = ((bits & 0x0f0f0f0f) << 4) | ((bits & 0xf0f0f0f0) >>> 4);
        bits = ((bits & 0x33333333) << 2) | ((bits & 0xcccccccc) >>> 2);
        bits = ((bits & 0x55555555) << 1) | ((bits & 0xaaaaaaaa) >>> 1);
        bits ^= r;
        return (double) (bits & 0xFFFFFFFFL) / (double) 0x100000000L;
    }

    public static double riS(int i, int r) {
        for (int v = 1 << 31; i != 0; i >>>= 1, v ^= v >>> 1)
            if ((i & 1) != 0)
                r ^= v;
        return (double) r / (double) 0x100000000L;
    }

    public static double riLP(int i, int r) {
        for (int v = 1 << 31; i != 0; i >>>= 1, v |= v >>> 1)
            if ((i & 1) != 0)
                r ^= v;
        return (double) r / (double) 0x100000000L;
    }

    public static final double halton(int d, int i) {
        // generalized Halton sequence
        switch (d) {
            case 0: {
                i = (i << 16) | (i >>> 16);
                i = ((i & 0x00ff00ff) << 8) | ((i & 0xff00ff00) >>> 8);
                i = ((i & 0x0f0f0f0f) << 4) | ((i & 0xf0f0f0f0) >>> 4);
                i = ((i & 0x33333333) << 2) | ((i & 0xcccccccc) >>> 2);
                i = ((i & 0x55555555) << 1) | ((i & 0xaaaaaaaa) >>> 1);
                return (double) (i & 0xFFFFFFFFL) / (double) 0x100000000L;
            }
            case 1: {
                double v = 0;
                double inv = 1.0 / 3;
                double p;
                int n;
                for (p = inv, n = i; n != 0; p *= inv, n /= 3)
                    v += (n % 3) * p;
                return v;
            }
            default:
        }
        int base = PRIMES[d];
        int[] perm = SIGMA[d];
        double v = 0;
        double inv = 1.0 / base;
        double p;
        int n;
        for (p = inv, n = i; n != 0; p *= inv, n /= base)
            v += perm[n % base] * p;
        return v;
    }

    public static final double mod1(double x) {
        // assumes x >= 0
        return x - (int) x;
    }

    public static final int[] generateSigmaTable(int n) {
        assert (n & (n - 1)) == 0;
        int[] sigma = new int[n];
        for (int i = 0; i < n; i++) {
            int digit = n;
            sigma[i] = 0;
            for (int bits = i; bits != 0; bits >>= 1) {
                digit >>= 1;
                if ((bits & 1) != 0)
                    sigma[i] += digit;
            }
        }
        return sigma;
    }
}