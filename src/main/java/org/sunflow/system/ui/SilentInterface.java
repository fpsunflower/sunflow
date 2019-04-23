package org.sunflow.system.ui;

import org.sunflow.system.UserInterface;
import org.sunflow.system.UI.Module;
import org.sunflow.system.UI.PrintLevel;

/**
 * Null implementation of a user interface. This is usefull to silence the
 * output.
 */
public class SilentInterface implements UserInterface {
    public void print(Module m, PrintLevel level, String s) {
    }

    public void taskStart(String s, int min, int max) {
    }

    public void taskUpdate(int current) {
    }

    public void taskStop() {
    }
}