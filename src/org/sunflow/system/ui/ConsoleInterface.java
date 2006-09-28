package org.sunflow.system.ui;

import org.sunflow.system.UserInterface;

/**
 * Basic console implementation of a user interface.
 */
public class ConsoleInterface implements UserInterface {
    private int min;
    private int max;
    private float invP;
    private String task;
    private int lastP;

    public ConsoleInterface() {
    }

    public void printDetailed(String s) {
        System.out.println(s);
    }
    
    public void printInfo(String s) {
        System.out.println(s);
    }

    public void printWarning(String s) {
        System.out.println("WARNING: " + s);
    }

    public void printError(String s) {
        System.out.println("ERROR: " + s);
    }

    public void taskStart(String s, int min, int max) {
        task = s;
        this.min = min;
        this.max = max;
        lastP = -1;
        invP = 100.0f / (max - min);
    }

    public void taskUpdate(int current) {
        int p = (min == max) ? 0 : (int) ((current - min) * invP);
        if (p != lastP)
            System.out.print(task + " [" + (lastP = p) + "%]\r");
    }

    public void taskStop() {
        System.out.print("                                                                      \r");
    }
}