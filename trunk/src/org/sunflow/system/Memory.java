package org.sunflow.system;

public final class Memory {
    public static final String sizeof(int[] array) {
        if (array == null)
            return "0b";
        long bytes = 4 * array.length;
        if (bytes < 1024)
            return String.format("%db", bytes);
        if (bytes < 1024 * 1024)
            return String.format("%dKb", (bytes + 512) >>> 10);
        return String.format("%dMb", (bytes + 512 * 1024) >>> 20);
    }
}