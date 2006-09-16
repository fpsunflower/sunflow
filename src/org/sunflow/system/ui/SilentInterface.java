package org.sunflow.system.ui;

import org.sunflow.system.UserInterface;

/**
 * Null implementation of a user interface. This is usefull to silence the
 * output.
 * 
 * @author Administrator
 */
public class SilentInterface implements UserInterface {
    public void printInfo(String s) {
    }

    public void printWarning(String s) {
    }

    public void printError(String s) {
    }

    public void taskStart(String s, int min, int max) {
    }

    public void taskUpdate(int current) {
    }

    public void taskStop() {
    }
}