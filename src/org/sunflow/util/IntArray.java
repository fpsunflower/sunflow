package org.sunflow.util;

public final class IntArray {
    private int[] array;
    private int size;

    public IntArray() {
        array = new int[10];
        size = 0;
    }
    
    public IntArray(int capacity) {
        array = new int[capacity];
        size = 0;
    }

    /**
     * Append an integer to the end of the array.
     * 
     * @param i
     */
    public final void add(int i) {
        if (size == array.length) {
            int[] oldArray = array;
            array = new int[(size * 3) / 2 + 1];
            System.arraycopy(oldArray, 0, array, 0, size);
        }
        array[size] = i;
        size++;
    }

    /**
     * Write a value to the specified index. Assumes the array is already big
     * enough.
     * 
     * @param index
     * @param value
     */
    public final void set(int index, int value) {
        array[index] = value;
    }

    /**
     * Returns the number of elements added to the array.
     * 
     * @return
     */
    public final int getSize() {
        return size;
    }

    /**
     * Return a copy of the array, trimmed to fit the size of its contents
     * exactly.
     * 
     * @return
     */
    public final int[] trim() {
        if (size < array.length) {
            int[] oldArray = array;
            array = new int[size];
            System.arraycopy(oldArray, 0, array, 0, size);
        }
        return array;
    }
}